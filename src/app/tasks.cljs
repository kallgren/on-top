(ns app.tasks
  (:require [app.utils :refer [weekday-kw week-parity]]))

(defn tasks-for [schedule date category-keys]
  (let [parity (week-parity date)
        wd     (weekday-kw (.getDay date))]
    (for [cat  category-keys
          name (get-in schedule [cat parity wd])]
      {:category cat :name name})))
