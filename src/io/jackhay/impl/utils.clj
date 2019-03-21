(ns io.jackhay.gpdistribute.impl.utils
  (:import java.text.SimpleDateFormat)
  (:import java.util.Date))

(defn- get-timestamp
   []
   (.format (SimpleDateFormat. "'['MM-dd-yyyy HH:mm.ss']'") (Date.)))

(defn- write-log
  ([msg] (println "gp-distribute" (get-timestamp) msg))
  ([tag msg] (write-log (str tag ": " msg))))

(defn write-error
  [msg]
  (write-log "ERROR" msg))

(defn write-info
  [msg]
  (write-log "INFO" msg))
