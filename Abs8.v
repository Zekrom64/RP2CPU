module Abs8(X,Y,Neg);

	input [7:0] X;
	output [7:0] Y;
	output Neg;
	
	assign Neg = X[7];
	assign Y = Neg ? -X : X;

endmodule
