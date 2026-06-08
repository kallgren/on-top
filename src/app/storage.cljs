(ns app.storage)

(def storage-key "on-top/done")

(defn day-stamp [date]
  (.toLocaleDateString date "en-CA"))   ; YYYY-MM-DD, local

(defn read-done [stamp]
  (try
    (when-let [raw (.getItem js/localStorage storage-key)]
      (let [data (js/JSON.parse raw)]
        (when (= (.-date data) stamp)
          (set (.-names data)))))
    (catch :default _ nil)))

(defn write-done! [stamp names]
  (try
    (.setItem js/localStorage storage-key
              (js/JSON.stringify #js {:date stamp :names (clj->js (vec names))}))
    (catch :default _ nil)))
