(ns knakk.svamp.resources
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan alts! <!]]
            [knakk.svamp.utils :refer [edn-xhr]]))

(enable-console-print!)

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
    (render [_]
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
          (dom/div #js {:className "monospace beigebg lineBar"}
            (dom/input #js {:type "text" :placeholder "Search for resources"})
            (dom/span nil " of type ")
            (apply dom/select nil
              (map (fn [e]
                     (dom/option #js {:value (:rdf-type e)} (:label e)))
                   (into [{:label "Any" :rdf-type "any"}] data)))
            (dom/input #js {:type "checkbox" :checked true :className "chk"})
            (dom/label nil "including drafts")))))))

(om/root metadata {} {:target (. js/document (getElementById "page-app-1"))})
