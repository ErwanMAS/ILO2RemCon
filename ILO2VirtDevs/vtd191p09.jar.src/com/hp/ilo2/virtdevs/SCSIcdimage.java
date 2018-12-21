package com.hp.ilo2.virtdevs;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class SCSIcdimage extends SCSI {
    private int fddState = 0;
    private int eventState = 0;
    private long mediaSize;
    private virtdevs cdi;

    public SCSIcdimage(Socket socket, InputStream inputStream, BufferedOutputStream outputStream, String selectedDevice, virtdevs paramvirtdevs) throws IOException {
        super(socket, inputStream, outputStream, selectedDevice);
        this.cdi = paramvirtdevs;
        int i = this.media.open(selectedDevice);
        D.println(D.INFORM, "Media open returns " + i + " / " + this.media.size() + " bytes");
    }

    public void process() throws IOException {
        D.println(D.INFORM, "Device: " + this.selectedDevice + " (0)");
        read_command(this.req, 12);
        D.println(D.INFORM, "SCSI Request: ");
        D.hexdump(D.INFORM, this.req, 12);

        this.mediaSize = this.media.size();
        if (this.mediaSize == 0) {
            this.reply.setmedia(0);
            this.fddState = 0;

            this.eventState = 4;
        } else {
            this.reply.setmedia(1);
            this.fddState += 1;
            if (this.fddState > 2) {
                this.fddState = 2;
            }
            if (this.eventState == 4)
                this.eventState = 0;
            this.eventState += 1;
            if (this.eventState > 2) {
                this.eventState = 2;
            }
        }
        switch (this.req[0] & 0xFF) {
            case SCSI_PA_MEDIA_REMOVAL:
                client_pa_media_removal(this.req);
                break;
            case SCSI_READ_CAPACITY:
                client_read_capacity();
                break;
            case SCSI_SEND_DIAGNOSTIC:
                client_send_diagnostic();
                break;
            case SCSI_TEST_UNIT_READY:
                client_test_unit_ready();
                break;
            case SCSI_READ_10:
            case SCSI_READ_12:
                client_read(this.req);
                break;
            case SCSI_START_STOP_UNIT:
                client_start_stop_unit(this.req);
                break;
            case SCSI_READ_TOC:
                client_read_toc(this.req);
                break;
            case SCSI_MODE_SENSE:
                client_mode_sense(this.req);
                break;
            case SCSI_GET_EVENT_STATUS:
                client_get_event_status(this.req);
                break;
            default:
                D.println(D.FATAL, "Unknown request:cmd = " + Integer.toHexString(this.req[0]));
                this.reply.set(5, 36, 0, 0);
                this.reply.send(this.out);
                this.out.flush();
        }
    }

    private void client_send_diagnostic() throws IOException {}

    private void client_read(byte[] request) throws IOException {
        int j = request[0] == SCSI_READ_12 ? 1 : 0;

        long l = SCSI.mk_int32(request, 2) * 2048;
        int len = j != 0 ? SCSI.mk_int32(request, 6) : SCSI.mk_int16(request, 7);
        len *= 2048;

        D.println(D.VERBOSE, "CDImage :Client read " + l + ", len=" + len);

        if (this.fddState == 0) {
            D.println(D.VERBOSE, "media not present");
            this.reply.set(2, 58, 0, 0);
            len = 0;
        } else if (this.fddState == 1) {
            D.println(D.VERBOSE, "media changed");
            this.reply.set(6, 40, 0, 0);
            len = 0;
            this.fddState = 2;

        } else if ((l >= 0) && (l < this.mediaSize)) {
            this.media.read(l, len, this.buffer);
            this.reply.set(0, 0, 0, len);
        } else {
            this.reply.set(5, 33, 0, 0);
            len = 0;
        }


        this.reply.send(this.out);
        if (len != 0)
            this.out.write(this.buffer, 0, len);
        this.out.flush();
    }

    private void client_pa_media_removal(byte[] request) throws IOException {
        if ((request[4] & 0x1) != 0) {
            D.println(D.VERBOSE, "Media removal prevented");
        } else {
            D.println(D.VERBOSE, "Media removal allowed");
        }
        this.reply.set(0, 0, 0, 0);
        this.reply.send(this.out);
        this.out.flush();
    }


    private void client_start_stop_unit(byte[] request) throws IOException {
        int i = (byte) (request[4] & 0x3);

        if (i == 3) {
            if (this.cdi.cdConnection != null) {
                this.fddState = 1;
                this.eventState = 2;
            } else {
                this.fddState = 0;
                this.eventState = 4;
            }
        } else if (i == 2) {
            this.fddState = 0;

            this.eventState = 4;
            if (this.cdi.cdConnection != null) {
                this.cdi.do_cdrom(this.cdi.cdSelected);
            }

            D.println(D.VERBOSE, "Media eject");
        }
        this.reply.set(0, 0, 0, 0);
        this.reply.send(this.out);
        this.out.flush();
    }

    private void client_test_unit_ready() throws IOException {
        if (this.fddState == 0) {
            D.println(D.VERBOSE, "media not present");
            this.reply.set(2, 58, 0, 0);
        } else if (this.fddState == 1) {
            D.println(D.VERBOSE, "media changed");
            this.reply.set(6, 40, 0, 0);
            this.fddState = 2;
        } else {
            D.println(D.VERBOSE, "device ready");
            this.reply.set(0, 0, 0, 0);
        }
        this.reply.send(this.out);
        this.out.flush();
    }

    private void client_read_capacity() throws IOException {
        byte[] arrayOfByte = {0, 0, 0, 0, 0, 0, 0, 0};

        this.reply.set(0, 0, 0, arrayOfByte.length);
        if (this.fddState == 0) {
            this.reply.set(2, 58, 0, 0);
        } else if (this.fddState == 1) {
            this.reply.set(6, 40, 0, 0);
        } else {
            int i = (int) (this.media.size() / 2048L - 1L);
            arrayOfByte[0] = ((byte) (i >> 24 & 0xFF));
            arrayOfByte[1] = ((byte) (i >> 16 & 0xFF));
            arrayOfByte[2] = ((byte) (i >> 8 & 0xFF));
            arrayOfByte[3] = ((byte) (i >> 0 & 0xFF));
            arrayOfByte[6] = 8;
        }
        this.reply.send(this.out);
        if (this.fddState == 2)
            this.out.write(arrayOfByte, 0, arrayOfByte.length);
        this.out.flush();
        D.println(D.VERBOSE, "client_read_capacity: ");
        D.hexdump(D.VERBOSE, arrayOfByte, 8);
    }

    private void client_read_toc(byte[] request) throws IOException {
        int i = (request[1] & 0x2) != 0 ? 1 : 0;
        int j = (request[9] & 0xC0) >> 6;
        int blockCount = (int) (this.media.size() / 2048L);
        double d = blockCount / 75.0 + 2.0;
        int m = (int) d / 60;
        int n = (int) d % 60;
        int i1 = (int) ((d - (int) d) * 75.0D);
        int i2 = SCSI.mk_int16(request, 7);

        for (int i3 = 0; i3 < i2; i3++) {
            this.buffer[i3] = 0;
        }

        if (j == 0) {
            this.buffer[0] = 0;
            this.buffer[1] = 18;
            this.buffer[2] = 1;
            this.buffer[3] = 1;

            this.buffer[4] = 0;
            this.buffer[5] = 20;
            this.buffer[6] = 1;
            this.buffer[7] = 0;
            this.buffer[8] = 0;
            this.buffer[9] = 0;
            this.buffer[10] = (byte) (i != 0 ? 2 : 0);
            this.buffer[11] = 0;

            this.buffer[12] = 0;
            this.buffer[13] = 20;
            this.buffer[14] = -86;
            this.buffer[15] = 0;
            this.buffer[16] = 0;
            this.buffer[17] = (i != 0 ? (byte) m : (byte) (blockCount >> 16 & 0xFF));
            this.buffer[18] = (i != 0 ? (byte) n : (byte) (blockCount >>  8 & 0xFF));
            this.buffer[19] = (i != 0 ? (byte) i1: (byte) (blockCount >>  0 & 0xFF));
        }

        if (j == 1) {
            this.buffer[0] = 0;
            this.buffer[1] = 10;
            this.buffer[2] = 1;
            this.buffer[3] = 1;

            this.buffer[4] = 0;
            this.buffer[5] = 20;
            this.buffer[6] = 1;
            this.buffer[7] = 0;
            this.buffer[8] = 0;
            this.buffer[9] = 0;
            this.buffer[10] = (byte) (i != 0 ? 2 : 0);
            this.buffer[11] = 0;
        }

        blockCount = 412;
        if (i2 < blockCount) blockCount = i2;
        D.hexdump(D.VERBOSE, this.buffer, blockCount);
        this.reply.set(0, 0, 0, blockCount);
        this.reply.send(this.out);
        this.out.write(this.buffer, 0, blockCount);
        this.out.flush();
    }

    private void client_mode_sense(byte[] request) throws IOException {
        this.buffer[0] = 0;
        this.buffer[1] = 8;
        this.buffer[2] = 1;
        this.buffer[3] = 0;
        this.buffer[4] = 0;
        this.buffer[5] = 0;
        this.buffer[6] = 0;
        this.buffer[7] = 0;
        this.reply.set(0, 0, 0, 8);
        D.hexdump(D.VERBOSE, this.buffer, 8);
        this.reply.setmedia(this.buffer[2]);
        this.reply.send(this.out);
        this.out.write(this.buffer, 0, 8);
        this.out.flush();
    }

    private void client_get_event_status(byte[] request) throws IOException {
        int i = request[4];
        int j = SCSI.mk_int16(request, 7);
        for (int k = 0; k < j; k++) {
            this.buffer[k] = 0;
        }

        if ((request[1] & 0x1) == 0) {
            this.reply.set(5, 36, 0, 0);
            this.reply.send(this.out);
            this.out.flush();
        }

        if ((i & 0x10) != 0) {
            this.buffer[0] = 0;
            this.buffer[1] = 6;
            this.buffer[2] = 4;
            this.buffer[3] = 16;
            if (this.eventState == 0) {
                this.buffer[4] = 0;
                this.buffer[5] = 0;
            } else if (this.eventState == 1) {
                this.buffer[4] = 4;
                this.buffer[5] = 2;

                if (j > 4)
                    this.eventState = 2;
            } else if (this.eventState == 4) {
                this.buffer[4] = 3;
                this.buffer[5] = 0;

                if (j > 4) {
                    this.eventState = 0;
                }
            } else {
                this.buffer[4] = 0;
                this.buffer[5] = 2;
            }

            D.hexdump(D.VERBOSE, this.buffer, 8);
            this.reply.set(0, 0, 0, j < 8 ? j : 8);
            this.reply.send(this.out);
            this.out.write(this.buffer, 0, j < 8 ? j : 8);
        } else {
            this.buffer[0] = 0;
            this.buffer[1] = 2;
            this.buffer[2] = Byte.MIN_VALUE;
            this.buffer[3] = 16;
            D.hexdump(D.VERBOSE, this.buffer, 4);
            this.reply.set(0, 0, 0, j < 4 ? j : 4);
            this.reply.send(this.out);
            this.out.write(this.buffer, 0, j < 4 ? j : 4);
        }
        this.out.flush();
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/SCSIcdimage.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */