(ns torcaui.parser
  (:require [om.next :as om]
            [cljs.core.async :as async :refer [<! >! put! chan]]
            [clojure.string :as string])
  (:import [goog Uri]
    [goog.net Jsonp]))



;; =============================================================================
;; Reads

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [state]} k _]
  (let [st @state]
    {:value (get st k)}))

(defmethod read :dom-com/props
  [{:keys [parser query ast target] :as env} k params]
  {:value (parser env query target)})

(defmethod read :search/results
  [{:keys [state ast] :as env} k {:keys [query]}]
  (println query)
  (merge
    {:value (get @state k [])}
    (when-not (or (string/blank? query)
                  (< (count query) 3))
      {:search ast})))

;; =============================================================================
;; Mutations

(defmulti mutate om/dispatch)

(defmethod mutate 'navigate!
  [{:keys [state] :as env} _ {:keys [navigation-point navigation-data] :as params}]
  {:value {:keys [:navigation-point :navigation-data]}
   :action #(swap! state assoc :navigation-point navigation-point
                   :navigation-data navigation-data)})