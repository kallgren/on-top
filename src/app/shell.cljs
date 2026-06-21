(ns app.shell
  (:require [uix.core :refer [defui $]]
            [uix.dom]
            [app.config :as config]
            [app.core.view :as core]
            [app.rare.view :as rare]
            [app.shared.today :refer [use-today]]
            [app.storage :as storage]
            [app.timer :refer [timer]]))

;; ── App ──────────────────────────────────────────────────────────────────────

(defui app []
  (let [today (use-today)]
    ($ :div {:class "px-7 pt-12 pb-16"}
       ($ core/view {:today today})
       ($ rare/view {:today today})
       ($ timer))))

;; ── Mount ────────────────────────────────────────────────────────────────────

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn ^:export init []
  (config/warn-unknown-keys! (storage/read-config))
  (uix.dom/render-root ($ app) root))
