(ns org.soulspace.schemashaper.adapter.overarch
  (:require [clojure.edn :as edn]
            [org.soulspace.schemashaper.application.conversion :as conv]))


(defmethod conv/model->schema :overarch
  [format coll]
  (->> coll
       (into #{})))

(defmethod conv/schema->model :overarch
  [format file]
  (->> file
       (slurp)
       (edn/read-string)
       (filter #(= :class (:el %)))))

