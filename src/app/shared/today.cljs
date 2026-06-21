(ns app.shared.today
  "Cross-cutting today hook: a Date that refreshes to the current day whenever the
   tab returns to the foreground, so both surfaces roll over on return."
  (:require [uix.core :refer [defhook use-state use-effect]]))

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
