(module alu {:parameters [WIDTH 16]} [[cmd 4] [a WIDTH] [b WIDTH] [en 1]]
        (cond* result_d0
               (= cmd 0) (+ a b)
               (= cmd 1) (- a b)
               (= cmd 2) (* a b)
               (= cmd 3) (mod a b)
               (bit-or a b))
        (register result_d (width (inc WIDTH) (cond (= en 0) result_d (= 1 (select cmd 3)) (+ result_d result_d0) :else result_d0)))
        (register result_dd (if (= 1 (select en_d 0) ) result_d result_dd))
        (pipeline [en] 2)
        (out result WIDTH result_dd)
        (out ready 1 en_d2))
