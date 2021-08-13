module add_multiply (
   input wire clk,
   input wire reset,
   input wire [18-1:0] a,
   input wire [18-1:0] b,
   input wire [36-1:0] c,
  output wire [36-1:0] result,
  output wire  overflow
);

logic [36-1:0] axb;
logic [37-1:0] sum;
logic  overflow_d1;
logic [36-1:0] sum_d1;

always @(posedge clk)
if (reset)
 axb <= '0;
else
 axb <= (a * b);

assign sum = ((c + axb));

always @(posedge clk)
if (reset)
 overflow_d1 <= '0;
else
 overflow_d1 <= (sum[36]);

always @(posedge clk)
if (reset)
 sum_d1 <= '0;
else
 sum_d1 <= (sum[35:0]);

assign result = sum_d1;

assign overflow = overflow_d1;


endmodule
