module EloDiskDriveRAM(Address, Data, ReadRedbus, WriteRedbus, Enable, DriveClock);

	input [15:0] Address;
	inout [7:0] Data;
	input ReadRedbus, WriteRedbus;
	input Enable;
	
	input DriveClock;

	reg [7:0] sectorBuf[0:127];
	reg [15:0] sectorNum;
	reg [7:0] diskCommand;
	reg runDiskCommand;
	
	reg [6:0] sectorWriteOffset;
	reg [7:0] sectorWriteData;

	reg [6:0] sectorOffset;
	wire [7:0] diskOut;
	AlteraRAMBootDisk diskRAM(
		.address({sectorNum[10:0], sectorOffset}),
		.clock(DriveClock),
		.data(sectorBuf[sectorOffset]),
		.wren(diskCommand == 5),
		.q(diskOut)
	);
	reg [7:0] diskname[0:127];
	
	integer i;
	initial begin
		for(i=0;i<128;i=i+1) begin
			sectorBuf[i] <= 0;
			diskname[i] <= 0;
		end
		sectorNum <= 0;
		diskCommand <= 0;
		runDiskCommand <= 0;
		
		diskname[0] <= 8'h46;
		diskname[1] <= 8'h4F;
		diskname[2] <= 8'h52;
		diskname[3] <= 8'h54;
		diskname[4] <= 8'h48;
	end
	
	always @(negedge DriveClock) begin
		if (WriteRedbus) begin
			if (Address < 128) sectorBuf[sectorWriteOffset] = Data;
			else begin
				case(Address)
				128: sectorNum[7:0] = Data;
				129: sectorNum[15:8] = Data;
				
				endcase
			end
		end
		if (runDiskCommand) begin
			case(diskCommand)
			1: begin // Read disk name
				for(i=0;i<128;i=i+1) begin
					sectorBuf[i] <= diskname[i];
				end
				diskCommand = 0;
			end
			2: begin // Write disk name
				for(i=0;i<128;i=i+1) begin
					diskname[i] <= sectorBuf[i];
				end
				diskCommand = 0;
			end
			3: begin // Read disk serial
				for(i=0;i<128;i=i+1) begin
					sectorBuf[i] <= 0;
				end
				diskCommand = 0;
			end
			4: begin // Read disk sector
				sectorBuf[i] = diskOut;
				sectorOffset = sectorOffset + 1;
				if (sectorOffset == 0) diskCommand = 0;
			end
			5: begin // Write disk sector
				sectorOffset = sectorOffset + 1;
				if (sectorOffset == 0) diskCommand = 0;
			end
			default: begin
				diskCommand = 8'hFF;
			end
			endcase
		end
	end

endmodule
