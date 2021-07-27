(ns charbel.analysis)

(defn compile* [form]
  (str form))

(defmacro compile [form]
  (compile* form))
