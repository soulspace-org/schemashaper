(ns org.soulspace.schemashaper.adapter.input.openapi
  (:require [clj-yaml.core :as yaml]
            [org.soulspace.schemashaper.application.conversion :as conv]))

(def openapi->types
  ""
  {"integer"             "int"
   "string"              "string"
   ["integer" "int32"]   "int"
   ["integer" "int64"]   "long"
   ["number" "float"]    "float"
   ["number" "double"]   "double"
;   ["string" "password"] 
   ;"array" ""
   ;"object" ""
   ;
   })

(defn openapi-type
  "Dispatch function."
  [[k v]]
  (keyword k))

(defn openapi-property->overarch
  ""
  [[k v]]
  (let [p-name k
        p-type ""
        ;
        ]
    {:el :field
     :name p-name
     :type p-type
     }))

(defn openapi-schema->overarch
  ""
  [[k v]]
  (let [e-name k
        e-id k ; TODO generate id
        e-desc (get (:description v) "")
        ;
        ]
    {:el :class
     :id e-id ;
     :name e-name
     :desc e-desc}))

(defmulti openapi->overarch
  "Converts an OpenAPI element into an overarch class model element."
  openapi-type)

(defmethod openapi->overarch :openapi
  [[k v]]
  (println "openapi" k))

(defmethod openapi->overarch :info
  [[k v]]
  (println "info" k))

(defmethod openapi->overarch :servers
  [[k v]]
  (println "servers" k))

(defmethod openapi->overarch :paths
  [[k v]]
  (println "paths" k))

(defmethod openapi->overarch :components
  [[k v]]
  (println "components" k)
  (map openapi->overarch (:schemas v)))

(defmethod openapi->overarch :schemas
  [[k v]]
  (println "schemas" k))

(defmethod conv/schema->model :openapi
  ([format input]
   (conv/schema->model format {} input))
  ([format config input]
   (let [openapi (yaml/parse-string input)
         _ (println "OpenAPI" openapi)
         paths (:paths openapi)
         _ (println "Components" (:components openapi))
         components (:components openapi)
         _ (println "Schema" (:schemas components))
         schemas (:schemas components)
         ;model (map openapi->overarch (:components openapi))
         _ (doseq [[k v] schemas] (println "Schema" k v))
         model []
         ;
         ]
     model)))

(comment
  (conv/schema->model :openapi (slurp "examples/petstore-openapi-30.yaml"))
  ;
  )