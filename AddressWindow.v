module AddressWindow(Address, Base, Head, Valid, Offset);
	
	input [15:0] Address, Base, Head;
	output Valid;
	output [15:0] Offset;
	
	wor baseValid;
	wire headValid;
	
	AlteraCompareUnsigned compareBase(
		.dataa(Address),
		.datab(Base),
		.aeb(baseValid),
		.agb(baseValid)
	);
	
	AlteraCompareUnsigned compareHead(
		.dataa(Address),
		.datab(Head),
		.alb(headValid)
	);
	
	assign Valid = baseValid & headValid;
	assign Offset = Address - Base;
	

endmodule
