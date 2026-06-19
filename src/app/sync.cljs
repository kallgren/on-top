(ns app.sync
  "Remote completions sync: Supabase is the source of truth, localStorage is a
   read-cache only. Hydrate fetches the whole completions map and reconciles it
   with the local pending outbox (pending wins). See docs/adr/0007.")

(defn parse-completions [s]
  (try
    (let [rows (js->clj (js/JSON.parse s))]
      (when (vector? rows)
        (into {} (for [row   rows
                       :let  [id (get row "task_id") through (get row "done_through")]
                       :when (and (string? id) (string? through))]
                   [id through]))))
    (catch :default _ nil)))

(defn reconcile [remote pending]
  (merge remote pending))

(defn fetch-completions! [url publishable-key on-ok]
  (-> (js/fetch (str url "?select=task_id,done_through")
                #js {:headers #js {:apikey        publishable-key
                                   :Authorization (str "Bearer " publishable-key)}})
      (.then (fn [res]
               (if (.-ok res)
                 (.text res)
                 (throw (js/Error. (str "HTTP " (.-status res)))))))
      (.then (fn [raw]
               (if-let [parsed (parse-completions raw)]
                 (on-ok parsed)
                 (throw (js/Error. "not a valid completions map")))))
      (.catch (fn [err]
                (js/console.warn "on-top: ignoring remote completions —"
                                 (.-message err))))))
