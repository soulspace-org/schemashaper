(ns org.soulspace.schemashaper.adapter.output.openapi
  )

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
