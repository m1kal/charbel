(ns charbel.synthesis
  (:require [charbel.expressions :refer [expression from-intermediate]]
            [clojure.string :as s]))

(defn signal-width [width]
  (if (= width 1) "" (str "[" (from-intermediate width) "-1:0]")))

(defn find-clock [clocks value]
  (if (number? value) [(nth clocks value [:clk])] (filter #(= (first %) value) clocks)))

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
    1 [:in (first input) 1]
    2 (cons :in input)
    3 input
    [:in :invalid_port -1]))

(defn port [[dir name width]]
  (str "  " (if (= dir :in) " input" "output") " wire " (signal-width width) " " (symbol name)))

(defn build-ports [ports]
  (s/join ",\n" (map (comp port detect-port) ports)))

(defn declare-signals [clocks ports body]
  (let [ports (mapv detect-port ports)
        body (map #(if (= :clk (first %)) (last %) %) body)
        output-forms (map second (filter #(some (fn [x] (= (first %) x)) [:register :assign :cond* :declare]) body))
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

(defmethod build-element :cond* [element _]
  (str
    "always @(*)\n"
    (s/join "\n else"
                         (map
                           (fn [[c v]] (str " if " (:result (expression c)) "\n  "
                                            (symbol (second element)) " = " (:result (expression v)) ";"))
                           (partition 2 (drop 2 element))))
    (if (= 1 (mod (count element) 2)) (str "\n else\n " (symbol (second element)) " = " (:result (expression (last element))) ";") "")
    "\n"))

(defmethod build-element :case [element _]
  (str
    "always @(*)\ncase (" (symbol (nth element 2)) ")\n"
    (s/join "\n"
                         (map
                           (fn [[c v]] (str " " (:result (expression c)) ":  "
                                            (symbol (second element)) " = " (:result (expression v)) ";"))
                           (partition 2 (drop 3 element))))
    (if (= 0 (mod (count element) 2)) (str "\n default: " (symbol (second element)) " = " (:result (expression (last element))) ";") "")
    "\nendcase\n"))

(defmethod build-element :register [element [[clock reset]]]
  (str
    (if (or (not (:init (apply expression (drop 2 element)))) reset) "" (str "initial " (symbol (second element)) " <= " (or (:init (apply expression (drop 2 element))) "'0") ";\n"))
    "always @(posedge " (symbol clock) ")\n"
    (if reset
      (str "if (" (symbol reset) ")\n " (symbol (second element)) " <= " (or (:init (apply expression (drop 2 element))) "'0") ";\nelse\n")
      "")
    " " (symbol (second element)) " <= " (:result (apply expression (drop 2 element))) ";\n"))

(defmethod build-element :assign [element _]
  (str "assign " (symbol (second element)) " = " (:result (apply expression (drop 2 element))) ";\n"))

(defmethod build-element :array [element _]
  (str "logic " (signal-width (nth element 2)) (signal-width (nth element 3)) " " (symbol (second element)) ";\n"))

(defmethod build-element :set-if [element [[clock]]]
  (str "always @(posedge " (symbol clock) ")\n if ("
       (:result (expression (nth element 1))) ")\n  "
       (symbol (nth element 2)) "[" (:result (expression (nth element 3)))
       "] <= " (:result (expression (nth element 4))) ";\n"))

(defmethod build-element :instance [element _]
  (str (symbol (second element)) " " (symbol (nth element 2)) "(\n"
       (s/join ",\n" (map (fn [[k, v]] (str " ." (symbol k) "(" (symbol v) ")")) (nth element 3)))
       "\n);\n\n"))

(defmethod build-element :clk [element clocks]
  (build-element (last element) (find-clock clocks (second element))))

(defmethod build-element :declare [element _]
  "")

(defmethod build-element :generate [element clocks]
  (let [[iter val1 val2] (second element)
        iter (from-intermediate iter)]
  (str "genvar " iter ";\ngenerate\nfor("iter"="val1";"iter"<"val2";"iter"+=1) begin\n"
       (clojure.string/join "\n" (mapv #(build-element % clocks) (drop 2 element)))
       "\nend\nendgenerate\n")))

(defmethod build-element :default [element _]
  (if (string? element) element
  (str "// unknown element:" (str element))))

(defn build-body [body clocks]
  (s/join "\n" (map #(build-element % clocks) body)))

(defn build-module
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

(defn generate-elems [input]
  (condp = (first input)
    :pipeline
    (let [[_ data num] input]
      (mapcat (fn [value] (let [name (symbol value)]
                         [[:register (str name "_d")
                         [:width (str num "-1:0][$bits(" name ")") [:vector (keyword (str name "_d")) value]]]
                         [:assign (str name "_d" num) [:width (str "$bits(" name ")") [:select (keyword (str name "_d")) (dec num)]]]]))
              data))
  [[:unknown-form]]))

(defn preprocess-module [ports body]
  (loop [ports ports output [] input body]
    (if (empty? input)
        {:ports ports :body output}
        (let [current (first input)
              command (first current)
              port (if (= :out command) (take 3 current) nil)
              elem (condp = command
                     :pipeline nil
                     :out (vec (conj (drop 3 current) (second current) :assign))
                     current)
              elems (if (or port elem) [] (generate-elems current))]
          (recur (if port (conj ports port) ports)
                 (if elem (conj output elem) (vec (concat output elems)))
                 (rest input))))))

(defn postprocess-module [sv-module]
  (let [maxdecl "`define max(a, b) ((a) > (b)) ? (a) : (b)\n\n"]
    (str "`default_nettype none\n\n"
         (if (re-find #"`max" sv-module) maxdecl "")
         sv-module
         "\n`default_nettype wire\n")))

(defn build [{:keys [name config ports body] :as input}]
  (let [{:keys [ports body]} (preprocess-module ports body)]
    (build-module (assoc input :ports ports :body body))))
