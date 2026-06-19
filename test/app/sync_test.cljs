(ns app.sync-test
  (:require [cljs.test :refer [deftest is]]
            [app.sync :as sync]))

(deftest reconcile-lets-pending-win-over-remote
  (is (= {"gmail" "2026-06-18"}
         (sync/reconcile {"gmail" "2026-06-10"} {"gmail" "2026-06-18"}))))

(deftest reconcile-fills-remaining-tasks-from-remote
  (is (= {"gmail" "2026-06-18" "dishes" "2026-06-17"}
         (sync/reconcile {"gmail" "2026-06-10" "dishes" "2026-06-17"}
                         {"gmail" "2026-06-18"}))))

(deftest reconcile-with-empty-pending-is-the-remote-map
  (is (= {"gmail" "2026-06-10"}
         (sync/reconcile {"gmail" "2026-06-10"} {}))))

(deftest reconcile-with-empty-remote-yields-pending
  (is (= {"gmail" "2026-06-18"}
         (sync/reconcile {} {"gmail" "2026-06-18"}))))

(deftest parse-completions-reads-rows-into-a-map
  (is (= {"gmail" "2026-06-18" "dishes" "2026-06-17"}
         (sync/parse-completions
          (str "[{\"task_id\": \"gmail\", \"done_through\": \"2026-06-18\"}, "
               "{\"task_id\": \"dishes\", \"done_through\": \"2026-06-17\"}]")))))

(deftest parse-completions-of-an-empty-table-is-an-empty-map
  (is (= {} (sync/parse-completions "[]"))))

(deftest parse-completions-skips-rows-missing-a-string-field
  (is (= {"gmail" "2026-06-18"}
         (sync/parse-completions
          (str "[{\"task_id\": \"gmail\", \"done_through\": \"2026-06-18\"}, "
               "{\"task_id\": \"dishes\", \"done_through\": null}]")))))

(deftest parse-completions-rejects-non-array-json
  (is (nil? (sync/parse-completions "{\"task_id\": \"gmail\"}")))
  (is (nil? (sync/parse-completions "42"))))

(deftest parse-completions-of-unparseable-input-is-nil
  (is (nil? (sync/parse-completions "[{\"task_id\": not closed"))))

(deftest parse-completions-of-nil-is-nil
  (is (nil? (sync/parse-completions nil))))
