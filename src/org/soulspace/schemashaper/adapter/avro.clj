(ns org.soulspace.schemashaper.adapter.avro
  (:require [clojure.string :as str]
            [clojure.set :as set]
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

(defn optional?
  "Returns true, if the element is optional."
  [e]
  (let [t (:type e)
        t-set (set t)]
    (and (vector? t) (contains? t-set "null"))))

(defn base-type
  "Returns the base type of the element."
  [e]
  (let [t (:type e)
        t-set (set t)
        base-set (set/difference t-set #{"null"})]
    (if (= 1 (count base-set))
      (first base-set)
      base-set)))

(defn class-id
  "Returns an id for the class."
  [schema-ns name]
  (keyword (str/join "/" [schema-ns name])))

(defn namespace-id
  "Returns an id for the class."
  [name]
  (let [parts (str/split name #"\.")
        ns-parts (drop-last parts)
        name-part (last parts)]
   (keyword (str/join "/" [(str/join "." ns-parts) name-part]))))

;;
;; AVRO to model conversions
;;
(defn avro-field->model-field
  "Returns a model field for the avro field."
  [schema-ns e]
  {:el :field
   :name (:name e)
   :optional (optional? e)
   :type (base-type e)})

(defn avro-record->model-class
  "Returns a model class for the avro record."
  [schema-ns criteria e]
  {:el :class
   :id (class-id schema-ns (:name e))
   :name (:name e)
   :ct (into []
             (map (partial avro-field->model-field schema-ns criteria)
                  (:fields e)))})

(defn avro-namespace->model-namespace
  "Returns a model namespace for the avro namespace."
  [criteria e]
  {:el :namespace
   :id (namespace-id (:name e))})

;;
;; Model to AVRO conversions
;;
(defn model-field->avro-field
  "Returns an avro field for the model field."
  [schema-ns criteria e]
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

(defn model-class->avro-record
  "Returns an avro record for the model class."
  [schema-ns criteria e]
  {:type "record"
   :name (:name e)
   :fields (into []
                 (map (partial model-field->avro-field schema-ns criteria)
                      (:ct e)))})

(defn model-namespace->avro-namespace
  "Returns an avro namespace for the model namespace."
  [criteria e]
  {:namespace (:name e)})

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
  ([format criteria coll]
   (->> coll
        (map (partial model-class->avro-record "default" criteria))
        (into [])
        (json/write-str))))

(comment
  (json/read-str (slurp "examples/sap-sample-avro.json") :key-fn keyword)
  (println (json/write-str (conv/model->schema :avro [{:el :class :name "TestClass" :ct [{:el :field :name "id" :type :int :optional true}]}])))
  (optional? {:name "Notes", :type ["null" "string"]})
  (optional? {:name "SessionID", :type "long"})
  ;(json/read)
  ;
  )