module EloCPU(Address, Data, RedbusDevice, Read, Write, Clock, Reset, BusRequest, BusRelease);

	inout [15:0] Address;
	inout [7:0] Data;
	inout [7:0] RedbusDevice;
	inout Read;
	inout Write;
	input Clock;
	input Reset;
	input BusRequest;
	output BusRelease;
	
	wire [15:0] cpuAddress;
	wire [7:0] cpuData;
	
	wire readMem, writeMem;
	
	Cpu65EL02 cpu(
		.Address(cpuAddress),
		.Data(cpuData),
		.ReadMem(readMem),
		.WriteMem(writeMem),
		.ReadRedbus(Read),
		.WriteRedbus(Write),
		.RedbusDevice(RedbusDevice),
		.BusRequest(BusRequest),
		.BusRelease(BusRelease)
	);
	
	

endmodule
