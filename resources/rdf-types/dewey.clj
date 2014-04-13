{
 :rdf-type "http://dewey.info/Class"
 :label "Dewey"
 :desc "a location in the dewey decimal system"
 :uri-fn (fn [{:keys [location]}]
           (str "<http://dewey.info/class/" (->> location first :value) ">"))
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
                               (re-find #"[1-9]{2}0$" l) (str (first l) "00"))]
                     (str (uri-fn all) " skos:broader " (uri-fn {:location [{:value broader-dewey}]}) " . "
                          (uri-fn {:location [{:value broader-dewey}]}) " skos:narrower " (uri-fn all) " . "))))]
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
              :value-template {:value "" :predicate "skos:notation" :type :float}}
             {:id :label
              :label "Label"
              :repeatable true
              :required true
              :value-template {:value "" :predicate "skos:prefLabel" :type :string}}
             {:id :exact-subject
              :label "Subject (excact match)"
              :desc "a subject which maps exactly this dewey location"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "skos:exactMatch" :type :uri}}
             {:id :close-subject
              :label "Subject (close match)"
              :desc "a subject which maps roughly to this dewey location"
              :repeatable true
              :required false
              :value-template {:value "" :predicate "skos:closeMatch" :type :uri}}]}]
 }
