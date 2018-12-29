module EloIOExpander(Address, Data, ReadRedbus, WriteRedbus, Enable, IOPort);

	input [15:0] Address;
	inout [7:0] Data;
	input Enable;
	input ReadRedbus;
	input WriteRedbus;
	
	reg [7:0] DataOut;
	assign Data = (Enable & ReadRedbus) ? DataOut : 8'bZZZZZZZZ;
	
	inout [15:0] IOPort;
	reg [15:0] outputs;
	
	genvar i;
	generate
		for(i = 0; i < 16; i=i+1) begin : genIOOutputs
			assign IOPort[i] = outputs[i] ? 1'b1 : 1'bZ;
		end
	endgenerate
	
	initial begin
		outputs <= 0;
	end
	
	/////////////
	// Read IO //
	/////////////
	always @(posedge ReadRedbus) begin
		case(Address)
		0: DataOut = IOPort[7:0];
		1: DataOut = IOPort[15:8];
		default: DataOut = 0;
		endcase
	end
	
	//////////////
	// Write IO //
	//////////////
	always @(posedge WriteRedbus) begin
		if (Enable) begin
			case(Address)
			2: outputs[7:0] = Data;
			3: outputs[15:8] = Data;
			endcase
		end
	end
	
endmodule
