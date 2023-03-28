(ns charbel.vhdl-synthesis
  (:require [charbel.vhdl-expressions :refer [expression from-intermediate]]
            [clojure.string :as s]))

(defn signal-width [width]
  (if (= width 1) "" (str "_vector(" (from-intermediate width) "-1 downto 0)")))

(defn find-clock [clocks value]
  (if (number? value) [(nth clocks value [:clk])] (filter #(= (first %) value) clocks)))

(defn build-parameter-list [parameters]
  (str " generic (\n  "
       (s/join ";\n  "
                            (map (fn [[k v]]
                                   (str " " (symbol k) " integer := " (from-intermediate v)))
                                 (partition 2 parameters)))
       "\n ) "))

(defn build-clock-inputs [clocks]
  (s/join
    (map #(str "   " (symbol %) ": in std_logic;\n")
         (mapcat #(->> % (take 2) (filter identity)) clocks))))

(defn detect-port [input]
  (if (keyword? input) input
    (condp = (count input)
      1 [:in (first input) 1]
      2 (cons :in input)
      3 input
      [:in :invalid_port -1])))

(defn port [input]
  (if (keyword? input)
    (str " " (from-intermediate input))
    (let [[dir name width] input]
      (str "  " (symbol name) ": " (if (= dir :in) " in" "out") " std_logic" (signal-width width)))))

(defn build-ports [ports]
  (s/join ";\n" (map (comp port detect-port) ports)))

(defn declare-signals [clocks ports body]
  (let [ports (filter coll? (mapv detect-port ports))
        body (map #(if (= :clk (first %)) (last %) %) (filter coll? body))
        output-forms (map second (filter #(some (fn [x] (= (first %) x)) [:register :assign :cond* :declare]) body))
        port-signals (map second ports)
        undeclared-signals
        (remove (set (concat clocks port-signals)) output-forms)
        forms-to-evaluate (map (fn [form] (first (filter #(= form (second %)) body))) undeclared-signals)
        env (apply hash-map (mapcat rest ports))
        widths (map #(:width (expression (if (= (first %) :declare) (dec (bit-shift-left 1 (last %))) (last %)) env)) forms-to-evaluate)
        signal-widths (zipmap undeclared-signals widths)]
    (s/join "\n"
            (map #(str "signal " (symbol (first %)) ": std_logic" (signal-width (last %)) ";") signal-widths))))

(defmulti build-element (fn [element clocks] (first element)))

(defmethod build-element :cond* [element _]
  (str
    "process(all)\nbegin\n"
    (s/join "\n else"
                         (map
                           (fn [[c v]] (str " if " (:result (expression c)) "then \n  "
                                            (symbol (second element)) " <= " (:result (expression v)) ";"))
                           (partition 2 (drop 2 element))))
    (if (= 1 (mod (count element) 2)) (str "\n else\n " (symbol (second element)) " <= " (:result (expression (last element))) ";") "")
    "\nend process;\n"))

(defmethod build-element :case [element _]
  (str
    "process(all)\nbegin\ncase (" (symbol (nth element 2)) ") is\n"
    (s/join "\n"
                         (map
                           (fn [[c v]] (str " when " (:result (expression c)) " =>  "
                                            (symbol (second element)) " = " (:result (expression v)) ";"))
                           (partition 2 (drop 3 element))))
    (if (= 0 (mod (count element) 2)) (str "\n when others => " (symbol (second element)) " = " (:result (expression (last element))) ";") "")
    "\nend case;\nend process;\n"))

(defmethod build-element :register [element [[clock reset]]]
  (str
    (if (or (not (:init (apply expression (drop 2 element)))) reset) "" (str "process\nbegin\n " (symbol (second element)) " <= " (or (:init (apply expression (drop 2 element))) "'0") ";wait;\nend process;\n"))
    "process(" (symbol clock) ")\nbegin\n if rising_edge(" (symbol clock) ") then\n"
    (if reset
      (str "if (" (symbol reset) " = '1') then \n " (symbol (second element)) " <= " (or (:init (apply expression (drop 2 element))) "(others => '0')") ";\nelse\n")
      "")
    " " (symbol (second element)) " <= " (:result (apply expression (drop 2 element))) (if reset ";\nend if" "") ";\nend if;\nend process;\n"))

(defmethod build-element :assign [element _]
  (str " " (symbol (second element)) " <= " (:result (apply expression (drop 2 element))) ";\n"))

(defmethod build-element :array [element _]
  (str "logic " (signal-width (nth element 2)) (signal-width (nth element 3)) " " (symbol (second element)) ";\n"))

(defmethod build-element :set-if [element [[clock]]]
  (str "process(" (symbol clock) ")\nbegin\n if rising_edge(" (symbol clock) ") then\nbegin\n if ("
       (:result (expression (nth element 1))) ") then\n  "
       (symbol (nth element 2)) "[" (:result (expression (nth element 3)))
       "] <= " (:result (expression (nth element 4))) ";\nend if;\nend if;\nend process;"))

(defmethod build-element :instance [element _]
  (str (symbol (nth element 2)) ": entity work." (symbol (second element)) "port map(\n"
       (s/join ",\n" (map (fn [[k, v]] (str  (symbol k) "=> " (symbol v) )) (nth element 3)))
       "\n);\n\n"))

(defmethod build-element :clk [element clocks]
  (build-element (last element) (find-clock clocks (second element))))

(defmethod build-element :declare [element _]
  "")

(defmethod build-element :generate [element clocks]
  (let [[iter val1 val2] (second element)
        iter (from-intermediate iter)]
  (str "for "iter" in "val1" to "val2" generate\n"
       (clojure.string/join "\n" (mapv #(build-element % clocks) (drop 2 element)))
       "\nend generate;\n")))

(defmethod build-element :default [element _]
  (if (string? element) element
    (str "-- unknown element:  " (str element))))

(defn build-body [body clocks]
  (s/join "\n" (map #(if (coll? %) (build-element % clocks) (str (from-intermediate %))) body)))

(defn build-module
  [{:keys [name config ports body]}]
  (str "entity " (symbol name) " is\n"
       (if (:parameters config) (build-parameter-list (:parameters config)))
       " port (\n"
       (build-clock-inputs (:clocks config))
       (build-ports ports)
       "\n);\nend " (symbol name) ";\n\narchitecture arch of " (symbol name) " is\n"
       (declare-signals (flatten (:clocks config)) ports body)
       "\n\nbegin\n"
       (build-body body (:clocks config))
       "\n\nend arch;\n"))

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
              command (if (coll? current) (first current) :nop)
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
  (str "library ieee;\n\nuse ieee.std_logic_1164.all;\nuse ieee.numeric_std.all;\n\n" sv-module))

(defn build [{:keys [name config ports body] :as input}]
  (let [{:keys [ports body]} (preprocess-module ports body)]
    (build-module (assoc input :ports ports :body body))))
