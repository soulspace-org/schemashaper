(ns org.soulspace.schemashaper.application.conversion
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [org.soulspace.schemashaper.domain.model :as model]
            ))


(def supported-schemas
  #{:avro :edmx :overarch :proto})


(defn adapter
  "Returns the adapter."
  ([adapter & rest]
   adapter))

;;;
;;; Conversion multimethods to implement by each schema adapter
;;;
(defmulti schema->model
  "Converts a schema to the model.
   
   Arities:
   [format input]
   [format criteria input]
   
   * `format` is used to dispatch to adapter (e.g. :avro)
   * `input` the read input to convert
   * `criteria` filter criteria applied in conversion"
  adapter)

(defmulti model->schema
  "Converts the model to a schema.

   Arities:
   [format coll]
   [format criteria coll]

   * `format` is used to dispatch to adapter (e.g. :avro)
   * `coll` the collection of model elements to convert
   * `criteria` filter criteria applied in conversion"
  adapter)

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

(defn write-file
  "Writes the content to the file with name `filename`. Creates missing parent directories first."
  [filename content]
  (io/make-parents filename)
  (spit filename content))

(defn convert
  "Converts the input to the output."
  ([input-format input-file output-format output-file]
   (->> input-file
        (slurp)
        (schema->model input-format)
        (model->schema output-format)
        (write-file output-file)))
  ([input-format input-file output-format output-file config-file]
   (let [config (edn/read-string (slurp config-file))]
     (->> input-file
          (slurp)
          (schema->model input-format config)
;          (filter-elements filter-file)
          (model->schema output-format config)
          (write-file output-file)))))