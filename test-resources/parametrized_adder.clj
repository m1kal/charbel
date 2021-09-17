(module add {:parameters [WIDTH_A 32 WIDTH_B 4 RESULT_WIDTH 33]}
        [[:in a WIDTH_A] [:in b WIDTH_B] [:out result RESULT_WIDTH]]
        (register tmp (+ a b))
        (assign result tmp))
