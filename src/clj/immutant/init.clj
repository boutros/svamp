(ns immutant.init
  (:require [immutant.web :as web]
            [knakk.svamp.routes :refer [approutes]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [taoensso.timbre :as timbre :refer [info]]))

;; set up logging
(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:shared-appender-config :spit-filename] "./server.log")

(info "starting up svamp!")

(web/start "/" (-> approutes
                   (wrap-resource "public")
                   wrap-file-info))
