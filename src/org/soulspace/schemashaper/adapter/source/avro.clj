(ns org.soulspace.schemashaper.adapter.source.avro
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.data.json :as json]
            ;[org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]))

(def avro->types
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
   "record"  "class"
   {:type "string" :logical-type "uuid"} :uuid
   {:type "int"  :logical-type "date"} :date
   {:type "int"  :logical-type "time-millis"} :time
   {:type "long" :logical-type "local-timestamp-millis"} :date-time-offset
   ;
   })

(defn optional?
  "Returns true, if the element is optional."
  [e-type]
  (let [t e-type
        t-set (set t)]
    (and (vector? t) (contains? t-set "null"))))


; TODO handle type maps consistently (arrays, maps, records, ...)
(defn model-type
  "Returns the model type for the avro type of element `e`."
  [e-type]
  (let [t (get avro->types e-type e-type)]
    ;(println "Model Type" t)
    t)
  )

(defn base-type
  "Returns the base type of the element."
  [e-type]
  (let [t-set (set e-type)
        base-set (set/difference t-set #{"null"})]
    ;(println "T-Set" t-set)
    ;(println "Base-Set" base-set)
    (if (= 1 (count base-set))
      (model-type (first base-set))
      base-set)))

(defn class-id
  "Returns an id for the class."
  [schema-ns name]
  (keyword (str/join "/" [schema-ns name])))

(defn namespace-id
  "Returns an id for the class."
  [schema-ns]
  (let [parts (str/split schema-ns #"\.")
        ns-parts (drop-last parts)
        name-part (last parts)]
   (keyword (str/join "/" [(str/join "." ns-parts) name-part]))))

(defn unqualified-name
  "Returns the unqualified name."
  [name]
  (last (str/split name #"\.")))

;;
;; AVRO to model conversions
;;
(defn avro-field->model-field
  "Returns a model field for the avro field."
  [schema-ns config e]
  (let [e-type (:type e)]
    {:el :field
     :name (:name e)
     :optional (optional? e-type)
     :type (base-type e-type)}))

(defn avro-record->model-class
  "Returns a model class for the avro record."
  [schema-ns config e]
  ;(println "Element" e)
  (when (= "record" (:type e))
    (let [e-name (:name e)
          e-ns (get e :namespace schema-ns)]
      {:el :class
       :id (class-id e-ns e-name)
       :avro/type "record"
       :name e-name
       :ct (into []
                 (map (partial avro-field->model-field e-ns config)
                      (:fields e)))})))

(defn avro->enum->model-enum
  "Returns a model enum for the avro enum."
  [schema-ns config e]
  (let [e-name (:name e)
        e-ns (get e :namespace schema-ns)]
    {:el :enum
     :id (class-id e-ns e-name)
     :avro/type "enum"
     :name e-name
     ; TODO define values in model
     }))

;;
;; Conversion functions for AVRO
;;
(defn json-read-str
  ""
  [input]
  (json/read-str input :key-fn keyword))

(defmethod conv/schema->model :avro
  ([format input]
   (conv/schema->model format {} input))
  ([format config input]
   (->> input
        (json-read-str)
        (map (partial avro-record->model-class "schema" config))
        )
   ; TODO
   ))

(comment
  (json/read-str (slurp "examples/sap-sample-avro.json") :key-fn keyword)
  (model-type {:type {:type "long" :logical-type "local-timestamp-millis"}})
  (base-type {:type ["null" {:type "long" :logical-type "local-timestamp-millis"}]})
  (optional? {:name "Notes", :type ["null" "string"]})
  (optional? {:name "SessionID", :type "long"})

  ;(json/read)
  ;
  )
