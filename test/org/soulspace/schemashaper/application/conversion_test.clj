(ns org.soulspace.schemashaper.application.conversion-test
  (:require [clojure.test :refer :all]
            [org.soulspace.schemashaper.adapter.avro :refer :all]))

(deftest compile-test
  (testing "Compile test"
    (is (= 1 1))))
