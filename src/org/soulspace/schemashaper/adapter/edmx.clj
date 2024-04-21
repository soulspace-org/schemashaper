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

(defn edmx-type->model-type
  "Returns the model type for the qualified edmx type `t`.
   
   If t is an edmx base type, the corresponding model base type is returned.
   Otherwise t is returned as is."
  [t]
  (get edmx->types t t))

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
  (println "Collection type?" t)
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
  [criteria e]
  (if-let [include-set (:include-set criteria)]
    (if (contains? include-set e)
      true
      false)
    ; include, if no include set is specified
    true))

(defn exclude?
  "Returns true if the element `e` is excluded by the given `criteria`."
  [criteria e]
  (if-let [exclude-set (:exclude-set criteria)]
    (if (contains? exclude-set e)
      true
      false)
    ; don't exclude, if no exclude set is specified
    false))

(defn include-element?
  "Returns true if the element with the `qualified-name` is included by the given `criteria`."
  [criteria qualified-name]
  (if (edm-type? qualified-name)
    ; always include Edm base types
    true
    ; not a base type, check against criteria
;    (let [result (and (include? criteria qualified-name)
;                      (not (exclude? criteria qualified-name)))]
;      (println "Check for result for" qualified-name "is" result)
;      result)
    (and (include? criteria qualified-name)
         (not (exclude? criteria qualified-name)))
    ))

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

(defn edmx-property->field
  "Returns a model field for the property element `e` in the context of the `schema-ns`."
  [schema-ns criteria {:keys [tag attrs content] :as e}]
  (when (contains? #{:Property :NavigationProperty} tag)
    (let [e-name (:Name attrs)
          type-map (parse-type-name (get attrs :Type (qualified-name schema-ns (:ToRole attrs))))
          qualified-type (:qualified-name type-map)]
      (when (include-element? criteria qualified-type)
        (if (:collection type-map)
          {:el :field
           :edmx/tag tag
           :name e-name
           :collection :list
           :card :zero-to-many
           :type (get edmx->types qualified-type qualified-type)}
          {:el :field
           :edmx/tag tag
           :name e-name
           :optional (optional? e)
           :type (get edmx->types qualified-type qualified-type)})))))

(defn edmx-entity-type->class
  "Returns a model class for the EntityType element `e` in the context of the `schema-ns`."
  [schema-ns criteria {:keys [tag attrs content] :as e}]
  (when (contains? #{:EntityType} tag)
    (let [e-name (:Name attrs)
          qname (qualified-name e-name schema-ns)
          ct (into [] (concat (map (partial edmx-property->field
                                            schema-ns criteria)
                                   (filter (tag-pred :Property) content))
                              (map (partial edmx-property->field
                                            schema-ns criteria)
                                   (filter (tag-pred :NavigationProperty) content))))]
      (when (include-element? criteria
                              qname)
        {:el :class
         :edmx/tag tag
         :edmx/schema-ns schema-ns
         :id (name->id schema-ns e-name)
         :name e-name
         :abstract (abstract? e)
         :ct ct}))))

(defn edmx-association->relation
  "Returns a model relation for the Association element in the context of the `schema-ns`."
  [schema-ns criteria {:keys [tag attrs content] :as e}]
  (when (contains? #{:Association} tag))
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
  (base-type "Collection(IdentData)")
  (split-index "Many0")
  (parse-type-name "ODataAPI.Event")
  (parse-type-name "Collection(ODataAPI.Event)")
  (parse-association-name "Event_Session_One_Many0" "ODataAPI")
  (parse-relationship-name "ODataAPI.Event_Session_One_Many0")
  (qualified-name "Event" "ODataAPI")
  (edm-type? "Edm.Int32")
  (edm-type? "ODataAPI.Event")
  (include? {} "ODataAPI.Event")
  (include? {:include-set #{"ODataAPI.Event"}} "ODataAPI.Event")
  (include? {:include-set #{"ODataAPI.Track"}} "ODataAPI.Event")
  (exclude? {} "ODataAPI.Event")
  (exclude? {:exclude-set #{"ODataAPI.Event"}} "ODataAPI.Event")
  (exclude? {:exclude-set #{"ODataAPI.Track"}} "ODataAPI.Event")

  (def test-event-props
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
  (def test-session-props
    [{:tag :Property
      :attrs {:Name "SessionId"
              :Type "Edm.Int64"
              :Nullable "false"}
      :content []}
     {:tag :NavigationProperty
      :attrs {:Name "Event"
              :Relationship "ODataAPI.Session_Event_Many_ZeroToOne1"
              :FromRole "Session"
              :ToRole "Event"}
      :content []}])
  (def test-entities
    [{:tag :EntityType
      :attrs {:Name "Event"}
      :content test-event-props}
     {:tag :EntityType
      :attrs {:Name "Session"}
      :content test-session-props}])

  (filter (tag-pred :Property) test-event-props)
  (filter (tag-pred :NavigationProperty) test-event-props)
  (concat (filter (tag-pred :Property) test-event-props)
          (filter (tag-pred :NavigationProperty) test-event-props))
  (into [] (concat (filter (tag-pred :Property) test-event-props)
                   (filter (tag-pred :NavigationProperty) test-event-props)))

  (edmx-entity-type->class "ODataAPI" {}
                           (first test-entities))
  (edmx-entity-type->class "ODataAPI" {:include-set #{"ODataAPI.Event"}
                                       :exclude-set #{}}
                           (first test-entities))
  (edmx-entity-type->class "ODataAPI" {:include-set #{}
                                       :exclude-set #{"ODataAPI.Event"}}
                           (first test-entities))
  (map (partial edmx-entity-type->class "ODataAPI" {})
       (filter #(= :EntityType (:tag %)) test-entities))



  ;
  )