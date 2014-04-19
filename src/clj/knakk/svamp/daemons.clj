(ns knakk.svamp.daemons
  "Long-running application processes"
  (:require [immutant.daemons :as daemon]
            [immutant.messaging :as msg]
            [knakk.svamp.index :as index]
            [taoensso.timbre :refer [info]]))

;; Controls the state of the daemon(s)
(def done (atom false))

;; QueueWorker daemon implementation
(defrecord QueueWorker [queue worker-fn]
  daemon/Daemon
  (start [_]
    (info (str "starting message processor for " queue))
    (reset! done false)
    ;(msg/with-connection {} ;;TODO create immutant minimal failure repo for with-connection
      (loop []
        (when-not @done
          (when-let [uri (msg/receive queue)]
            (worker-fn uri)
            (info "indexed " uri))
          (recur))));)
  (stop [_]
    (info (str "stopping message processor for " queue))
    (reset! done true)))

(defn start-all! []
 (do
   (daemon/create "index-processor"
                  (QueueWorker. index/queue index/index-resource!)
                  :singleton true))
 )
