(ns app.day.layout-test
  (:require [cljs.test :refer [deftest is]]
            [app.day.layout :as layout]))

(deftest ->min-reads-hh-mm-as-minutes-since-midnight
  (is (= 0 (layout/->min "00:00")))
  (is (= 330 (layout/->min "05:30")))
  (is (= 822 (layout/->min "13:42")))
  (is (= 1439 (layout/->min "23:59"))))

(deftest min-str-renders-minutes-as-hh-mm
  (is (= "0:00" (layout/min-str 0)))
  (is (= "5:30" (layout/min-str 330)))
  (is (= "10:00" (layout/min-str 600)))
  (is (= "13:42" (layout/min-str 822))))

(def two-hour-day
  [{:id "a" :start "09:00" :end "10:00"}
   {:id "b" :start "10:00" :end "11:00"}])

(deftest fit-layout-splits-height-proportionally-when-the-day-fits
  (let [laid (layout/fit-layout two-hour-day 200)]
    (is (= [100 100] (map :height laid)) "equal durations get equal heights")
    (is (= [0 100] (map :top laid)) "tops stack cumulatively")
    (is (= 200 (layout/total-h laid)) "the laid-out day fills the available height")))

(deftest fit-layout-floors-short-blocks-and-bends-the-rest-around-them
  (let [bs   [{:id "tiny" :start "10:00" :end "10:10"}
              {:id "b"    :start "10:10" :end "12:10"}
              {:id "c"    :start "12:10" :end "14:10"}]
        laid (layout/fit-layout bs 300)
        by   (into {} (map (juxt :id identity) laid))]
    (is (= 44 (:height (by "tiny"))) "the short block is floored at 44px")
    (is (= 128 (:height (by "b"))) "the remaining height is split proportionally among the rest")
    (is (= 128 (:height (by "c"))))
    (is (= [0 44 172] (map :top laid)) "tops still stack cumulatively over the floored block")
    (is (= 300 (layout/total-h laid)) "the floored day still fills the available height")))

(deftest offset-at-interpolates-the-now-line-within-the-spanning-block
  (let [laid (layout/fit-layout two-hour-day 200)]
    (is (= 0 (layout/offset-at laid 540)) "the first block's start sits at the top")
    (is (= 50 (layout/offset-at laid 570)) "halfway through the first hour is halfway down its box")
    (is (= 200 (layout/offset-at laid 660)) "the last block's end sits at the bottom")))

(deftest offset-at-is-absent-outside-the-days-span
  (let [laid (layout/fit-layout two-hour-day 200)]
    (is (nil? (layout/offset-at laid 480)) "before the first start there is no now-line")
    (is (nil? (layout/offset-at laid 720)) "after the last end there is no now-line")))

(deftest total-h-of-an-empty-layout-is-zero
  (is (= 0 (layout/total-h []))))
