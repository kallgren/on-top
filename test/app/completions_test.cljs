(ns app.completions-test
  (:require [cljs.test :refer [deftest is]]
            [app.completions :as completions]))

(deftest covered?-when-done-through-reaches-today
  (is (completions/covered? {"gmail" "2026-06-18"} "gmail" "2026-06-18"))   ; done through today
  (is (completions/covered? {"gmail" "2026-06-20"} "gmail" "2026-06-18")))  ; done through a later day

(deftest not-covered?-once-the-day-passes-the-done-through
  (is (not (completions/covered? {"gmail" "2026-06-17"} "gmail" "2026-06-18"))))

(deftest not-covered?-without-a-record
  (is (not (completions/covered? {} "gmail" "2026-06-18"))))

(deftest toggle-marks-an-uncovered-task-through-today
  (let [schedule {:digital {:week-odd {:thursday [{:id "gmail" :text "Gmail"}]}}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: week-odd Thursday
    (is (= {"gmail" "2026-06-18"}
           (completions/toggle {} schedule [:digital] thursday "gmail")))))

(deftest toggle-unmarks-by-rolling-coverage-back-to-the-previous-occurrence
  (let [schedule {:digital {:week-odd {:monday   [{:id "gmail" :text "Gmail"}]
                                       :thursday [{:id "gmail" :text "Gmail"}]}}}
        thursday (js/Date. 2026 5 18)]            ; covered through today; prior occurrence is Mon 2026-06-15
    (is (= {"gmail" "2026-06-15"}
           (completions/toggle {"gmail" "2026-06-18"} schedule [:digital] thursday "gmail")))))
