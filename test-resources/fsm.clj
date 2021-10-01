(module fsm {:clocks [[clk]]} [[:in next 1] [:in signal 16] [:in in 16] [:out signal_out 16]]
        (register state (init 0 (width 4 (if (= 1 signal) (mod (inc state) 3) state))))
        (cond* result (= state 0) 0 (= state 1) (+ accum in) (= state 2) in)
        (register accum (width 26 result))
        (assign signal_out (select accum 15 0)))
