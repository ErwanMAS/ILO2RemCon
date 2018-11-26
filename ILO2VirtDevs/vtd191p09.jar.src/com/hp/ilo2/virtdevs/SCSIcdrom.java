package com.hp.ilo2.virtdevs;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class SCSIcdrom extends SCSI {
    public static final int SCSI_IOCTL_DATA_OUT = 0;
    public static final int SCSI_IOCTL_DATA_IN = 1;
    public static final int SCSI_IOCTL_DATA_UNSPECIFIED = 2;
    public static final int CONST = 0;
    private static final int WRITE =        0x00_00_00_00;
    private static final int READ =         0x01_00_00_00;
    private static final int NONE =         0x02_00_00_00;
    private static final int BLKS =            0x80_00_00;
    private static final int B32 =             0x04_00_00;
    private static final int B24 =             0x03_00_00;
    private static final int B16 =             0x02_00_00;
    private static final int B08 =             0x01_00_00;

    private static final int[] commands = {
            SCSI_PA_MEDIA_REMOVAL,  NONE,
            SCSI_READ_CAPACITY,     READ | 0x00_08,
            SCSI_SEND_DIAGNOSTIC,   NONE,
            0,                      NONE,
            SCSI_READ_10,           READ | BLKS | B16 | 0x00_07,
            SCSI_READ_12,           READ | BLKS | B32 | 0x00_06,
            SCSI_START_STOP_UNIT,   NONE,
            SCSI_READ_CD,           READ | BLKS | B24 | 0x00_06,
            SCSI_READ_CD_MSF,       READ,
            SCSI_READ_HEADER,       READ | 0x00_08,
            SCSI_READ_SUBCHANNEL,   READ | B16 | 0x00_07,
            SCSI_READ_TOC,          READ | B16 | 0x00_07,
            SCSI_STOP_PLAY_SCAN,    NONE,
            SCSI_MECHANISM_STATUS,  READ | B16 | 0x00_08,
            SCSI_MODE_SENSE,        READ | B16 | 0x00_07,
            SCSI_GET_EVENT_STATUS,  READ | B16 | 0x00_07
    };


    private byte[] sense = new byte[3];
    private int retryCount;
    private VErrorDialog errorDialog;
    private boolean doSplitReads = false;

    public SCSIcdrom(Socket socket, InputStream inStream, BufferedOutputStream outStream, String selectedDevice) throws IOException {
        super(socket, inStream, outStream, selectedDevice, false);

        D.println(D.INFORM, "Media opening " + selectedDevice + "(" + (0x2) + ")");


        int i = this.media.open(selectedDevice, targetIsDevice);
        D.println(D.INFORM, "Media open returns " + i);
        this.retryCount = Integer.valueOf(virtdevs.prop.getProperty("com.hp.ilo2.virtdevs.retrycount", "10"));
    }

    void media_err(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2) {
        String str = "The CDROM drive reports a media error:\nCommand: " + D.hex(paramArrayOfByte1[0], 2) + " " + D.hex(paramArrayOfByte1[1], 2) + " " + D.hex(paramArrayOfByte1[2], 2) + " " + D.hex(paramArrayOfByte1[3], 2) + " " + D.hex(paramArrayOfByte1[4], 2) + " " + D.hex(paramArrayOfByte1[5], 2) + " " + D.hex(paramArrayOfByte1[6], 2) + " " + D.hex(paramArrayOfByte1[7], 2) + " " + D.hex(paramArrayOfByte1[8], 2) + " " + D.hex(paramArrayOfByte1[9], 2) + " " + D.hex(paramArrayOfByte1[10], 2) + " " + D.hex(paramArrayOfByte1[11], 2) + "\n" + "Sense Code: " + D.hex(paramArrayOfByte2[0], 2) + "/" + D.hex(paramArrayOfByte2[1], 2) + "/" + D.hex(paramArrayOfByte2[2], 2) + "\n\n";

        if ((this.errorDialog == null) || (this.errorDialog.disposed())) {
            this.errorDialog = new VErrorDialog(str, false);
        } else {
            this.errorDialog.append(str);
        }
    }

    public void close() throws IOException {
        this.req[0] = SCSI_PA_MEDIA_REMOVAL;
        this.req[1] = (this.req[2] = this.req[3] = this.req[4] = this.req[5] = this.req[7] = this.req[7] = this.req[8] = this.req[9] = this.req[10] = this.req[11] = 0);

        this.media.scsi(this.req, 2, 0, this.buffer, null);
        super.close();
    }

    int scsi_length(int commandIndex, byte[] paramArrayOfByte) {
        int length = 0;
        commandIndex++; //commandIndex now refers to the command's flags
        switch (commands[commandIndex] & 0x7F0000) {
            case 0:
                length = commands[commandIndex] & 0xFFFF;
                break;
            case B32:
                length = SCSI.mk_int32(paramArrayOfByte, commands[commandIndex] & 0xFFFF);
                break;
            case B24:
                length = SCSI.mk_int24(paramArrayOfByte, commands[commandIndex] & 0xFFFF);
                break;
            case B16:
                length = SCSI.mk_int16(paramArrayOfByte, commands[commandIndex] & 0xFFFF);
                break;
            case B08:
                length = paramArrayOfByte[(commands[commandIndex] & 0xFFFF)] & 0xFF;
                break;
            default:
                D.println(D.FATAL, "Unknown Size!");
        }
        if ((commands[commandIndex] & BLKS) == BLKS)
            length *= 2048;
        return length;
    }

    void start_stop_unit() {
        byte[] arrayOfByte1 = {SCSI_START_STOP_UNIT, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0};
        byte[] arrayOfByte2 = new byte[3];

        int i = this.media.scsi(arrayOfByte1, 2, 0, this.buffer, arrayOfByte2);
        D.println(D.VERBOSE, "Start/Stop unit = " + i + " " + arrayOfByte2[0] + "/" + arrayOfByte2[1] + "/" + arrayOfByte2[2]);
    }


    boolean within_75(byte[] paramArrayOfByte) {
        byte[] arrayOfByte1 = new byte[8];
        byte[] arrayOfByte2 = {SCSI_READ_CAPACITY, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        boolean isRead12 = paramArrayOfByte[0] == SCSI_READ_12;
        int j = SCSI.mk_int32(paramArrayOfByte, 2);
        int k = isRead12 ? SCSI.mk_int32(paramArrayOfByte, 6) : SCSI.mk_int16(paramArrayOfByte, 7);

        this.media.scsi(arrayOfByte2, 1, 8, arrayOfByte1, null);

        int m = SCSI.mk_int32(arrayOfByte1, 0);
        return (j > m - 75) || (j + k > m - 75);
    }

    int split_read() {
        boolean isRead12 = this.req[0] == SCSI_READ_12;
        int j = SCSI.mk_int32(this.req, 2);
        int k = isRead12 ? SCSI.mk_int32(this.req, 6) : SCSI.mk_int16(this.req, 7);
        int m = k > 32 ? 32 : k;

        int i2 = 1;

        this.req[2] = ((byte) (j >> 24));
        this.req[3] = ((byte) (j >> 16));
        this.req[4] = ((byte) (j >> 8));
        this.req[5] = ((byte) j);
        if (isRead12) {
            this.req[6] = ((byte) (m >> 24));
            this.req[7] = ((byte) (m >> 16));
            this.req[8] = ((byte) (m >> 8));
            this.req[9] = ((byte) m);
        } else {
            this.req[7] = ((byte) (m >> 8));
            this.req[8] = ((byte) m);
        }
        int n = this.media.scsi(this.req, i2, m * 2048, this.buffer, this.sense);
        if (n < 0) {
            return n;
        }
        k -= m;
        if (k <= 0) {
            return n;
        }

        j += m;
        this.req[2] = ((byte) (j >> 24));
        this.req[3] = ((byte) (j >> 16));
        this.req[4] = ((byte) (j >> 8));
        this.req[5] = ((byte) j);
        if (isRead12) {
            this.req[6] = ((byte) (k >> 24));
            this.req[7] = ((byte) (k >> 16));
            this.req[8] = ((byte) (k >> 8));
            this.req[9] = ((byte) k);
        } else {
            this.req[7] = ((byte) (k >> 8));
            this.req[8] = ((byte) k);
        }
        int i1 = this.media.scsi(this.req, i2, k * 2048, this.buffer, this.sense, B08);
        if (i1 < 0) {
            return i1;
        }
        return n + i1;
    }

    public void process() throws IOException {
        read_command(this.req, 12);
        D.println(D.INFORM, "SCSI Request:");
        D.hexdump(D.INFORM, this.req, 12);
        int n;
        if (this.media.dio.filehandle == -1) {
            int ret = this.media.open(this.selectedDevice, this.targetIsDevice);
            if (ret < 0) {
                new VErrorDialog("Could not open CDROM (" + this.media.dio.sysError(-ret) + ")", false);
                throw new IOException("Couldn't open cdrom " + ret);
            }
        }

        int commandIndex = 0;
        for (; commandIndex < commands.length; commandIndex += 2) {
            if (this.req[0] == (byte) commands[commandIndex])
                break;
        }

        int len;
        int dir;
        if (commandIndex != commands.length) {
            len = scsi_length(commandIndex, this.req);
            dir = commands[(commandIndex + 1)] >> 24;
            int command = this.req[0] & 0xFF;

            if (dir == WRITE) {
                read_complete(this.buffer, len);
            }
            D.println(D.INFORM, "SCSI dir=" + dir + " len=" + len);
            int retries = 0;
            do {
                long timeBefore = System.currentTimeMillis();
                if (((command == SCSI_READ_10) || (command == SCSI_READ_12)) && (this.doSplitReads)) {
                    n = split_read();
                } else {
                    n = this.media.scsi(this.req, dir, len, this.buffer, this.sense);
                }
                long timeAfter = System.currentTimeMillis();

                D.println(D.INFORM, "ret=" + n + " sense=" + D.hex(this.sense[0], 2) + " " + D.hex(this.sense[1], 2) + " " + D.hex(this.sense[2], 2) + " Time=" + (timeAfter - timeBefore));


                if (command == SCSI_MODE_SENSE) {
                    D.println(D.INFORM, "media type: " + D.hex(this.buffer[3], 2));

                    this.reply.setmedia(this.buffer[3]);
                }
                if (command == SCSI_READ_TOC) {
                    D.hexdump(D.VERBOSE, this.buffer, len);
                }

                if (command == SCSI_START_STOP_UNIT) {
                    n = 0;
                }

                if ((command == SCSI_READ_10) || (command == SCSI_READ_12)) {
                    if (this.sense[1] == 41) {
                        n = -1;
                    } else if ((n < 0) && (within_75(this.req))) {

                        this.sense[0] = 5;
                        this.sense[1] = 33;
                        this.sense[2] = 0;
                        n = 0;
                    } else if (n < 0) {


                        this.doSplitReads = true;
                    }
                }

                if ((this.sense[0] == 3) || (this.sense[0] == 4)) {
                    media_err(this.req, this.sense);
                    n = -1;
                }

            } while ((n < 0) && (retries++ < this.retryCount));

            len = n;
            if ((len < 0) || (len > 0x20000)) {
                D.println(D.FATAL, "AIEE! len out of bounds: " + len + ", cmd: " + D.hex(command, 2) + "\n");

                len = 0;
                this.reply.set(5, 32, 0, 0);
            } else {
                this.reply.set(this.sense[0], this.sense[1], this.sense[2], len);
            }
        } else {
            D.println(D.FATAL, "AIEE! Unhandled command" + D.hex(this.req[0], 2) + "\n");

            this.reply.set(5, 32, 0, 0);
            len = 0;
        }
        this.reply.send(this.out);

        if (len != 0)
            this.out.write(this.buffer, 0, len);
        this.out.flush();
    }
}