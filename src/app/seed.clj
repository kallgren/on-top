(ns app.seed
  "Compile-time loader for the committed seed schedule."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defmacro load-seed
  []
  (-> (io/resource "app/seed.edn") slurp edn/read-string))
