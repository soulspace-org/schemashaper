(ns org.soulspace.schemashaper.adapter.avro
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]))

(def avro->types
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
   "record"  :class})

(def types->avro
  {:nil              "null"
   :bytes            "int"
   :short            "short"
   :int              "int"
   :long             "long"
   :float            "float"
   :double           "double"
   :decimal          "string"
   :boolean          "boolean"
   :string           "string"
   :array            "array"
   :enum             "enum"
   :map              "map"
   :binary           "bytes"
   :class            "record"
   ;:date             ""
   ;:time             ""
   ;:duration         ""
   ;:date-time-offset ""
   })

;;
;; AVRO to model conversions
;;
(defn avro-field->field
  ""
  [])

(defn avro-record->class
  ""
  [])

;;
;; Model to AVRO conversions
;;
(defn field->avro-field
  "Returns an avro field for the model field."
  [e]
  (cond
    (:collection e)
    {:name (:name e)
     :type {:type "array"
            :values (get types->avro (:type e) (:type e))
            :default []}}
    
    (:optional e)
    {:name (:name e)
     :type ["null" (get types->avro (:type e) (:type e))]}
    
    :else
    {:name (:name e)
     :type (get types->avro (:type e) (:type e))}))

(defn class->avro-record
  "Returns an avro record for the model class."
  [e]
  {:type "record"
   :name (:name e)
   :fields (into [] (map field->avro-field (:ct e)))})

;;
;; Conversion functions for AVRO
;;
(defmethod conv/schema->model :avro
  ([format input]
   (conv/schema->model format {} input))
  ([format filter input]
   (->> input
        (json/read-str))
   ; TODO
   ))

(defmethod conv/model->schema :avro
  ([format coll]
   (conv/model->schema format {} coll))
  ([format filter coll]
   (->> coll
        (map class->avro-record)
        (into [])
        (json/json-str))))

(comment
  (json/read-json "test-avro.json")
  (json/json-str (conv/model->schema :avro [{:el :class :name "TestClass" :ct [{:el :field :name "id" :type :int :optional true}]}]))
  ;(json/read)
  ;
  )