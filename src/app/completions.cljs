(ns app.completions
  (:require [app.tasks :as tasks]
            [app.date-utils :refer [iso-date]]))

(defn covered? [completions id today]
  (when-let [through (get completions id)]
    (not (pos? (compare today through)))))

(defn toggle [completions schedule category-keys date id]
  (let [today (iso-date date)]
    (if (covered? completions id today)
      (assoc completions id (tasks/previous-occurrence schedule date id category-keys))
      (assoc completions id today))))
