(ns app.config-test
  (:require [cljs.test :refer [deftest is]]
            [app.config :as config]))

(deftest parse-config-reads-a-full-blob
  (is (= {:completions-db-url       "https://x.supabase.co/rest/v1/completions"
          :supabase-publishable-key "sb_publishable_x"
          :gist-url                 "https://gist.github.com/me/abc"}
         (config/parse-config
          (str "{\"completionsDbUrl\": \"https://x.supabase.co/rest/v1/completions\", "
               "\"supabasePublishableKey\": \"sb_publishable_x\", "
               "\"gistUrl\": \"https://gist.github.com/me/abc\"}")))))

(deftest parse-config-maps-completions-db-url
  (is (= {:completions-db-url "https://x.supabase.co/rest/v1/completions"}
         (config/parse-config
          "{\"completionsDbUrl\": \"https://x.supabase.co/rest/v1/completions\"}"))))

(deftest parse-config-maps-supabase-publishable-key
  (is (= {:supabase-publishable-key "sb_publishable_x"}
         (config/parse-config "{\"supabasePublishableKey\": \"sb_publishable_x\"}"))))

(deftest parse-config-maps-gist-url
  (is (= {:gist-url "https://gist.github.com/me/abc"}
         (config/parse-config "{\"gistUrl\": \"https://gist.github.com/me/abc\"}"))))

(deftest parse-config-ignores-unknown-keys
  (is (= {:gist-url "https://gist.github.com/me/abc"}
         (config/parse-config
          "{\"gistUrl\": \"https://gist.github.com/me/abc\", \"bogus\": \"x\"}"))))

(deftest parse-config-ignores-the-retired-per-surface-urls
  (is (= {} (config/parse-config "{\"coreScheduleUrl\": \"https://gist.example/core\"}")))
  (is (= ["coreScheduleUrl"]
         (config/unknown-keys "{\"coreScheduleUrl\": \"https://gist.example/core\"}"))))

(deftest parse-config-ignores-non-string-values
  (is (= {} (config/parse-config "{\"gistUrl\": 42}"))))

(deftest parse-config-of-unparseable-input-is-empty
  (is (= {} (config/parse-config "{\"gistUrl\": not closed"))))

(deftest parse-config-of-non-object-json-is-empty
  (is (= {} (config/parse-config "[\"https://gist.example/raw\"]")))
  (is (= {} (config/parse-config "42"))))

(deftest parse-config-of-absent-or-blank-is-empty
  (is (= {} (config/parse-config nil)))
  (is (= {} (config/parse-config ""))))

(deftest gist-file-url-derives-a-raw-latest-url-from-a-gist-page-url
  (is (= "https://gist.githubusercontent.com/me/abc/raw/core.edn"
         (config/gist-file-url {:gist-url "https://gist.github.com/me/abc"} "core.edn"))))

(deftest gist-file-url-tolerates-a-trailing-slash
  (is (= "https://gist.githubusercontent.com/me/abc/raw/notes.md"
         (config/gist-file-url {:gist-url "https://gist.github.com/me/abc/"} "notes.md"))))

(deftest gist-file-url-rewrites-a-raw-file-url-onto-the-target-file
  (is (= "https://gist.githubusercontent.com/me/abc/raw/day.edn"
         (config/gist-file-url
          {:gist-url "https://gist.githubusercontent.com/me/abc/raw/core.edn"} "day.edn"))))

(deftest gist-file-url-is-nil-without-a-gist
  (is (nil? (config/gist-file-url {} "core.edn")))
  (is (nil? (config/gist-file-url {:gist-url ""} "core.edn"))))

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
                  "\"gistUrl\": \"z\"}"))))
  (is (nil? (config/unknown-keys "{}"))))

(deftest unknown-keys-of-unparseable-or-non-object-is-nil
  (is (nil? (config/unknown-keys "{\"gistUrl\": not closed")))
  (is (nil? (config/unknown-keys "[\"x\"]")))
  (is (nil? (config/unknown-keys nil))))
