def Insn(*argv):
  bitmask = 0
  for arg in argv:
    bitmask = bitmask | arg
  return bitmask

BitLocFunction = 0
BitLocTarget = 5
BitLocAluop = 9
BitLocAddressing = 13
BitLocFlag = 17
BitLocValue = 19

# No addressing =0
Immediate = 0b0001 << BitLocAddressing
ZeroPage =  0b0010 << BitLocAddressing
ZeroPageX = 0b0011 << BitLocAddressing
ZeroPageY = 0b0100 << BitLocAddressing
Absolute =  0b0101 << BitLocAddressing
AbsoluteX = 0b0110 << BitLocAddressing
AbsoluteY = 0b0111 << BitLocAddressing
Indirect  = 0b1000 << BitLocAddressing
IndirectX = 0b1001 << BitLocAddressing
IndirectY = 0b1010 << BitLocAddressing
SStack    = 0b1011 << BitLocAddressing
SStackIndirect = 0b1100 << BitLocAddressing
RStack    = 0b1101 << BitLocAddressing
RStackIndirect = 0b1110 << BitLocAddressing

TargetA =   0b0000 << BitLocTarget
TargetD =   0b0001 << BitLocTarget
TargetX =   0b0010 << BitLocTarget
TargetY =   0b0011 << BitLocTarget
TargetS =   0b0100 << BitLocTarget
TargetR =   0b0101 << BitLocTarget
TargetI =   0b0110 << BitLocTarget
TargetMem = 0b0111 << BitLocTarget
TargetP =   0b1000 << BitLocTarget

FuncAluop =   0b00001 << BitLocFunction
FuncSPush =   0b00010 << BitLocFunction
FuncRPush =   0b00011 << BitLocFunction
FuncSPull =   0b00100 << BitLocFunction
FuncRPull =   0b00101 << BitLocFunction
FuncModflag = 0b00110 << BitLocFunction
FuncBranch =  0b00111 << BitLocFunction
FunENT =      0b01000 << BitLocFunction
FunJSR =      0b01000 << BitLocFunction
FunRTS =      0b01000 << BitLocFunction
FunJMP =      0b01000 << BitLocFunction
FunRTI =      0b01000 << BitLocFunction
FunBRK =      0b01000 << BitLocFunction
FunENT =      0b01000 << BitLocFunction
FunENT =      0b01000 << BitLocFunction

Instructions = {
  0x00: Insn(FuncBRK),                                    # BRK
  0x01: Insn(FuncAluop, TargetA, AluORA, IndirectX),      # ORA ((*,X))
  0x02: Insn(FuncNXT),                                    # NXT
  0x03: Insn(FuncAluop, TargetA, AluORA, SStack),         # ORA (*,S)
  0x04: Insn(FuncAluop, TargetA, AluTSB, ZeroPage),       # TSB [*]
  0x05: Insn(FuncAluop, TargetA, AluORA, ZeroPage),       # ORA [*]
  0x06: Insn(FuncAluop, TargetMem, AluASL, ZeroPage),     # ASL [*]
  0x07: Insn(FuncAluop, TargetA, AluORA, RStack),         # ORA (*,R)
  0x08: Insn(FuncSPush, TargetP)                          # PHP
  0x09: Insn(FuncAluop, TargetA, AluORA, Immediate)       # ORA
  0x0A: Insn(FuncAluop, TargetA, AluASL)                  # ASL A
  0x0B: Insn(FuncRPush, TargetI)                          # RHI
  0x0C: Insn(FuncAluop, TargetA, AluTSB, Absolute)        # TSB (**)
  0x0D: Insn(FuncAluop, TargetA, AluORA, Absolute)        # ORA (**)
  0x0E: Insn(FuncAluop, TargetMem, AluASL, Absolute),     # ASL (**)
  0x0F: Insn(FuncAluop, AluMUL, ZeroPage),                # MUL [*]
  0x10: Insn(FuncBranch, FlagN, ValueFalse, Immediate),   # BPL *
  0x11: Insn(FuncAluop, TargetA, AluORA, IndirectY),      # ORA ((*),Y)
  0x12: Insn(FuncAluop, TargetA, AluORA, Indirect),       # ORA ((*))
  0x13: Insn(FuncAluop, TargetA, AluORA, SStackIndirect), # ORA ((*,S),Y)
  0x14: Insn(FuncAluop, TargetA, AluTRB, ZeroPage),       # TRB [*]
  0x15: Insn(FuncAluop, TargetA, AluORA, ZeroPageX),      # ORA [*,X]
  0x16: Insn(FuncAluop, TargetMem, AluASL, ZeroPageX),    # ASL [*,X]
  0x17: Insn(FuncAluop, TargetA, AluORA, RStackIndirect), # ORA ((*,R),Y)
  0x18: Insn(FuncModflag, FlagC, ValueFalse),             # CLC
  0x19: Insn(FuncAluop, TargetA, AluORA, AbsoluteY),      # ORA (**,Y)
  0x1A: Insn(FuncAluop, TargetA, AluINC),                 # INC A
  0x1B: Insn(FuncRPush, TargetX),                         # RHX
  0x1C: Insn(FuncAluop, TargetA, AluTRB, Absolute),       # TRB (**)
  0x1D: Insn(FuncAluop, TargetA, AluORA, AbsoluteX),      # ORA (**,X)
  0x1E: Insn(FuncAluop, TargetMem, AluASL, AbsoluteX),    # ASL (**,X)
  0x1F: Insn(FuncAluop, AluMUL, ZeroPageX),               # MUL [*,X]
  0x20: Insn(FuncJSR, Absolute),                          # JSR **
  0x21: Insn(FuncAluop, TargetA, AluAND, IndirectX),      # AND ((*,X))
  0x22: Insn(FuncENT, Absolute),                          # ENT **
  0x23: Insn(FuncAluop, TargetA, AluAND, SStack),         # AND (*,S)
  0x24: Insn(FuncAluop, TargetA, AluBIT, ZeroPage),       # BIT [*]
  0x25: Insn(FuncAluop, TargetA, AluAND, ZeroPage),       # AND [*]
  0x26: Insn(FuncAluop, TargetMem, AluROL, ZeroPage),     # ROL [*]
  0x27: Insn(FuncAluop, TargetA, AluAND, RStack),         # AND (*,R)
  0x28: Insn(FuncSPull, TargetP),                         # PLP
  0x29: Insn(FuncAluop, TargetA, AluAND, Immediate),      # BIT *
  0x2A: Insn(FuncAluop, TargetA, AluROL),                 # ROL A
  0x2B: Insn(FuncRPull, TargetI),                         # RLI
  0x2C: Insn(FuncAluop, TargetA, AluBIT, Absolute),       # BIT (**)
  0x2D: Insn(FuncAluop, TargetA, AluAND, Absolute),       # AND (**)
  0x2E: Insn(FuncAluop, TargetMem, AluROL, Absolute),     # ROL (**)
  0x2F: Insn(FuncAluop, AluMUL, Absolute),                # MUL (**)
  0x30: Insn(FuncBranch, FlagN, ValueTrue, Immediate),    # BMI *
  0x31: Insn(FuncAluop, TargetA, AluAND, IndirectY),      # AND ((*),Y)
  0x32: Insn(FuncAluop, TargetA, AluAND, Indirect),       # AND ((*))
  0x33: Insn(FuncAluop, TargetA, AluAND, SStackIndirect), # AND ((*,S),Y)
  0x34: Insn(FuncAluop, TargetA, AluBIT, ZeroPageX),      # BIT [*,X]
  0x35:
  0x36:
  0x37:
  0x38:
  0x39:
  0x3A:
  0x3B:
  0x3C:
  0x3D:
  0x3E:
  0x3F:
  0x40:
  0x41:
  0x42:
  0x43:
  0x44:
  0x45:
  0x46:
  0x47:
  0x48:
  0x49:
  0x4A:
  0x4B:
  0x4C:
  0x4D:
  0x4E:
  0x4F:
}

