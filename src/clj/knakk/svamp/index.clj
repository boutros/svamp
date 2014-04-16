(ns knakk.svamp.index
  (:require [clojure.string :as string]
            [knakk.svamp.settings :refer [settings]]
            [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.index :as esi]
            [clojurewerkz.elastisch.native.document :as esd]))


;; Data  ======================================================================

(def indexes ["drafts" "public"])

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



;; Private functions ==========================================================




;; Public API =================================================================

(defn create!
  "Create all given indexes using the settings from indexing.edn.

  Returns {:error [<errormessages>], :result [<successmessages>]}"
  [indexes]
  (let [res (atom {:result [] :error []})]
    (doseq [idx indexes]
      (try
        (esi/create idx :settings index-settings)
        (swap! res assoc :result (conj (:result @res) (str "Index added: " idx)))
        (catch Exception e
          (swap! res assoc :error (conj (:error @res) (.toString e))))))
    @res))

(defn delete!
  "Delete all given indexes.

  Returns {:error [<errormessages>], :result [<successmessages>]}"
  [indexes]
  (let [res (atom {:result [] :error []})]
    (doseq [idx indexes]
      (try
        (esi/delete idx)
        (swap! res assoc :result (conj (:result @res) (str "Index deleted: " idx)))
        (catch Exception e
          (swap! res assoc :error (conj (:error @res) (.toString e))))))
    @res))

(defn reset!
  "Deletes, then re-creates given indexes."
  [indexes]
  (let [deleted (delete! indexes)
        created (create! indexes)]
    (merge-with into deleted created)))




(comment

  (reset ["drafts" "public"])
  (es/connect! [["127.0.0.1" 9300]] {"cluster.name" "svamp"})

  (delete ["draft" "fukks" "fisk"])
  (create ["public" "drafts"])
)

