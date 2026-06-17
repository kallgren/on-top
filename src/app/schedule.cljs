(ns app.schedule
  "Runtime resolution of the Schedule: a remote gist override over the
   compiled-in seed schedule. See docs/adr/0005."
  (:require [cljs.reader :as reader]))

(defn parse-schedule [s]
  (try
    (let [parsed (reader/read-string s)]
      (when (map? parsed) parsed))
    (catch :default _ nil)))

(defn resolve-schedule [cached seed]
  (or cached seed))

(defn fetch-schedule! [url on-ok]
  (-> (js/fetch url)
      (.then (fn [res]
               (if (.-ok res)
                 (.text res)
                 (throw (js/Error. (str "HTTP " (.-status res)))))))
      (.then (fn [raw]
               (if-let [parsed (parse-schedule raw)]
                 (on-ok raw parsed)
                 (throw (js/Error. "not a valid schedule")))))
      (.catch (fn [err]
                (js/console.warn "on-top: ignoring remote schedule —"
                                 (.-message err))))))
