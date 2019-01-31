module EloDisplay(Address, Data, ReadRedbus, WriteRedbus, Enable, DisplayClock, CursorEnable, DisplayAddr, DisplayChar, KBIn, KBWrite);

	input [15:0] Address;
	inout [7:0] Data;
	assign Data = ReadRedbus && Enable ? registers[Address] : 8'bZZZZZZZZ;
	
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
	
	input KBWrite;
	input [7:0] KBIn;
	
	always @(posedge KBWrite) begin
		
	end
	
	wire blitCmdRun;
	wor blitCmdFinish;
	
	AsyncLatch blitCmdLatch(
		.Set(WriteRedbus && Enable && Address == 7),
		.Reset(blitCmdFinish),
		.Flag(blitCmdRun)
	);
	
	input DisplayClock;
	
	always @(posedge DisplayClock) begin
		if (Enable && WriteRedbus) registers[Address] = Data;
		if (blitCmdRun) begin
			case (registers[7])
			1:;
			2:;
			3:;
			endcase
		end
	end

endmodule
