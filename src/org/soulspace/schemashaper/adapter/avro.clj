(ns org.soulspace.schemashaper.adapter.avro
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.data.json :as json]
            [org.soulspace.schemashaper.domain.model :as model]
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

(def types->avro
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

(defn optional?
  "Returns true, if the element is optional."
  [e-type]
  (let [t e-type
        t-set (set t)]
    (and (vector? t) (contains? t-set "null"))))

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

(defn avro-type
  "Returns the avro type of the model type of element `e`."
  [e-type]
  (let [t (get types->avro e-type e-type)]
    ;(println "AVRO Type" t)
    (if (vector? t)
      {:type (first t)
       :logical-type (second t)}
      t)))

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
  [schema-ns criteria e]
  (let [e-type (:type e)]
    {:el :field
     :name (:name e)
     :optional (optional? e-type)
     :type (base-type e-type)}))

(defn avro-record->model-class
  "Returns a model class for the avro record."
  [schema-ns criteria e]
  ;(println "Element" e)
  (when (= "record" (:type e))
    (let [e-name (:name e)
          e-ns (get e :namespace schema-ns)]
      {:el :class
       :id (class-id e-ns e-name)
       :avro/type "record"
       :name e-name
       :ct (into []
                 (map (partial avro-field->model-field e-ns criteria)
                      (:fields e)))})))

(defn avro->enum->model-enum
  "Returns a model enum for the avro enum."
  [schema-ns criteria e]
  (let [e-name (:name e)
        e-ns (get e :namespace schema-ns)]
    {:el :enum
     :id (class-id e-ns e-name)
     :avro/type "enum"
     :name e-name
     ; TODO define values in model
     }))

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
            :items (avro-type (:type e))
            :default []}}

    (:map e)
    {:name (:name e)
     :type {:type "map"
            :values (avro-type (:type e))
            :default {}}}

    (:optional e)
    {:name (:name e)
     :type ["null" (avro-type (:type e))]}

    :else
    {:name (:name e)
     :type (avro-type (:type e))}))

(defn model-enum->avro-enum
  "Returns an avro enum for the model enum."
  [schema-ns criteria e]
  {:type "enum"
   :name (:name e)
   ; TODO define values in model
   :symbols (into [] (keys (:values e)))}
  )

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
;  (println "Namespace:" e)
  (->> e
       (:ct)
       (map (partial model-class->avro-record (:name e) criteria))
       (into [])))

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
  ([format criteria input]
   (->> input
        (json-read-str)
        (map (partial avro-record->model-class "schema" criteria))
        )
   ; TODO
   ))

(defmethod conv/model->schema :avro
  ([format coll]
   (conv/model->schema format {} coll))
  ([format criteria coll]
   (->> coll
        (mapcat (partial model-namespace->avro-namespace criteria)) 
        (json/write-str))))

(comment
  (json/read-str (slurp "examples/sap-sample-avro.json") :key-fn keyword)
  (model-type {:type {:type "long" :logical-type "local-timestamp-millis"}})
  (base-type {:type ["null" {:type "long" :logical-type "local-timestamp-millis"}]})
  (vector? (:uuid types->avro))
  (vector? (:date types->avro))
  (optional? {:name "Notes", :type ["null" "string"]})
  (optional? {:name "SessionID", :type "long"})

;  (println (conv/schema->model :avro {} (slurp "C:/PAG/datona/cluu/CcbChangeRequest.avsc")))

  (println (conv/model->schema :avro
                               {}
                               #{{:el :namespace
                                  :name "TestNamespace"
                                  :ct [{:el :class
                                        :name "TestClass"
                                        :ct [{:el :field
                                              :name "id"
                                              :type :uuid
                                              :optional true}]}]}}))
  ;(json/read)
  ;
  )
