module fsm (
   input wire clk,
   input wire [1-1:0] next,
   input wire [16-1:0] signal,
  output wire [16-1:0] signal_out
);

logic [4-1:0] state;
logic [32-1:0] result;
logic [26-1:0] accum;

always @(posedge clk)
 state <= (((1 == signal) ? ((state + 1) % 3) : state));

always @(*)
 if (state == 0)
  result = 0
 else if (state == 1)
  result = (accum + input)
 else if (state == 2)
  result = input

always @(posedge clk)
 accum <= (result);

assign signal_out = (accum[15:0]);


endmodule
