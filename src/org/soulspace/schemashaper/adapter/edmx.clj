(ns org.soulspace.schemashaper.adapter.edmx
  (:require [clojure.string :as str]
            [clojure.data.xml :as xml]
            [org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]))

(def edmx->types
  {"Edm.Byte"           :byte
   "Edm.Int16"          :short
   "Edm.Int32"          :int
   "Edm.Int64"          :long
   "Edm.Single"         :float
   "Edm.Double"         :double
   "Edm.Decimal"        :decimal
   "Edm.Boolean"        :boolean
   "Edm.String"         :string
   "Edm.Guid"           :uuid
   "Edm.Date"           :date             ; TODO
   "Edm.TimeOfDay"      :time             ; TODO
   "Edm.Duration"       :duration         ; TODO
   "Edm.DateTimeOffset" :date-time-offset ; TODO
   "Edm.Binary"         :binary})

(def types->edmx
  {:byte     "Edm.Byte"
   :short    "Edm.Int16"
   :int      "Edm.Int32"
   :long     "Edm.Int64"
   :float    "Edm.Single"
   :double   "Edm.Double"
   :decimal  "Edm.Decimal"
   :boolean  "Edm.Boolean"
   :string   "Edm.String"
   :uuid     "Edm.Guid"
   :binary   "Edm.Binary"
   :enum     "Edm.String"
   :date     "Edm.Date"                   ; TODO
   :time     "Edm.TimeOfDay"              ; TODO
   :duration "Edm.Duration"               ; TODO
   :date-time-offset "Edm.DateTimeOffset" ; TODO
   })

(defn name->id
  "Generates an id from a name."
  [n]
  (-> n
      (str/replace "_" "/")
      (keyword)))

(defn collection?
  "Returns true if t is a collection type."
  [t]
  (str/starts-with? t "Collection("))

(defn collection-type
  "Returns the type if t is a collection."
  [t]
;  (println "Collection type?" t)
  (when-let [match (re-matches #"Collection\((.*)\)" t)]
    (second match)))

(defn elements
  "Returns the elements of the first schema."
  [{:keys [tag attrs content] :as e}]
  (when (= :Edmx tag)
    (-> content
        first
        :content
        first
        :content)))

(defn schema
  "Returns the first Schema element from the content of the DataService element."
  [{:keys [tag attrs content] :as e}]
  (when (= :DataService tag)
    (->> content
         (filter #(= :Schema (:tag %)))
         first)))

(defn data-service
  "Returns the DataService element."
  [{:keys [tag attrs content] :as e}]
  (when (= :Edmx tag)
    (->> content
         (filter #(= :DataService (:tag %)))
          first)))

;; new parserless
(defn ->field
  "Returns a model field for the property."
  [{:keys [tag attrs content] :as e}]
;  (print "Field " e)
  (let [f-name (:Name attrs)
        f-type (get attrs :Type (:ToRole attrs))]
    (if-let [collection-type (collection-type f-type)]
      {:el :field
       :edmx/tag tag
       :name f-name
       :collection :list
       :cardinality :zero-to-many
       :type (get edmx->types collection-type collection-type)}
      {:el :field
       :name f-name
       :optional (Boolean/valueOf (get attrs :Nullable "true"))
       :type (get edmx->types f-type f-type)})))

(defn ->class
  "Returns a model class for the EntityType tag."
  [{:keys [tag attrs content] :as e}]
;  (println "Element" e)
  (when (= tag :EntityType)
    {:el :class
     :edmx/tag tag
     :id (name->id (:Name attrs))
     :name (:Name attrs)
     :ct (into [] (concat (map ->field (filter #(= :Property (:tag %)) content))
                          (map ->field (filter #(= :NavigationProperty (:tag %)) content))))}))

(defmethod conv/schema->model :edmx
  [format input]
;  (println "Format" format ", File" file)
  (let [edmx (xml/parse-str input)
;        _ (println "EDMX" edmx)
        els (elements edmx)
;        _ (println "Elements" els)
        model (map ->class (filter #(= :EntityType (:tag %)) els))]
    model))

(defmethod conv/model->schema :edmx 
  [format coll])

(comment
  (keyword (str/replace "Bla.Fasel_Foo" "_" "/"))
  (Boolean/valueOf "true")
  (collection-type "Collection(IdentData)")
  ; (slurp "C:/PAG/datona/Cluu_Odata_$metadata.xml")
  ;
  )
