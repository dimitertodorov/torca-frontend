; torcaui.app
; Store global torcaui state here for access outside the OM constructs
; Require and refer to state for easy access to the Global state.


(ns torcaui.app
  (:require [torcaui.state :as state]
            [torcaui.utils :as utils]
            [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer close!]]))

(def mouse-move-ch
  (chan (sliding-buffer 1)))

(def mouse-down-ch
  (chan (sliding-buffer 1)))

(def mouse-up-ch
  (chan (sliding-buffer 1)))

(def controls-ch
  (chan))

(def api-ch
  (chan))

(def errors-ch
  (chan))

(def navigation-ch
  (chan))


(def ^{:doc "websocket channel"} ws-ch
  (chan))

(defn navigation-menu []
  {:content [{:text "Home"
              :icon "fa-home"
              :href "/"
              :external false
              :navigation-points [:dashboard]}
             {:text "Forms Extra"
              :icon "fa-edit"
              :content [{:text "Potato"
                         :external false
                         :href "/test/ijsdfojisfosidjfosdjif"
                         :navigation-points [:potato]}
                        {:text "Some Other"
                         :external false
                         :content [{:text "Veg"
                                    :href "/veg"
                                    :external false
                                    :navigation-points [:vegetarian]}]}]}
             {:text "Landing"
              :href "/landing"
              :icon "fa-plane"
              :navigation-points [:landing]}]})

(defn app-state []
  (let [initial-state (state/initial-state)]
    (atom (assoc initial-state
            :search/results []
            :navigation-point :landing
            :navigation-menu (navigation-menu)
            :render-context (-> js/window
                                (aget "renderContext")
                                utils/js->clj-kw)
            :comms {:controls  controls-ch
                    :api       api-ch
                    :errors    errors-ch
                    :nav       navigation-ch
                    :ws        ws-ch
                    :controls-mult (async/mult controls-ch)
                    :api-mult (async/mult api-ch)
                    :errors-mult (async/mult errors-ch)
                    :nav-mult (async/mult navigation-ch)
                    :ws-mult (async/mult ws-ch)
                    :mouse-move {:ch mouse-move-ch
                                 :mult (async/mult mouse-move-ch)}
                    :mouse-down {:ch mouse-down-ch
                                 :mult (async/mult mouse-down-ch)}
                    :mouse-up {:ch mouse-up-ch
                               :mult (async/mult mouse-up-ch)}}))))

(def state
  (app-state))

(defn api-url [url]

  (let [base-url (get-in @state [:render-context :base_url])]
    (utils/mlog base-url)
    (if (and url base-url)
      (str base-url url)
      (str url))))