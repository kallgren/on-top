(ns app.shell
  (:require [uix.core :refer [defui defhook $ use-state use-ref use-effect]]
            [uix.dom]
            [app.config :as config]
            [app.core.view :as core]
            [app.rare.view :as rare]
            [app.schedule :as sched]
            [app.shared.today :refer [use-today]]
            [app.storage :as storage]
            [app.timer :refer [timer]]))

;; ── Header ───────────────────────────────────────────────────────────────────

(defn today-parts [date]
  [(.toLocaleDateString date "en-US" #js {:weekday "long"})
   (.toLocaleDateString date "en-US" #js {:month "long" :day "numeric"})])

(defui app-header [{:keys [date]}]
  (let [[wd md] (today-parts date)]
    ($ :header {:class "mb-8 flex flex-col items-center gap-1.5 text-center"}
       ($ :div {:class "pwa:hidden text-[34px] font-extrabold uppercase leading-none tracking-[0.28em] pl-[0.28em] text-muted text-inset"}
          "On Top")
       ($ :div {:class "text-[19px] font-medium tracking-wide text-muted"}
          (str wd " · " md)))))

;; ── Pane indicator ───────────────────────────────────────────────────────────

(def pane-count 2)

(defui pane-dots [{:keys [active on-select]}]
  ($ :div {:class "fixed inset-x-0 bottom-0 z-20 flex justify-center gap-2.5 pb-[calc(1.25rem+env(safe-area-inset-bottom))] wide:hidden"}
     (for [i (range pane-count)]
       ($ :button {:key i
                   :on-click #(on-select i)
                   :aria-label (str "Surface " (inc i))
                   :class (str "h-2.5 w-2.5 rounded-full transition-colors "
                               (if (= i active) "bg-muted" "bg-edge"))}))))

;; ── Surfaces ─────────────────────────────────────────────────────────────────

(defhook use-combined-schedule []
  (let [[combined set-combined!] (use-state nil)]
    (use-effect
     (fn []
       (when-let [url (not-empty (:schedule-url (config/parse-config (storage/read-config))))]
         (sched/fetch-schedule! url (fn [_raw parsed] (set-combined! parsed))))
       js/undefined)
     [])
    combined))

(defui surfaces [{:keys [today]}]
  (let [scroll-ref (use-ref)
        [active set-active!] (use-state 0)
        combined (use-combined-schedule)]
    (use-effect
     (fn []
       (let [el @scroll-ref
             on-scroll (fn []
                         (let [w (.-clientWidth el)]
                           (when (pos? w)
                             (set-active! (js/Math.round (/ (.-scrollLeft el) w))))))]
         (.addEventListener el "scroll" on-scroll #js {:passive true})
         #(.removeEventListener el "scroll" on-scroll)))
     [])
    ($ :<>
       ($ :div {:ref scroll-ref
                :class (str "no-scrollbar flex snap-x snap-mandatory overflow-x-auto "
                            "wide:snap-none wide:overflow-x-visible")}
          ($ :section {:class "w-full shrink-0 snap-center px-1 wide:flex-1 wide:px-7"}
             ($ :div {:class "mx-auto w-full max-w-md"}
                ($ core/view {:today today :combined combined})))
          ($ :section {:class "w-full shrink-0 snap-center wide:w-[42rem]"}
             ($ :div {:class "mx-auto w-full max-w-2xl px-7"}
                ($ rare/view {:today today :combined combined}))))
       ($ pane-dots {:active active
                     :on-select (fn [i]
                                  (let [el @scroll-ref]
                                    (.scrollTo el #js {:left (* i (.-clientWidth el))
                                                       :behavior "smooth"})))}))))

;; ── App ──────────────────────────────────────────────────────────────────────

(defui app []
  (let [today (use-today)]
    ($ :div {:class "px-7 pt-12 pb-10"}
       ($ app-header {:date today})
       ($ surfaces {:today today})
       ($ timer))))

;; ── Mount ────────────────────────────────────────────────────────────────────

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn ^:export init []
  (config/warn-unknown-keys! (storage/read-config))
  (uix.dom/render-root ($ app) root))
