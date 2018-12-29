module Display2VGA(Clock, DisplayAddr, CursorEnable, DisplayChar, PixelOut, HSync, VSync);


	input Clock;
	
	output [11:0] DisplayAddr;
	input CursorEnable;
	input [7:0] DisplayChar;
	
	output PixelOut;
	output reg HSync, VSync;

	reg divisorCounter;

	wire inFrame;
	wire pixelValue;
	reg hblank, vblank;
	
	assign PixelOut = (pixelValue && inFrame) && !(hblank || vblank);
	
	always @(posedge Clock) begin
		divisorCounter = !divisorCounter;
	end
	
	reg [9:0] linePixelCounter;
	reg [4:0] lineFPCounter;
	reg [6:0] lineSyncCounter;
	reg [5:0] lineBPCounter;
	
	reg [8:0] frameLineCounter;
	reg [4:0] frameFPCounter;
	reg [1:0] frameSyncCounter;
	reg [5:0] frameBPCounter;
	
	reg [7:0] fontMem[0:1023];
	
	wire [8:0] frameInframeLine;
	assign frameInframeLine = frameLineCounter - 40;
	
	assign inFrame = frameLineCounter >= 40 && frameLineCounter < 440;
	assign DisplayAddr = linePixelCounter[9:3] + (frameInframeLine[8:3] * 80);
	wire [7:0] currentLine;
	assign currentLine = fontMem[{DisplayChar[6:0], frameInframeLine[2:0]}];
	assign pixelValue = (currentLine[7-linePixelCounter[2:0]] ^ DisplayChar[7]) || CursorEnable;
	
	initial begin
		HSync <= 0;
		VSync <= 0;
		hblank <= 0;
		vblank <= 0;
		
		divisorCounter <= 0;
		
		linePixelCounter <= 0;
		lineFPCounter <= 0;
		lineSyncCounter <= 0;
		lineBPCounter <= 0;
		
		frameLineCounter <= 0;
		frameFPCounter <= 0;
		frameSyncCounter <= 0;
		frameBPCounter <= 0;
		
		$readmemh("displayfont.bin", fontMem);
	end
	
	always @(posedge divisorCounter) begin
		if (linePixelCounter != 640) begin
			linePixelCounter = linePixelCounter + 1;
		end else if (lineFPCounter != 16) begin
			lineFPCounter = lineFPCounter + 1;
			hblank = 1;
		end else if (lineSyncCounter != 96) begin
			HSync = 1;
			lineSyncCounter = lineSyncCounter + 1;
		end else if (lineBPCounter != 48) begin
			HSync = 0;
			lineBPCounter = lineBPCounter + 1;
		end else begin
			hblank = 0;
			linePixelCounter = 0;
			lineFPCounter = 0;
			lineSyncCounter = 0;
			lineBPCounter = 0;
			
			if (frameLineCounter < 480) begin
				frameLineCounter = frameLineCounter + 1;
			end else if (frameFPCounter < 10) begin
				frameFPCounter = frameFPCounter + 1;
				vblank = 1;
			end else if (frameSyncCounter < 2) begin
				frameSyncCounter = frameSyncCounter + 1;
				VSync = 1;
			end else if (frameBPCounter < 33) begin
				frameBPCounter = frameBPCounter + 1;
				VSync = 0;
			end else begin
				vblank = 0;
				frameLineCounter = 0;
				frameFPCounter = 0;
				frameSyncCounter = 0;
				frameBPCounter = 0;
			end
		end
	end

endmodule
