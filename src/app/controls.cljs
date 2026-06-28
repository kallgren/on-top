(ns app.controls
  "Shared chrome controls. `corner-button` is the wide-only glyph button parked in
   a top corner — the resting state is bare, gaining a background only on hover, so
   the corners stay quiet until reached for. Callers supply the corner via `class`
   (e.g. `left-7`), the glyph as children, and `active?` to lift the glyph from
   muted to label colour without adding any fill."
  (:require [uix.core :refer [defui $]]))

(defui corner-button [{:keys [on-click label class active? aria-pressed children]}]
  ($ :button {:on-click on-click
              :aria-label label
              :aria-pressed aria-pressed
              :class (str "fixed top-7 z-20 hidden h-9 w-9 wide:flex items-center "
                          "justify-center rounded-lg cursor-pointer transition "
                          "hover:bg-edge/30 hover:text-label "
                          (if active? "text-label " "text-muted ")
                          class)}
     children))
