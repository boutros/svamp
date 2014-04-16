(ns user
  "hacking repl"
  (:require [schema.core :as s]
            [knakk.svamp.settings :refer [settings]]
            [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.index :as esi]
            [clojurewerkz.elastisch.native.document :as esd]))

(comment
(es/connect! [["127.0.0.1" 9300]] {"cluster.name" "svamp"})

(def index-settings
  {"index"
   {"analysis"
    {"analyzer"
     {"default_index"
      {"type" "custom"
       "tokenizer" "standard"
       "char_filter" ["html_strip"]
       "filter" ["lowercase" "myNgram"]}
      "default_search"
      {"type" "custom"
       "tokenizer" "standard"
       "filter" ["standard" "lowercase"]}}
     "filter"
     {"myNgram"
      {"type" "nGram"
       "min_gram" 2
       "max_gram" 20}}}}})

(esi/create "public" :settings index-settings)
(esi/create "drafts" :settings index-settings)

(esd/create "drafts" "test" {:str "eplekake med fisk og julesmørbrød"})

(esd/search-all-indexes-and-types :query {:match_phrase_prefix {:message "eple"}})

  )

;(esd/search-all-indexes-and-types :query {:prefix {:str "ep"}})
