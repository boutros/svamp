(ns knakk.svamp.settings
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan alts! <!]]))

(enable-console-print!)

; TODO types: :no-space-string in attition to :text
(def data
  (atom {:rdf-store {:type :single
                     :title "RDF store"
                     :desc "RDF (quad-store) where all metadata is stored. The endpoint must have support for SPARQL 1.1. Openlink's Virtuoso is the recommended option."
                     :elements {:endpoint {:type :text :value "http://localhost:8890/sparql" :desc "SPARQL endpoint"}
                                :default-graph {:type :text :value "http://data.svamp.no"}
                                :auth-method {:type :options :selected :digest :options {:none "none" :basic "basic" :digest "digest"} :desc "authentication method for the SPARQL endpoint"}
                                :username {:type :text :value "noe1" :desc "username for basic/digest authentication"}
                                :password {:type :text :value "hemli" :desc "password for basic/digest authentication"}
                                :open-timeout {:type :number :value 1500 :desc "in milliseconds"}
                                :read-timeout {:type :number :value 3000 :desc "in milliseconds"}}}
         :ns {:type :multi
              :title "RDF Namespaces"
              :desc "Namespace prefixes must be defined here and are accessible to all SPARQL queries. The ID must be the same as the namespace prefix."
              :template {:prefix {:type :text :value ""}
                         :uri {:type :text :value ""}}
              :elements {:foaf {:prefix {:type :text :value "foaf"}
                                :uri {:type :text :value "http://xmlns.com/foaf/0.1/"}}
                         :dc {:prefix {:type :text :value "dc"}
                              :uri {:type :text :value "http://purl.org/dc/terms/"}}
                         :skos {:prefix {:type :text :value "skos"}
                                :uri {:type :text :value "http://www.w3.org/2004/02/skos/core#"}}}}
         :api {:type :multi
               :title "External APIs"
               :desc "URLs and credentials for external HTTP APIs."
               :template {:url {:type :text, :value ""}
                          :name {:type :text, :value ""}
                          :username {:type :text, :value ""}
                          :password {:type :text, :value ""}
                          :token {:type :text, :value "" }}
               :elements {:ol {:url {:type :text, :value "https://openlibrary.org/api/books"}
                               :name {:type :text, :value "Open Library"}
                               :username {:type :text, :value "bob"}
                               :password {:type :text, :value "bob"}
                               :token {:type :text, :value "" }}
                          :mb {:url {:type :text, :value "http://musicbrainz.org/ws/2/"}
                               :name {:type :text, :value "Musizcbrainz"}
                               :username {:type :text, :value ""}
                               :password {:type :text, :value ""}
                               :token {:type :text, :value "b8s1adnZf"}}}}}))

(defn handle-text [e element owner]
  (om/transact! element :value (fn [_] (.. e -target -value))))

; TODO make regex customizable for all types?
(defn handle-number [e element owner old-value]
  (let [new-value (.. e -target -value)]
   (if (re-find #"^[0-9]*$" new-value)
     (do
       (om/update! element :value new-value)
       (om/set-state! owner :old-value new-value))
     (om/set-state! owner :old-value old-value))))

(defn text-input [element owner]
  (reify
    om/IRender
    (render [this]
      (dom/input #js {:value (:value element)
                      :type "text"
                      :onChange #(handle-text % element owner)}))))

(defn number-input [element owner]
  (reify
    om/IInitState
    (init-state [_]
      {:old-value (:value element)})
    om/IRenderState
    (render-state [_ {:keys [old-value]}]
      (dom/input #js {:value old-value
                      :type "text"
                      :onChange #(handle-number % element owner old-value)}))))

(defn options-input [element owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/select #js {:value (:selected element)}
        (map (fn [[k v]]
          (dom/option #js {:value k} v))
        (seq (:options element)))))))

(defmulti input-type (fn [element _] (:type element)))
(defmethod input-type :number [element owner] (number-input element owner))
(defmethod input-type :text [element owner] (text-input element owner))
(defmethod input-type :options [element owner] (options-input element owner))

(defn single-view [group owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul nil
        (map (fn [[k v]]
          (dom/li nil
            (dom/label nil (name k))
            (om/build input-type v)
            (dom/span #js {:className "elementDesc"} (:desc v))))
        (seq (:elements group)))))))


(defn multi-row [[id kvpairs] owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [delete]}]
      (dom/div nil
        (dom/div #js {:className "multiID"}
          (dom/strong nil "ID: ")
          (dom/span nil (name id))
          (dom/span #js {:className "multiDelete"
                         :onClick (fn [e] (put! delete id))} "x"))
        (apply dom/div #js {:className "row"}
          (map (fn [[k v] pair]
                 (dom/div #js {:className "multiElement column half"}
                   (dom/label nil (name k))
                   (om/build input-type v)))
               (seq kvpairs)))))))

(defn add-multi-element [group owner template]
  (when-let [id (keyword (om/get-state owner :new-id))]
    (om/transact! group :elements #(assoc % id @template))
    (om/set-state! owner :new-id "")))

(defn handle-change-id [e owner current-id]
  (let [new-id (.. e -target -value)]
    (if-not (or (= "" new-id) (re-find #"^[\w\d-]+$" new-id))
      (om/set-state! owner :ned-id current-id)
      (om/set-state! owner :new-id new-id))))

(defn add-disabled? [group id]
  (or (= id "")
      (some #(= % (keyword id)) (keys (:elements group)))))

(defn multi-view [group owner]
  (reify
    om/IInitState
    (init-state [_]
      {:delete (chan) :template (:template group) :new-id ""})
    om/IWillMount
    (will-mount [_]
      (let [delete (om/get-state owner :delete)]
        (go (loop []
              (let [id (<! delete)]
                (om/transact! group :elements #(dissoc % id))
                (recur))))))
    om/IRenderState
    (render-state [_ {:keys [template delete new-id]}]
      (dom/div nil
        (apply dom/div nil
          (om/build-all multi-row
                        (seq (:elements group))
                        {:init-state {:delete delete}}))
        (dom/div #js {:className "addID"}
          (dom/strong nil "ID: ")
          (dom/input #js {:type "text" :value new-id
                          :onChange #(handle-change-id % owner new-id)})
          (dom/button #js {:onClick #(add-multi-element group owner template)
                           :disabled (add-disabled? group new-id)} "add new element"))))))

(defmulti group-view (fn [group _] (:type group)))
(defmethod group-view :single [group owner] (single-view group owner))
(defmethod group-view :multi [group owner] (multi-view group owner))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

(defn groups [group owner]
  (reify
    om/IInitState
    (init-state [_]
      {:open true})
    om/IRenderState
    (render-state [_ {:keys [open]}]
      (dom/div #js {:className "settingsGroup"}
        (dom/h2 #js {:onClick #(om/set-state! owner :open (not open))}
                (:title group)
          (dom/span #js {:className (str "triangle " (if open "open" "close"))}))
        (dom/p nil (:desc group))
        (dom/div #js {:className "settingsElements" :style (display open)}
          (om/build group-view group))))))

(om/root
  (fn [settings owner]
    (apply dom/div #js {:className "settings"}
      (om/build-all groups (vals settings))))
  data
  {:target (. js/document (getElementById "page-app"))})
