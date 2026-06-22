(ns app.timer
  (:require [uix.core :refer [defui $ use-state use-effect]]))

(def timer-secs (* 30 60))

(defn mmss [secs]
  (let [m (quot secs 60)
        s (mod secs 60)]
    (str m ":" (when (< s 10) "0") s)))

(defui timer [{:keys [start-hidden?]}]
  (let [[end-at set-end-at] (use-state nil)
        [left set-left]     (use-state timer-secs)
        done? (and (some? end-at) (zero? left))]
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
    (if end-at
      ($ :div {:role "timer"
               :class (str "fixed inset-x-0 bottom-0 z-20 flex items-center justify-between gap-4 "
                           "border-t-2 px-7 pt-4 pb-[calc(1rem+env(safe-area-inset-bottom))] "
                           (if done? "bg-done border-done" "bg-surface border-edge"))}
         ($ :span {:class (if done?
                            "text-[19px] font-bold uppercase tracking-[0.2em] text-white"
                            "text-[34px] font-bold tabular-nums tracking-wide text-label")}
            (if done? "Time's up" (mmss left)))
         ($ :button {:on-click #(set-end-at nil)
                     :aria-label "Close timer"
                     :class (str "flex h-11 w-11 shrink-0 cursor-pointer items-center justify-center rounded-full "
                                 "text-[22px] leading-none transition-colors "
                                 (if done? "text-white hover:bg-white/15" "text-muted hover:bg-edge/30"))}
            "✕"))
      ($ :button {:on-click #(do (set-left timer-secs)
                                 (set-end-at (+ (js/Date.now) (* timer-secs 1000))))
                  :aria-label "Start 30-minute timer"
                  :class (str "fixed bottom-[calc(1.75rem+env(safe-area-inset-bottom))] right-7 z-20 "
                              "flex h-16 w-16 items-center justify-center rounded-full "
                              "border border-edge/50 bg-surface/80 text-muted backdrop-blur-md "
                              "text-[22px] font-bold tracking-wide "
                              "cursor-pointer shadow-sm transition hover:bg-surface-hover/80 active:scale-95 "
                              (if start-hidden? "pointer-events-none opacity-0" "opacity-100"))}
         "Go"))))
