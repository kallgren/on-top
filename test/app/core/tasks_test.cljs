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

;; ── Enrichment ───────────────────────────────────────────────────────────────

(deftest name-for-looks-up-a-known-id
  (is (= "Gmail inbox" (tasks/name-for {"gmail" {:name "Gmail inbox"}} "gmail"))))

(deftest name-for-falls-back-to-the-id-for-an-unknown-id
  (is (= "gmail" (tasks/name-for {} "gmail")))
  (is (= "gmail" (tasks/name-for {"other" {:name "Other"}} "gmail"))))

(deftest enrich-joins-names-onto-id-only-tasks
  (is (= [{:category :digital :id "gmail" :name "Gmail inbox"}
          {:category :digital :id "unknown" :name "unknown"}]
         (tasks/enrich {"gmail" {:name "Gmail inbox"}}
                       [{:category :digital :id "gmail"}
                        {:category :digital :id "unknown"}]))))
