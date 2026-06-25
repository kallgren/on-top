(ns app.keybinding-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.keybinding :as kb]))

(defn- event [& {:keys [key meta ctrl alt]}]
  #js {:key key :metaKey (boolean meta) :ctrlKey (boolean ctrl) :altKey (boolean alt)})

(deftest hotkey-fires-on-bare-key
  (is (kb/hotkey? (event :key "r") "r" nil)))

(deftest hotkey-ignores-other-keys
  (is (not (kb/hotkey? (event :key "x") "r" nil))))

(deftest hotkey-ignores-modified-presses
  (testing "so Cmd+R / Ctrl+R reload still works"
    (is (not (kb/hotkey? (event :key "r" :meta true) "r" nil)))
    (is (not (kb/hotkey? (event :key "r" :ctrl true) "r" nil)))
    (is (not (kb/hotkey? (event :key "r" :alt true) "r" nil)))))

(deftest hotkey-suppressed-in-editable-fields
  (testing "guards the future settings view's inputs"
    (is (not (kb/hotkey? (event :key "r") "r" #js {:tagName "INPUT"})))
    (is (not (kb/hotkey? (event :key "r") "r" #js {:tagName "TEXTAREA"})))
    (is (not (kb/hotkey? (event :key "r") "r" #js {:isContentEditable true}))))
  (testing "a non-editable element does not suppress"
    (is (kb/hotkey? (event :key "r") "r" #js {:tagName "DIV"}))))
