(ns app.core.store
  "Core's surface logic over the shared store: the coverage predicate, the
   Done-through decision (covered? + previous-occurrence rollback, ADR 0006),
   the day's projection, and the toggle! adapter that dispatches the generic
   toggled event."
  (:require [uix.core :refer [defhook use-state]]
            [app.date-utils :refer [iso-date]]
            [app.shared.store :as store]
            [app.storage :as storage]
            [app.core.tasks :as tasks]))

;; ── Coverage ─────────────────────────────────────────────────────────────────

(defn covered? [completions id today]
  (when-let [through (get completions id)]
    (not (pos? (compare today through)))))

(defn next-done-through [completions schedule category-keys date id]
  (let [today (iso-date date)]
    (if (covered? completions id today)
      (tasks/previous-occurrence schedule date id category-keys)
      today)))

(defn select [{:keys [completions]} schedule today category-keys]
  (let [today-key (iso-date today)]
    (for [{:keys [id] :as task} (tasks/tasks-for schedule today category-keys)]
      (assoc task :done? (boolean (covered? completions id today-key))))))

;; ── Wiring ───────────────────────────────────────────────────────────────────

(def completions-key "on-top/core-completions")
(def outbox-key "on-top/core-outbox")

(defn- read-initial []
  {:completions (or (storage/read-completions completions-key) {})
   :outbox      (or (storage/read-outbox outbox-key) #{})})

(defn- persist! [{:keys [completions outbox]}]
  (storage/write-completions! completions-key completions)
  (storage/write-outbox! outbox-key outbox))

(defhook use-store [today schedule category-keys]
  (let [[store] (use-state #(store/create (read-initial)))
        snapshot (store/use-subscribe store)]
    (store/use-sync! store snapshot
                     {:persist! persist! :creds store/creds :surface "core"} today)
    [(select snapshot schedule today category-keys)
     (fn [id]
       (swap! store store/toggled id
              (next-done-through (:completions @store) schedule category-keys today id)))]))
