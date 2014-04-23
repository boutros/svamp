{
 :rdf-type "http://www.w3.org/2004/02/skos/core#Concept"
 :index-type "subject"
 :index-mappings { "<http://www.w3.org/2004/02/skos/core#narrower>" {"narrower" {:type "string" :index "not_analyzed"}}
                   "<http://www.w3.org/2004/02/skos/core#broader>" {"broader" {:type "string" :index "not_analyzed"}}
                   "<http://www.w3.org/2004/02/skos/core#prefLabel>" {"prefLabel" {:type "string"}}
                   "<http://www.w3.org/2004/02/skos/core#altLabel>" {"altLabel" {:type "string"}}
                   "<http://www.w3.org/2004/02/skos/core#hiddenLabel>" {"hiddenLabel" {:type "string"}}
                   "<http://www.w3.org/2004/02/skos/core#scopeNote>" {"scopeNote" {:type "string"}}
                   "<http://www.w3.org/2004/02/skos/core#exactMatch>" {"exactMatch" {:type "string" :index "not_analyzed"}}
                   "<http://www.w3.org/2004/02/skos/core#closeMatch>" {"closeMatch" {:type "string" :index "not_analyzed"}}}
 :label "Subject"
 :desc "an area of study, or a concept"
 :uri-fn (fn [{:keys [scotId]}]
           (str "<http://vocabulary.curriculum.edu.au/scot/" (->> scotId first :value) ">"))
 :inner-rules []
 :outer-rules [(fn [graph uri]
                 (str "WITH " graph "
                        DELETE { " uri " <svamp://internal/resource/displayLabel> ?displayLabel }
                        WHERE {
                               " uri " <svamp://internal/resource/displayLabel> ?displayLabel ;
                                 <svamp://internal/resource/template> \"subject.clj\" .
                             }"))
               (fn [graph uri]
                 (str "INSERT INTO " graph "
                          { " uri "<svamp://internal/resource/displayLabel> `bif:concat(if(bound(?broaderLabel), bif:concat(str(?broaderLabel), " > "), ""), str(?prefLabel))` }
                       WHERE {
                              " uri " <http://www.w3.org/2004/02/skos/core#prefLabel> ?prefLabel ;
                                      <svamp://internal/resource/template> \"subject.clj\"
                              OPTIONAL { " uri " <http://www.w3.org/2004/02/skos/core#broader> ?b .
                                        ?b <http://www.w3.org/2004/02/skos/core#prefLabel> ?broaderLabel }
                                       }"))]
 :search-label (fn [{:keys [prefLabel altLabel hiddenLabel]}]
                 (let [strip-tag (fn [s] (last (re-find #"(.*)@[a-zA-Z-]{2,5}$" s)))]
                   (clojure.string/join " " (into [(->> prefLabel first :value strip-tag)]
                                                   (map (comp strip-tag :value) (into hiddenLabel altLabel))))))
 :display-label (fn [{:keys [prefLabel]}]
                 (->> prefLabel first :value))
 :groups [{:title "Identification"
           :elements
           [{:id :scotId
             :desc "A number, used for generating the URI. Just pick one!"
             :unique true
             :label "ID"
             :repeatable false
             :required true
             :value-template {:value "" :predicate "<svamp://internal/temporary>" :type :integer}}]}
          {:title "Description"
            :elements
            [{:id :prefLabel
              :label "Preferred label"
              :repeatable true
              :required true
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#prefLabel>" :type :string}}
             {:id :altLabel
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
             {:id :note
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
             {:id :broader
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
            [{:id :exactMatch
              :label "Excact match"
              :desc "a link which maps exactly to this subject"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#exactMatch>" :type :string}}
             {:id :closeMatch
              :label "Close match"
              :desc "a link which maps roughly to this subject"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#closeMatch>" :type :string}}]}]
 }
