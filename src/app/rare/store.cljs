(ns app.rare.store
  "Rare's surface logic over the shared store: the day's projection grouping
   collapsed Current / Upcoming / Missed rows by category, and the toggle adapter
   that dispatches the generic toggled event with the row's precomputed
   set-done-through. See app.rare.schedule for the placement model."
  (:require [uix.core :refer [defhook use-state]]
            [app.rare.schedule :as schedule]
            [app.shared.store :as store]
            [app.storage :as storage]))

;; ── Projection ───────────────────────────────────────────────────────────────

(defn select [{:keys [completions]} schedule today]
  (group-by :category (schedule/derive-schedule schedule completions today)))

;; ── Toggle ───────────────────────────────────────────────────────────────────

(defn toggle [state {:keys [id set-done-through]}]
  (store/toggled state id set-done-through))

;; ── Wiring ───────────────────────────────────────────────────────────────────

(def completions-key "on-top/rare-completions")
(def outbox-key "on-top/rare-outbox")

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
                     {:persist! persist! :creds store/creds :surface "rare"} today)
    [(select snapshot schedule today)
     (fn [row] (swap! store toggle row))]))
