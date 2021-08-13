module edge_detector (
   input wire clk,
   input wire  data,
  output wire  rising
);

logic  data_d1;
logic  data_d2;

always @(posedge clk)
 data_d1 <= data;

always @(posedge clk)
 data_d2 <= (data_d1);

assign rising = (data_d1 & (data_d2 ^ 1));


endmodule
