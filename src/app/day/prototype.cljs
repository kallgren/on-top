(ns app.day.prototype
  "THROWAWAY UI prototype — the chosen Day timetable look.
   The whole day fits the viewport (water-filled, 44px floor); the current Block
   fills blue, past Blocks dim, 'Free' is a dashed open-time special case. No
   stores, no sync, no persistence: blocks are hardcoded, done-state is in-memory.
   Fold the validated decisions into the real day/view.cljs, then delete this.
   Run: `pnpm prototype`  →  http://localhost:8081/day-prototype.html"
  (:require [uix.core :refer [defui $ use-state use-effect use-ref]]
            [uix.dom]))

;; ── Hardcoded day (no seed.edn, no fetch) ────────────────────────────────────

(def blocks
  [{:id "dw"       :name "DW"         :start "05:00" :end "09:30"}
   {:id "nm"       :name "NM"         :start "09:30" :end "10:30"}
   {:id "core"     :name "Core tasks" :start "10:30" :end "11:00"}
   {:id "todoist"  :name "Todoist"    :start "11:00" :end "11:30"}
   {:id "messages" :name "Messages"   :start "11:30" :end "12:00"}
   {:id "free"     :name "Free"       :start "12:00" :end "19:30"}
   {:id "wind"     :name "Wind down"  :start "19:30" :end "20:30"}])

(def min-h 44)    ;; short-block height floor
(def gutter 56)   ;; left time-column width

;; ── Time helpers ─────────────────────────────────────────────────────────────

(defn ->min [s]
  (let [[h m] (map js/parseInt (.split s ":"))]
    (+ (* 60 h) m)))

(defn min-str [m]
  (let [h (quot m 60) mm (mod m 60)]
    (str h ":" (when (< mm 10) "0") mm)))

(defn total-h [laid]
  (if (seq laid) (+ (:top (last laid)) (:height (last laid))) 0))

(defn offset-at [laid m]
  (some (fn [b]
          (when (and (>= m (:s b)) (<= m (:e b)))
            (+ (:top b) (* (/ (- m (:s b)) (- (:e b) (:s b))) (:height b)))))
        laid))

(defn now-minutes [bs]
  (let [d (js/Date.)
        live (+ (* 60 (.getHours d)) (.getMinutes d))
        lo (->min (:start (first bs))) hi (->min (:end (last bs)))]
    (if (and (>= live lo) (<= live hi)) live 822)))  ;; 13:42 demo when off-span

(defn fit-layout [bs avail]
  (let [base (map (fn [b]
                    (let [s (->min (:start b)) e (->min (:end b))]
                      (assoc b :s s :e e :dur (- e s))))
                  bs)
        floored (loop [fixed #{}]
                  (let [free (remove #(fixed (:id %)) base)
                        a (max 0 (- avail (* min-h (count fixed))))
                        free-dur (reduce + (map :dur free))
                        newly (keep (fn [b]
                                      (when (< (/ (* (:dur b) a) (max 1 free-dur)) min-h)
                                        (:id b)))
                                    free)]
                    (if (and (seq newly) (< (count fixed) (count base)))
                      (recur (into fixed newly))
                      fixed)))
        a (max 0 (- avail (* min-h (count floored))))
        free-dur (reduce + (map :dur (remove #(floored (:id %)) base)))
        height-of (fn [b] (if (floored (:id b))
                            min-h
                            (/ (* (:dur b) a) (max 1 free-dur))))]
    (loop [bs base top 0 acc []]
      (if (empty? bs)
        acc
        (let [b (first bs) h (height-of b)]
          (recur (rest bs) (+ top h) (conj acc (assoc b :top top :height h))))))))

;; ── Block style helpers ──────────────────────────────────────────────────────

(def now-text "text-[#1f6feb] dark:text-[#5b9dff]")
(def now-bg "bg-[#1f6feb] dark:bg-[#5b9dff]")

(defn stacked-style [b]
  (let [t (:top b)]
    #js {:top (if (pos? t) (- t 2) t)
         :height (if (pos? t) (+ (:height b) 2) (:height b))}))

;; ── Timetable ────────────────────────────────────────────────────────────────

(defui timetable [{:keys [laid now-off now-min done toggle now-ref]}]
  (let [current-id (:id (some #(when (and (>= now-min (:s %)) (< now-min (:e %))) %) laid))]
    ($ :div {:class "relative mx-auto w-2/3" :style #js {:height (total-h laid)}}
       (for [b laid]
         (let [past? (and now-min (< (:e b) now-min) (not= (:id b) current-id))]
           ($ :div {:key (str "t" (:id b))
                    :class (str "absolute text-[12px] font-medium tabular-nums text-muted -translate-y-1/2 "
                                (when past? "opacity-40"))
                    :style #js {:top (:top b) :left 0 :width (- gutter 8)}}
              (min-str (:s b)))))
       ($ :div {:class "absolute -translate-y-1/2 text-[12px] font-medium tabular-nums text-muted"
                :style #js {:top (total-h laid) :left 0 :width (- gutter 8)}}
          (min-str (:e (last laid))))
       ($ :div {:class "absolute top-0 bottom-0" :style #js {:left gutter :right 0}}
          (for [b laid]
            (let [d? (contains? done (:id b))
                  now? (= (:id b) current-id)
                  past? (and now-min (< (:e b) now-min) (not now?))
                  free? (= (:id b) "free")]
              ($ :button {:key (:id b)
                          :on-click #(toggle (:id b))
                          :class (str "absolute left-0 right-0 flex items-center justify-center "
                                      "px-3 text-center select-none cursor-pointer "
                                      "active:scale-[0.99] transition-colors "
                                      (if free?
                                        (str "border-2 border-dashed bg-surface text-label "
                                             (cond
                                               now? "border-[#1f6feb] dark:border-[#5b9dff] z-10 shadow-md "
                                               d? "border-done "
                                               :else "border-edge "))
                                        (str "border-2 "
                                             (cond
                                               d? "bg-done border-done text-white "
                                               now? (str now-bg " border-[#1f6feb] dark:border-[#5b9dff] text-white z-10 shadow-md ")
                                               :else "bg-surface border-edge text-label ")))
                                      (when past? "opacity-40"))
                          :style (stacked-style b)}
                 (when (and now? (not d?))
                   ($ :span {:class (str "absolute left-2 top-1.5 text-[10px] font-black uppercase tracking-[0.18em] "
                                         (if free? now-text "text-white"))}
                      "Now"))
                 ($ :span {:class (str "text-[14px] font-semibold leading-tight " (when free? "opacity-60"))}
                    (:name b))
                 (when d? ($ :span {:class "absolute right-3 text-[15px] font-bold"} "✓"))))))
       (when now-off
         ($ :div {:ref now-ref :class "pointer-events-none absolute left-0 right-0 z-20"
                  :style #js {:top now-off}}
            ($ :div {:class (str "absolute -translate-y-1/2 text-[12px] font-bold tabular-nums " now-text)
                     :style #js {:left 0 :width (- gutter 8)}}
               (min-str now-min))
            ($ :div {:class (str "absolute h-0.5 -translate-y-1/2 " now-bg)
                     :style #js {:left gutter :right 0}}))))))

;; ── Root ─────────────────────────────────────────────────────────────────────

(defui app []
  (let [[now-min] (use-state #(now-minutes blocks))
        [vh set-vh!] (use-state (.-innerHeight js/window))
        [done set-done!] (use-state #{})
        now-ref (use-ref)
        avail (max 300 (- vh 150))
        laid (fit-layout blocks avail)
        now-off (offset-at laid now-min)
        toggle (fn [id] (set-done! #(if (contains? % id) (disj % id) (conj % id))))]
    (use-effect
     (fn []
       (let [on-resize #(set-vh! (.-innerHeight js/window))]
         (.addEventListener js/window "resize" on-resize)
         #(.removeEventListener js/window "resize" on-resize)))
     [])
    ($ :div {:class "mx-auto w-full max-w-[420px] px-6 pb-10 pt-10"}
       ($ :div {:class "mb-6 text-center text-[15px] font-medium tracking-wide text-muted"}
          "Day — timetable prototype")
       ($ timetable {:laid laid :now-off now-off :now-min now-min
                     :done done :toggle toggle :now-ref now-ref}))))

;; ── Mount ────────────────────────────────────────────────────────────────────

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn ^:export init []
  (uix.dom/render-root ($ app) root))
