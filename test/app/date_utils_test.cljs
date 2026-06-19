(ns app.date-utils-test
  (:require [cljs.test :refer [deftest is]]
            [app.date-utils :as dates]))

(deftest iso-date-formats-as-yyyy-mm-dd
  (is (= "2026-06-19" (dates/iso-date (js/Date. 2026 5 19))))
  (is (= "2026-03-05" (dates/iso-date (js/Date. 2026 2 5)))))    ; single-digit month/day zero-padded

(deftest week-parity-tracks-iso-week-parity
  (is (= :week-odd  (dates/week-parity (js/Date. 2026 5 18))))   ; 2026-06-18: ISO week 25
  (is (= :week-even (dates/week-parity (js/Date. 2026 5 22)))))  ; 2026-06-22: ISO week 26

(deftest week-parity-holds-across-the-year-boundary
  (is (= :week-odd  (dates/week-parity (js/Date. 2025 11 29))))  ; 2025-12-29: ISO week 1 of 2026
  (is (= :week-even (dates/week-parity (js/Date. 2026 0 5)))))   ; 2026-01-05: ISO week 2 of 2026
