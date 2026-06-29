(ns app.badge
  "App-icon badge over the platform Badging API: a count of pressing work, set
   while the app runs and left on the icon by the OS once it closes — no push,
   no service worker. The count goes stale across a rollover until the next
   foreground, which is acceptable. Unsupported platforms no-op."
  (:require [uix.core :refer [defhook use-effect]]))

(defn supported? []
  (boolean (and (exists? js/navigator) (.-setAppBadge js/navigator))))

(defn- set-count! [n]
  (when (supported?)
    (-> (if (pos? n)
          (.setAppBadge js/navigator n)
          (.clearAppBadge js/navigator))
        (.catch (fn [_])))))

(defn request-permission!
  "iOS only shows the badge once notifications are permitted, even though we send
   none. Must run from a user gesture; harmless and one-shot elsewhere."
  []
  (when (and (exists? js/Notification)
             (= "default" (.-permission js/Notification)))
    (.requestPermission js/Notification)))

(defhook use-app-badge [n]
  (use-effect
   (fn [] (set-count! n) js/undefined)
   [n]))
