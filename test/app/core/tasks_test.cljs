(ns app.core.tasks-test
  (:require [cljs.test :refer [deftest is]]
            [app.core.tasks :as tasks]))

(deftest tasks-for-returns-the-days-scheduled-tasks
  (let [schedule {:digital   {:week-odd {:thursday [{:id "gmail" :name "Gmail"}]}}
                  :household {:week-odd {:thursday [{:id "dishes" :name "Dishes"}]}}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: ISO week 25 (odd), Thursday
    (is (= [{:category :digital   :id "gmail"  :name "Gmail"}
            {:category :household :id "dishes" :name "Dishes"}]
           (tasks/tasks-for schedule thursday [:digital :household])))))

(deftest tasks-for-tracks-the-given-date
  (let [schedule {:digital {:week-odd  {:thursday [{:id "gmail" :name "Gmail"}]}
                            :week-even {:monday   [{:id "calendar" :name "Calendar"}]}}}
        thursday (js/Date. 2026 5 18)             ; 2026-06-18: week-odd, Thursday
        monday   (js/Date. 2026 5 22)]            ; 2026-06-22: week-even, Monday
    (is (= [{:category :digital :id "gmail" :name "Gmail"}]
           (tasks/tasks-for schedule thursday [:digital])))
    (is (= [{:category :digital :id "calendar" :name "Calendar"}]
           (tasks/tasks-for schedule monday [:digital])))))

(deftest previous-occurrence-finds-the-most-recent-prior-scheduled-day
  (let [schedule {:digital {:week-odd {:monday   [{:id "gmail" :name "Gmail"}]
                                       :thursday [{:id "gmail" :name "Gmail"}]}}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: week-odd Thursday
    (is (= "2026-06-15"                           ; the Monday of the same week
           (tasks/previous-occurrence schedule thursday "gmail" [:digital])))))

(deftest previous-occurrence-reaches-back-a-full-fortnight
  (let [schedule {:household {:week-odd {:thursday [{:id "dust" :name "Dust"}]}}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: fortnightly, prior is 14 days back
    (is (= "2026-06-04"
           (tasks/previous-occurrence schedule thursday "dust" [:household])))))

