(module counter
        {:parameters [WIDTH 16] :clocks [[clk_a]]}
        [[:in start 1] [:in stop 1] [:in limit WIDTH] [:in en 1] [:out value WIDTH]]
        (assign done (= cnt (dec limit)))
        (register started (init 0 (width 1 (if (= start 1) 1 (if stop 0 started)))))
        (register cnt (width WIDTH (if (= started 0) 0 (if (= en 1) (if done 0 (inc cnt)) cnt))))
        (assign value cnt))
