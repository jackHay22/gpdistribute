(ns io.jackhay.gpdistribute.impl.engine
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [io.jackhay.gpdistribute.impl.utils :as utils]
            [io.jackhay.gpdistribute.impl.config :as config])
  (:import [java.net ServerSocket SocketException Socket InetAddress])
  (:import java.io.PrintWriter))

(def CURRENT-CYCLE (atom -1))

;prevent port collisions from trying to restart
(def INCOMING-WORKER-STARTED? (atom false))
(def OPP-POOL-WORKER-STARTED? (atom false))
(def DIST-SERVER-STARTED? (atom false))

(def POPULATION-POOL (atom nil))

(def PENDING-DIST-CHAN (async/chan 500))
(def COMPLETED-CHAN (async/chan 500))

(defrecord DistConfiguration
  ;see io.jackhay.gpdistribute.impl.config for a detailed desc. of options
  [stateful? verbose?
   status-task? status-task-delay
   buffer-size
   engine-hostname
   eval-hostname
   indiv-ingress-p
   indiv-egress-p
   pop-req-p])

(defn- make-packet
  "make a clojure map for distributing
  individuals to eval workers"
  [indiv request-pop]
  {:indiv indiv :cycle @CURRENT-CYCLE :req-pop? request-pop})

(defn get-default-config
  "Returns default configuration merged
  with arg overrides"
  [args]
  (merge
    (DistConfiguration.
      config/STATEFUL
      config/VERBOSE
      config/STATUS-TASK
      config/STATUS-DELAY
      config/BUFFER-SIZE
      config/ENGINE-HOSTNAME
      config/EVAL-HOSTNAME
      config/INDIV-INGRESS-P
      config/INDIV-EGRESS-P
      config/POP-REQ-P)
    args))

(defn- distribution-worker
  "take a socket and an individual and send"
  [host port stateful? verbose?]
  (reset! DIST-SERVER-STARTED? true)
  (if verbose?
    (utils/log "Starting distribution worker..."))
  (async/go-loop []
    (let [indiv (async/<!! PENDING-DIST-CHAN)
          client-socket (Socket. host port)]
      (with-open [writer (io/writer client-socket)]
        (.write writer (str (pr-str (make-packet indiv stateful?)) "\n"))
        (.flush writer))
        (recur))))

(defn- pop-pool-worker
  "start a thread that servers population pool requests"
  [port verbose?]
  (reset! OPP-POOL-WORKER-STARTED? true)
  (if verbose?
    (utils/log "Starting opponent pool worker..."))
  (future
    (let [socket (ServerSocket. port)]
      (.setSoTimeout socket 0)
      (loop []
        (let [client-socket (.accept socket)
              writer (io/writer client-socket)]
            (try
              (doall (map
                #(.write writer (str (pr-str %) "\n"))
                      (map (fn [p] (make-packet p false))
                          @POPULATION-POOL)))
              (catch Exception e
                (.printStackTrace e)))
            (.flush writer)
            (.close client-socket))
      (recur)))))

(defn- incoming-socket-worker
  "start a listener for completed individuals"
  [port verbose?]
  ;prevent restart collision
  (reset! INCOMING-WORKER-STARTED? true)
  (if verbose?
    (utils/log "Starting incoming socket worker..."))
  (let [socket (ServerSocket. port)]
    ;infinite timeout
    (.setSoTimeout socket 0)
    (async/go-loop []
      (let [client-socket (.accept socket)
            ind (read-string (try
                  (.readLine (io/reader client-socket))
                (catch SocketException e
                  nil)))]
          (if (and (not (= ind nil)) (record? ind))
            (async/>! COMPLETED-CHAN ind)
            (if verbose?
              (utils/log (str "ERROR: failed to ingest individual: " ind))))
          (recur)))))

(defn get-distribution-fn
  "takes distribution configuration
  and returns dist hook fn"
  [config]
  (let [verbose? (:verbose? config)]
      ;start services (if not yet started)
      (if (and (:stateful? config)
               (not @OPP-POOL-WORKER-STARTED?))
        (pop-pool-worker (:pop-pool-req-p config) verbose?)
        (if verbose?
          (utils/log "Already started population pool worker")))

      ;server worker that sends individuals out that arrive on outgoing channel
      (if (not @DIST-SERVER-STARTED?)
        (distribution-worker (:host config) (:indiv-ingress-p config)
                             (:stateful? config) verbose?)
        (if verbose?
          (utils/log "Already started distribution server")))

      ;task that listens for incoming individuals
      (if (not @INCOMING-WORKER-STARTED?)
        (incoming-socket-worker (:indiv-egress-p config) verbose?)
        (if verbose?
          (utils/log "Already started incoming server worker")))

      ;return eval hook
      (fn [individual]
        ;push indiv to pending channel
        (async/go
          (async/>! PENDING-DIST-CHAN indiv))
        ;take an individual from the completed channel
        ;(blocking wait)
        (async/<!! COMPLETED-CHAN))))
