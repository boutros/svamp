(ns knakk.svamp.sparql
  "SPARQL operations."
  (:require [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :as json]
            [knakk.svamp.settings :refer [settings]]
            [clj-http.client :as http]))



;; Constants ==================================================================

(def http-options
  {:throw-exceptions true :debug false :debug-body true
   :socket-timeout (get-in @settings [:rdf-store :read-timeout])
   :conn-timeout (get-in @settings [:rdf-store :open-timeout])})


;; Private helper functions ===================================================

(defn- do-query
  "Perform SPARQL query <q>.

  Returns the clj-http response map. Throws exceptions on network errors and
  http status codes other than 2xx & 3xx."
  [q]
  (let [auth-method (get-in @settings [:rdf-store :auth-method])
        auth-credentials [(get-in @settings [:rdf-store :username])
                          (get-in @settings [:rdf-store :username])]]
    (http/get
     (get-in @settings [:rdf-store :endpoint])
     (merge http-options
            {:query-params
             {"query" q "format" "application/sparql-results+json"}}
            (condp = auth-method
              :none {}
              :basic {:basic-auth auth-credentials}
              :digest {:digest-auth auth-credentials})))))

(defn- uri [s] (str "<" s ">"))

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

(defn insert
  "Perform a SPARQL INSERT query <q> against the application's RDF-store.

  Returns: {:results <nil> or <successmessage>,
            :error <nil> or <error string>
            :error-details <nil> or <errordetails>}"
  [q]
  (let [res {:result nil :error nil :error-details nil}]
    (try
      (assoc res :result
        (->> (do-query q) :body json/parse-string keywordize-keys :results
             :bindings first :callret-0 :value))
      (catch Exception e
        (assoc res :error (.getMessage e)
                   :error-details (some->> e .getData :object :body))))))

(defn delete
  "Perform a SPARQL DELETE query <q> against the application's RDF-store.

  Returns: {:results <nil> or <successmessage>,
            :error <nil> or <error string>
            :error-details <nil> or <errordetails>}"
  [q]
  (let [res {:result nil :error nil :error-details nil}]
    (try
      (let [msg (->> (do-query q) :body json/parse-string keywordize-keys :results
             :bindings first :callret-0 :value)]
      (if (re-find #"nothing to do" msg)
        (assoc res :error "nothing delted; 0 triples matched the query")
        (assoc res :result msg)))
      (catch Exception e
        (assoc res :error (.getMessage e)
                   :error-details (some->> e .getData :object :body))))))

(defn select-resource [resource]
  (select
   (str "select * where { " (uri resource) " ?p ?o }")))

(defn solutions
  "Returns the solution maps from a sparql/json response."
  [response]
  (for [solution (->> response :results :bindings)]
    (into {}
          (for [[k v] solution]
            [k (:value v)]))))

