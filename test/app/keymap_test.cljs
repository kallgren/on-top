(ns app.keymap-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.keymap :as keymap]))

(deftest key-of-resolves-a-binding-to-its-key
  (testing "the handlers pull their key literal from here, never hardcode it"
    (is (= "j" (keymap/key-of :move-down)))
    (is (= "r" (keymap/key-of :toggle-rare)))
    (is (= "?" (keymap/key-of :help)))
    (is (= "Escape" (keymap/key-of :dismiss)))))

(deftest every-key-is-bound-once
  (testing "no two bindings claim the same key"
    (let [keys (map :key keymap/bindings)]
      (is (= (count keys) (count (distinct keys)))))))

(deftest every-binding-is-renderable
  (testing "the overlay can show any entry: a key cap and a description"
    (doseq [b keymap/bindings]
      (is (keyword? (:id b)))
      (is (seq (:key b)))
      (is (seq (:desc b))))))
