module Cpu65EL02AddrSeq();

	input [3:0] Sequence;
	input [4:0] Mode;
	input DataSize;
	
	input [15:0] RegInPC, RegInX, RegInY, RegInI, RegInS, RegInR;
	input [15:0] RegInsnAddr, RegInsnIndr;
	
	output [15:0] RegOutPC, RegOutI, RegOutS, RegOutR;
	output RegWrPC, RegWrI, RegWrS, RegWrR;
	
	output [15:0] Address;
	
	`define SEQ(NextSeq,Addrsel,IncDec,ToAdd,MaskAdd,MaskOut,Extend)
	
	// No addressing is done, operation driven by data sequencer
	localparam MODE_NONE = 0;
	// Standard addressing modes
	localparam MODE_IMMEDIATE = 1;
	localparam MODE_ZEROPAGE = 2;
	localparam MODE_ZEROPAGEX = 3;
	localparam MODE_ZEROPAGEY = 4;
	localparam MODE_ABSOLUTE = 5;
	localparam MODE_ABSOLUTEX = 6;
	localparam MODE_ABSOLUTEY = 7;
	localparam MODE_INDIRECT = 8;
	localparam MODE_INDIRECTX = 9;
	localparam MODE_INDIRECTY = 10;
	localparam MODE_SSTACK = 11;
	localparam MODE_SSTACKINDR = 12;
	localparam MODE_RSTACK = 13;
	localparam MODE_RSTACKINDR = 14;
	
	// Special addressing modes, used for few instructions
	localparam MODE_NXA_NXT = 16;
	localparam MODE_BRK = 17;
	localparam MODE_PHZ = 18;
	localparam MODE_PLZ = 19;
	localparam MODE_RHZ = 20;
	localparam MODE_RLZ = 21;
	localparam MODE_ABSOLUTEXINDR = 22;
	localparam MODE_ENT_REA = 23;
	localparam MODE_PEA = 24;
	localparam MODE_PEI = 25;
	localparam MODE_PER = 26;
	
	// Sequence indices
	localparam SEQ_RINSN = 4'd0;
	localparam SEQ_RADDRL = 4'd1;
	localparam SEQ_RADDRH = 4'd2;
	localparam SEQ_RINDRL = 4'd3;
	localparam SEQ_RINDRH = 4'd4;
	localparam SEQ_RVALL = 4'd5;
	localparam SEQ_RVALH = 4'd6;
	localparam SEQ_WVALL = 4'd7;
	localparam SEQ_WVALH = 4'd8;
	localparam SEQ_WVALX = 4'd9;
	localparam SEQ_END = 4'd0;
	localparam SEQ_DATA = 4'd15;
	
	// Address register selectors
	localparam ADDRSEL_NONE = 3'd0; // Selection is ignored, used during instruction fetch
	localparam ADDRSEL_PC = 3'd0;   // Selects the program counter
	localparam ADD_NONE = 3'd0;     // Selects none as the register to add
	localparam ADDRSEL_X = 3'd1;    // Selects X
	localparam ADDRSEL_Y = 3'd2;    // Selects Y
	localparam ADDRSEL_I = 3'd3;    // Selects I
	localparam ADDRSEL_ADDR = 3'd4; // Selects the "address" addressing register, holds an immediate address in the instruction
	localparam ADDRSEL_INDR = 3'd5; // Selects the "indirect" addressing register, holds an indirected address from the instruction
	localparam ADDRSEL_S = 3'd6;    // Selects S
	localparam ADDRSEL_R = 3'd7;    // Selects R
	
	// Increment/Decrement modes
	localparam INCDEC_NONE = 2'd0;   // No operation is done
	localparam INCDEC_ADDONE = 2'd1; // 1 is added to the effective address
	localparam INCDEC_PREDEC = 2'd2; // The address register is decremented first
	localparam INCDEC_INC = 2'd3;    // The address register is incremented after the sequence step
	
	// Address masking modes
	localparam MASK_NONE = 2'd0; // No masking is done
	localparam MASK_SIZE = 2'd1; // The address is masked to the operand register size
	localparam MASK_ADDI = 2'd2; // The register to add is masked to the operand size
	
	// True/False values
	localparam NO = 1'b0;
	localparam YES = 1'b1;
	
	function [14:0] SequenceRom;
		input [4:0] Mod;
		begin
			case(Mod)
			// None
			MODE_NONE: SequenceRom = 0;
			// Immediate
			MODE_IMMEDIATE: begin
				case(Sequence)           // Next seq.   Addr. select  Inc/Dec mode   Addr. add      Mask     Extend
				SEQ_RINSN:  SequenceRom = { SEQ_DATA,   ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				default: SequenceRom = 0;
				endcase
			end
			// Zero Page
			MODE_ZEROPAGE: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_DATA,   ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				default: SequenceRom = 0;
				endcase
			end
			// Zero Page, X
			MODE_ZEROPAGEX: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_DATA,   ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADDRSEL_X,    MASK_SIZE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADDRSEL_X,    MASK_SIZE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADDRSEL_X,    MASK_SIZE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADDRSEL_X,    MASK_SIZE, NO  };
				default: SequenceRom = 0;
				endcase
			end
			// Zero Page, Y
			MODE_ZEROPAGEX: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_DATA,   ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_SIZE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADDRSEL_Y,    MASK_SIZE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_SIZE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADDRSEL_Y,    MASK_SIZE, NO  };
				default: SequenceRom = 0;
				endcase
			end
			// Absolute
			MODE_ABSOLUTE: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_RADDRH, ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRH: SequenceRom = { SEQ_DATA,   ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// Absolute, X
			MODE_ABSOLUTEX: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_RADDRH, ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRH: SequenceRom = { SEQ_DATA,   ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADDRSEL_X,    MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADDRSEL_X,    MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADDRSEL_X,    MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADDRSEL_X,    MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// Absolute, Y
			MODE_ABSOLUTEY: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_RADDRH, ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRH: SequenceRom = { SEQ_DATA,   ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADDRSEL_Y,    MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_ADDR, INCDEC_ADDONE, ADDRSEL_Y,    MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// Indirect
			MODE_INDIRECT: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_RINDRL, ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RINDRL: SequenceRom = { SEQ_RINDRH, ADDRSEL_ADDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RINDRH: SequenceRom = { SEQ_DATA,   ADDRSEL_ADDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// Indirect, X
			MODE_INDIRECTX: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_RINDRL, ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RINDRL: SequenceRom = { SEQ_RINDRH, ADDRSEL_ADDR, INCDEC_NONE,   ADDRSEL_X,    MASK_ADDI, NO  };
				SEQ_RINDRH: SequenceRom = { SEQ_DATA,   ADDRSEL_ADDR, INCDEC_ADDONE, ADDRSEL_X,    MASK_ADDI, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// Indirect, Y
			MODE_INDIRECT: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_RINDRL, ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RINDRL: SequenceRom = { SEQ_RINDRH, ADDRSEL_ADDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RINDRH: SequenceRom = { SEQ_DATA,   ADDRSEL_ADDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_ADDONE, ADDRSEL_Y,    MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_ADDONE, ADDRSEL_Y,    MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// S-stack
			MODE_SSTACK: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_DATA,   ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_NONE,   ADDRSEL_ADDR, MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_NONE,   ADDRSEL_ADDR, MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_NONE,   ADDRSEL_ADDR, MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_NONE,   ADDRSEL_ADDR, MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// S-stack Indirect
			MODE_SSTACKINDR: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_RINDRL, ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RINDRL: SequenceRom = { SEQ_RINDRH, ADDRSEL_S,    INCDEC_NONE,   ADDRSEL_ADDR, MASK_NONE, NO  };
				SEQ_RINDRH: SequenceRom = { SEQ_DATA,   ADDRSEL_S,    INCDEC_ADDONE, ADDRSEL_ADDR, MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// R-stack
			MODE_RSTACK: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_DATA,   ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_R,    INCDEC_NONE,   ADDRSEL_ADDR, MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_R,    INCDEC_NONE,   ADDRSEL_ADDR, MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_R,    INCDEC_NONE,   ADDRSEL_ADDR, MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_R,    INCDEC_NONE,   ADDRSEL_ADDR, MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// R-stack Indirect
			MODE_RSTACKINDR: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_RINDRL, ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RINDRL: SequenceRom = { SEQ_RINDRH, ADDRSEL_R,    INCDEC_NONE,   ADDRSEL_ADDR, MASK_NONE, NO  };
				SEQ_RINDRH: SequenceRom = { SEQ_DATA,   ADDRSEL_R,    INCDEC_ADDONE, ADDRSEL_ADDR, MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADDRSEL_Y,    MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// NXA / NXT
			MODE_NXA_NXT: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RVALL,  ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_I,    INCDEC_INC,    ADD_NONE,     MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_I,    INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// BRK
			MODE_BRK: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_WVALL,  ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_WVALH,  ADDRSEL_S,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALH:  SequenceRom = { SEQ_WVALX,  ADDRSEL_S,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALX:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// PHx
			MODE_PHZ: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_WVALL,  ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// PLx
			MODE_PLZ: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RVALL,  ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_INC,    ADD_NONE,     MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// RHx
			MODE_RHZ: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_WVALL,  ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_R,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_R,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// RLx
			MODE_RLZ: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RVALL,  ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_R,    INCDEC_INC,    ADD_NONE,     MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_R,    INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// Absolute X, Indirect
			MODE_ABSOLUTEXINDR: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_RADDRH, ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRH: SequenceRom = { SEQ_RINDRL, ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RINDRL: SequenceRom = { SEQ_RINDRH, ADDRSEL_ADDR, INCDEC_NONE,   ADDRSEL_X,    MASK_NONE, NO  };
				SEQ_RINDRH: SequenceRom = { SEQ_DATA,   ADDRSEL)ADDR, INCDEC_ADDONE, ADDRSEL_X,    MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, YES };
				SEQ_RVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, YES };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_INDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// ENT / REA
			MODE_ENT_REA: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RVALL,  ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_RVALH,  ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALH:  SequenceRom = { SEQ_WVALL,  ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_WVALH,  ADDRSEL_R,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_R,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// PEA
			MODE_PEA: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RVALL,  ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_RVALH,  ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALH:  SequenceRom = { SEQ_WVALL,  ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_WVALH,  ADDRSEL_S,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// PEI
			MODE_PEI: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RADDRL, ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RADDRL: SequenceRom = { SEQ_RVALL,  ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_RVALH,  ADDRSEL_ADDR, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALH:  SequenceRom = { SEQ_WVALL,  ADDRSEL_ADDR, INCDEC_ADDONE, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_WVALH,  ADDRSEL_S,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			// PER
			MODE_PER: begin
				case(Sequence)
				SEQ_RINSN:  SequenceRom = { SEQ_RVALL,  ADDRSEL_NONE, INCDEC_NONE,   ADD_NONE,     MASK_NONE, NO  };
				SEQ_RVALL:  SequenceRom = { SEQ_WVALL,  ADDRSEL_PC,   INCDEC_INC,    ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALL:  SequenceRom = { SEQ_WVALH,  ADDRSEL_S,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				SEQ_WVALH:  SequenceRom = { SEQ_END,    ADDRSEL_S,    INCDEC_PREDEC, ADD_NONE,     MASK_NONE, NO  };
				default:  SequenceRom = 0;
				endcase
			end
			default: SequenceRom = 0;
			endcase
		end
	endfunction

endmodule
