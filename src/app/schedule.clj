(ns app.schedule
  "Compile-time loader for the committed routine schedule."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defmacro load-schedule
  []
  (-> (io/resource "app/schedule.edn") slurp edn/read-string))
