(ns app.config-test
  (:require [cljs.test :refer [deftest is]]
            [app.config :as config]))

(deftest parse-config-reads-a-full-blob
  (is (= {:completions-db-url "https://x.supabase.co/rest/v1/completions"
          :supabase-anon-key  "anon"
          :schedule-url       "https://gist.example/raw"}
         (config/parse-config
          (str "{\"completionsDbUrl\": \"https://x.supabase.co/rest/v1/completions\", "
               "\"supabaseAnonKey\": \"anon\", "
               "\"scheduleUrl\": \"https://gist.example/raw\"}")))))

(deftest parse-config-maps-completions-db-url
  (is (= {:completions-db-url "https://x.supabase.co/rest/v1/completions"}
         (config/parse-config
          "{\"completionsDbUrl\": \"https://x.supabase.co/rest/v1/completions\"}"))))

(deftest parse-config-maps-supabase-anon-key
  (is (= {:supabase-anon-key "anon"}
         (config/parse-config "{\"supabaseAnonKey\": \"anon\"}"))))

(deftest parse-config-maps-schedule-url
  (is (= {:schedule-url "https://gist.example/raw"}
         (config/parse-config "{\"scheduleUrl\": \"https://gist.example/raw\"}"))))

(deftest parse-config-ignores-unknown-keys
  (is (= {:schedule-url "https://gist.example/raw"}
         (config/parse-config
          "{\"scheduleUrl\": \"https://gist.example/raw\", \"bogus\": \"x\"}"))))

(deftest parse-config-ignores-non-string-values
  (is (= {} (config/parse-config "{\"scheduleUrl\": 42}"))))

(deftest parse-config-of-unparseable-input-is-empty
  (is (= {} (config/parse-config "{\"scheduleUrl\": not closed"))))

(deftest parse-config-of-non-object-json-is-empty
  (is (= {} (config/parse-config "[\"https://gist.example/raw\"]")))
  (is (= {} (config/parse-config "42"))))

(deftest parse-config-of-absent-or-blank-is-empty
  (is (= {} (config/parse-config nil)))
  (is (= {} (config/parse-config ""))))
