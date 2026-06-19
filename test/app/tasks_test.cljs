(ns app.tasks-test
  (:require [cljs.test :refer [deftest is]]
            [app.tasks :as tasks]))

(deftest tasks-for-returns-the-days-scheduled-tasks
  (let [schedule {:digital   {:week-odd {:thursday ["Gmail"]}}
                  :household {:week-odd {:thursday ["Dishes"]}}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: ISO week 25 (odd), Thursday
    (is (= [{:category :digital   :name "Gmail"}
            {:category :household :name "Dishes"}]
           (tasks/tasks-for schedule thursday [:digital :household])))))

(deftest tasks-for-tracks-the-given-date
  (let [schedule {:digital {:week-odd  {:thursday ["Gmail"]}
                            :week-even {:monday   ["Calendar"]}}}
        thursday (js/Date. 2026 5 18)             ; 2026-06-18: week-odd, Thursday
        monday   (js/Date. 2026 5 22)]            ; 2026-06-22: week-even, Monday
    (is (= [{:category :digital :name "Gmail"}]
           (tasks/tasks-for schedule thursday [:digital])))
    (is (= [{:category :digital :name "Calendar"}]
           (tasks/tasks-for schedule monday [:digital])))))
