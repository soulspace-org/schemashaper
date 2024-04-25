(ns org.soulspace.schemashaper.domain.model
  )

(def model-nodes
  #{:annotation :class :enum :enum-value :field :function :interface
    :method :namespace :package :parameter :protocol :stereotype})

(def model-relations
  #{:aggregation :association :composition :implementation :inheritance})

(def model-hierarchy
  (-> (make-hierarchy)
      (derive :annotation     :model-node)
      (derive :class          :model-node)
      (derive :enum           :model-node)
      (derive :enum-value     :model-node)
      (derive :field          :model-node)
      (derive :function       :model-node)
      (derive :interface      :model-node)
      (derive :method         :model-node)
      (derive :namespace      :model-node)
      (derive :package        :model-node)
      (derive :parameter      :model-node)
      (derive :protocol       :model-node)
      (derive :stereotype     :model-node)
      (derive :aggregation    :model-relation)
      (derive :association    :model-relation)
      (derive :composition    :model-relation)
      (derive :implementation :model-relation)
      (derive :inheritance    :model-relation)
      (derive :model-node     :model-element)
      (derive :model-relation :model-element)))

(def types
  #{:byte :short :int :long :float :double :decimal :boolean :string
    :uuid :binary :instant :date :time :duration :date-time-offset
    :list :map :set :class})

(defn element-type
  "Returns the element type."
  ([e] (:el e))
  ([_ e] (:el e))
  ([_ _ e] (:el e)))

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

(defn traverse
  "Recursively traverses the `coll` of elements and returns the elements (selected
   by the optional `select-fn`) and transformed by the `step-fn`.

   `select-fn` - a predicate on the current element
   `step-fn` - a function with three signatures [], [acc] and [acc e]
   
   The no args signature of the `step-fn` should return an empty accumulator,
   the one args signature extracts the result from the accumulator on return
   and the 2 args signature receives the accumulator and the current element and
   should add the transformed element to the accumulator."
  ([step-fn coll]
   ; selection might be handled in the step function
   (letfn [(trav [acc coll]
             (if (seq coll)
               (let [e (first coll)]
                 (recur (trav (step-fn acc e) (:ct e))
                        (rest coll)))
               (step-fn acc)))]
     (trav (step-fn) coll)))
  ([select-fn step-fn coll]
   ; selection handled by the select function
   (letfn [(trav [acc coll]
             (if (seq coll)
               (let [e (first coll)]
                 (if (select-fn e)
                   (recur (trav (step-fn acc e) (:ct e))
                          (rest coll))
                   (recur (trav acc (:ct e))
                          (rest coll))))
               (step-fn acc)))]
     (trav (step-fn) coll))))

(comment
  (include? {} "API.Event")
  (include? {:include-set #{}} "API.Event")
  (include? {:include-set #{"API.Event"}} "API.Event")
  (include? {:include-set #{"API.Track"}} "API.Event")
  (exclude? {} "API.Event")
  (exclude? {:exclude-set #{}} "API.Event")
  (exclude? {:exclude-set #{"API.Event"}} "API.Event")
  (exclude? {:exclude-set #{"API.Track"}} "API.Event")
  ;
  )