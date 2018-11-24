package com.hp.ilo2.virtdevs;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Date;


public class SCSIFloppy extends SCSI {
    int fdd_state = 0;
    long media_sz;
    Date date = new Date();
    byte[] rcs_resp = {0, 0, 0, 16, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 11, 64, 0, 0, 2, 0};


    public SCSIFloppy(Socket socket, InputStream inputStream, BufferedOutputStream outputStream, String selectedDevice, boolean targetIsDevice) throws IOException {
        super(socket, inputStream, outputStream, selectedDevice, targetIsDevice ? 1 : 0);
        int i = this.media.open(selectedDevice, targetIsDevice ? 1 : 0);
        D.println(D.INFORM, "open returns " + i);
    }

    public void setWriteProt(boolean writeProtect) {
        this.writeprot = writeProtect;

        if (this.fdd_state == 2) this.fdd_state = 0;
    }

    public void process() throws IOException {
        this.date.setTime(System.currentTimeMillis());
        D.println(D.INFORM, "Date = " + this.date);
        D.println(D.INFORM, "Device: " + this.selectedDevice + " (" + this.targetIsDevice + ")");
        read_command(this.req, 12);
        D.println(D.INFORM, "SCSI Request: ");
        D.hexdump(D.INFORM, this.req, 12);
        this.media_sz = this.media.size();


        if ((this.media_sz < 0) || ((this.media.dio != null) && (this.media.dio.filehandle == -1))) {
            D.println(D.INFORM, "Disk change detected\n");
            this.media.close();
            this.media.open(this.selectedDevice, this.targetIsDevice);
            this.media_sz = this.media.size();
            this.fdd_state = 0;
        }
        D.println(D.INFORM, "retval=" + this.media_sz + " type=" + this.media.type() + " physdrive=" + (this.media.dio != null ? this.media.dio.PhysicalDevice : -1));


        if (this.media_sz <= 0) {
            this.reply.setmedia(0);
            this.fdd_state = 0;
        } else {
            this.reply.setmedia(36);
            this.fdd_state += 1;
            if (this.fdd_state > 2) {
                this.fdd_state = 2;
            }
        }
        if ((!this.writeprot) && (this.media.wp())) {
            this.writeprot = true;
        }
        switch (this.req[0] & 0xFF) {
            case SCSI_FORMAT_UNIT:
                client_format_unit(this.req);
                break;
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
            case SCSI_WRITE_10:
            case SCSI_WRITE_VERIFY:
            case SCSI_WRITE_12:
                client_write(this.req);
                break;
            case SCSI_READ_CAPACITIES:
                client_read_capacities();
                break;
            case SCSI_START_STOP_UNIT:
                client_start_stop_unit(this.req);
                break;
            default:
                D.println(D.FATAL, "Unknown request:cmd = " + Integer.toHexString(this.req[0]));
        }
    }

    void client_read_capacities()
            throws IOException {
        if (this.fdd_state != 1) {
            this.reply.set(0, 0, 0, this.rcs_resp.length);
        } else {
            this.reply.set(6, 40, 0, this.rcs_resp.length);
            this.fdd_state = 2;
        }
        if (this.media.type() == 0) {
            this.rcs_resp[4] = (this.rcs_resp[5] = this.rcs_resp[6] = this.rcs_resp[7] = this.rcs_resp[10] = this.rcs_resp[11] = 0);
        } else {
            long l;
            if (this.media.type() == MediaAccess.ImageFile) {
                l = this.media.size() / 512L;
                this.rcs_resp[4] = ((byte) (int) (l >> 24 & 0xFF));
                this.rcs_resp[5] = ((byte) (int) (l >> 16 & 0xFF));
                this.rcs_resp[6] = ((byte) (int) (l >> 8 & 0xFF));
                this.rcs_resp[7] = ((byte) (int) (l >> 0 & 0xFF));
                this.rcs_resp[10] = 2;
                this.rcs_resp[11] = 0;
            } else {
                l = this.media.size() / this.media.dio.BytesPerSec;
                this.rcs_resp[4] = ((byte) (int) (l >> 24 & 0xFF));
                this.rcs_resp[5] = ((byte) (int) (l >> 16 & 0xFF));
                this.rcs_resp[6] = ((byte) (int) (l >> 8 & 0xFF));
                this.rcs_resp[7] = ((byte) (int) (l >> 0 & 0xFF));
                this.rcs_resp[10] = ((byte) (this.media.dio.BytesPerSec >> 8 & 0xFF));
                this.rcs_resp[11] = ((byte) (this.media.dio.BytesPerSec & 0xFF));
            }
        }
        this.reply.setflags(this.writeprot);
        this.reply.send(this.out);
        this.out.write(this.rcs_resp, 0, this.rcs_resp.length);
        this.out.flush();
    }

    void client_send_diagnostic() throws IOException {
        this.fdd_state = 1;
    }


    void client_read(byte[] paramArrayOfByte)
            throws IOException {
        int j = paramArrayOfByte[0] == SCSI_READ_12 ? 1 : 0;

        long l = SCSI.mk_int32(paramArrayOfByte, 2) * 512L;
        int i = j != 0 ? SCSI.mk_int32(paramArrayOfByte, 6) : SCSI.mk_int16(paramArrayOfByte, 7);
        i *= 512;

        D.println(D.VERBOSE, "FDIO.client_read:Client read " + l + ", len=" + i);


        if ((l >= 0L) && (l < this.media_sz)) {
            try {
                this.media.read(l, i, this.buffer);
                this.reply.set(0, 0, 0, i);
            } catch (IOException localIOException) {
                D.println(D.FATAL, "Exception during read: " + localIOException);

                this.reply.set(3, 16, 0, 0);
                i = 0;
            }
        } else {
            this.reply.set(5, 33, 0, 0);
            i = 0;
        }

        this.reply.setflags(this.writeprot);
        this.reply.send(this.out);
        if (i != 0)
            this.out.write(this.buffer, 0, i);
        this.out.flush();
    }


    void client_write(byte[] paramArrayOfByte)
            throws IOException {
        int j = paramArrayOfByte[0] == SCSI_WRITE_12 ? 1 : 0;

        long l = SCSI.mk_int32(paramArrayOfByte, 2) * 512L;
        int i = j != 0 ? SCSI.mk_int32(paramArrayOfByte, 6) : SCSI.mk_int16(paramArrayOfByte, 7);
        i *= 512;

        D.println(D.VERBOSE, "FDIO.client_write:lba = " + l + ", length = " + i);
        read_complete(this.buffer, i);

        if (!this.writeprot) {
            if ((l >= 0L) && (l < this.media_sz)) {
                try {
                    this.media.write(l, i, this.buffer);
                    this.reply.set(0, 0, 0, 0);
                } catch (IOException localIOException) {
                    D.println(D.FATAL, "Exception during write: " + localIOException);

                    this.reply.set(3, 16, 0, 0);
                }

            } else {
                this.reply.set(5, 33, 0, 0);
            }
        } else {
            this.reply.set(7, 39, 0, 0);
        }
        this.reply.setflags(this.writeprot);
        this.reply.send(this.out);
        this.out.flush();
    }

    void client_pa_media_removal(byte[] paramArrayOfByte)
            throws IOException {
        if ((paramArrayOfByte[4] & 0x1) != 0) {


            this.reply.set(5, 36, 0, 0);
        } else {
            this.reply.set(0, 0, 0, 0);
        }
        this.reply.setflags(this.writeprot);
        this.reply.send(this.out);
        this.out.flush();
    }


    void client_start_stop_unit(byte[] paramArrayOfByte)
            throws IOException {
        if ((paramArrayOfByte[4] & 0x2) != 0) {


            this.reply.set(5, 36, 0, 0);
        } else {
            this.reply.set(0, 0, 0, 0);
        }
        this.reply.setflags(this.writeprot);
        this.reply.send(this.out);
        this.out.flush();
    }

    void client_test_unit_ready() throws IOException {
        if (this.fdd_state == 0) {
            D.println(D.VERBOSE, "media not present");
            this.reply.set(2, 58, 0, 0);
        } else if (this.fdd_state == 1) {
            D.println(D.VERBOSE, "media changed");
            this.reply.set(6, 40, 0, 0);
            this.fdd_state = 2;
        } else {
            D.println(D.VERBOSE, "device ready");
            this.reply.set(0, 0, 0, 0);
        }
        this.reply.setflags(this.writeprot);
        this.reply.send(this.out);
        this.out.flush();
    }

    void client_format_unit(byte[] paramArrayOfByte) throws IOException {
        byte[] arrayOfByte = new byte[100];
        int i = SCSI.mk_int16(paramArrayOfByte, 7);


        read_complete(arrayOfByte, i);
        D.println(D.VERBOSE, "Format params: ");
        D.hexdump(D.VERBOSE, arrayOfByte, i);
        int j = arrayOfByte[1] & 0x1;
        int k;
        if ((SCSI.mk_int32(arrayOfByte, 4) == 2880) && (SCSI.mk_int24(arrayOfByte, 9) == 512)) {
            k = 2;
        } else if ((SCSI.mk_int32(arrayOfByte, 4) == 1440) && (SCSI.mk_int24(arrayOfByte, 9) == 512)) {
            k = 5;
        } else {
            k = 0;
        }

        if (this.writeprot) {
            this.reply.set(7, 39, 0, 0);
        } else if (k != 0) {
            int m = paramArrayOfByte[2] & 0xFF;

            this.media.format(k, m, m, j, j);
            D.println(D.VERBOSE, "format");
            this.reply.set(0, 0, 0, 0);
        } else {
            this.reply.set(5, 38, 0, 0);
        }

        this.reply.setflags(this.writeprot);
        this.reply.send(this.out);
        this.out.flush();
    }

    void client_read_capacity() throws IOException {
        byte[] arrayOfByte = {0, 0, 0, 0, 0, 0, 0, 0};


        this.reply.set(0, 0, 0, arrayOfByte.length);
        if (this.fdd_state == 0) {
            this.reply.set(2, 58, 0, 0);
        } else if (this.fdd_state == 1) {
            this.reply.set(6, 40, 0, 0);
        } else if (this.media.type() != 0) {
            long l;
            if (this.media.type() == 100) {
                l = this.media.size() / 512L - 1L;
                arrayOfByte[0] = ((byte) (int) (l >> 24 & 0xFF));
                arrayOfByte[1] = ((byte) (int) (l >> 16 & 0xFF));
                arrayOfByte[2] = ((byte) (int) (l >> 8 & 0xFF));
                arrayOfByte[3] = ((byte) (int) (l >> 0 & 0xFF));
                arrayOfByte[6] = 2;
            } else {
                l = this.media.size() / this.media.dio.BytesPerSec - 1L;
                arrayOfByte[0] = ((byte) (int) (l >> 24 & 0xFF));
                arrayOfByte[1] = ((byte) (int) (l >> 16 & 0xFF));
                arrayOfByte[2] = ((byte) (int) (l >> 8 & 0xFF));
                arrayOfByte[3] = ((byte) (int) (l >> 0 & 0xFF));
                arrayOfByte[6] = ((byte) (this.media.dio.BytesPerSec >> 8 & 0xFF));
                arrayOfByte[7] = ((byte) (this.media.dio.BytesPerSec & 0xFF));
            }
        }
        this.reply.setflags(this.writeprot);
        this.reply.send(this.out);
        if (this.fdd_state == 2)
            this.out.write(arrayOfByte, 0, arrayOfByte.length);
        this.out.flush();
        D.println(D.VERBOSE, "FDIO.client_read_capacity: ");
        D.hexdump(D.VERBOSE, arrayOfByte, 8);
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/SCSIFloppy.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */