(ns torcaui.funcs
  (:require [goog.dom :as gdom]
            [cognitect.transit :as t]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devcards.core :as dc :refer-macros [defcard deftest]]))

(enable-console-print!)

(defn potato [act]
  (enable-console-print!)
  (println act))
