(module add_multiply [[:in a 18] [:in b 18] [:in c 36] [:out result 36] [:out overflow 1]]
                     (register axb (* a b))
                     (assign sum (width 37 (+ c axb)))
                     (register overflow_d1 (select sum 36))
                     (register sum_d1 (select sum 35 0))
                     (assign result sum_d1)
                     (assign overflow overflow_d1))
