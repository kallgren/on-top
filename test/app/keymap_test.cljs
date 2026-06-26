(ns app.keymap-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.keymap :as keymap]))

(deftest key-of-resolves-a-binding-to-its-key
  (testing "the handlers pull their key literal from here, never hardcode it"
    (is (= "j" (keymap/key-of :move-down)))
    (is (= "r" (keymap/key-of :toggle-rare)))
    (is (= "g" (keymap/key-of :toggle-timer)))
    (is (= "i" (keymap/key-of :toggle-note)))
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

(deftest every-binding-sits-in-a-known-group
  (let [group-ids (set (map :id keymap/groups))]
    (doseq [b keymap/bindings]
      (is (contains? group-ids (:group b))))))

(deftest no-group-renders-empty
  (testing "every section the overlay draws has at least one binding"
    (let [used (set (map :group keymap/bindings))]
      (doseq [g keymap/groups]
        (is (contains? used (:id g)))))))
