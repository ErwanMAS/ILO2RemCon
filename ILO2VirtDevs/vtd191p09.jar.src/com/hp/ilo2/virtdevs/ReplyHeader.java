package com.hp.ilo2.virtdevs;

import java.io.OutputStream;

public class ReplyHeader {
    public static final int magic = 0xBADC0DE;

    //flags
    public static final int WP = 1;
    public static final int KEEPALIVE = 2;

    int flags;
    byte sense_key;
    byte asc;
    byte ascq;
    byte media;
    int length;
    byte[] data = new byte[16];

    void set(int sense_key, int asc, int ascq, int length) {
        this.sense_key = ((byte) sense_key);
        this.asc = ((byte) asc);
        this.ascq = ((byte) ascq);
        this.length = length;
    }

    void setmedia(int media) {
        this.media = ((byte) media);
    }

    void setflags(boolean wp) {
        if (wp) this.flags |= WP;
        else {
            this.flags &= ~WP;
        }
    }

    void keepalive(boolean paramBoolean) {
        if (paramBoolean) this.flags |= KEEPALIVE;
        else {
            this.flags &= ~KEEPALIVE;
        }
    }

    void send(OutputStream out) throws java.io.IOException {
        this.data[0]  = (byte) (magic       & 0xFF); //0xDE
        this.data[1]  = (byte) (magic >>  8 & 0xFF); //0xC0
        this.data[2]  = (byte) (magic >> 16 & 0xFF); //0xAD
        this.data[3]  = (byte) (magic >> 24 & 0xFF); //0x0B
        this.data[4]  = (byte) (flags       & 0xFF);
        this.data[5]  = (byte) (flags >>  8 & 0xFF);
        this.data[6]  = (byte) (flags >> 16 & 0xFF);
        this.data[7]  = (byte) (flags >> 24 & 0xFF);
        this.data[8]  = this.media;
        this.data[9]  = this.sense_key;
        this.data[10] = this.asc;
        this.data[11] = this.ascq;
        this.data[12] = (byte) (length       & 0xFF);
        this.data[13] = (byte) (length >>  8 & 0xFF);
        this.data[14] = (byte) (length >> 16 & 0xFF);
        this.data[15] = (byte) (length >> 24 & 0xFF);
        out.write(this.data, 0, 16);
    }

    void sendsynch(OutputStream out, byte[] paramArrayOfByte) throws java.io.IOException {
        this.data[0]  = (byte) (magic       & 0xFF); //0xDE
        this.data[1]  = (byte) (magic >>  8 & 0xFF); //0xC0
        this.data[2]  = (byte) (magic >> 16 & 0xFF); //0xAD
        this.data[3]  = (byte) (magic >> 24 & 0xFF); //0x0B
        this.data[4]  = (byte) (flags       & 0xFF);
        this.data[5]  = (byte) (flags >> 8  & 0xFF);
        this.data[6]  = (byte) (flags >> 16 & 0xFF);
        this.data[7]  = (byte) (flags >> 24 & 0xFF);
        this.data[8]  = paramArrayOfByte[4];
        this.data[9]  = paramArrayOfByte[5];
        this.data[10] = paramArrayOfByte[6];
        this.data[11] = paramArrayOfByte[7];
        this.data[12] = paramArrayOfByte[8];
        this.data[13] = paramArrayOfByte[9];
        this.data[14] = paramArrayOfByte[10];
        this.data[15] = paramArrayOfByte[11];
        out.write(this.data, 0, 16);
    }
}