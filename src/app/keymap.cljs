(ns app.keymap
  "The one catalog of keyboard bindings (see CONTEXT-MAP's Cursor). Pure data: a
   binding's `:id` is referenced by the hook that wires its behavior, while `:key`,
   `:cap`, `:desc` and `:group` drive the shortcuts overlay — so a binding and the
   help that documents it can never disagree about which key does what. `:cap` is
   the face shown on the key cap when it differs from the raw event key. `groups`
   fixes the overlay's section order and labels.")

(def groups
  [{:id :navigation :label "Navigation"}
   {:id :actions    :label "Actions"}
   {:id :general    :label "General"}])

(def bindings
  [{:id :move-down    :key "j" :group :navigation :desc "Move down"}
   {:id :move-up      :key "k" :group :navigation :desc "Move up"}
   {:id :cross-left   :key "h" :group :navigation :desc "Cross to the pane on the left"}
   {:id :cross-right  :key "l" :group :navigation :desc "Cross to the pane on the right"}
   {:id :toggle-task  :key "e" :group :actions :desc "Toggle the task under the cursor"}
   {:id :toggle-rare  :key "r" :group :actions :desc "Show or hide the Rare pane"}
   {:id :toggle-timer :key "g" :group :actions :desc "Start or stop the timer"}
   {:id :toggle-note  :key "i" :group :actions :desc "Show or hide the note under the cursor"}
   {:id :dismiss      :key "Escape" :cap "Esc" :group :general :desc "Dismiss the cursor"}
   {:id :help         :key "?" :group :general :desc "Show this shortcuts list"}])

(def ^:private by-id
  (into {} (map (juxt :id identity)) bindings))

(defn key-of [id]
  (:key (by-id id)))
