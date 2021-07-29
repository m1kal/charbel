(ns charbel.synthesis
  (:require [charbel.analysis :refer :all]))

(defn expression [[a & b]]
  (condp = a
    :select (str (symbol (first b)) "[" (second b) (if (= 2 (count b)) "]" (str ":" (last b) "]")))
    :+ (str (symbol (first b)) " + " (symbol (second b)))
    (str "unknown " a)))

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
        (remove (set (concat clock-signals port-signals)) output-forms)]
    (clojure.string/join "\n" (map #(str "logic [31:0] " (symbol %) ";") undeclared-signals))))

(defn build-element [element clocks]
  (condp = (first element)
    :register
    (str
      "always @(posedge " (symbol (:clk clocks)) ")\n"
      (if (:reset clocks)
        (str "if (" (symbol (:reset clocks)) ")\n " (symbol (second element)) " <= 0;\nelse \n")
        "")
      " " (symbol (second element)) " <= " (apply expression (drop 2 element)) ";\n")
    :assign
    (str "assign " (symbol (second element)) " = " (apply expression (drop 2 element)) ";\n")
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

  (remove (set (concat '(:a :b :c) '(:clk :reset))) [:dout :c])

  (let [example {:name   :adder,
                 :clocks [[:clk :clk] [:reset :reset]],
                 :ports  [[:in :a 16] [:in :b 16] [:out :c 16]],
                 :body   [[:register :dout [:+ :a :b]] [:assign :c [:select :dout 16 0]]]}]

    (build example)
    )

  )
