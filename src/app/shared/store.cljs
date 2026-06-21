(ns app.shared.store
  "Shared completion store: a pure reducer over {:completions :outbox} plus the
   atom store and subscription hook that adapt it to React, with fetch / flush /
   localStorage as effects around the pure events. See docs/adr/0007, #2."
  (:require [uix.core :refer [defhook use-state use-effect use-effect-event]]
            [app.sync :as sync]))

;; ── Events ───────────────────────────────────────────────────────────────────

(defn toggled [{:keys [completions outbox]} id value]
  {:completions (assoc completions id value)
   :outbox      (sync/mark-dirty outbox id)})

(defn hydrated [{:keys [completions outbox]} remote]
  {:completions (sync/reconcile remote (select-keys completions outbox))
   :outbox      outbox})

(defn flush-confirmed [{:keys [completions outbox]} confirmed]
  {:completions completions
   :outbox      (sync/clear-pending outbox confirmed)})

;; ── Adapter ──────────────────────────────────────────────────────────────────

(defn create [initial]
  (atom initial))

(defhook use-subscribe [store]
  (let [[snapshot set-snapshot!] (use-state #(deref store))]
    (use-effect
     (fn []
       (let [k (gensym "sub")]
         (add-watch store k (fn [_ _ _ next] (set-snapshot! next)))
         (set-snapshot! @store)
         #(remove-watch store k)))
     [store])
    snapshot))

(defhook use-sync! [store snapshot {:keys [persist! creds]} refresh-dep]
  (let [flush!   (use-effect-event
                  (fn []
                    (let [{:keys [completions outbox]} @store]
                      (when-let [{:keys [url key]} (creds)]
                        (when (seq outbox)
                          (sync/upsert-completions!
                           url key (sync/flush-payload outbox completions)
                           #(swap! store flush-confirmed outbox)))))))
        hydrate! (use-effect-event
                  (fn []
                    (when-let [{:keys [url key]} (creds)]
                      (sync/fetch-completions! url key #(swap! store hydrated %))
                      (flush!))))]
    (use-effect (fn [] (persist! snapshot) js/undefined) [snapshot persist!])
    (use-effect (fn [] (flush!) js/undefined) [(:outbox snapshot)])
    (use-effect (fn [] (hydrate!) js/undefined) [refresh-dep])
    nil))
