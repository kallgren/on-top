(ns app.timer
  (:require [uix.core :refer [defui defhook $ use-state use-effect]]))

(def timer-secs (* 30 60))

(defn mmss [secs]
  (let [m (quot secs 60)
        s (mod secs 60)]
    (str m ":" (when (< s 10) "0") s)))

;; ── Controller ───────────────────────────────────────────────────────────────

(defhook use-timer
  "The app-level focus controller: holds whether a session is running and the
   ordered `{:name :note}` list it was started on. `start!` takes that payload so
   any trigger (the Go button, the toggle-timer hotkey) drops into focus on its
   own list. The presentational `timer` renders from this."
  []
  (let [[session set-session!] (use-state nil)]
    {:running? (some? session)
     :items    (:items session)
     :start!   (fn [items] (set-session! {:items (vec items)}))
     :stop!    (fn [] (set-session! nil))}))

;; ── Sheet ────────────────────────────────────────────────────────────────────

(defui note-block [{:keys [name note]}]
  ($ :div {:class "border-b border-edge/60 py-3 last:border-b-0"}
     ($ :h3 {:class "text-[20px] font-bold text-label first-letter:uppercase"} name)
     ($ :p {:class "mt-3 whitespace-pre-line text-[15px] leading-relaxed text-label"} note)))

(defui note-sheet [{:keys [items]}]
  ($ :div {:class (str "w-full max-w-md wide:max-w-3xl overflow-hidden "
                       "rounded-2xl border-2 border-edge bg-surface shadow-lg")}
     ($ :div {:class "max-h-[40vh] overflow-y-auto overscroll-contain px-7 pt-4 pb-5"}
        (for [{:keys [name note]} items]
          ($ note-block {:key name :name name :note note})))))

;; ── View ─────────────────────────────────────────────────────────────────────

(defui timer [{:keys [running? items on-go on-stop]}]
  (let [[end-at set-end-at] (use-state nil)
        [left set-left]     (use-state timer-secs)
        [notes-open? set-notes-open!] (use-state true)
        done? (and running? (zero? left))]
    (use-effect
     (fn []
       (if running?
         (do (set-left timer-secs)
             (set-end-at (+ (js/Date.now) (* timer-secs 1000)))
             (set-notes-open! true))
         (set-end-at nil))
       js/undefined)
     [running?])
    (use-effect
     (fn []
       (when end-at
         (let [tick (fn [] (set-left (max 0 (js/Math.round (/ (- end-at (js/Date.now)) 1000)))))]
           (tick)
           (let [id (js/setInterval tick 500)]
             #(js/clearInterval id)))))
     [end-at])
    (use-effect
     (fn []
       (when (and end-at (zero? left) (.-vibrate js/navigator))
         (.vibrate js/navigator 200))
       js/undefined)
     [end-at left])
    (if running?
      ($ :div {:class (str "fixed inset-x-0 bottom-0 z-20 flex flex-col items-center gap-3 "
                           "px-4 pb-[calc(1rem+env(safe-area-inset-bottom))]")}
         (when (and (seq items) notes-open?)
           ($ note-sheet {:items items}))
         ($ :div {:role "timer"
                  :class (str "w-full max-w-md wide:max-w-3xl overflow-hidden rounded-2xl border-2 shadow-lg "
                              (if done? "border-done" "border-edge"))}
            ($ :div {:class (str "flex items-center justify-between gap-4 "
                                 "px-7 pt-4 pb-4 "
                                 (if done? "bg-done" "bg-surface"))}
               ($ :span {:class (if done?
                                  "text-[19px] font-bold uppercase tracking-[0.2em] text-white"
                                  "text-[34px] font-bold tabular-nums tracking-wide text-label")}
                  (if done? "Time's up" (mmss left)))
               ($ :div {:class "flex items-center gap-1"}
                  (when (seq items)
                    ($ :button {:on-click #(set-notes-open! (not notes-open?))
                                :aria-expanded notes-open?
                                :aria-label (if notes-open? "Hide notes" "Show notes")
                                :class (str "flex h-11 w-11 shrink-0 cursor-pointer items-center justify-center rounded-full "
                                            "transition-colors "
                                            (if done? "text-white hover:bg-white/15" "text-muted hover:bg-edge/30"))}
                       ($ :svg {:viewBox "0 0 24 24"
                                :class (str "h-[20px] w-[20px] transition-transform " (when-not notes-open? "rotate-180"))
                                :fill "none" :stroke "currentColor" :stroke-width 2
                                :stroke-linecap "round" :stroke-linejoin "round"}
                          ($ :path {:d "M6 9l6 6 6-6"}))))
                  ($ :button {:on-click on-stop
                              :aria-label "Close timer"
                              :class (str "flex h-11 w-11 shrink-0 cursor-pointer items-center justify-center rounded-full "
                                          "text-[22px] leading-none transition-colors "
                                          (if done? "text-white hover:bg-white/15" "text-muted hover:bg-edge/30"))}
                     "✕")))))
      ($ :button {:on-click on-go
                  :aria-label "Start 30-minute timer"
                  :class (str "fixed bottom-[calc(1.75rem+env(safe-area-inset-bottom))] right-7 z-20 "
                              "flex h-16 w-16 items-center justify-center rounded-full "
                              "border-2 border-edge bg-surface text-muted "
                              "cursor-pointer shadow-lg transition hover:bg-surface-hover hover:text-label active:scale-95")}
         ($ :svg {:viewBox "0 0 24 24" :class "h-7 w-7" :fill "none" :stroke "currentColor"
                  :stroke-width 2 :stroke-linecap "round" :stroke-linejoin "round"}
            ($ :line {:x1 10 :y1 2 :x2 14 :y2 2})
            ($ :line {:x1 12 :y1 14 :x2 15 :y2 11})
            ($ :circle {:cx 12 :cy 14 :r 8}))))))
