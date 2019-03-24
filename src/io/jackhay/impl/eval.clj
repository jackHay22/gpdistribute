(ns io.jackhay.gpdistribute.impl.eval
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [io.jackhay.gpdistribute.impl.utils :as utils])
  (:import [java.net ServerSocket SocketException Socket InetAddress]))

;-------STATE-------
;Holds individuals being used as tests for individuals in test mode
(def POPULATION-POOL (atom (list)))

;Number Received individuals per generation (logging)
(def INDIV-COUNT (atom 0))

;This is the current evaluation cycle:
; if a new cycle is detected the population pool is purged
(def CURRENT-CYCLE (atom 0))

(defn- valid-indiv?
  "takes individual, checks if valid
  -> bool"
  [i]
  ;(and
    (map? i)
    ;TODO: check if clojush individual
    ;(instance? individual i)
    ;)
    )

(defn- status-task
  "runnable (thread) log process"
  [delay-seconds]
  (let [delay (* delay-seconds 1000)]
    (future
      (loop []
        (do
          (log/write-info (str "Active threads: " (Thread/activeCount)))
          (log/write-info (str "Opponent pool size: " (count @POPULATION-POOL)))
          (log/write-info (str "Current cycle: " @CURRENT-CYCLE))
          (log/write-info (str "Simulation started on " @INDIV-COUNT " individuals"))
          (Thread/sleep delay))
          (recur)))))

(defn- request-population-pool!
  "request population pool from remote engine
  (blocking), resets! POPULATION-POOL"
  [hostname req-p verbose]
  (if verbose
    (utils/write-info (str "Requesting population from: "
                          hostname ":" req-p)))
  ;TODO: on failure to connect wait an retry
  (let [client-socket (Socket. hostname req-p)
        reader (io/reader client-socket)]
      (reset! POPULATION-POOL
        (filter valid-indiv?
          (map #(:indiv (read-string %))
                (line-seq reader))))))

(defn- async-persistent-server
  "start listening server,
  push individuals to incoming channel"
  [socket in-channel verbose?]
  (if verbose?
    (log/write-info "Starting persistent async server..."))
  ;continues on main thread
  (async/go-loop []
    (with-open [client-socket (.accept socket)]
      (let [line-from-sock (.readLine (io/reader client-socket))]
         ;verify that line can be placed in channel
         (if (and (not (nil? line-from-sock)) (not (empty? line-from-sock)))
             (async/>! in-channel line-from-sock)
             (if verbose?
               (log/write-warning "Ingress server read empty line")))))
    (recur)))

(defn- out-channel-worker
  "start a distribution worker
  waits on outgoing channel, writes
  individuals to configured socket"
  [engine-hostname port out-channel verbose?]
  (if verbose?
    (log/write-info "Starting outgoing channel worker..."))
  (async/go-loop []
    (let [indiv (async/<! out-channel)
          client-socket (Socket. engine-hostname port)
          writer (io/writer client-socket)]
        (if verbose?
          (log/write-info
            (str "Finished simulation cycle on individual: " (:uuid indiv))))
        (.write writer (str (pr-str indiv) "\n"))
        (.flush writer)
        (.close client-socket))
    (recur)))

(defn- in-channel-worker
  "start channel worker with starting state
  uses eval hook to evaluate individual and
  push to outgoing chan"
  [eval-fn in-channel out-channel engine-hostname pop-req-p verbose?]
  (if verbose?
    (log/write-info "Starting incoming channel worker..."))
  (async/go-loop []
    (try
      (let [indiv-packet (read-string (async/<! in-channel))
            ;unpack individual
            indiv (:indiv indiv-packet)
            current-cycle (:cycle indiv-packet)]
          ;validate individual before starting simulation
          (if (valid-indiv? indiv)
          ;check if current cycle has changed
            (do
              (if (not (= current-cycle @CURRENT-CYCLE))
                 (do
                   (if verbose?
                     (log/write-info "Detected new cycle, clearing opponent pool"))
                   (reset! POPULATION-POOL (list))
                   (reset! INDIV-COUNT 0)
                   (reset! CURRENT-CYCLE current-cycle)))

              (swap! INDIV-COUNT inc)
              ;if node hasn't requested opponents for this cycle,
              ; request from engine host (block)
              (if (empty? @POPULATION-POOL)
                (request-opponent-pool!
                  engine-hostname pop-req-p))

              (if verbose?
                (log/write-info (str "Running simulations on individual "
                                      (:uuid indiv) " against " (count @POPULATION-POOL)
                                      " opponents")))
              (async/>! out-channel
                ;create return map
                ;TODO: using eval function (and potentially rest of pop)
                ))
             (if verbose?
               (log/write-error (str "Received invalid individual: " indiv)))))
      (catch Exception e
        (log/write-error "In channel worked failed to evaluate individual on opponent pool (Exception)")
        (.printStackTrace e)))
    (recur)))

(defn start-eval-worker
  "Starts evaluation services, takes
  a fn that takes an individual and optionally
  the rest of the population (depends on config)
  Configuration describes various port/hostname
  params and async channel buffer sizes, etc..."
  [eval-hook config]
  (let [in-channel (async/chan (:buffer-size config))
        out-channel (async/chan (:buffer-size config))
        verbose? (:verbose? config)]
      (do
        ;start status task thread (logging process)
        (if (and verbose? (:status-task? config))
            (status-task (:status-task-delay config)))
        ;start in-channel worker
        (in-channel-worker
          eval-hook in-channel out-channel
          (:engine-hostname config) (:pop-req-p config)
          verbose?)
        ;start out-channel worker
        (out-channel-worker
          (:engine-hostname config)
          (:indiv-egress-p config)
          out-channel verbose?)
        ;start listening server
        (async-persistent-server
          (ServerSocket. (:indiv-ingress-p config))
          in-channel verbose?))))
