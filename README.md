# Charbel

Write synthesizable FPGA code with Clojure syntax.
SystemVerilog code is generated.

The goal of the project is to write Verilog modules with less code. With fewer lines of code a larger part of a module fits on the screen. Clojure syntax seems a perfect solution.

## Online version (using ClojureScript)

To use Charbel in the browser (without the need to install and learn Clojure), use

[https://m1kal.github.io/charbel/](https://m1kal.github.io/charbel/)

This site works on the client side - your code is not sent anywhere.

## Usage

Add dependency in project.clj:

[![Clojars Project](https://img.shields.io/clojars/v/com.github.m1kal/charbel.svg)](https://clojars.org/com.github.m1kal/charbel)

Require the library:

    (:require [charbel.core :refer :all])

Create an intermediate representation from Clojure expression:

    (def adder-module
      (module adder
        [[:in a 16] [:in b 16] [:out c 16]]
        (register dout (+ a b))
        (assign c (select dout 16 0)))

Or use input string:

    (def adder-module
      (module-from-string
        "(module sum
                 [[:in a 4] [:in b 4] [:out c 4]]
                 (assign c (+ a b)))"))

Create SystemVerilog code:

    (build adder-module)

## Usage

### Define a module

    (module <name> [configuration] <ports> <body>)

* configuration is an optional map containing parameters and clocks
  * :clocks [[\<clock1\> \<reset1\>] [\<clock2\> \<reset2\>] ...]
  * :parameters [\<name1\> \<value1\> \<name2\> \<value2\> ...]
* ports is a vector of vectors, each containing
  * optional direction: :in or :out
  * name
  * width
* body is a list of statements
  * (register \<name\> \<expression\>)
  * (assign \<name\> \<expression\>)
  * (out \<name\> \<width\> \<expression\>)
  * (cond* \<name\> \<condition1\> \<expression1\> \<condition2\> \<expression2\> ...)
  * (case \<name\> \<selector\> \<value1\> \<expression1\> \<value2\> \<expression2\> ...)
  * (instance \<module\> \<name\> ([\<port\> \<connection\>] ...))
  * (declare \<name\> \<width\>)
  * (generate [\<genvar\> \<start\> \<stop\>] expressions)

### Supported expressions

    (+ <a> <b> ...)
    (- <a> <b> ...)
    (* <a> <b> ...)
    (if <condition> <value-if-true> <value-if-false>)
    (cond <condition1> <expression1> <condition2> <expression2> ...)
    (select <signal> <msb> [<lsb>])
    (vector <a> <b> ...)
    (inc <a>)
    (dec <a>)
    (bit-and <a> <b> ...)
    (bit-or <a> <b> ...)
    (bit-xor <a> <b> ...)
    (bit-not <a>)
    (mod <a> <b>)
    (width <width> <expression>)
    (= <a> <b> ...)
    (init <initial-value> <expression>)
    (get <memory> <address>)

`width` and `init` do not modify the expression.

### Handling memories

Memory needs to be declared:

    (array <name> <word-size> <width>)

Write to an address:

    (set-if <condition> <memory-name> <address> <data>)

Read from a memory - use the expression:

    (get <memory> <address>)

### Clocks

If clocks are not provided in configuration, the default clock is called "clk", synchronous reset is called "reset".

If more than one clock is present in a module, the registers use the first clock by default. To change it, each `register` statement using non-default clock needs to be wrapped in `clk`:

    (clk <clock-name> (register <name> <expression>))

Each clock can have an associated synchronous reset signal.

### Additional information

Output ports don't need to be declared in ports section. It is enough to use `out` statement in the body.

`let` binding should work and should result in asynchronous signal.

Width calculation does not always work correctly: when using internal signals of width larger than 32 bits, some statements might need to be wrapped in `width`.

Unsigned 64-bit numbers can be used in the code. If wider "magic numbers" are required, use literals or concatenate values using `vector`.

SystemVerilog statements can be inserted in the body as strings.

If reset signal is used, all signals are reset to 0, unless `init` is provided. If reset signal is not used, `initial` block is generated to set each register to initial value (0 or value provided by `init`).

### Building SystemVerilog code

    (build <module>)
    ;or
    (build <module> :postprocess)

The latter form adds *\`default_nettype none*, and *\`max* macro if it is needed.

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

However, pull requests might be rejected. If you have an improvement idea and want your changes to be merged, please contact me first (or create a Github issue).

## License

Copyright (c) 2021 Michał Kahl

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

