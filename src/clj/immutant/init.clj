(ns immutant.init
  (:require [immutant.web :as web]
            [knakk.svamp.routes :refer [approutes]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]))

(web/start "/" (-> approutes
                   (wrap-resource "public")
                   wrap-file-info))
