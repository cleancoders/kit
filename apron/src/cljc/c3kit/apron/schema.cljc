(ns c3kit.apron.schema
  "Defines data structure, coerces, validates."
  (:refer-clojure :exclude [uri?])
  (:require
    [c3kit.apron.corec :as ccc]
    [clojure.edn :as edn]
    [clojure.string :as str]
    #?(:cljs [com.cognitect.transit.types]) ;; https://github.com/cognitect/transit-cljs/issues/41
    ))

(comment "Schema Sample"
         {:field {:type     :string                         ; see type-validators for list
                  :db       [:unique-value]                       ; passed to database
                  :coerce   [#(str % "y")]                  ; single/list of coerce fns
                  :validate [#(> (count %) 1)]              ; single/list of validation fns
                  :message  "message describing the field"
                  :present  [#(str %)]                      ; single/list of presentation fns
                  }})

(def stdex
  #?(:clj  clojure.lang.ExceptionInfo
     :cljs cljs.core/ExceptionInfo))

(defn coerce-ex [v type]
  (ex-info (str "can't convert " (pr-str v) " to " type) {:value v :type type}))

(def date #?(:clj java.util.Date :cljs js/Date))

(defn exmessage [e]
  (when e
    #?(:clj  (.getMessage e)
       :cljs (cljs.core/ex-message e))))

; Common Validations --------------------------------------

(defn present? [v]
  (not (or (nil? v)
           (and (string? v) (str/blank? v)))))

(defn nil-or [f]
  (fn [v]
    (or (nil? v) (f v))))

(def email-pattern #"[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?")

(defn email? [value] (if (re-matches email-pattern value) true false))

(defn uri? [value]
  #?(:clj  (instance? java.net.URI value)
     :cljs (string? value)))

(defn is-enum? [enum]
  (let [enum-set (set (map #(keyword (name (:enum enum)) (name %)) (:values enum)))]
    (fn [value]
      (or (nil? value)
          (contains? enum-set value)))))

; Common Coersions ----------------------------------------

#?(:cljs
   (defn parse! [f v]
     (let [result (f v)]
       (if (js/isNaN result)
         (throw (js/Error "parsed NaN"))
         result))))

(defn ->boolean [value]
  (cond (nil? value) false
        (boolean? value) value
        (string? value) (not (= "false" (str/lower-case value)))
        :else (boolean value)))

(defn ->string [value]
  (if (= nil value)
    nil
    (str value)))

(defn ->keyword [value]
  (cond
    (= nil value) nil
    (keyword? value) value
    :else (let [s (str value)]
            (if (str/starts-with? s ":")
              (keyword (subs s 1))
              (keyword s)))))

(defn ->float [v]
  (cond
    (= nil v) nil
    (string? v) (if (str/blank? v)
                  nil
                  (try
                    #?(:clj (Double/parseDouble v) :cljs (parse! js/parseFloat v))
                    (catch #?(:clj Exception :cljs :default) _
                      (throw (coerce-ex v "float")))))
    #?@(:cljs [(js/isNaN v) nil])
    (integer? v) (double v)
    (#?(:clj float? :cljs number?) v) v
    :else (throw (coerce-ex v "float"))))

(defn ->int [v]
  (cond
    (= nil v) nil
    (string? v) (if (str/blank? v)
                  nil
                  (try
                    #?(:clj  (long (Double/parseDouble v))
                       :cljs (parse! js/parseInt v))
                    (catch #?(:clj Exception :cljs :default) _
                      (throw (coerce-ex v "int")))))
    #?@(:cljs [(js/isNaN v) nil])
    (integer? v) v
    (#?(:clj float? :cljs number?) v) (long v)
    :else (throw (coerce-ex v "float"))))

(defn ->date [v]
  (cond
    (nil? v) nil
    (instance? date v) v
    (integer? v) (doto (new #?(:clj java.util.Date :cljs js/Date)) (.setTime v))
    ;#?(:clj (instance? org.joda.time.DateTime v)) #?(:clj (java.util.Date. (.getMillis v)))
    #?(:cljs (instance? goog.date.Date v)) #?(:cljs (js/Date. (.getTime v)))
    (and (string? v) (str/starts-with? v "#inst")) (edn/read-string v)
    :else (throw (coerce-ex v "date"))))

(defn ->uri [v]
  (cond
    (nil? v) nil
    #?@(:clj [(instance? java.net.URI v) v])
    (string? v) #?(:clj (java.net.URI/create v) :cljs v)
    :else (throw (coerce-ex v "uri"))))


;; MDM : https://github.com/cognitect/transit-cljs/issues/41
#?(:cljs (extend-type com.cognitect.transit.types/UUID IUUID))

(defn ->uuid [v]
  (cond
    (nil? v) nil
    (uuid? v) v
    (string? v) #?(:clj (java.util.UUID/fromString v) :cljs (uuid v))
    :else (throw (coerce-ex v "uuid"))))

; Type Tables ---------------------------------------------

(def type-validators
  {:boolean (nil-or #(or (= true %) (= false %)))
   :double  (nil-or #?(:clj float? :cljs number?))
   :float   (nil-or #?(:clj float? :cljs number?))
   :instant (nil-or #(instance? date %))
   :int     (nil-or integer?)
   :keyword (nil-or keyword?)
   :kw-ref  (nil-or keyword?)
   :long    (nil-or integer?)
   :ref     (nil-or integer?)
   :string  (nil-or string?)
   :uri     (nil-or uri?)
   :uuid    (nil-or uuid?)
   :ignore  (constantly true)})

(def type-coercers
  {:boolean ->boolean
   :double  ->float
   :float   ->float
   :instant ->date
   :int     ->int
   :keyword ->keyword
   :kw-ref  ->keyword
   :long    ->int
   :ref     ->int
   :string  ->string
   :uri     ->uri
   :uuid    ->uuid
   :ignore  identity})


; Common Schema Attributes --------------------------------

(def omit
  "Used as a :present value to remove the entry from presentation"
  (constantly nil))

(defn kind [key]
  {:type     :keyword
   :value    key
   :validate [#(or (nil? %) (= key %))]
   :coerce   [#(or % key)]
   :message  (str "mismatch; must be " key)})

(def id {:type :ref})

(defn str-or-nil [v] (if (= nil v) nil (str v)))

; Processing ---------------------------------------------

(defn- multiple? [thing]
  (or (sequential? thing)
      (set? thing)))

(defn- ->vec [v]
  (cond
    (nil? v) []
    (multiple? v) (vec v)
    :else [v]))

(defn ->seq [v]
  (cond
    (nil? v) []
    (multiple? v) v
    :else (list v)))

(defn type-coercer! [type]
  (or (get type-coercers type)
      (throw (ex-info (str "unhandled coersion type: " type) {}))))

(defn type-validator! [type]
  (or (get type-validators type)
      (throw (ex-info (str "unhandled validation type: " type) {}))))

(defn build-coersion [spec]
  (try
    (let [type (:type spec)
          customs (->vec (:coerce spec))
          ?seq (multiple? type)
          type (if ?seq (first type) type)
          coersions (conj customs (type-coercer! type))
          coersion (fn [value] (reduce #(%2 %1) value coersions))]
      (if ?seq
        #(mapv coersion (->seq %))
        coersion))
    (catch
      #?(:clj Exception :cljs :default) _
      (throw (ex-info "unhandled coersion" {:spec spec})))))

(defn build-validator [spec]
  (try
    (let [type (:type spec)
          customs (->vec (:validate spec))
          ?seq (multiple? type)
          type (if ?seq (first type) type)
          validators (cons (type-validator! type) customs)
          validator (fn [value] (every? #(% value) validators))]
      (if ?seq
        #(if (nil? %)
           (validator %)
           (and (multiple? %) (every? validator %)))
        #(validator %)))
    (catch
      #?(:clj Exception :cljs :default) e
      (throw (ex-info "unhandled validation" {:spec spec} e)))))

; Error Handling ------------------------------------------

(defrecord SchemaError [errors schema before after]
  Object
  (toString [_] (str "SchemaError: " errors)))

(defn make-error [errors schema before after]
  (SchemaError. errors schema before after))

(defn error? [result]
  (or (instance? SchemaError result)
      (and (map? result)
           (contains? result :errors)
           (contains? result :schema)
           (contains? result :before)
           (contains? result :after))))

(defn error-message-map
  "Nil when there are no errors, otherwise a map {<field> <message>} ."
  ([result]
   (when (error? result)
     (when-let [errors (seq (:errors result))]
       (into {} (map (fn [[k e]] [k (:message (ex-data e) "is invalid")]) errors))))))

(defn messages
  "Sequence of error messages in a validate/coerce/conform result; nil if none."
  [result]
  (when-let [errors (error-message-map result)]
    (mapv (fn [[k v]] (str (name k) " " v)) errors)))

; Single Value Actions ------------------------------------

(defn coerce-value
  "returns coerced value or throws an exception"
  ([schema key value] (coerce-value (get schema key) value))
  ([spec value]
   (let [coersion (build-coersion spec)]
     (try
       (coersion value)
       (catch
         #?(:clj Exception :cljs :default) e
         (throw (ex-info "coersion failed" {:message (:message spec "coersion failed") :value value} e)))))))

(defn validate-value
  "return true or falue"
  ([schema key value] (validate-value (get schema key) value))
  ([spec value]
   (let [validator (build-validator spec)]
     (if (validator value) true false))))

(defn validate-coerced-value!
  "throws an exception when validation fails, true otherwise. Does the work."
  ([spec value coerced]
   (if (try
         (validate-value spec coerced)
         (catch
           #?(:clj Exception :cljs :default) e
           (ex-info "validation error" {:message (:message spec "is invalid") :value value :coerced coerced} e)))
     coerced
     (throw (ex-info "invalid" {:message (:message spec "is invalid") :value value :coerced coerced})))))

(defn validate-value!
  "throws an exception when validation fails, true otherwise"
  ([schema key value] (validate-coerced-value! (get schema key) value value))
  ([spec value] (validate-coerced-value! spec value value)))

(defn conform-value
  "coerce and validate, returns coerced value or throws"
  ([schema key value] (conform-value (get schema key) value))
  ([spec value]
   (let [coerced (coerce-value spec value)]
     (and (validate-coerced-value! spec value coerced) coerced))))

(defn present-value
  "returns a presentable representation of the value"
  ([schema key value] (present-value (get schema key) value))
  ([spec value]
   (let [presenters (->vec (:present spec))
         presenter-fn (fn [v] (reduce #(%2 %1) v presenters))]
     (if (sequential? (:type spec))
       (when-let [result (seq (filter identity (map presenter-fn value)))] (vec result))
       (presenter-fn value)))))

; Entity Actions ------------------------------------------

(defn result-or-ex [f spec value]
  (try
    (f spec value)
    (catch #?(:clj Exception :cljs :default) e e)))

(defn- error-or-result [errors schema entity result]
  (if (seq errors)
    (SchemaError. errors schema entity result)
    result))

(defn- process-fields [processor schema entity]
  (loop [errors {} result {} specs schema]
    (if (seq specs)
      (let [[key spec] (first specs)
            value (get entity key)
            field-result (result-or-ex processor spec value)]
        (if (ccc/ex? field-result)
          (recur (assoc errors key field-result) result (rest specs))
          (let [result (if (nil? field-result) result (assoc result key field-result))]
            (recur errors result (rest specs)))))
      (error-or-result errors schema entity result))))

(defn- coerce-whole-entity [result schema entity]
  (loop [errors {} result result specs (filter (fn [[k s]] (:coerce s)) (:* schema))]
    (if (seq specs)
      (let [[key spec] (first specs)
            value (result-or-ex coerce-value spec result)]
        (if (ccc/ex? value)
          (recur (assoc errors key value) result (rest specs))
          (recur errors (assoc result key value) (rest specs))))
      (error-or-result errors schema entity result))))

(defn- validate-whole-entity [result schema entity]
  (loop [errors {} result result specs (filter (fn [[k s]] (:validate s)) (:* schema))]
    (if (seq specs)
      (let [[key spec] (first specs)
            value (result-or-ex (fn [spec value]
                                  (let [value (validate-value! spec value)]
                                    (if (ccc/ex? value)
                                      value
                                      (get result key))))
                                (assoc spec :type :ignore) result)]
        (if (ccc/ex? value)
          (recur (assoc errors key value) result (rest specs))
          (recur errors (assoc result key value) (rest specs))))
      (error-or-result errors schema entity result))))

(defn- present-whole-entity [result schema entity]
  (loop [errors {} result result specs (filter (fn [[k s]] (:present s)) (:* schema))]
    (if (seq specs)
      (let [[key spec] (first specs)
            value (result-or-ex present-value spec result)]
        (if (ccc/ex? value)
          (recur (assoc errors key value) result (rest specs))
          (recur errors (assoc result key value) (rest specs))))
      (error-or-result errors schema entity result))))

(defn coerce
  "Returns coerced entity or SchemaError if any coersion failed. Use error? to check result."
  [schema entity]
  (let [result (process-fields coerce-value (dissoc schema :*) entity)]
    (if (error? result)
      result
      (coerce-whole-entity result schema entity))))

(defn validate
  "Returns entity with all values true, or SchemaError when one or more invalid fields. Use error? to check result."
  [schema entity]
  (let [result (process-fields validate-value! (dissoc schema :*) entity)]
    (if (error? result)
      result
      (validate-whole-entity result schema entity))))

(defn conform
  "Returns coerced entity or SchemaError upon any coersion or validation failure. Use error? to check result."
  [schema entity]
  (let [result (process-fields conform-value (dissoc schema :*) entity)]
    (if (error? result)
      result
      (let [coerced (coerce-whole-entity result schema entity)]
        (if (error? result)
          result
          (validate-whole-entity coerced schema entity))))))

(defn present
  "Returns presentable entity or SchemaError upon any presentation failure. Use error? to check result."
  [schema entity]
  (let [result (process-fields present-value schema entity)]
    (if (error? result)
      result
      (let [result (present-whole-entity result schema entity)]
        (if (error? result)
          result
          (ccc/remove-nils result))))))

;(defn coersion-errors [schema entity]
;  (messages (coerce schema entity)))

(defn validation-errors [schema entity]
  (error-message-map (validate schema entity)))

(defn conform-errors [schema entity]
  (error-message-map (conform schema entity)))

(defn validate! [schema entity]
  (let [result (validate schema entity)]
    (if (error? result)
      (throw (ex-info "Invalid entity" result))
      result)))

(defn coerce! [schema entity]
  (let [result (coerce schema entity)]
    (if (error? result)
      (throw (ex-info "Uncoercable entity" result))
      result)))

(defn conform! [schema entity]
  (let [result (conform schema entity)]
    (if (error? result)
      (throw (ex-info "Unconformable entity" result))
      result)))

(defn conform-all! [schema entities]
  (let [conforms (map #(conform schema %) entities)
        errors (filter error? conforms)]
    (if (seq errors)
      (throw (ex-info "Unconformable entities" (make-error (apply merge (map #(get % :errors) errors)) schema entities conforms)))
      conforms)))

(defn present! [schema entity]
  (let [result (present schema entity)]
    (if (error? result)
      (throw (ex-info "Unpresentable entity" result))
      result)))

