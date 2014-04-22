(ns user
  "hacking repl"
  (:require [schema.core :as s]
            [clojure.repl :refer :all]
            [clojure.java.io :as io]
            [knakk.svamp.settings :refer [settings]]
            [knakk.svamp.resources :as resources]
            [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.index :as esi]
            [clojurewerkz.elastisch.native.document :as esd]))

(defn read-lines [^String filename]
  (with-open [x (io/reader filename)]
    (vec (line-seq x))))

(defn gen-resource
  [type values]
  (let [template (->> (str "resource-types/" type) io/resource slurp load-string)]
    (assoc template :groups
     (into []
      (for [g (:groups template)]
       (assoc g :elements
        (into []
         (for [e (:elements g)
               :let [vt (:value-template e)]]
          (assoc e :values
            (for [v values
                  :when (= (:id e) (:id v))]
              (assoc vt :value (:value v))))))))))))

(comment

;; Importer Dewey 1000-numrene

(def dewey-all
  (map #(clojure.string/split % #"\s{2}") (read-lines "data/dewey_cleaned.txt")))

(doseq [[loc label] dewey-all]
  (resources/create! (gen-resource "dewey.clj" [{:id :location :value loc}
                                                {:id :label :value label}])
                                   true
                                   "dewey.clj"))
)


