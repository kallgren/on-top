(ns app.rare.schedule-test
  (:require [cljs.test :refer [deftest is]]
            [app.rare.schedule :as sch]))

(deftest deadline-row-carries-ready-due-label
  ;; A deadline task whose occurrence (Jun 18) is 3 days past `today` (Jun 15)
  ;; and inside its 30-day lead window, so it surfaces as current.
  (let [today (js/Date. 2026 5 15)
        task  {:id "tax" :anchor "Jun 18" :lead-days 30}
        rows  (sch/task-rows :digital "yearly" task nil today)
        cur   (first (remove :done? rows))]
    (is (= "Due in 3 days" (:due-label cur))
        "the row carries the finished countdown, not a raw date")
    (is (not (contains? cur :deadline))
        "no raw deadline iso leaks to the view")))

(deftest non-deadline-row-has-no-due-label
  (let [today (js/Date. 2026 5 15)
        task  {:id "a" :anchor "Jun 1"} ; no :lead-days
        rows  (sch/task-rows :household "monthly" task nil today)]
    (is (every? (comp nil? :due-label) rows))))

(deftest due?-marks-deadlines-in-countdown-or-overdue
  (let [today (js/Date. 2026 5 15)
        cur   (fn [task] (first (remove :done? (sch/task-rows :digital "yearly" task nil today))))]
    (is (:due? (cur {:id "a" :anchor "Jun 18" :lead-days 30}))
        "occurrence 3 days out, inside its lead window — Due")
    (let [overdue (cur {:id "b" :anchor "Jun 5" :lead-days 7})]
      (is (:due? overdue) "occurrence 10 days past, not done — still Due")
      (is (nil? (:due-label overdue)) "overdue carries no countdown, only the Due flag"))
    (is (not (:due? (cur {:id "c" :anchor "Jul 5" :lead-days 30})))
        "occurrence 20 days out — current within lead window, but not yet Due")
    (is (not (:due? (cur {:id "d" :anchor "Jun 1"})))
        "an overdue *non-deadline* task is never Due")))

(deftest done-deadline-row-is-not-due
  (let [today (js/Date. 2026 5 15)
        task  {:id "tax" :anchor "Jun 18" :lead-days 30}
        rows  (sch/task-rows :digital "yearly" task "2026-06-18" today)]
    (is (every? (comp not :due?) rows)
        "a completed deadline carries no Due flag on any of its rows")))

(deftest rows-carry-no-display-name
  (let [today (js/Date. 2026 5 15)
        task  {:id "a" :anchor "Jun 1"}
        rows  (sch/task-rows :household "monthly" task "2026-05-01" today)]
    (is (seq rows))
    (is (every? #(not (contains? % :name)) rows)
        "names are joined at the boundary, never sourced in derivation")
    (is (every? #(= "a" (:id %)) rows)
        "rows are keyed strictly by id")))
