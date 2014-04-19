(ns immutant.init
  (:require [immutant.web :as web]
            [immutant.messaging :as msg]
            [knakk.svamp.daemons :as daemons]
            [knakk.svamp.routes :refer [approutes]]
            [knakk.svamp.index :as index]
            [knakk.svamp.settings :refer [settings]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [taoensso.timbre :as timbre :refer [info]]
            [clojurewerkz.elastisch.native :as es]))

;; set up logging
(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:shared-appender-config :spit-filename] "./server.log")

(info "starting up svamp!")

;; connect to elasticsearch
(es/connect! [["127.0.0.1" 9300]] {"cluster.name" "svamp"})

;; start indexing queue
(msg/start index/queue)

;; start all deamons
(daemons/start-all!)

(web/start "/" (-> approutes
                   (wrap-resource "public")
                   wrap-file-info))
