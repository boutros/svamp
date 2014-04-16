(ns knakk.svamp.help
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan alts! <!]]
            [knakk.svamp.utils :refer [edn-xhr]]))

(enable-console-print!)

(defn button-action
  [action owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [action-fn]}]
      (dom/div #js {:className "monospace"}
        (dom/button #js {:onClick #(action-fn nil)}
                    (:text action))))))

(defn dropdown-action
  [action owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [action-fn]}]
      (dom/div #js {:className "monospace"}
        (apply dom/select nil
          (map
            (fn [o]
              (dom/option #js {:value (:index-type o)} (:label o)))
            (into [{:label "All types" :index-type false}] (:options action))))
        (dom/button nil #js {:onClick #(action-fn nil)}
                    (:button action))))))

(defmulti action-type (fn [action _] (:type action)))
(defmethod action-type :button [action owner opts] (button-action action owner))
(defmethod action-type :dropdown [action owner opts] (dropdown-action action owner))

;; TODO common component; move to utils
(defn spinner []
  (om/component
    (dom/div #js {:className "spinner"}
      (dom/div #js {:className "bounce1"})
      (dom/div #js {:className "bounce2"})
      (dom/div #js {:className "bounce3"}))))

(defn q-and-a
  [qa owner]
  (reify
    om/IInitState
    (init-state [_]
      {:result [] :error [] :waiting false
       :action-fn (fn [params]
                 (do
                   (om/set-state! owner :result [])
                   (om/set-state! owner :error [])
                   (om/set-state! owner :waiting true)
                   (edn-xhr
                    {:method :post
                     :url "api/help"
                     :data {:id (:id @qa) :params params}
                     :on-complete #(om/set-state! owner %)})))})
    om/IRenderState
    (render-state [_ {:keys [result error waiting action-fn]}]
      (dom/div #js {:id (name (:id qa))}
        (dom/p #js {:className "qaQuestion"}
          (dom/span #js {:className "qaLetter"} "Q")
          (dom/span nil (:question qa)))
        (dom/p nil
          (dom/span #js {:className "qaLetter"} "A")
          (dom/span nil (:answer qa)))
        (om/build action-type (:action qa) {:init-state {:action-fn action-fn}})
        (when waiting
          (dom/img #js {:className "loading" :src "img/loading.gif"}))
        (when (seq result)
          (apply dom/ul #js {:className "actionResult monospace"}
            (map
             (fn [line] (dom/li nil line))
             result)))
        (when (seq error)
          (apply dom/ul #js {:className "actionError monospace"}
            (map
             (fn [line] (dom/li nil line))
             error)))
        (when (seq (:see-also qa))
          (dom/div #js {:className "seeAlso monospace"}
            (dom/span nil "See also:")
            (apply dom/ul nil
              (map
               (fn [link]
                 (dom/li nil
                   (dom/a #js {:className "mrgh"
                               :href (str "help#" (name (:id link)))}
                          (:desc link))))
               (:see-also qa)))))))))

(defn help
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (edn-xhr
        {:method :get
         :url "api/help"
         :on-complete #(om/update! data %)}))
    om/IRender
    (render [_]
      (apply dom/div #js {:className "help"}
        (om/build-all q-and-a data)))))

(om/root help {} {:target (. js/document (getElementById "page-app"))})
