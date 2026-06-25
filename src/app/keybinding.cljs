(ns app.keybinding
  (:require [uix.core :refer [defhook use-effect use-effect-event]]))

(defn- editable? [el]
  (boolean
   (when el
     (or (#{"INPUT" "TEXTAREA"} (.-tagName el))
         (.-isContentEditable el)))))

(defn hotkey? [e key active-el capturing?]
  (and (= key (.-key e))
       (not (.-metaKey e))
       (not (.-ctrlKey e))
       (not (.-altKey e))
       (not (editable? active-el))
       (not capturing?)))

(defn- capturing-keys? []
  (boolean (.querySelector js/document "[data-capture-keys]")))

(defhook use-hotkey [key on-press]
  (let [press (use-effect-event on-press)]
    (use-effect
     (fn []
       (let [on-key (fn [e]
                      (when (hotkey? e key js/document.activeElement (capturing-keys?))
                        (press)))]
         (.addEventListener js/window "keydown" on-key)
         #(.removeEventListener js/window "keydown" on-key)))
     [key])))
