(ns io.jackhay.gpdistribute.impl.engine
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [io.jackhay.gpdistribute.impl.utils :as utils]
            [io.jackhay.gpdistribute.impl.config :as config])
  (:import [java.net ServerSocket SocketException Socket InetAddress]))

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

(defn distribute-population
  "takes population and distribution configuration
  and distributes according to config"
  [pop dist-config]
  )
