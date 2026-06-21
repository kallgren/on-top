(ns app.shell
  (:require [uix.core :refer [defui defhook $ use-state use-effect]]
            [uix.dom]
            [app.config :as config]
            [app.core.view :as core]
            [app.storage :as storage]
            [app.timer :refer [timer]]))

;; ── Hooks ────────────────────────────────────────────────────────────────────

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

;; ── App ──────────────────────────────────────────────────────────────────────

(defui app []
  (let [today (use-today)]
    ($ :div {:class "px-7 pt-12 pb-16"}
       ($ core/view {:today today})
       ($ timer))))

;; ── Mount ────────────────────────────────────────────────────────────────────

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn ^:export init []
  (config/warn-unknown-keys! (storage/read-config))
  (uix.dom/render-root ($ app) root))
