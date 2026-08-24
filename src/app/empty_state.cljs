(ns app.empty-state
  (:require [uix.core :refer [defui $]]))

(defui view []
  ($ :p {:class "py-20 text-center text-[17px] font-medium italic text-muted tracking-wide text-inset"}
     "You're on top :)"))
