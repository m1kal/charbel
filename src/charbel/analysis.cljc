(ns charbel.analysis)

(declare parse*)

(defn parse-seq [[f & r :as form]]
  (cond
    (nil? f) []
    (vector? f) (mapv parse* form)
    (= f 'let) (mapv parse* (cons 'do (concat (map #(cons 'assign %) (partition 2 (first r))) (rest r))))
    :else (vec (cons (keyword f) (mapv parse* r)))))

(defn parse* [form]
  (cond
    (or (seq? form) (vector? form)) (parse-seq form)
    (number? form) form
    (map? form) (apply hash-map (parse* (apply concat (vec form))))
    :else (keyword form)))

(defmacro parse [form]
  (vec (parse* form)))

(defn postprocess [forms]
  (reduce (fn [acc elem] (if (= (first elem) :do) (vec (concat acc (rest elem))) (conj acc elem))) [] forms))

(defn module* [name & args]
  (let [[config ports body] (if (map? (first args))
                              [(into {:clocks [['clk 'reset]]} (first args)) (second args) (drop 2 args)]
                              [{:clocks [['clk 'reset]]} (first args) (rest args)])]
    {:name   (keyword name)
     :config (parse* config)
     :ports  (parse* ports)
     :body   (postprocess (mapv parse* body))}))

