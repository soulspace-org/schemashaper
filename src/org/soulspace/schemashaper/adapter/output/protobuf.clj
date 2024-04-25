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


(defn model-field->proto-field
  "Returns the proto schema for a model field."
  [idx e]
  (str "  "
       (when (:optional e) "optional ")
       (:name e)
       " = " idx
       )
  ; TODO
  )

(defn model-class->proto-message
  "Returns the proto schema for a model class."
  [e]
  [(str "message " (:name e) " {")
   (map-indexed model-field->proto-field (filter #(= :field (:el %)) (:ct e)))
   (str "}")]
  )

(defmulti model->proto
  "Renders the ProtoBuffer representation of the model element `e`."
  model/element-type)


;;
;; Conversion functions for ProtoBuf
;;
(defmethod conv/schema->model :proto
  ([format input]
   (conv/schema->model format {} input))
  ([format config input]))

(defmethod conv/model->schema :proto
  ([format coll]
   (conv/model->schema format {} coll))
  ([format config coll]
   (into [] (map model-class->proto-message coll))))

(comment
  (map-indexed #(println %1 %2) ["a" "b" "c"])
  ;
  )
