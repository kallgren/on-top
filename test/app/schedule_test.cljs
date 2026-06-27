(ns app.schedule-test
  (:require [cljs.test :refer [deftest is]]
            [app.schedule :as schedule]))

(deftest parse-schedule-reads-a-valid-map
  (is (= {:digital {:week-odd {:monday ["Gmail"]}}}
         (schedule/parse-schedule "{:digital {:week-odd {:monday [\"Gmail\"]}}}"))))

(deftest parse-schedule-reads-a-valid-vector
  (is (= [{:id "wake" :start "07:00" :name "Wake"}]
         (schedule/parse-schedule "[{:id \"wake\" :start \"07:00\" :name \"Wake\"}]"))))

(deftest parse-schedule-rejects-unparseable-input
  (is (nil? (schedule/parse-schedule "{:digital not closed"))))

(deftest parse-schedule-rejects-non-collection-edn
  (is (nil? (schedule/parse-schedule "\"Gmail\"")))
  (is (nil? (schedule/parse-schedule "42"))))

(deftest parse-schedule-of-nil-is-nil
  (is (nil? (schedule/parse-schedule nil))))

(deftest resolve-schedule-prefers-cached-over-seed
  (let [cached {:digital {:week-odd {:monday ["Cached"]}}}
        seed   {:digital {:week-odd {:monday ["Seed"]}}}]
    (is (= cached (schedule/resolve-schedule cached seed)))
    (is (= seed (schedule/resolve-schedule nil seed)))))

(deftest schedule-source-paints-cached-edn-over-seed
  (let [seed {:digital {:week-odd {:monday ["Seed"]}}}]
    (is (= {:digital {:week-odd {:monday ["Cached"]}}}
           (:initial (schedule/schedule-source
                      {:config-url "https://gist.example/x"
                       :cached     "{:digital {:week-odd {:monday [\"Cached\"]}}}"
                       :seed       seed}))))))

(deftest schedule-source-falls-back-to-seed-without-a-cache
  (let [seed {:digital {:week-odd {:monday ["Seed"]}}}]
    (is (= seed (:initial (schedule/schedule-source
                           {:config-url "https://gist.example/x" :cached nil :seed seed})))
        "no cache → the seed floor")
    (is (= seed (:initial (schedule/schedule-source
                           {:config-url "https://gist.example/x" :cached "{:not closed" :seed seed})))
        "unparseable cache never poisons the painted Schedule")))

(deftest schedule-source-targets-the-configured-gist-url
  (is (= "https://gist.example/rare"
         (:url (schedule/schedule-source
                {:config-url "https://gist.example/rare" :cached nil :seed {}})))))

(deftest schedule-source-has-no-url-when-the-surface-gist-is-unconfigured
  (is (nil? (:url (schedule/schedule-source {:config-url nil :cached nil :seed {}}))))
  (is (nil? (:url (schedule/schedule-source {:config-url "" :cached nil :seed {}})))))

(deftest schedule->categories-derives-pairs-from-core-shape-keys
  (is (= [[:digital "Digital"] [:household "Household"]]
         (schedule/schedule->categories
          (array-map :digital   {:week-odd {:monday ["gmail"]}}
                     :household {:week-odd {:monday ["bathroom"]}})))))

(deftest schedule->categories-derives-pairs-from-rare-shape-keys
  (is (= [[:digital "Digital"] [:household "Household"]]
         (schedule/schedule->categories
          (array-map :digital   {"monthly" [{:id "review-subs" :anchor "Jun 6"}]}
                     :household {"2 weeks" [{:id "change-bed-linen" :anchor "Jun 13"}]})))))

(deftest schedule->categories-title-cases-dashed-keys-word-by-word
  (is (= [[:home-office "Home Office"]]
         (schedule/schedule->categories (array-map :home-office {})))))

(deftest schedule->categories-preserves-file-order
  (is (= [:household :digital :errands]
         (map first
              (schedule/schedule->categories
               (array-map :household {} :digital {} :errands {}))))))

(deftest schedule->categories-of-an-empty-schedule-is-empty
  (is (= [] (schedule/schedule->categories {}))))
