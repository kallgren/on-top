(ns app.day.store-test
  (:require [cljs.test :refer [deftest is]]
            [app.day.store :as store]))

(deftest covered?-when-done-through-reaches-today
  (is (store/covered? {"deep-work" "2026-06-18"} "deep-work" "2026-06-18"))   ; done through today
  (is (store/covered? {"deep-work" "2026-06-20"} "deep-work" "2026-06-18")))  ; done through a later day

(deftest not-covered?-once-the-day-passes-the-done-through
  (is (not (store/covered? {"deep-work" "2026-06-17"} "deep-work" "2026-06-18"))))

(deftest not-covered?-without-a-record
  (is (not (store/covered? {} "deep-work" "2026-06-18"))))

(deftest next-done-through-marks-an-uncovered-block-through-today
  (let [today (js/Date. 2026 5 18)]                ; 2026-06-18
    (is (= "2026-06-18" (store/next-done-through {} today "deep-work")))))

(deftest next-done-through-rolls-coverage-back-to-yesterday
  (let [today (js/Date. 2026 5 18)]                ; covered through today; yesterday is 2026-06-17
    (is (= "2026-06-17"
           (store/next-done-through {"deep-work" "2026-06-18"} today "deep-work")))))

(def schedule
  [{:id "deep-work" :name "Deep work" :start "07:00" :end "09:30"}
   {:id "free"      :name "Free"      :start "12:00" :end "17:30" :open? true}
   {:id "dinner"    :name "Dinner"    :start "17:30" :end "18:30"}])

(deftest select-projects-todays-blocks-marking-done-by-coverage
  (let [today (js/Date. 2026 5 18)]                ; 2026-06-18
    (is (= [{:id "deep-work" :name "Deep work" :start "07:00" :end "09:30" :done? true}
            {:id "free"      :name "Free"      :start "12:00" :end "17:30" :open? true}
            {:id "dinner"    :name "Dinner"    :start "17:30" :end "18:30" :done? false}]
           (store/select {:completions {"deep-work" "2026-06-18"} :outbox #{"deep-work"}}
                         schedule today)))))

(deftest select-excludes-open-blocks-from-completion
  (let [today (js/Date. 2026 5 18)
        free  (some #(when (= "free" (:id %)) %)
                    (store/select {:completions {} :outbox #{}} schedule today))]
    (is (not (contains? free :done?)) "Open blocks never carry a done-state")))
