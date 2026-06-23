(ns app.day.view
  (:require [uix.core :refer [defui $ use-state use-effect]]
            [app.day.layout :as layout]
            [cljs.reader :as reader]
            [shadow.resource :as rc]))

;; ── Setup ────────────────────────────────────────────────────────────────────

(def seed-schedule (reader/read-string (rc/inline "app/day/seed.edn")))

(def column-width 248)
(def gutter-width 56)

(defn- now-minutes []
  (let [d (js/Date.)]
    (+ (* 60 (.getHours d)) (.getMinutes d))))

;; ── Components ───────────────────────────────────────────────────────────────

(defn- stacked-style [b]
  (let [t (:top b)]
    #js {:top (if (pos? t) (- t 2) t)
         :height (if (pos? t) (+ (:height b) 2) (:height b))}))

(defui gutter-times [{:keys [laid now-min current-id]}]
  ($ :<>
     (for [b laid]
       (let [past? (and (< (:e b) now-min) (not= (:id b) current-id))]
         ($ :div {:key (str "g" (:id b))
                  :class (str "absolute text-[12px] font-medium tabular-nums text-muted -translate-y-1/2 "
                              (when past? "opacity-40"))
                  :style #js {:top (:top b) :left 0 :width (- gutter-width 8)}}
            (layout/min-str (:s b)))))
     ($ :div {:class "absolute -translate-y-1/2 text-[12px] font-medium tabular-nums text-muted"
              :style #js {:top (layout/total-h laid) :left 0 :width (- gutter-width 8)}}
        (layout/min-str (:e (last laid))))))

(defui blocks [{:keys [laid now-min current-id]}]
  ($ :div {:class "absolute top-0 bottom-0" :style #js {:left gutter-width :right 0}}
     (for [b laid]
       (let [now? (= (:id b) current-id)
             past? (and (< (:e b) now-min) (not now?))
             open? (:open? b)]
         ($ :div {:key (:id b)
                  :class (str "absolute left-0 right-0 flex items-center justify-center "
                              "px-3 text-center select-none "
                              (if open?
                                (str "border-2 border-dashed bg-surface text-label "
                                     (if now? "border-now z-10 shadow-md " "border-edge "))
                                (str "border-2 "
                                     (if now?
                                       "bg-now border-now text-white z-10 shadow-md "
                                       "bg-surface border-edge text-label ")))
                              (when past? "opacity-40"))
                  :style (stacked-style b)}
            (when now?
              ($ :span {:class (str "absolute left-2 top-1.5 text-[10px] font-black uppercase tracking-[0.18em] "
                                    (if open? "text-now" "text-white"))}
                 "Now"))
            ($ :span {:class (str "text-[14px] font-semibold leading-tight " (when open? "opacity-60"))}
               (:name b)))))))

(defui now-line [{:keys [now-off now-min]}]
  (when now-off
    ($ :div {:class "pointer-events-none absolute left-0 right-0 z-20" :style #js {:top now-off}}
       ($ :div {:class "absolute -translate-y-1/2 text-[12px] font-bold tabular-nums text-now"
                :style #js {:left 0 :width (- gutter-width 8)}}
          (layout/min-str now-min))
       ($ :div {:class "absolute h-0.5 -translate-y-1/2 bg-now"
                :style #js {:left gutter-width :right 0}}))))

;; ── View ─────────────────────────────────────────────────────────────────────

(defui view []
  (let [[now-min] (use-state now-minutes)
        [vh set-vh!] (use-state #(.-innerHeight js/window))
        avail (max 300 (- vh 150))
        laid (layout/fit-layout seed-schedule avail)
        now-off (layout/offset-at laid now-min)
        current-id (:id (some #(when (and (>= now-min (:s %)) (< now-min (:e %))) %) laid))]
    (use-effect
     (fn []
       (let [on-resize #(set-vh! (.-innerHeight js/window))]
         (.addEventListener js/window "resize" on-resize)
         #(.removeEventListener js/window "resize" on-resize)))
     [])
    ($ :div {:class "no-scrollbar overflow-y-auto px-6 py-2" :style #js {:maxHeight avail}}
       ($ :div {:class "relative mx-auto" :style #js {:width column-width :height (layout/total-h laid)}}
          ($ gutter-times {:laid laid :now-min now-min :current-id current-id})
          ($ blocks {:laid laid :now-min now-min :current-id current-id})
          ($ now-line {:now-off now-off :now-min now-min})))))
