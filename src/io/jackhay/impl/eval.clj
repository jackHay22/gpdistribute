(ns io.jackhay.gpdistribute.impl.eval
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [io.jackhay.gpdistribute.impl.utils :as utils])
  (:import [java.net ServerSocket SocketException Socket InetAddress]))

(defn start-eval-worker
  "Starts evaluation services, takes
  a fn that takes an individual and optionally
  the rest of the population (depends on config)
  Configuration describes various port/hostname
  params and async channel buffer sizes, etc..."
  [eval-hook config]
  )
