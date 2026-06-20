(ns app.config
  "Device-local remote config: a single localStorage JSON blob holding the
   completions DB URL, Supabase publishable key, and the schedule's gist URL.
   The blob uses camelCase JSON keys; parse-config maps them to kebab keywords.
   See docs/adr/0007."
  (:require [clojure.string :as str]))

(def ^:private key->keyword
  {"completionsDbUrl"       :completions-db-url
   "supabasePublishableKey" :supabase-publishable-key
   "scheduleUrl"            :schedule-url})

(def ^:private known-keys (set (keys key->keyword)))

(defn- parse-json-object [s]
  (try
    (let [m (js->clj (js/JSON.parse s))]
      (when (map? m) m))
    (catch :default _ nil)))

(defn parse-config [s]
  (let [m (parse-json-object s)]
    (reduce-kv
     (fn [acc json-key kw]
       (let [v (get m json-key)]
         (cond-> acc (string? v) (assoc kw v))))
     {}
     key->keyword)))

(defn unknown-keys [s]
  (seq (remove known-keys (keys (parse-json-object s)))))

(defn warn-unknown-keys! [s]
  (when-let [ks (unknown-keys s)]
    (js/console.warn (str "on-top: ignoring unknown config keys — " (str/join ", " ks)))))

(defn remote-creds [{:keys [completions-db-url supabase-publishable-key]}]
  (when (and (not-empty completions-db-url) (not-empty supabase-publishable-key))
    {:url completions-db-url :key supabase-publishable-key}))
