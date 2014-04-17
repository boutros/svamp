(ns knakk.svamp.resources
  "Functions for creating, editing and deleting metadata resources."
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [knakk.svamp.settings :refer [settings]]
            [knakk.svamp.sparql :as sparql]
            [clj-time.core :refer [now]]))


;; Constants ==================================================================

;; Private helper functions ===================================================

(defn- no-value? [e] (empty? (:value e)))

(defn- extract-id-vals
  [elements]
  (into {}
    (for [el elements]
      [(:id el) (remove no-value? (:values el))])))


;(defn- prefix-or-err
;  [namespaces prefix]
;  (if (contains? namespaces prefix)
;    (get-in namespaces [prefix :uri])
;    (throw
;      (IllegalArgumentException.
;        (str "settings.edn: missing namespace prefix \"" (name prefix) "\"")))))

(defn- uri [s]
  (str "<" s ">"))

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

(defn- extract-id-pred
  [elements]
  (into {}
    (for [el elements
          :let [t (name (get-in el [:value-template :type]))]]
      [(get-in el [:value-template :predicate]) (:id el) ])))

;; Rules helper functions =====================================================

;; (defn trim-lang-tag [s])

;; (defn urlize [s])

;; Public API =================================================================


(defn rdf-types []
  ;; TODO error handling:
  ;;   read-string will fail if input is not well-formed clj syntax
  (let [files (->> "rdf-types" io/resource io/file file-seq (filter #(.isFile %)))
        filenames (map #(.getName %) files)
        types (map (comp #(select-keys % [:label :desc :index-type])
                    read-string slurp) files)]
    (vec (map #(assoc %1 :file %2) types filenames))))


(defn build-query
  "Takes a resource map and builds the SPARQL query to be inserted.

  Returns the query string."
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
        values (conj (reduce into [] (map (fn [[k v]] v) r))
                     {:predicate "a" :value "svamp://internal/class/Resource" :type :uri}
                     {:predicate "<svamp://internal/resource/searchLabel>" :value search-label :type :string}
                     {:predicate "<svamp://internal/resource/displayLabel>" :value display-label :type :string}
                     {:predicate "<svamp://internal/resource/template>" :value template :type :no-tag-string}
                     {:predicate "<svamp://internal/resource/created>" :value (now) :type :date}
                     {:predicate "<svamp://internal/resource/updated>" :value (now) :type :date}
                     ;(if publish?
                     ;  {:predicate "<svamp://internal/resource/published>" :value (now) :type :date})
                     )
        pred-vals (map (fn [v] (str (:predicate v) " " (literal v)) ) values)
        inner (clojure.string/join " . " (map #(% r2) (:inner-rules resource)))]
    (str "INSERT INTO GRAPH " g " { "
         id " a " (uri (:rdf-type resource)) " ; "
         (clojure.string/join " ; " pred-vals)
         " . "
         inner
         "}")))


