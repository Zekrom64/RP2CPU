def Insn(*argv):
	bitmask = 0
	for arg in argv:
		bitmask = bitmask | arg
	return bitmask
	
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
	0x10: Insn(FuncBranch, FlagN, ValueFalse),              # BPL *
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
	0x20:
	0x21:
	0x22:
	0x23:
	0x24:
	0x25:
	0x26:
	0x27:
	0x28:
	0x29:
	0x2A:
	0x2B:
	0x2C:
	0x2D:
	0x2E:
	0x2F:
}

