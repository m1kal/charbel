module two-port-memory (
   input wire clk1,
   input wire clk2,
   input wire [32-1:0] wraddr,
   input wire [32-1:0] din,
   input wire  we,
   input wire [32-1:0] rdaddr,
  output wire [32-1:0] q_d
);

logic [32-1:0] wraddr_d;
logic [32-1:0] din_d;
logic [32-1:0] we_d;
logic [32-1:0] rdaddr_d;
logic [32-1:0] q_internal;

always @(posedge clk1)
 wraddr_d <= wraddr;

always @(posedge clk1)
 din_d <= din;

always @(posedge clk1)
 we_d <= we;

always @(posedge clk2)
 rdaddr_d <= rdaddr;

logic [32-1:0][32-1:0] mem;

always @(posedge clk1)
 if (we_d)
  mem[wraddr_d] <= din_d;

always @(posedge clk2)
 q_internal <= ((mem[rdaddr_d]));

assign q_d = q_internal;


endmodule
