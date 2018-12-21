package com.hp.ilo2.virtdevs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MediaAccess {
    public static final int Unknown = 0;
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
    public static final int ImageFile = 100;
    public static final int NoRootDir = 1;
    public static final int Removable = 2;
    public static final int Fixed = 3;
    public static final int Remote = 4;
    public static final int CDROM = 5;
    public static final int Ramdisk = 6;

    private File file;
    private RandomAccessFile randomAccessFile;
    private boolean readonly = false;
    private int zeroOffset = 0;

    public int open(String path) throws IOException {
        boolean isDiskImage = false;

        this.zeroOffset = 0;
        this.readonly = false;
        this.file = new File(path);
        if ((!this.file.exists()))
            throw new IOException("File " + path + " does not exist");
        if (this.file.isDirectory()) {
            throw new IOException("File " + path + " is a directory");
        }


        try {
            this.randomAccessFile = new RandomAccessFile(path, "rw");
        } catch (IOException e) {
            this.randomAccessFile = new RandomAccessFile(path, "r");
            this.readonly = true;
        }

        byte[] buf = new byte[16];
        read(0L, 16, buf);
        if ((buf[0] == 'C') && (buf[1] == 'P') && (buf[2] == 'Q') && (buf[3] == 'R') && (buf[4] == 'F') && (buf[5] == 'B') && (buf[6] == 'L') && (buf[7] == 'O')) {
            this.zeroOffset = (buf[14] | buf[15] << 8);
        }

        return 0;
    }

    public int close() throws IOException {
        this.randomAccessFile.close();

        return 0;
    }

    public void read(long offset, int length, byte[] data) throws IOException {
        offset += this.zeroOffset;
        this.randomAccessFile.seek(offset);
        this.randomAccessFile.read(data, 0, length);
    }

    public void write(long offset, int length, byte[] data) throws IOException {
        offset += this.zeroOffset;
        this.randomAccessFile.seek(offset);
        this.randomAccessFile.write(data, 0, length);
    }

    public long size() throws IOException {
        return this.randomAccessFile.length() - this.zeroOffset;
    }

    public int format(int mediaType, int startCylinder, int endCylinder, int startHead, int endHead) throws IOException {
        return 0;
    }

    public String[] devices() {
        return null;
    }

    public int devtype(String paramString) {
        return 0;
    }

    public int scsi(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3) {
        return scsi(paramArrayOfByte1, paramInt1, paramInt2, paramArrayOfByte2, paramArrayOfByte3, 0);
    }

    public int scsi(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, int paramInt3) {
        return -1;
    }

    public boolean wp() {
        return this.readonly;
    }

    public int type() {
        if (this.randomAccessFile != null) {
            return MediaAccess.ImageFile;
        }
        return 0;
    }
}