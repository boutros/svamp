(ns knakk.svamp.resources
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan alts! <!]]
            [knakk.svamp.utils :refer [edn-xhr]])
  (:import goog.async.Throttle))

(enable-console-print!)

(defn do-search [q owner]
  ;; TODO throttle this function
  (edn-xhr
   {:method :post
    :url "api/search"
    :data {:q q
           :drafts? (om/get-state owner :including-drafts?)
           :type (om/get-state owner :index-type)}
    :on-complete (fn [r] (om/set-state! owner :results r))}))

(def throttled ;; WIP not working, can't understand why - but arguments aren't passed on
  (Throttle. (fn [q owner] (do-search q owner)) 300))

(defn resource-search
  [data owner]
   (reify
     om/IInitState
     (init-state [_]
      {:searching false :including-drafts? true :index-type "Any type"
       :query "" :results {}})
     om/IRenderState
     (render-state [_ {:keys [searching query results including-drafts? index-type]}]
       (dom/div #js {:className "monospace beigebg lineBar"}
         (dom/input #js {:type "text" :placeholder "Search for resources" :value query
                         :onChange (fn [e]
                                     (let [q (.. e -target -value)]
                                       (om/set-state! owner :query q)
                                       (do-search q owner)))})
         (dom/span nil " of type ")
         (apply dom/select #js {:value index-type
                                :onChange (fn [e]
                                            (do
                                              (om/set-state! owner :index-type (.. e -target -value))
                                              (do-search query owner)))}
           (map (fn [e]
                  (dom/option #js {:value (:index-type e)} (:label e)))
                (into [{:label "Any type" :index-type "Any type"}] data)))
         (dom/input #js {:type "checkbox" :checked including-drafts? :className "chk"
                         :onChange (fn [e]
                                     (do
                                       (om/set-state! owner :including-drafts? (not including-drafts?))
                                       (do-search query owner)))})
         (dom/label nil "including drafts")
         (dom/div #js {:className "searchResults"}
           (apply dom/ul nil
             (map (fn [hit]
                    (dom/li #js {:className (if (= "drafts" (:_index hit)) "red" "")}
                      (dom/span #js {:className "searchResultsType"} (:_type hit))
                      (dom/span #js {:className "searchResultsLabel"} (-> hit :_source :displayLabel))))
                  (->> results :hits :hits))))))))

(defn metadata
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (edn-xhr
        {:method :get
         :url "api/resource-types"
         :on-complete #(om/update! data %)}))
    om/IRender
    (render [this]
      (dom/div nil
        (dom/div nil
          (dom/h3 nil "Create a new resource")
          (apply dom/ul #js {:className "monospace row resources"}
            (map (fn [e]
                  (dom/a #js {:href (str "resource-new?template=" (:file e))}
                     (dom/li #js {:className "column one-third"}
                             (dom/strong nil (:label e))
                             (dom/span #js {:className "dark-gray"} (:desc e)))))
                 data)))
        (dom/div nil
          (dom/h3 nil "Modify an existing resource")
          (om/build resource-search data))))))

(om/root metadata {} {:target (. js/document (getElementById "page-app-1"))})
