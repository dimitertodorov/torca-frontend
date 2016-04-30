(ns torcaui.core
  (:require [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer close!]]
            [torcaui.async :refer [put!]]
            [goog.dom :as gdom]
            [goog.dom.classes :as classes]
            [goog.style :as style]
            [goog.events :as events]
            [goog.fx.dom]
            [goog.fx.easing :as ease]
            [om.next :as om :include-macros true]
            [om.dom :as dom]
            [torcaui.parser :as p]
            [torcaui.app :refer [state mouse-down-ch mouse-up-ch mouse-move-ch]]
            [torcaui.utils :as utils :refer [mlog merror third set-canonical!]]
            [torcaui.controllers.navigation :as nav-con]
            [torcaui.controllers.controls :as controls-con]
            [torcaui.controllers.api :as api-con]
            [torcaui.controllers.errors :as errors-con]
            [torcaui.state :as state]
            [torcaui.datetime :as datetime]
            [torcaui.components.app :as app]
            [torcaui.funcs :as funcs]
            [torcaui.config :as config]
            [torcaui.history :as history]
            [torcaui.routes :as routes]
            [torcaui.components.landing :as landing]
            [torcaui.localstorage :as localstorage]
            [devcards.core :as dc :refer-macros [defcard deftest]]
            [sablono.core :as html :refer-macros [html]])
  (:require-macros [cljs.core.async.macros :as am :refer [go go-loop alt!]]
                   [torcaui.utils :refer [inspect timing swallow-errors]])
  (:import [goog Uri]
    [goog.net Jsonp]))




(enable-console-print!)
(js/window.addEventListener "mousedown" #(put! mouse-down-ch %))
(js/window.addEventListener "mouseup"   #(put! mouse-up-ch   %))
(js/window.addEventListener "mousemove" #(put! mouse-move-ch %))


(defn log-channels?
  "Log channels in development, can be overridden by the log-channels query param"
  []
  (:log-channels? utils/initial-query-map (config/log-channels?)))

(def ^:private debug-state)

(def reconciler)
(def parser (om/parser {:read p/read :mutate p/mutate}))

(def base-url
  "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

(defn jsonp
  ([uri] (jsonp (chan) uri))
  ([c uri]
   (let [gjsonp (Jsonp. (Uri. uri))]
     (.send gjsonp nil #(put! c %))
     c)))

(defn search-loop [c]
  (go
    (loop [[query cb] (<! c)]
      (let [[_ results] (<! (jsonp (str base-url query)))]
        (cb {:search/results results}))
      (recur (<! c)))))


(defn send-to-chan [c]
  (fn [{:keys [search] :as opts} cb]
    (if search
      (let [{[search] :children} (om/query->ast search)
            query (get-in search [:params :query])]
        (put! c [query cb])))))


(def send-chan (chan))

(search-loop send-chan)

(defn nav-handler
  [[navigation-point {:keys [inner? query-params] :as args} :as value] history]
  (when (log-channels?)
    (mlog "Navigation Verbose: " value))
  (nav-con/navigated-to history navigation-point args reconciler)
  (nav-con/post-navigated-to! history navigation-point args reconciler))

(defn controls-handler
  [value container]
  (when (log-channels?)
    (mlog "Controls Verbose: " value)))

(defn api-handler
  [value container]
  (when (log-channels?)
    (mlog "API Verbose: " (first value) (second value) (utils/third value))))


(defn errors-handler
  [value state container]
  (when (log-channels?)
    (mlog "Errors Verbose: " value)))

(defn body []
  (goog.dom/getElement "main_body"))

(defn prepare_canvas []
  (let [width (.-width (goog.dom/getViewportSize))
        height (.-height (goog.dom/getViewportSize))]
    (if (< width 769)
      (goog.dom.classes/add (body) "body-small")
      (goog.dom.classes/remove (body) "body-small"))))

(defn listen-resize []
  (let [vsm (goog.dom/getWindow)]
    (events/listen vsm (.-RESIZE events/EventType) #(prepare_canvas))))

(defn find-top-level-node []
  (.-body js/document))

(defn find-app-container []
  (goog.dom/getElement "omapp"))

(declare reinstall-om!)



(defn install-om [state container comms]
  (let [rec (om/reconciler {:state state
                            :parser parser
                            :send    (send-to-chan send-chan)
                            :remotes [:remote :search]})]
    (set! reconciler rec)
    (om/add-root! reconciler app/App container)))


(defn main [state top-level-node history-imp]
  (let [comms       (:comms @state)
        container   (find-app-container)
        uri-path    (.getPath utils/parsed-uri)
        controls-tap (chan)
        nav-tap (chan)
        api-tap (chan)
        errors-tap (chan)]

    (routes/define-routes! state)
    (prepare_canvas)
    (listen-resize)
    (install-om state container comms)
    (async/tap (:nav-mult comms) nav-tap)
    (async/tap (:controls-mult comms) controls-tap)
    (async/tap (:api-mult comms) api-tap)
    (async/tap (:errors-mult comms) errors-tap)
    (go (while true
          (alt!
            controls-tap ([v] (controls-handler v container))
            api-tap ([v] (api-handler v container))
            nav-tap ([v] (nav-handler v history-imp))
            errors-tap ([v] (errors-handler v state container))
            (async/timeout 10000) (do #_(print "TODO: print out history: ")))))))

(defn ^:export reinstall-om! []
  (println "REINSTALLING OM")
  (routes/define-routes! debug-state)
  (install-om debug-state (find-app-container) (:comms debug-state)))

(defn ^:export setup! []
  (let [top-level-node (find-top-level-node)
        history-imp (history/new-history-imp top-level-node)]
    ;; globally define the state so that we can get to it for debugging
    (routes/define-routes! state)
    (set! debug-state state)
    (main state top-level-node history-imp)
    ;;(println (str "/" (.getToken history-imp)))
    ;; Redirect to root if index.html is specified.
    (let [token (.getToken history-imp)]
      (if (= token "index.html")
        (aset js/window "location" "/")))
    (routes/dispatch! (str "/" (.getToken history-imp)))))

