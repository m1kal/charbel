(ns charbel.synthesis
  (:require [charbel.analysis :refer :all]))

(defn from-intermediate [value]
  (if (keyword? value) (symbol value) (if (number? value) value "-unknown-type-")))

(declare expression)

(defn complex-expression
  ([[a & b] env]
   (condp = a
     :select
     {:result (str (:result (expression (first b) env))
                   "["
                   (second b)
                   (if (= 2 (count b)) "]" (str ":" (last b) "]")))
      :width  (if (= 2 (count b)) 1 (inc (- (second b) (last b))))}
     :+
     (let [[x y] b
           e1 (expression x env) e2 (expression y env)]
     {:result (str (:result e1) " + " (:result e2)) :width (max (:width e1) (:width e2))})
     :bit-and
     (let [[x y] b
           e1 (expression x env) e2 (expression y env)]
       {:result (str (:result e1) " & " (:result e2)) :width (max (:width e1) (:width e2))})
     :if
     (let [[x y z] b
           e1 (expression x env) e2 (expression y env) e3 (expression z env)]
       {:result (str (:result e1) " ? " (:result e2) " : " (:result e3))
        :width (max (:width e2) (:width e3))})
     {:result (str "unknown " a) :width 99})))

(defn expression
  ([x] (expression x {}))
  ([form env]
   (cond
     (number? form) {:result form :width (inc (int (Math/floor (/ (Math/log form) (Math/log 2)))))}
     (keyword? form) {:result (str (from-intermediate form)) :width (or (env form) 32)}
     (or (vector? form) (seq? form)) (complex-expression form env)
     :else {:result "unknown" :width "unknown"}
     )
  ))


(defn build-clocks [clocks]
  (let [hm (apply hash-map (flatten clocks))
        clk (:clk hm)
        reset (:reset hm)]
    (str
      (if clk (str "   input wire " (symbol clk) ",\n") "")
      (if reset (str "   input wire " (symbol reset) ",\n") ""))))

(defn port [[dir name width]]
  (str "  " (if (= dir :in) " input wire" "output wire")
       "[" width "-1:0] " (symbol name)))

(defn build-ports [ports]
  (clojure.string/join ",\n" (map port ports)))

(defn declare-signals [clocks ports body]
  (let [output-forms (map second body)
        clock-signals (map second clocks)
        port-signals (map second ports)
        undeclared-signals
        (remove (set (concat clock-signals port-signals)) output-forms)
        forms-to-evaluate (map (fn [form] (first (filter #(= form (second %)) body))) undeclared-signals)
        env (apply hash-map (mapcat rest ports))
        widths (map #(:width (expression (last %) env)) forms-to-evaluate)
        signal-widths (zipmap undeclared-signals widths)]
    (clojure.string/join "\n"
                         (map #(str "logic [" (last %) ":0] " (symbol (first %)) ";") signal-widths))))

(defn build-element [element clocks]
  (condp = (first element)
    :register
    (str
      "always @(posedge " (symbol (:clk clocks)) ")\n"
      (if (:reset clocks)
        (str "if (" (symbol (:reset clocks)) ")\n " (symbol (second element)) " <= 0;\nelse \n")
        "")
      " " (symbol (second element)) " <= " (:result (apply expression (drop 2 element))) ";\n")
    :assign
    (str "assign " (symbol (second element)) " = " (:result (apply expression (drop 2 element))) ";\n")
    (str "unknown " (str element))))

(defn build-body [body clocks]
  (clojure.string/join "\n" (map #(build-element % clocks) body)))

(defn build [{:keys [name clocks ports body]}]
  (str "module " (symbol name) " (\n"
       (build-clocks clocks)
       (build-ports ports)
       "\n);\n\n"
       (declare-signals clocks ports body)
       "\n\n"
       (build-body body (apply hash-map (flatten clocks)))
       "\n\nendmodule\n"))


(comment

  (let [example {:name   :adder,
                 :clocks [[:clk :clk] [:reset :reset]],
                 :ports  [[:in :a 16] [:in :b 16] [:out :c 16]],
                 :body   [[:register :dout [:+ :a :b]]
                          [:assign :unused [:+ 3 17]]
                          [:assign :c [:select :dout 16 0]]]}]
    (build example))
  
  )
