(ns torcaui-devcards.core
  (:require [devcards.core :as dc :include-macros true]
            [torcaui.core :as torcaui]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as sab :include-macros true])
  (:require-macros [devcards.core :refer [defcard defcard-om]]))

(enable-console-print!)

(defn main []
  (dc/start-devcard-ui!))
