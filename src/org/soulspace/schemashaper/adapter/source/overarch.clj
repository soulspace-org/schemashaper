(ns org.soulspace.schemashaper.adapter.source.overarch
  (:require [clojure.edn :as edn]
            [org.soulspace.schemashaper.application.conversion :as conv]))


;;
;; Conversion functions for Overarch
;;
(defmethod conv/schema->model :overarch
  ([format input]
   (conv/schema->model format {} input))
  ([format config input]
   (->> input
        (edn/read-string)
        (filter #(= :class (:el %))))))
