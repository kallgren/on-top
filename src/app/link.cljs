(ns app.link
  "A Task's Link (see CONTEXT-MAP). The Notes file it comes from can be a remote
   gist, so a Link is untrusted input and `openable` is the one gate it passes."
  (:require [clojure.string :as str]
            [uix.core :refer [defhook]]
            [app.keybinding :refer [use-hotkey]]
            [app.keymap :as keymap]))

(def ^:private blocked-schemes #{"javascript" "data" "vbscript"})

(def ^:private scheme-re #"^([a-zA-Z][a-zA-Z0-9+.\-]*):")

(defn openable [link]
  (let [url (str/trim (or link ""))]
    (when-not (re-find #"\s" url)
      (when-let [[_ scheme] (re-find scheme-re url)]
        (when-not (contains? blocked-schemes (str/lower-case scheme))
          url)))))

(defn open! [url]
  (.open js/window url "_blank" "noopener"))

(defhook use-open-link [row]
  (use-hotkey (keymap/key-of :open-link)
              #(when-let [url (openable (:link row))]
                 (open! url))
              {:repeats? false}))
