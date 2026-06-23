(ns app.day.store
  "Day's surface logic over the shared store. Every Time block recurs every day,
   so coverage has no schedule walk: covering marks through today, unmarking rolls
   back to yesterday unconditionally. Open blocks are excluded from completion
   entirely. The date rollover is the reset — a Time block simply stops being
   covered once today moves past its Done-through (ADR 0006/0007/0009)."
  (:require [uix.core :refer [defhook use-state]]
            [app.date-utils :refer [iso-date]]
            [app.shared.store :as store]
            [app.storage :as storage]))

;; ── Coverage ─────────────────────────────────────────────────────────────────

(defn covered? [completions id today]
  (when-let [through (get completions id)]
    (not (pos? (compare today through)))))

(defn- yesterday [date]
  (iso-date (js/Date. (.getFullYear date) (.getMonth date) (dec (.getDate date)))))

(defn next-done-through [completions date id]
  (let [today (iso-date date)]
    (if (covered? completions id today)
      (yesterday date)
      today)))

(defn select [{:keys [completions]} schedule today]
  (let [today-key (iso-date today)]
    (for [{:keys [id open?] :as block} schedule]
      (if open?
        block
        (assoc block :done? (boolean (covered? completions id today-key)))))))

;; ── Wiring ───────────────────────────────────────────────────────────────────

(def completions-key "on-top/day-completions")
(def outbox-key "on-top/day-outbox")

(defn- read-initial []
  {:completions (or (storage/read-completions completions-key) {})
   :outbox      (or (storage/read-outbox outbox-key) #{})})

(defn- persist! [{:keys [completions outbox]}]
  (storage/write-completions! completions-key completions)
  (storage/write-outbox! outbox-key outbox))

(defhook use-store [today schedule]
  (let [[store] (use-state #(store/create (read-initial)))
        snapshot (store/use-subscribe store)]
    (store/use-sync! store snapshot
                     {:persist! persist! :creds store/creds :surface "day"} today)
    [(select snapshot schedule today)
     (fn [id]
       (swap! store store/toggled id
              (next-done-through (:completions @store) today id)))]))
