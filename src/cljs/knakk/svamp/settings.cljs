(ns knakk.svamp.settings
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan alts! <!]]
            [knakk.svamp.utils :refer [edn-xhr]]))

(enable-console-print!)

(defn handle-text
  "Update state for text-input."
  [e element owner]
  (om/transact! element :value (fn [_] (.. e -target -value))))

; TODO make regex customizable for all types?
(defn handle-number
  "Change-handler for numbers."
  [e element owner old-value]
  (let [new-value (.. e -target -value)]
   (if (re-find #"^[0-9]*$" new-value)
     (do
       (om/update! element :value new-value)
       (om/set-state! owner :old-value new-value))
     (om/set-state! owner :old-value old-value))))

(defn text-input
  "Component: text input."
  [element owner]
  (reify
    om/IRender
    (render [this]
      (dom/input #js {:value (:value element)
                      :type "text"
                      :onChange #(handle-text % element owner)}))))

(defn number-input
  "Component: numbers-only input."
  [element owner]
  (reify
    om/IInitState
    (init-state [_]
      {:old-value (:value element)})
    om/IRenderState
    (render-state [_ {:keys [old-value]}]
      (dom/input #js {:value old-value
                      :type "text"
                      :onChange #(handle-number % element owner old-value)}))))

(defn options-input
  "Component: dropdown input."
  [element owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/select #js {:value (:selected element)}
        (map (fn [[k v]]
          (dom/option #js {:value k} v))
        (seq (:options element)))))))

;; Dispatch on input type:
(defmulti input-type (fn [element _] (:type element)))
(defmethod input-type :number [element owner] (number-input element owner))
(defmethod input-type :text [element owner] (text-input element owner))
(defmethod input-type :options [element owner] (options-input element owner))
;;TODO (defmethod input-type :checkbox [element owner] (checkbox-input element owner))

(defn single-view
  "Component: view for non-repeatable settings."
  [group owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul nil
        (map (fn [e]
          (dom/li nil
            (dom/label nil (name (:id e)))
            (om/build input-type e)
            (dom/span #js {:className "elementDesc"} (:desc e))))
        (:elements group))))))

(defn multi-row ; TODO row is a wrong name here, rename repeatable?
  "Component: repeatable elements to an id."
  [multirow owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [delete]}]
      (dom/div nil
        (dom/div #js {:className "multiID"}
          (dom/strong nil "ID: ")
          (dom/span nil (name (:id multirow)))
          (dom/span #js {:className "delete right"
                         :onClick (fn [e] (put! delete @multirow))} "x"))
        (apply dom/div #js {:className "row"}
          (map (fn [[k v] pair]
                 (dom/div #js {:className "multiElement column half"}
                   (dom/label nil (name k))
                   (om/build input-type v)))
               (:content multirow)))))))

(defn add-multi-element
  "Update state: add new repeatable element."
  [group owner template]
  (when-let [id (keyword (om/get-state owner :new-id))]
    (om/transact! group :elements #(conj % (assoc @template :id id)))
    (om/set-state! owner :new-id "")))

(defn handle-change-id
  "Change-handler: id for new element."
  [e owner current-id]
  (let [new-id (.. e -target -value)]
    (if-not (or (= "" new-id) (re-find #"^[\w\d-]+$" new-id))
      (om/set-state! owner :ned-id current-id)
      (om/set-state! owner :new-id new-id))))

(defn add-disabled?
  "Helper: disable add-element button if the input form is empty or if
   the id is allready in use in that settings-group."
  [group id]
  (or (= id "")
      (some #(= % (keyword id)) (map :id (group :elements)))))

(defn multi-view
  "Component: view for repeatable settings."
  [group owner]
  (reify
    om/IInitState
    (init-state [_]
      {:delete (chan) :template (:template group) :new-id ""})
    om/IWillMount
    (will-mount [_]
      (let [delete (om/get-state owner :delete)]
        (go (loop []
              (let [el (<! delete)]
                (om/transact! group :elements
                  (fn [xs] (vec (remove #(= el %) xs))))
                (recur))))))
    om/IRenderState
    (render-state [_ {:keys [template delete new-id]}]
      (dom/div nil
        (apply dom/div nil
          (om/build-all multi-row
                        (seq (:elements group))
                        {:init-state {:delete delete}}))
        (dom/div #js {:className "addID beigebg lineBar"}
          (dom/strong nil "ID: ")
          (dom/input #js {:type "text" :value new-id
                          :onChange #(handle-change-id % owner new-id)
                          :onKeyPress #(when (== (.-keyCode %) 13)
                                         (add-multi-element group owner template))})
          (dom/button #js {:onClick #(add-multi-element group owner template)
                           :disabled (add-disabled? group new-id)} "add new element"))))))

;; Dispatch on :type of group, repeatable or non-repeatable
(defmulti group-view (fn [group _] (:type group)))
(defmethod group-view :single [group owner] (single-view group owner))
(defmethod group-view :multi [group owner] (multi-view group owner))

;; TODO move to utisl
(defn display [show]
  "Helper: hide a dom element when show is true."
  (if show
    #js {}
    #js {:display "none"}))

(defn groups
  "Component: Group of settings."
  [group owner]
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

(defn settings
  [settings owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (edn-xhr
        {:method :get
         :url "api/settings"
         :on-complete #(om/update! settings %)}))
    om/IRender
    (render [_]
      (apply dom/div #js {:className "settings"}
             (om/build-all groups settings)))))

(om/root settings {} {:target (. js/document (getElementById "page-app"))})
