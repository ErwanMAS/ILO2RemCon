package com.hp.ilo2.virtdevs;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class SCSIcdimage extends SCSI
{
  int fdd_state = 0;
  int event_state = 0;
  long media_sz;
  virtdevs cdi;
  
  public void setWriteProt(boolean paramBoolean)
  {
    this.writeprot = paramBoolean;
  }
  
  public SCSIcdimage(Socket paramSocket, InputStream paramInputStream, BufferedOutputStream paramBufferedOutputStream, String paramString, int paramInt, virtdevs paramvirtdevs) throws IOException
  {
    super(paramSocket, paramInputStream, paramBufferedOutputStream, paramString, paramInt);
    this.cdi = paramvirtdevs;
    int i = this.media.open(paramString, 0);
    D.println(D.INFORM, "Media open returns " + i + " / " + this.media.size() + " bytes");
  }
  
  public void process()
    throws IOException
  {
    D.println(D.INFORM, "Device: " + this.selectedDevice + " (" + this.targetIsDevice + ")");
    read_command(this.req, 12);
    D.println(D.INFORM, "SCSI Request: ");
    D.hexdump(1, this.req, 12);
    
    this.media_sz = this.media.size();
    if (this.media_sz == 0L) {
      this.reply.setmedia(0);
      this.fdd_state = 0;
      
      this.event_state = 4;
    }
    else
    {
      this.reply.setmedia(1);
      this.fdd_state += 1;
      if (this.fdd_state > 2) {
        this.fdd_state = 2;
      }
      if (this.event_state == 4)
        this.event_state = 0;
      this.event_state += 1;
      if (this.event_state > 2) {
        this.event_state = 2;
      }
    }
    switch (this.req[0] & 0xFF) {
    case 30: 
      client_pa_media_removal(this.req);
      break;
    case 37: 
      client_read_capacity();
      break;
    case 29: 
      client_send_diagnostic();
      break;
    case 0: 
      client_test_unit_ready();
      break;
    case 40: 
    case 168: 
      client_read(this.req);
      break;
    case 27: 
      client_start_stop_unit(this.req);
      break;
    case 67: 
      client_read_toc(this.req);
      break;
    case 90: 
      client_mode_sense(this.req);
      break;
    case 74: 
      client_get_event_status(this.req);
      break;
    default: 
      D.println(D.FATAL, "Unknown request:cmd = " + Integer.toHexString(this.req[0]));
      this.reply.set(5, 36, 0, 0);
      this.reply.send(this.out);this.out.flush();
    }
    
  }
  
  void client_send_diagnostic()
    throws IOException
  {}
  
  void client_read(byte[] paramArrayOfByte)
    throws IOException
  {
    int j = paramArrayOfByte[0] == 168 ? 1 : 0;
    
    long l = SCSI.mk_int32(paramArrayOfByte, 2) * 2048L;
    int i = j != 0 ? SCSI.mk_int32(paramArrayOfByte, 6) : SCSI.mk_int16(paramArrayOfByte, 7);
    i *= 2048;
    
    D.println(D.VERBOSE, "CDImage :Client read " + l + ", len=" + i);
    
    if (this.fdd_state == 0) {
      D.println(D.VERBOSE, "media not present");
      this.reply.set(2, 58, 0, 0);
      i = 0;
    } else if (this.fdd_state == 1) {
      D.println(D.VERBOSE, "media changed");
      this.reply.set(6, 40, 0, 0);
      i = 0;
      this.fdd_state = 2;

    }
    else if ((l >= 0L) && (l < this.media_sz)) {
      this.media.read(l, i, this.buffer);
      this.reply.set(0, 0, 0, i);
    }
    else {
      this.reply.set(5, 33, 0, 0);
      i = 0;
    }
    

    this.reply.send(this.out);
    if (i != 0)
      this.out.write(this.buffer, 0, i);
    this.out.flush();
  }
  
  void client_pa_media_removal(byte[] paramArrayOfByte)
    throws IOException
  {
    if ((paramArrayOfByte[4] & 0x1) != 0)
    {



      D.println(D.VERBOSE, "Media removal prevented");
    } else {
      D.println(D.VERBOSE, "Media removal allowed");
    }
    this.reply.set(0, 0, 0, 0);
    this.reply.send(this.out);
    this.out.flush();
  }
  





  void client_start_stop_unit(byte[] paramArrayOfByte)
    throws IOException
  {
    int i = (byte)(paramArrayOfByte[4] & 0x3);
    
    if (i == 3) {
      if (this.cdi.cdConnection != null) {
        this.fdd_state = 1;
        this.event_state = 2;
      }
      else {
        this.fdd_state = 0;
        this.event_state = 4;
      }
    }
    else if (i == 2) {
      this.fdd_state = 0;
      
      this.event_state = 4;
      if (this.cdi.cdConnection != null) {
        this.cdi.do_cdrom(this.cdi.cdSelected);
      }
      
      D.println(D.VERBOSE, "Media eject");
    }
    this.reply.set(0, 0, 0, 0);
    this.reply.send(this.out);
    this.out.flush();
  }
  
  void client_test_unit_ready() throws IOException
  {
    if (this.fdd_state == 0) {
      D.println(D.VERBOSE, "media not present");
      this.reply.set(2, 58, 0, 0);
    } else if (this.fdd_state == 1) {
      D.println(D.VERBOSE, "media changed");
      this.reply.set(6, 40, 0, 0);
      this.fdd_state = 2;
    } else {
      D.println(D.VERBOSE, "device ready");
      this.reply.set(0, 0, 0, 0);
    }
    this.reply.send(this.out);
    this.out.flush();
  }
  
  void client_read_capacity() throws IOException
  {
    byte[] arrayOfByte = { 0, 0, 0, 0, 0, 0, 0, 0 };
    

    this.reply.set(0, 0, 0, arrayOfByte.length);
    if (this.fdd_state == 0)
    {
      this.reply.set(2, 58, 0, 0);
    } else if (this.fdd_state == 1)
    {
      this.reply.set(6, 40, 0, 0);
    } else {
      int i = (int)(this.media.size() / 2048L - 1L);
      arrayOfByte[0] = ((byte)(i >> 24 & 0xFF));
      arrayOfByte[1] = ((byte)(i >> 16 & 0xFF));
      arrayOfByte[2] = ((byte)(i >> 8 & 0xFF));
      arrayOfByte[3] = ((byte)(i >> 0 & 0xFF));
      arrayOfByte[6] = 8;
    }
    this.reply.send(this.out);
    if (this.fdd_state == 2)
      this.out.write(arrayOfByte, 0, arrayOfByte.length);
    this.out.flush();
    D.println(D.VERBOSE, "client_read_capacity: ");
    D.hexdump(3, arrayOfByte, 8);
  }
  
  void client_read_toc(byte[] paramArrayOfByte) throws IOException
  {
    int i = (paramArrayOfByte[1] & 0x2) != 0 ? 1 : 0;
    int j = (paramArrayOfByte[9] & 0xC0) >> 6;
    int k = (int)(this.media.size() / 2048L);
    double d = k / 75.0D + 2.0D;
    int m = (int)d / 60;
    int n = (int)d % 60;
    int i1 = (int)((d - (int)d) * 75.0D);
    int i2 = SCSI.mk_int16(paramArrayOfByte, 7);
    
    for (int i3 = 0; i3 < i2; i3++) {
      this.buffer[i3] = 0;
    }
    if (j == 0) {
      this.buffer[0] = 0;
      this.buffer[1] = 18;
      this.buffer[2] = 1;
      this.buffer[3] = 1;
      
      this.buffer[4] = 0;
      this.buffer[5] = 20;
      this.buffer[6] = 1;
      this.buffer[7] = 0;
      this.buffer[8] = 0;
      this.buffer[9] = 0;
      this.buffer[10] = (byte)(i != 0 ? 2 : 0);
      this.buffer[11] = 0;
      
      this.buffer[12] = 0;
      this.buffer[13] = 20;
      this.buffer[14] = -86;
      this.buffer[15] = 0;
      this.buffer[16] = 0;
      this.buffer[17] = (i != 0 ? (byte)m : (byte)(k >> 16 & 0xFF));
      this.buffer[18] = (i != 0 ? (byte)n : (byte)(k >> 8 & 0xFF));
      this.buffer[19] = (i != 0 ? (byte)i1 : (byte)(k & 0xFF));
    }
    
    if (j == 1) {
      this.buffer[0] = 0;
      this.buffer[1] = 10;
      this.buffer[2] = 1;
      this.buffer[3] = 1;
      
      this.buffer[4] = 0;
      this.buffer[5] = 20;
      this.buffer[6] = 1;
      this.buffer[7] = 0;
      this.buffer[8] = 0;
      this.buffer[9] = 0;
      this.buffer[10] = (byte)(i != 0 ? 2 : 0);
      this.buffer[11] = 0;
    }
    


    k = 412;
    if (i2 < k) k = i2;
    D.hexdump(3, this.buffer, k);
    this.reply.set(0, 0, 0, k);
    this.reply.send(this.out);
    this.out.write(this.buffer, 0, k);
    this.out.flush();
  }
  
  void client_mode_sense(byte[] paramArrayOfByte) throws IOException
  {
    this.buffer[0] = 0;
    this.buffer[1] = 8;
    this.buffer[2] = 1;
    this.buffer[3] = 0;
    this.buffer[4] = 0;
    this.buffer[5] = 0;
    this.buffer[6] = 0;
    this.buffer[7] = 0;
    this.reply.set(0, 0, 0, 8);
    D.hexdump(3, this.buffer, 8);
    this.reply.setmedia(this.buffer[2]);
    this.reply.send(this.out);
    this.out.write(this.buffer, 0, 8);
    this.out.flush();
  }
  
  void client_get_event_status(byte[] paramArrayOfByte) throws IOException
  {
    int i = paramArrayOfByte[4];
    int j = SCSI.mk_int16(paramArrayOfByte, 7);
    for (int k = 0; k < j; k++) {
      this.buffer[k] = 0;
    }
    

    if ((paramArrayOfByte[1] & 0x1) == 0)
    {
      this.reply.set(5, 36, 0, 0);
      this.reply.send(this.out);this.out.flush();
    }
    







    if ((i & 0x10) != 0)
    {


      this.buffer[0] = 0;
      this.buffer[1] = 6;
      
      this.buffer[2] = 4;
      
      this.buffer[3] = 16;
      if (this.event_state == 0)
      {
        this.buffer[4] = 0;
        this.buffer[5] = 0;
      } else if (this.event_state == 1)
      {
        this.buffer[4] = 4;
        this.buffer[5] = 2;
        

        if (j > 4)
          this.event_state = 2;
      } else if (this.event_state == 4)
      {
        this.buffer[4] = 3;
        this.buffer[5] = 0;
        

        if (j > 4) {
          this.event_state = 0;
        }
      } else {
        this.buffer[4] = 0;
        this.buffer[5] = 2;
      }
      

      D.hexdump(3, this.buffer, 8);
      this.reply.set(0, 0, 0, j < 8 ? j : 8);
      this.reply.send(this.out);
      this.out.write(this.buffer, 0, j < 8 ? j : 8);
      this.out.flush();

    }
    else
    {
      this.buffer[0] = 0;
      this.buffer[1] = 2;
      
      this.buffer[2] = Byte.MIN_VALUE;
      
      this.buffer[3] = 16;
      D.hexdump(3, this.buffer, 4);
      this.reply.set(0, 0, 0, j < 4 ? j : 4);
      this.reply.send(this.out);
      this.out.write(this.buffer, 0, j < 4 ? j : 4);
      this.out.flush();
    }
  }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/SCSIcdimage.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */