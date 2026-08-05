(ns app.layout-test
  (:require [cljs.test :refer [deftest is]]
            [app.layout :as layout]))

(deftest with-defaults-falls-back-to-defaults-when-nothing-stored
  (is (= {:day false :rare true :cleared false} (layout/with-defaults nil)))
  (is (= {:day false :rare true :cleared false} (layout/with-defaults {}))))

(deftest with-defaults-overrides-per-key
  (is (= {:day true :rare true :cleared false} (layout/with-defaults {:day true})))
  (is (= {:day false :rare false :cleared false} (layout/with-defaults {:rare false})))
  (is (= {:day false :rare true :cleared true} (layout/with-defaults {:cleared true}))))

(deftest with-defaults-keeps-a-fully-specified-stored-map
  (is (= {:day true :rare false :cleared true}
         (layout/with-defaults {:day true :rare false :cleared true}))))
