(ns torcaui.utils
  (:refer-clojure :exclude [uuid])
  (:require
    [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer close!]]
    [goog.Uri]
    [goog.async.AnimationDelay]
    [goog.crypt.Md5 :as md5]
    [goog.crypt :as crypt]
    [goog.dom :as dom]
    [goog.events :as ge]
    [goog.net.EventType :as gevt]
    [goog.string :as gstring]
    [goog.style :as style]
    [torcaui.config :as config]
    [torcaui.async :refer [raise! put!]]
    [om.core :as om :include-macros true]
    [clojure.string :as string]
    [sablono.core :as html :include-macros true]))


(defn third [coll]
  (nth coll 2 nil))

(defn js->clj-kw
  "Same as js->clj, but keywordizes-keys by default"
  [ds]
  (js->clj ds :keywordize-keys true))


(defn uuid
  "returns a type 4 random UUID: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx"
  []
  (let [r (repeatedly 30 (fn [] (.toString (rand-int 16) 16)))]
    (apply str (concat (take 8 r) ["-"]
                       (take 4 (drop 8 r)) ["-4"]
                       (take 3 (drop 12 r)) ["-"]
                       [(.toString  (bit-or 0x8 (bit-and 0x3 (rand-int 15))) 16)]
                       (take 3 (drop 15 r)) ["-"]
                       (take 12 (drop 18 r))))))

(defn deep-merge* [& maps]
  (let [f (fn [old new]
            (if (and (map? old) (map? new))
              (merge-with deep-merge* old new)
              new))]
    (if (every? map? maps)
      (apply merge-with f maps)
      (last maps))))

(defn deep-merge
  "Merge nested maps. At each level maps are merged left to right. When all
  maps have a common key whose value is also a map, those maps are merged
  recursively. If any of the values are not a map then the value from the
  right-most map is chosen.

  E.g.:
  user=> (deep-merge {:a {:b 1}} {:a {:c 3}})
  {:a {:c 3, :b 1}}

  user=> (deep-merge {:a {:b 1}} {:a {:b 2}})
  {:a {:b 2}}

  user=> (deep-merge {:a {:b 1}} {:a {:b {:c 4}}})
  {:a {:b {:c 4}}}

  user=> (deep-merge {:a {:b {:c 1}}} {:a {:b {:e 2 :c 15} :f 3}})
  {:a {:f 3, :b {:e 2, :c 15}}}

  Each of the arguments to this fn must be maps:

  user=> (deep-merge {:a 1} [1 2])
  AssertionError Assert failed: (and (map? m) (every? map? ms))

  Like merge, a key that maps to nil will override the same key in an earlier
  map that maps to a non-nil value:

  user=> (deep-merge {:a {:b {:c 1}, :d {:e 2}}}
                     {:a {:b nil, :d {:f 3}}})
  {:a {:b nil, :d {:f 3, :e 2}}}"
  [& maps]
  (let [maps (filter identity maps)]
    (assert (every? map? maps))
    (apply merge-with deep-merge* maps)))


(defn set-page-title! [& [title]]
  (set! (.-title js/document) (if title
                                (str title  " - TORCAUI")
                                "TORCAUI")))

(defn set-page-description!
  [description]
  (let [meta-el (.querySelector js/document "meta[name=description]")]
    (.setAttribute meta-el "content" (str description))))

(def parsed-uri
  (goog.Uri. (-> (.-location js/window) (.-href))))

(defn parse-uri-bool
  "Parses a boolean from a url into true, false, or nil"
  [string]
  (condp = string
    "true" true
    "false" false
    nil))

(def initial-query-map
  {:log-channels? (parse-uri-bool (.getParameterValue parsed-uri "log-channels"))
   :logging-enabled? (parse-uri-bool (.getParameterValue parsed-uri "logging-enabled"))})

(defn logging-enabled? []
  (let [from-query-map (:logging-enabled? initial-query-map)
        from-config (config/logging-enabled?)]
    (cond
      (= (type from-query-map) js/Boolean) from-query-map
      :else from-config)))

(defn mlog [& messages]
  (when (logging-enabled?)
    (.apply (.-log js/console) js/console (clj->js messages))))

(defn mwarn [& messages]
  (when (logging-enabled?)
    (.apply (.-warn js/console) js/console (clj->js messages))))

(defn merror [& messages]
  (when (logging-enabled?)
    (.apply (.-error js/console) js/console (clj->js messages))))

(def unexterned-prop aget)

(defn set-canonical!
  "Upserts a canonical URL if canonical-page is not nil, otherwise deletes the canonical rel."
  [canonical-page]
  (if-let [link-el (.querySelector js/document "link[rel=\"canonical\"]")]
    (if (some? canonical-page)
      (.setAttribute link-el "href" canonical-page)
      (dom/removeNode link-el))
    (when (some? canonical-page)
      (let [new-link-el (dom/createElement "link")]
        (.setAttribute new-link-el "rel" "canonical")
        (.setAttribute new-link-el "href" canonical-page)
        (dom/appendChild (.-head js/document) new-link-el)))))

(defn scroll-to-node-bottom [node]
  (when node
    (.scrollIntoView node false)))

(defn scroll-to-node!
  [node]
  (let [scrolling-element (dom/getDocumentScrollElement)
        node-top (style/getPageOffsetTop node)
        current-top (style/getPageOffsetTop scrolling-element)
        header-height (if-let [navbar (dom/getElementByClass "navbar-fixed-top")]
                        (+ (js/parseFloat (style/getComputedStyle navbar "marginTop"))
                           (.-offsetHeight navbar)
                           (js/parseFloat (style/getComputedStyle navbar "marginBottom")))
                        0)]
    (set! (.-scrollTop scrolling-element) (- node-top current-top header-height))))
;; Stores unique keys to uuids for the functions
(def debounce-state (atom {}))

(defn debounce
  "Takes a unique key and a function, will only execute the last function
   in a sliding 20ms interval (slightly longer than 16ms, time for rAF, seems to work best)"
  [unique-key f & {:keys [timeout]
                   :or {timeout 100}}]
  (js/clearTimeout (get @debounce-state unique-key))
  (let [timeout-id (js/setTimeout f timeout)]
    (swap! debounce-state assoc unique-key timeout-id)))

(defn edit-input
  "Meant to be used in a react event handler, usually for the :on-change event on input.
  Path is the vector of keys you would pass to assoc-in to change the value in state,
  event is the Synthetic React event. Pulls the value out of the event.
  Optionally takes :value as a keyword arg to override the event's value"
  [owner path event & {:keys [value]
                       :or {value (.. event -target -value)}}]
  (raise! owner [:edited-input {:path path :value value}]))

(defn toggle-input
  "Meant to be used in a react event handler, usually for the :on-change event on input.
  Path is the vector of keys you would pass to update-in to toggle the value in state,
  event is the Synthetic React event."
  [owner path event]
  (raise! owner [:toggled-input {:path path}]))

;; TODO: get rid of bootstrap popovers
(defn popover
  "Sets up a popover given selector and options. Once this is called, the popover
   should work as expected"
  [selector options]
  (mwarn "Please remove the popover on" selector)
  (let [jq (aget js/window "$")
        $node (jq selector)
        $popover (aget $node "popover")]
    (.call $popover $node (clj->js options))))

;; TODO: get rid of bootstrap tooltips
(defn tooltip
  "Sets up a tooltip given selector and options. Once this is called, the tooltip
   should work as expected"
  [selector & [options]]
  (mwarn "Please remove the tooltip on" selector)
  (let [jq (aget js/window "$")
        $node (jq selector)
        $tooltip (aget $node "tooltip")]
    (if options
      (.call $tooltip $node (clj->js options))
      (.call $tooltip $node))))

;; TODO: get rid of bootstrap typeahead
(defn typeahead
  "Sets up typahead given selector and options. Once this is called, typeahead
   should work as expected"
  [selector & [options]]
  (mwarn "Please remove typeahead on" selector)
  (let [jq (aget js/window "$")
        $node (jq selector)
        $typeahead (aget $node "typeahead")]
    (.call $typeahead $node (clj->js options))))

(defn rAF
  "Calls passed in function inside a requestAnimationFrame, falls back to timeouts for
   browers without requestAnimationFrame"
  [f]
  (.start (goog.async.AnimationDelay. f)))

(defn scroll-to-id!
  "Scrolls to the element with given id, if node exists"
  [id]
  (when-let [node (dom/getElement id)]
    (scroll-to-node! node)))

(defn scroll-to-selector!
  "Scrolls to the first element matching the selector, if node exists"
  [selector]
  (when-let [node (.querySelector js/document selector)]
    (scroll-to-node! node)))

(defn scroll!
  "Scrolls to fragment if the url had one, or scrolls to the top of the page"
  [args]
  (if (:_fragment args)
    ;; give the page time to render
    (rAF #(scroll-to-id! (:_fragment args)))
    (rAF #(set! (.-scrollTop (.-body js/document)) 0))))

(defn csrf-token []
  (aget js/window "CSRFToken"))

(defn oauth-csrf-token []
  (or (aget js/window "OAuthCSRFToken")
      (aget js/window "GitHubCSRFToken")))

(defn notify-error [ch message]
  (put! ch [:error-triggered message]))

(defn extract-access-token [params]
  (if params
    (let [access-token (re-matches #"^(?:access_token)=([a-zA-Z0-9]*)(?:&token_type=).*" params)]
      (if access-token
        (nth access-token 1)
        nil))))

(defn dasherize
  "Turns underscores and whitespaces into dashes."
  [s]
  (-> s
      (string/replace #" " "-")
      (string/replace #"_" "-")))