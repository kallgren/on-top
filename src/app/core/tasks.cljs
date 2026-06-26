(ns app.core.tasks
  (:require [app.date-utils :refer [iso-date weekday-kw week-parity]]))

(defn tasks-for [schedule date category-keys]
  (let [parity (week-parity date)
        wd     (weekday-kw (.getDay date))]
    (for [cat category-keys
          id  (get-in schedule [cat parity wd])]
      {:category cat :id id})))

(defn- day-before [date]
  (js/Date. (.getFullYear date) (.getMonth date) (dec (.getDate date))))

(defn previous-occurrence [schedule date id category-keys]
  (loop [day (day-before date)
         n   14]
    (when (pos? n)
      (if (some #(= id (:id %)) (tasks-for schedule day category-keys))
        (iso-date day)
        (recur (day-before day) (dec n))))))

;; ── Enrichment ───────────────────────────────────────────────────────────────

(defn name-for
  "The display name for a task id: the Notes lookup's name, or the id itself when
   the id has no definition (the id-fallback rule, applied in one place)."
  [notes id]
  (get-in notes [id :name] id))

(defn enrich
  "Join display names from the Notes lookup onto id-only tasks."
  [notes tasks]
  (map #(assoc % :name (name-for notes (:id %))) tasks))
