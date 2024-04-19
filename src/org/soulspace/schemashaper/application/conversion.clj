(ns org.soulspace.schemashaper.application.conversion
  (:require [org.soulspace.schemashaper.domain.model :as model]
            [clojure.edn :as edn]))


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


(defn filter-include
  [include-set coll]
  (if include-set
    (filter #(contains? include-set (:name %)) coll)
    coll))

(defn filter-exclude
  [exclude-set coll]
  (if exclude-set
    (remove #(contains? exclude-set (:name %)) coll)
    coll))

(defn filter-elements
  [filter-file coll]
  (if filter-file
    (let [filter-spec (edn/read-string (slurp filter-file))
          include-set (get filter-spec :include-set)
          exclude-set (get filter-spec :exclude-set)]
      (->> coll
           (filter-include include-set)
           (filter-exclude exclude-set)))
    coll))


(defn convert
  ""
  ([input-format input-file output-format output-file]
   (->> input-file
        (slurp)
        (schema->model input-format)
        (model->schema output-format)
        (spit output-file)))
  ([input-format input-file output-format output-file filter-file]
   (->> input-file
        (slurp)
        (schema->model input-format)
        (filter-elements filter-file)
        (model->schema output-format)
        (spit output-file)))
  )