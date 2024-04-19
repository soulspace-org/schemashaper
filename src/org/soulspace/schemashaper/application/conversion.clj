(ns org.soulspace.schemashaper.application.conversion
  (:require [org.soulspace.schemashaper.domain.model :as model]))


(def supported-schemas
  #{:avro :edmx :overarch :proto})

(defn schema-type
  "Returns the schema type."
  ([type & rest]
   type))

(defmulti schema->model
  "Converts a schema to the model."
  schema-type)

(defmulti model->schema
  "Converts the model to a schema."
  schema-type)
