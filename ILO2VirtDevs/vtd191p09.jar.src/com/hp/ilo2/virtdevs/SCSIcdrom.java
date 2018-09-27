package com.hp.ilo2.virtdevs;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Properties;





public class SCSIcdrom
  extends SCSI
{
  public static final int SCSI_IOCTL_DATA_OUT = 0;
  public static final int SCSI_IOCTL_DATA_IN = 1;
  public static final int SCSI_IOCTL_DATA_UNSPECIFIED = 2;
  public static final int CONST = 0;
  static final int WRITE = 0;
  static final int READ = 16777216;
  static final int NONE = 33554432;
  public static final int BLKS = 8388608;
  static final int B32 = 262144;
  static final int B24 = 196608;
  static final int B16 = 131072;
  static final int B08 = 65536;
  static final int[] commands = { 30, 33554432, 37, 16777224, 29, 33554432, 0, 33554432, 40, 25296903, 168, 25427974, 27, 33554432, 190, 25362438, 185, 16777216, 68, 16777224, 66, 16908295, 67, 16908295, 78, 33554432, 189, 16908296, 90, 16908295, 74, 16908295 };
  

















  byte[] sense = new byte[3];
  int retrycount;
  VErrorDialog dlg;
  boolean do_split_reads = false;
  
  void media_err(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    String str = "The CDROM drive reports a media error:\nCommand: " + D.hex(paramArrayOfByte1[0], 2) + " " + D.hex(paramArrayOfByte1[1], 2) + " " + D.hex(paramArrayOfByte1[2], 2) + " " + D.hex(paramArrayOfByte1[3], 2) + " " + D.hex(paramArrayOfByte1[4], 2) + " " + D.hex(paramArrayOfByte1[5], 2) + " " + D.hex(paramArrayOfByte1[6], 2) + " " + D.hex(paramArrayOfByte1[7], 2) + " " + D.hex(paramArrayOfByte1[8], 2) + " " + D.hex(paramArrayOfByte1[9], 2) + " " + D.hex(paramArrayOfByte1[10], 2) + " " + D.hex(paramArrayOfByte1[11], 2) + "\n" + "Sense Code: " + D.hex(paramArrayOfByte2[0], 2) + "/" + D.hex(paramArrayOfByte2[1], 2) + "/" + D.hex(paramArrayOfByte2[2], 2) + "\n\n";
    
















    if ((this.dlg == null) || (this.dlg.disposed())) {
      this.dlg = new VErrorDialog(str, false);
    } else {
      this.dlg.append(str);
    }
  }
  
  public SCSIcdrom(Socket paramSocket, InputStream paramInputStream, BufferedOutputStream paramBufferedOutputStream, String paramString, int paramInt) throws IOException
  {
    super(paramSocket, paramInputStream, paramBufferedOutputStream, paramString, paramInt);
    

    D.println(D.INFORM, "Media opening " + paramString + "(" + (paramInt | 0x2) + ")");
    
    int i = this.media.open(paramString, paramInt);
    D.println(D.INFORM, "Media open returns " + i);
    this.retrycount = Integer.valueOf(virtdevs.prop.getProperty("com.hp.ilo2.virtdevs.retrycount", "10")).intValue();
  }
  
  public void close()
    throws IOException
  {
    this.req[0] = 30;
    this.req[1] = (this.req[2] = this.req[3] = this.req[4] = this.req[5] = this.req[7] = this.req[7] = this.req[8] = this.req[9] = this.req[10] = this.req[11] = 0);
    


    this.media.scsi(this.req, 2, 0, this.buffer, null);
    super.close();
  }
  
  int scsi_length(int paramInt, byte[] paramArrayOfByte)
  {
    int i = 0;
    paramInt++;
    switch (commands[paramInt] & 0x7F0000) {
    case 0: 
      i = commands[paramInt] & 0xFFFF;
      break;
    case 262144: 
      i = SCSI.mk_int32(paramArrayOfByte, commands[paramInt] & 0xFFFF);
      break;
    case 196608: 
      i = SCSI.mk_int24(paramArrayOfByte, commands[paramInt] & 0xFFFF);
      break;
    case 131072: 
      i = SCSI.mk_int16(paramArrayOfByte, commands[paramInt] & 0xFFFF);
      break;
    case 65536: 
      i = paramArrayOfByte[(commands[paramInt] & 0xFFFF)] & 0xFF;
      break;
    default: 
      D.println(D.FATAL, "Unknown Size!");
    }
    if ((commands[paramInt] & 0x800000) == 8388608)
      i *= 2048;
    return i;
  }
  
  void start_stop_unit()
  {
    byte[] arrayOfByte1 = { 27, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 };
    byte[] arrayOfByte2 = new byte[3];
    

    int i = this.media.scsi(arrayOfByte1, 2, 0, this.buffer, arrayOfByte2);
    D.println(D.VERBOSE, "Start/Stop unit = " + i + " " + arrayOfByte2[0] + "/" + arrayOfByte2[1] + "/" + arrayOfByte2[2]);
  }
  



  boolean within_75(byte[] paramArrayOfByte)
  {
    byte[] arrayOfByte1 = new byte[8];
    byte[] arrayOfByte2 = { 37, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    int i = paramArrayOfByte[0] == 168 ? 1 : 0;
    int j = SCSI.mk_int32(paramArrayOfByte, 2);
    int k = i != 0 ? SCSI.mk_int32(paramArrayOfByte, 6) : SCSI.mk_int16(paramArrayOfByte, 7);
    

    this.media.scsi(arrayOfByte2, 1, 8, arrayOfByte1, null);
    
    int m = SCSI.mk_int32(arrayOfByte1, 0);
    if ((j > m - 75) || (j + k > m - 75))
      return true;
    return false;
  }
  
  int split_read()
  {
    int i = this.req[0] == 168 ? 1 : 0;
    int j = SCSI.mk_int32(this.req, 2);
    int k = i != 0 ? SCSI.mk_int32(this.req, 6) : SCSI.mk_int16(this.req, 7);
    int m = k > 32 ? 32 : k;
    
    int i2 = 1;
    


    this.req[2] = ((byte)(j >> 24));
    this.req[3] = ((byte)(j >> 16));
    this.req[4] = ((byte)(j >> 8));
    this.req[5] = ((byte)j);
    if (i != 0) {
      this.req[6] = ((byte)(m >> 24));
      this.req[7] = ((byte)(m >> 16));
      this.req[8] = ((byte)(m >> 8));
      this.req[9] = ((byte)m);
    } else {
      this.req[7] = ((byte)(m >> 8));
      this.req[8] = ((byte)m);
    }
    int n = this.media.scsi(this.req, i2, m * 2048, this.buffer, this.sense);
    if (n < 0) {
      return n;
    }
    k -= m;
    if (k <= 0) {
      return n;
    }
    

    j += m;
    this.req[2] = ((byte)(j >> 24));
    this.req[3] = ((byte)(j >> 16));
    this.req[4] = ((byte)(j >> 8));
    this.req[5] = ((byte)j);
    if (i != 0) {
      this.req[6] = ((byte)(k >> 24));
      this.req[7] = ((byte)(k >> 16));
      this.req[8] = ((byte)(k >> 8));
      this.req[9] = ((byte)k);
    } else {
      this.req[7] = ((byte)(k >> 8));
      this.req[8] = ((byte)k);
    }
    int i1 = this.media.scsi(this.req, i2, k * 2048, this.buffer, this.sense, 65536);
    if (i1 < 0) {
      return i1;
    }
    return n + i1;
  }
  
  public void process()
    throws IOException
  {
    int j = 0;int k = 0;
    

    read_command(this.req, 12);
    D.println(D.INFORM, "SCSI Request:");
    D.hexdump(1, this.req, 12);
    int n;
    if (this.media.dio.filehandle == -1) {
      n = this.media.open(this.selectedDevice, this.targetIsDevice);
      if (n < 0) {
        new VErrorDialog("Could not open CDROM (" + this.media.dio.sysError(-n) + ")", false);
        throw new IOException("Couldn't open cdrom " + n);
      }
    }
    int i = 0;
    for (; i < commands.length; i += 2) {
      if (this.req[0] == (byte)commands[i])
        break;
    }
    if (i != commands.length) {
      j = scsi_length(i, this.req);
      k = commands[(i + 1)] >> 24;
      i = this.req[0] & 0xFF;
      

      if (k == 0) {
        read_complete(this.buffer, j);
      }
      D.println(D.INFORM, "SCSI dir=" + k + " len=" + j);
      int m = 0;
      do {
        long l1 = System.currentTimeMillis();
        if (((i == 40) || (i == 168)) && (this.do_split_reads))
        {
          n = split_read();
        } else {
          n = this.media.scsi(this.req, k, j, this.buffer, this.sense);
        }
        long l2 = System.currentTimeMillis();
        
        D.println(D.INFORM, "ret=" + n + " sense=" + D.hex(this.sense[0], 2) + " " + D.hex(this.sense[1], 2) + " " + D.hex(this.sense[2], 2) + " Time=" + (l2 - l1));
        



        if (i == 90) {
          D.println(D.INFORM, "media type: " + D.hex(this.buffer[3], 2));
          
          this.reply.setmedia(this.buffer[3]);
        }
        if (i == 67) {
          D.hexdump(3, this.buffer, j);
        }
        
        if (i == 27)
        {

          n = 0;
        }
        

        if ((i == 40) || (i == 168)) {
          if (this.sense[1] == 41)
          {
            n = -1;
          } else if ((n < 0) && (within_75(this.req)))
          {

            this.sense[0] = 5;
            this.sense[1] = 33;
            this.sense[2] = 0;
            n = 0;
          } else if (n < 0)
          {




            this.do_split_reads = true;
          }
        }
        
        if ((this.sense[0] == 3) || (this.sense[0] == 4))
        {
          media_err(this.req, this.sense);
          n = -1;
        }
        
      } while ((n < 0) && (m++ < this.retrycount));
      
      j = n;
      if ((j < 0) || (j > 131072)) {
        D.println(D.FATAL, "AIEE! len out of bounds: " + j + ", cmd: " + D.hex(i, 2) + "\n");
        
        j = 0;
        this.reply.set(5, 32, 0, 0);
      } else {
        this.reply.set(this.sense[0], this.sense[1], this.sense[2], j);
      }
    }
    else {
      D.println(D.FATAL, "AIEE! Unhandled command" + D.hex(this.req[0], 2) + "\n");
      
      this.reply.set(5, 32, 0, 0);
      j = 0;
    }
    this.reply.send(this.out);
    

    if (j != 0)
      this.out.write(this.buffer, 0, j);
    this.out.flush();
  }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/SCSIcdrom.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */