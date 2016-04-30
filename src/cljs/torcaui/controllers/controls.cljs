(ns torcaui.controllers.controls
  (:require [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer close!]]
            [cljs.reader :as reader]
            [torcaui.async :refer [put!]]
            [torcaui.app :refer [api-url]]
            [torcaui.state :as state]
            [torcaui.utils.ajax :as ajax]
            [torcaui.utils :as utils :include-macros true]
            [torcaui.utils.seq :refer [dissoc-in find-index]]
            [torcaui.utils.state :as state-utils]
            [torcaui.localstorage :as localstorage]
            [goog.dom]
            [goog.string :as gstring]
            [goog.labs.userAgent.engine :as engine]
            goog.style)
  (:require-macros [cljs.core.async.macros :as am :refer [go go-loop alt!]])
  (:import [goog.fx.dom.Scroll]))




