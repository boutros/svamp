(ns knakk.svamp.routes
  "Application HTTP routes."
  (:require [compojure.core :refer [GET defroutes context]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import [java.io.PushbackReader]))


;; Utility functions ==========================================================

(defn load-edn
  "Load and parse an edn-file."
  [filename]
  (with-open [rdr (-> (io/resource filename)
                      io/reader
                      java.io.PushbackReader.)]
    (edn/read rdr)))

(defn api-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})


;; Resources ==================================================================

(defn page-settings [] (io/resource "html/settings.html"))
(defn page-resources [] (io/resource "html/resources.html"))
(defn page-new-resource [type] (io/resource "html/resource-new.html"))

(def settings (atom (load-edn "settings.edn")))
(def rdf-types
  ;; TODO error handling:
  ;;   edn/read-string will fail if input is not well-formed EDN
  (let [files (->> "rdf-types"
                    io/resource
                    io/file
                    file-seq
                    (filter #(.isFile %)))]
    (vec (map (comp #(select-keys % [:label :desc :rdf-type])
                    edn/read-string slurp) files))))



;; Routing ====================================================================

(defroutes approutes
  ;; API
  (GET "/api/settings" [] (api-response @settings))
  (GET "/api/rdf-types" [] (api-response
                             (let [files (->> "rdf-types" io/resource io/file
                                              file-seq (filter #(.isFile %)))]
                               (vec (map (comp #(select-keys % [:label :desc :rdf-type])
                                               edn/read-string slurp) files)))))

  ;; Web interface
  (GET "/resources" [] (page-resources))
  (GET "/resource-new" [rdf-type] (page-new-resource rdf-type))
  (GET "/settings"[] (page-settings))

  (route/not-found "Nothing here. Try elsewhere."))