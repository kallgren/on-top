(ns app.rare.view
  (:require [uix.core :refer [defui $ use-state]]
            [app.badge :as badge]
            [app.config :as config]
            [app.cursor :as cursor]
            [app.date-utils :refer [iso->date]]
            [app.keybinding :refer [use-hotkey]]
            [app.keymap :as keymap]
            [app.modal :as modal]
            [app.rare.cards :as cards]
            [app.rare.store :as store]
            [app.schedule :as schedule]
            [app.shared.schedule :as sched]
            [cljs.reader :as reader]
            [shadow.resource :as rc]))

;; ── Setup ────────────────────────────────────────────────────────────────────

(def seed-schedule (reader/read-string (rc/inline "app/rare/seed.edn")))

(def schedule-cache-key "on-top/rare-schedule-cache")

;; ── Helpers ──────────────────────────────────────────────────────────────────

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

(defui round-checkbox [{:keys [checked? on-toggle class]}]
  ($ :button
     {:type "button"
      :aria-pressed checked?
      :aria-label "Toggle done"
      :on-click (fn [e] (.stopPropagation e) (on-toggle))
      :class (str "shrink-0 w-4 h-4 rounded-full border-2 cursor-pointer "
                  "flex items-center justify-center "
                  (if checked?
                    "bg-done border-done"
                    "bg-transparent border-edge")
                  " " class)}
     (when checked?
       ($ :svg {:viewBox "0 0 10 10" :class "w-2.5 h-2.5 text-white" :fill "none"
                :stroke "currentColor" :stroke-width 2 :stroke-linecap "round" :stroke-linejoin "round"}
          ($ :path {:d "M2 5l2.5 2.5 3.5-4"})))))

(defui freq-badge [{:keys [freq]}]
  ($ :span {:class "rounded px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wider bg-edge/40 text-heading"}
     freq))

(defui note-marker []
  ($ :span {:aria-hidden true
            :class "shrink-0 flex h-5 w-5 items-center justify-center text-heading/35"}
     ($ :svg {:viewBox "0 0 24 24" :class "h-5 w-5" :fill "none" :stroke "currentColor"
              :stroke-width 2 :stroke-linecap "round" :stroke-linejoin "round"}
        ($ :circle {:cx 12 :cy 12 :r 9})
        ($ :line {:x1 12 :y1 11 :x2 12 :y2 16})
        ($ :line {:x1 12 :y1 7.5 :x2 12 :y2 7.5}))))

(defui details-modal [{:keys [row on-close]}]
  (let [{:keys [name note]} row]
    ($ modal/shell
       {:title name
        :on-close on-close
        :close-key (keymap/key-of :open-details)
        :body ($ :<>
                 ($ :h2 {:class "text-[20px] font-bold text-label first-letter:uppercase"} name)
                 (when note
                   ($ :p {:class "mt-3 whitespace-pre-line text-[15px] leading-relaxed text-label"} note)))})))

(defui due-text [{:keys [due]}]
  ($ :span {:class "text-[12px] font-semibold uppercase text-red-500"} due))

(defui missed-badge [{:keys [missed]}]
  ($ :span {:class "shrink-0 rounded px-1.5 py-0.5 text-[11px] font-semibold uppercase tracking-wider bg-red-500/12 text-red-500"}
     (str missed " missed")))

(defui task-name [{:keys [name done? class]}]
  ($ :span {:class (str "min-w-0 text-[15px] font-medium leading-snug text-label "
                        (when done? "line-through ")
                        class)}
     name))

(defui task-row-mobile [{:keys [row on-toggle]}]
  (let [{:keys [name note freq display-iso due-label done? missed]} row
        {:keys [rel full today-or-yesterday?]} (date-display display-iso)
        date-text (if today-or-yesterday? rel (str full " (" rel ")"))]
    ($ :div {:class "flex items-start gap-3"}
       ($ round-checkbox {:checked? done? :on-toggle #(on-toggle row) :class "mt-0.5"})
       ($ :div {:class "flex min-w-0 flex-1 flex-col gap-1"}
          ($ :div {:class "flex items-center gap-2"}
             ($ task-name {:name name :done? done? :class "flex-1"})
             (when note ($ note-marker)))
          ($ :div {:class "flex items-center gap-2"}
             ($ :div {:class "flex min-w-0 flex-1 items-center gap-2"}
                ($ :span {:class "text-[13px] font-semibold text-muted"} date-text)
                (when due-label ($ due-text {:due due-label}))
                (when (pos? missed) ($ missed-badge {:missed missed})))
             ($ freq-badge {:freq freq}))))))

(defui task-row-desktop [{:keys [row on-toggle at-cursor?]}]
  (let [{:keys [name note freq display-iso due-label done? missed]} row
        {:keys [rel full today-or-yesterday?]} (date-display display-iso)]
    ($ :div {:class "flex w-full items-center gap-3"}
       ($ round-checkbox {:checked? done?
                          :on-toggle #(on-toggle row)
                          :class (if at-cursor? "flex" "hidden group-hover:flex")})
       ($ task-name {:name name :done? done?})
       (when-not done?
         ($ :span {:class "text-[13px] font-semibold text-muted"} rel))
       (when due-label ($ due-text {:due due-label}))
       (when (pos? missed) ($ missed-badge {:missed missed}))
       ($ :div {:class "flex-1"})
       (when note ($ note-marker))
       (when (or done? (not today-or-yesterday?))
         ($ :span {:class "text-[13px] font-semibold tabular-nums text-muted"} full))
       ($ freq-badge {:freq freq}))))

(defui task-row [{:keys [row on-toggle on-open-details at-cursor?]}]
  ($ :div
     {:role "button"
      :on-click #(on-open-details row)
      ;; A focused row wears the hover face steadily plus the Cursor ring, so it
      ;; reads the same as a hovered row.
      :class (str "group block w-full px-4 py-2.5 rounded-lg "
                  "cursor-pointer select-none touch-manipulation text-left "
                  (cond
                    (and (:due? row) at-cursor?) (str "bg-red-500/14 " cursor/cursor-ring " ")
                    (:due? row)                  "bg-red-500/8 hover:bg-red-500/14 "
                    at-cursor?                   (str "bg-page " cursor/cursor-ring " ")
                    :else                        "hover:bg-page "))}
     ($ :div {:class "wide:hidden"}
        ($ task-row-mobile {:row row :on-toggle on-toggle}))
     ($ :div {:class "hidden wide:block"}
        ($ task-row-desktop {:row row :on-toggle on-toggle :at-cursor? at-cursor?}))))

(defui task-list [{:keys [tasks on-toggle on-open-details class cursor-key]}]
  ($ :div {:class (str "flex flex-col" (when class (str " " class)))}
     (for [t tasks]
       ($ task-row {:key             (:key t)
                    :row             t
                    :on-toggle       on-toggle
                    :on-open-details on-open-details
                    :at-cursor?      (= (:key t) cursor-key)}))))

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
  [{:keys [label tasks on-toggle on-open-details on-collapse above? cursor-key]}]
  (let [divider ($ reveal-divider {:label label :on-click on-collapse})
        items   ($ task-list {:tasks tasks :on-toggle on-toggle :on-open-details on-open-details
                              :cursor-key cursor-key
                              :class (str "opacity-50 " (if above? "pt-1" "pb-1"))})]
    (if above?
      ($ :<> items divider)
      ($ :<> divider items))))

(defui fold
  [{:keys [label tasks on-toggle on-open-details expanded? on-fold top? cursor-key]}]
  (if expanded?
    ($ revealed-section {:label label :tasks tasks :on-toggle on-toggle
                         :on-open-details on-open-details
                         :on-collapse on-fold :above? top? :cursor-key cursor-key})
    ($ :div {:class (str "group/tab absolute inset-x-0 z-10 h-8 flex items-center justify-center "
                         (if top? "top-0 -translate-y-[80%]" "bottom-0 translate-y-[80%]"))}
       ($ reveal-pill {:label label :up? top? :on-click on-fold
                       :class (str "opacity-0 pointer-events-none "
                                   "group-hover/tab:opacity-100 group-hover/tab:pointer-events-auto")}))))

(defui category-card
  [{:keys [label completed current upcoming on-toggle on-open-details cursor-key
           show-completed? show-upcoming? on-toggle-completed on-toggle-upcoming]}]
  ($ :div {:class "rounded-2xl border-2 border-edge bg-surface p-2"}
     ($ card-header {:label label})
     ($ :div {:class "relative"}
        (when (seq completed)
          ($ fold {:label "Completed" :tasks completed :on-toggle on-toggle
                   :on-open-details on-open-details
                   :expanded? show-completed? :on-fold on-toggle-completed :top? true
                   :cursor-key cursor-key}))
        (if (empty? current)
          ($ :p {:class "py-4 text-center text-[15px] font-medium italic text-muted"}
             "All clear!")
          ($ task-list {:tasks current :on-toggle on-toggle :on-open-details on-open-details
                        :cursor-key cursor-key}))
        (when (seq upcoming)
          ($ fold {:label "Upcoming" :tasks upcoming :on-toggle on-toggle
                   :on-open-details on-open-details
                   :expanded? show-upcoming? :on-fold on-toggle-upcoming :top? false
                   :cursor-key cursor-key})))))

;; ── View ─────────────────────────────────────────────────────────────────────

(defui view [{:keys [today cursor notes]}]
  (let [schedule       (sched/use-schedule config/rare-schedule-file schedule-cache-key seed-schedule)
        [by-category toggle] (store/use-store today schedule notes)
        [expanded set-expanded!] (use-state {})
        [details set-details!] (use-state nil)
        toggle         (badge/use-due-badge (->> by-category vals (apply concat)) toggle)
        categories     (schedule/schedule->categories schedule)
        cards          (cards/build-cards by-category categories expanded)
        focused        (cursor/use-list-cursor (cards/visible-rows cards) toggle cursor)
        cursor-key     (:key focused)]
    (use-hotkey (keymap/key-of :open-details)
                #(when focused (set-details! focused)))
    ($ :div {:class "flex flex-col gap-4"}
       (for [{:keys [cat label completed current upcoming show-completed? show-upcoming?]} cards]
         ($ category-card {:key       (str cat)
                           :label     label
                           :completed completed :current current :upcoming upcoming
                           :show-completed? show-completed? :show-upcoming? show-upcoming?
                           :on-toggle toggle
                           :on-open-details set-details!
                           :cursor-key cursor-key
                           :on-toggle-completed #(set-expanded! (fn [m] (update-in m [cat :completed?] not)))
                           :on-toggle-upcoming  #(set-expanded! (fn [m] (update-in m [cat :upcoming?] not)))}))
       (when details
         ($ details-modal {:row details :on-close #(set-details! nil)})))))
