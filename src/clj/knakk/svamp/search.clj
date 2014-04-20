(ns knakk.svamp.search
  (:require [knakk.svamp.settings :refer [settings]]
            [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.index :as esi]
            [clojurewerkz.elastisch.native.document :as esd]))


;; Data =======================================================================

(def all-indexes ["drafts" "public"])

;; Private functions ==========================================================

(defn- search
  [q indexes type]
  (if type
    (cond
     (= 1 (count q)) (esd/search indexes  (name type) :query {:prefix {:searchLabel q}})
     :else (esd/search indexes (name type) :query {:match {:searchLabel q}}))
    (cond
     (= 1 (count q)) (esd/search-all-types indexes :query {:prefix {:searchLabel q}})
     :else (esd/search-all-types indexes :query {:match {:searchLabel q}}))))

;; Public API =================================================================

(defn search-all
  "Search all indexes. Optionally limited by type.

  Returns the Elasticsearch results."
  [q & {:keys [type]}]
  (search q all-indexes type))


(defn search-drafts
  "Search the draft index. Optionally limited by type.

  Returns the Elasticsearch results."
  [q & {:keys [type]}]
  (search q "drafts" type))

(defn search-public
  "Search the public index. Optionally limited by type.

  Returns the Elasticsearch results."
  [q & {:keys [type]}]
  (search q "public" type))

(comment
  (es/connect! [["127.0.0.1" 9300]] {"cluster.name" "svamp"})
  (->> (search-all "bi") :hits :hits)

  )
