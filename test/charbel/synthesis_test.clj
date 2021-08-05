(ns charbel.synthesis_test
  (:require [clojure.test :refer :all]
            [charbel.analysis :refer :all]
            [charbel.expressions :refer :all]
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
                        "\nlogic [17-1:0] dout;\n\n"
                        "always @(posedge clk)\n dout <= (a + b);\n\n"
                        "assign c = (dout[16:0]);\n\n\nendmodule\n")]
      (is (= result expected)))))

(deftest expression-test
  (testing "Building expressions"
    (is (= (expression 0) {:result 0 :width 1}))
    (is (= (expression 1) {:result 1 :width 1}))
    (is (= (expression 256) {:result 256 :width 9}))
    (is (= (expression 511) {:result 511 :width 9}))

    (is (= (expression [:+ 2 2]) {:result "(2 + 2)" :width 3}))
    (is (= (expression [:+ 4 :a]) {:result "(4 + a)" :width 33}))
    (is (= (expression [:+ 4 :a] {:a 18}) {:result "(4 + a)" :width 19}))

    (is (= (expression [:select :a 4]) {:result "(a[4])" :width 1}))
    (is (= (expression [:select :a 4 1]) {:result "(a[4:1])" :width 4}))

    (is (= (expression [:* 4 :a] {:a 6}) {:result "(4 * a)" :width 9}))

    (is (= (expression [:inc :a] {:a 6}) {:result "(a + 1)" :width 7}))
    (is (= (expression [:dec :a] {:a 6}) {:result "(a - 1)" :width 6}))
    (is (= (expression [:bit-and 0xff 0xff00]) {:result "(255 & 65280)" :width 16}))
    (is (= (expression [:bit-or 0xff 0xff00]) {:result  "(255 | 65280)" :width 16}))
    (is (= (expression [:bit-xor 0xff 0xff00]) {:result "(255 ^ 65280)" :width 16}))
    (is (= (expression [:if [:= 1 2] [:* :a :b] [:+ :a :b]] {:a 12 :b 12})
           {:result "((1 == 2) ? (a * b) : (a + b))" :width 24}))
    (is (= (expression [:= 1 2]) {:result "(1 == 2)" :width 1}))
    (is (= (expression [:mod 32 5]) {:result "(32 % 5)" :width 3}))
    (is (= (expression [:width 21 0xcafecafe]) {:result "(3405695742)" :width 21}))
    (is (= (expression [:get :memory 321]) {:result "(memory[321])" :width 128}))

    ))
