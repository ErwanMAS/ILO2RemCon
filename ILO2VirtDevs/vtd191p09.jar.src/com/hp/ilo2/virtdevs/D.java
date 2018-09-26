package com.hp.ilo2.virtdevs;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Properties;





public class D
{
  public static final int NONE = -1;
  public static final int FATAL = 0;
  public static final int INFORM = 1;
  public static final int WARNING = 2;
  public static final int VERBOSE = 3;
  public static int debug = 0;
  public static PrintStream out;
  
  public static void println(int paramInt, String paramString) {
    if (debug >= paramInt) out.println(paramString);
  }
  
  public static void print(int paramInt, String paramString) {
    if (debug >= paramInt) out.println(paramString);
  }
  
  public static String hex(byte paramByte, int paramInt) {
    return hex(paramByte & 0xFF, paramInt);
  }
  
  public static String hex(short paramShort, int paramInt) {
    return hex(paramShort & 0xFFFF, paramInt);
  }
  
  public static String hex(int paramInt1, int paramInt2) {
    String str = Integer.toHexString(paramInt1);
    while (str.length() < paramInt2) {
      str = "0" + str;
    }
    return str;
  }
  
  public static String hex(long paramLong, int paramInt) {
    String str = Long.toHexString(paramLong);
    while (str.length() < paramInt) {
      str = "0" + str;
    }
    return str;
  }
  
  public static void hexdump(int paramInt1, byte[] paramArrayOfByte, int paramInt2)
  {
    if (debug < paramInt1) { return;
    }
    if (paramInt2 == 0) paramInt2 = paramArrayOfByte.length;
    for (int i = 0; i < paramInt2; i++) {
      if (i % 16 == 0)
        out.print("\n");
      out.print(hex(paramArrayOfByte[i], 2) + " ");
    }
    out.print("\n");
  }
  
  static {
    String str = virtdevs.prop.getProperty("com.hp.ilo2.virtdevs.debugfile");
    try
    {
      if (str == null) {
        out = System.out;
      } else {
        out = new PrintStream(new FileOutputStream(str));
      }
    } catch (Exception localException) {
      out = System.out;
      out.println("Exception trying to open debug trace\n" + localException);
    }
    str = virtdevs.prop.getProperty("com.hp.ilo2.virtdevs.debug");
    
    if (str != null) {
      debug = Integer.valueOf(str).intValue();
    }
  }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/D.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */