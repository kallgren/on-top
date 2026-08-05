(ns app.rare.cards-test
  (:require [cljs.test :refer [deftest is]]
            [app.rare.cards :as cards]))

(deftest partition-tasks-splits-by-state
  (let [tasks [{:key "c" :sort-key 3 :done? true}
               {:key "a" :sort-key 1}
               {:key "u" :sort-key 2 :upcoming? true}]
        {:keys [completed current upcoming]} (cards/partition-tasks tasks)]
    (is (= ["c"] (map :key completed)) "done? → completed")
    (is (= ["a"] (map :key current)) "neither done? nor upcoming? → current")
    (is (= ["u"] (map :key upcoming)) "upcoming? → upcoming")))

(deftest partition-tasks-respects-sort-key
  (let [tasks [{:key "b" :sort-key 2}
               {:key "a" :sort-key 1}
               {:key "c" :sort-key 3}]]
    (is (= ["a" "b" "c"] (map :key (:current (cards/partition-tasks tasks))))
        "current is ordered by :sort-key")))

(def two-categories
  [[:digital "Digital"]
   [:household "Household"]])

(def two-plus-categories
  [[:digital "Digital"]
   [:household "Household"]
   [:errands "Errands"]])

(deftest build-cards-drops-categories-with-no-rows
  (let [by-cat {:digital [{:key "a" :sort-key 1}]
                :household []}]
    (is (= [:digital] (map :cat (cards/build-cards by-cat two-categories {} false)))
        "categories with no rows are dropped when cleared is hidden")
    (is (= [:digital] (map :cat (cards/build-cards by-cat two-categories {} true)))
        "categories with no rows are dropped even when cleared is shown")))

(deftest build-cards-hides-cleared-categories-by-default
  (let [by-cat {:digital   [{:key "a" :sort-key 1}]
                :household [{:key "d" :sort-key 1 :done? true}]
                :errands   [{:key "u" :sort-key 1 :upcoming? true}]}]
    (is (= [:digital] (map :cat (cards/build-cards by-cat two-plus-categories {} false)))
        "a category with nothing Current is dropped when cleared is hidden")))

(deftest build-cards-reveals-cleared-categories-when-shown
  (let [by-cat {:digital   [{:key "a" :sort-key 1}]
                :household [{:key "d" :sort-key 1 :done? true}]
                :errands   [{:key "u" :sort-key 1 :upcoming? true}]}]
    (is (= [:digital :household :errands]
           (map :cat (cards/build-cards by-cat two-plus-categories {} true)))
        "completed-only and upcoming-only categories reappear when cleared is shown")))

(deftest build-cards-preserves-category-order
  (let [by-cat {:digital   [{:key "a" :sort-key 1}]
                :household [{:key "b" :sort-key 1}]}
        built  (cards/build-cards by-cat two-categories {} false)]
    (is (= [:digital :household] (map :cat built))
        "cards follow the categories order")))

(deftest build-cards-reads-fold-state-from-expanded
  (let [by-cat {:digital [{:key "a" :sort-key 1}]}
        cats   [[:digital "Digital"]]]
    (is (= {:show-completed? nil :show-upcoming? nil}
           (select-keys (first (cards/build-cards by-cat cats {} false)) [:show-completed? :show-upcoming?]))
        "absent fold state reads as nil")
    (is (= {:show-completed? true :show-upcoming? nil}
           (select-keys (first (cards/build-cards by-cat cats {:digital {:completed? true}} false))
                        [:show-completed? :show-upcoming?]))
        "partial fold state reads each flag independently")
    (is (= {:show-completed? true :show-upcoming? true}
           (select-keys (first (cards/build-cards by-cat cats {:digital {:completed? true :upcoming? true}} false))
                        [:show-completed? :show-upcoming?]))
        "both flags read from the map")))

(defn- card [cat completed current upcoming show-completed? show-upcoming?]
  {:cat cat :completed completed :current current :upcoming upcoming
   :show-completed? show-completed? :show-upcoming? show-upcoming?})

(deftest visible-rows-collapsed-fold-contributes-nothing
  (let [c (card :digital [{:key "comp"}] [{:key "cur"}] [{:key "up"}] false false)]
    (is (= ["cur"] (map :key (cards/visible-rows [c])))
        "only current rows when both folds are collapsed")))

(deftest visible-rows-expanded-completed-sits-above-current
  (let [c (card :digital [{:key "comp"}] [{:key "cur"}] [{:key "up"}] true false)]
    (is (= ["comp" "cur"] (map :key (cards/visible-rows [c])))
        "expanded Completed is prepended above current")))

(deftest visible-rows-expanded-upcoming-sits-below-current
  (let [c (card :digital [{:key "comp"}] [{:key "cur"}] [{:key "up"}] false true)]
    (is (= ["cur" "up"] (map :key (cards/visible-rows [c])))
        "expanded Upcoming is appended below current")))

(deftest visible-rows-both-folds-order-completed-current-upcoming
  (let [c (card :digital [{:key "comp"}] [{:key "cur"}] [{:key "up"}] true true)]
    (is (= ["comp" "cur" "up"] (map :key (cards/visible-rows [c])))
        "completed, then current, then upcoming")))

(deftest visible-rows-concatenates-cards-in-order
  (let [a (card :digital [] [{:key "a"}] [] false false)
        b (card :household [] [{:key "b"}] [] false false)]
    (is (= ["a" "b"] (map :key (cards/visible-rows [a b])))
        "multiple cards concatenate in card order")))

(deftest visible-rows-returns-a-vector
  (is (vector? (cards/visible-rows []))))
