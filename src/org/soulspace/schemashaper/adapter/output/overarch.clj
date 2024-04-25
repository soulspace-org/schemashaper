(ns org.soulspace.schemashaper.adapter.output.overarch
  (:require [clojure.edn :as edn]
            [org.soulspace.schemashaper.application.conversion :as conv]))


;;
;; Conversion functions for Overarch
;;

(defmethod conv/model->schema :overarch
  ([format coll]
   (conv/model->schema format {} coll))
  ([format config coll]
   (->> coll
        (into #{}))))
