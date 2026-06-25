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

(deftest clamp-advances-in-place-when-a-row-leaves
  (testing "a toggled middle row leaves: the index holds, the row below shifts up"
    (is (= 1 (cursor/clamp 1 3))))
  (testing "the toggled row was last (or a late load shrank the list): pin to the new last"
    (is (= 2 (cursor/clamp 3 3)))
    (is (= 2 (cursor/clamp 5 3)))))

(deftest clamp-is-nil-when-empty-or-dormant
  (is (nil? (cursor/clamp 0 0)))
  (is (nil? (cursor/clamp nil 5))))

(deftest wake-restores-the-remembered-row
  (is (= 2 (cursor/wake 2 5))))

(deftest wake-lands-on-the-first-row-when-fresh-or-gone
  (testing "never visited (dormant), or the old spot no longer exists"
    (is (= 0 (cursor/wake nil 5)))
    (is (= 0 (cursor/wake 7 5)))))

(deftest wake-is-nil-when-the-pane-has-no-rows
  (is (nil? (cursor/wake 0 0)))
  (is (nil? (cursor/wake nil 0))))
