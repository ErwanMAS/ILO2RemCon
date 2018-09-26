package com.hp.ilo2.virtdevs;

import java.awt.Component;
import java.io.IOException;
import java.net.Socket;
import javax.swing.Timer;

public class Connection implements Runnable, java.awt.event.ActionListener
{
  public static final int FLOPPY = 1;
  public static final int CDROM = 2;
  public static final int USBKEY = 3;
  Socket s;
  java.io.InputStream in;
  java.io.BufferedOutputStream out;
  String host;
  int port;
  int device;
  String target;
  int targetIsDevice;
  SCSI scsi;
  boolean writeprot = false;
  
  virtdevs v;
  
  byte[] pre;
  byte[] key;
  boolean changing_disks;
  VMD5 digest;
  
  public Connection(String paramString1, int paramInt1, int paramInt2, String paramString2, int paramInt3, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, virtdevs paramvirtdevs)
    throws IOException
  {
    this.host = paramString1;
    this.port = paramInt1;
    this.device = paramInt2;
    this.target = paramString2;
    this.pre = paramArrayOfByte1;
    this.key = paramArrayOfByte2;
    this.v = paramvirtdevs;
    
    MediaAccess localMediaAccess = new MediaAccess();
    int i = localMediaAccess.devtype(paramString2);
    if ((i == 2) || (i == 5)) {
      this.targetIsDevice = 1;
      D.println(0, "Got CD or removable connection\n");
    } else {
      this.targetIsDevice = 0;
      D.println(0, "Got NO CD or removable connection\n");
    }
    
    int j = localMediaAccess.open(paramString2, this.targetIsDevice);
    long l = localMediaAccess.size();
    localMediaAccess.close();
    
    if ((this.device == 1) && (l > 2949120L)) {
      this.device = 3;
    }
    this.digest = new VMD5();
  }
  
  public int connect() throws java.net.UnknownHostException, IOException
  {
    byte[] arrayOfByte1 = { 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    


    this.s = new Socket(this.host, this.port);
    









    this.s.setTcpNoDelay(true);
    this.in = this.s.getInputStream();
    this.out = new java.io.BufferedOutputStream(this.s.getOutputStream());
    

    this.digest.reset();
    this.digest.update(this.pre);
    this.digest.update(this.key);
    byte[] arrayOfByte2 = this.digest.digest();
    System.arraycopy(arrayOfByte2, 0, arrayOfByte1, 2, this.key.length);
    System.arraycopy(arrayOfByte2, 0, this.key, 0, this.key.length);
    
    arrayOfByte1[1] = ((byte)this.device);
    if (this.targetIsDevice == 0)
    {


      int tmp227_226 = 1; byte[] tmp227_225 = arrayOfByte1;tmp227_225[tmp227_226] = ((byte)(tmp227_225[tmp227_226] | 0xFFFFFF80));
    }
    this.out.write(arrayOfByte1);
    this.out.flush();
    
    this.in.read(arrayOfByte1, 0, 4);
    D.println(3, "Hello response0: " + D.hex(arrayOfByte1[0], 2));
    D.println(3, "Hello response1: " + D.hex(arrayOfByte1[1], 2));
    


    if ((arrayOfByte1[0] == 32) && (arrayOfByte1[1] == 0))
    {



      D.println(1, "Connected.  Protocol version = " + (arrayOfByte1[3] & 0xFF) + "." + (arrayOfByte1[2] & 0xFF));
    }
    else {
      D.println(0, "Unexpected Hello Response!");
      this.s.close();
      this.s = null;
      this.in = null;this.out = null;
      return arrayOfByte1[0];
    }
    return 0;
  }
  
  public void close() throws IOException
  {
    if (this.scsi != null) {
      try {
        Timer localTimer = new Timer(2000, this);
        localTimer.setRepeats(false);
        localTimer.start();
        this.scsi.change_disk();
        localTimer.stop();
      }
      catch (Exception localException)
      {
        this.scsi.change_disk();
      }
    } else {
      internal_close();
    }
  }
  
  public void actionPerformed(java.awt.event.ActionEvent paramActionEvent)
  {
    try {
      internal_close();
    }
    catch (Exception localException) {}
  }
  
  public void internal_close() throws IOException {
    if (this.s != null) this.s.close();
    this.s = null;
    this.in = null;
    this.out = null;
  }
  
  public void setWriteProt(boolean paramBoolean)
  {
    this.writeprot = paramBoolean;
    if (this.scsi != null) {
      this.scsi.setWriteProt(this.writeprot);
    }
  }
  
  public void change_disk(String paramString)
    throws IOException
  {
    MediaAccess localMediaAccess = new MediaAccess();
    int j = localMediaAccess.devtype(paramString);
    int i; if ((j == 2) || (j == 5)) {
      i = 1;
    } else {
      i = 0;
    }
    if (i == 0) {
      int k = localMediaAccess.open(paramString, 0);
      localMediaAccess.close();
    }
    this.target = paramString;
    this.targetIsDevice = i;
    this.changing_disks = true;
    this.scsi.change_disk();
  }
  
  public void run()
  {
    do {
      this.changing_disks = false;
      try {
        if ((this.device == 1) || (this.device == 3)) {
          this.scsi = new SCSIFloppy(this.s, this.in, this.out, this.target, this.targetIsDevice);
        } else if (this.device == 2) {
          if (this.targetIsDevice == 1) {
            this.scsi = new SCSIcdrom(this.s, this.in, this.out, this.target, 1);
          } else {
            this.scsi = new SCSIcdimage(this.s, this.in, this.out, this.target, 0, this.v);
          }
        } else {
          D.println(0, "Unsupported virtual device " + this.device);
          
          return;
        }
      } catch (Exception localException) {
        D.println(0, "Exception while opening " + this.target + "(" + localException + ")");
      }
      

      this.scsi.setWriteProt(this.writeprot);
      for (;;) {
        if (((this.device == 1) || (this.device == 3)) && (this.scsi.getWriteProt())) {
          this.v.roCbox.setState(true);
          this.v.roCbox.setEnabled(false);
          this.v.roCbox.repaint();
        }
        try {
          this.scsi.process();
        } catch (IOException localIOException1) {
          D.println(1, "Exception in Connection::run() " + localIOException1);
          localIOException1.printStackTrace();
          



          D.println(3, "Closing scsi and socket");
          try {
            this.scsi.close();
            if (!this.changing_disks) internal_close();
          } catch (IOException localIOException2) {
            D.println(0, "Exception closing connection " + localIOException2);
          }
          this.scsi = null;
          if ((this.device == 1) || (this.device == 3)) {
            this.v.roCbox.setEnabled(true);
            this.v.roCbox.repaint();
          }
        }
      }
    } while (this.changing_disks);
    /*if ((this.device == 1) || (this.device == 3)) {
      this.v.fdDisconnect();
    } else if (this.device == 2) {
      this.v.cdDisconnect();
    }*/
  }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/Connection.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */