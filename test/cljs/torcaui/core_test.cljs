(ns torcaui.core-test
  (:require-macros [cljs.test :refer [deftest testing is async]]
                   [dommy.core :refer [sel sel1]])
  (:require [cljs.test]
            [torcaui.core]
            [js.jquery]
            [torcaui.app :as app]
            [torcaui.routes :as routes]
            [torcaui.utils :as utils]
            [om.dom :as dom]
            [om.core :as om :include-macros true]
            [torcaui.components.aside :as aside]
            [dommy.core :refer [attr text] :refer-macros [sel sel1]]))

(defn insert-container! [container]
  (dommy.core/append! (sel1 js/document :body) container))


(defn new-container! []
  (let [id (str "container-" (gensym))
        n (.createElement js/document "DIV")]
    (set! (.-id n) id)
    (insert-container! n)
    (sel1 (str "#" id))))

(def jquery (js* "$"))

(defn x []
  (->
    (jquery "h1.class2")
    (jquery "span")
    (.text)))

(defn widget [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/h1 #js {:className (:class data)} nil (:text data)))))

(deftest alertbox-has-correct-message-text
  (let [c (new-container!)]
    (om/root widget {:class "class1" :text "Hello world!" :other "Potato"} {:target c})
    (is (= "Hello world!" (text (sel1 c :h1))))))

(deftest jquery-works
  (let [c (new-container!)]
    (om/root widget {:class "class2" :text "Hello world!" :other "Potato"} {:target c})
    (let [xv (x)]
      (is (= "Hello world!" xv)))))


(deftest test-pass []
                   (is (= 2 2)))

(deftest test-fail []
                   (is (= (+ 1 1) 2)))

(deftest hamburgers []
                    (is (= (:hamburger-menu @app/state) "closed")))



(deftest extract-token-extracts-valid-token
  (let [params "access_token=74652cc61e9cc798bdce5a7e3d71b5716b9e8f96c82eae8fc22ab36ad2e5db5d&token_type=bearer"
        access-token (utils/extract-access-token params)]
    (is (= access-token "74652cc61e9cc798bdce5a7e3d71b5716b9e8f96c82eae8fc22ab36ad2e5db5d"))))

(deftest extract-token-returns-nil-on-invalid-token
  (let [params "access_token=74652cc61e9cc798bdce5a7$%22&token_type=bearer"
        access-token (utils/extract-access-token params)]
    (is (= access-token nil))))


