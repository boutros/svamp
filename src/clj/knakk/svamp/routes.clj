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
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :body (pr-str data)})

(defn api-response-raw [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :body data})


;; Resources ==================================================================

(defn page-settings [] (io/resource "html/settings.html"))
(defn page-resources [] (io/resource "html/resources.html"))
(defn page-new-resource [] (io/resource "html/resource-new.html"))

(def settings (atom (load-edn "settings.edn")))
(defn rdf-types []
  ;; TODO error handling:
  ;;   edn/read-string will fail if input is not well-formed EDN
  (let [files (->> "rdf-types" io/resource io/file file-seq (filter #(.isFile %)))
        filenames (map #(.getName %) files)
        types (map (comp #(select-keys % [:label :desc])
                    edn/read-string slurp) files)]
    (vec (map #(assoc %1 :file %2) types filenames))))

(defn template [filename]
  (->> filename io/resource slurp edn/read))

;; Routing ====================================================================

(defroutes apiroutes
  (GET "/settings" [] (api-response (load-edn "settings.edn")))
  (GET "/rdf-types" [] (api-response (rdf-types)))
  (GET "/template" {{template :template} :params}
       (if-let [f (io/resource (str "rdf-types/" template))]
         (api-response-raw (slurp f))
         (api-response {:error (str "cannot find template file: " template)} 400))))

(defroutes approutes
  ;; API
  (context "/api" [] (handler/api apiroutes))

  ;; Web interface
  (GET "/resources" [] (page-resources))
  (GET "/resource-new" [] (page-new-resource))
  (GET "/settings"[] (page-settings))

  (route/not-found "Nothing here. Try elsewhere."))