(ns org.soulspace.schemashaper.adapter.protobuf
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]))

(def proto->types
  {"null"    :nil
   "int"     :int
   "long"    :long
   "float"   :float
   "double"  :double
   "boolean" :boolean
   "string"  :string
   "array"   :array
   "enum"    :enum
   "map"     :map
   "bytes"   :binary
   "fixed"   :string
   "message" :class})

(def types->proto
  {:nil     "null"
   :bytes   "int"
   :short   "short"
   :int     "int"
   :long    "long"
   :float   "float"
   :double  "double"
   :decimal "string"
   :boolean "boolean"
   :string  "string"
   :array   "array"
   :enum    "enum"
   :map     "map"
   :binary  "bytes"
   :class   "message"})

(defn field->proto
  "Returns the proto schema for a model field."
  [idx e]
  (str "  "
       (when (:optional e) "optional ")
       )
  ; TODO
  )

(defn class->proto
  "Returns the proto schema for a model class."
  [e]
  )

(defmethod conv/model->schema :proto
  [format coll]
  (into [] (map class->proto coll)))

(defmethod conv/schema->model :proto
  [format file])

(comment
  (map-indexed #(println %1 %2) ["a" "b" "c"])
  ;
  )