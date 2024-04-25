(ns org.soulspace.schemashaper.adapter.output.edmx
  (:require [clojure.string :as str]
            [clojure.data.xml :as xml]
            [org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]))


(def types->edmx
  {"byte"     "Edm.Byte"
   "short"    "Edm.Int16"
   "int"      "Edm.Int32"
   "long"     "Edm.Int64"
   "float"    "Edm.Single"
   "double"   "Edm.Double"
   "decimal"  "Edm.Decimal"
   "boolean"  "Edm.Boolean"
   "string"   "Edm.String"
   "uuid"     "Edm.Guid"
   "binary"   "Edm.Binary"
   "enum"     "Edm.String"
   "date"     "Edm.Date"                   ; TODO
   "time"     "Edm.TimeOfDay"              ; TODO
   "duration" "Edm.Duration"               ; TODO
   "date-time-offset" "Edm.DateTimeOffset" ; TODO
   })

(defn model-type->edmx-type
  "Returns the qualified edmx type for the model type `t`.
   
   If t is a model base type, the corresponding edmx base type is returned.
   Otherwise t is returned as is."
  [t]
  (get types->edmx t t))

(defmulti model->edmx
  "Renders the EDMX representation of the model element `e`."
  model/element-type)

(defmethod model->edmx :field
  [e])

(defmethod model->edmx :class
  [e])

(defmethod model->edmx :enum-value
  [e])

(defmethod model->edmx :enum
  [e])

;;
;; Conversion functions for EDMX
;;
(defmethod conv/model->schema :edmx 
  ([format coll]
   (conv/model->schema format {} coll))
  ([format config coll]
   ))

(comment
  ;
  )
