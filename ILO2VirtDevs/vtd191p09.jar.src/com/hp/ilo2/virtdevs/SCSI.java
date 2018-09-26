package com.hp.ilo2.virtdevs;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Socket;




public abstract class SCSI
{
  public static final int SCSI_FORMAT_UNIT = 4;
  public static final int SCSI_INQUIRY = 18;
  public static final int SCSI_MODE_SELECT_6 = 21;
  public static final int SCSI_MODE_SELECT = 85;
  public static final int SCSI_MODE_SENSE_6 = 26;
  public static final int SCSI_MODE_SENSE = 90;
  public static final int SCSI_PA_MEDIA_REMOVAL = 30;
  public static final int SCSI_READ_10 = 40;
  public static final int SCSI_READ_12 = 168;
  public static final int SCSI_READ_CAPACITY = 37;
  public static final int SCSI_READ_CAPACITIES = 35;
  public static final int SCSI_REQUEST_SENSE = 3;
  public static final int SCSI_REZERO_UNIT = 1;
  public static final int SCSI_SEEK = 43;
  public static final int SCSI_SEND_DIAGNOSTIC = 29;
  public static final int SCSI_START_STOP_UNIT = 27;
  public static final int SCSI_TEST_UNIT_READY = 0;
  public static final int SCSI_VERIFY = 47;
  public static final int SCSI_WRITE_10 = 42;
  public static final int SCSI_WRITE_12 = 170;
  public static final int SCSI_WRITE_VERIFY = 46;
  public static final int SCSI_READ_CD = 190;
  public static final int SCSI_READ_CD_MSF = 185;
  public static final int SCSI_READ_HEADER = 68;
  public static final int SCSI_READ_SUBCHANNEL = 66;
  public static final int SCSI_READ_TOC = 67;
  public static final int SCSI_STOP_PLAY_SCAN = 78;
  public static final int SCSI_MECHANISM_STATUS = 189;
  public static final int SCSI_GET_EVENT_STATUS = 74;
  MediaAccess media = new MediaAccess();
  ReplyHeader reply = new ReplyHeader();
  

  String selectedDevice;
  
  protected InputStream in;
  
  protected BufferedOutputStream out;
  
  protected Socket sock;
  
  boolean writeprot = false;
  boolean please_exit = false;
  int targetIsDevice = 0;
  byte[] buffer = new byte[131072];
  byte[] req = new byte[12];
  
  public void setWriteProt(boolean paramBoolean)
  {
    this.writeprot = paramBoolean;
  }
  
  public boolean getWriteProt()
  {
    D.println(3, "media.wp = " + this.media.wp());
    return this.media.wp();
  }
  
  public SCSI(Socket paramSocket, InputStream paramInputStream, BufferedOutputStream paramBufferedOutputStream, String paramString, int paramInt)
  {
    this.sock = paramSocket;
    this.in = paramInputStream;
    this.out = paramBufferedOutputStream;
    this.selectedDevice = paramString;
    this.targetIsDevice = paramInt;
  }
  
  public void close() throws IOException
  {
    this.media.close();
  }
  
  public static int mk_int32(byte[] paramArrayOfByte, int paramInt)
  {
    int i = paramArrayOfByte[(paramInt + 0)];
    int j = paramArrayOfByte[(paramInt + 1)];
    int k = paramArrayOfByte[(paramInt + 2)];
    int m = paramArrayOfByte[(paramInt + 3)];
    
    int n = (i & 0xFF) << 24 | (j & 0xFF) << 16 | (k & 0xFF) << 8 | m & 0xFF;
    
    return n;
  }
  
  public static int mk_int24(byte[] paramArrayOfByte, int paramInt)
  {
    int i = paramArrayOfByte[(paramInt + 0)];
    int j = paramArrayOfByte[(paramInt + 1)];
    int k = paramArrayOfByte[(paramInt + 2)];
    
    int m = (i & 0xFF) << 16 | (j & 0xFF) << 8 | k & 0xFF;
    
    return m;
  }
  
  public static int mk_int16(byte[] paramArrayOfByte, int paramInt)
  {
    int i = paramArrayOfByte[(paramInt + 0)];
    int j = paramArrayOfByte[(paramInt + 1)];
    int k = (i & 0xFF) << 8 | j & 0xFF;
    return k;
  }
  

  protected int read_complete(byte[] paramArrayOfByte, int paramInt)
    throws IOException
  {
    int i = 0;
    int j = 0;
    while (paramInt > 0)
    {
      try
      {
        this.sock.setSoTimeout(1000);
        j = this.in.read(paramArrayOfByte, i, paramInt);
      }
      catch (InterruptedIOException localInterruptedIOException) {
        continue;
      }
      if (j < 0)
        break;
      paramInt -= j;
      i += j;
    }
    return i;
  }
  
  protected int read_command(byte[] paramArrayOfByte, int paramInt) throws IOException
  {
    int i = 0;
    for (;;) {
      try {
        this.sock.setSoTimeout(1000);
        i = this.in.read(paramArrayOfByte, 0, paramInt);
      } catch (InterruptedIOException localInterruptedIOException) {
        this.reply.keepalive(true);
        D.println(3, "Sending keepalive");
        this.reply.send(this.out);
        this.out.flush();
        this.reply.keepalive(false);
        if (!this.please_exit) {continue;}
      } break;
      /*label81:
      
      if ((paramArrayOfByte[0] & 0xFF) != 254) break;
      this.reply.sendsynch(this.out, paramArrayOfByte);
      this.out.flush();*/
    }
    


    if (this.please_exit) throw new IOException("Asked to exit");
    if (i < 0) throw new IOException("Socket Closed");
    return i;
  }
  
  public abstract void process() throws IOException;
  
  public void change_disk() {
    this.please_exit = true;
  }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/SCSI.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */