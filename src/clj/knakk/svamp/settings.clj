(ns knakk.svamp.settings
  "Application settings managment"
  (:require [knakk.svamp.utils :refer [debounce]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre :refer [info]]))

(defn- extract-single [elements]
  (into {}
    (for [e elements
          :let [id-element (:id e)
                options? (= (:type e) :options)
                value (if options?
                        (:selected e)
                        (:value e))]]
      [id-element value])))

(defn- extract-multi [elements]
  (into {}
    (for [e elements :let [id-element (:id e)
                           sub-elements (:content e)]]
      [id-element
       (into {}
         (for [[k v] sub-elements]
           [k (:value v)]))])))

(defmulti settings-type (fn [g] (:type g)))
(defmethod settings-type :single [g] (extract-single (:elements g)))
(defmethod settings-type :multi [g] (extract-multi (:elements g)))

(defn vec-to-map
  "Transform a settings vector (file structure) to a map active settings"
  [v]
  (into {}
    (for [group v :let [id-group (:id group)]]
      [id-group (settings-type group)])))

;; Settings in vector format (for predictable ordering in UI)
(def file-settings
  (->> "settings.edn" io/resource slurp edn/read-string atom))

;; Settings in map format (for easy key access)
;; This is the one used by the application server.
(def settings
  (atom (vec-to-map @file-settings)))

(defn sync-settings
  "Sync file-settings to settings. This has to be run for changes in settings
  to take effect in the running application server."
  []
  (reset! settings (vec-to-map @file-settings)))

(def debounce-wait 5000) ;; In ms

(def save-debounced
  (debounce
    #(do
       (spit (clojure.java.io/file "test.edn") @file-settings)
       (info "settings flushed to disk")) debounce-wait))

(add-watch file-settings :settings-state-watch
  (fn [_ _ _ _]
    (save-debounced)))


