(module fsm {:clk clk} [[:in next 1] [:in signal 16] [:out signal_out 16]]
        (register state (width 4 (if (= 1 signal) (mod (inc state) 3) state)))
        (cond* result (= state 0) 0 (= state 1) (+ accum input) (= state 2) input)
        (register accum (width 26 result))
        (assign signal_out (select accum 15 0))
        )
