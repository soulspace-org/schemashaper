(ns org.soulspace.schemashaper.adapter.output.graphql
  (:require [clojure.string :as str]
            [org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]))

; TODO check and fix
(def type->graphql
  {"nil"              "null"
   "bytes"            "Int"
   "short"            "Int"
   "int"              "Int"
   "long"             "Int"
   "float"            "Float"
   "double"           "Float"
   "decimal"          "String"
   "boolean"          "Boolean"
   "string"           "String"
   ;"array"            "array"
   "enum"             "enum"
   ;"map"              "map"
   ;"binary"           "bytes"
   ;"class"            "record"
   "uuid"             "ID"
   "date"             ["int" "date"]
   ;"time"             ["int" "time-millis"]
   ;"date-time-offset" ["long" "local-timestamp-millis"]
   ;"duration"         ""
   })

(defn render-indent
  "Renders an indent of `n` space chars."
  [n]
  (str/join (repeat n " ")))

(defn base-type
  "Returns the base type of the element `e`."
  [e]
  (let [b-type (get type->graphql (:type e) (:type e))]
    b-type))

(defn optional?
  "Returs true, if the element `e` is optional."
  [e]
  (boolean (:optional e)))

(defn model-type->graphql-type
  "Returns the GrapQL for the model type."
  [e]
  (let [b-type (base-type e)]
    (if (:collection e)
      (str "[" b-type "!]"
           (when-not (optional? e) "!"))
      (str b-type
           (when-not (optional? e) "!")))))

(defmulti model->graphql
  "Renders the GraphQL representation of the model element `e`."
  model/element-type)

(defn model-fields->graphql-fields
  "Renders the GraphQL fields for the model fields in `coll`."
  ([indent coll]
   (->> coll
        (filter #(contains? #{:field} (:el %)))
        (map (partial model->graphql (+ 2 indent)))
        (str/join "\n"))))

(defn model-enum-values->graphql-enum-values
  "Renders the GraphQL enum values for the model enum values in `coll`."
  ([indent coll]
   (->> coll
        (filter #(contains? #{:enum-value} (:el %)))
        (map (partial model->graphql (+ 2 indent)))
        (str/join "\n"))))

(defmethod model->graphql :field
  ([indent e]
   (str (render-indent indent) (:name e) ": " (model-type->graphql-type e))))

(defmethod model->graphql :class
  ([indent e]
   (str (render-indent indent)
        "type " (:name e) " {\n"
        (model-fields->graphql-fields indent (:ct e))
        "\n" (render-indent indent)
        "}\n")))

(defmethod model->graphql :enum-value
  ([indent e]
   (str (render-indent indent) (:name e))))

(defmethod model->graphql :enum
  ([indent e]
   (str (render-indent indent)
        "enum " (:name e) " {\n"
        (model-enum-values->graphql-enum-values indent (:ct e))
        "\n" (render-indent indent)
        "}\n")))


(defmethod model->graphql :namespace
  ([indent e]
   (->> (:ct e)
        (map (partial model->graphql indent))
        (str/join "\n"))))

;;
;; Conversion to GraphQL schema
;;
(defmethod conv/model->schema :graphql
  ([format coll]
   (conv/model->schema format {} coll))
  ([format config coll]
   (println "model->schema")
   (let [query (->> coll
                    (map (partial model->graphql 0))
                    (str/join "\n")
                    )]
     (str query))))

(comment "Conversion tests"
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
  (println
   (conv/model->schema :graphql input))

  ;
  )