(ns user
  "hacking repl"
  (:require [schema.core :as s]
            [clojure.repl :refer :all]
            [clojure.java.io :as io]
            [clojure.xml :as xml]
            [knakk.svamp.settings :refer [settings]]
            [knakk.svamp.resources :as resources]
            [knakk.svamp.sparql :as sparql]
            [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.index :as esi]
            [clojurewerkz.elastisch.native.document :as esd]))

(defn read-lines [^String filename]
  (with-open [x (io/reader filename)]
    (vec (line-seq x))))

(defn gen-resource
  [type values]
  (let [template (->> (str "resource-types/" type) io/resource slurp load-string)]
    (assoc template :groups
     (into []
      (for [g (:groups template)]
       (assoc g :elements
        (into []
         (for [e (:elements g)
               :let [vt (:value-template e)]]
          (assoc e :values
            (for [v values
                  :when (= (:id e) (:id v))]
              (assoc vt :value (:value v))))))))))))

(defn english? [n]
  (= (get-in n [:attrs :xml:lang]) "en"))

(defn tag? [subj tag]
  (and (english? subj)
       (= tag (:tag subj))))

(defn filter-tag [content tag]
  (map (comp #(str % "@en") first :content) (filter #(tag? % tag) content)))

(defn filter-tag-link [content tag]
  (map (comp :rdf:resource :attrs )
       (filter #(= (:tag %) tag) content)))

(comment

;; Import subject scos vocabs

(let [nodes (:content (xml/parse (clojure.java.io/file "data/scot.rdf")))]
  (doseq [subject nodes
        :when (contains? (set (map (comp :rdf:resource :attrs) (:content subject)))
                         "http://www.w3.org/2004/02/skos/core#Concept")
        :let [id (->> subject :attrs :rdf:about (re-find #"(\d)+$") first)
              content (:content subject)
              broader (filter-tag-link content :skos:broader)
              narrower (filter-tag-link content :skos:narrower)
              related (filter-tag-link content :skos:related)
              exact-match (filter-tag-link content :skos:exactMatch)
              close-match (filter-tag-link content :skos:closeMatch)
              pref-labels (filter-tag content :skos:prefLabel)
              alt-labels (filter-tag content :skos:altLabel)
              hidden-labels (filter-tag content :skos:hiddenLabel)
              scope-note (filter-tag content :skos:scopeNote)]]
    (resources/create!
     (gen-resource "subject.clj"
                   (into [{:id :scotId :value id}]
                         (concat
                          (map #(hash-map :id :broader :value %) broader)
                          (map #(hash-map :id :narrower :value %) narrower)
                          (map #(hash-map :id :related :value %) related)
                          (map #(hash-map :id :exactMatch :value %) exact-match)
                          (map #(hash-map :id :closeMatch :value %) close-match)
                          (map #(hash-map :id :prefLabel :value %) pref-labels)
                          (map #(hash-map :id :altLabel :value %) alt-labels)
                          (map #(hash-map :id :hiddenLabel :value %) hidden-labels)
                          (map #(hash-map :id :note :value %) scope-note))))
     true "subject.clj")))

;; Importer Dewey 1000-numrene

(def dewey-all
  (map #(clojure.string/split % #"\s{2}") (read-lines "data/dewey_cleaned.txt")))

(doseq [[loc label] dewey-all]
  (resources/create! (gen-resource "dewey.clj" [{:id :location :value loc}
                                                {:id :label :value label}])
                                   true
                                   "dewey.clj"))
)


