(ns app.core.store-test
  (:require [cljs.test :refer [deftest is]]
            [app.core.store :as store]))

(deftest covered?-when-done-through-reaches-today
  (is (store/covered? {"gmail" "2026-06-18"} "gmail" "2026-06-18"))   ; done through today
  (is (store/covered? {"gmail" "2026-06-20"} "gmail" "2026-06-18")))  ; done through a later day

(deftest not-covered?-once-the-day-passes-the-done-through
  (is (not (store/covered? {"gmail" "2026-06-17"} "gmail" "2026-06-18"))))

(deftest not-covered?-without-a-record
  (is (not (store/covered? {} "gmail" "2026-06-18"))))

(deftest next-done-through-marks-an-uncovered-task-through-today
  (let [schedule {:digital {:week-odd {:thursday [{:id "gmail" :text "Gmail"}]}}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: week-odd Thursday
    (is (= "2026-06-18"
           (store/next-done-through {} schedule [:digital] thursday "gmail")))))

(deftest next-done-through-rolls-coverage-back-to-the-previous-occurrence
  (let [schedule {:digital {:week-odd {:monday   [{:id "gmail" :text "Gmail"}]
                                       :thursday [{:id "gmail" :text "Gmail"}]}}}
        thursday (js/Date. 2026 5 18)]            ; covered through today; prior occurrence is Mon 2026-06-15
    (is (= "2026-06-15"
           (store/next-done-through {"gmail" "2026-06-18"} schedule [:digital] thursday "gmail")))))

(deftest select-projects-todays-tasks-each-marked-done-by-coverage
  (let [schedule {:digital   {:week-odd {:thursday [{:id "gmail" :text "Gmail"}]}}
                  :household {:week-odd {:thursday [{:id "dishes" :text "Dishes"}]}}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: week-odd Thursday
    (is (= [{:category :digital   :id "gmail"  :text "Gmail"  :done? true}
            {:category :household :id "dishes" :text "Dishes" :done? false}]
           (store/select {:completions {"gmail" "2026-06-18"} :outbox #{"gmail"}}
                         schedule thursday [:digital :household])))))
