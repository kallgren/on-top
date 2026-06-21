(ns app.rare.store-test
  (:require [cljs.test :refer [deftest is]]
            [app.rare.store :as store]))

(def schedule
  {:household {"monthly" [{:id "a" :name "A" :anchor "Jun 1"}]}
   :digital   {"yearly"  [{:id "b" :name "B" :anchor "Feb 1"}]}})

(deftest select-groups-rows-by-category
  (let [today  (js/Date. 2026 5 16) ;; 2026-06-16, local
        by-cat (store/select {:completions {} :outbox #{}} schedule today)]
    (is (= #{:household :digital} (set (keys by-cat)))
        "every category with rows is present")
    (is (every? #(= :household (:category %)) (:household by-cat))
        "rows land under their own category")))

(deftest toggle-dispatches-the-rows-set-done-through
  (let [row {:id "a" :set-done-through "2026-06-01"}]
    (is (= {:completions {"a" "2026-06-01"} :outbox #{"a"}}
           (store/toggle {:completions {} :outbox #{}} row))
        "writes the row's precomputed set-done-through and marks the id dirty")))
