(ns app.layout
  "The shell's wide-screen Layout: which of the optional Panes (Day, Rare) are
   open beside the always-present Core, plus whether Rare's cleared cards
   (Categories with nothing Current) are revealed. Per-device preferences merged
   over defaults and persisted to localStorage; no part of the synced completion
   model. `use-layout` owns the state; `use-pane-cursor` borrows Rare's open
   state so it can keep the Cursor in step. While `suspended?` the `d` and `c`
   hotkeys are no-ops, so a face that has replaced the Panes cannot silently
   persist a Layout you cannot see and would exit into."
  (:require [uix.core :refer [defhook use-state use-effect use-callback]]
            [app.keybinding :as keybinding]
            [app.keymap :as keymap]
            [app.storage :as storage]))

(def defaults {:day false :rare true :cleared false})

(defn with-defaults [stored]
  (merge defaults stored))

(defhook use-layout [suspended?]
  (let [[layout set-layout!] (use-state #(with-defaults (storage/read-layout)))
        toggle-day     (use-callback #(set-layout! (fn [m] (update m :day not))) [])
        toggle-cleared (use-callback #(set-layout! (fn [m] (update m :cleared not))) [])
        set-rare!      (use-callback (fn [open?] (set-layout! #(assoc % :rare open?))) [])]
    (use-effect (fn [] (storage/write-layout! layout) js/undefined) [layout])
    (keybinding/use-hotkey (keymap/key-of :toggle-day) #(when-not suspended? (toggle-day)))
    (keybinding/use-hotkey (keymap/key-of :toggle-cleared) #(when-not suspended? (toggle-cleared)))
    {:day-open?     (:day layout)
     :rare-open?    (:rare layout)
     :show-cleared? (:cleared layout)
     :toggle-day    toggle-day
     :set-rare!     set-rare!}))
