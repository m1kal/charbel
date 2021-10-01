`default_nettype none

`define max(a, b) ((a) > (b)) ? (a) : (b)

module add #(
  parameter WIDTH_A = 32,
  parameter WIDTH_B = 4,
  parameter RESULT_WIDTH = 33
 )  (
   input wire clk,
   input wire reset,
   input wire [WIDTH_A-1:0] a,
   input wire [WIDTH_B-1:0] b,
  output wire [RESULT_WIDTH-1:0] result
);

logic [`max(WIDTH_A, WIDTH_B) + 1-1:0] tmp;

always @(posedge clk)
if (reset)
 tmp <= '0;
else
 tmp <= (a + b);

assign result = tmp;


endmodule

`default_nettype wire
