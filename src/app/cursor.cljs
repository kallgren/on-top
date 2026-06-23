(ns app.cursor
  "The keyboard Cursor (see CONTEXT-MAP): a single point of attention held as
   per-surface state, dormant until a navigation key wakes it. `next-cursor`,
   `clamp` and `wake` are the pure movement decisions; `use-list-cursor` wraps
   them in state plus the shared keybinding listeners, firing the shell's
   `on-exit-left` / `on-exit-right` on `h`/`l` so the shell owns crossing while
   the active Surface owns within-Pane keys. It hands the view back the row the
   Cursor is on. A view-layer concern only — inert on touch since the keys never
   fire."
  (:require [uix.core :refer [defhook use-state use-effect use-ref]]
            [app.keybinding :refer [use-hotkey]]))

;; The woken Cursor's look: the resting hover face plus a ring. One swappable
;; token — recolour the ring here and every Surface that lands the Cursor follows.
(def cursor-ring "ring-2 ring-cue")

;; ── Decisions ────────────────────────────────────────────────────────────────

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

(defn clamp
  "The cursor pinned inside a list of `n` rows. The same index when it still
   points at a row; the new last row when the list shrank under it — a toggled
   Rare row leaving the visible list (the row below shifts up into place), or a
   late Schedule load. nil when nothing is left, or the cursor was dormant."
  [cursor n]
  (when (and cursor (pos? n))
    (-> cursor (min (dec n)) (max 0))))

(defn wake
  "The cursor when a Pane is crossed into: the remembered row when it still
   exists, otherwise the first row — which also covers a never-visited Pane
   (dormant) and one whose old spot is gone. nil only when the Pane has no rows."
  [cursor n]
  (when (pos? n)
    (if (and cursor (< cursor n)) cursor 0)))

;; ── Hook ─────────────────────────────────────────────────────────────────────

(defhook use-list-cursor
  "Owns one Surface's cursor over `rows` (its ordered navigable rows) and toggles
   the row the Cursor is on via `(toggle row)`. Shell wiring rides in `opts`:
   `:active?` (only the active Surface binds keys, so a single cursor is shared),
   `:dormant?` (a hidden Pane forgets where the Cursor was), and `:on-exit-left`
   / `:on-exit-right` fired on `h`/`l`. Returns the row the Cursor is on, or nil
   while dormant or inactive."
  [rows toggle {:keys [active? dormant? on-exit-left on-exit-right]}]
  (let [rows (vec rows)
        n    (count rows)
        [cursor set-cursor!] (use-state nil)
        was-active? (use-ref active?)]
    (use-hotkey "j" #(when active? (set-cursor! (fn [c] (next-cursor c 1 n)))))
    (use-hotkey "k" #(when active? (set-cursor! (fn [c] (next-cursor c -1 n)))))
    (use-hotkey "e" #(when active?
                       (when-let [row (get rows cursor)]
                         (toggle row))))
    (use-hotkey "h" #(when (and active? on-exit-left) (on-exit-left)))
    (use-hotkey "l" #(when (and active? on-exit-right) (on-exit-right)))
    (use-hotkey "Escape" #(when active? (set-cursor! nil)))
    (use-effect
     ;; Crossing in (this Surface just became active): restore the remembered row
     ;; clamped to the current list, or land on the first row. The ref skips the
     ;; cold start, where the default Pane is active from mount but stays dormant.
     (fn []
       (when (and active? (not @was-active?))
         (set-cursor! (fn [c] (wake c n))))
       (reset! was-active? active?))
     [active? n])
    (use-effect
     ;; A hidden Pane forgets where the Cursor was, so revealing it starts fresh
     ;; rather than restoring a stale spot.
     (fn []
       (when dormant? (set-cursor! nil)))
     [dormant?])
    (use-effect
     ;; A shrinking list — a toggled Rare row leaving, a late Schedule load —
     ;; keeps the Cursor on a real row instead of pointing past the end.
     (fn []
       (set-cursor! (fn [c] (clamp c n))))
     [n])
    (use-effect
     ;; Reaching for the mouse dismisses the Cursor, the way clicking elsewhere
     ;; drops native focus. The listener only lives while the Cursor is awake,
     ;; so touch taps — where it never wakes — never pay for it.
     (fn []
       (when (and active? cursor)
         (let [dismiss #(set-cursor! nil)]
           (.addEventListener js/window "pointerdown" dismiss)
           #(.removeEventListener js/window "pointerdown" dismiss))))
     [active? cursor])
    (when active?
      (get rows cursor))))
