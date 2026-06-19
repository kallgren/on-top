(ns app.core
  (:require [uix.core :refer [defui defhook $ use-state use-effect use-effect-event]]
            [uix.dom]
            [app.completions :as completion]
            [app.config :as config]
            [app.date-utils :as dates]
            [app.schedule :as sched]
            [app.storage :as storage]
            [app.sync :as sync]
            [app.tasks :as tasks]
            [app.timer :refer [timer]]
            [cljs.reader :as reader]
            [shadow.resource :as rc]))

;; ── Setup ────────────────────────────────────────────────────────────────────

(def seed-schedule (reader/read-string (rc/inline "app/seed.edn")))

(def categories
  [[:digital   "Digital"]
   [:household "Household"]])

;; ── Components ───────────────────────────────────────────────────────────────

(defui task-button [{:keys [id text done? on-toggle]}]
  ($ :button
     {:on-click #(on-toggle id)
      :aria-pressed done?
      :aria-label text
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
          text))))

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

(defui task-list [{:keys [by-category done? toggle]}]
  (for [[cat label] categories
        :let [ts (by-category cat)]
        :when (seq ts)]
    ($ :section {:key (str cat) :class "contents"}
       ($ :h2 {:class "px-1 text-left text-[15px] font-semibold uppercase tracking-[0.2em] text-heading"}
          label)
       (for [{:keys [id text]} ts]
         ($ task-button {:key id :id id :text text
                         :done? (done? id)
                         :on-toggle toggle})))))

;; ── Hooks ────────────────────────────────────────────────────────────────────

(defhook use-schedule []
  (let [[schedule set-schedule!] (use-state #(sched/resolve-schedule
                                              (sched/parse-schedule (storage/read-schedule-cache))
                                              seed-schedule))]
    (use-effect
     (fn []
       (when-let [url (not-empty (:schedule-url (config/parse-config (storage/read-config))))]
         (sched/fetch-schedule! url
                                (fn [raw parsed]
                                  (storage/write-schedule-cache! raw)
                                  (set-schedule! parsed))))
       js/undefined)
     [])
    schedule))

(defhook use-completions [today schedule category-keys]
  (let [today-key (dates/iso-date today)
        [completions set-completions] (use-state #(or (storage/read-completions) {}))
        [outbox set-outbox] (use-state #(or (storage/read-outbox) #{}))
        flush! (fn [completions outbox]
                 (let [{:keys [completions-db-url supabase-publishable-key]}
                       (config/parse-config (storage/read-config))]
                   (when (and (not-empty completions-db-url)
                              (not-empty supabase-publishable-key)
                              (seq outbox))
                     (sync/upsert-completions!
                      completions-db-url supabase-publishable-key
                      (sync/flush-payload outbox completions)
                      #(set-outbox (fn [pending] (sync/clear-pending pending outbox)))))))
        hydrate! (use-effect-event
                  (fn []
                    (let [{:keys [completions-db-url supabase-publishable-key]}
                          (config/parse-config (storage/read-config))]
                      (when (and (not-empty completions-db-url) (not-empty supabase-publishable-key))
                        (sync/fetch-completions! completions-db-url supabase-publishable-key
                                                 (fn [remote]
                                                   (set-completions (sync/reconcile remote (select-keys completions outbox)))))
                        (flush! completions outbox)))))]
    (use-effect
     (fn [] (storage/write-completions! completions))
     [completions])
    (use-effect
     (fn [] (storage/write-outbox! outbox))
     [outbox])
    (use-effect
     (fn [] (hydrate!) js/undefined)
     [today])
    [(fn [id] (completion/covered? completions id today-key))
     (fn [id]
       (let [next-completions (completion/toggle completions schedule category-keys today id)
             next-outbox      (sync/mark-dirty outbox id)]
         (set-completions next-completions)
         (set-outbox next-outbox)
         (flush! next-completions next-outbox)))]))

(defhook use-today []
  (let [[today set-today!] (use-state #(js/Date.))]
    (use-effect
     (fn []
       (let [on-visible (fn []
                          (when (= "visible" (.-visibilityState js/document))
                            (set-today! (js/Date.))))]
         (.addEventListener js/document "visibilitychange" on-visible)
         #(.removeEventListener js/document "visibilitychange" on-visible)))
     [])
    today))

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

(defui day-view [{:keys [today schedule]}]
  (let [category-keys (map first categories)
        [done? toggle] (use-completions today schedule category-keys)
        more? (use-overflow?)
        by-category (group-by :category (tasks/tasks-for schedule today category-keys))]
    ($ :<>
       ($ :div {:class "mx-auto w-full max-w-md"}
          ($ screen-header {:date today})
          ($ :div {:class "flex w-full flex-col gap-4 px-1 py-2"}
             (if (empty? by-category)
               ($ empty-state)
               ($ task-list {:by-category by-category :done? done? :toggle toggle}))))
       ($ scroll-cue {:show? more?}))))

(defui app []
  (let [today (use-today)
        schedule (use-schedule)]
    ($ :div {:class "px-7 pt-12 pb-16"}
       ($ day-view {:key (dates/iso-date today) :today today :schedule schedule})
       ($ timer))))

;; ── Mount ────────────────────────────────────────────────────────────────────

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn ^:export init []
  (uix.dom/render-root ($ app) root))
