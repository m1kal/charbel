# Charbel

Write synthesizable FPGA code with Clojure syntax.
SystemVerilog code is generated.

## Usage

Create an intermediate representation from Clojure expression:

    (def adder-module (module adder
            [[:in a 16] [:in b 16] [:out c 16]]
            (register dout (+ a b))
            (assign c (select dout 16 0)))

Create Verilog code:

    (build adder-module)

Functions to create and assign signals:
* register name expression
* assign name expression

Supported expressions:
(+ a b), (* a b), (if a b c), (select a position),
(inc a), (dec a), (bit-and a b), (bit-xor a b),
(bit-or a b), (= a b), (width w a), (mod a b)

Clocks and resets can be declared explicitly as the first
argument to `module`. If the argument is not provided,
clock is called "clk", synchronous reset is called "reset".

## Examples

    (build
      (module lookup
              {:clocks [[clk]]}
              [[:in datain 32] [:in we 1], [:in address 32] [:out dataout 32]]
              (array mem 32 32)
              (set-if (= we 1) mem address datain)
              (register q (get mem address))
              (assign dataout q)))

results in the following SystemVerilog code:

    module lookup (
        input wire clk,
        input wire[32-1:0] datain,
        input wire we,
        input wire[32-1:0] address,
       output wire[32-1:0] dataout
    );
    
    logic [128-1:0] q;
    
    logic [32-1:0][32-1:0] mem;
    
    always @(posedge clk)
     if ((we == 1))
      mem[address] <= datain;
    
    always @(posedge clk)
     q <= (mem[address]);
    
    assign dataout = q;

    endmodule

Create a parametrized counter:

    (module counter
      {:parameters [WIDTH 16] :clocks [[clk_a]]}
      [[:in start 1] [:in stop 1] [:in limit WIDTH] [:in en 1] [:out value WIDTH]]
      (assign done (= cnt (dec limit)))
      (register started
                (init 0
                  (width 1
                    (if (= start 1)
                        1
                        (if stop 0 started)))))
      (register cnt
                (width WIDTH
                (if (= started 0)
                    0
                    (if (= en 1)
                        (if done 0 (inc cnt))
                        cnt))))
      (assign value cnt))

Create a state machine:

    (module fsm
      {:clocks [[clk]]}
      [[:in next 1] [:in signal 16] [:out signal_out 16]]
      (register state (init 0 (width 4 (if (= 1 signal) (mod (inc state) 3) state))))
      (cond* result (= state 0) 0 (= state 1) (+ accum input) (= state 2) input)
      (register accum (width 26 result))
      (assign signal_out (select accum 15 0)))

See `test-resources` directory for more examples.

## Contact

If you want to contribute, request features, discuss or comment the project, send an e-mail to [charbel-support@protonmail.com](mailto:charbel-support@protonmail.com).

# Contributing

So far this has been one-person project. If you have an idea to improve the project, feel free to fork it.

However, pull requests might be rejected. If you have an improvement idea and want your changes to be merged, please contact me first (or create an issue).

## License

Copyright (c) 2021 Michał Kahl

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 
