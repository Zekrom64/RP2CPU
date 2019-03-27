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

# Continues to the next cycle if value is word
ExtendIfLong = 1 << 4
# Adds one to the effective address
AddrPlusOne =  0b01 << 5
# Predecrements the selected addressing register
PredecReg =    0b10 << 5
# Increments the selected addressing register
IncReg =       0b11 << 5
# Masks the adding addressing register with 0xFF
MaskAddAddr =     1 << 7

# Primary addressing register
AddrselPC =   0 << 8
AddrselX  =   1 << 8
AddrselY  =   2 << 8
AddrselI  =   3 << 8
AddrselAddr = 4 << 8
AddrselIndr = 5 << 8
AddrselS    = 6 << 8
AddrselR    = 7 << 8

# Adds an addressing register to the effective address
def Add(addrsel):
  return addrsel << 3

# Masks the effective address with 0xFF
MaskOutAddr = 1 << 14


Sequences = {
  0x00: { # None
    Start: Seq(Finish)
  },
  0x01: [ # Immediate
    Start:          Seq(StepReadValLSB),
	StepReadValLSB: Seq(Finish, AddrselPC, IncReg, ExtendIfLong),
	StepReadValMSB: Seq(Finish, AddrselPC, IncReg)
  ],
  0x02: [ # Zero Page
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(Finish, AddrselPC, IncReg),
	StepReadValLSB:  Seq(Finish, AddrselAddr, ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselAddr, AddrPlusOne)
  ],
  0x03: [ # Zero Page, X
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(Finish, AddrselPC, IncReg),
	StepReadValLSB:  Seq(Finish, AddrselAddr, Add(AddrselX), MaskOutAddr, ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselAddr, Add(AddrselX), MaskOutAddr, AddrPlusOne)
  ],
  0x04: [ # Zero Page, Y
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(Finish, AddrselPC, IncReg),
	StepReadValLSB:  Seq(Finish, AddrselAddr, Add(AddrselY), MaskOutAddr, ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselAddr, Add(AddrselY), MaskOutAddr, AddrPlusOne)
  ],
  0x05: [ # Absolute
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadAddrMSB, AddrselPC, IncReg),
	StepReadAddrMSB: Seq(Finish, AddrselPC, IncReg),
	StepReadValLSB:  Seq(Finish, AddrselAddr, ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselAddr, AddrPlusOne)
  ],
  0x06: [ # Absolute, X
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadAddrMSB, AddrselPC, IncReg),
	StepReadAddrMSB: Seq(Finish, AddrselPC, IncReg),
	StepReadValLSB:  Seq(Finish, AddrselAddr, Add(AddrselX), ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselAddr, Add(AddrselX), AddrPlusOne)
  ],
  0x07: [ # Absolute, Y
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadAddrMSB, AddrselPC, IncReg),
	StepReadAddrMSB: Seq(Finish, AddrselPC, IncReg),
	StepReadValLSB:  Seq(Finish, AddrselAddr, Add(AddrselY), ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselAddr, Add(AddrselY), AddrPlusOne)
  ],
  0x08: [ # Indirect
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadIndrLSB, AddrselPC, IncReg),
	StepReadIndrLSB: Seq(StepReadIndrMSB, AddrselAddr),
	StepReadIndrMSB: Seq(Finish, AddrselAddr, AddrPlusOne),
	StepReadValLSB:  Seq(Finish, AddrselIndr, ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselIndr, AddrPlusOne)
  ],
  0x09: [ # Indirect, X
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadIndrLSB, AddrselPC, IncReg),
	StepReadIndrLSB: Seq(StepReadIndrMSB, AddrselAddr, Add(AddrselX), MaskAddAddr),
	StepReadIndrMSB: Seq(Finish, AddrselAddr, Add(AddrselX), MaskAddAddr, AddrPlusOne),
	StepReadValLSB:  Seq(Finish, AddrselIndr, ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselIndr, AddrPlusOne)
  ],
  0x0A: [ # Indirect, Y
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadIndrLSB, AddrselPC, IncReg),
	StepReadIndrLSB: Seq(StepReadIndrMSB, AddrselAddr),
	StepReadIndrMSB: Seq(StepReadValLSB, AddrselAddr, AddrPlusOne),
	StepReadValLSB:  Seq(Finish, AddrselIndr, Add(AddrselY), ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselIndr, Add(AddrselY), AddrPlusOne)
  ],
  0x0B: [ # S-stack
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadValLSB, AddrselPC, IncReg),
	StepReadValLSB:  Seq(Finish, AddrselS, Add(AddrselAddr), ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselS, Add(AddrselAddr), AddrPlusOne)
  ],
  0x0C: [ # S-stack Indirect
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadIndrLSB, AddrselPC, IncReg),
	StepReadIndrLSB: Seq(StepReadIndrMSB, AddrselS, Add(AddrselAddr)),
	StepReadIndrMSB: Seq(StepReadValLSB, AddrselS, Add(AddrselAddr), AddrPlusOne),
	StepReadValLSB: Seq(Finish, AddrselIndr, Add(AddrselY), ExtendIfLong),
	StepReadValMSB: Seq(Finish, AddrselIndr, Add(AddrselY), AddrPlusOne),
  ],
  0x0D: [ # R-stack
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadValLSB, AddrselPC, IncReg),
	StepReadValLSB:  Seq(Finish, AddrselR, Add(AddrselAddr), ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselR, Add(AddrselAddr), AddrPlusOne)
  ],
  0x0E: [ # R-stack Indirect
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadIndrLSB, AddrselPC, IncReg),
	StepReadIndrLSB: Seq(StepReadIndrMSB, AddrselR, Add(AddrselAddr)),
	StepReadIndrMSB: Seq(StepReadValLSB, AddrselR, Add(AddrselAddr), AddrPlusOne),
	StepReadValLSB: Seq(Finish, AddrselIndr, Add(AddrselY), ExtendIfLong),
	StepReadValMSB: Seq(Finish, AddrselIndr, Add(AddrselY), AddrPlusOne),
  ],
  0x10: [ # NXA / NXT
    Start:           Seq(StepReadValLSB),
	StepReadValLSB:  Seq(Finish, AddrselI, IncReg, ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselI, IncReg)
  ],
  0x11: [ # BRK
    Start:           Seq(StepWriteValLSB),
	StepWriteValLSB: Seq(StepWriteValMSB, AddrselS, PredecReg),
	StepWriteValMSB: Seq(StepWriteEx, AddrselS, PredecReg),
	StepWriteEx:     Seq(Finish, AddrselS, PredecReg)
  ],
  0x12: [ # PHx
    Start:           Seq(StepWriteValLSB),
	StepWriteValLSB: Seq(Finish, AddrselS, PredecReg, ExtendIfLong),
	StelWriteValMSB: Seq(Finish, AddrselS, PredecReg)
  ],
  0x13: [ # PLx
    Start:           Seq(StepReadValLSB),
	StepReadValLSB:  Seq(Finish, AddrselS, IncReg, ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselS, IncReg)
  ]
  0x14: [ # RHx
    Start:           Seq(StepWriteValLSB),
	StepWriteValLSB: Seq(Finish, AddrselR, PredecReg, ExtendIfLong),
	StelWriteValMSB: Seq(Finish, AddrselR, PredecReg)
  ],
  0x15: [ # RLx
    Start:           Seq(StepReadValLSB),
	StepReadValLSB:  Seq(Finish, AddrselR, IncReg, ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselR, IncReg)
  ],
  0x16: [ # Absolute, X, Indirect
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadAddrMSB, AddrselPC, IncReg),
	StepReadAddrMSB: Seq(StepReadIndrLSB, AddrselPC, IncReg),
	StepReadIndrLSB: Seq(StepReadIndrMSB, AddrselAddr, Add(AddrselX)),
	StepReadIndrMSB: Seq(StepReadValLSB, AddrselAddr, Add(AddrselX), AddrPlusOne),
	StepReadValLSB:  Seq(Finish, AddrselIndr, ExtendIfLong),
	StepReadValMSB:  Seq(Finish, AddrselIndr, AddrPlusOne)
  ],
  0x17: [ # ENT Abs / REA Abs
    Start:           Seq(StepReadAddrLSB),                      # Read immediate word for jump
	StepReadAddrLSB: Seq(StepReadAddrMSB, AddrselPC, IncReg),
	StepReadAddrMSB: Seq(StepWriteValLSB, AddrselPC, IncReg),
	StepWriteValLSB: Seq(StepWriteValMSB, AddrselR, PredecReg), # Push I to R-stack
	StepWriteValMSB: Seq(Finish, AddrselR, PredecReg)
  ],
  0x18: [ # PEA Abs
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadAddrMSB, AddrselPC, IncReg),
	StepReadAddrMSB: Seq(StepWriteValLSB, AddrselPC, IncReg),
	StepWriteValLSB: Seq(StepWriteValMSB, AddrselS, PredecReg),
	StepWriteValMSB: Seq(Finish, AddrselS, PredecReg)
  ],
  0x19: [ # PEI (Zp)
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepReadIndrLSB, AddrselPC, IncReg),
	StepReadIndrLSB: Seq(StepReadIndrMSB, AddrselAddr),
	StepReadIndrMSB: Seq(StepWriteValLSB, AddrselAddr, AddrPlusOne),
	StepWriteValLSB: Seq(StepWriteValMSB, AddrselS, PredecReg),
	StepWriteValMSB: Seq(Finish, AddrselS, PredecReg)
  ],
  0x1A: [ # PER Rel
    Start:           Seq(StepReadAddrLSB),
	StepReadAddrLSB: Seq(StepWriteValLSB, AddrselPC),
	StepWriteValLSB: Seq(StepWriteValMSB< AddrselS, PredecReg),
	StepWriteVal
  ]
}

