(ns knakk.svamp.help
  "Help & troubleshooting in question/answer format"
  (:require [knakk.svamp.resources :as resources]
            [knakk.svamp.settings :refer [settings]]
            [knakk.svamp.index :as index]))


;; Public API =================================================================

(defn hooks
  "Theese are the hooks with functions which can be run from the help page.

  All hooks must return a following map:
  {:results [<successmessages], :errors [<errormessages>]}"
  [id params]
  (condp = id
    :recreate-indexes (index/reset-all! index/indexes)
    :re-index (let [file (:resource-file params)]
                (if (= "All types" file)
                  (apply merge-with into
                         (map index/update-mapping!
                              (for [t (resources/all-types)] (:file t))))
                  (index/update-mapping! file)))
    :indexing-queue {:results ["Size of the queue: 0"] :errors []}))

(defn data []
  [
   {:id :recreate-indexes
    :title "Reset resource indexes"
    :question "I modified the resource indexing settings - when does the changes take effect?"
    :answer "You'll have to recreate the indexes. Then schedule a full re-index."
    :action {:type :button :text "Drop and recreate indexes"}
    :see-also [{:id :re-index :desc "Schedule resource re-indexing"}]}

   {:id :re-index
    :title "Schedule resource re-indexing"
    :question "I changed some resource type-definitions, but it doesn't seem to have any effect on my search-results."
    :answer "You have to update the mapping and schedule a re-index for the changed types:"
    :action {:type :dropdown :options (into [{:index-type "all" :label "All types"}] (resources/all-types)) :button "Update mappings and schedule re-indexing"}
    :see-also [{:id :indexing-queue :desc "Check status of indexing queue"}]}

   {:id :indexing-queue
    :title "Check status of indexing queue"
    :question "How will I know that all my resources are indexed?"
    :answer "Check the status of the indexing queue. It will tell you how many resources are waiting to be indexed."
    :action {:type :button :text "Check indexing queue status"}
    :see-also [{:id :re-index :desc "Schedule resource re-indexing"}]}
   ])
