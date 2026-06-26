(ns app.rare.store-test
  (:require [cljs.test :refer [deftest is]]
            [app.rare.store :as store]))

(def schedule
  {:household {"monthly" [{:id "a" :anchor "Jun 1"}]}
   :digital   {"yearly"  [{:id "b" :anchor "Feb 1"}]}})

(def notes
  {"a" {:name "Clean the fridge"}})   ; "b" has no definition → id fallback

(deftest select-groups-rows-by-category
  (let [today  (js/Date. 2026 5 16) ;; 2026-06-16, local
        by-cat (store/select {:completions {} :outbox #{}} schedule notes today)]
    (is (= #{:household :digital} (set (keys by-cat)))
        "every category with rows is present")
    (is (every? #(= :household (:category %)) (:household by-cat))
        "rows land under their own category")))

(deftest select-joins-names-from-notes-with-the-id-as-fallback
  (let [today  (js/Date. 2026 5 16)
        by-cat (store/select {:completions {} :outbox #{}} schedule notes today)]
    (is (= "Clean the fridge" (:name (first (:household by-cat))))
        "a defined id renders its notes name")
    (is (= "b" (:name (first (:digital by-cat))))
        "an undefined id falls back to the id as its name")))

(deftest select-carries-note-from-notes-when-present
  (let [today  (js/Date. 2026 5 16)
        notes  {"a" {:name "Clean the fridge" :note "Empty shelves first"}}
        by-cat (store/select {:completions {} :outbox #{}} schedule notes today)]
    (is (= "Empty shelves first" (:note (first (:household by-cat))))
        "the task's note rides onto its row")
    (is (not (contains? (first (:digital by-cat)) :note))
        "a row whose task has no note carries no :note key")))

(deftest select-keys-done-through-strictly-by-id
  (let [today  (js/Date. 2026 5 16)
        by-cat (store/select {:completions {"a" "2026-06-01"} :outbox #{}}
                             schedule notes today)]
    (is (some :done? (:household by-cat))
        "a completion keyed by id marks the task's done row")))

(deftest toggle-dispatches-the-rows-set-done-through
  (let [row {:id "a" :set-done-through "2026-06-01"}]
    (is (= {:completions {"a" "2026-06-01"} :outbox #{"a"}}
           (store/toggle {:completions {} :outbox #{}} row))
        "writes the row's precomputed set-done-through and marks the id dirty")))
