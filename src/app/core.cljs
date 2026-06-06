(ns app.core
  (:require [uix.core :refer [defui $]]
            [uix.dom]))

(defui app []
  ($ :main {:class "min-h-screen p-6"}
     ($ :h1 {:class "text-lg font-medium text-stone-700"} "On Top")))

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn ^:export init []
  (uix.dom/render-root ($ app) root))
