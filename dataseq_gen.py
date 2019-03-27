def Seq(*argv):
  bitmask = 0
  for arg in argv:
    bitmask = bitmask | arg
  return bitmask

# Enumeration of steps for addressing
Start =           0 # Starting sequence index
Finish =          0 # Terminating sequence index
StepReadAddrLSB = 1 # Address LSB read
StepReadAddrMSB = 2 # Address MSB read
StepReadIndrLSB = 3 # Indirect address LSB read
StepReadIndrMSB = 4 # Indirect address MSB read
StepReadValLSB =  5 # Value LSB read
StepReadValMSB =  6 # Value MSB read
#StepWriteValLSB = 7 # Value LSB write
#StepWriteValMSB = 8 # Value MSB write
StepWriteEx = 9     # Extended value write

TargetA = 1,
TargetD = 2,
TargetX = 3,
TargetY = 4,
TargetS = 5,
TargetR = 6,
TargetI = 7,
TargetPC = 8,
TargetP = 9,
TargetBRK = 10,
TargetPOR = 11,
TargetRBA = 12,
Target

Sequences
