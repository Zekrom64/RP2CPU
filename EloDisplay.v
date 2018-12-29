module EloDisplay(Address, Data, ReadRedbus, WriteRedbus, Enable, CursorEnable, DisplayAddr, DisplayChar);

	input [15:0] Address;
	input [7:0] Data;
	input ReadRedbus;
	input WriteRedbus;
	input Enable;
	
	reg [7:0] kbBuffer[0:15];
	reg [7:0] registers[0:13];
	
	reg [7:0] textMem[0:4000];
	
	output CursorEnable;
	input [11:0] DisplayAddr;
	output [7:0] DisplayChar;
	assign CursorEnable = DisplayAddr == (registers[1] + (registers[2] * 80));
	assign DisplayChar = textMem[DisplayAddr];
	
	integer i;
	initial begin
		for(i=0;i<16;i=i+1) begin
			kbBuffer[i] <= 0;
		end
		for(i=0;i<14;i=i+1) begin
			registers[i] <= 0;
		end
		for(i=0;i<4000;i=i+1) begin
			textMem[i] <= 32; // Spaces
		end
		
		for(i=0;i<256;i=i+1) begin
			textMem[i] <= i;
		end
		/*
		for(i=0;i<256;i=i+1) begin
			textMem[(i%16)+((i/16)*80)] <= i;
		end
		for(i=0;i<4000;i=i+1) begin
			textMem[i] = (i / 80) + 32;
		end
		*/
	end
	
	always @(posedge WriteRedbus) begin
		registers[Address] = Data;
	end

endmodule
