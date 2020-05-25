module Cpu65EL02(
	Data, Address,
	ReadMem, WriteMem,
	ReadRedbus, WriteRedbus, RedbusDevice,
	BusRequest, BusRelease,
	
	Reset, Clock);
	
	
	///////////////////
	// Bus Registers //
	///////////////////
	
	reg [7:0] DataOut;
	
	reg [15:0] addressOut;
	reg readAddress, writeAddress;
	
	reg redbusEnable, redbusWindowEnable;
	reg [15:0] redbusBase, redbusWindow;
	reg [7:0] redbusAccessDevice;

	///////////////////
	// CPU Registers //
	///////////////////
	
	reg [3:0] insnPhase;
	reg [7:0] insnOpcode;
	
	reg [15:0] insnAddress;
	reg [15:0] insnIndirect;
	
	reg [15:0] insnValueIn;
	reg [15:0] insnValueOut;
	
	reg [15:0] SP,PC,A,X,Y,R,I,D;
	reg [15:0] addressBRK, addressPOR;
	
	reg flagCarry, flagZero, flagInterrupt, flagDecimal, flagIndexSize, flagMemorySize, flagOverflow, flagNegative;
	
	///////////////////
	// Bus Mastering //
	///////////////////
	input BusRequest;
	output reg BusRelease;
	wire redbusValid;
	wire redbusAccess;
	wire redbusAddress;
	
	//////////////
	// Data Bus //
	//////////////
	inout [7:0] Data;
	wire [7:0] DataIn;
	assign Data = (writeAddress & !BusRelease) ? DataOut : 8'bZZZZZZZZ;
	assign DataIn = (redbusAccess & !redbusWindowEnable) ? 0 : Data;
	
	/////////////////
	// Address Bus //
	/////////////////
	output [15:0] Address;
	output ReadMem, WriteMem, ReadRedbus, WriteRedbus;
	
	assign Address = BusRelease ? 16'bZZZZZZZZZZZZZZZZ : (redbusValid ? redbusAddress : addressOut);
	
	////////////////////
	// Redbus Control //
	////////////////////
	output [7:0] RedbusDevice;
	
	assign RedbusDevice = BusRelease ? 8'bZZZZZZZZ : redbusAccessDevice;
	
	AddressWindow redbusAddressWindow(
		.Address(addressOut),
		.Base(redbusBase),
		.Head(redbusBase + 256),
		.Valid(redbusValid),
		.Offset(redbusAddress)
	);
	assign redbusAccess = redbusValid & redbusEnable;
	assign ReadMem = readAddress & !redbusAccess;
	assign WriteMem = writeAddress & !redbusAccess;
	assign ReadRedbus = readAddress & redbusAccess;
	assign WriteRedbus = writeAddress & redbusAccess;

	//////////////////////////////
	// CPU Configuration Inputs //
	//////////////////////////////
	input [7:0] ConfigCPUDevice;
	input [7:0] ConfigDriveDevice;
	input [7:0] ConfigTermDevice;
	
	////////////////////////////
	// Miscellaneous Controls //
	////////////////////////////
	input Reset, Clock;
	
	///////////////
	// CPU Tasks //
	///////////////
	
	// Resets the CPU completely
	task resetCPU();
		begin
			// Clear registers
			SP = 0;
			PC = 0;
			A = 0;
			X = 0;
			Y = 0;
			R = 0;
			I = 0;
			D = 0;
			flagCarry = 0;
			flagZero = 0;
			flagInterrupt = 0;
			flagDecimal = 0;
			flagIndexSize = 0;
			flagMemorySize = 0;
			flagOverflow = 0;
			flagNegative = 0;
			
			// Set MMU addresses
			addressPOR = 0;
			addressBRK = 0;
			
			// Reset instruction decoding
			insnPhase = 0;
			
			// Clear bus controls
			BusRelease = 0;
			readAddress = 0;
			writeAddress = 0;
			
			// Reset Redbus controls
			redbusEnable = 0;
			redbusWindowEnable = 0;
			redbusBase = 0;
			redbusWindow = 0;
			redbusAccessDevice = 0;
		end
	endtask
	
	/* Cold boots the CPU, loading registers with initial values.
	 *
	 * Note: Cold booting will also load initial instructions into
	 *   memory, this is not the CPU's job, this only loads registers.
	 */
	task coldBootCPU();
		begin
			addressPOR = 8192;
			addressBRK = 8192;
			SP = 512;
			PC = 1024;
			R = 768;
			A = 0;
			X = 0;
			Y = 0;
			D = 0;
			flagCarry = 0;
			flagZero = 0;
			flagDecimal = 0;
			flagOverflow = 0;
			flagNegative = 0;
			
			flagIndexSize = 1;
			flagMemorySize = 1;
			flagInterrupt = 1;
		end
	endtask
	
	
	//////////////////////////
	// CPU Reset Operations //
	//////////////////////////
	initial begin
		resetCPU();
	end
	
	
	////////////////
	// MMU Opcode //
	////////////////
	task mmuOp;
		input [7:0] opcode;
		begin
			case(opcode)
			8'h00: redbusAccessDevice = A[7:0];
			8'h80: A = {8'h00, redbusAccessDevice};
			8'h01: redbusBase = A;
			8'h81: A = redbusBase;
			8'h02: redbusEnable = 1;
			8'h82: redbusEnable = 0;
			8'h03: redbusWindow = A;
			8'h83: A = redbusWindow;
			8'h04: redbusWindowEnable = 1;
			8'h84: redbusWindowEnable = 0;
			8'h05: addressBRK = A;
			8'h85: A = addressBRK;
			8'h06: addressPOR = A;
			8'h86: A = addressPOR;
			endcase
		end
	endtask
	
	////////////////////////
	// Instruction Decode //
	////////////////////////
	
	
	/* Instruction decode happens over several phases:
	 * 
	 * 0 - Instruction fetch / Bus Control
	 * 1 - Read address LSB
	 * 2 - Read address MSB
	 * 3 - Read indirect LSB
	 * 4 - Read indirect MSB
	 * 5 - Read value LSB
	 * 6 - Read value MSB
	 * 7 - Write value LSB
	 * 8 - Write value MSB
	 * 9 - Extended value write
	 *
	 * The initial instruction is decoded into a microcode word which
	 * directs the Address and Data sequencers. The sequencers control
	 * the stepping of the instruction through the phases. Once both
	 * sequencers are finished, the next instruction is fetched.
	 */
	 
	/* The address sequencer controls the generation of addresses for
	 * memory access. This controls the different addressing modes and
	 * special instruction addressing (eg. NXA)
	 */
	 
	/* The data sequencer controls the flow of data through the different
	 * components of the CPU (ALU, registers, MMU, bus I/O). It controls,
	 * with the direction of the uCode word, what the instruction does.
	 */
	
	///////////////////////
	// Clock Transitions //
	///////////////////////
	
	// Clock positive edge
	// Used to prepare bus and set control signals
	always @(posedge Clock or posedge Reset) begin
		if (Reset) begin
			resetCPU();
		end else if (!BusRelease) begin
			case(insnPhase)
			0: begin
				if (BusRequest) begin // Release bus control
					BusRelease = 1;
				end else begin // Instruction fetch: Prepare bus
					addressOut = PC;
					readAddress = 1;
					writeAddress = 0;
					PC = PC + 1;
				end
			end
			1: begin // Read address LSB
				
			end
			2: begin // Read address MSB
			
			end
			3: begin // Read indirect LSB
			
			end
			4: begin // Read indirect MSB
			
			end
			5: begin // Read value LSB
			
			end
			6: begin // Read value MSB
			
			end
			7: begin // Write value LSB
			
			end
			8: begin // Write value MSB
			
			end
			9: begin // Extended write
			
			end
			endcase
		end
	end
	
	// Clock negative edge
	// Used to access bus and execute operations
	always @(negedge Clock) begin
		if (!BusRelease) begin
			case(insnPhase)
			0: begin // Instruction fetch: Load instruction
				insnOpcode = DataIn;
				insnAddress[15:0] = 0;
				insnIndirect[15:0] = 0;
				insnValueIn[15:0] = 0;
			end
			1: begin // Read address LSB
				insnAddress[7:0] = DataIn;
			end
			2: begin // Read address MSB
				insnAddress[15:8] = DataIn;
			end
			3: begin // Read indirect LSB
				insnIndirect[7:0] = DataIn;
			end
			4: begin // Read indirect MSB
				insnIndirect[15:8] = DataIn;
			end
			5: begin // Read value LSB
				insnValueIn[7:0] = DataIn;
			end
			6: begin // Read value MSB
				insnValueIn[15:8] = DataIn;
			end
			7: begin // Write value LSB
				DataOut = insnValueOut[7:0];
			end
			8: begin // Write value MSB
				DataOut = insnValueOut[15:8];
			end
			9: begin // Extended value write
				DataOut = insnValueOut[7:0];
			end
			endcase
		end
		// Deassert bus release if it is no longer requested
		if (!BusRequest) begin
			BusRelease = 0;
		end
	end
	
endmodule
