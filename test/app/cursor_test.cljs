(ns app.cursor-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.cursor :as cursor]))

(deftest first-nav-key-wakes-onto-the-first-row
  (testing "a dormant cursor wakes onto row 0 regardless of direction"
    (is (= 0 (cursor/next-cursor nil 1 5)))    ; j wakes
    (is (= 0 (cursor/next-cursor nil -1 5))))) ; k wakes too

(deftest j-moves-down-k-moves-up
  (is (= 3 (cursor/next-cursor 2 1 5)))
  (is (= 1 (cursor/next-cursor 2 -1 5))))

(deftest clamps-at-both-ends-without-wrapping
  (testing "j at the bottom and k at the top do nothing"
    (is (= 4 (cursor/next-cursor 4 1 5)))
    (is (= 0 (cursor/next-cursor 0 -1 5)))))

(deftest stays-dormant-with-no-rows
  (is (nil? (cursor/next-cursor nil 1 0)))
  (is (nil? (cursor/next-cursor nil -1 0))))
