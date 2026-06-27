(ns app.layout
  "The shell's wide-screen Layout: which of the optional Panes (Day, Rare) are
   open beside the always-present Core. A per-device preference merged over
   defaults and persisted to localStorage; no part of the synced completion
   model. `use-layout` owns the state; `use-pane-cursor` borrows Rare's open
   state so it can keep the Cursor in step."
  (:require [uix.core :refer [defhook use-state use-effect use-callback]]
            [app.keybinding :as keybinding]
            [app.keymap :as keymap]
            [app.storage :as storage]))

(def defaults {:day false :rare true})

(defn with-defaults [stored]
  (merge defaults stored))

(defhook use-layout []
  (let [[layout set-layout!] (use-state #(with-defaults (storage/read-layout)))
        toggle-day (use-callback #(set-layout! (fn [m] (update m :day not))) [])
        set-rare!  (use-callback (fn [open?] (set-layout! #(assoc % :rare open?))) [])]
    (use-effect (fn [] (storage/write-layout! layout) js/undefined) [layout])
    (keybinding/use-hotkey (keymap/key-of :toggle-day) toggle-day)
    {:day-open?  (:day layout)
     :rare-open? (:rare layout)
     :toggle-day toggle-day
     :set-rare!  set-rare!}))
