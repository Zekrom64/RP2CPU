module Abs32(X,Y,Neg);

	input [31:0] X;
	output [31:0] Y;
	output Neg;
	
	assign Neg = X[31];
	assign Y = Neg ? -X : X;

endmodule
