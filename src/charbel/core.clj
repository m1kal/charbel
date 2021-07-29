(ns charbel.core
  (:require [charbel.analysis :refer :all]
             [charbel.synthesis :refer :all]))

(comment

  (build (module adder {:clk clk :reset reset} [[:in a 16] [:in b 16] [:out c 16]]
          (register dout (+ a b))
          (assign c (select dout 16 0))))

  )
