(ns app.keymap
  "The one catalog of keyboard bindings (see CONTEXT-MAP's Cursor). Pure data: a
   binding's `:id` is referenced by the hook that wires its behavior, while `:key`,
   `:cap` and `:desc` drive the shortcuts overlay — so a binding and the help that
   documents it can never disagree about which key does what. `:cap` is the face
   shown on the key cap when it differs from the raw event key.")

(def bindings
  [{:id :move-down   :key "j" :desc "Move down"}
   {:id :move-up     :key "k" :desc "Move up"}
   {:id :cross-left  :key "h" :desc "Cross to the pane on the left"}
   {:id :cross-right :key "l" :desc "Cross to the pane on the right"}
   {:id :toggle-task :key "e" :desc "Toggle the task under the cursor"}
   {:id :dismiss     :key "Escape" :cap "Esc" :desc "Dismiss the cursor"}
   {:id :toggle-rare :key "r" :desc "Show or hide Rare"}
   {:id :help        :key "?" :desc "Show this shortcuts list"}])

(def ^:private by-id
  (into {} (map (juxt :id identity)) bindings))

(defn key-of [id]
  (:key (by-id id)))
