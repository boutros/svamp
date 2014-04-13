(ns knakk.svamp.resources
  "Functions for creating, editing and deleting metadata resources."
  (:require [knakk.svamp.settings :refer [settings]]))


;; Constants ==================================================================

;; Private helper functions ===================================================

(defn- no-value? [e] (empty? (:value e)))

(defn- extract-id-vals
  [elements]
  (into {}
    (for [el elements]
      [(:id el) (remove no-value? (:values el))])))

(defn- prefix-or-err
  [namespaces prefix]
  (if (contains? namespaces prefix)
    (get-in namespaces [prefix :uri])
    (throw
      (IllegalArgumentException.
        (str "settings.edn: missing namespace prefix \"" (name prefix) "\"")))))

(defn- uri [s]
  (str "<" s ">"))

(defn- infer-prefixes
  "Finds all occurences of the pattern 'prefix:name' in the query string.
  Returns the query string with the namespaces prefixed."
  ;; TODO clean up this function
  [s]
  (str
    (apply str
           (apply sorted-set
                      (let [cleaned-string (clojure.string/replace s #"(\\\".*\\\")" "")] ; "as not to match inside quoted strings
                      (for [[_ p] (re-seq #"(\b[a-zA-Z0-9]+):[a-zA-Z]" cleaned-string) :when (not-any? #(= p %) #{"mailto" "sql" "bif"})]
                        (str "PREFIX " p ": " (uri (prefix-or-err (@settings :ns) (keyword p))) " " )))))
    s))

(defn- literal [v]
  (condp = (:type v)
    :string (if-let [m (re-find #"@[a-zA-Z-]{2,5}$" (:value v))]
              (str "\"" (subs (:value v) 0 (- (count (:value v)) (count m))) "\"" m)
              (str  "\"" (:value v) "\""))
    :uri (uri (:value v))
    :integer (:value v)
    :float (:Value v)))


;; Public API =================================================================


(defn build-query
  "Takes a resource map and builds the SPARQL query to be inserted.

  Returns the query string.
  Throws IllegalArgumentException on missing namespace prefix in @settings."
  [resource draft?]
  (let [r (into {}
                (map
                  (comp #(extract-id-vals %) :elements)
                  (:groups resource)))
        r2 (assoc r :uri-fn (:uri-fn resource)
             :search-label (:search-label resource)
             :display-label (:display-label resource))
        g (uri
            (get-in @settings[:data (if draft?
                                      :drafts-graph
                                      :default-graph)]))
        id ((:uri-fn r2) r2)
        search-label ((:search-label r2) r2)
        display-label ((:display-label r2) r2)
        values (conj (reduce into [] (map (fn [[k v]] v) r))
                     {:predicate "svamp:searchLabel" :value search-label :type :string}
                     {:predicate "svamp:displayLabel" :value display-label :type :string})
        pred-vals (map (fn [v] (str (:predicate v) " " (literal v)) ) values)
        inner (clojure.string/join " . " (map #(% r2) (:inner-rules resource)))]
    (infer-prefixes
     (str "INSERT DATA { GRAPH " g " { "
         id " a " (uri (:rdf-type resource)) " ; "
         (clojure.string/join " ; " pred-vals)
         " . "
         inner
         "} }"))))
