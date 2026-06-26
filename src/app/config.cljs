(ns app.config
  "Device-local remote config: a single localStorage JSON blob holding the
   completions DB URL, Supabase publishable key, and one gist URL holding every
   remote file — the Core, Rare, and Day Schedules and the global Notes file,
   under fixed names (core.edn, rare.edn, day.edn, notes.md). The blob uses
   camelCase JSON keys; parse-config maps them to kebab keywords. See
   docs/adr/0007 and docs/adr/0010."
  (:require [clojure.string :as str]))

(def ^:private key->keyword
  {"completionsDbUrl"       :completions-db-url
   "supabasePublishableKey" :supabase-publishable-key
   "gistUrl"                :gist-url})

(def core-schedule-file "core.edn")
(def rare-schedule-file "rare.edn")
(def day-schedule-file "day.edn")
(def notes-file "notes.md")

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

(defn gist-file-url
  "The raw-latest URL for `filename` within the configured gist, or nil when no
   gist is set. GitHub serves a gist file's newest revision at .../raw/<name>,
   so any gist URL (page or raw) reduces to the host swap plus that suffix."
  [{:keys [gist-url]} filename]
  (when-let [base (not-empty gist-url)]
    (-> base
        (str/replace #"/raw(/[^/]*)?/?$" "")
        (str/replace #"/+$" "")
        (str/replace "gist.github.com" "gist.githubusercontent.com")
        (str "/raw/" filename))))
