(ns app.tasks
  (:require [app.date-utils :refer [iso-date weekday-kw week-parity]]))

(defn tasks-for [schedule date category-keys]
  (let [parity (week-parity date)
        wd     (weekday-kw (.getDay date))]
    (for [cat               category-keys
          {:keys [id text]} (get-in schedule [cat parity wd])]
      {:category cat :id id :text text})))

(defn- day-before [date]
  (js/Date. (.getFullYear date) (.getMonth date) (dec (.getDate date))))

(defn previous-occurrence [schedule date id category-keys]
  (loop [day (day-before date)
         n   14]
    (when (pos? n)
      (if (some #(= id (:id %)) (tasks-for schedule day category-keys))
        (iso-date day)
        (recur (day-before day) (dec n))))))
