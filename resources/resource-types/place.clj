{
 :rdf-type "http://data.deichman.no/format/Place"
 :index-type "place"
 :index-mappings {}
 :uri-fn (fn [{:keys [getty-id]}]
           (str "<http://vocab.getty.edu/tgn/" (->> getty-id first :value) ">"))
 :label "Place"
 :desc "a point in space."
 :groups []
 }
