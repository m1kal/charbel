(ns charbel.analysis)

(defn parse* [form]
  (cond
    (or (seq? form) (vector? form))
    (let [f (first form)
          r (rest form)]
      (cond
        (nil? f) []
        (vector? f) (mapv parse* form)
        (= f 'let) (mapv parse* (cons 'do (concat (map #(cons 'assign %) (partition 2 (first r))) (rest r))))
        :else (vec (cons (keyword f) (mapv parse* r)))))
    (number? form)
    form
    (map? form) (parse* (vec form))
    :else (keyword form)))

(defmacro parse [form]
  (vec (parse* form)))

(defn postprocess [forms]
  (reduce (fn [acc elem] (if (= (first elem) :do) (vec (concat acc (rest elem))) (conj acc elem))) [] forms))

(defn module* [name & args]
  (let [[clocks ports body] (if (map? (first args))
                              [(first args) (second args) (drop 2 args)]
                              [{:clk 'clk :reset 'reset} (first args) (rest args)])]
    {:name   (keyword name)
     :clocks (parse* clocks)
     :ports  (parse* ports)
     :body   (postprocess (mapv parse* body))}))

(defmacro module [& args]
  (apply module* args))

(defmacro deffn [name & args]
  (let [definition (list 'fn args)]
    (list 'def name {:f definition :src (str definition)})))

(defn module-from-string [input]
  (let [[command & args ] (read-string input)]
    (if (= 'module command) (apply module* args) "Error: not a module")))

(comment

  (parse (+ 1 m))
  (parse (if (> x y) (bit-and a b) (+ 3 m)))
  (parse (let [x ^{:width 4} (+ a b)] (inc x)))
  (parse (let [x (width 4 (+ a b))] (inc x)))

  (module adder {:clk clk :reset reset} [[:in a 16] [:in b 16] [:out c 16]]
          (register dout (+ a b))
          (assign c (select dout 16 0)))

  (module adder [[:in a 16] [:in b 16] [:out c 16]]
          (register dout (+ a b))
          (assign c (select dout 16 0)))

  )
