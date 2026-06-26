(ns app.modal
  "A reusable modal shell extracted from the keyboard-shortcuts overlay. It owns
   the dimmed backdrop (click-to-close), the bordered card, the ✕ close button,
   the `data-capture-keys` marker that sends every other hotkey dormant while it's
   open, and the Escape-to-close keydown listener. Callers pass a `title` (the
   dialog's accessible name), a `body` (the card's contents, heading and all), and
   an `on-close` callback; what fills the card stays with each caller."
  (:require [uix.core :refer [defui $ use-effect]]))

(defui shell [{:keys [title body on-close]}]
  (use-effect
   (fn []
     (let [on-key (fn [e]
                    (when (= "Escape" (.-key e))
                      (on-close)))]
       (.addEventListener js/window "keydown" on-key)
       #(.removeEventListener js/window "keydown" on-key)))
   [on-close])
  ($ :div {:data-capture-keys true
           :on-click on-close
           :class "fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-6"}
     ($ :div {:on-click #(.stopPropagation %)
              :role "dialog" :aria-modal true :aria-label title
              :class (str "relative w-full max-w-md rounded-2xl border-2 border-edge "
                          "bg-surface px-9 py-8 shadow-xl")}
        ($ :button {:on-click on-close
                    :aria-label "Close"
                    :class (str "absolute right-4 top-4 flex h-8 w-8 items-center "
                                "justify-center rounded-full text-[18px] leading-none "
                                "text-muted cursor-pointer transition hover:bg-edge/30")}
           "✕")
        body)))
