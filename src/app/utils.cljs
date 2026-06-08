(ns app.utils)

(def weekday-kw [:sunday :monday :tuesday :wednesday :thursday :friday :saturday])

(defn iso-week [date]
  (let [d   (js/Date. (js/Date.UTC (.getFullYear date) (.getMonth date) (.getDate date)))
        day (let [g (.getUTCDay d)] (if (zero? g) 7 g))]
    (.setUTCDate d (+ (.getUTCDate d) (- 4 day)))      ; Thursday of this week
    (let [year-start (js/Date. (js/Date.UTC (.getUTCFullYear d) 0 1))]
      (js/Math.ceil (/ (inc (/ (- (.getTime d) (.getTime year-start)) 86400000)) 7)))))

(defn week-parity [date]
  (if (odd? (iso-week date)) :week-odd :week-even))
