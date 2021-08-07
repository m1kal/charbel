module edge_detector (
   input wire clk,
   input wire [1-1:0] data,
  output wire [1-1:0] rising
);

logic [1-1:0] data_d1;
logic [1-1:0] data_d2;

always @(posedge clk)
 data_d1 <= data;

always @(posedge clk)
 data_d2 <= (data_d1);

assign rising = (data_d1 & (data_d2 ^ 1));


endmodule
