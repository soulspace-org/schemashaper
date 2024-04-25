(ns org.soulspace.schemashaper.adapter.output.edmx
  (:require [clojure.string :as str]
            [clojure.data.xml :as xml]
            ;[org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]))

(def types->edmx
  {"byte"     "Edm.Byte"
   "short"    "Edm.Int16"
   "int"      "Edm.Int32"
   "long"     "Edm.Int64"
   "float"    "Edm.Single"
   "double"   "Edm.Double"
   "decimal"  "Edm.Decimal"
   "boolean"  "Edm.Boolean"
   "string"   "Edm.String"
   "uuid"     "Edm.Guid"
   "binary"   "Edm.Binary"
   "enum"     "Edm.String"
   "date"     "Edm.Date"                   ; TODO
   "time"     "Edm.TimeOfDay"              ; TODO
   "duration" "Edm.Duration"               ; TODO
   "date-time-offset" "Edm.DateTimeOffset" ; TODO
   })

(defn model-type->edmx-type
  "Returns the qualified edmx type for the model type `t`.
   
   If t is a model base type, the corresponding edmx base type is returned.
   Otherwise t is returned as is."
  [t]
  (get types->edmx t t))

(defn optional?
  "Returns true if the element `e` is optional (nullable)."
  [{:keys [tag attrs content] :as e}]
  (Boolean/valueOf (get attrs :Nullable "true")))

(defn abstract?
  "Returns true if the element `e` is abstract."
  [{:keys [tag attrs content] :as e}]
  (Boolean/valueOf (get attrs :Abstract "false")))

(defn element-tag?
  "Returns true if the tag of the `element` equals the given `tag`."
  [tag element]
  (= tag (:tag element)))

(defn tag-pred
  "Returns a predicate that takes an element and checks if the element has the given `tag`"
  [tag]
  (fn [e] (= tag (:tag e))))

(defn qualified-name
  "Returns the qualified name of the element given the base name `n` and the schema namespace `schema-ns`."
  [n schema-ns]
  (str schema-ns "." n))

(defn split-index
  ""
  [n]
  (re-matches #"(\w+)(\d+)" n))

(defn collection-type?
  "Returns true if the type `t` is a collection type."
  [t]
  (str/starts-with? t "Collection("))

(defn base-type
  "Returns the type if t is a collection, otherwise returns nil."
  [t]
;  (println "Collection type?" t)
  (when-let [match (re-matches #"Collection\((.*)\)" t)]
    (second match)))

(defn parse-base-type-name
  "Parses the type name `n` and returns a map containing the name and the schema-ns for the type."
  [n]
  (let [parts (str/split n #"\.")]
    (if (= (count parts) 2)
      {:name (last parts)
       :schema-ns (str/join "." (drop-last parts))
       :qualified-name n}
      (println "edmx: missing namespace for type" n))))

(defn parse-type-name
  "Parses the type name `n` and returns a map with the name and the schema-ns,
   if contained."
  ([n]
   (if (collection-type? n)
     (merge {:collection :list
             :cardinality :zero-to-many}
            (parse-base-type-name (base-type n)))
     (parse-base-type-name n))))

(defn parse-association-name
  "Parses the name `n` of an association and returns a map with the names
   and cardinalities of the roles of the association."
  [n schema-ns]
  (let [parts (str/split n #"_")
        [_ to-card idx] (split-index (nth parts 3))]
    {:name n
     :schema-ns schema-ns
     :qualified-name (str n "." schema-ns)
     :from-name (nth parts 0)
     :to-name (nth parts 1)
     :from-card (nth parts 2)
     :to-card to-card
     :index idx}))

(defn parse-relationship-name
  "Parses the name `n` of a relation and returns a map with the namespace and the names
   and cardinalities of the roles of the relation."
  [n]
  (let [{a-name :name schema-ns :schema-ns} (parse-type-name n)]
    (parse-association-name a-name schema-ns)))

(defn edm-type?
  "Returns true if the type is in the Edm namespace."
  [t]
  (= "Edm" (:schema-ns (parse-type-name t))))

(defn include?
  "Returns true if the element `e` is included by the given `criteria`."
  [config e]
  (if-let [include-set (:include-set config)]
    (if (contains? include-set e)
      true
      false)
    ; include, if no include set is specified
    true))

(defn exclude?
  "Returns true if the element `e` is excluded by the given `criteria`."
  [config e]
  (if-let [exclude-set (:exclude-set config)]
    (if (contains? exclude-set e)
      true
      false)
    ; don't exclude, if no exclude set is specified
    false))

;;
;; Conversion functions for EDMX
;;
(defmethod conv/model->schema :edmx 
  ([format coll]
   (conv/model->schema format {} coll))
  ([format config coll]
   ))

(comment
  ;
  )
