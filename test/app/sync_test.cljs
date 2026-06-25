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

(deftest select-query-filters-the-fetch-to-one-surface
  (is (= "?select=task_id,done_through&surface=eq.core" (sync/select-query "core")))
  (is (= "?select=task_id,done_through&surface=eq.rare" (sync/select-query "rare")))
  (is (not= (sync/select-query "core") (sync/select-query "rare"))
      "each surface hydrates only its own rows, so a shared task id never crosses over"))

(deftest mark-dirty-adds-a-toggled-id-to-the-outbox
  (is (= #{"gmail"} (sync/mark-dirty #{} "gmail")))
  (is (= #{"gmail" "dishes"} (sync/mark-dirty #{"gmail"} "dishes"))))

(deftest flush-payload-builds-a-surface-tagged-upsert-row-per-dirty-id
  (is (= #{{"surface" "core" "task_id" "gmail" "done_through" "2026-06-18"}
           {"surface" "core" "task_id" "dishes" "done_through" "2026-06-17"}}
         (set (sync/flush-payload "core" #{"gmail" "dishes"}
                                  {"gmail" "2026-06-18" "dishes" "2026-06-17" "trash" "2026-06-10"})))))

(deftest flush-payload-keeps-a-shared-task-id-distinct-across-surfaces
  (is (not= (sync/flush-payload "core" #{"gmail"} {"gmail" "2026-06-18"})
            (sync/flush-payload "rare" #{"gmail"} {"gmail" "2026-06-18"}))
      "same task id on two surfaces upserts two distinct (surface, task_id) rows"))

(deftest clear-pending-drops-confirmed-ids
  (is (= #{} (sync/clear-pending #{"gmail" "dishes"} #{"gmail" "dishes"}))))

(deftest clear-pending-keeps-ids-toggled-again-mid-flight
  (is (= #{"gmail"} (sync/clear-pending #{"gmail" "dishes"} #{"dishes"}))))
