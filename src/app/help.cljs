(ns app.help
  "The keyboard-shortcuts overlay and its one trigger glyph (see CONTEXT-MAP's
   Cursor). Self-contained: it owns whether the overlay is open, opens on `?` or a
   click of the wide-only top-right glyph, and renders app.keymap so the list can
   never drift from the live bindings. While open it mounts a `data-capture-keys`
   marker that sends every other hotkey dormant, and its own listener closes it on
   `?`, Esc, or a click outside. Keyboard-only: the glyph shows at the wide
   breakpoint and the overlay is unreachable on touch. The shell mounts `view`
   once and knows nothing else — so the glyph is a one-line removal."
  (:require [uix.core :refer [defui $ use-state use-effect]]
            [app.keybinding :refer [use-hotkey]]
            [app.keymap :as keymap]))

(defui key-cap [{:keys [label]}]
  ($ :kbd {:class (str "inline-flex min-w-7 items-center justify-center rounded-md "
                       "border-2 border-edge bg-page px-2 py-1 "
                       "text-[13px] font-bold text-label")}
     label))

(defui shortcut-row [{:keys [binding]}]
  ($ :div {:class "flex items-center justify-between gap-6 py-2.5"}
     ($ :span {:class "text-[15px] font-medium text-label"} (:desc binding))
     ($ key-cap {:label (or (:cap binding) (:key binding))})))

(defui overlay [{:keys [on-close]}]
  (use-effect
   (fn []
     (let [on-key (fn [e]
                    (when (#{"Escape" "?"} (.-key e))
                      (on-close)))]
       (.addEventListener js/window "keydown" on-key)
       #(.removeEventListener js/window "keydown" on-key)))
   [on-close])
  ($ :div {:data-capture-keys true
           :on-click on-close
           :class "fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-6"}
     ($ :div {:on-click #(.stopPropagation %)
              :role "dialog" :aria-modal true :aria-label "Keyboard shortcuts"
              :class (str "w-full max-w-sm rounded-2xl border-2 border-edge "
                          "bg-surface p-6 shadow-xl")}
        ($ :h2 {:class "mb-3 text-[15px] font-bold uppercase tracking-[0.2em] text-heading"}
           "Keyboard shortcuts")
        ($ :div {:class "divide-y divide-edge/50"}
           (for [b keymap/bindings]
             ($ shortcut-row {:key (:id b) :binding b}))))))

(defui view []
  (let [[open? set-open!] (use-state false)]
    (use-hotkey (keymap/key-of :help) #(set-open! true))
    ($ :<>
       ($ :button {:on-click #(set-open! true)
                   :aria-label "Keyboard shortcuts"
                   :class (str "fixed right-7 top-7 z-20 hidden h-9 w-9 wide:flex "
                               "items-center justify-center rounded-full "
                               "border-2 border-edge bg-surface text-[16px] font-bold "
                               "text-muted cursor-pointer transition "
                               "hover:bg-surface-hover active:scale-95")}
          "?")
       (when open?
         ($ overlay {:on-close #(set-open! false)})))))
