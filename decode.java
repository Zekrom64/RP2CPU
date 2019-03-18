package com.eloraam.redpower.control;

import akf;
import amq;
import bq;
import com.eloraam.redpower.RedPowerControl;
import com.eloraam.redpower.core.BlockExtended;
import com.eloraam.redpower.core.CoreLib;
import com.eloraam.redpower.core.IFrameSupport;
import com.eloraam.redpower.core.IHandlePackets;
import com.eloraam.redpower.core.IRedbusConnectable;
import com.eloraam.redpower.core.Packet211TileDesc;
import com.eloraam.redpower.core.RedbusLib;
import com.eloraam.redpower.core.TileExtended;
import com.eloraam.redpower.core.WorldCoord;
import ef;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import md;
import qx;
import ur;
import yc;
import ym;

public class TileCPU
  extends TileExtended
  implements IRedbusConnectable, IHandlePackets, IFrameSupport
{
  public TileCPU()
  {
    this.memory = new byte['?'];
    coldBootCPU();
  }
  
  public void coldBootCPU()
  {
    this.addrPOR = 8192;this.addrBRK = 8192;
    this.regSP = 512;this.regPC = 1024;
    this.regR = 768;
    
    this.regA = 0;this.regX = 0;this.regY = 0;this.regD = 0;
    this.flagC = false;this.flagZ = false;this.flagID = false;
    this.flagD = false;this.flagBRK = false;this.flagO = false;this.flagN = false;
    
    this.flagE = true;this.flagM = true;this.flagX = true;
    
    this.memory[0] = ((byte)this.byte0);this.memory[1] = ((byte)this.byte1);
    
    InputStream is = RedPowerControl.class.getResourceAsStream("/eloraam/control/rpcboot.bin");
    try
    {
      try
      {
        is.read(this.memory, 1024, 256);
      }
      finally
      {
        if (is != null) {
          is.close();
        }
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    this.sliceCycles = -1;
  }
  
  public void warmBootCPU()
  {
    if (this.sliceCycles >= 0)
    {
      this.regSP = 512;this.regR = 768;
      this.regPC = this.addrPOR;
    }
    this.sliceCycles = 0;
  }
  
  public void haltCPU()
  {
    this.sliceCycles = -1;
  }
  
  public boolean isRunning()
  {
    return this.sliceCycles >= 0;
  }
  
  public int rbGetAddr()
  {
    return this.rbaddr;
  }
  
  public void rbSetAddr(int addr) {}
  
  public int rbRead(int reg)
  {
    if (!this.mmuEnRBW) {
      return 0;
    }
    return readOnlyMem(this.mmuRBW + reg);
  }
  
  public void rbWrite(int reg, int dat)
  {
    if (!this.mmuEnRBW) {
      return;
    }
    writeOnlyMem(this.mmuRBW + reg, dat);
  }
  
  public int getConnectableMask()
  {
    return 16777215;
  }
  
  public int getConnectClass(int side)
  {
    return 66;
  }
  
  public int getCornerPowerMode()
  {
    return 0;
  }
  
  public void onBlockPlaced(ur ist, int side, md ent)
  {
    this.Rotation = ((int)Math.floor(ent.z * 4.0F / 360.0F + 0.5D) + 1 & 0x3);
  }
  
  public boolean onBlockActivated(qx player)
  {
    if (player.ah()) {
      return false;
    }
    if (CoreLib.isClient(this.k)) {
      return true;
    }
    player.openGui(RedPowerControl.instance, 2, this.k, this.l, this.m, this.n);
    
    return true;
  }
  
  public int getBlockID()
  {
    return RedPowerControl.blockPeripheral.cm;
  }
  
  public int getExtendedID()
  {
    return 1;
  }
  
  public boolean isUseableByPlayer(qx player)
  {
    if (this.k.q(this.l, this.m, this.n) != this) {
      return false;
    }
    return player.e(this.l + 0.5D, this.m + 0.5D, this.n + 0.5D) <= 64.0D;
  }
  
  protected void refreshBackplane()
  {
    boolean bpok = true;
    WorldCoord wc = new WorldCoord(this);
    for (int i = 0; i < 7; i++) {
      if (!bpok)
      {
        this.backplane[i] = null;
      }
      else
      {
        wc.step(CoreLib.rotToSide(this.Rotation));
        TileBackplane tbp = (TileBackplane)CoreLib.getTileEntity(this.k, wc, TileBackplane.class);
        
        this.backplane[i] = tbp;
        if (tbp == null) {
          bpok = false;
        }
      }
    }
  }
  
  public void g()
  {
    this.rtcTicks += 1;
    if (this.sliceCycles < 0) {
      return;
    }
    this.rbTimeout = false;
    this.rbCache = null;
    this.waiTimeout = false;
    this.sliceCycles += 1000;
    if (this.sliceCycles > 100000) {
      this.sliceCycles = 100000;
    }
    refreshBackplane();
    while ((this.sliceCycles > 0) && 
      (!this.waiTimeout) && 
      (!this.rbTimeout))
    {
      this.sliceCycles -= 1;
      executeInsn();
    }
  }
  
  protected int readOnlyMem(int addr)
  {
    addr &= 0xFFFF;
    if (addr < 8192) {
      return this.memory[addr] & 0xFF;
    }
    int atop = (addr >> 13) - 1;
    if (this.backplane[atop] == null) {
      return 255;
    }
    return this.backplane[atop].readBackplane(addr & 0x1FFF);
  }
  
  public int readMem(int addr)
  {
    if ((this.mmuEnRB) && (addr >= this.mmuRBB) && (addr < this.mmuRBB + 256))
    {
      if (this.rbCache == null) {
        this.rbCache = RedbusLib.getAddr(this.k, new WorldCoord(this), this.mmuRBA);
      }
      if (this.rbCache == null)
      {
        this.rbTimeout = true;
        return 0;
      }
      int tr = this.rbCache.rbRead(addr - this.mmuRBB);
      
      return tr;
    }
    return readOnlyMem(addr);
  }
  
  protected void writeOnlyMem(int addr, int val)
  {
    addr &= 0xFFFF;
    if (addr < 8192)
    {
      this.memory[addr] = ((byte)val);
      return;
    }
    int atop = (addr >> 13) - 1;
    if (this.backplane[atop] == null) {
      return;
    }
    this.backplane[atop].writeBackplane(addr & 0x1FFF, val);
  }
  
  public void writeMem(int addr, int val)
  {
    if ((this.mmuEnRB) && (addr >= this.mmuRBB) && (addr < this.mmuRBB + 256))
    {
      if (this.rbCache == null) {
        this.rbCache = RedbusLib.getAddr(this.k, new WorldCoord(this), this.mmuRBA);
      }
      if (this.rbCache == null)
      {
        this.rbTimeout = true;
        return;
      }
      this.rbCache.rbWrite(addr - this.mmuRBB, val & 0xFF);
      return;
    }
    writeOnlyMem(addr, val);
  }
  
  private void incPC()
  {
    this.regPC = (this.regPC + 1 & 0xFFFF);
  }
  
  private int maskM()
  {
    return this.flagM ? 255 : 65535;
  }
  
  private int maskX()
  {
    return this.flagX ? 255 : 65535;
  }
  
  private int negM()
  {
    return this.flagM ? 128 : 32768;
  }
  
  private int negX()
  {
    return this.flagX ? 128 : 32768;
  }
  
  private int readB()
  {
    int i = readMem(this.regPC);incPC();
    return i;
  }
  
  private int readM()
  {
    int i = readMem(this.regPC);incPC();
    if (!this.flagM)
    {
      i |= readMem(this.regPC) << 8;incPC();
    }
    return i;
  }
  
  private int readX()
  {
    int i = readMem(this.regPC);incPC();
    if (!this.flagX)
    {
      i |= readMem(this.regPC) << 8;incPC();
    }
    return i;
  }
  
  private int readM(int addr)
  {
    int i = readMem(addr);
    if (!this.flagM) {
      i |= readMem(addr + 1) << 8;
    }
    return i;
  }
  
  private int readX(int addr)
  {
    int i = readMem(addr);
    if (!this.flagX) {
      i |= readMem(addr + 1) << 8;
    }
    return i;
  }
  
  private void writeM(int addr, int reg)
  {
    writeMem(addr, reg);
    if (!this.flagM) {
      writeMem(addr + 1, reg >> 8);
    }
  }
  
  private void writeX(int addr, int reg)
  {
    writeMem(addr, reg);
    if (!this.flagX) {
      writeMem(addr + 1, reg >> 8);
    }
  }
  
  private int readBX()
  {
    int i = readMem(this.regPC) + this.regX;
    if (this.flagX) {
      i &= 0xFF;
    }
    incPC();
    return i;
  }
  
  private int readBY()
  {
    int i = readMem(this.regPC) + this.regY;
    if (this.flagX) {
      i &= 0xFF;
    }
    incPC();
    return i;
  }
  
  private int readBS()
  {
    int i = readMem(this.regPC) + this.regSP & 0xFFFF;
    incPC();
    return i;
  }
  
  private int readBR()
  {
    int i = readMem(this.regPC) + this.regR & 0xFFFF;
    incPC();
    return i;
  }
  
  private int readBSWY()
  {
    int i = readMem(this.regPC) + this.regSP & 0xFFFF;
    incPC();
    return readW(i) + this.regY & 0xFFFF;
  }
  
  private int readBRWY()
  {
    int i = readMem(this.regPC) + this.regR & 0xFFFF;
    incPC();
    return readW(i) + this.regY & 0xFFFF;
  }
  
  private int readW()
  {
    int i = readMem(this.regPC);incPC();
    i |= readMem(this.regPC) << 8;incPC();
    return i;
  }
  
  private int readW(int addr)
  {
    int i = readMem(addr);
    i |= readMem(addr + 1) << 8;
    return i;
  }
  
  private int readWX()
  {
    int i = readMem(this.regPC);incPC();
    i |= readMem(this.regPC) << 8;incPC();
    return i + this.regX & 0xFFFF;
  }
  
  private int readWY()
  {
    int i = readMem(this.regPC);incPC();
    i |= readMem(this.regPC) << 8;incPC();
    return i + this.regY & 0xFFFF;
  }
  
  private int readWXW()
  {
    int i = readMem(this.regPC);incPC();
    i |= readMem(this.regPC) << 8;incPC();
    i = i + this.regX & 0xFFFF;
    int j = readMem(i);
    j |= readMem(i + 1) << 8;
    return j;
  }
  
  private int readBW()
  {
    int i = readMem(this.regPC);incPC();
    int j = readMem(i);
    j |= readMem(i + 1) << 8;
    return j;
  }
  
  private int readWW()
  {
    int i = readMem(this.regPC);incPC();
    i |= readMem(this.regPC) << 8;incPC();
    int j = readMem(i);
    j |= readMem(i + 1) << 8;
    return j;
  }
  
  private int readBXW()
  {
    int i = readMem(this.regPC) + this.regX & 0xFF;incPC();
    int j = readMem(i);
    j |= readMem(i + 1) << 8;
    return j;
  }
  
  private int readBWY()
  {
    int i = readMem(this.regPC);incPC();
    int j = readMem(i);
    j |= readMem(i + 1) << 8;
    return j + this.regY & 0xFFFF;
  }
  
  private void upNZ()
  {
    this.flagN = ((this.regA & negM()) > 0);
    this.flagZ = (this.regA == 0);
  }
  
  private void upNZ(int i)
  {
    this.flagN = ((i & negM()) > 0);
    this.flagZ = (i == 0);
  }
  
  private void upNZX(int i)
  {
    this.flagN = ((i & negX()) > 0);
    this.flagZ = (i == 0);
  }
  
  private void push1(int b)
  {
    if (this.flagE) {
      this.regSP = (this.regSP - 1 & 0xFF | this.regSP & 0xFF00);
    } else {
      this.regSP = (this.regSP - 1 & 0xFFFF);
    }
    writeMem(this.regSP, b);
  }
  
  private void push1r(int b)
  {
    this.regR = (this.regR - 1 & 0xFFFF);11
    writeMem(this.regR, b);
  }
  
  private void push2(int w)
  {
    push1(w >> 8);
    push1(w & 0xFF);
  }
  
  private void push2r(int w)
  {
    push1r(w >> 8);
    push1r(w & 0xFF);
  }
  
  private void pushM(int b)
  {
    if (this.flagM) {
      push1(b);
    } else {
      push2(b);
    }
  }
  
  private void pushX(int b)
  {
    if (this.flagX) {
      push1(b);
    } else {
      push2(b);
    }
  }
  
  private void pushMr(int b)
  {
    if (this.flagM) {
      push1r(b);
    } else {
      push2r(b);
    }
  }
  
  private void pushXr(int b)
  {
    if (this.flagX) {
      push1r(b);
    } else {
      push2r(b);
    }
  }
  
  private int pop1()
  {
    int tr = readMem(this.regSP);
    if (this.flagE) {
      this.regSP = (this.regSP + 1 & 0xFF | this.regSP & 0xFF00);
    } else {
      this.regSP = (this.regSP + 1 & 0xFFFF);
    }
    return tr;
  }
  
  private int pop1r()
  {
    int tr = readMem(this.regR);
    this.regR = (this.regR + 1 & 0xFFFF);
    return tr;
  }
  
  private int pop2()
  {
    int tr = pop1();
    tr |= pop1() << 8;
    return tr;
  }
  
  private int pop2r()
  {
    int tr = pop1r();
    tr |= pop1r() << 8;
    return tr;
  }
  
  private int popM()
  {
    if (this.flagM) {
      return pop1();
    }
    return pop2();
  }
  
  private int popMr()
  {
    if (this.flagM) {
      return pop1r();
    }
    return pop2r();
  }
  
  private int popX()
  {
    if (this.flagX) {
      return pop1();
    }
    return pop2();
  }
  
  private int popXr()
  {
    if (this.flagX) {
      return pop1r();
    }
    return pop2r();
  }
  
  private int getFlags()
  {
    return (this.flagC ? 1 : 0) | (this.flagZ ? 2 : 0) | (this.flagID ? 4 : 0) | (this.flagD ? 8 : 0) | (this.flagX ? 16 : 0) | (this.flagM ? 32 : 0) | (this.flagO ? 64 : 0) | (this.flagN ? 128 : 0);
  }
  
  private void setFlags(int flags)
  {
    this.flagC = ((flags & 0x1) > 0);
    this.flagZ = ((flags & 0x2) > 0);
    this.flagID = ((flags & 0x4) > 0);
    this.flagD = ((flags & 0x8) > 0);
    boolean m2 = (flags & 0x20) > 0;
    this.flagO = ((flags & 0x40) > 0);
    this.flagN = ((flags & 0x80) > 0);
    if (this.flagE)
    {
      this.flagX = false;this.flagM = false;
    }
    else
    {
      this.flagX = ((flags & 0x10) > 0);
      if (this.flagX)
      {
        this.regX &= 0xFF;this.regY &= 0xFF;
      }
      if (m2 != this.flagM)
      {
        if (m2)
        {
          this.regB = (this.regA >> 8);this.regA &= 0xFF;
        }
        else
        {
          this.regA |= this.regB << 8;
        }
        this.flagM = m2;
      }
    }
  }
  
  private void i_adc(int val)
  {
    if (this.flagM)
    {
      if (this.flagD)
      {
        int v1 = (this.regA & 0xF) + (val & 0xF) + (this.flagC ? 1 : 0);
        if (v1 > 9) {
          v1 = (v1 + 6 & 0xF) + 16;
        }
        int v2 = (this.regA & 0xF0) + (val & 0xF0) + v1;
        if (v2 > 160) {
          v2 += 96;
        }
        this.flagC = (v2 > 100);
        this.regA = (v2 & 0xFF);
        this.flagO = false;
      }
      else
      {
        int v = this.regA + val + (this.flagC ? 1 : 0);
        this.flagC = (v > 255);
        this.flagO = (((v ^ this.regA) & (v ^ val) & 0x80) > 0);
        
        this.regA = (v & 0xFF);
      }
    }
    else
    {
      int v = this.regA + val + (this.flagC ? 1 : 0);
      this.flagC = (v > 65535);
      this.flagO = (((v ^ this.regA) & (v ^ val) & 0x8000) > 0);
      
      this.regA = (v & 0xFFFF);
    }
    upNZ();
  }
  
  private void i_sbc(int val)
  {
    if (this.flagM)
    {
      if (this.flagD)
      {
        int v1 = (this.regA & 0xF) - (val & 0xF) + (this.flagC ? 1 : 0) - 1;
        if (v1 < 0) {
          v1 = (v1 - 6 & 0xF) - 16;
        }
        int v2 = (this.regA & 0xF0) - (val & 0xF0) + v1;
        if (v2 < 0) {
          v2 -= 96;
        }
        this.flagC = (v2 < 100);
        this.regA = (v2 & 0xFF);
        this.flagO = false;
      }
      else
      {
        int v = this.regA - val + (this.flagC ? 1 : 0) - 1;
        this.flagC = ((v & 0x100) == 0);
        
        this.flagO = (((v ^ this.regA) & (v ^ -val) & 0x80) > 0);
        this.regA = (v & 0xFF);
      }
    }
    else
    {
      int v = this.regA - val + (this.flagC ? 1 : 0) - 1;
      this.flagC = ((v & 0x10000) == 0);
      
      this.flagO = (((v ^ this.regA) & (v ^ -val) & 0x8000) > 0);
      this.regA = (v & 0xFFFF);
    }
    upNZ();
  }
  
  private void i_mul(int val)
  {
    if (this.flagM)
    {
      int v;
      int v;
      if (this.flagC) {
        v = (byte)val * (byte)this.regA;
      } else {
        v = val * this.regA;
      }
      this.regA = (v & 0xFF);
      this.regD = (v >> 8 & 0xFF);
      this.flagN = (v < 0);
      this.flagZ = (v == 0);
      this.flagO = ((this.regD != 0) && (this.regD != 255));
    }
    else
    {
      long v;
      long v;
      if (this.flagC) {
        v = (short)val * (short)this.regA;
      } else {
        v = val * this.regA;
      }
      this.regA = ((int)(v & 0xFFFF));
      this.regD = ((int)(v >> 16 & 0xFFFF));
      this.flagN = (v < 0L);
      this.flagZ = (v == 0L);
      this.flagO = ((this.regD != 0) && (this.regD != 65535));
    }
  }
  
  private void i_div(int val)
  {
    if (val == 0)
    {
      this.regA = 0;this.regD = 0;this.flagO = true;
      this.flagZ = false;this.flagN = false;
      return;
    }
    if (this.flagM)
    {
      int q;
      if (this.flagC)
      {
        int q = (byte)this.regD << 8 | this.regA;
        val = (byte)val;
      }
      else
      {
        q = this.regD << 8 | this.regA;
      }
      this.regD = (q % val & 0xFF);
      q /= val;
      this.regA = (q & 0xFF);
      if (this.flagC) {
        this.flagO = ((q > 127) || (q < -128));
      } else {
        this.flagO = (q > 255);
      }
      this.flagZ = (this.regA == 0);
      this.flagN = (q < 0);
    }
    else if (this.flagC)
    {
      int q = (short)this.regD << 16 | this.regA;
      val = (short)val;
      this.regD = (q % val & 0xFFFF);
      q /= val;
      this.regA = (q & 0xFFFF);
      this.flagO = ((q > 32767) || (q < 32768));
      this.flagZ = (this.regA == 0);
      this.flagN = (q < 0);
    }
    else
    {
      long q = this.regD << 16 | this.regA;
      this.regD = ((int)(q % val & 0xFFFF));
      q /= val;
      this.regA = ((int)(q & 0xFFFF));
      this.flagO = (q > 65535L);
      this.flagZ = (this.regA == 0);
      this.flagN = (q < 0L);
    }
  }
  
  private void i_and(int val)
  {
    this.regA &= val;
    upNZ();
  }
  
  private void i_asl(int addr)
  {
    int i = readM(addr);
    
    this.flagC = ((i & negM()) > 0);
    i = i << 1 & maskM();upNZ(i);
    writeM(addr, i);
  }
  
  private void i_lsr(int addr)
  {
    int i = readM(addr);
    this.flagC = ((i & 0x1) > 0);
    i >>>= 1;upNZ(i);
    writeM(addr, i);
  }
  
  private void i_rol(int addr)
  {
    int i = readM(addr);
    int n = (i << 1 | (this.flagC ? 1 : 0)) & maskM();
    this.flagC = ((i & negM()) > 0);
    upNZ(n);
    writeM(addr, n);
  }
  
  private void i_ror(int addr)
  {
    int i = readM(addr);
    int n = i >>> 1 | (this.flagC ? negM() : 0);
    this.flagC = ((i & 0x1) > 0);
    upNZ(n);
    writeM(addr, n);
  }
  
  private void i_brc(boolean cond)
  {
    int n = readB();
    if (cond) {
      this.regPC = (this.regPC + (byte)n & 0xFFFF);
    }
  }
  
  private void i_bit(int val)
  {
    if (this.flagM)
    {
      this.flagO = ((val & 0x40) > 0);
      this.flagN = ((val & 0x80) > 0);
    }
    else
    {
      this.flagO = ((val & 0x4000) > 0);
      this.flagN = ((val & 0x8000) > 0);
    }
    this.flagZ = ((val & this.regA) > 0);
  }
  
  private void i_trb(int val)
  {
    this.flagZ = ((val & this.regA) > 0);
    this.regA &= (val ^ 0xFFFFFFFF);
  }
  
  private void i_tsb(int val)
  {
    this.flagZ = ((val & this.regA) > 0);
    this.regA |= val;
  }
  
  private void i_cmp(int reg, int val)
  {
    reg -= val;
    this.flagC = (reg >= 0);
    this.flagZ = (reg == 0);
    this.flagN = ((reg & negM()) > 0);
  }
  
  private void i_cmpx(int reg, int val)
  {
    reg -= val;
    this.flagC = (reg >= 0);
    this.flagZ = (reg == 0);
    this.flagN = ((reg & negX()) > 0);
  }
  
  private void i_dec(int addr)
  {
    int i = readM(addr);
    i = i - 1 & maskM();
    writeM(addr, i);
    upNZ(i);
  }
  
  private void i_inc(int addr)
  {
    int i = readM(addr);
    i = i + 1 & maskM();
    writeM(addr, i);
    upNZ(i);
  }
  
  private void i_eor(int val)
  {
    this.regA ^= val;upNZ();
  }
  
  private void i_or(int val)
  {
    this.regA |= val;upNZ();
  }
  
  private void i_mmu(int mmu)
  {
    switch (mmu)
    {
    case 0:
      int t = this.regA & 0xFF;
      if (this.mmuRBA != t)
      {
        if (this.rbCache != null) {
          this.rbTimeout = true;
        }
        this.mmuRBA = t;
      }
      break;
    case 128: 
      this.regA = this.mmuRBA;
      break;
    case 1: 
      this.mmuRBB = this.regA;
      break;
    case 129: 
      this.regA = this.mmuRBB;
      if (this.flagM)
      {
        this.regB = (this.regA >> 8);this.regA &= 0xFF;
      }
      break;
    case 2: 
      this.mmuEnRB = true;
      break;
    case 130: 
      this.mmuEnRB = false;
      break;
    case 3: 
      this.mmuRBW = this.regA;
      break;
    case 131: 
      this.regA = this.mmuRBW;
      if (this.flagM)
      {
        this.regB = (this.regA >> 8);this.regA &= 0xFF;
      }
      break;
    case 4: 
      this.mmuEnRBW = true;
      break;
    case 132: 
      this.mmuEnRBW = false;
      break;
    case 5: 
      this.addrBRK = this.regA;
      break;
    case 133: 
      this.regA = this.addrBRK;
      if (this.flagM)
      {
        this.regB = (this.regA >> 8);this.regA &= 0xFF;
      }
      break;
    case 6: 
      this.addrPOR = this.regA;
      break;
    case 134: 
      this.regA = this.addrPOR;
      if (this.flagM)
      {
        this.regB = (this.regA >> 8);this.regA &= 0xFF;
      }
      break;
    case 135: 
      this.regA = (this.rtcTicks & 0xFFFF);
      this.regD = (this.rtcTicks >> 16 & 0xFFFF);
    }
  }
  
  public void executeInsn()
  {
    int insn = readMem(this.regPC);
    
    incPC();
    int n;
    switch (insn)
    {
    case 105: 
      i_adc(readM()); break;
    case 101: 
      i_adc(readM(readB())); break;
    case 117: 
      i_adc(readM(readBX())); break;
    case 109: 
      i_adc(readM(readW())); break;
    case 125: 
      i_adc(readM(readWX())); break;
    case 121: 
      i_adc(readM(readWY())); break;
    case 97: 
      i_adc(readM(readBXW())); break;
    case 113: 
      i_adc(readM(readBWY())); break;
    case 114: 
      i_adc(readM(readBW())); break;
    case 99: 
      i_adc(readM(readBS())); break;
    case 115: 
      i_adc(readM(readBSWY())); break;
    case 103: 
      i_adc(readM(readBR())); break;
    case 119: 
      i_adc(readM(readBRWY())); break;
    case 41: 
      i_and(readM()); break;
    case 37: 
      i_and(readM(readB())); break;
    case 53: 
      i_and(readM(readBX())); break;
    case 45: 
      i_and(readM(readW())); break;
    case 61: 
      i_and(readM(readWX())); break;
    case 57: 
      i_and(readM(readWY())); break;
    case 33: 
      i_and(readM(readBXW())); break;
    case 49: 
      i_and(readM(readBWY())); break;
    case 50: 
      i_and(readM(readBW())); break;
    case 35: 
      i_and(readM(readBS())); break;
    case 51: 
      i_and(readM(readBSWY())); break;
    case 39: 
      i_and(readM(readBR())); break;
    case 55: 
      i_and(readM(readBRWY())); break;
    case 10: 
      this.flagC = ((this.regA & negM()) > 0);
      this.regA = (this.regA << 1 & maskM());upNZ();
      break;
    case 6: 
      i_asl(readB()); break;
    case 22: 
      i_asl(readBX()); break;
    case 14: 
      i_asl(readW()); break;
    case 30: 
      i_asl(readWX()); break;
    case 144: 
      i_brc(!this.flagC); break;
    case 176: 
      i_brc(this.flagC); break;
    case 240: 
      i_brc(this.flagZ); break;
    case 48: 
      i_brc(this.flagN); break;
    case 208: 
      i_brc(!this.flagZ); break;
    case 16: 
      i_brc(!this.flagN); break;
    case 80: 
      i_brc(!this.flagO); break;
    case 112: 
      i_brc(this.flagO); break;
    case 128: 
      i_brc(true); break;
    case 137: 
      this.flagZ = ((readM() & this.regA) == 0); break;
    case 36: 
      i_bit(readM(readB())); break;
    case 52: 
      i_bit(readM(readBX())); break;
    case 44: 
      i_bit(readM(readW())); break;
    case 60: 
      i_bit(readM(readWX())); break;
    case 0: // BRK 
      push2(this.regPC);
      push1(getFlags());
      this.flagBRK = true;
      this.regPC = this.addrBRK;
      break;
    case 24: 
      this.flagC = false; break;
    case 216: 
      this.flagD = false; break;
    case 88: 
      this.flagID = false; break;
    case 184: 
      this.flagO = false; break;
    case 201: 
      i_cmp(this.regA, readM()); break;
    case 197: 
      i_cmp(this.regA, readM(readB())); break;
    case 213: 
      i_cmp(this.regA, readM(readBX())); break;
    case 205: 
      i_cmp(this.regA, readM(readW())); break;
    case 221: 
      i_cmp(this.regA, readM(readWX())); break;
    case 217: 
      i_cmp(this.regA, readM(readWY())); break;
    case 193: 
      i_cmp(this.regA, readM(readBXW())); break;
    case 209: 
      i_cmp(this.regA, readM(readBWY())); break;
    case 210: 
      i_cmp(this.regA, readM(readBW())); break;
    case 195: 
      i_cmp(this.regA, readM(readBS())); break;
    case 211: 
      i_cmp(this.regA, readM(readBSWY())); break;
    case 199: 
      i_cmp(this.regA, readM(readBR())); break;
    case 215: 
      i_cmp(this.regA, readM(readBRWY())); break;
    case 224: 
      i_cmpx(this.regX, readX()); break;
    case 228: 
      i_cmpx(this.regX, readX(readB())); break;
    case 236: 
      i_cmpx(this.regX, readX(readW())); break;
    case 192: 
      i_cmpx(this.regY, readX()); break;
    case 196: 
      i_cmpx(this.regY, readX(readB())); break;
    case 204: 
      i_cmpx(this.regY, readX(readW())); break;
    case 58: 
      this.regA = (this.regA - 1 & maskM());upNZ(this.regA); break;
    case 198: 
      i_dec(readB()); break;
    case 214: 
      i_dec(readBX()); break;
    case 206: 
      i_dec(readW()); break;
    case 222: 
      i_dec(readWX()); break;
    case 202: 
      this.regX = (this.regX - 1 & maskX());upNZ(this.regX); break;
    case 136: 
      this.regY = (this.regY - 1 & maskX());upNZ(this.regY); break;
    case 73: 
      i_eor(readM()); break;
    case 69: 
      i_eor(readM(readB())); break;
    case 85: 
      i_eor(readM(readBX())); break;
    case 77: 
      i_eor(readM(readW())); break;
    case 93: 
      i_eor(readM(readWX())); break;
    case 89: 
      i_eor(readM(readWY())); break;
    case 65: 
      i_eor(readM(readBXW())); break;
    case 81: 
      i_eor(readM(readBWY())); break;
    case 82: 
      i_eor(readM(readBW())); break;
    case 67: 
      i_eor(readM(readBS())); break;
    case 83: 
      i_eor(readM(readBSWY())); break;
    case 71: 
      i_eor(readM(readBR())); break;
    case 87: 
      i_eor(readM(readBRWY())); break;
    case 26: 
      this.regA = (this.regA + 1 & maskM());upNZ(this.regA); break;
    case 230: 
      i_inc(readB()); break;
    case 246: 
      i_inc(readBX()); break;
    case 238: 
      i_inc(readW()); break;
    case 254: 
      i_inc(readWX()); break;
    case 232: 
      this.regX = (this.regX + 1 & maskX());upNZ(this.regX); break;
    case 200: 
      this.regY = (this.regY + 1 & maskX());upNZ(this.regY); break;
    case 76: 
      this.regPC = readW(); break;
    case 108: 
      this.regPC = readWW(); break;
    case 124: 
      this.regPC = readWXW(); break;
    case 32: 
      push2(this.regPC + 1);this.regPC = readW(); break;
    case 252: 
      push2(this.regPC + 1);this.regPC = readWXW(); break;
    case 169: 
      this.regA = readM();upNZ(); break;
    case 165: 
      this.regA = readM(readB());upNZ(); break;
    case 181: 
      this.regA = readM(readBX());upNZ(); break;
    case 173: 
      this.regA = readM(readW());upNZ(); break;
    case 189: 
      this.regA = readM(readWX());upNZ(); break;
    case 185: 
      this.regA = readM(readWY());upNZ(); break;
    case 161: 
      this.regA = readM(readBXW());upNZ(); break;
    case 177: 
      this.regA = readM(readBWY());upNZ(); break;
    case 178: 
      this.regA = readM(readBW());upNZ(); break;
    case 163: 
      this.regA = readM(readBS());upNZ(); break;
    case 179: 
      this.regA = readM(readBSWY());upNZ(); break;
    case 167: 
      this.regA = readM(readBR());upNZ(); break;
    case 183: 
      this.regA = readM(readBRWY());upNZ(); break;
    case 162: 
      this.regX = readX();upNZ(this.regX); break;
    case 166: 
      this.regX = readX(readB());upNZ(this.regX); break;
    case 182: 
      this.regX = readX(readBY());upNZ(this.regX); break;
    case 174: 
      this.regX = readX(readW());upNZ(this.regX); break;
    case 190: 
      this.regX = readX(readWY());upNZ(this.regX); break;
    case 160: 
      this.regY = readX();upNZ(this.regY); break;
    case 164: 
      this.regY = readX(readB());upNZ(this.regY); break;
    case 180: 
      this.regY = readX(readBX());upNZ(this.regY); break;
    case 172: 
      this.regY = readX(readW());upNZ(this.regY); break;
    case 188: 
      this.regY = readX(readWX());upNZ(this.regY); break;
    case 74: 
      this.flagC = ((this.regA & 0x1) > 0);
      this.regA >>>= 1;upNZ();
      break;
    case 70: 
      i_lsr(readB()); break;
    case 86: 
      i_lsr(readBX()); break;
    case 78: 
      i_lsr(readW()); break;
    case 94: 
      i_lsr(readWX()); break;
    case 234: 
      break;
    case 9: 
      i_or(readM()); break;
    case 5: 
      i_or(readM(readB())); break;
    case 21: 
      i_or(readM(readBX())); break;
    case 13: 
      i_or(readM(readW())); break;
    case 29: 
      i_or(readM(readWX())); break;
    case 25: 
      i_or(readM(readWY())); break;
    case 1: 
      i_or(readM(readBXW())); break;
    case 17: 
      i_or(readM(readBWY())); break;
    case 18: 
      i_or(readM(readBW())); break;
    case 3: 
      i_or(readM(readBS())); break;
    case 19: 
      i_or(readM(readBSWY())); break;
    case 7: 
      i_or(readM(readBR())); break;
    case 23: 
      i_or(readM(readBRWY())); break;
    case 72: 
      pushM(this.regA); break;
    case 8: 
      push1(getFlags()); break;
    case 218: 
      pushX(this.regX); break;
    case 90: 
      pushX(this.regY); break;
    case 104: 
      this.regA = popM();upNZ(); break;
    case 40: 
      setFlags(pop1()); break;
    case 250: 
      this.regX = popX();upNZX(this.regX); break;
    case 122: 
      this.regY = popX();upNZX(this.regY); break;
    case 42: 
      n = (this.regA << 1 | (this.flagC ? 1 : 0)) & maskM();
      this.flagC = ((this.regA & negM()) > 0);
      this.regA = n;upNZ();
      break;
    case 38: 
      i_rol(readB()); break;
    case 54: 
      i_rol(readBX()); break;
    case 46: 
      i_rol(readW()); break;
    case 62: 
      i_rol(readWX()); break;
    case 106: 
      n = this.regA >>> 1 | (this.flagC ? negM() : 0);
      this.flagC = ((this.regA & 0x1) > 0);
      this.regA = n;upNZ();
      break;
    case 102: 
      i_ror(readB()); break;
    case 118: 
      i_ror(readBX()); break;
    case 110: 
      i_ror(readW()); break;
    case 126: 
      i_ror(readWX()); break;
    case 64: 
      setFlags(pop1());
      this.regPC = pop2();
      break;
    case 96: 
      this.regPC = (pop2() + 1); break;
    case 233: 
      i_sbc(readM()); break;
    case 229: 
      i_sbc(readM(readB())); break;
    case 245: 
      i_sbc(readM(readBX())); break;
    case 237: 
      i_sbc(readM(readW())); break;
    case 253: 
      i_sbc(readM(readWX())); break;
    case 249: 
      i_sbc(readM(readWY())); break;
    case 225: 
      i_sbc(readM(readBXW())); break;
    case 241: 
      i_sbc(readM(readBWY())); break;
    case 242: 
      i_sbc(readM(readBW())); break;
    case 227: 
      i_sbc(readM(readBS())); break;
    case 243: 
      i_sbc(readM(readBSWY())); break;
    case 231: 
      i_sbc(readM(readBR())); break;
    case 247: 
      i_sbc(readM(readBRWY())); break;
    case 56: 
      this.flagC = true; break;
    case 248: 
      this.flagD = true; break;
    case 120: 
      this.flagID = true; break;
    case 133: 
      writeM(readB(), this.regA); break;
    case 149: 
      writeM(readBX(), this.regA); break;
    case 141: 
      writeM(readW(), this.regA); break;
    case 157: 
      writeM(readWX(), this.regA); break;
    case 153: 
      writeM(readWY(), this.regA); break;
    case 129: 
      writeM(readBXW(), this.regA); break;
    case 145: 
      writeM(readBWY(), this.regA); break;
    case 146: 
      writeM(readBW(), this.regA); break;
    case 131: 
      writeM(readBS(), this.regA); break;
    case 147: 
      writeM(readBSWY(), this.regA); break;
    case 135: 
      writeM(readBR(), this.regA); break;
    case 151: 
      writeM(readBRWY(), this.regA); break;
    case 134: 
      writeX(readB(), this.regX); break;
    case 150: 
      writeX(readBY(), this.regX); break;
    case 142: 
      writeX(readW(), this.regX); break;
    case 132: 
      writeX(readB(), this.regY); break;
    case 148: 
      writeX(readBX(), this.regY); break;
    case 140: 
      writeX(readW(), this.regY); break;
    case 170: 
      this.regX = this.regA;
      if (this.flagX) {
        this.regX &= 0xFF;
      }
      upNZX(this.regX); break;
    case 168: 
      this.regY = this.regA;
      if (this.flagX) {
        this.regY &= 0xFF;
      }
      upNZX(this.regY); break;
    case 186: 
      this.regX = this.regSP;
      if (this.flagX) {
        this.regX &= 0xFF;
      }
      upNZX(this.regX); break;
    case 138: 
      this.regA = this.regX;
      if (this.flagM) {
        this.regA &= 0xFF;
      }
      upNZ(); break;
    case 154: 
      if (this.flagX) {
        this.regSP = (this.regSP & 0xFF00 | this.regX & 0xFF);
      } else {
        this.regSP = this.regX;
      }
      upNZX(this.regX);
      break;
    case 152: 
      this.regA = this.regY;
      if (this.flagM) {
        this.regA &= 0xFF;
      }
      upNZX(this.regY); break;
    case 100: 
      writeM(readB(), 0); break;
    case 116: 
      writeM(readBX(), 0); break;
    case 156: 
      writeM(readW(), 0); break;
    case 158: 
      writeM(readWX(), 0); break;
    case 20: 
      i_trb(readM(readB())); break;
    case 28: 
      i_trb(readM(readW())); break;
    case 4: 
      i_tsb(readM(readB())); break;
    case 12: 
      i_tsb(readM(readW())); break;
    case 219: 
      this.sliceCycles = -1;
      if (this.k.c(this.l, this.m + 1, this.n))
      {
        this.k.a(this.l + 0.5D, this.m + 0.5D, this.n + 0.5D, "fire.ignite", 1.0F, this.k.t.nextFloat() * 0.4F + 0.8F);
        
        this.k.e(this.l, this.m + 1, this.n, amq.au.cm);
      }
      break;
    case 203: 
      this.waiTimeout = true;
      break;
    case 155: 
      this.regY = this.regX;upNZX(this.regY); break;
    case 187: 
      this.regX = this.regY;upNZX(this.regX); break;
    case 244: 
      push2(readW()); break;
    case 212: 
      push2(readBW()); break;
    case 98: 
      n = readB();push2(this.regPC + n); break;
    case 235: 
      if (this.flagM)
      {
        n = this.regA;this.regA = this.regB;this.regB = n;
      }
      else
      {
        this.regA = (this.regA >> 8 & 0xFF | this.regA << 8 & 0xFF00);
      }
      break;
    case 251: 
      if (this.flagE != this.flagC) {
        if (this.flagE)
        {
          this.flagE = false;this.flagC = true;
        }
        else
        {
          this.flagE = true;this.flagC = false;
          if (!this.flagM) {
            this.regB = (this.regA >> 8);
          }
          this.flagM = true;this.flagX = true;
          this.regA &= 0xFF;this.regX &= 0xFF;this.regY &= 0xFF;
        }
      }
      break;
    case 194: 
      setFlags(getFlags() & (readB() ^ 0xFFFFFFFF)); break;
    case 226: 
      setFlags(getFlags() | readB()); break;
    case 139: 
      if (this.flagX) {
        this.regSP = (this.regR & 0xFF00 | this.regX & 0xFF);
      } else {
        this.regR = this.regX;
      }
      upNZX(this.regR);
      break;
    case 171: 
      this.regX = this.regR;
      if (this.flagX) {
        this.regX &= 0xFF;
      }
      upNZX(this.regX);
      break;
    case 68: 
      push2r(readW()); break;
    case 84: 
      push2r(readBW()); break;
    case 130: 
      int n = readB();push2r(this.regPC + n); break;
    case 59: 
      this.regX = popXr();upNZX(this.regX); break;
    case 107: 
      this.regA = popMr();upNZ(this.regA); break;
    case 123: 
      this.regY = popXr();upNZX(this.regY); break;
    case 27: 
      pushXr(this.regX); break;
    case 75: 
      pushMr(this.regA); break;
    case 91: 
      pushXr(this.regY); break;
    case 11: 
      push2r(this.regI); break;
    case 43: 
      this.regI = pop2r();upNZX(this.regI); break;
    case 92: 
      this.regI = this.regX;upNZX(this.regX); break;
    case 220: 
      this.regX = this.regI;
      if (this.flagX) {
        this.regX &= 0xFF;
      }
      upNZX(this.regX);
      break;
    case 2: 
      this.regPC = readW(this.regI);this.regI += 2;
      break;
    case 66: 
      if (this.flagM)
      {
        this.regA = readMem(this.regI);this.regI += 1;
      }
      else
      {
        this.regA = readW(this.regI);this.regI += 2;
      }
      break;
    case 34: 
      push2r(this.regI);this.regI = (this.regPC + 2);
      this.regPC = readW(this.regPC);
      break;
    case 15: 
      i_mul(readM(readB())); break;
    case 31: 
      i_mul(readM(readBX())); break;
    case 47: 
      i_mul(readM(readW())); break;
    case 63: 
      i_mul(readM(readWX())); break;
    case 79: 
      i_div(readM(readB())); break;
    case 95: 
      i_div(readM(readBX())); break;
    case 111: 
      i_div(readM(readW())); break;
    case 127: 
      i_div(readM(readWX())); break;
    case 143: 
      this.regD = 0;this.regB = 0;
      break;
    case 159: 
      this.regD = ((this.regA & negM()) > 0 ? 65535 : 0);
      this.regB = (this.regD & 0xFF);
      break;
    case 175: 
      this.regA = this.regD;
      if (this.flagM) {
        this.regA &= 0xFF;
      }
      upNZ(this.regA);
      break;
    case 191: 
      if (this.flagM) {
        this.regD = (this.regA | this.regB << 8);
      } else {
        this.regD = this.regA;
      }
      upNZ(this.regA);
      break;
    case 207: 
      this.regD = popM(); break;
    case 223: 
      pushM(this.regD); break;
    case 239: 
      i_mmu(readB());
      break;
    }
  }
  
  public byte[] getFramePacket()
  {
    Packet211TileDesc pkt = new Packet211TileDesc();
    pkt.subId = 7;
    writeToPacket(pkt);
    pkt.headout.write(pkt.subId);
    return pkt.toByteArray();
  }
  
  public void handleFramePacket(byte[] ba)
    throws IOException
  {
    Packet211TileDesc pkt = new Packet211TileDesc(ba);
    pkt.subId = pkt.getByte();
    readFromPacket(pkt);
  }
  
  public void onFrameRefresh(ym iba) {}
  
  public void onFramePickup(ym iba) {}
  
  public void onFrameDrop() {}
  
  public void a(bq tag)
  {
    super.a(tag);
    
    this.memory = tag.j("ram");
    if (this.memory.length != 8192) {
      this.memory = new byte['?'];
    }
    this.Rotation = tag.c("rot");
    this.addrPOR = (tag.d("por") & 0xFFFF);
    this.addrBRK = (tag.d("brk") & 0xFFFF);
    
    int efl = tag.c("efl");
    this.flagE = ((efl & 0x1) > 0);
    this.mmuEnRB = ((efl & 0x2) > 0);
    this.mmuEnRBW = ((efl & 0x4) > 0);
    setFlags(tag.c("fl"));
    
    this.regSP = (tag.d("rsp") & 0xFFFF);
    this.regPC = (tag.d("rpc") & 0xFFFF);
    this.regA = (tag.d("ra") & 0xFFFF);
    if (this.flagM)
    {
      this.regB = (this.regA >> 8);this.regA &= 0xFF;
    }
    this.regX = (tag.d("rx") & 0xFFFF);
    this.regY = (tag.d("ry") & 0xFFFF);
    this.regD = (tag.d("rd") & 0xFFFF);
    this.regR = (tag.d("rr") & 0xFFFF);
    this.regI = (tag.d("ri") & 0xFFFF);
    
    this.mmuRBB = (tag.d("mmrb") & 0xFFFF);
    this.mmuRBW = (tag.d("mmrbw") & 0xFFFF);
    this.mmuRBA = (tag.c("mmra") & 0xFF);
    this.sliceCycles = tag.e("cyc");
    this.rtcTicks = tag.e("rtct");
    
    this.byte0 = (tag.c("b0") & 0xFF);
    this.byte1 = (tag.c("b1") & 0xFF);
    this.rbaddr = (tag.c("rbaddr") & 0xFF);
  }
  
  public void b(bq tag)
  {
    super.b(tag);
    tag.a("rot", (byte)this.Rotation);
    tag.a("ram", this.memory);
    tag.a("por", (short)this.addrPOR);
    tag.a("brk", (short)this.addrBRK);
    
    int efl = (this.flagE ? 1 : 0) | (this.mmuEnRB ? 2 : 0) | (this.mmuEnRBW ? 4 : 0);
    tag.a("efl", (byte)efl);
    tag.a("fl", (byte)getFlags());
    
    tag.a("rsp", (short)this.regSP);
    tag.a("rpc", (short)this.regPC);
    if (this.flagM) {
      this.regA = (this.regA & 0xFF | this.regB << 8);
    }
    tag.a("ra", (short)this.regA);
    if (this.flagM) {
      this.regA &= 0xFF;
    }
    tag.a("rx", (short)this.regX);
    tag.a("ry", (short)this.regY);
    tag.a("rd", (short)this.regD);
    tag.a("rr", (short)this.regR);
    tag.a("ri", (short)this.regI);
    
    tag.a("mmrb", (short)this.mmuRBB);
    tag.a("mmrbw", (short)this.mmuRBW);
    tag.a("mmra", (byte)this.mmuRBA);
    tag.a("cyc", this.sliceCycles);
    tag.a("rtct", this.rtcTicks);
    
    tag.a("b0", (byte)this.byte0);
    tag.a("b1", (byte)this.byte1);
    tag.a("rbaddr", (byte)this.rbaddr);
  }
  
  protected void readFromPacket(Packet211TileDesc pkt)
    throws IOException
  {
    this.Rotation = pkt.getByte();
  }
  
  protected void writeToPacket(Packet211TileDesc pkt)
  {
    pkt.addByte(this.Rotation);
  }
  
  public ef l()
  {
    Packet211TileDesc packet = new Packet211TileDesc();
    packet.subId = 7;
    packet.xCoord = this.l;packet.yCoord = this.m;
    packet.zCoord = this.n;
    writeToPacket(packet);
    packet.encode();
    return packet;
  }
  
  public void handlePacket(Packet211TileDesc packet)
  {
    try
    {
      if (packet.subId != 7) {
        return;
      }
      readFromPacket(packet);
    }
    catch (IOException e) {}
    this.k.i(this.l, this.m, this.n);
  }
  
  public int Rotation = 0;
  public byte[] memory;
  int addrPOR;
  int addrBRK;
  int regSP;
  int regPC;
  int regA;
  int regB;
  int regX;
  int regY;
  int regR;
  int regI;
  int regD;
  boolean flagC;
  boolean flagZ;
  boolean flagID;
  boolean flagD;
  boolean flagBRK;
  boolean flagO;
  boolean flagN;
  boolean flagE;
  boolean flagM;
  boolean flagX;
  int mmuRBB = 0;
  int mmuRBA = 0;
  int mmuRBW = 0;
  boolean mmuEnRB = false;
  boolean mmuEnRBW = false;
  private boolean rbTimeout = false;
  private boolean waiTimeout = false;
  public int sliceCycles = -1;
  IRedbusConnectable rbCache = null;
  public int rtcTicks = 0;
  public int byte0 = 2;
  public int byte1 = 1;
  public int rbaddr = 0;
  TileBackplane[] backplane = new TileBackplane[7];
}
