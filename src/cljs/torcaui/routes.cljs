(ns torcaui.routes
  (:require [cljs.core.async :as async]
            [clojure.string :as str]
            [secretary.core :as sec :refer-macros [defroute]]))

(defn- send-nav! [nav-chan nav-target args]
  (async/put! nav-chan [nav-target args]))

(defn define-general-routes! [nav-chan]
  (defroute root-path "/" []
            (send-nav! nav-chan :landing {:landing-data "POTATOES RULE"}))
  (defroute error-path "*" []
            (send-nav! nav-chan :error {:status 404})))

(defn define-routes! [state]
  (let [nav-ch (get-in @state [:comms :nav])]
    (define-general-routes! nav-ch)))

(defn parse-uri [uri]
  (let [[uri-path fragment] (str/split (sec/uri-without-prefix uri) "#")
        [uri-path query-string] (str/split uri-path  #"\?")
        uri-path (sec/uri-with-leading-slash uri-path)]
    [uri-path query-string fragment]))

(defn dispatch!
  "Dispatch an action for a given route if it matches the URI path."
  ;; Based on secretary.core: https://github.com/gf3/secretary/blob/579bc224f23e6c26a2299a2e5a48491fd3792faf/src/secretary/core.cljs#L314
  [uri]
  (println (str "dispatching  to " uri))
  (let [[uri-path query-string fragment] (parse-uri uri)
        query-params (when query-string
                       {:query-params (sec/decode-query-params query-string)})
        {:keys [action params]} (sec/locate-route uri-path)
        action (or action identity)
        params (merge params query-params {:_fragment fragment})]
    (action params)))