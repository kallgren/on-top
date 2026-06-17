(ns app.core
  (:require [uix.core :refer [defui defhook $ use-state use-effect]]
            [uix.dom]
            [app.schedule :as sched]
            [app.storage :as storage]
            [app.timer :refer [timer]]
            [app.utils :refer [weekday-kw week-parity]]
            [cljs.reader :as reader]
            [shadow.resource :as rc]))

;; ── Setup ────────────────────────────────────────────────────────────────────

(def seed-schedule (reader/read-string (rc/inline "app/seed.edn")))

(def categories
  [[:digital   "Digital"]
   [:household "Household"]])

;; ── Helpers ──────────────────────────────────────────────────────────────────

(defn tasks-for [schedule date]
  (let [parity (week-parity date)
        wd     (weekday-kw (.getDay date))]
    (for [[cat] categories
          name  (get-in schedule [cat parity wd])]
      {:category cat :name name})))

;; ── Components ───────────────────────────────────────────────────────────────

(defui task-button [{:keys [name done? on-toggle]}]
  ($ :button
     {:on-click #(on-toggle name)
      :aria-pressed done?
      :aria-label name
      :class (str "flex aspect-[2/1] w-full items-center justify-center "
                  "overflow-hidden rounded-2xl border-2 px-6 "
                  "cursor-pointer select-none touch-manipulation active:scale-[0.98] "
                  "transition-colors duration-100 [container-type:inline-size] "
                  (if done?
                    "bg-done border-done"
                    "bg-surface border-edge hover:bg-surface-hover"))}
     (if done?
       ($ :span {:class "font-bold leading-none text-white text-check-fluid"}
          "✓")
       ($ :span {:class "font-bold text-label text-center text-label-fluid"}
          name))))

(defui scroll-cue [{:keys [show?]}]
  (when show?
    ($ :div {:class "pointer-events-none fixed inset-x-0 bottom-0 flex justify-center pb-3"}
       ($ :svg {:class "animate-bounce text-cue" :viewBox "0 0 24 24" :fill "none"
                :stroke "currentColor" :stroke-width 3.5
                :stroke-linecap "round" :stroke-linejoin "round"
                :aria-hidden true
                :style #js {:width "40px" :height "40px"}}
          ($ :path {:d "M5 9l7 7 7-7"})))))

(defui empty-state []
  ($ :p {:class "py-20 text-center text-[17px] font-medium italic text-muted tracking-wide text-inset"}
     "You're on top :)"))

(defui task-list [{:keys [by-category done toggle]}]
  (for [[cat label] categories
        :let [ts (by-category cat)]
        :when (seq ts)]
    ($ :section {:key (str cat) :class "contents"}
       ($ :h2 {:class "px-1 text-left text-[15px] font-semibold uppercase tracking-[0.2em] text-heading"}
          label)
       (for [{:keys [name]} ts]
         ($ task-button {:key name :name name
                         :done? (contains? done name)
                         :on-toggle toggle})))))

;; ── Hooks ────────────────────────────────────────────────────────────────────

(defhook use-schedule []
  (let [[schedule set-schedule!] (use-state #(sched/resolve-schedule
                                              (sched/parse-schedule (storage/read-schedule-cache))
                                              seed-schedule))]
    (use-effect
     (fn []
       (when-let [url (not-empty (storage/read-schedule-url))]
         (sched/fetch-schedule! url
                                (fn [raw parsed]
                                  (storage/write-schedule-cache! raw)
                                  (set-schedule! parsed))))
       js/undefined)
     [])
    schedule))

(defhook use-done [stamp]
  (let [[done set-done] (use-state #(or (storage/read-done stamp) #{}))]
    (use-effect
     (fn [] (storage/write-done! stamp done))
     [stamp done])
    [done (fn [name]
            (set-done #(if (contains? % name) (disj % name) (conj % name))))]))

(defhook use-overflow? []
  (let [[more? set-more?] (use-state false)]
    (use-effect
     (fn []
       (let [doc (.-documentElement js/document)
             update! (fn []
                       (set-more? (> (- (.-scrollHeight doc)
                                        (.-innerHeight js/window)
                                        (.-scrollY js/window))
                                     8)))]
         (update!)
         (.addEventListener js/window "scroll" update! #js {:passive true})
         (.addEventListener js/window "resize" update!)
         #(do (.removeEventListener js/window "scroll" update!)
              (.removeEventListener js/window "resize" update!))))
     [])
    more?))

;; ── Header ───────────────────────────────────────────────────────────────────

(defn today-parts [date]
  [(.toLocaleDateString date "en-US" #js {:weekday "long"})
   (.toLocaleDateString date "en-US" #js {:month "long" :day "numeric"})])

(defui screen-header [{:keys [date]}]
  (let [[wd md] (today-parts date)]
    ($ :header {:class "mb-8 flex flex-col items-center gap-1.5 text-center"}
       ($ :div {:class "pwa:hidden text-[34px] font-extrabold uppercase leading-none tracking-[0.28em] pl-[0.28em] text-muted text-inset"}
          "On Top")
       ($ :div {:class "text-[19px] font-medium tracking-wide text-muted"}
          (str wd " · " md)))))

;; ── App ──────────────────────────────────────────────────────────────────────

(defui app []
  (let [today (js/Date.)
        stamp (storage/day-stamp today)
        [done toggle] (use-done stamp)
        schedule (use-schedule)
        more? (use-overflow?)
        by-category (group-by :category (tasks-for schedule today))]
    ($ :div {:class "px-7 pt-12 pb-16"}
       ($ :div {:class "mx-auto w-full max-w-md"}
          ($ screen-header {:date today})
          ($ :div {:class "flex w-full flex-col gap-4 px-1 py-2"}
             (if (empty? by-category)
               ($ empty-state)
               ($ task-list {:by-category by-category :done done :toggle toggle}))))
       ($ scroll-cue {:show? more?})
       ($ timer))))

;; ── Mount ────────────────────────────────────────────────────────────────────

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn ^:export init []
  (uix.dom/render-root ($ app) root))
