package com.hp.ilo2.virtdevs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileMediaAccess extends BaseMediaAccess {
    private RandomAccessFile randomAccessFile;
    private boolean readonly = false;

    public int open(String path) throws IOException {
        return open(path, false);
    }

    public int open(String path, boolean isDiskImage) throws IOException {
        this.zeroOffset = 0;

        this.readonly = false;
        File file = new File(path);
        if ((!file.exists()) && !isDiskImage)
            throw new IOException("File " + path + " does not exist");
        if (file.isDirectory()) {
            throw new IOException("File " + path + " is a directory");
        }

        try {
            this.randomAccessFile = new RandomAccessFile(path, "rw");
        } catch (IOException e) {
            if (!isDiskImage) {
                this.randomAccessFile = new RandomAccessFile(path, "r");
                this.readonly = true;
            } else {
                throw e;
            }
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

    public int format(int media_type, int startCylinder, int endCylinder, int startHead, int endHead)
            throws IOException {
        return 0;
    }

    public int devtype(String paramString) {
        return BaseMediaAccess.Unknown;
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
            return BaseMediaAccess.ImageFile;
        }
        return BaseMediaAccess.Unknown;
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/MediaAccess.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */