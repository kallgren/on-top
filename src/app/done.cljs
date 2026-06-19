(ns app.done
  (:require [app.tasks :as tasks]
            [app.utils :refer [iso-date]]))

(defn covered? [done id today]
  (when-let [through (get done id)]
    (not (pos? (compare today through)))))

(defn toggle [done schedule category-keys date id]
  (let [today (iso-date date)]
    (if (covered? done id today)
      (assoc done id (tasks/previous-occurrence schedule date id category-keys))
      (assoc done id today))))
