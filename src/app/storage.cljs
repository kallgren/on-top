(ns app.storage)

(def completions-key "on-top/completions")
(def outbox-key "on-top/outbox")
(def config-key "on-top/config")
(def schedule-cache-key "on-top/schedule-cache")

(defn read-completions []
  (try
    (when-let [raw (.getItem js/localStorage completions-key)]
      (let [data (js/JSON.parse raw)]
        (into {} (for [k    (js/Object.keys data)
                       :let [v (aget data k)]
                       :when (string? v)]
                   [k v]))))
    (catch :default _ nil)))

(defn write-completions! [completions]
  (try
    (.setItem js/localStorage completions-key (js/JSON.stringify (clj->js completions)))
    (catch :default _ nil)))

(defn read-outbox []
  (try
    (when-let [raw (.getItem js/localStorage outbox-key)]
      (let [ids (js/JSON.parse raw)]
        (when (array? ids)
          (into #{} (filter string?) ids))))
    (catch :default _ nil)))

(defn write-outbox! [outbox]
  (try
    (.setItem js/localStorage outbox-key (js/JSON.stringify (clj->js (vec outbox))))
    (catch :default _ nil)))

(defn read-config []
  (try (.getItem js/localStorage config-key) (catch :default _ nil)))

(defn read-schedule-cache []
  (try (.getItem js/localStorage schedule-cache-key) (catch :default _ nil)))

(defn write-schedule-cache! [edn-string]
  (try (.setItem js/localStorage schedule-cache-key edn-string) (catch :default _ nil)))
