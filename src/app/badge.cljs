(ns app.badge
  "App-icon badge over the platform Badging API: a count of pressing work, set
   while the app runs and left on the icon by the OS once it closes — no push,
   no service worker. The count goes stale across a rollover until the next
   foreground, which is acceptable. Unsupported platforms no-op."
  (:require [uix.core :refer [defhook use-callback use-effect use-ref]]))

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

(defhook use-due-badge
  "Badge the count of Due rows on the app icon, and wrap toggle so the first tap
   asks for notification permission — iOS won't render the badge without it."
  [rows toggle]
  (let [due-count (->> rows (filter :due?) count)
        asked?    (use-ref false)]
    (use-app-badge due-count)
    (use-callback
     (fn [row]
       (when-not @asked?
         (reset! asked? true)
         (request-permission!))
       (toggle row))
     [toggle])))
