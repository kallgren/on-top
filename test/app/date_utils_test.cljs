(ns app.date-utils-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.date-utils :as dates]))

(deftest iso-date-formats-as-yyyy-mm-dd
  (is (= "2026-06-19" (dates/iso-date (js/Date. 2026 5 19))))
  (is (= "2026-03-05" (dates/iso-date (js/Date. 2026 2 5)))))    ; single-digit month/day zero-padded

(deftest iso-round-trips-locally
  (testing "iso->date is the local inverse of iso-date"
    (doseq [iso ["2026-01-01" "2026-06-15" "2026-12-31"]]
      (is (= iso (dates/iso-date (dates/iso->date iso)))))))

(deftest days-between-counts-calendar-days
  (let [d (fn [y m day] (js/Date. y (dec m) day))]
    (is (= 0  (dates/days-between (d 2026 6 15) (d 2026 6 15))) "same day")
    (is (= 1  (dates/days-between (d 2026 6 15) (d 2026 6 16))) "next day")
    (is (= -1 (dates/days-between (d 2026 6 16) (d 2026 6 15))) "previous day")
    (is (= 16 (dates/days-between (d 2026 6 15) (d 2026 7 1)))  "across a month")
    (testing "ignores time of day"
      (is (= 1 (dates/days-between (js/Date. 2026 5 15 23 0) (js/Date. 2026 5 16 1 0)))))))

(deftest week-parity-tracks-iso-week-parity
  (is (= :week-odd  (dates/week-parity (js/Date. 2026 5 18))))   ; 2026-06-18: ISO week 25
  (is (= :week-even (dates/week-parity (js/Date. 2026 5 22)))))  ; 2026-06-22: ISO week 26

(deftest week-parity-holds-across-the-year-boundary
  (is (= :week-odd  (dates/week-parity (js/Date. 2025 11 29))))  ; 2025-12-29: ISO week 1 of 2026
  (is (= :week-even (dates/week-parity (js/Date. 2026 0 5)))))   ; 2026-01-05: ISO week 2 of 2026
