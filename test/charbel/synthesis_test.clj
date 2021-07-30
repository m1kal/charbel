(ns charbel.synthesis_test
  (:require [clojure.test :refer :all]
            [charbel.analysis :refer :all]
            [charbel.synthesis :refer :all]))


(deftest simple-module
  (testing "Create a basic module"
    (let [result
          (build (module adder
                         {:clk clk}
                         [[:in a 16] [:in b 16] [:out c 16]]
                         (register dout (+ a b))
                         (assign c (select dout 16 0))))
          expected (str "module adder (\n   input wire clk,\n"
                        "   input wire[16-1:0] a,\n   input wire[16-1:0] b,\n"
                        "  output wire[16-1:0] c\n);\n"
                        "\nlogic [16:0] dout;\n\n"
                        "always @(posedge clk)\n dout <= a + b;\n\n"
                        "assign c = dout[16:0];\n\n\nendmodule\n")]
      (is (= result expected)))))
