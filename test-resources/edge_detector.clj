(module edge_detector {:clocks [[clk]]} [[:in data 1] [:out rising 1]]
        (register data_d1 data)
        (register data_d2 (width 1 data_d1))
        (assign rising (bit-and data_d1 (bit-xor data_d2 1))))
