(ns charbel.synthesis
  (:require [charbel.expressions :refer [expression from-intermediate]]
            [clojure.string :as s]))

(defn signal-width [width]
  (if (= width 1) "" (str "[" (from-intermediate width) "-1:0]")))

(defn build-parameter-list [parameters]
  (str " #(\n  "
       (s/join ",\n  "
                            (map (fn [[k v]]
                                   (str "parameter " (symbol k) " = " (from-intermediate v)))
                                 (partition 2 parameters)))
       "\n ) "))

(defn build-clock-inputs [clocks]
  (s/join
    (map #(str "   input wire " (symbol %) ",\n")
         (mapcat #(->> % (take 2) (filter identity)) clocks))))

(defn detect-port [input]
  (condp = (count input)
    2 (cons :in input)
    3 input
    [:in :invalid_port -1]))

(defn port [[dir name width]]
  (str "  " (if (= dir :in) " input" "output") " wire " (signal-width width) " " (symbol name)))

(defn build-ports [ports]
  (s/join ",\n" (map (comp port detect-port) ports)))

(defn declare-signals [clocks ports body]
  (let [output-forms (map second (filter #(some (fn [x] (= (first %) x)) [:register :assign :cond* :declare]) body))
        port-signals (map second ports)
        undeclared-signals
        (remove (set (concat clocks port-signals)) output-forms)
        forms-to-evaluate (map (fn [form] (first (filter #(= form (second %)) body))) undeclared-signals)
        env (apply hash-map (mapcat rest ports))
        widths (map #(:width (expression (if (= (first %) :declare) (dec (bit-shift-left 1 (last %))) (last %)) env)) forms-to-evaluate)
        signal-widths (zipmap undeclared-signals widths)]
    (s/join "\n"
                         (map #(str "logic " (signal-width (last %)) " " (symbol (first %)) ";") signal-widths))))

(defmulti build-element (fn [element clocks] (first element)))

(defmethod build-element :cond* [element clocks]
  (str
    "always @(*)\n"
    (s/join "\n else"
                         (map
                           (fn [[c v]] (str " if " (:result (expression c)) "\n  "
                                            (symbol (second element)) " = " (:result (expression v)) ";"))
                           (partition 2 (drop 2 element))))
    (if (= 1 (mod (count element) 2)) (str "\n else\n " (symbol (second element)) " = " (:result (expression (last element))) ";") "")
    "\n"))

(defmethod build-element :case [element clocks]
  (str
    "always @(*)\ncase (" (symbol (nth element 2)) ")\n"
    (s/join "\n"
                         (map
                           (fn [[c v]] (str " " (:result (expression c)) ":  "
                                            (symbol (second element)) " = " (:result (expression v)) ";"))
                           (partition 2 (drop 3 element))))
    (if (= 0 (mod (count element) 2)) (str "\n default: " (symbol (second element)) " = " (:result (expression (last element))) ";") "")
    "\nendcase\n"))


(defmethod build-element :register [element clock]
  (str
    (if (or (not (:init (apply expression (drop 2 element)))) (:reset clock)) "" (str "initial " (symbol (second element)) " <= " (or (:init (apply expression (drop 2 element))) "'0") ";\n"))
    "always @(posedge " (symbol (:clk clock)) ")\n"
    (if (:reset clock)
      (str "if (" (symbol (:reset clock)) ")\n " (symbol (second element)) " <= " (or (:init (apply expression (drop 2 element))) "'0") ";\nelse\n")
      "")
    " " (symbol (second element)) " <= " (:result (apply expression (drop 2 element))) ";\n"))

(defmethod build-element :assign [element clocks]
  (str "assign " (symbol (second element)) " = " (:result (apply expression (drop 2 element))) ";\n"))

(defmethod build-element :array [element clocks]
  (str "logic " (signal-width (nth element 2)) (signal-width (nth element 3)) " " (symbol (second element)) ";\n"))

(defmethod build-element :set-if [element clock]
  (str "always @(posedge " (symbol (:clk clock)) ")\n if ("
       (:result (expression (nth element 1))) ") \n  "
       (symbol (nth element 2)) "[" (:result (expression (nth element 3)))
       "] <= " (:result (expression (nth element 4))) ";\n"))

(defmethod build-element :instance [element clocks]
  (str (symbol (second element)) " " (symbol (nth element 2)) "(\n"
       (s/join ",\n" (map (fn [[k, v]] (str " ." (symbol k) "(" (symbol v) ")")) (nth element 3)))
       "\n);\n\n"))

(defmethod build-element :declare [element clocks]
  "")

(defmethod build-element :default [element clocks]
  (str "unknown " (str element)))

(defn build-body [body clocks]
  (s/join "\n" (map #(build-element % (zipmap [:clk :reset] (first clocks))) body)))

(defn build
  "Generate SystemVerilog code based on the output of module function."
  [{:keys [name config ports body]}]
  (str "module " (symbol name)
       (if (:parameters config) (build-parameter-list (:parameters config)))
       " (\n"
       (build-clock-inputs (:clocks config))
       (build-ports ports)
       "\n);\n\n"
       (declare-signals (flatten (:clocks config)) ports body)
       "\n\n"
       (build-body body (:clocks config))
       "\n\nendmodule\n"))