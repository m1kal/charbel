(ns charbel.synthesis_test
  (:require [clojure.test :refer :all]
            [charbel.core :refer [module module-from-string]]
            [charbel.analysis :refer :all]
            [charbel.expressions :refer :all]
            [charbel.synthesis :refer :all]))

(deftest empty-module
  (testing "Create an empty module"
    (let [result
          (build (module empty []))
          expected (str "module empty (\n   input wire clk,\n   input wire reset,\n\n);"
                        "\n\n\n\n\n\nendmodule\n")]
      (is (= result expected)))))

(deftest simple-module
  (testing "Create a basic module"
    (let [result
          (build (module adder
                         {:clocks [[clk]]}
                         [[:in a 16] [:in b 16] [:out c 16]]
                         (register dout (+ a b))
                         (assign c (select dout 16 0))))
          expected (str "module adder (\n   input wire clk,\n"
                        "   input wire [16-1:0] a,\n   input wire [16-1:0] b,\n"
                        "  output wire [16-1:0] c\n);\n"
                        "\nlogic [17-1:0] dout;\n\n"
                        "always @(posedge clk)\n dout <= (a + b);\n\n"
                        "assign c = (dout[16:0]);\n\n\nendmodule\n")]
      (is (= result expected)))))

(deftest port-detection
  (testing "Create an empty module"
    (let [result
          (build (module strange [[j 12] [:in k 1] [:out l WIDTH] [:unknown m 4] [:in n 4 5]]))
          expected (str "module strange (\n   input wire clk,\n   input wire reset,\n"
                        "   input wire [12-1:0] j,\n   input wire  k,\n  output wire [WIDTH-1:0] l,\n"
                        "  output wire [4-1:0] m,\n   input wire [-1-1:0] invalid_port"
                        "\n);"
                        "\n\n\n\n\n\nendmodule\n")]
      (is (= result expected)))))


(deftest add-multiply
  (testing "Create a simple module"
    (let [input (slurp "test-resources/add_multiply.clj")
          intermediate-form (module-from-string input)
          result (build intermediate-form)
          expected (slurp "test-resources/add_multiply.sv")]
      (is (= expected result)))))

(deftest edge-detector
  (testing "Create an edge detector"
    (let [input (slurp "test-resources/edge_detector.clj")
          intermediate-form (module-from-string input)
          result (build intermediate-form)
          expected (slurp "test-resources/edge_detector.sv")]
      (is (= expected result)))))

(deftest fsm
  (testing "Create a state machine"
    (let [input (slurp "test-resources/fsm.clj")
          intermediate-form (module-from-string input)
          result (build intermediate-form)
          expected (slurp "test-resources/fsm.sv")]
      (is (= expected result)))))

(deftest counter
  (testing "Create a parametrized counter"
    (let [input (slurp "test-resources/counter.clj")
          intermediate-form (module-from-string input)
          result (build intermediate-form)
          expected (slurp "test-resources/counter.sv")]
      (is (= expected result)))))

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
    (is (= (expression [:bit-or 0xff 0xff00]) {:result "(255 | 65280)" :width 16}))
    (is (= (expression [:bit-xor 0xff 0xff00]) {:result "(255 ^ 65280)" :width 16}))
    (is (= (expression [:bit-not 0xaaa]) {:result "(~2730)" :width 12}))
    (is (= (expression [:if [:= 1 2] [:* :a :b] [:+ :a :b]] {:a 12 :b 12})
           {:result "((1 == 2) ? (a * b) : (a + b))" :width 24}))
    (is (= (expression [:= 1 2]) {:result "(1 == 2)" :width 1}))
    (is (= (expression [:mod 32 5]) {:result "(32 % 5)" :width 3}))
    (is (= (expression [:width 21 0xcafecafe]) {:result "(3405695742)" :width 21}))
    (is (= (expression [:get :memory 321]) {:result "(memory[321])" :width 128}))

    ))
