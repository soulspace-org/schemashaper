(ns org.soulspace.schemashaper.adapter.overarch
  (:require [clojure.edn :as edn]
            [org.soulspace.schemashaper.application.conversion :as conv]))


;;
;; Conversion functions for Overarch
;;
(defmethod conv/schema->model :overarch
  ([format input]
   (conv/schema->model format {} input))
  ([format criteria input]
   (->> input
        (edn/read-string)
        (filter #(= :class (:el %))))))

(defmethod conv/model->schema :overarch
  ([format coll]
   (conv/model->schema format {} coll))
  ([format criteria coll]
   (->> coll
        (into #{}))))
