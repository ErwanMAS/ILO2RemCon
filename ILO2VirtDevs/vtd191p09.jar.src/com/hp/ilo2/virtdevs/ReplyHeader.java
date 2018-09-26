package com.hp.ilo2.virtdevs;

import java.io.OutputStream;

public class ReplyHeader {
  public static final int magic = 195936478;
  public static final int WP = 1;
  public static final int KEEPALIVE = 2;
  int flags;
  byte sense_key;
  byte asc;
  byte ascq;
  byte media;
  int length;
  byte[] data = new byte[16];
  
  void set(int paramInt1, int paramInt2, int paramInt3, int paramInt4)
  {
    this.sense_key = ((byte)paramInt1);
    this.asc = ((byte)paramInt2);
    this.ascq = ((byte)paramInt3);
    this.length = paramInt4;
  }
  
  void setmedia(int paramInt)
  {
    this.media = ((byte)paramInt);
  }
  
  void setflags(boolean paramBoolean)
  {
    if (paramBoolean) this.flags |= 0x1; else {
      this.flags &= 0xFFFFFFFE;
    }
  }
  
  void keepalive(boolean paramBoolean) {
    if (paramBoolean) this.flags |= 0x2; else {
      this.flags &= 0xFFFFFFFD;
    }
  }
  
  void send(OutputStream paramOutputStream) throws java.io.IOException {
    this.data[0] = -34;
    this.data[1] = -64;
    this.data[2] = -83;
    this.data[3] = 11;
    this.data[4] = ((byte)(this.flags & 0xFF));
    this.data[5] = ((byte)(this.flags >> 8 & 0xFF));
    this.data[6] = ((byte)(this.flags >> 16 & 0xFF));
    this.data[7] = ((byte)(this.flags >> 24 & 0xFF));
    this.data[8] = this.media;
    this.data[9] = this.sense_key;
    this.data[10] = this.asc;
    this.data[11] = this.ascq;
    this.data[12] = ((byte)(this.length & 0xFF));
    this.data[13] = ((byte)(this.length >> 8 & 0xFF));
    this.data[14] = ((byte)(this.length >> 16 & 0xFF));
    this.data[15] = ((byte)(this.length >> 24 & 0xFF));
    paramOutputStream.write(this.data, 0, 16);
  }
  
  void sendsynch(OutputStream paramOutputStream, byte[] paramArrayOfByte) throws java.io.IOException
  {
    this.data[0] = -34;
    this.data[1] = -64;
    this.data[2] = -83;
    this.data[3] = 11;
    this.data[4] = ((byte)(this.flags & 0xFF));
    this.data[5] = ((byte)(this.flags >> 8 & 0xFF));
    this.data[6] = ((byte)(this.flags >> 16 & 0xFF));
    this.data[7] = ((byte)(this.flags >> 24 & 0xFF));
    this.data[8] = paramArrayOfByte[4];
    this.data[9] = paramArrayOfByte[5];
    this.data[10] = paramArrayOfByte[6];
    this.data[11] = paramArrayOfByte[7];
    this.data[12] = paramArrayOfByte[8];
    this.data[13] = paramArrayOfByte[9];
    this.data[14] = paramArrayOfByte[10];
    this.data[15] = paramArrayOfByte[11];
    paramOutputStream.write(this.data, 0, 16);
  }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/ReplyHeader.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */