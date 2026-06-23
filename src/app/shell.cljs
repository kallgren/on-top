(ns app.shell
  (:require [uix.core :refer [defui $ use-state use-ref use-effect]]
            [uix.dom]
            [app.config :as config]
            [app.core.view :as core]
            [app.keybinding :as keybinding]
            [app.rare.view :as rare]
            [app.shared.today :refer [use-today]]
            [app.storage :as storage]
            [app.timer :refer [timer]]))

;; ── Header ───────────────────────────────────────────────────────────────────

(defn today-parts [date]
  [(.toLocaleDateString date "en-US" #js {:weekday "long"})
   (.toLocaleDateString date "en-US" #js {:month "long" :day "numeric"})])

(defui app-header [{:keys [date]}]
  (let [[wd md] (today-parts date)]
    ($ :header {:class "mb-8 flex flex-col items-center gap-1.5 px-7 text-center wide:px-0"}
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

;; ── Reveal button ────────────────────────────────────────────────────────────

(defui list-icon []
  ($ :svg {:viewBox "0 0 18 14" :aria-hidden true
           :class "h-[18px] w-[20px]" :fill "none" :stroke "currentColor"}
     ($ :circle {:cx 2 :cy 2  :r 1.4 :fill "currentColor" :stroke "none"})
     ($ :circle {:cx 2 :cy 7  :r 1.4 :fill "currentColor" :stroke "none"})
     ($ :circle {:cx 2 :cy 12 :r 1.4 :fill "currentColor" :stroke "none"})
     ($ :line {:x1 6 :y1 2  :x2 17 :y2 2  :stroke-width 2 :stroke-linecap "round"})
     ($ :line {:x1 6 :y1 7  :x2 17 :y2 7  :stroke-width 2 :stroke-linecap "round"})
     ($ :line {:x1 6 :y1 12 :x2 17 :y2 12 :stroke-width 2 :stroke-linecap "round"})))

;; Wide-only; renders only while Rare is hidden. Badge appears once something is
;; Current, red when any Current row carries a deadline countdown.
(defui reveal-button [{:keys [count due? on-show]}]
  ($ :button
     {:on-click on-show
      :aria-label (str "Show Rare" (when (pos? count) (str " (" count " due)")))
      :class (str "fixed top-6 right-6 z-20 hidden wide:block "
                  "rounded-lg p-2 text-muted cursor-pointer transition-colors hover:bg-label/10")}
     ($ list-icon)
     (when (pos? count)
       ($ :span {:class (str "absolute -top-2 -right-2 flex h-[22px] w-[22px] items-center justify-center "
                             "rounded-full text-[11px] font-bold leading-none tabular-nums "
                             (if due? "bg-red-500 text-white" "bg-label text-page"))}
          count))))

;; ── Surfaces ─────────────────────────────────────────────────────────────────

(defui surfaces [{:keys [today]}]
  (let [scroll-ref (use-ref)
        [active set-active!] (use-state 0)
        [rare-hidden? set-rare-hidden!] (use-state false)
        {:keys [by-category toggle current-count due?]} (rare/use-rare today)]
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
    (keybinding/use-hotkey "r" #(set-rare-hidden! not))
    ($ :<>
       ($ :div {:ref scroll-ref
                :class (str "no-scrollbar flex snap-x snap-mandatory overflow-x-auto overflow-y-hidden "
                            "wide:snap-none wide:overflow-x-visible wide:overflow-y-visible")}
          ($ :section {:class "w-full shrink-0 snap-center px-8 wide:flex-1 wide:px-7"}
             ($ :div {:class "mx-auto w-full max-w-md"}
                ($ core/view {:today today})))
          ($ :section {:class (str "w-full shrink-0 snap-center wide:w-[42rem]"
                                   (when rare-hidden? " wide:hidden"))}
             ($ :div {:class "mx-auto w-full max-w-2xl px-4 wide:px-7"}
                ($ rare/view {:by-category by-category :toggle toggle}))))
       ($ pane-dots {:active active
                     :on-select (fn [i]
                                  (let [el @scroll-ref]
                                    (.scrollTo el #js {:left (* i (.-clientWidth el))
                                                       :behavior "smooth"})))})
       (when rare-hidden?
         ($ reveal-button {:count current-count :due? due?
                           :on-show #(set-rare-hidden! false)})))))

;; ── App ──────────────────────────────────────────────────────────────────────

(defui app []
  (let [today (use-today)]
    ($ :div {:class "pt-12 pb-10 wide:px-7"}
       ($ app-header {:date today})
       ($ surfaces {:today today})
       ($ timer))))

;; ── Mount ────────────────────────────────────────────────────────────────────

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn ^:export init []
  (config/warn-unknown-keys! (storage/read-config))
  (uix.dom/render-root ($ app) root))
