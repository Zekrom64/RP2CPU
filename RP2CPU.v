module RP2CPU(Clock, Reset, PixelOut, HSync, VSync);
	
	input Clock, Reset;
	
	output PixelOut, HSync, VSync;

	reg SystemClock;
	reg CursorClock;

	reg [31:0] SystemDivisor;
	reg [31:0] CursorDivisor;
	
	always @(posedge Clock) begin
		SystemDivisor = SystemDivisor + 1;
		if (SystemDivisor == 50) begin
			SystemDivisor = 0;
			SystemClock = !SystemClock;
		end
		CursorDivisor = CursorDivisor + 1;
		if (CursorDivisor == 25000000) begin
			CursorDivisor = 0;
			CursorClock = !CursorClock;
		end
	end

	wire [15:0] Address;
	wire [8:0] Data;
	wire [8:0] RedbusDevice;
	wire Read, Write;
	
	/*
	EloCPU cpu(
		.Address(Address),
		.Data(Data),
		.RedbusDevice(RedbusDevice),
		.Read(Read),
		.Write(Write),
		.Clock(SystemClock),
		.Reset(Reset),
		.BusRequest(0)
	);
	*/
	
	/*
	EloDiskDriveRAM diskDrive(
		.Address(Address),
		.Data(Data),
		.ReadRedbus(Read),
		.WriteRedbus(Write),
		.Enable(RedbusDevice == 2),
		
		.DriveClock(Clock)
	);
	*/
	
	wire [11:0] displayAddr;
	wire displayCursor;
	wire [7:0] displayData;
	
	Display2VGA displayVGA(
		.Clock(Clock),
		.DisplayAddr(displayAddr),
		.CursorEnable(displayCursor & CursorClock),
		.DisplayChar(displayData),
		.PixelOut(PixelOut),
		.HSync(HSync),
		.VSync(VSync)
	);
	
	EloDisplay display(
		.Address(Address),
		.Data(Data),
		.ReadRedbus(Read),
		.WriteRedbus(Write),
		.Enable(RedbusDevice == 1),
		
		.DisplayAddr(displayAddr),
		.CursorEnable(displayCursor),
		.DisplayChar(displayData),
	);

endmodule
