(ns app.shell
  (:require [uix.core :refer [defui $ use-state use-ref use-effect]]
            [uix.dom]
            [app.config :as config]
            [app.core.view :as core]
            [app.rare.view :as rare]
            [app.shared.today :refer [use-today]]
            [app.storage :as storage]
            [app.timer :refer [timer]]))

;; ── Header ───────────────────────────────────────────────────────────────────

(defn today-parts [date]
  [(.toLocaleDateString date "en-US" #js {:weekday "long"})
   (.toLocaleDateString date "en-US" #js {:month "long" :day "numeric"})])

(defui app-header [{:keys [date collapsed? on-scroll-top]}]
  (let [[wd md] (today-parts date)]
    ($ :header
       {:class (str "fixed inset-x-0 top-0 z-10 flex flex-col items-center px-7 text-center "
                    "transition-all duration-200 "
                    "wide:static wide:z-auto wide:mb-8 wide:px-0 wide:pb-0 wide:pt-12 "
                    (if collapsed?
                      "gap-0 pb-2.5 pt-[calc(env(safe-area-inset-top)+1.25rem)] "
                      "gap-1.5 pb-8 pt-[calc(env(safe-area-inset-top)+3rem)] "))}
       ($ :div {:class (str "pwa:hidden overflow-hidden font-extrabold uppercase leading-none "
                            "tracking-[0.28em] pl-[0.28em] text-[34px] text-muted text-inset "
                            "transition-all duration-200 "
                            (if collapsed? "max-h-0 opacity-0" "max-h-[2.75rem] opacity-100"))}
          "On Top")
       ($ :button {:type "button"
                   :on-click on-scroll-top
                   :aria-label "Scroll to top"
                   :class (str "font-medium tracking-wide text-muted transition-all duration-200 "
                               (if collapsed?
                                 (str "cursor-pointer rounded-full border border-edge/50 bg-page/80 "
                                      "px-4 py-1.5 text-[15px] shadow-sm backdrop-blur-md active:scale-95")
                                 "pointer-events-none text-[19px]"))}
          (str wd " · " md)))))

;; ── Rare shortcut ────────────────────────────────────────────────────────────

(defui rare-button [{:keys [hidden on-click]}]
  ($ :button {:on-click on-click
              :aria-label "Go to Rare"
              :class (str "fixed right-7 top-[calc(1.75rem+env(safe-area-inset-top))] z-20 "
                          "flex h-12 w-12 items-center justify-center rounded-full "
                          "border-2 border-edge bg-surface text-muted "
                          "text-[26px] font-bold leading-none "
                          "cursor-pointer shadow-lg transition hover:bg-surface-hover active:scale-95 "
                          "wide:hidden "
                          (if hidden "pointer-events-none opacity-0" "opacity-100"))}
     "›"))

;; ── Pane indicator ───────────────────────────────────────────────────────────

(def pane-count 2)
(def core-pane 0)
(def rare-pane 1)

(defui pane-dots [{:keys [active on-select]}]
  ($ :div {:class "fixed inset-x-0 bottom-0 z-20 flex justify-center gap-2.5 pb-[calc(1.25rem+env(safe-area-inset-bottom))] wide:hidden"}
     (for [i (range pane-count)]
       ($ :button {:key i
                   :on-click #(on-select i)
                   :aria-label (str "Surface " (inc i))
                   :class (str "h-2.5 w-2.5 rounded-full transition-colors "
                               (if (= i active) "bg-muted" "bg-edge"))}))))

;; ── Surfaces ─────────────────────────────────────────────────────────────────

(def pane-top
  (str "pt-[calc(env(safe-area-inset-top)+9rem)] "
       "pwa:pt-[calc(env(safe-area-inset-top)+6.5rem)] wide:pt-0"))

(def collapse-threshold 4)

(defui surfaces [{:keys [today]}]
  (let [scroll-ref (use-ref)
        core-ref (use-ref)
        rare-ref (use-ref)
        [active set-active!] (use-state 0)
        [collapsed? set-collapsed!] (use-state false)]
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
    (use-effect
     (fn []
       (let [panes [@core-ref @rare-ref]
             listeners (mapv (fn [el]
                               (let [on-scroll #(set-collapsed! (> (.-scrollTop el) collapse-threshold))]
                                 (.addEventListener el "scroll" on-scroll #js {:passive true})
                                 [el on-scroll]))
                             panes)]
         #(doseq [[el on-scroll] listeners]
            (.removeEventListener el "scroll" on-scroll))))
     [])
    (use-effect
     (fn []
       (when-let [el (if (= active rare-pane) @rare-ref @core-ref)]
         (set-collapsed! (> (.-scrollTop el) collapse-threshold)))
       js/undefined)
     [active])
    (let [scroll-to! (fn [i]
                       (let [el @scroll-ref]
                         (.scrollTo el #js {:left (* i (.-clientWidth el))
                                            :behavior "smooth"})))
          scroll-top! (fn []
                        (when-let [el (if (= active rare-pane) @rare-ref @core-ref)]
                          (.scrollTo el #js {:top 0 :behavior "smooth"})))]
      ($ :<>
         ($ rare-button {:hidden (= active rare-pane)
                         :on-click #(scroll-to! rare-pane)})
         ($ app-header {:date today :collapsed? collapsed? :on-scroll-top scroll-top!})
         ($ :div {:ref scroll-ref
                  :class (str "no-scrollbar flex h-dvh snap-x snap-mandatory overflow-x-auto overflow-y-hidden "
                              "wide:h-auto wide:snap-none wide:overflow-x-visible wide:overflow-y-visible wide:pb-10")}
            ($ :section {:ref core-ref
                         :class (str "no-scrollbar h-full w-full shrink-0 snap-center overflow-y-auto "
                                     "wide:h-auto wide:flex-1 wide:overflow-visible")}
               ($ :div {:class (str pane-top " pb-10 wide:pb-0")}
                  ($ :div {:class "mx-auto w-full max-w-md px-8 wide:px-7"}
                     ($ core/view {:today today}))))
            ($ :section {:ref rare-ref
                         :class (str "no-scrollbar h-full w-full shrink-0 snap-center overflow-y-auto "
                                     "wide:h-auto wide:w-[42rem] wide:overflow-visible")}
               ($ :div {:class (str pane-top " pb-10 wide:pb-0")}
                  ($ :div {:class "mx-auto w-full max-w-2xl px-4 wide:px-7"}
                     ($ rare/view {:today today})))))
         ($ pane-dots {:active active
                       :on-select scroll-to!})
         ($ timer {:start-hidden? (not= active core-pane)})))))

;; ── App ──────────────────────────────────────────────────────────────────────

(defui app []
  (let [today (use-today)]
    ($ :div {:class "wide:px-7"}
       ($ surfaces {:today today}))))

;; ── Mount ────────────────────────────────────────────────────────────────────

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn ^:export init []
  (config/warn-unknown-keys! (storage/read-config))
  (uix.dom/render-root ($ app) root))
