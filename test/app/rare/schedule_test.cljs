(ns app.rare.schedule-test
  (:require [cljs.test :refer [deftest is]]
            [app.rare.schedule :as sch]))

(deftest deadline-row-carries-ready-due-label
  ;; A deadline task whose occurrence (Jun 18) is 3 days past `today` (Jun 15)
  ;; and inside its 30-day lead window, so it surfaces as current.
  (let [today (js/Date. 2026 5 15)
        task  {:id "tax" :anchor "Jun 18" :before 30}
        rows  (sch/task-rows :digital "yearly" task nil today)
        cur   (first (remove :done? rows))]
    (is (= "Due in 3 days" (:due-label cur))
        "the row carries the finished countdown, not a raw date")
    (is (not (contains? cur :deadline))
        "no raw deadline iso leaks to the view")))

(deftest non-deadline-row-has-no-due-label
  (let [today (js/Date. 2026 5 15)
        task  {:id "a" :anchor "Jun 1"} ; no :before
        rows  (sch/task-rows :household "monthly" task nil today)]
    (is (every? (comp nil? :due-label) rows))))

(deftest rows-carry-no-display-name
  (let [today (js/Date. 2026 5 15)
        task  {:id "a" :anchor "Jun 1"}
        rows  (sch/task-rows :household "monthly" task "2026-05-01" today)]
    (is (seq rows))
    (is (every? #(not (contains? % :name)) rows)
        "names are joined at the boundary, never sourced in derivation")
    (is (every? #(= "a" (:id %)) rows)
        "rows are keyed strictly by id")))
