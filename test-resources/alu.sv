module alu #(
  parameter WIDTH = 16
 )  (
   input wire clk,
   input wire reset,
   input wire [4-1:0] cmd,
   input wire [WIDTH-1:0] a,
   input wire [WIDTH-1:0] b,
   input wire  en,
  output wire [WIDTH-1:0] result,
  output wire  ready
);

logic [WIDTH-1:0] result_d0;
logic [(WIDTH + 1)-1:0] result_d;
logic [32-1:0] result_dd;
logic [2-1:0][$bits(en)-1:0] en_d;
logic [$bits(en)-1:0] en_d2;

always @(*)
 if (cmd == 0)
  result_d0 = (a + b);
 else if (cmd == 1)
  result_d0 = (a - b);
 else if (cmd == 2)
  result_d0 = (a * b);
 else if (cmd == 3)
  result_d0 = (a % b);
 else
 result_d0 = (a | b);

always @(posedge clk)
if (reset)
 result_d <= '0;
else
 result_d <= (((en == 0) ? result_d : ((1 == (cmd[3])) ? (result_d + result_d0) : (result_d0))));

always @(posedge clk)
if (reset)
 result_dd <= '0;
else
 result_dd <= ((1 == (en_d[0])) ? result_d : result_dd);

always @(posedge clk)
if (reset)
 en_d <= '0;
else
 en_d <= (({en_d, en}));

assign en_d2 = ((en_d[1]));

assign result = result_dd;

assign ready = en_d2;


endmodule
