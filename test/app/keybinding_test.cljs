(ns app.keybinding-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.keybinding :as kb]))

(defn- event [& {:keys [key meta ctrl alt]}]
  #js {:key key :metaKey (boolean meta) :ctrlKey (boolean ctrl) :altKey (boolean alt)})

(deftest hotkey-fires-on-bare-key
  (is (kb/hotkey? (event :key "r") "r" nil false)))

(deftest hotkey-ignores-other-keys
  (is (not (kb/hotkey? (event :key "x") "r" nil false))))

(deftest hotkey-ignores-modified-presses
  (testing "so Cmd+R / Ctrl+R reload still works"
    (is (not (kb/hotkey? (event :key "r" :meta true) "r" nil false)))
    (is (not (kb/hotkey? (event :key "r" :ctrl true) "r" nil false)))
    (is (not (kb/hotkey? (event :key "r" :alt true) "r" nil false)))))

(deftest hotkey-suppressed-in-editable-fields
  (testing "guards the future settings view's inputs"
    (is (not (kb/hotkey? (event :key "r") "r" #js {:tagName "INPUT"} false)))
    (is (not (kb/hotkey? (event :key "r") "r" #js {:tagName "TEXTAREA"} false)))
    (is (not (kb/hotkey? (event :key "r") "r" #js {:isContentEditable true} false))))
  (testing "a non-editable element does not suppress"
    (is (kb/hotkey? (event :key "r") "r" #js {:tagName "DIV"} false))))

(deftest hotkey-suppressed-while-an-overlay-captures-keys
  (testing "the shortcuts overlay (or any key-capturing layer) silences app keys"
    (is (not (kb/hotkey? (event :key "r") "r" nil true))))
  (testing "with no overlay the key fires"
    (is (kb/hotkey? (event :key "r") "r" nil false))))
