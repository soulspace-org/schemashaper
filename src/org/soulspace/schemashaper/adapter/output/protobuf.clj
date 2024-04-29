(ns org.soulspace.schemashaper.adapter.output.protobuf
  (:require [clojure.string :as str]
            [org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]))

(def types->proto
  {"nil"     "null"
   "bytes"   "int"
   "short"   "short"
   "int"     "int"
   "long"    "long"
   "float"   "float"
   "double"  "double"
   "decimal" "string"
   "boolean" "boolean"
   "string"  "string"
   "array"   "array"
   "enum"    "enum"
   "map"     "map"
   "binary"  "bytes"
   "class"   "message"})


(defmulti model->proto
  "Renders the ProtoBuffer representation of the model element `e`."
  model/element-type)

(defmethod model->proto :field
  [idx e]
  (str "  "
       (when (:optional e) "optional ")
       (:name e)
       " = " idx "\n"))

(defmethod model->proto :class
  [e]
  [(str "message " (:name e) " {")
   (map-indexed model->proto (:ct e))
   (str "}")])

(defmethod model->proto :enum-value
  [e])

(defmethod model->proto :enum
  [e])

(defmethod model->proto :namespace
  [e]
  (map model->proto (:ct e)))


;;
;; Conversion functions for ProtoBuf
;;
(defmethod conv/model->schema :proto
  ([format coll]
   (conv/model->schema format {} coll))
  ([format config coll]
   (into [] (mapcat model->proto coll))))

(comment
  (map-indexed #(println %1 %2) ["a" "b" "c"])
  ;
  )
