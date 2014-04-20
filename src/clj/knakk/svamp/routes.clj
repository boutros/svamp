(ns knakk.svamp.routes
  "Application HTTP routes."
  (:require [compojure.core :refer [GET POST PUT defroutes context]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [knakk.svamp.sparql :as sparql]
            [knakk.svamp.search :as search]
            [knakk.svamp.resources :as resources]
            [knakk.svamp.help :as help])
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
(defn page-help [] (io/resource "html/help.html"))

(def settings (atom (load-edn "settings.edn")))


;; Routing ====================================================================

(defroutes apiroutes
  (GET "/settings" [] (api-response (load-edn "settings.edn")))
  (GET "/resource-types" [] (api-response (resources/all-types)))
  (GET "/help" [] (api-response (help/data)))
  (POST "/help" [id params] (api-response (help/hooks id params)))
  (GET "/template" [template]
       (if-let [f (io/resource (str "resource-types/" template))]
         (api-response (select-keys (read-string (slurp f))
                                    [:rdf-type :label :desc :groups]))
         (api-response {:error (str "cannot find template file: " template)} 400)))
  (POST "/resource" [resource publish? template]
        (let [t (io/resource (str "resource-types/" template))
              res-fns (select-keys (load-string (slurp t))
                                   [:uri-fn :inner-rules :outer-rules
                                    :search-label :display-label])
              full-resource (merge resource res-fns)]
          (api-response
           (resources/create! full-resource publish? template))))
  (POST "/search" [q drafts? type]
        (let [t (if (= "Any type" type) false type)]
          (api-response
           (if drafts?
             (search/search-all q :type t)
             (search/search-public q :type t))))))

(defroutes approutes
  ;; API
  (context "/api" [] (-> apiroutes handler/api wrap-edn-params))

  ;; Web interface
  (GET "/resources" [] (page-resources))
  (GET "/resource-new" [] (page-new-resource))
  (GET "/settings"[] (page-settings))
  (GET "/help" [] (page-help))

  (route/not-found "Nothing here. Try elsewhere."))
