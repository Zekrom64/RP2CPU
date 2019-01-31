module AsyncLatch(Set,Reset,Flag);

	input Set, Reset;
	output reg Flag;
	
	initial begin
		Flag = 0;
	end
	
	always @(*) begin
		if (Reset) Flag = 0;
		else if (Set) Flag = 1;
	end

endmodule
