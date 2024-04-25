(ns org.soulspace.schemashaper.adapter.source.protobuf
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]))

(def proto->types
  {"null"    "nil"
   "int"     "int"
   "long"    "long"
   "float"   "float"
   "double"  "double"
   "boolean" "boolean"
   "string"  "string"
   "array"   "array"
   "enum"    "enum"
   "map"     "map"
   "bytes"   "binary"
   "fixed"   "string"
   "message" "class"})

(defn proto-field->model-field
  ""
  [])

(defn proto-message->model-class
  ""
  [])

;;
;; Conversion functions for ProtoBuf
;;
(defmethod conv/schema->model :proto
  ([format input]
   (conv/schema->model format {} input))
  ([format config input]))

(comment
  (map-indexed #(println %1 %2) ["a" "b" "c"])
  ;
  )