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

(deftest resolve-schedule-prefers-cached-over-seed
  (let [cached {:digital {:week-odd {:monday ["Cached"]}}}
        seed   {:digital {:week-odd {:monday ["Seed"]}}}]
    (is (= cached (schedule/resolve-schedule cached seed)))
    (is (= seed (schedule/resolve-schedule nil seed)))))
