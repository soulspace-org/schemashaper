(ns org.soulspace.schemashaper.domain.model
  )

(def model-nodes
  #{:class :field :method :enum :enum-value})

(def model-relations
  #{:composition :aggregation :association})

(def types
  #{:byte :short :int :long :float :double :decimal :boolean :string
    :uuid :binary :instant :date :time :duration :date-time-offset
    :list :map :set :class})

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
   ; selection handled by th select function
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

