(ns app.rare.view-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.rare.view :as view]))

(deftest toggle-open-opens-closes-and-switches
  (testing "opening a closed note, closing the open one, switching from another"
    (is (= "books" (view/toggle-open nil "books")))
    (is (= "books" (view/toggle-open "mail" "books")))
    (is (nil? (view/toggle-open "books" "books")))))

(deftest i-toggles-the-cursored-rows-note
  (testing "a note-bearing cursored row opens, then closes on a second press"
    (is (= "books" (view/note-key-target {:id "books" :note "read it"} nil)))
    (is (nil? (view/note-key-target {:id "books" :note "read it"} "books")))))

(deftest i-is-a-no-op-without-a-note-under-the-cursor
  (testing "dormant cursor or a note-less row leaves the open note untouched"
    (is (nil? (view/note-key-target nil nil)))
    (is (= "mail" (view/note-key-target nil "mail")))
    (is (= "mail" (view/note-key-target {:id "books"} "mail")))))
