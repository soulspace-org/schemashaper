(ns org.soulspace.schemashaper.adapter.input.openapi
  (:require [clj-yaml.core :as yaml]))

(def openapi->types
  {"integer" "int"
   ["integer" "int32"] "int"
   ["integer" "int64"] "long"
   "string" "string"
   ;"array" ""
   ;"object" ""
   ;
   })

(defn openapi-type
  "Dispatch function."
  [e]
  )

(defmulti openapi->overarch
  "Converts an OpenAPI element into an overarch class model element."
  openapi-type)

(defmethod openapi->overarch :openapi
  [[k v]]
  (println "openapi"))

(defmethod openapi->overarch :info
  [[k v]]
  (println "info"))

(defmethod openapi->overarch :servers
  [[k v]]
  (println "servers"))

(defmethod openapi->overarch :paths
  [[k v]]
  (println "paths"))

(defmethod openapi->overarch :components
  [[k v]]
  (println "components")
  )

(defmethod openapi->overarch :schemas
  [[k v]]
  (println "schemas"))


(defmethod conv/schema->model :openapi
  ([format input]
   (conv/schema->model format {} input))
  ([format config input]
   (let [openapi (yaml/parse-string input)
         _ (println "OpenAPI" openapi)
         model []]
     model)))

(comment
  ()
  ;
  )