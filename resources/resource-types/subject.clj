{
 :rdf-type "http://www.w3.org/2004/02/skos/core#Concept"
 :index-type "subject"
 :label "Subject"
 :desc "an area of study, or a concept"
 :uri-fn (fn [{:keys [scot-id]}]
           (str "<http://vocabulary.curriculum.edu.au/scot/" (->> scot-id first :value) ">"))
 :inner-rules []
 :outer-rules []
 :search-label (fn [{:keys [pref-label alt-label hidden-label]}]
                 (clojure.string/join " " (into [(->> pref-label first :value)]
                                                 (map :value (into hidden-label alt-label)))))
 :display-label (fn [{:keys [pref-label]}]
                 (->> pref-label first :value))
 :groups [{:title "Identification"
           :elements
           [{:id :scot-id
             :desc "A number, used for generating the URI. Just pick one!"
             :unique true
             :label "ID"
             :repeatable false
             :required true
             :value-template {:value "" :predicate "<svamp://internal/temporary>" :type :integer}}]}
          {:title "Description"
            :elements
            [{:id :pref-label
              :label "Preferred label"
              :repeatable true
              :required true
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#prefLabel>" :type :string}}
             {:id :alt-label
              :label "Alternate label"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#altLabel>" :type :string}}
             {:id :hidden-label
              :label "Hidden label"
              :desc "E.g. miss-spelled and deprecated labels"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#hiddenLabel>" :type :string}}
             {:id :definition
              :label "Note"
              :desc "subject scope"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#scopeNote>" :type :string}}]}
           {:title "Relations (internal)"
            :elements
            [{:id :narrower
              :label "Narrower subject"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#narrower>" :type :uri}}
             {:id :borader
              :label "Broader subject"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#broader>" :type :uri}}
             {:id :related
              :label "Related subject"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#related>" :type :uri}}]}
           {:title "Links (external)"
            :elements
            [{:id :exact-match
              :label "Excact match"
              :desc "a link which maps exactly to this subject"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#exactMatch>" :type :string}}
             {:id :close-match
              :label "Close match"
              :desc "a link which maps roughly to this subject"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#closeMatch>" :type :string}}]}]
 }
