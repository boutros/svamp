{
 :rdf-type "http://www.w3.org/2004/02/skos/core#Concept"
 :index-type "subject"
 :label "Subject"
 :desc "an area of study, or a concept"
 :uri-fn (fn [{:keys [label]}]
           (str "<http://data.svamp.no/subject/" (->> label first :value) ">"))
 :inner-rules []
 :outer-rules []
 :search-label (fn [{:keys [label]}]
                 (->> label first :value))
 :display-label (fn [{:keys [label]}]
                 (->> label first :value))
 :groups [ {:title "Description"
            :elements
            [{:id :pref-label
              :unique true
              :label "Preferred label"
              :desc "NOTE: The first label entered will be used for generating the URI"
              :repeatable true
              :required true
              :value-template {:value "" :predicate "skos:prefLabel" :type :string}}
             {:id :alt-label
              :label "Alternate label"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "skos:altLabel" :type :string}}
             {:id :definition
              :label "Definition"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "skos:definition" :type :string}}
             {:id :definition
              :label "Note"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "skos:note" :type :string}}]}
           {:title "Relations"
            :elements
            [{:id :narrower
              :label "Narrower subject"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "skos:narrower" :type :uri}}
             {:id :borader
              :label "Broader subject"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "skos:broader" :type :uri}}
             {:id :related
              :label "Related subject"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "skos:related" :type :uri}}]}]
 }
