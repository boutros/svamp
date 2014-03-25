(ns knakk.svamp.routes
  "http routes of the application"
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.io :refer [resource]]))

(defn page-settings [] (resource "html/settings.html"))
(defn page-resources [] (resource "html/resources.html"))

(defroutes approutes
  (GET "/resources" [] (page-resources))
  (GET "/settings"[] (page-settings))
  (route/not-found "Nothing here. Try elsewhere."))