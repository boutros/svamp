(ns knakk.svamp.help
  "Help & troubleshooting in question/answer format"
  (:require [knakk.svamp.resources :refer [rdf-types]]
            [knakk.svamp.settings :refer [settings]]
            [knakk.svamp.index :as index]))


;; Private functions ==========================================================

;; Public API =================================================================

(defn hooks
  "Theese are the hooks with functions which can be run from the help page.

  All hooks must return a following map:
  {:result [<successmessages], :error [<errormessages>]}"
  [id params]
  (condp = id
    :recreate-indexes (index/reset! index/indexes)
    :re-index {:result "Full re-index scheduled" :error nil}
    :indexing-queue {:result "Size of the queue: 0" :error nil}))

(defn data []
  [
   {:id :recreate-indexes
    :question "I modified the indexing settings/mapings, when and how does the changes take effect?"
    :answer "You'll have to recreate the indexes. Then schedule a full re-index."
    :action {:type :button :text "Drop and recreate indexes"}
    :see-also [{:id :re-index :desc "Schedule a re-index"}]}

   {:id :re-index
    :question "I changed some resource type-definitions, but it doesn't seem to have any effect on my search-results."
    :answer "Schedule a re-index, by type or all types:"
    :action {:type :dropdown :options (rdf-types) :button "Schedule re-indexing"}
    :see-also [{:id :indexing-queue :desc "Check the status of indexing queue"}]}

   {:id :indexing-queue
    :question "How will I know that all my resources are indexed?"
    :answer "Check the status of the indexing queue. It will tell you how many resources are waiting to be indexed."
    :action {:type :button :text "Check indexing queue status"}
    :see-also [{:id :re-index :desc "Schedule a re-index"}]}
   ])
