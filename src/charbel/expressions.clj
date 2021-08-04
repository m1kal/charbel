(ns charbel.expressions)

(defn from-intermediate [value]
  (if (keyword? value) (symbol value) (if (number? value) value "-unknown-type-")))

(declare expression)

(defmulti complex-expression (fn [expr env] (first expr)))

(defmethod complex-expression :select [[a & b] env]
  {:result (str (:result (expression (first b) env))
                "[" (second b) (if (= 2 (count b)) "]" (str ":" (last b) "]")))
   :width  (if (= 2 (count b)) 1 (inc (- (second b) (last b))))})

(defmethod complex-expression :+ [[a x y & r] env]
  (let [e1 (expression x env) e2 (expression y env)]
    {:result (str (:result e1) " + " (:result e2)) :width (inc (max (:width e1) (:width e2)))}))

(defmethod complex-expression :- [[a x y & r] env]
  (let [e1 (expression x env) e2 (expression y env)]
    {:result (str (:result e1) " - " (:result e2)) :width (max (:width e1) (:width e2))}))

(defmethod complex-expression :* [[a x y & r] env]
  (let [e1 (expression x env) e2 (expression y env)]
    {:result (str (:result e1) " * " (:result e2)) :width (+ (:width e1) (:width e2))}))

(defmethod complex-expression :inc [[a x & r] env]
  (let [e1 (expression x env)]
    {:result (str (:result e1) " + 1") :width (inc (:width e1))}))

(defmethod complex-expression :dec [[a x & r] env]
  (let [e1 (expression x env)]
    {:result (str (:result e1) " - 1") :width (:width e1)}))

(defmethod complex-expression :bit-and [[a x y & r] env]
  (let [e1 (expression x env) e2 (expression y env)]
    {:result (str (:result e1) " & " (:result e2)) :width (max (:width e1) (:width e2))}))

(defmethod complex-expression :bit-or [[a x y & r] env]
  (let [e1 (expression x env) e2 (expression y env)]
    {:result (str (:result e1) " | " (:result e2)) :width (max (:width e1) (:width e2))}))

(defmethod complex-expression :bit-xor [[a x y & r] env]
  (let [e1 (expression x env) e2 (expression y env)]
    {:result (str (:result e1) " ^ " (:result e2)) :width (max (:width e1) (:width e2))}))

(defmethod complex-expression :if [[a x y z & r] env]
  (let [e1 (expression x env) e2 (expression y env) e3 (expression z env)]
    {:result (str (:result e1) " ? " (:result e2) " : " (:result e3))
     :width  (max (:width e2) (:width e3))}))

(defmethod complex-expression := [[a x y & r] env]
  (let [e1 (expression x env) e2 (expression y env)]
    {:result (str (:result e1) " == " (:result e2)) :width 1}))

(defmethod complex-expression :mod [[a x y & r] env]
  (let [e1 (expression x env) e2 (expression y env)]
    {:result (str (:result e1) " % " (:result e2)) :width (:width e2)}))

(defmethod complex-expression :width [[a w x & r] env]
  (let [e1 (expression w env) e2 (expression x env)]
    {:result (str (:result e2)) :width (:result e1)}))

(defmethod complex-expression :get [[a mem addr & r] env]
  (let [e (expression addr env)]
    {:result (str (symbol mem) "[" (:result e) "]") :width 128}))

(defmethod complex-expression :default [form env]
  {:result (str "unnkown " (first form)) :width 32})

(defn expression
  ([x] (expression x {}))
  ([form env]
   (cond
     (number? form) {:result form :width (inc (int (Math/floor (/ (Math/log (max form 1)) (Math/log 2)))))}
     (keyword? form) {:result (str (from-intermediate form)) :width (or (env form) 32)}
     (or (vector? form) (seq? form))
     (let [e (complex-expression form env)]
       {:result (str "(" (:result e) ")") :width (:width e)})
     :else {:result "unknown" :width "unknown"})))
