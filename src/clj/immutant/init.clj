(ns immutant.init
  (:require [immutant.web :as web]
            [knakk.svamp.routes :refer [approutes]]
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

(web/start "/" (-> approutes
                   (wrap-resource "public")
                   wrap-file-info))
