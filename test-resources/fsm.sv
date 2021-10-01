module fsm (
   input wire clk,
   input wire  next,
   input wire [16-1:0] signal,
   input wire [16-1:0] in,
  output wire [16-1:0] signal_out
);

logic [4-1:0] state;
logic [16-1:0] result;
logic [26-1:0] accum;

initial state <= 0;
always @(posedge clk)
 state <= ((((1 == signal) ? ((state + 1) % 3) : state)));

always @(*)
 if (state == 0)
  result = 0;
 else if (state == 1)
  result = (accum + in);
 else if (state == 2)
  result = in;

always @(posedge clk)
 accum <= (result);

assign signal_out = (accum[15:0]);


endmodule
