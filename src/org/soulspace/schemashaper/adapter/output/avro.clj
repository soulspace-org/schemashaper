(ns org.soulspace.schemashaper.adapter.output.avro
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            ;[org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]
            [org.soulspace.schemashaper.domain.model :as model]))

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

(defn avro-type
  "Returns the avro type of the model type of element `e`."
  [e-type]
  (let [t (get types->avro e-type e-type)]
    ;(println "AVRO Type" t)
    (if (vector? t)
      {:type (first t)
       :logical-type (second t)}
      t)))

(defn unqualified-name
  "Returns the unqualified name."
  [name]
  (last (str/split name #"\.")))

;;
;; Model to AVRO conversions
;;
(defn model-field->avro-field
  "Returns an avro field for the model field."
  [schema-ns config e]
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

(defn model-class->avro-record
  "Returns an avro record for the model class."
  [schema-ns config e]
  {:type "record"
   :name (:name e)
   :fields (into []
                 (map (partial model-field->avro-field schema-ns config)
                      (:ct e)))})

(defn model-namespace->avro-namespace
  "Returns an avro namespace for the model namespace."
  [config e]
;  (println "Namespace:" e)
  (->> e
       (:ct)
       (map (partial model-class->avro-record (:name e) config))
       (into [])))

; TODO use model->avro instead of old methods
(defmulti model->avro
  "Renders the AVRO representation of the model element `e`."
  model/element-type)

(defmethod model->avro :class
  [schema-ns config e]
  {:type "record"
   :name (:name e)
   :doc (if-let [doc (:desc e)] doc "")
   :fields (into []
                 (map (partial model->avro schema-ns config)
                      (:ct e)))})

(defmethod model->avro :enum
  [e]
  {:type "enum"
   :name (:name e)
   :doc (if-let [doc (:desc e)] doc "")
   :symbols (into [] (map model->avro (:ct e)))})

(defmethod model->avro :enum-value
  [e]
  (:name e))

(defmethod model->avro :field
  [e]
  (cond
    (:collection e)
    {:name (:name e)
     :doc (if-let [doc (:desc e)] doc "")
     :type {:type "array"
            :items (avro-type (:type e))
            :default []}}

    (:map e)
    {:name (:name e)
     :doc (if-let [doc (:desc e)] doc "")
     :type {:type "map"
            :values (avro-type (:type e))
            :default {}}}

    (:optional e)
    {:name (:name e)
     :doc (if-let [doc (:desc e)] doc "")
     :type ["null" (avro-type (:type e))]}

    :else
    {:name (:name e)
    :doc (if-let [doc (:desc e)] doc "")
    :type (avro-type (:type e))}))

;;
;; Conversion to AVRO schema
;;
(defmethod conv/model->schema :avro
  ([format coll]
   (conv/model->schema format {} coll))
  ([format config coll]
   (->> coll
        (mapcat (partial model-namespace->avro-namespace config)) 
        (json/write-str))))

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
 
  (conv/model->schema :avro input)

  (json/read-str (slurp "examples/sap-sample-avro.json") :key-fn keyword)
  (vector? (:uuid types->avro))
  (vector? (:date types->avro))

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
  ;
  )
