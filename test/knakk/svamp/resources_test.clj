(ns knakk.svamp.resources-test
  (:require [knakk.svamp.resources :refer :all]
            [knakk.svamp.settings :refer [settings]]
            [expectations :refer :all] ))

;; Test helpers ===============================================================

(defn strip-newlines
  "Transform newlines and multiple whitespace chars into one space char."
  [s]
  (clojure.string/replace s #"[\s\n]+" " "))

;; Test Data  =================================================================

(def test-resource-1
  {:rdf-type "http://dewey.info/Class"
   :uri-fn (fn [{:keys [location]}]
             (str "<http://dewey.info/class/" (->> location first :value) ">"))
   ;; inner-rules are inserted into the resource query
   :inner-rules [(fn [{:keys [location uri-fn] :as all}]
                   (let [l (->> location first :value)]
                     (str (uri-fn all) " a skos:Concept")))
                 (fn [{:keys [location uri-fn] :as all}]
                   (let [l (->> location first :value)]
                     (when-let [broader-dewey
                                (cond
                                 (re-find #"\.\d{2,}" l) (subs l 0 (dec (count l)))
                                 (re-find #"\.\d{1}$" l) (subs l 0 (- (count l) 2))
                                 (re-find #"[1-9]$" l) (str (subs l 0 (dec (count l))) "0")
                                 (re-find #"[1-]{2}0$" l) (str (first l) "00"))]
                       (str (uri-fn all) " skos:broader " (uri-fn {:location [{:value broader-dewey}]}) " . "
                            (uri-fn {:location [{:value broader-dewey}]}) " skos:narrower " (uri-fn all) " . "))))]
   ;; outer-rules are complete, independent queries run consequetly after the resource query
   :outer-rules []
   :search-label (fn [{:keys [location label]}]
                   (str (->> location first :value) " "
                        (->> label first :value)))
   :display-label (fn [{:keys [location label]}]
                   (str (->> location first :value) " "
                        (->> label first :value)))
   :groups [ {:elements
              [{:id :location
                :repeatable false
                :values [{:value "641.5" :predicate "skos:notation" :type :float}]}
               {:id :label
                :repeatable true
                :values [{:value "Kokebøker@no" :predicate "skos:prefLabel" :type :string}
                         {:value "Cookbooks@en" :predicate "skos:prefLabel" :type :string}]}
               {:id :subject
                :repeatable true
                :values [{:value "" :predicate "dc:subject" :type :uri}]}
               {:id :narrower
                :repeatable true
                :values[{:value "" :predicate "skos:narrower" :type :uri}]}
               {:id :broader
                :repeatable true
                :values [{:value "" :predicate "skos:broader" :type :uri}]}]}]})

(def query-wanted-1
  "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
  INSERT INTO GRAPH <http://data.svamp.no>
    {
     <http://dewey.info/class/641.5> a <http://dewey.info/Class> ;
                                     skos:notation 641.5 ;
                                     skos:prefLabel \"Kokebøker\"@no ;
                                     skos:prefLabel \"Cookbooks\"@en ;
                                     a <svamp://internal/class/Resource> ;
                                     <svamp://internal/resource/searchLabel> \"641.5 Kokebøker\"@no ;
                                     <svamp://internal/resource/displayLabel> \"641.5 Kokebøker\"@no ;
                                     <svamp://internal/resource/template> \"dewey.clj\" .
      <http://dewey.info/class/641.5> a skos:Concept .
      <http://dewey.info/class/641.5> skos:broader <http://dewey.info/class/641> .
      <http://dewey.info/class/641> skos:narrower <http://dewey.info/class/641.5> .
     }")


;; Tests  =====================================================================

(expect (strip-newlines query-wanted-1)
        (build-query test-resource-1 false "dewey.clj"))
