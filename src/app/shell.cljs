(ns app.shell
  (:require [uix.core :refer [defui defhook $ use-state use-ref use-effect use-layout-effect use-callback]]
            [uix.dom]
            [app.config :as config]
            [app.core.view :as core]
            [app.day.view :as day]
            [app.help :as help]
            [app.keybinding :as keybinding]
            [app.keymap :as keymap]
            [app.notes :as notes]
            [app.rare.view :as rare]
            [app.shared.notes :as shared-notes]
            [app.shared.today :refer [use-today]]
            [app.storage :as storage]
            [app.timer :refer [timer]]
            [shadow.resource :as rc]))

;; ── Notes floor ──────────────────────────────────────────────────────────────

(def seed-notes (notes/parse (rc/inline "app/seed-notes.md")))

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

;; ── Viewport ─────────────────────────────────────────────────────────────────

(def wide-query "(min-width: 1200px)")

(defn- use-wide? []
  (let [[wide? set-wide!] (use-state #(.-matches (js/matchMedia wide-query)))]
    (use-effect
     (fn []
       (let [mq (js/matchMedia wide-query)
             on-change #(set-wide! (.-matches mq))]
         (.addEventListener mq "change" on-change)
         #(.removeEventListener mq "change" on-change)))
     [])
    wide?))

;; ── Pane indicator ───────────────────────────────────────────────────────────

(def pane-count 3)

(def landing-pane 1)

(defui pane-dots [{:keys [active on-select]}]
  ($ :div {:class "fixed inset-x-0 bottom-0 z-20 flex justify-center gap-2.5 pb-[calc(1.25rem+env(safe-area-inset-bottom))] wide:hidden"}
     (for [i (range pane-count)]
       ($ :button {:key i
                   :on-click #(on-select i)
                   :aria-label (str "Surface " (inc i))
                   :class (str "h-2.5 w-2.5 rounded-full transition-colors "
                               (if (= i active) "bg-muted" "bg-edge"))}))))

;; ── Pane scroll ──────────────────────────────────────────────────────────────

(defhook use-pane-scroll
  "Owns the horizontal swipe between Panes, landing on `landing`. Returns
   `{:ref :active :scroll-to}`: `:ref` for the scroller, `:active`/`:scroll-to`
   for the Pane dots."
  [landing]
  (let [scroll-ref (use-ref)
        [active set-active!] (use-state landing)
        scroll-to (use-callback
                   (fn [i]
                     (let [el @scroll-ref]
                       (.scrollTo el #js {:left (* i (.-clientWidth el))
                                          :behavior "smooth"})))
                   [])]
    (use-layout-effect
     (fn []
       (let [el @scroll-ref]
         (set! (.-scrollLeft el) (* landing (.-clientWidth el))))
       js/undefined)
     [landing])
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
    {:ref scroll-ref :active active :scroll-to scroll-to}))

;; ── Pane cursor ──────────────────────────────────────────────────────────────

(defhook use-pane-cursor
  "Owns crossing the one keyboard Cursor between Panes and the `r` toggle (see
   ADR-0011). Returns `{:rare-hidden? :core :rare}`, where `:core`/`:rare` are
   opaque `use-list-cursor` opts bundles each Surface forwards untouched."
  []
  (let [[rare-hidden? set-rare-hidden!] (use-state false)
        [cursor-pane set-cursor-pane!] (use-state :core)
        [reset-nonce set-reset-nonce!] (use-state 0)
        dismiss (use-callback
                 (fn []
                   (set-cursor-pane! :core)
                   (set-reset-nonce! inc))
                 [])]
    (keybinding/use-hotkey
     (keymap/key-of :toggle-rare)
     (fn []
       (let [hiding? (not rare-hidden?)]
         (set-rare-hidden! hiding?)
         (when (and hiding? (= cursor-pane :rare))
           (dismiss)))))
    {:rare-hidden? rare-hidden?
     :core {:active? (= cursor-pane :core)
            :on-dismiss dismiss
            :reset-nonce reset-nonce
            :on-exit-right (fn []
                             (set-rare-hidden! false)
                             (set-cursor-pane! :rare))}
     :rare {:active? (= cursor-pane :rare)
            :on-dismiss dismiss
            :reset-nonce reset-nonce
            :on-exit-left #(set-cursor-pane! :core)}}))

;; ── Surfaces ─────────────────────────────────────────────────────────────────

(defui surfaces [{:keys [today wide?]}]
  (let [{:keys [ref active scroll-to]} (use-pane-scroll landing-pane)
        {:keys [rare-hidden? core rare]} (use-pane-cursor)
        notes (shared-notes/use-notes seed-notes)]
    ($ :<>
       ($ :div {:ref ref
                :class (str "no-scrollbar flex snap-x snap-mandatory overflow-x-auto overflow-y-hidden "
                            "wide:snap-none wide:overflow-x-visible wide:overflow-y-visible")}
          ($ :section {:class "w-full shrink-0 snap-center wide:hidden"}
             (when-not wide? ($ day/view {:today today})))
          ($ :section {:class "w-full shrink-0 snap-center px-8 wide:flex-1 wide:px-7"}
             ($ :div {:class "mx-auto w-full max-w-md"}
                ($ core/view {:today today :cursor core :notes notes})))
          ($ :section {:class (str "w-full shrink-0 snap-center wide:w-[42rem]"
                                   (when rare-hidden? " wide:hidden"))}
             ($ :div {:class "mx-auto w-full max-w-2xl px-4 wide:px-7"}
                ($ rare/view {:today today :cursor rare}))))
       ($ pane-dots {:active active :on-select scroll-to}))))

;; ── Desktop drawer ───────────────────────────────────────────────────────────

(defui day-drawer [{:keys [today]}]
  (let [[open? set-open!] (use-state false)]
    ($ :div {:class "fixed inset-y-0 left-0 z-30"}
       ($ :div {:class "absolute inset-y-0 left-0 w-4"
                :on-mouse-enter #(set-open! true)})
       ($ :div {:class (str "absolute inset-y-0 left-0 w-[360px] bg-page "
                            (if open?
                              "translate-x-0 shadow-[6px_0_30px_-4px_rgba(0,0,0,0.25)] "
                              "-translate-x-full pointer-events-none"))
                :on-mouse-leave #(set-open! false)}
          ($ :div {:class "px-6 pt-12 pb-6 text-center text-[15px] font-bold uppercase tracking-[0.24em] text-muted"}
             "Daily schedule")
          ($ day/view {:today today})))))

;; ── App ──────────────────────────────────────────────────────────────────────

(defui app []
  (let [today (use-today)
        wide? (use-wide?)]
    ($ :div {:class "pt-12 pb-10 wide:px-7"}
       ($ app-header {:date today})
       ($ surfaces {:today today :wide? wide?})
       ($ timer)
       ($ help/view)
       (when wide? ($ day-drawer {:today today})))))

;; ── Mount ────────────────────────────────────────────────────────────────────

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn ^:export init []
  (config/warn-unknown-keys! (storage/read-config))
  (uix.dom/render-root ($ app) root))
