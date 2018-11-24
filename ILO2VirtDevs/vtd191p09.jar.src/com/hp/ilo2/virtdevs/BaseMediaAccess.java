package com.hp.ilo2.virtdevs;

import java.io.IOException;

public abstract class BaseMediaAccess {
    //types returned by DirectIOMediaAccess.devtype(String)
    public static final int Unknown = 0;
    public static final int NoRootDir = 1;
    public static final int Removable = 2;
    public static final int Fixed = 3;
    public static final int Remote = 4;
    public static final int CDROM = 5;
    public static final int Ramdisk = 6;
    public static final int ImageFile = 100;

    public static final int F5_1Pt2_512 = 1;
    public static final int F3_1Pt44_512 = 2;
    public static final int F3_2Pt88_512 = 3;
    public static final int F3_20Pt88_512 = 4;
    public static final int F3_720_512 = 5;
    public static final int F5_360_512 = 6;
    public static final int F5_320_512 = 7;
    public static final int F5_320_1024 = 8;
    public static final int F5_180_512 = 9;
    public static final int F5_160_512 = 10;
    public static final int RemovableMedia = 11;
    public static final int FixedMedia = 12;
    public static final int F3_120M_512 = 13;

    protected int zeroOffset = 0;

    abstract public int open(String path) throws IOException;
    abstract public int close() throws IOException;

    abstract public void  read(long offset, int length, byte[] buffer) throws IOException;
    abstract public void write(long offset, int length, byte[] buffer) throws IOException;
    abstract public long size() throws IOException;

    abstract public int  type();

    abstract public boolean wp();
}
