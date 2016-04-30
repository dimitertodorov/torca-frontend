(ns torcaui.components.app
  (:require [torcaui.components.landing :as landing]
    [om.next :as om :refer-macros [defui ui]]
    [om.dom :as dom]
    [torcaui.utils :as utils :include-macros true])
  (:require-macros [torcaui.utils :refer [html]]))

(def nav->component
  {:landing landing/AutoCompleter
   :none landing/Home
   :error landing/Home})

(def nav->factory
  (zipmap (keys nav->component)
          (map om/factory (vals nav->component))))

(defui App
  static om/IQueryParams
  (params [this]
    {:dom-com/query [:farkle]})
  static om/IQuery
  (query [this]
    `[:navigation-point {:dom-com/props ?dom-com/query} :navigation-menu :navigation-data])
  Object
  (render [this]
    (print "Render Root View")
    (let [{:keys [navigation-point dom-com/props :search/results] :as pots} (om/props this)
          dominant-component (navigation-point nav->factory)]
      (html [:div.class-1
             [:div.main-component (dominant-component {:search/results []})]
             [:div.bottom-footer (str pots)]]))))
