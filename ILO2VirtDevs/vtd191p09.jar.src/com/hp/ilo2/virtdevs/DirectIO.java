package com.hp.ilo2.virtdevs;

import java.io.PrintStream;
import java.util.Properties;















public class DirectIO
{
  public int media_type;
  public int StartCylinder;
  public int EndCylinder;
  public int StartHead;
  public int EndHead;
  public int Cylinders;
  public int TracksPerCyl;
  public int SecPerTrack;
  public int BytesPerSec;
  public int media_size;
  public int filehandle = -1;
  public int aux_handle = -1;
  public long bufferaddr;
  public int wp;
  
  public native int open(String paramString);
  
  public native int close();
  
  public native int read(long paramLong, int paramInt, byte[] paramArrayOfByte);
  
  public native int write(long paramLong, int paramInt, byte[] paramArrayOfByte);
  
  public native long size();
  
  public native int format();
  
  public native String[] devices();
  
  public native int devtype(String paramString);
  
  public native int scsi(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, int paramInt3);
  
  public native String sysError(int paramInt);
  
  protected void finalize() { if (this.filehandle != -1)
      close(); }
  
  public int misc0;
  public int PhysicalDevice;
  public static int keydrive = 1;
  
  static {
    String str1 = "cpqma-" + Integer.toHexString(virtdevs.UID) + MediaAccess.dllext;
    
    String str2 = System.getProperty("file.separator");
    String str3 = System.getProperty("java.io.tmpdir");
    String str4 = System.getProperty("os.name").toLowerCase();
    
    if (str3 == null) {
      str3 = str4.startsWith("windows") ? "C:\\TEMP" : "/tmp";
    }
    
    if (!str3.endsWith(str2)) {
      str3 = str3 + str2;
    }
    str3 = str3 + str1;
    

    str1 = virtdevs.prop.getProperty("com.hp.ilo2.virtdevs.dll");
    String str5 = virtdevs.prop.getProperty("com.hp.ilo2.virtdevs.keydrive", "true");
    keydrive = Boolean.valueOf(str5).booleanValue() ? 1 : 0;
    
    if (str1 != null) str3 = str1;
    System.out.println("Loading " + str3);
    System.load(str3);
  }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/DirectIO.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */