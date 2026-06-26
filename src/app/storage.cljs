(ns app.storage)

(def config-key "on-top/config")

(defn read-completions [key]
  (try
    (when-let [raw (.getItem js/localStorage key)]
      (let [data (js/JSON.parse raw)]
        (into {} (for [k    (js/Object.keys data)
                       :let [v (aget data k)]
                       :when (string? v)]
                   [k v]))))
    (catch :default _ nil)))

(defn write-completions! [key completions]
  (try
    (.setItem js/localStorage key (js/JSON.stringify (clj->js completions)))
    (catch :default _ nil)))

(defn read-outbox [key]
  (try
    (when-let [raw (.getItem js/localStorage key)]
      (let [ids (js/JSON.parse raw)]
        (when (array? ids)
          (into #{} (filter string?) ids))))
    (catch :default _ nil)))

(defn write-outbox! [key outbox]
  (try
    (.setItem js/localStorage key (js/JSON.stringify (clj->js (vec outbox))))
    (catch :default _ nil)))

(defn read-config []
  (try (.getItem js/localStorage config-key) (catch :default _ nil)))

(defn read-text-cache [key]
  (try (.getItem js/localStorage key) (catch :default _ nil)))

(defn write-text-cache! [key text]
  (try (.setItem js/localStorage key text) (catch :default _ nil)))
