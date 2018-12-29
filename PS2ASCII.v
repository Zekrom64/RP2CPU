module PS2ASCII(PS2Clock, PS2Data, WriteChar, CharOut);

	input PS2Clock, PS2Data;
	output WriteChar;
	output [7:0] CharOut;
	
	reg IsBreak;
	reg IsExtended;
	
	reg IsCapsLock;
	reg IsLShift, IsRShift;
	reg IsLCtrl, IsRCtrl;
	
	wire IsShifted, IsUpperCase;
	assign IsShifted = IsLShift || IsRShift;
	assign IsUpperCast = IsCapsLock ^ IsShifted;
	
	task DecodeScancode();
		begin
			if (IsBreak) begin // Key release
				if (RxData == 8'h12) IsLShift = 0;
				if (RxData == 8'h59) IsRShift = 0;
				if (RxData == 8'h14) begin
					if (IsExtended) IsRCtrl = 0;
					else IsLCtrl = 0;
				end
			end else begin // Key press
				if (RxData == 8'hF0) IsBreak = 1;
				else if (RxData == 8'hE0) IsExtended = 1;
				else begin
					if (IsExtended) begin
//						if (RxData == 8'h71) 
					end else begin
						if (RxData == 8'h58) IsCapsLock = !IsCapsLock;
					end
				end
			end
		end
	endtask
	
	////////////////////
	// Serial Receive //
	////////////////////
	
	reg IsReceiving, IsDataComplete, IsReceiveComplete;
	reg [2:0] RxBit;
	reg [7:0] RxData;
	
	wire RxDataParity;
	assign RxDataParity = ((^RxData[7:6])^(^RxData[5:4]))^((^RxData[3:2])^(^RxData[1:0]));
	
	initial begin
		IsReceiving <= 0;
	end
	
	always @(posedge PS2Clock) begin
		if (!IsReceiving) begin
			IsReceiving = 1;
		end else if (!IsDataComplete) begin
			RxData[RxBit] = PS2Data;
			RxBit = RxBit + 1;
			if (RxBit == 0) begin
				IsDataComplete = 1;
			end
		end else if (!IsReceiveComplete) begin
			if (RxData == RxDataParity) DecodeScancode();
			IsReceiveComplete = 1;
		end else begin
			IsReceiving <= 0;
			IsDataComplete <= 0;
			IsReceiveComplete <= 0;
			RxBit <= 0;
			RxData <= 0;
		end
	end

endmodule
