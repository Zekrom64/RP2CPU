module Abs16(X,Y,Neg);

	input [15:0] X;
	output [15:0] Y;
	output Neg;
	
	assign Neg = X[15];
	assign Y = Neg ? -X : X;

endmodule
