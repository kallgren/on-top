(ns app.core.view
  (:require [uix.core :refer [defui defhook $ use-state use-effect use-ref]]
            [app.core.store :as store]
            [app.cursor :as cursor]
            [app.date-utils :as dates]
            [app.schedule :as schedule]
            [cljs.reader :as reader]
            [shadow.resource :as rc]))

;; ── Setup ────────────────────────────────────────────────────────────────────

(def seed-schedule (reader/read-string (rc/inline "app/core/seed.edn")))

(def schedule-cache-key "on-top/core-schedule-cache")

;; ── Components ───────────────────────────────────────────────────────────────

(defui task-button [{:keys [id name done? at-cursor? on-toggle]}]
  ($ :button
     {:on-click #(on-toggle id)
      :aria-pressed done?
      :aria-label name
      :class (str "flex aspect-[2/1] w-full items-center justify-center "
                  "overflow-hidden rounded-2xl border-2 px-6 "
                  "cursor-pointer select-none touch-manipulation active:scale-[0.98] "
                  "transition-colors duration-100 [container-type:inline-size] "
                  (cond
                    (and done? at-cursor?) "bg-done border-cursor"
                    done?                  "bg-done border-done"
                    at-cursor?             "bg-surface-hover border-cursor"
                    :else                  "bg-surface border-edge hover:bg-surface-hover"))}
     (if done?
       ($ :span {:class "font-bold leading-none text-white text-check-fluid"}
          "✓")
       ($ :span {:class "font-bold text-label text-center text-label-fluid"}
          name))))

(defui scroll-cue [{:keys [show?]}]
  (when show?
    ($ :div {:class "pointer-events-none fixed inset-x-0 bottom-0 flex justify-center pb-[calc(3rem+env(safe-area-inset-bottom))] wide:pb-3"}
       ($ :svg {:class "animate-bounce text-cue" :viewBox "0 0 24 24" :fill "none"
                :stroke "currentColor" :stroke-width 3.5
                :stroke-linecap "round" :stroke-linejoin "round"
                :aria-hidden true
                :style #js {:width "40px" :height "40px"}}
          ($ :path {:d "M5 9l7 7 7-7"})))))

(defui empty-state []
  ($ :p {:class "py-20 text-center text-[17px] font-medium italic text-muted tracking-wide text-inset"}
     "You're on top :)"))

(defui task-list [{:keys [categories by-category toggle cursor-id]}]
  (for [[cat label] categories
        :let [ts (by-category cat)]
        :when (seq ts)]
    ($ :section {:key (str cat) :class "contents"}
       ($ :h2 {:class "px-1 text-left text-[15px] font-semibold uppercase tracking-[0.2em] text-heading"}
          label)
       (for [{:keys [id name done?]} ts]
         ($ task-button {:key id :id id :name name
                         :done? done?
                         :at-cursor? (= id cursor-id)
                         :on-toggle toggle})))))

;; ── Hooks ────────────────────────────────────────────────────────────────────

(defhook use-overflow? [ref]
  (let [[more? set-more?] (use-state false)]
    (use-effect
     (fn []
       (let [update! (fn []
                       (when-let [el @ref]
                         (set-more? (> (- (.. el getBoundingClientRect -bottom)
                                          (.-innerHeight js/window))
                                       8))))]
         (update!)
         (.addEventListener js/window "scroll" update! #js {:passive true})
         (.addEventListener js/window "resize" update!)
         #(do (.removeEventListener js/window "scroll" update!)
              (.removeEventListener js/window "resize" update!))))
     [ref])
    more?))

;; ── View ─────────────────────────────────────────────────────────────────────

(defui day-view [{:keys [today schedule notes cursor]}]
  (let [categories (schedule/schedule->categories schedule)
        [tasks toggle] (store/use-store today schedule notes (map first categories))
        focused (cursor/use-list-cursor tasks #(toggle (:id %)) cursor)
        cursor-id (:id focused)
        content-ref (use-ref)
        more? (use-overflow? content-ref)
        by-category (group-by :category tasks)]
    ($ :<>
       ($ :div {:ref content-ref :class "flex w-full flex-col gap-4 px-1 py-2"}
          (if (empty? by-category)
            ($ empty-state)
            ($ task-list {:categories categories :by-category by-category
                          :toggle toggle :cursor-id cursor-id})))
       ($ scroll-cue {:show? more?}))))

(defui view [{:keys [today cursor notes schedule]}]
  ($ day-view {:key (dates/iso-date today) :today today :schedule schedule
               :notes notes :cursor cursor}))
