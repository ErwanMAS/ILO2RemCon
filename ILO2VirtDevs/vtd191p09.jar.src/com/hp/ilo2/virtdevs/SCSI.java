package com.hp.ilo2.virtdevs;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Socket;


public abstract class SCSI {
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

    protected InputStream in;
    protected BufferedOutputStream out;
    protected Socket sock;

    MediaAccess media = new MediaAccess();
    ReplyHeader reply = new ReplyHeader();

    String selectedDevice;
    boolean writeprot = false;
    boolean pleaseExit = false;
    int targetIsDevice = 0;
    byte[] buffer = new byte[0x20000];
    byte[] req = new byte[12];

    public SCSI(Socket socket, InputStream inStream, BufferedOutputStream outStream, String selectedDevice, int targetIsDevice) {
        this.sock = socket;
        this.in = inStream;
        this.out = outStream;
        this.selectedDevice = selectedDevice;
        this.targetIsDevice = targetIsDevice;

    }

    public static int mk_int32(byte[] buffer, int offset) {
        int i = buffer[offset + 0];
        int j = buffer[offset + 1];
        int k = buffer[offset + 2];
        int m = buffer[offset + 3];

        int n = (i & 0xFF) << 24 | (j & 0xFF) << 16 | (k & 0xFF) << 8 | m & 0xFF;

        return n;
    }

    public static int mk_int24(byte[] buffer, int offset) {
        int i = buffer[offset + 0];
        int j = buffer[offset + 1];
        int k = buffer[offset + 2];

        int m = (i & 0xFF) << 16 | (j & 0xFF) << 8 | k & 0xFF;

        return m;
    }

    public static int mk_int16(byte[] buffer, int offset) {
        int i = buffer[offset + 0];
        int j = buffer[offset + 1];
        int k = (i & 0xFF) << 8 | j & 0xFF;
        return k;
    }

    public boolean getWriteProt() {
        D.println(D.VERBOSE, "media.wp = " + this.media.wp());
        return this.media.wp();
    }

    public void setWriteProt(boolean paramBoolean) {
        this.writeprot = paramBoolean;
    }

    public void close() throws IOException {
        this.media.close();
    }

    protected int read_complete(byte[] buffer, int length)
            throws IOException {
        int totalBytesRead = 0;
        int bytesRead;
        while (length > 0) {
            try {
                this.sock.setSoTimeout(1000);
                bytesRead = this.in.read(buffer, totalBytesRead, length);
            } catch (InterruptedIOException e) {
                continue;
            }
            if (bytesRead < 0)
                break;
            length -= bytesRead;
            totalBytesRead += bytesRead;
        }
        return totalBytesRead;
    }

    protected int read_command(byte[] buffer, int length) throws IOException {
        int totalBytesRead = 0;
        while (true) {
            try {
                this.sock.setSoTimeout(1000);
                totalBytesRead = this.in.read(buffer, 0, length);
            } catch (InterruptedIOException localInterruptedIOException) {
                this.reply.keepalive(true);
                D.println(D.VERBOSE, "Sending keepalive");
                this.reply.send(this.out);
                this.out.flush();
                this.reply.keepalive(false);
                if (!this.pleaseExit) {
                    break;
                }
                continue;
            }

            if ((buffer[0] & 0xFF) != 0xFE) break;

            this.reply.sendsynch(this.out, buffer);
            this.out.flush();
        }


        if (this.pleaseExit) throw new IOException("Asked to exit");
        if (totalBytesRead < 0) throw new IOException("Socket Closed");
        return totalBytesRead;
    }

    public abstract void process() throws IOException;

    public void change_disk() {
        this.pleaseExit = true;
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/SCSI.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */