(ns charbel.analysis)

(defn parse* [form]
  (cond
    (or (seq? form) (vector? form))
    (let [f (first form)
          r (rest form)]
      (if (vector? f)
        (mapv parse* form)
        (vec (cons (keyword f) (mapv parse* r)))))
    (number? form)
    form
    (map? form) (parse* (vec form))
    :else (keyword form)))

(defmacro parse [form]
  (vec (parse* form)))

(defn module* [name clocks ports & body]
  {:name (keyword name) :clocks (parse* clocks) :ports (parse* ports) :body (mapv parse* body)})

(defmacro module [& args]
  (apply module* args))

(defmacro deffn [name & args]
  (let [definition (list 'fn args)]
    (list 'def name {:f definition :src (str definition)})))

(comment

  (parse (+ 1 m))
  (parse (if (> x y) (bit-and a b) (+ 3 m)))
  (parse (let [x ^{:width 4} (+ a b)] (inc x)))
  (parse (let [x (width 4 (+ a b))] (inc x)))

  (module adder {:clk clk :reset reset} [[:in a 16] [:in b 16] [:out c 16]]
          (register dout (+ a b))
          (assign c (select dout 16 0)))

  )
