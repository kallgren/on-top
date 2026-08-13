(ns app.core.week-grid-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.date-utils :refer [iso-date iso-week week-parity]]
            [app.core.week-grid :as grid]))

(defn- every-day-of [year]
  (take-while #(= year (.getFullYear %))
              (iterate #(grid/add-days % 1) (js/Date. year 0 1))))

(deftest blocks-invariants
  (testing "over every day of 2026 and of the 53-week year 2026→2027 boundary"
    (doseq [year [2026 2027]
            today (every-day-of year)
            :let [[upper lower] (grid/blocks today)
                  upper-start (first (:dates upper))
                  lower-start (first (:dates lower))
                  this-monday (grid/monday-of today)]]
      (is (< (.getTime upper-start) (.getTime lower-start))
          (str (iso-date today) ": upper week must be earlier than lower"))
      (is (= 7 (js/Math.round (/ (- (.getTime lower-start) (.getTime upper-start)) 86400000)))
          (str (iso-date today) ": the two blocks must be adjacent weeks"))
      (is (#{(iso-date this-monday) (iso-date (grid/add-days this-monday -7))}
           (iso-date upper-start))
          (str (iso-date today) ": upper is this week or last week"))
      (is (#{(iso-date this-monday) (iso-date (grid/add-days this-monday 7))}
           (iso-date lower-start))
          (str (iso-date today) ": lower is this week or next week"))
      (is (= [true] (filter true? (map :current? [upper lower])))
          (str (iso-date today) ": exactly one block is this week"))
      (is (= (iso-date this-monday)
             (iso-date (first (:dates (first (filter :current? [upper lower]))))))
          (str (iso-date today) ": the current block is anchored to this week"))
      (is (= (week-parity today) (:parity (first (filter :current? [upper lower]))))
          (str (iso-date today) ": the current block carries today's parity"))
      (is (apply not= (map :parity [upper lower]))
          (str (iso-date today) ": the blocks are opposite parities"))
      (is (every? #(= (:parity %) (week-parity (first (:dates %)))) [upper lower])
          (str (iso-date today) ": each block's parity matches its real week")))))

(deftest schedule-order-holds-away-from-the-boundary
  (doseq [year [2026 2027]
          today (every-day-of year)
          :let [[upper lower] (grid/blocks today)]]
    (if (= 53 (iso-week today))
      (is (= [:week-even :week-odd] (map :parity [upper lower]))
          (str (iso-date today) ": at the 53-week boundary the pair stays ascending"))
      (is (= [:week-odd :week-even] (map :parity [upper lower]))
          (str (iso-date today) ": Schedule order — odd above even")))))
