(ns app.rare.view
  (:require [uix.core :refer [defui $ use-state]]
            [app.date-utils :refer [iso->date]]
            [app.rare.store :as store]))

;; ── Setup ────────────────────────────────────────────────────────────────────

(def categories
  [[:digital   "Digital"]
   [:household "Household"]])

;; ── Helpers ──────────────────────────────────────────────────────────────────

(defn partition-tasks [tasks]
  (let [sorted (sort-by :sort-key tasks)]
    {:completed (filter :done? sorted)
     :current   (remove #(or (:upcoming? %) (:done? %)) sorted)
     :upcoming  (filter :upcoming? sorted)}))

(defn date-display [iso-str]
  (let [now       (js/Date.)
        target    (iso->date iso-str)
        diff-ms   (- (.getTime now) (.getTime target))
        diff-days (Math/floor (/ diff-ms 86400000))
        weekday   (.toLocaleDateString target "en-US" #js {:weekday "short"})
        month-day (.toLocaleDateString target "en-US" #js {:month "short" :day "numeric"})
        today-or-yesterday? (<= 0 diff-days 1)   ; rel already names the day; full date adds nothing
        rel       (cond
                    (= diff-days 0)   "Today"
                    (= diff-days 1)   "Yesterday"
                    (> diff-days 0)   (str diff-days " days ago")
                    (>= diff-days -6) (.toLocaleDateString target "en-US" #js {:weekday "long"})
                    :else             (str "in " (- diff-days) " days"))]
    {:rel rel :full (str weekday " " month-day) :today-or-yesterday? today-or-yesterday?}))

;; ── Components ───────────────────────────────────────────────────────────────

(defui round-checkbox [{:keys [checked? class]}]
  ($ :span
     {:aria-hidden true
      :class (str "shrink-0 w-4 h-4 rounded-full border-2 cursor-pointer "
                  "flex items-center justify-center "
                  (if checked?
                    "bg-done border-done"
                    "bg-transparent border-edge")
                  (when class (str " " class)))}
     (when checked?
       ($ :svg {:viewBox "0 0 10 10" :class "w-2.5 h-2.5 text-white" :fill "none"
                :stroke "currentColor" :stroke-width 2 :stroke-linecap "round" :stroke-linejoin "round"}
          ($ :path {:d "M2 5l2.5 2.5 3.5-4"})))))

(defui freq-badge [{:keys [freq]}]
  ($ :span {:class "rounded px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wider bg-edge/40 text-heading"}
     freq))

(defui task-row [{:keys [row on-toggle]}]
  (let [{:keys [name freq display-iso due-label done? missed]} row
        due                due-label
        {:keys [rel full today-or-yesterday?]} (date-display display-iso)]
    ($ :button
       {:on-click #(on-toggle row)
        :class (str "group flex w-full items-center gap-3 px-4 py-2.5 rounded-lg "
                    "cursor-pointer select-none touch-manipulation text-left "
                    (if due "bg-red-500/8 hover:bg-red-500/14 " "hover:bg-page "))}
       ($ round-checkbox {:checked? done? :class "pointer-events-none hidden group-hover:flex"})
       ($ :span {:class (str "text-[15px] font-medium leading-snug text-label "
                             (when done? "line-through"))}
          name)
       (when-not done?
         ($ :span {:class "text-[13px] font-semibold text-muted"} rel))
       (when due
         ($ :span {:class "text-[12px] font-semibold uppercase text-red-500"} due))
       (when (pos? missed)
         ($ :span {:class "rounded px-1.5 py-0.5 text-[11px] font-semibold uppercase tracking-wider bg-red-500/12 text-red-500"}
            (str missed " missed")))
       ($ :div {:class "flex-1"})
       (when (or done? (not today-or-yesterday?))
         ($ :span {:class "text-[13px] font-semibold tabular-nums text-muted"} full))
       ($ freq-badge {:freq freq}))))

(defui task-list [{:keys [tasks on-toggle class]}]
  ($ :div {:class (str "flex flex-col" (when class (str " " class)))}
     (for [t tasks]
       ($ task-row {:key       (:key t)
                    :row       t
                    :on-toggle on-toggle}))))

(defui reveal-divider [{:keys [label on-click]}]
  ($ :div {:class "group/rev mx-4 my-1 flex items-center gap-3 cursor-pointer"
           :on-click on-click}
     ($ :div {:class "flex-1 border-t border-dashed border-edge"})
     ($ :span {:class "text-[11px] font-semibold uppercase tracking-[0.15em] text-heading"}
        ($ :span {:class "group-hover/rev:hidden"} label)
        ($ :span {:class "hidden group-hover/rev:inline"} (str "Hide " (.toLowerCase label))))
     ($ :div {:class "flex-1 border-t border-dashed border-edge"})))

(defui card-header [{:keys [label]}]
  ($ :div {:class "flex items-center gap-3 px-4 py-3"}
     ($ :span {:class "text-[20px] font-bold uppercase tracking-[0.2em] text-heading"}
        label)))

(defui reveal-pill [{:keys [label on-click class up?]}]
  ($ :button
     {:on-click on-click
      :class (str "flex items-center gap-1.5 rounded-full border-2 border-edge bg-surface "
                  "px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.1em] text-heading "
                  "cursor-pointer select-none"
                  (when class (str " " class)))}
     label
     ($ :svg {:viewBox "0 0 10 10" :fill "none" :stroke "currentColor" :stroke-width 2.5
              :stroke-linecap "round" :stroke-linejoin "round"
              :class "w-2.5 h-2.5"}
        ($ :path {:d (if up? "M1.5 7l3.5-4 3.5 4" "M1.5 3l3.5 4 3.5-4")}))))

(defui revealed-section
  [{:keys [label tasks on-toggle on-collapse above?]}]
  (let [divider ($ reveal-divider {:label label :on-click on-collapse})
        items   ($ task-list {:tasks tasks :on-toggle on-toggle
                              :class (str "opacity-50 " (if above? "pt-1" "pb-1"))})]
    (if above?
      ($ :<> items divider)
      ($ :<> divider items))))

(defui fold
  [{:keys [label tasks on-toggle expanded? on-fold top?]}]
  (if expanded?
    ($ revealed-section {:label label :tasks tasks :on-toggle on-toggle
                         :on-collapse on-fold :above? top?})
    ($ :div {:class (str "group/tab absolute inset-x-0 z-10 h-8 flex items-center justify-center "
                         (if top? "top-0 -translate-y-[80%]" "bottom-0 translate-y-[80%]"))}
       ($ reveal-pill {:label label :up? top? :on-click on-fold
                       :class (str "opacity-0 pointer-events-none "
                                   "group-hover/tab:opacity-100 group-hover/tab:pointer-events-auto")}))))

(defui category-card [{:keys [label cat-tasks on-toggle]}]
  (let [[show-completed? set-completed!] (use-state false)
        [show-upcoming?  set-upcoming!]  (use-state false)
        {:keys [completed current upcoming]} (partition-tasks cat-tasks)]
    ($ :div {:class "pb-3"}
       ($ :div {:class "rounded-2xl border-2 border-edge bg-surface p-2"}
          ($ card-header {:label label})
          ($ :div {:class "relative"}
             (when (seq completed)
               ($ fold {:label "Completed" :tasks completed :on-toggle on-toggle
                        :expanded? show-completed? :on-fold #(set-completed! not) :top? true}))
             (if (empty? current)
               ($ :p {:class "py-4 text-center text-[15px] font-medium italic text-muted"}
                  "All clear!")
               ($ task-list {:tasks current :on-toggle on-toggle}))
             (when (seq upcoming)
               ($ fold {:label "Upcoming" :tasks upcoming :on-toggle on-toggle
                        :expanded? show-upcoming? :on-fold #(set-upcoming! not) :top? false})))))))

;; ── View ─────────────────────────────────────────────────────────────────────

(defui view [{:keys [today schedule]}]
  (let [[by-category toggle] (store/use-store today schedule)]
    ($ :div {:class "flex flex-col gap-4"}
       (for [[cat label] categories
             :let [cat-rows (by-category cat)]
             :when (seq cat-rows)]
         ($ category-card {:key       (str cat)
                           :label     label
                           :cat-tasks cat-rows
                           :on-toggle toggle})))))
