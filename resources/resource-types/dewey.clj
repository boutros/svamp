{
 :rdf-type "http://dewey.info/Class"
 :index-type "dewey"
 :index-mappings { "<http://www.w3.org/2004/02/skos/core#notation>" {"location" {:type "float"}}
                   "<http://www.w3.org/2004/02/skos/core#narrower>" {"narrower" {:type "string" :index "not_analyzed"}}
                   "<http://www.w3.org/2004/02/skos/core#broader>" {"broader" {:type "string" :index "not_analyzed"}}
                   "<http://www.w3.org/2004/02/skos/core#prefLabel>" {"prefLabel" {:type "string"}}
                   "<http://www.w3.org/2004/02/skos/core#exactMatch>" {"exactMatch" {:type "string" :index "not_analyzed"}}
                   "<http://www.w3.org/2004/02/skos/core#closeMatch>" {"closeMatch" {:type "string" :index "not_analyzed"}}}
 :label "Dewey"
 :desc "a location in the dewey decimal system"
 :uri-fn (fn [{:keys [location]}]
           (str "<http://dewey.info/class/" (->> location first :value) ">"))
 :inner-rules [(fn [{:keys [location uri-fn] :as all}]
                 (let [l (->> location first :value)]
                   (str (uri-fn all) " a <http://www.w3.org/2004/02/skos/core#Concept>")))
               (fn [{:keys [location uri-fn] :as all}]
                 (let [l (->> location first :value)]
                   (when-let [broader-dewey
                              (cond
                               (re-find #"\.\d{2,}" l) (subs l 0 (dec (count l)))
                               (re-find #"\.\d{1}$" l) (subs l 0 (- (count l) 2))
                               (re-find #"[1-9]$" l) (str (subs l 0 (dec (count l))) "0")
                               (re-find #"[1-9]{2}0$" l) (str (first l) "00"))]
                     (str (uri-fn all) " <http://www.w3.org/2004/02/skos/core#broader> " (uri-fn {:location [{:value broader-dewey}]}) " . "
                          (uri-fn {:location [{:value broader-dewey}]}) " <http://www.w3.org/2004/02/skos/core#narrower> " (uri-fn all) " . "))))]
 :outer-rules []
 :search-label (fn [{:keys [location label]}]
                 (str (->> location first :value) " "
                      (->> label first :value)))
 :display-label (fn [{:keys [location label]}]
                  (str (->> location first :value) " "
                       (->> label first :value)))
 :groups [ {:elements
            [{:id :location
              :label "Location"
              :unique true
              :desc "The number in the decimal system"
              :repeatable false
              :required true
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#notation>" :type :float}}
             {:id :label
              :label "Label"
              :repeatable true
              :required true
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#prefLabel>" :type :string}}
             {:id :exactSubject
              :label "Subject (excact match)"
              :desc "a subject which maps exactly this dewey location"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#exactMatch>" :type :uri}}
             {:id :closeSubject
              :label "Subject (close match)"
              :desc "a subject which maps roughly to this dewey location"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "<http://www.w3.org/2004/02/skos/core#closeMatch>" :type :uri}}]}]
}
