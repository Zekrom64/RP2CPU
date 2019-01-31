module SerialIn(SerialReady, SerialData, SerialClock, DataOut, DataWrite);
	
	input SerialReady, SerialData, SerialClock;
	output reg [7:0] DataOut;
	output reg DataWrite;
	
	reg [2:0] dataCounter;
	
	always @(negedge SerialClock or posedge SerialReady) begin
		if (SerialReady && !SerialClock) begin
			DataOut[dataCounter] = SerialData;
			dataCounter = dataCounter + 1;
			if (dataCounter == 0) DataWrite = 1;
		end
		if (SerialReady && SerialClock) DataWrite = 0;
	end

endmodule
