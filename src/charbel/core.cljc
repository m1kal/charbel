(ns charbel.core
  #?(:cljs (:require-macros [charbel.core]))
  (:require [charbel.analysis :as a]
            [charbel.synthesis :as s]))

(defmacro module
  "Create intermediate representation from input. See README for syntax and examples."
  [& args]
  (apply a/module* args))

(defn module-from-string [input]
  (let [[command & args] (read-string input)]
    (if (= 'module command) (apply a/module* args) "Error: not a module")))

(defn build
  "Generate SystemVerilog code based on the output of module function."
  [input]
  (s/build input))

(comment

  (build (module adder {:clocks [[clk reset] [config_clk]]} [[:in a 16] [:in b 16] [:out c 16]]
                 (register dout (+ a b))
                 (assign c (select dout 16 0))))

  (build (module adder {:clocks [[clk]]} [[:in a 16] [:out c 16]]
                 (register in_d (+ a 3))
                 (register d (width 12 (if (= in_d 0) 0 (mod (inc d) 257))))
                 (register dd (q d))
                 (let [x (inc a) y (inc x)]
                   (register z (- y x)))
                 (assign c (select dd 12 10))))

  )
