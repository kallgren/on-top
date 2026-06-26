(ns app.core.tasks
  (:require [app.date-utils :refer [iso-date weekday-kw week-parity]]
            [app.notes :as notes]))

(defn tasks-for [schedule date category-keys]
  (let [parity (week-parity date)
        wd     (weekday-kw (.getDay date))]
    (for [cat category-keys
          id  (get-in schedule [cat parity wd])]
      {:category cat :id id})))

(defn todays-notes
  "Today's note-bearing Core Tasks as an ordered seq of `{:name :note}`, in Core
   list order (Category, then schedule order). Stable: it never reads completion,
   so the focus sheet holds still as Tasks are completed during a session."
  [schedule date category-keys notes]
  (->> (tasks-for schedule date category-keys)
       (notes/enrich notes)
       (filter :note)
       (map #(select-keys % [:name :note]))))

(defn- day-before [date]
  (js/Date. (.getFullYear date) (.getMonth date) (dec (.getDate date))))

(defn previous-occurrence [schedule date id category-keys]
  (loop [day (day-before date)
         n   14]
    (when (pos? n)
      (if (some #(= id (:id %)) (tasks-for schedule day category-keys))
        (iso-date day)
        (recur (day-before day) (dec n))))))
