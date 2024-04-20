(ns org.soulspace.schemashaper.adapter.edmx
  (:require [clojure.string :as str]
            [clojure.data.xml :as xml]
            ;[org.soulspace.schemashaper.domain.model :as model]
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

(defn include?
  "Returns true if the element `e` is included by the given `criteria`."
  [criteria e]
  (if-let [include-set (:include-set criteria)]
    (if (contains? include-set e)
      true
      false)
    true))

(defn exclude?
  "Returns true if the element `e` is excluded by the given `criteria`."
  [criteria e]
  (if-let [exclude-set (:exclude-set criteria)]
    (if (contains? exclude-set e)
      false
      true)
    false))

(defn element-tag?
  "Returns true if the tag of the `element` equals the given `tag`."
  [tag element]
  (= tag (:tag element)))

(defn tag-pred
  "Returns a predicate that takes an element and checks if the element has the given `tag`"
  [tag]
  (fn [e] (= tag (:tag e))))

(defn split-index
  ""
  [n]
  (re-matches #"(\w+)(\d+)" n))

(defn parse-name
  "Parses the name `n` and returns a map with the name and the namespace, if contained."
  [n]
  (let [parts (str/split n #"\.")]
    (if (> (count parts) 1)
      {:name (last parts)
       :schema-ns (str/join "." (drop-last parts))}
      {:name (first parts)})))

(defn parse-association-name
  "Parses the name `n` of an association and returns a map with the names
   and cardinalities of the roles of the association."
  [n]
  (let [parts (str/split n #"_")
        [_ to-card idx] (split-index (nth parts 3))]
  {:from-name (nth parts 0)
   :to-name (nth parts 1)
   :from-card (nth parts 2)
   ; TODO handle trailing digits
   :to-card to-card
   :index idx}))

(defn parse-relation-name
  "Parses the name `n` of a relation and returns a map with the namespace and the names
   and cardinalities of the roles of the relation."
  [n]
  (let [parsed-name (parse-name n)
        parsed-asso (parse-association-name (:name parsed-name))]
  (merge parsed-name parsed-asso)))

(defn type-name
  ""
  ([{:keys [schema-ns name]}]
   (type-name schema-ns name))
  ([schema-ns name]
   (str/join "." [schema-ns name])))

(defn name->id
  "Generates an id from a name."
  ([n]
   (-> n
       (str/replace "_" "/")
       (keyword)))
  ([schema-ns n]
   (->> n
        (str schema-ns "/")
        (keyword))))

(defn collection-type
  "Returns the type if t is a collection, otherwise returns nil."
  [t]
  (println "Collection type?" t)
  (when-let [match (re-matches #"Collection\((.*)\)" t)]
    (second match)))

(defn schema
  "Returns the first Schema element from the content of the DataService element."
  [{:keys [tag attrs content] :as e}]
  (when (= :DataServices tag)
    (->> content
         (filter (tag-pred :Schema))
         first)))

(defn data-service
  "Returns the DataService element."
  [{:keys [tag attrs content] :as e}]
  (when (= :Edmx tag)
    (->> content
         (filter (tag-pred :DataServices))
         first)))

;; new parserless
(defn edmx-property->field
  "Returns a model field for the property element `p` in the context of the `schema-ns`."
  [schema-ns criteria {:keys [tag attrs content] :as p}]
  (when (contains? #{:Property :NavigationProperty} tag)
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
         :type (get edmx->types f-type f-type)}))))

(defn edmx-entity-type->class
  "Returns a model class for the EntityType element `e` in the context of the `schema-ns`."
  [schema-ns criteria {:keys [tag attrs content] :as e}]
  (when (contains? #{:EntityType} tag)
    (let [ct (into [] (concat (map (partial edmx-property->field
                                            schema-ns criteria)
                                   (filter (tag-pred :Property) content))
                              (map (partial edmx-property->field
                                            schema-ns criteria)
                                   (filter (tag-pred :NavigationProperty) content))))]
      {:el :class
       :edmx/tag tag
       :edmx/schema-ns schema-ns
       :id (name->id schema-ns (:Name attrs))
       :name (:Name attrs)
       :ct ct})))

(defn edmx-association->relation
  "Returns a model relation for the Association element in the context of the `schema-ns`."
  [schema-ns criteria {:keys [tag attrs content] :as e}]
  )

;;
;; Conversion functions for EDMX
;;
(defmethod conv/schema->model :edmx
  ([format input]
   (conv/schema->model format {} input))
  ([format criteria input]
   (let [edmx (xml/parse-str input)
;         _ (println "EDMX" edmx)
         data-service (data-service edmx)
         schema (schema data-service)
         schema-namespace (:Namespace (:attrs schema))
         els (:content schema)
         model (map (partial edmx-entity-type->class
                             schema-namespace criteria)
                    (filter (tag-pred :EntityType) els))]
     model)))

(defmethod conv/model->schema :edmx 
  ([format coll]
   (conv/model->schema format {} coll))
  ([format criteria coll]
   ))

(comment
  (keyword (str/replace "Bla.Fasel_Foo" "_" "/"))
  (Boolean/valueOf "true")
  (collection-type "Collection(IdentData)")
  (split-index "Many0")
  (parse-name "ODataAPI.Event")
  (parse-association-name "Event_Session_One_Many0")
  (parse-relation-name "ODataAPI.Event_Session_One_Many0")
  (def test-props
    [{:tag :Property
      :attrs {:Name "EventID"
              :Type "Edm.Int64"
              :Nullable "false"}
      :content []}
     {:tag :Property
      :attrs {:Name "Description"
              :Type "Edm.String"
              :Nullable "true"
              :MaxLength 512}
      :content []}])
  (def test-entities
    [{:tag :EntityType
      :attrs {:Name "Event"}
      :content [{:tag :Property
                 :attrs {:Name "EventID"
                         :Type "Edm.Int64"
                         :Nullable "false"}
                 :content []}
                {:tag :Property
                 :attrs {:Name "Description"
                         :Type "Edm.String"
                         :Nullable "true"
                         :MaxLength 512}
                 :content []}]}])
  (def test-entities2
    [{:tag :EntityType
      :attrs {:Name "Event"}
      :content test-props}])
  
  (filter (tag-pred :Property) test-props)
  (filter (tag-pred :NavigationProperty) test-props)
  (concat (filter (tag-pred :Property) test-props)
          (filter (tag-pred :NavigationProperty) test-props))
  (into [] (concat (filter (tag-pred :Property) test-props)
                   (filter (tag-pred :NavigationProperty) test-props)))
  
  ; doesn't work
  (edmx-entity-type->class "MyNS" {} {:tag :EntityType :attrs {:Name "Event"} :content test-props})
  (edmx-entity-type->class "MyNS" {} (first test-entities))
  (map (partial edmx-entity-type->class "ODataAPI" {})
       (filter #(= :EntityType (:tag %)) test-entities))



  ;
  )
