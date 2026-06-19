(ns app.storage)

(def storage-key "on-top/done")
(def schedule-url-key "on-top/schedule-url")
(def schedule-cache-key "on-top/schedule-cache")

(defn read-done []
  (try
    (when-let [raw (.getItem js/localStorage storage-key)]
      (let [data (js/JSON.parse raw)]
        (into {} (for [k    (js/Object.keys data)
                       :let [v (aget data k)]
                       :when (string? v)]
                   [k v]))))
    (catch :default _ nil)))

(defn write-done! [done]
  (try
    (.setItem js/localStorage storage-key (js/JSON.stringify (clj->js done)))
    (catch :default _ nil)))

(defn read-schedule-url []
  (try (.getItem js/localStorage schedule-url-key) (catch :default _ nil)))

(defn read-schedule-cache []
  (try (.getItem js/localStorage schedule-cache-key) (catch :default _ nil)))

(defn write-schedule-cache! [edn-string]
  (try (.setItem js/localStorage schedule-cache-key edn-string) (catch :default _ nil)))
