(ns knakk.svamp.routes
  "http routes of the application"
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import [java.io.PushbackReader]))

;; Util functions
(defn load-edn [f]
  (with-open [rdr (-> (io/resource f)
                      io/reader
                      java.io.PushbackReader.)]
    (edn/read rdr)))

(defn api-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

;; Resources
(def settings (atom (load-edn "settings.edn")))
(defn page-settings [] (io/resource "html/settings.html"))
(defn page-resources [] (io/resource "html/resources.html"))

;; Routing
(defroutes approutes
  (GET "/api/settings" [] (api-response @settings))
  (GET "/resources" [] (page-resources))
  (GET "/settings"[] (page-settings))
  (route/not-found "Nothing here. Try elsewhere."))