(ns charbel.analysis_test
  (:require [clojure.test :refer :all]
            [charbel.analysis :refer :all]))

(deftest parse-expression
  (testing "Parse a Clojure expression"
    (is (= (parse (let [internal_wire (width 4 (+ a b))] (if (< internal_wire 7) (+ x 4) (inc x))))
           [:let [:internal_wire [:width 4 [:+ :a :b]]]
                 [:if [:< :internal_wire 7] [:+ :x 4] [:inc :x]]]))))
