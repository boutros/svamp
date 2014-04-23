(ns knakk.svamp.resources
  "Functions for creating, editing and deleting metadata resources."
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [immutant.messaging :as msg]
            [knakk.svamp.settings :refer [settings]]
            [knakk.svamp.sparql :as sparql]
            [taoensso.timbre :as timbre :refer [info error]]
            [clj-time.core :refer [now]]))

;; Private helper functions ===================================================

(defn- no-value? [e] (empty? (:value e)))

(defn- extract-id-vals
  [elements]
  (into {}
    (for [el elements]
      [(:id el) (remove no-value? (:values el))])))

(defn- uri [s] (str "<" s ">"))

(defn- clean-uri [s] (subs s 1 (dec (count s))))


(defn- literal [v]
  (condp = (:type v)
    :string (if-let [m (re-find #"@[a-zA-Z-]{2,5}$" (:value v))]
              (str "\"" (subs (:value v) 0 (- (count (:value v)) (count m))) "\"" m)
              (str  "\"" (:value v) "\"@" (get-in @settings [:data :default-lang])))
    :no-tag-string (str "\"" (:value v) "\"")
    :uri (uri (:value v))
    :integer (:value v)
    :float (:value v)
    :date (str "\"" (:value v) "\"^^xsd:dateTime")))

(defn- parse-or-nil [file]
  "Slurps and parses a clojure file.

  Returns the parsed clojure data or nil if the file cannot be parsed.
  An error will be logged."
  (try
    (read-string (slurp file))
  (catch Exception e
    (error (str "failed to parse file '" file "': " (.getMessage e))))))

(defn insert-query
  "Takes a resource map and builds the SPARQL query to be inserted.

  Returns a map of the query string and the uri (id):
  {:query '...' :uri 'http://...'}"
  [resource publish? template]
  (let [r (into {}
                (map
                  (comp #(extract-id-vals %) :elements)
                  (:groups resource)))
        r2 (assoc r :uri-fn (:uri-fn resource)
             :search-label (:search-label resource)
             :display-label (:display-label resource))
        g (uri
            (get-in @settings [:data (if publish?
                                      :default-graph
                                      :drafts-graph)]))
        id ((:uri-fn r2) r2)
        search-label ((:search-label r2) r2)
        display-label ((:display-label r2) r2)
        values (into
                (reduce into [] (map (fn [[k v]] v) r))
                 (remove nil?
                         [{:predicate "a" :value "svamp://internal/class/Resource" :type :uri}
                          {:predicate "<svamp://internal/resource/searchLabel>" :value search-label :type :string}
                          {:predicate "<svamp://internal/resource/displayLabel>" :value display-label :type :string}
                          {:predicate "<svamp://internal/resource/template>" :value template :type :no-tag-string}
                          {:predicate "<svamp://internal/resource/created>" :value (now) :type :date}
                          {:predicate "<svamp://internal/resource/updated>" :value (now) :type :date}
                          (when publish? {:predicate "<svamp://internal/resource/published>" :value (now) :type :date})
                          ]))
        pred-vals (map (fn [v] (str (:predicate v) " " (literal v))) values)
        inner (clojure.string/join " . " (map #(% r2) (:inner-rules resource)))
        rules (into [] (map #(% g id) (:outer-rules resource)))]
    {:query
     (str "INSERT INTO GRAPH " g " { "
          id " a " (uri (:rdf-type resource)) " ; "
          (clojure.string/join " ; " pred-vals)
          " . " inner "}")
     :uri id
     :rules rules}))

(defn- delete-query
  [resource published?]
  (let [g (uri (get-in @settings [:data
                                  (if published?
                                      :default-graph
                                      :drafts-graph)]))]
    (str "WITH " g " DELETE { ?s ?p ?o } WHERE { ?s ?p ?o . "
         (uri resource) " ?p ?o . }")))

;; Public API =================================================================

(defn all-types
  "Returns a vector of the resource types, where each type is a map with the
  keys: :file, :index-type, :desc & :label"
  []
  (let [files (->> "resource-types" io/resource io/file file-seq (filter #(.isFile %)))
        filenames (map #(.getName %) files)
        types (map (comp #(select-keys % [:label :desc :index-type])
                    parse-or-nil) files)]
    (vec (map #(assoc %1 :file %2) types filenames))))

(defn all-by-type
  "Returns a vector of all URIs for a given type."
  [type-file]
  (let [r (sparql/select
           (str "select ?uri where "
                "{ ?uri <svamp://internal/resource/template> \""
                type-file "\" . }"))]
    (->> (:results r) sparql/bindings :uri (into []))))

(defn create! [resource publish? template]
  (let [query (insert-query resource publish? template)
        res (sparql/insert (:query query))
        rules (:rules query)]
    (if (:error res)
      (error (str "failed to create resource: " (:error res)))
      (do
        (doseq [q rules] ;; TODO error handling etc. Move to own fn? This is temporary!
          (info (sparql/insert q)))
        (info (str "resource created: " (:uri query)))
        (msg/publish "/queue/indexing" (clean-uri (:uri query)))))
    res))

(defn delete! [resource published?]
  (let [res (sparql/delete (delete-query resource published?))]
    (if (:error res)
      (error (str "failed to delete resource " (uri resource) ": " (:error res)))
      (info (str "resource deleted: " (uri resource))))
    (println "Remove from index queue") ;; TODO
    res))

