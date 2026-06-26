(ns app.help
  "The keyboard-shortcuts overlay and its one trigger glyph (see CONTEXT-MAP's
   Cursor). Self-contained: it owns whether the overlay is open, opens on `?` or a
   click of the wide-only top-right glyph, and renders app.keymap so the list can
   never drift from the live bindings. It renders through app.modal/shell, which
   supplies the backdrop, card, ✕, `data-capture-keys` marker and Escape-to-close;
   passing the shell `:close-key` makes `?` toggle it shut too. Keyboard-only:
   the glyph shows at the wide breakpoint and the overlay is unreachable on touch.
   The shell mounts `view` once and knows nothing else — so the glyph is a
   one-line removal."
  (:require [uix.core :refer [defui $ use-state]]
            [app.keybinding :refer [use-hotkey]]
            [app.keymap :as keymap]
            [app.modal :as modal]))

(defui key-cap [{:keys [label]}]
  ($ :kbd {:class (str "inline-flex min-w-7 items-center justify-center rounded-md "
                       "border border-b-[3px] border-edge bg-surface-hover px-2 py-1 "
                       "font-mono text-[12px] font-semibold text-muted")}
     label))

(defui shortcut-row [{:keys [binding]}]
  ($ :div {:class "flex items-center justify-between gap-6 py-1.5"}
     ($ :span {:class "text-[15px] font-medium text-label"} (:desc binding))
     ($ key-cap {:label (or (:cap binding) (:key binding))})))

(defui shortcut-group [{:keys [label bindings]}]
  ($ :div
     ($ :h3 {:class "mb-1 text-[11px] font-bold uppercase tracking-[0.18em] text-muted"}
        label)
     ($ :div
        (for [b bindings]
          ($ shortcut-row {:key (:id b) :binding b})))))

(defui overlay [{:keys [on-close]}]
  ($ modal/shell
     {:title "Keyboard shortcuts"
      :on-close on-close
      :close-key (keymap/key-of :help)
      :body ($ :<>
               ($ :h2 {:class "mb-6 text-[15px] font-bold uppercase tracking-[0.2em] text-heading"}
                  "Keyboard shortcuts")
               ($ :div {:class "flex flex-col gap-6"}
                  (let [by-group (group-by :group keymap/bindings)]
                    (for [{:keys [id label]} keymap/groups]
                      ($ shortcut-group {:key id :label label :bindings (by-group id)})))))}))

(defui view []
  (let [[open? set-open!] (use-state false)]
    (use-hotkey (keymap/key-of :help) #(set-open! true))
    ($ :<>
       ($ :button {:on-click #(set-open! true)
                   :aria-label "Keyboard shortcuts"
                   :class (str "fixed right-7 top-7 z-20 hidden h-9 w-9 wide:flex "
                               "items-center justify-center rounded-full "
                               "border border-edge/60 text-[16px] font-bold leading-none "
                               "text-muted cursor-pointer transition hover:bg-edge/30")}
          "?")
       (when open?
         ($ overlay {:on-close #(set-open! false)})))))
