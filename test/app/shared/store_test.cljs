(ns app.shared.store-test
  (:require [cljs.test :refer [deftest is]]
            [app.shared.store :as store]))

(deftest toggled-writes-a-value-and-marks-the-id-dirty
  (is (= {:completions {"gmail" "2026-06-18"} :outbox #{"gmail"}}
         (store/toggled {:completions {} :outbox #{}} "gmail" "2026-06-18"))))

(deftest hydrated-keeps-pending-over-remote-and-fills-the-rest
  (is (= {:completions {"gmail" "2026-06-18" "dishes" "2026-06-17"} :outbox #{"gmail"}}
         (store/hydrated {:completions {"gmail" "2026-06-18"} :outbox #{"gmail"}}
                         {"gmail" "2026-06-10" "dishes" "2026-06-17"}))))

(deftest flush-confirmed-clears-confirmed-ids-but-keeps-ids-toggled-again-mid-flight
  (is (= {:completions {"gmail" "2026-06-18" "dishes" "2026-06-17"} :outbox #{"dishes"}}
         (store/flush-confirmed
          {:completions {"gmail" "2026-06-18" "dishes" "2026-06-17"} :outbox #{"gmail" "dishes"}}
          #{"gmail"}))))
