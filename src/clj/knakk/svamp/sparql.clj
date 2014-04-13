(ns knakk.svamp.sparql
  "SPARQL operations."
  (:require [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :as json]
            [knakk.svamp.settings :refer [settings]]
            [clj-http.client :as http]))


;; Constants ==================================================================

(def http-options
  {:throw-exceptions true :debug false :debug-body false
   :socket-timeout (get-in @settings [:rdf-store :read-timeout])
   :conn-timeout (get-in @settings [:rdf-store :open-timeout])})


;; Private helper functions ===================================================

(defn- do-query
  "Perform SPARQL query <q>.

  Returns the clj-http response map. Throws exceptions on network errors and
  http status codes other than 2xx & 3xx."
  [q]
  (http/get
   (get-in @settings [:rdf-store :endpoint])
   (merge http-options
          {:query-params
           {"query" q "format" "application/sparql-results+json"}})))


;; Public API =================================================================

(defn select
  "Perform a SPARQL SELECT query <q> against the application's RDF-store.

  Returns: {:results <nil> or <sparql-results map>,
            :error <nil> or <error string>}"
  [q]
  (let [res {:results nil :error nil}]
    (try
      (assoc res :results
        (->> (do-query q) :body json/parse-string keywordize-keys))
      (catch Exception e
        (assoc res :error (.getMessage e))))))

; (defn insert
;   "Perform a SPARQL INSERT query 'q' against the application's RDF-store.
;   Returns: {:results nil/false/true, :error nil/error string"
;   [q]
;   q)

