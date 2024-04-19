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


(defn element-tag?
  "Returns true if the tag of the `element` equals the given `tag`."
  [tag element]
  (= tag (:tag element)))

(defn tag-pred
  "Returns a predicate that takes an element and checks if the element has the given `tag`"
  [tag]
  (fn [e] (= tag (:tag e))))

(defn tag-pred
  "Returns a predicate that takes an element and checks if the element has the given `tag`"
  [tag]
  (fn [e] (let [_ (print "Tag" tag)
                _ (println " Element" e)
                result (= tag (:tag e))
                _ (println "Result" result)]
            result)))

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
(defn ->field
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

(defn ->class-simple
  "Returns a model class for the EntityType tag."
  [schema-ns criteria {:keys [tag attrs content] :as e}]
  (when (contains? #{:EntityType} tag)
    (let [ct (into [] (concat (map (partial ->field schema-ns)
                                   (filter (tag-pred :Property) content))
                              (map (partial ->field schema-ns)
                                   (filter (tag-pred :NavigationProperty) content))))]
      {:el :class
       :edmx/tag tag
       :edmx/schema-ns schema-ns
       :id (name->id schema-ns (:Name attrs))
       :name (:Name attrs)
       :ct ct})))



(defn ->class
  "Returns a model class for the EntityType tag."
  [schema-ns criteria {:keys [tag attrs content] :as e}]
  (let [id (name->id schema-ns (:Name attrs))
        properties (filter (tag-pred :Property) content)
        nav-properties (filter (tag-pred :NavigationProperty) content)
        ct (into [] (concat (map (partial ->field schema-ns)
                                 properties)
                            (map (partial ->field schema-ns)
                                 nav-properties)))]
    {:el :class
     :edmx/tag tag
     :edmx/schema-ns schema-ns
     :id (name->id schema-ns (:Name attrs))
     :name (:Name attrs)
     :ct ct}))

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
         model (map (partial ->class schema-namespace criteria)
                    (filter (tag-pred :EntityType) els))]
     model)))

(defmethod conv/model->schema :edmx 
  ([format coll]
   (conv/model->schema format {} coll))
  ([format criteria coll]
   )
  )

(comment
  (keyword (str/replace "Bla.Fasel_Foo" "_" "/"))
  (Boolean/valueOf "true")
  (collection-type "Collection(IdentData)")
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
  (->class "MyNS" {} {:tag :EntityType :attrs {:Name "Event"} :content test-props})
  (->class "MyNS" {} (first test-entities))
  (map (partial ->class "ODataAPI" {})
       (filter #(= :EntityType (:tag %)) test-entities))



  ;
  )
