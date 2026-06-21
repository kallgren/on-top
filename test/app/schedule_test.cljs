(ns app.schedule-test
  (:require [cljs.test :refer [deftest is]]
            [app.schedule :as schedule]))

(deftest parse-schedule-reads-a-valid-map
  (is (= {:digital {:week-odd {:monday ["Gmail"]}}}
         (schedule/parse-schedule "{:digital {:week-odd {:monday [\"Gmail\"]}}}"))))

(deftest parse-schedule-rejects-unparseable-input
  (is (nil? (schedule/parse-schedule "{:digital not closed"))))

(deftest parse-schedule-rejects-non-map-edn
  (is (nil? (schedule/parse-schedule "[\"Gmail\" \"Calendar\"]")))
  (is (nil? (schedule/parse-schedule "42"))))

(deftest parse-schedule-of-nil-is-nil
  (is (nil? (schedule/parse-schedule nil))))

(deftest slice-extracts-a-surfaces-slice
  (let [combined {:core {:digital {:week-odd {:monday ["Gmail"]}}}
                  :rare {:digital {"monthly" [{:id "back-up"}]}}}]
    (is (= {:digital {:week-odd {:monday ["Gmail"]}}}
           (schedule/slice combined :core)))
    (is (= {:digital {"monthly" [{:id "back-up"}]}}
           (schedule/slice combined :rare)))))

(deftest slice-of-missing-surface-is-nil
  (is (nil? (schedule/slice {:core {:digital {}}} :rare))))

(deftest slice-of-nil-combined-is-nil
  (is (nil? (schedule/slice nil :core))))

(deftest resolve-schedule-prefers-gist-then-cache-then-seed
  (let [gist   {:digital {:week-odd {:monday ["Gist"]}}}
        cached {:digital {:week-odd {:monday ["Cached"]}}}
        seed   {:digital {:week-odd {:monday ["Seed"]}}}]
    (is (= gist   (schedule/resolve-schedule gist cached seed)))
    (is (= cached (schedule/resolve-schedule nil cached seed)))
    (is (= seed   (schedule/resolve-schedule nil nil seed)))))
