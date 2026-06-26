(ns app.core.tasks-test
  (:require [cljs.test :refer [deftest is]]
            [app.core.tasks :as tasks]))

(deftest tasks-for-returns-the-days-scheduled-ids
  (let [schedule {:digital   {:week-odd {:thursday ["gmail"]}}
                  :household {:week-odd {:thursday ["dishes"]}}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: ISO week 25 (odd), Thursday
    (is (= [{:category :digital   :id "gmail"}
            {:category :household :id "dishes"}]
           (tasks/tasks-for schedule thursday [:digital :household])))))

(deftest tasks-for-tracks-the-given-date
  (let [schedule {:digital {:week-odd  {:thursday ["gmail"]}
                            :week-even {:monday   ["calendar"]}}}
        thursday (js/Date. 2026 5 18)             ; 2026-06-18: week-odd, Thursday
        monday   (js/Date. 2026 5 22)]            ; 2026-06-22: week-even, Monday
    (is (= [{:category :digital :id "gmail"}]
           (tasks/tasks-for schedule thursday [:digital])))
    (is (= [{:category :digital :id "calendar"}]
           (tasks/tasks-for schedule monday [:digital])))))

(deftest todays-notes-returns-note-bearing-tasks-in-core-list-order
  (let [schedule {:digital   {:week-odd {:thursday ["gmail" "downloads"]}}
                  :household {:week-odd {:thursday ["dishes" "dust"]}}}
        notes    {"gmail" {:name "Gmail inbox" :note "Snooze the rest"}
                  "dishes" {:name "Dishes"}
                  "dust"  {:name "Dust" :note "Top of the shelves too"}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: week-odd, Thursday
    (is (= [{:name "Gmail inbox" :note "Snooze the rest"}
            {:name "Dust" :note "Top of the shelves too"}]
           (tasks/todays-notes schedule thursday [:digital :household] notes)))))

(deftest todays-notes-is-empty-when-no-scheduled-task-has-a-note
  (let [schedule {:digital {:week-odd {:thursday ["gmail" "downloads"]}}}
        notes    {"gmail" {:name "Gmail inbox"}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: week-odd, Thursday
    (is (empty? (tasks/todays-notes schedule thursday [:digital] notes)))))

(deftest previous-occurrence-finds-the-most-recent-prior-scheduled-day
  (let [schedule {:digital {:week-odd {:monday   ["gmail"]
                                       :thursday ["gmail"]}}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: week-odd Thursday
    (is (= "2026-06-15"                           ; the Monday of the same week
           (tasks/previous-occurrence schedule thursday "gmail" [:digital])))))

(deftest previous-occurrence-reaches-back-a-full-fortnight
  (let [schedule {:household {:week-odd {:thursday ["dust"]}}}
        thursday (js/Date. 2026 5 18)]            ; 2026-06-18: fortnightly, prior is 14 days back
    (is (= "2026-06-04"
           (tasks/previous-occurrence schedule thursday "dust" [:household])))))
