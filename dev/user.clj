(ns user
  "hacking repl"
  (:require [schema.core :as s]
            [knakk.svamp.settings :refer [settings]]
            [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.index :as esi]
            [clojurewerkz.elastisch.native.document :as esd]))

(comment
(es/connect! [["127.0.0.1" 9300]] {"cluster.name" "svamp"})


(esd/search-all-indexes-and-types :query {:match_phrase_prefix {:message "eple"}})
(esd/search-all-indexes-and-types :query {:prefix {:str "ep"}})
  )


