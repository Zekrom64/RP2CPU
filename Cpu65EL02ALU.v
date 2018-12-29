module Cpu65EL02ALU(
	A,B,D,Y,
	Op,Size,
	InFlagD,InFlagC,
	OutFlagC,OutFlagV,OutFlagZ,OutFlagN);

	input [15:0] A, B, D;
	output [15:0] Y;
	
	input [4:0] Op;
	input Size;
	
	input InFlagD, InFlagC;
	output OutFlagC, OutFlagV, OutFlagZ, OutFlagN;
	
	wire [31:0] divOpA;
	wire [15:0] mulOpA, divmulOpB;
	wire opANeg, opBNeg;
	wire divOpANeg, mulOpANeg;
	
	Abs32 absDivOpA(
		.X(Size ? {{16{D[7]}},D[7:0],A[7:0]} : {D,A}),
		.Y(divOpA),
		.Neg(divOpANeg)
	);
	Abs16 absMulOpA(
	);
	
	Abs16 absOpB(
		.X(Size ? {{8{B[7]}},B[7:0]} : B),
		.Y(divmulOpB),
		.Neg(opBNeg)
	);
	
	wire [31:0] divideQuotient;
	wire [15:0] divideRemainder;
	AlteraDivider divide(
		.numer(divOpA),
		.denom(divmulOpB),
		.quotient(divideQuotient),
		.remain(divideRemainder)
	);
	
	function [17:0] computeResult;
		input [4:0] Op;
		begin
			case(Op)
			 0: computeResult = {2'b00, A | B}; // OR
			 1: computeResult = {2'b00, A & B}; // AND
			 2: computeResult = {2'b00, A ^ B}; // EOR
			 3: ;// ADC
			 4: ;// CMP
			 5: ;// SBC
			 6: ;// TSB
			 7: ;// TRB
			 8: begin // BIT
				computeResult[15:0] = A & B;
				computeResult[16] = 0;
				computeResult[17] = Size ? (A[6] & B[6]) : (A[14] & B[14]);
			 end
			 9: ;// ASL
			10: ;// ROL
			11: ;// LSR
			12: ;// ROR
			13: ;// DEC
			14: ;// INC
			15: ;// MUL
			16: ;// DIV
			17: computeResult = 0; // ZEA
			18: computeResult = {2'b00, (Size ? A[7] : A[15])}; // SEA
			19: computeResult = {2'b00, Size ? 8'h00 : A[15:8], A[7:0]}; // Pass-through
			default: computeResult = 0;
			endcase
		end
	endfunction
	
	wire [17:0] result;
	assign result = computeResult(Op);
	
	assign OutFlagC = result[16];
	assign OutFlagV = result[17];
	assign OutFlagN = Size ? result[7] : result[15];
	assign OutFlagZ = result[15:0] == 0;
	
endmodule
