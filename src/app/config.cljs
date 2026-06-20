(ns app.config
  "Device-local remote config: a single localStorage JSON blob holding the
   completions DB URL, Supabase publishable key, and the schedule's gist URL.
   The blob uses camelCase JSON keys; parse-config maps them to kebab keywords.
   See docs/adr/0007.")

(defn parse-config [s]
  (try
    (let [m (js->clj (js/JSON.parse s))]
      (if (map? m)
        (cond-> {}
          (string? (m "completionsDbUrl"))       (assoc :completions-db-url (m "completionsDbUrl"))
          (string? (m "supabasePublishableKey")) (assoc :supabase-publishable-key (m "supabasePublishableKey"))
          (string? (m "scheduleUrl"))            (assoc :schedule-url (m "scheduleUrl")))
        {}))
    (catch :default _ {})))

(defn remote-creds [{:keys [completions-db-url supabase-publishable-key]}]
  (when (and (not-empty completions-db-url) (not-empty supabase-publishable-key))
    {:url completions-db-url :key supabase-publishable-key}))
