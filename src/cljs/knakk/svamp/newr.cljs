(ns knakk.svamp.newr
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.Uri.QueryData]
            [cljs.core.async :refer [put! chan alts! <!]]
            [knakk.svamp.utils :refer [edn-xhr display]]))

(enable-console-print!)

(def temp-results
  [{:label "Knut Hamsun (1895-1952)" :uri "<bla1>"}
   {:label "Knut Hansen" :uri "<bla2>"}
   {:label "Knut Haukelid (1812-1924)" :uri "<bla3>"}
   {:label "Knut Haugland" :uri "<bla4>"}
   {:label "Knut Helle" :uri "<bla5>"}
   {:label "Knut Hergel" :uri "<bla6>"}
   {:label "Knut Hjeltnes (1912-)" :uri "<bla7>"}
   {:label "Knut Holtman (1920-1921)" :uri "<bla8>"}
   {:label "Knut Haakonsen" :uri "<bla9>"}
   ])

;; TODO move to utils.cljs
(defn query-params
  "Creates a hash of key-values from the query string.
  It assumes first character is '?', therfore the subs 1."
  []
  (let [query-data (goog.Uri.QueryData. (subs js/window.location.search 1))]
    (zipmap (map keyword (.getKeys query-data)) (.getValues query-data))))

;; TODO common handler, should be shared
(defn handle-text
  "Update state for text-input."
  [e element owner]
  (om/transact! element :value (fn [_] (.. e -target -value))))

;; TODO common componant, should be shared
(defn text-input [element owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
        (dom/input #js {:value (:value element)
                       :type "text"
                       :onChange #(handle-text % element owner)})))))

(defn uri-search
  [e element owner]
  (let [query (.. e -target -value)]
    (om/set-state! owner :searching (not= "" query))
    (om/set-state! owner :query query)))

(defn uri-search-bar
  "Component: Search for URIs. Rolldown resultlist of URI labels."
  [element owner]
  (reify
    om/IInitState
    (init-state [_]
      {:searching false  :query "" :results temp-results})
    om/IRenderState
    (render-state [_ {:keys [searching results query chosen-chan]}]
      (dom/div nil
        (dom/div #js {:className "uriSearchBar"}
          (dom/input #js {:className "monospace" :placeholder "search" :value query
                          :onChange #(uri-search % element owner)
                          ;; delay with 100ms as not to hide before the onClick of the results:
                          :onBlur (fn [e] (js/setTimeout
                                            #(do
                                               (om/set-state! owner :searching false)
                                               (om/set-state! owner :query ""))
                                            100))
                          :onKeyUp #(when (== (.-keyCode %) 27)
                                      (do
                                        (om/set-state! owner :searching false)
                                        (om/set-state! owner :query "")))
                          :onFocus #(uri-search % element owner)})
           (dom/div #js {:className "uriSearchResults" :style (display searching)}
            (apply dom/ul nil
              (om/build-all (fn [r]
                              (dom/li #js {:onClick (fn [e]
                                                      (do
                                                        (om/update! element :value (:uri r))
                                                        ;; TODO send uri as well via channel:
                                                        ;; {:label (:label r) :uri (:uri r)}
                                                        (put! chosen-chan (:label r))))}
                                                        (:label r))) results))
            (dom/button nil
              (str "Create a new " (get-in element [:property :predicate])))))))))

(defn uri-input [element owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chosen "" :chosen-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [chosen-chan (om/get-state owner :chosen-chan)]
        (go (loop []
              (let [c (<! chosen-chan)]
                (om/set-state! owner :chosen c))
                (recur)))))
    om/IRenderState
    (render-state [_ {:keys [chosen chosen-chan]}]
      (dom/div nil
        (dom/div nil
          (dom/input #js {:value (:value element) :disabled true :type "text" :title chosen})
          (when (seq (:value element))
            (dom/span #js {:className "delete mrgh" :onClick #(om/update! element :value nil)} "x")))
        (om/build uri-search-bar element {:init-state {:chosen-chan chosen-chan}})))))

(defmulti input-type (fn [element _] (:rdf-type element)))
(defmethod input-type :integer [element owner] (text-input element owner))
(defmethod input-type :string [element owner] (text-input element owner))
(defmethod input-type :uri [element owner] (uri-input element owner))

(defn single-view
  [element owner]
  (reify
    om/IRender
    (render [_]
      (let [property (:property element)]
        (dom/div #js {:className "resource monospace"}
          (dom/div #js {:className "elementTitle"}
            (dom/label nil (:label property))
            (when (:required element)
              (dom/span #js {:className "red bold"} "*")))
          (om/build input-type element)
          (when (:repeatable element)
            (dom/button #js {:className "addElement"
                             :title (str "add another " (:label property))} "+"))
          ;(dom/div #js {:className "relementDesc"} (:desc property))
          )))))


(defn multi-view
  [element owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chosen ""})
    om/IRenderState
    (render-state [_ {:keys [chosen]}]
      (dom/div #js {:className "resource monospace row"}
        (dom/div nil
          (dom/div #js {:className "elementTitle"}
            (dom/label nil (:property-label element))
            (when (:required element)
              (dom/span #js {:className "red"} "*")))
          (dom/div #js {:className "column half"}
            (dom/input #js {:value (:value element) :disabled true :type "text" :title chosen})
            (when (seq (:value element))
              (dom/span #js {:className "delete mrgh" :onClick #(om/update! element :value nil)} "x"))
            (apply dom/select nil
              (map (fn [p] (dom/option #js {:value (:predicate p)} (:label p))) (:properties element)))))
        (om/build uri-search-bar element)))))

(defmulti element-view (fn [element _] (:input-type element)))
(defmethod element-view :single [element owner] (single-view element owner))
(defmethod element-view :multi [element owner] (multi-view element owner))

(defn input-group
  [group owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h2 nil (:title group))
        (apply dom/div nil
          (om/build-all element-view (:elements group)))))))

; (defn assoc-value [data]
;   (assoc data :groups
;     (map
;       (fn [group]
;         (assoc group :elements
;           (map
;             (fn [element]
;               (assoc element :value [(:value-template element)]))
;             (:elements group))))
;       (data :groups))))

(defn resource
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (edn-xhr
        {:method :get
         :url (str "api/template?template=" (:template (query-params)))
         :on-complete #(om/update! data %)}))
    om/IRender
    (render [_]
      (if (:error data)
        (dom/div #js {:className "error"} (:error data))
        (dom/div nil
          (dom/div #js {:className "page-fixed-header monospace"}
            (dom/button nil "Save draft")
            (dom/button #js {:disabled true} "Preview RDF")
            (dom/button #js {:disabled true :title "You must fill in the required fields to publish."}
              (dom/strong nil "Publish")))
          (dom/h1 nil (:label data))
          (dom/p nil (:desc data))
          (dom/div #js {:className "rdfType monospace"} (:rdf-type data))
          (apply dom/div nil
            (om/build-all input-group (:groups data))))))))

(om/root resource {} {:target (. js/document (getElementById "page-app"))})