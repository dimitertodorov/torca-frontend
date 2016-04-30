(ns torcaui.components.landing
  (:require [om.next :as om :refer-macros [defui]]
    [om.dom :as dom]))

(defui Home
  static om/IQuery
  (query [this]
    '[:navigation-data])
  Object
  (render [this]
    (println "Render Landing")
    (dom/div nil
             (dom/p nil "Landing page"
                    (dom/a #js {:href "/potato"} "POTATO"))
             (dom/p nil "Landing page"
                    (dom/a #js {:href "/"} "ROOT"))
             (dom/p nil (str (-> this om/props :navigation-data))))))


(defn result-list [results]
  (dom/ul #js {:key "result-list"}
          (map #(dom/li nil %) results)))

(defn search-field [ac query]
  (dom/input
    #js {:key "search-field"
         :value query
         :onChange
         (fn [e]
           (om/set-query! ac
                          {:params {:query (.. e -target -value)}}))}))

(defui AutoCompleter
  static om/IQueryParams
  (params [this]
    {:query ""})
  static om/IQuery
  (query [_]
    '[(:search/results {:query ?query})])
  Object
  (componentWillMount [this]
    (om/set-query! this {:params {:query "Dimiters"}}))
  (render [this]
    (println (om/props this))
    (println (om/tempid))
    (let [{:keys [search/results]} (om/props this)]
      (dom/div nil
               (dom/h2 nil (str (om/get-params this)))
               (cond->
                 [(search-field this (:query (om/get-params this)))]
                 (not (empty? results)) (conj (result-list results)))))))

