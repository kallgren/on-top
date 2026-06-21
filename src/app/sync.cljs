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

(defn mark-dirty [outbox id]
  (conj outbox id))

(defn flush-payload [surface outbox completions]
  (for [id outbox]
    {"surface" surface "task_id" id "done_through" (get completions id)}))

(defn clear-pending [outbox confirmed]
  (reduce disj outbox confirmed))

(defn select-query [surface]
  (str "?select=task_id,done_through&surface=eq." surface))

(defn fetch-completions! [url publishable-key surface on-ok]
  (-> (js/fetch (str url (select-query surface))
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

(defn upsert-completions! [url publishable-key payload on-ok]
  (-> (js/fetch url
                #js {:method  "POST"
                     :headers #js {:apikey        publishable-key
                                   :Authorization (str "Bearer " publishable-key)
                                   :Content-Type  "application/json"
                                   :Prefer        "resolution=merge-duplicates"}
                     :body    (js/JSON.stringify (clj->js payload))})
      (.then (fn [res]
               (if (.-ok res)
                 (on-ok)
                 (throw (js/Error. (str "HTTP " (.-status res)))))))
      (.catch (fn [err]
                (js/console.warn "on-top: completions flush failed —"
                                 (.-message err))))))
