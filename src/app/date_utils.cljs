(ns app.date-utils)

(def weekday-kw [:sunday :monday :tuesday :wednesday :thursday :friday :saturday])

(defn iso-date [date]
  (.toLocaleDateString date "en-CA"))   ; YYYY-MM-DD, local (not UTC)

(defn parse-month-day [date-str year]
  (js/Date. (str date-str " " year)))

(defn iso->date
  "Parse a \"YYYY-MM-DD\" string to a local-midnight Date, the local inverse of
   iso-date."
  [iso-str]
  (let [[y m d] (map js/parseInt (.split iso-str "-"))]
    (js/Date. y (dec m) d)))

(defn days-between
  "Whole calendar days from `from` to `to`, measured at local midnight so the
   count is timezone-stable and independent of the time of day."
  [from to]
  (let [mid  (fn [d] (.getTime (js/Date. (.getFullYear d) (.getMonth d) (.getDate d))))
        diff (- (mid to) (mid from))]
    (Math/round (/ diff 86400000))))

(defn- iso-week [date]
  (let [d   (js/Date. (js/Date.UTC (.getFullYear date) (.getMonth date) (.getDate date)))
        day (let [g (.getUTCDay d)] (if (zero? g) 7 g))]
    (.setUTCDate d (+ (.getUTCDate d) (- 4 day)))      ; Thursday of this week
    (let [year-start (js/Date. (js/Date.UTC (.getUTCFullYear d) 0 1))]
      (js/Math.ceil (/ (inc (/ (- (.getTime d) (.getTime year-start)) 86400000)) 7)))))

(defn week-parity [date]
  (if (odd? (iso-week date)) :week-odd :week-even))
