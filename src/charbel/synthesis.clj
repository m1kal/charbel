(ns charbel.synthesis
  (:require [charbel.analysis :refer :all]
            [charbel.expressions :refer :all]))

(defn build-clocks [clocks]
  (let [hm (apply hash-map (flatten clocks))
        clk (:clk hm)
        reset (:reset hm)]
    (str
      (if clk (str "   input wire " (symbol clk) ",\n") "")
      (if reset (str "   input wire " (symbol reset) ",\n") ""))))

(defn port [[dir name width]]
  (str "  " (if (= dir :in) " input wire " "output wire ")
       "[" width "-1:0] " (symbol name)))

(defn build-ports [ports]
  (clojure.string/join ",\n" (map port ports)))

(defn declare-signals [clocks ports body]
  (let [output-forms (map second (filter #(some (fn [x] (= (first %) x)) [:register :assign :cond* :declare] ) body))
        clock-signals (map second clocks)
        port-signals (map second ports)
        undeclared-signals
        (remove (set (concat clock-signals port-signals)) output-forms)
        forms-to-evaluate (map (fn [form] (first (filter #(= form (second %)) body))) undeclared-signals)
        env (apply hash-map (mapcat rest ports))
        widths (map #(:width (expression (if (= (first %) :declare) (dec (bit-shift-left 1 (last %))) (last %)) env)) forms-to-evaluate)
        signal-widths (zipmap undeclared-signals widths)]
    (clojure.string/join "\n"
                         (map #(str "logic [" (last %) "-1:0] " (symbol (first %)) ";") signal-widths))))

(defn build-element [element clocks]
  (condp = (first element)
    :cond*
    (str
      "always @(*)\n"
      (clojure.string/join "\n else"
                           (map
                             (fn [[c v]] (str " if " (:result (expression c)) "\n  "
                                              (symbol (second element)) " = " (:result (expression v))))
                             (partition 2 (drop 2 element))))
      (if (= 1 (mod (count element) 2)) (str "\n else\n " (symbol (second element)) " = " (:result (expression (last element)))) "")
      "\n")
    :register
    (str
      (if (or (not (:init (apply expression (drop 2 element)))) (:reset clocks)) "" (str "initial " (symbol (second element)) " <= " (or (:init (apply expression (drop 2 element))) "'0") ";\n"))
      "always @(posedge " (symbol (:clk clocks)) ")\n"
      (if (:reset clocks)
        (str "if (" (symbol (:reset clocks)) ")\n " (symbol (second element)) " <= " (or (:init (apply expression (drop 2 element))) "'0") ";\nelse\n")
        "")
      " " (symbol (second element)) " <= " (:result (apply expression (drop 2 element))) ";\n")
    :assign
    (str "assign " (symbol (second element)) " = " (:result (apply expression (drop 2 element))) ";\n")
    :array
    (str "logic [" (nth element 2) "-1:0][" (nth element 3) "-1:0] "  (symbol (second element)) ";\n")
    :set-if (str "always @(posedge " (symbol (:clk clocks)) ")\n if ("
                 (:result (expression (nth element 1))) ") \n  "
                 (symbol (nth element 2)) "[" (:result (expression (nth element 3)))
                 "] <= " (:result (expression (nth element 4))) ";\n")
    :instance
    (str (symbol (second element)) " " (symbol (nth element 2)) "(\n"
         (clojure.string/join ",\n" (map (fn [[k,v]] (str " ." (symbol k) "(" (symbol v) ")")) (nth element 3)))
         "\n);\n\n")
    :declare ""
    (str "unknown " (str element))
    ))

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
                          [:cond* :if_else_register [:= :x 1] 35 [:= :x 2] 36 37]
                          [:assign :c [:select :dout 16 0]]]}]
    (build example))

  (let [example {:name   :lookup,
                 :clocks [[:clk :clk]],
                 :ports  [[:in :we 1] [:in :din 32] [:in :wa 9] [:in :ra 9] [:out :res 32]],
                 :body   [[:array :mem 512 32]
                          [:register :q [:get :mem :ra]]
                          [:assign :res :q]
                          [:set-if [:= :we 1] :mem :wa :din]]}]
    (build example))


  )
