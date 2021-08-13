module counter #(
  parameter WIDTH = 16
 )  (
   input wire clk_a,
   input wire  start,
   input wire  stop,
   input wire [WIDTH-1:0] limit,
   input wire  en,
  output wire [WIDTH-1:0] value
);

logic  done;
logic  started;
logic [WIDTH-1:0] cnt;

assign done = (cnt == (limit - 1));

initial started <= 0;
always @(posedge clk_a)
 started <= ((((start == 1) ? 1 : (stop ? 0 : started))));

always @(posedge clk_a)
 cnt <= (((started == 0) ? 0 : ((en == 1) ? (done ? 0 : (cnt + 1)) : cnt)));

assign value = cnt;


endmodule
