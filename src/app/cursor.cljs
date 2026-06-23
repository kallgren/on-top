(ns app.cursor
  "The keyboard Cursor (see CONTEXT-MAP): a single point of attention held as
   per-surface state, dormant until a navigation key wakes it onto the first row.
   `next-cursor` is the pure movement decision; `use-list-cursor` wraps it in
   state plus the shared keybinding listeners and hands the view back the id of
   the row the Cursor is on. A view-layer concern only — inert on touch since the
   keys never fire."
  (:require [uix.core :refer [defhook use-state]]
            [app.keybinding :refer [use-hotkey]]))

;; ── Decision ─────────────────────────────────────────────────────────────────

(defn next-cursor
  "The cursor after a navigation key. `cursor` is the current row index or nil
   when dormant, `delta` is +1 (down) or -1 (up), `n` is the focusable row count.
   A dormant cursor wakes onto the first row; an awake one moves and clamps at
   both ends (no wrap). With no rows it stays dormant."
  [cursor delta n]
  (cond
    (zero? n)     nil
    (nil? cursor) 0
    :else         (-> (+ cursor delta) (max 0) (min (dec n)))))

;; ── Hook ─────────────────────────────────────────────────────────────────────

(defhook use-list-cursor
  "Owns one surface's cursor over `rows` (its ordered navigable rows, each with an
   :id) and toggles the row the Cursor is on via `toggle`. Only the `active?`
   surface's keys are live, so a single cursor is shared across surfaces. Returns
   the id of the row the Cursor is on, or nil while dormant or inactive."
  [rows toggle active?]
  (let [rows (vec rows)
        n    (count rows)
        [cursor set-cursor!] (use-state nil)]
    (use-hotkey "j" #(when active? (set-cursor! (fn [c] (next-cursor c 1 n)))))
    (use-hotkey "k" #(when active? (set-cursor! (fn [c] (next-cursor c -1 n)))))
    (use-hotkey "e" #(when active?
                       (when-let [row (get rows cursor)]
                         (toggle (:id row)))))
    (when active?
      (:id (get rows cursor)))))
