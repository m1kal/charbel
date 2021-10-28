(ns charbel.expressions
  (:require [clojure.string :as s]))

(defn from-intermediate [value]
  (if (keyword? value) (symbol value) (if (or (string? value) (number? value)) value "-unknown-type-")))

(defn max-width [args]
  (cond
    (apply = args) (first args)
    (every? number? args) (apply max args)
    :else (reduce (fn [acc elem] (str "`max(" acc ", " elem ")"))
            (map (comp str from-intermediate) args))))

(defn width-sum [args]
  (if (every? number? args)
    (apply + args)
    (s/join " + " (map (comp str from-intermediate) args))))

(declare expression)

(defmulti complex-expression (fn [expr _] (first expr)))

(defmethod complex-expression :select [[_ & b] env]
  {:result (str (:result (expression (first b) env))
                "[" (second b) (if (= 2 (count b)) "]" (str ":" (last b) "]")))
   :width  (if (= 2 (count b)) 1 (inc (- (second b) (last b))))})

(defmethod complex-expression :vector [[_ & r] env]
  (let [e (map #(expression % env) r)]
    {:result (str "{" (s/join ", " (map :result e)) "}") :width (width-sum (map :width e))}))

(defmethod complex-expression :+ [[_ & r] env]
  (let [e (map #(expression % env) r)]
    {:result (s/join " + " (map :result e)) :width (width-sum [(max-width (map :width e)) 1])}))

(defmethod complex-expression :- [[_ & r] env]
  (let [e (map #(expression % env) r)]
    {:result (s/join " - " (map :result e)) :width (max-width (map :width e))}))

(defmethod complex-expression :* [[_ & r] env]
  (let [e (map #(expression % env) r)]
    {:result (s/join " * " (map :result e)) :width (width-sum (map :width e))}))

(defmethod complex-expression :inc [[_ x] env]
  (let [e1 (expression x env)]
    {:result (str (:result e1) " + 1") :width (width-sum [1 (:width e1)])}))

(defmethod complex-expression :dec [[_ x] env]
  (let [e1 (expression x env)]
    {:result (str (:result e1) " - 1") :width (:width e1)}))

(defmethod complex-expression :bit-and [[_ & r] env]
  (let [e (map #(expression % env) r)]
    {:result (s/join " & " (map :result e)) :width (max-width (map :width e))}))

(defmethod complex-expression :bit-or [[_ & r] env]
  (let [e (map #(expression % env) r)]
    {:result (s/join " | " (map :result e)) :width (max-width (map :width e))}))

(defmethod complex-expression :bit-xor [[_ & r] env]
  (let [e (map #(expression % env) r)]
    {:result (s/join " ^ " (map :result e)) :width (max-width (map :width e))}))

(defmethod complex-expression :bit-not [[_ x] env]
  (let [e1 (expression x env)]
    {:result (str "~" (:result e1)) :width (:width e1)}))

(defmethod complex-expression :if [[_ x y z] env]
  (let [e1 (expression x env) e2 (expression y env) e3 (expression z env)]
    {:result (str (:result e1) " ? " (:result e2) " : " (:result e3))
     :width  (max (:width e2) (:width e3))}))

(defmethod complex-expression := [[_ x y & _] env]
  (let [e1 (expression x env) e2 (expression y env)]
    {:result (str (:result e1) " == " (:result e2)) :width 1}))

(defmethod complex-expression :mod [[_ x y] env]
  (let [e1 (expression x env) e2 (expression y env)]
    {:result (str (:result e1) " % " (:result e2)) :width (:width e2)}))

(defmethod complex-expression :width [[_ w x] env]
  (let [e1 (expression w env) e2 (expression x env)
        e2 (if (and (number? (:result e1)) (number? (:result e2)))
               (assoc e2 :result (str (:result e1) "'d" (:result e2)))
               e2)
        width (if (string? w) w (:result e1))]
    (assoc e2 :width width)))

(defmethod complex-expression :init [[_ iv x] env]
  (assoc (expression x env) :init iv))

(defmethod complex-expression :get [[_ mem addr] env]
  (let [e (expression addr env)]
    {:result (str (symbol mem) "[" (:result e) "]") :width 128}))

(defmethod complex-expression :cond [[_ cond expr & args] env]
  (let [e (expression expr env)]
    (if (empty? args)
      e
      (let [r (expression (cons :cond args) env)]
        {:result (str (:result (expression cond env)) " ? " (:result e) " : " (:result r))
         :width  (max-width [(:width e) (:width r)])}))))

(defmethod complex-expression :default [form _]
  {:result (str "unknown " (first form)) :width 32})

(defn expression
  ([x] (expression x {}))
  ([form env]
   (cond
     (number? form) {:result form :width (inc (int (Math/floor (/ (Math/log (max form 1)) (Math/log 2)))))}
     (keyword? form) {:result (str (from-intermediate form)) :width (or (env form) 32)}
     (string? form) {:result form :width 32}
     (or (vector? form) (seq? form))
     (let [e (complex-expression form env)]
       (assoc e :result (str "(" (:result e) ")")))
     (= true form) {:width 1 :result 1}
     (= false form) {:width 1 :result 0}
     :else {:result "unknown" :width "unknown"})))
