(module two-port-memory {:clocks [[clk1] [clk2]]}
        [[wraddr 32] [din 32] [we 1] [rdaddr 32]]
        (register wraddr_d wraddr)
        (register din_d din)
        (register we_d we)
        (clk clk2 (register rdaddr_d rdaddr))
        (array mem 32 32)
        (set-if we_d mem wraddr_d din_d)
        (clk clk2 (register q_internal (width 32 (get mem rdaddr_d))))
        (out q_d 32 q_internal))
