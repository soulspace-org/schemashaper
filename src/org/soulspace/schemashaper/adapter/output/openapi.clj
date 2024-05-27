(ns org.soulspace.schemashaper.adapter.output.openapi
  (:require [org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]))

; TODO check and fix
(def types->openapi
  {"nil"              "null"
   "bytes"            "int"
   "short"            "short"
   "int"              "int"
   "long"             "long"
   "float"            "float"
   "double"           "double"
   "decimal"          "string"
   "boolean"          "boolean"
   "string"           "string"
   "array"            "array"
   "enum"             "enum"
   "map"              "map"
   "binary"           "bytes"
   "class"            "record"
   "uuid"             ["string" "uuid"]
   "date"             ["int" "date"]
   "time"             ["int" "time-millis"]
   "date-time-offset" ["long" "local-timestamp-millis"]
   ;"duration"         ""
   })

(defmulti model->openapi
  "Renders the OpenAPI representation of the model element `e`."
  model/element-type)

(defmethod model->openapi :field
  [e])

(defmethod model->openapi :class
  [e])

(defmethod model->openapi :enum
  [e])

(defmethod model->openapi :enum-value
  [e])

;;
;; Conversion to OpenAPI schema
;;
(defmethod conv/model->schema :openapi
  ([format coll]
   (conv/model->schema format {} coll))
  ([format config coll]
   (->> coll
        ;TODO implement
        )))

(comment
  (def input #{{:el :class
                :id :test/card
                :name "Card"
                :desc "Card in a card game."
                :ct [{:el :field
                      :name "id"
                      :type ":uuid"}
                     {:el :field
                      :name "colour"
                      :type "CardColour"}]}
               {:el :enum
                :id :test/card-colour
                :name "CardColour"
                :desc "Colour of the card."
                :ct [{:el :enum-value
                      :name "CLUBS"}
                     {:el :enum-value
                      :name "SPADES"}
                     {:el :enum-value
                      :name "HEART"}
                     {:el :enum-value
                      :name "DIAMONDS"}]}})
  (conv/model->schema :openapi input)
  ;
  )
