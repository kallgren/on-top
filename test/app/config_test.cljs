(ns app.config-test
  (:require [cljs.test :refer [deftest is]]
            [app.config :as config]))

(deftest parse-config-reads-a-full-blob
  (is (= {:completions-db-url       "https://x.supabase.co/rest/v1/completions"
          :supabase-publishable-key "sb_publishable_x"
          :core-schedule-url        "https://gist.example/core"
          :rare-schedule-url        "https://gist.example/rare"}
         (config/parse-config
          (str "{\"completionsDbUrl\": \"https://x.supabase.co/rest/v1/completions\", "
               "\"supabasePublishableKey\": \"sb_publishable_x\", "
               "\"coreScheduleUrl\": \"https://gist.example/core\", "
               "\"rareScheduleUrl\": \"https://gist.example/rare\"}")))))

(deftest parse-config-maps-completions-db-url
  (is (= {:completions-db-url "https://x.supabase.co/rest/v1/completions"}
         (config/parse-config
          "{\"completionsDbUrl\": \"https://x.supabase.co/rest/v1/completions\"}"))))

(deftest parse-config-maps-supabase-publishable-key
  (is (= {:supabase-publishable-key "sb_publishable_x"}
         (config/parse-config "{\"supabasePublishableKey\": \"sb_publishable_x\"}"))))

(deftest parse-config-maps-core-schedule-url
  (is (= {:core-schedule-url "https://gist.example/core"}
         (config/parse-config "{\"coreScheduleUrl\": \"https://gist.example/core\"}"))))

(deftest parse-config-maps-rare-schedule-url
  (is (= {:rare-schedule-url "https://gist.example/rare"}
         (config/parse-config "{\"rareScheduleUrl\": \"https://gist.example/rare\"}"))))

(deftest parse-config-ignores-unknown-keys
  (is (= {:core-schedule-url "https://gist.example/core"}
         (config/parse-config
          "{\"coreScheduleUrl\": \"https://gist.example/core\", \"bogus\": \"x\"}"))))

(deftest parse-config-ignores-the-renamed-single-schedule-url
  (is (= {} (config/parse-config "{\"scheduleUrl\": \"https://gist.example/raw\"}")))
  (is (= ["scheduleUrl"]
         (config/unknown-keys "{\"scheduleUrl\": \"https://gist.example/raw\"}"))))

(deftest parse-config-ignores-non-string-values
  (is (= {} (config/parse-config "{\"coreScheduleUrl\": 42}"))))

(deftest parse-config-of-unparseable-input-is-empty
  (is (= {} (config/parse-config "{\"coreScheduleUrl\": not closed"))))

(deftest parse-config-of-non-object-json-is-empty
  (is (= {} (config/parse-config "[\"https://gist.example/raw\"]")))
  (is (= {} (config/parse-config "42"))))

(deftest parse-config-of-absent-or-blank-is-empty
  (is (= {} (config/parse-config nil)))
  (is (= {} (config/parse-config ""))))

(deftest remote-creds-when-url-and-key-are-both-present
  (is (= {:url "https://x.supabase.co/rest/v1/completions" :key "sb_publishable_x"}
         (config/remote-creds {:completions-db-url       "https://x.supabase.co/rest/v1/completions"
                               :supabase-publishable-key "sb_publishable_x"}))))

(deftest remote-creds-is-nil-without-both
  (is (nil? (config/remote-creds {:completions-db-url "https://x.supabase.co/rest/v1/completions"})))
  (is (nil? (config/remote-creds {:supabase-publishable-key "sb_publishable_x"})))
  (is (nil? (config/remote-creds {:completions-db-url "" :supabase-publishable-key "sb_publishable_x"})))
  (is (nil? (config/remote-creds {}))))

(deftest unknown-keys-lists-keys-outside-the-allow-list
  (is (= ["supabaseAnonKey"]
         (config/unknown-keys
          "{\"completionsDbUrl\": \"x\", \"supabaseAnonKey\": \"y\"}"))))

(deftest unknown-keys-is-nil-when-every-key-is-recognized
  (is (nil? (config/unknown-keys
             (str "{\"completionsDbUrl\": \"x\", "
                  "\"supabasePublishableKey\": \"y\", "
                  "\"coreScheduleUrl\": \"z\", "
                  "\"rareScheduleUrl\": \"w\"}"))))
  (is (nil? (config/unknown-keys "{}"))))

(deftest unknown-keys-of-unparseable-or-non-object-is-nil
  (is (nil? (config/unknown-keys "{\"coreScheduleUrl\": not closed")))
  (is (nil? (config/unknown-keys "[\"x\"]")))
  (is (nil? (config/unknown-keys nil))))
