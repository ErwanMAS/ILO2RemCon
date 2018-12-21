package com.hp.ilo2.virtdevs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import javax.swing.Timer;

public class Connection implements Runnable, java.awt.event.ActionListener {
    static final int FLOPPY = 1;
    static final int CDROM = 2;
    static final int USBKEY = 3;

    String host;
    int port;
    int device;
    String target;
    byte[] pre;
    byte[] key;
    private Socket socket;
    private java.io.InputStream in;
    private java.io.BufferedOutputStream out;
    private SCSI scsi;
    private boolean writeProtect = false;
    private boolean changing_disks;
    private virtdevs v;
    private VMD5 digest;

    public Connection(String host, int port, int device, String target, byte[] pre, byte[] key, virtdevs virtdevs)
            throws IOException {
        this.host = host;
        this.port = port;
        this.device = device;
        this.target = target;
        this.pre = pre;
        this.key = key;
        this.v = virtdevs;

        MediaAccess localMediaAccess = new MediaAccess();


        localMediaAccess.open(target);
        long size = localMediaAccess.size();
        localMediaAccess.close();

        if ((this.device == FLOPPY) && (size > 0x2D_00_00)) {
            this.device = USBKEY;
        }
        this.digest = new VMD5();
    }

    public int connect() throws IOException {
        byte[] buffer = {16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        this.socket = new Socket(this.host, this.port);
        this.socket.setTcpNoDelay(true);
        this.in = this.socket.getInputStream();
        this.out = new java.io.BufferedOutputStream(this.socket.getOutputStream());

        this.digest.reset();
        this.digest.update(this.pre);
        this.digest.update(this.key);
        byte[] keyMD5 = this.digest.digest();
        System.arraycopy(keyMD5, 0, buffer, 2, this.key.length);
        System.arraycopy(keyMD5, 0, this.key, 0, this.key.length);

        buffer[1] = ((byte) this.device);
        buffer[1] = ((byte) (buffer[1] | 0xFFFFFF80));

        this.out.write(buffer);
        this.out.flush();

        this.in.read(buffer, 0, 4);
        D.println(D.VERBOSE, "Hello response0: " + D.hex(buffer[0], 2));
        D.println(D.VERBOSE, "Hello response1: " + D.hex(buffer[1], 2));


        if ((buffer[0] == 32) && (buffer[1] == 0)) {
            D.println(D.INFORM, "Connected.  Protocol version = " + (buffer[3] & 0xFF) + "." + (buffer[2] & 0xFF));
        } else {
            D.println(D.FATAL, "Unexpected Hello Response!");
            this.socket.close();
            this.socket = null;
            this.in = null;
            this.out = null;
            return buffer[0];
        }
        return 0;
    }

    public void close() throws IOException {
        if (this.scsi != null) {
            try {
                Timer localTimer = new Timer(2000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            internalClose();
                        } catch (IOException ignored) {}
                    }
                });
                localTimer.setRepeats(false);
                localTimer.start();
                this.scsi.change_disk();
                localTimer.stop();
            } catch (Exception e) {
                this.scsi.change_disk();
            }
        } else {
            internalClose();
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent event) {
        try {
            internalClose();
        } catch (Exception ignored) {}
    }

    private void internalClose() throws IOException {
        if (this.socket != null) this.socket.close();
        this.socket = null;
        this.in = null;
        this.out = null;
    }

    public void setWriteProtect(boolean writeProtect) {
        this.writeProtect = writeProtect;
        if (this.scsi != null) {
            this.scsi.setWriteProtect(this.writeProtect);
        }
    }

    public void change_disk(String paramString)
            throws IOException {
        MediaAccess localMediaAccess = new MediaAccess();
        int j = localMediaAccess.devtype(paramString);
        boolean i;
        if ((j == MediaAccess.Removable) || (j == MediaAccess.CDROM)) {
            i = true;
        } else {
            i = false;
        }
        if (!i) {
            localMediaAccess.open(paramString);
            localMediaAccess.close();
        }
        this.target = paramString;
        this.changing_disks = true;
        this.scsi.change_disk();
    }

    public void run() {
        while(true) {
            this.changing_disks = false;

            try {
                if ((this.device == FLOPPY) || (this.device == USBKEY)) {
                    this.scsi = new SCSIFloppy(this.socket, this.in, this.out, this.target);
                } else if (this.device == CDROM) {
                    this.scsi = new SCSIcdimage(this.socket, this.in, this.out, this.target, this.v);
                } else {
                    D.println(D.FATAL, "Unsupported virtual device " + this.device);
                    return;
                }
            } catch (Exception e) {
                D.println(D.FATAL, "Exception while opening " + this.target + "(" + e + ")");
            }


            this.scsi.setWriteProtect(this.writeProtect);
            while (true) {
                if (((this.device == FLOPPY) || (this.device == USBKEY)) && (this.scsi.getWriteProtect())) {
                    this.v.readOnlyCheckbox.setState(true);
                    this.v.readOnlyCheckbox.setEnabled(false);
                    this.v.readOnlyCheckbox.repaint();
                }
                try {
                    this.scsi.process();
                } catch (IOException localIOException1) {
                    D.println(D.INFORM, "Exception in Connection::run() " + localIOException1);
                    localIOException1.printStackTrace();


                    D.println(D.VERBOSE, "Closing scsi and socket");
                    try {
                        this.scsi.close();
                        if (!this.changing_disks) internalClose();
                    } catch (IOException localIOException2) {
                        D.println(D.FATAL, "Exception closing connection " + localIOException2);
                    }
                    this.scsi = null;
                    if ((this.device == FLOPPY) || (this.device == USBKEY)) {
                        this.v.readOnlyCheckbox.setEnabled(true);
                        this.v.readOnlyCheckbox.repaint();
                    }

                    if (this.changing_disks) break;

                    if ((this.device == FLOPPY) || (this.device == USBKEY)) {
                        this.v.fdDisconnect();
                    } else if (this.device == CDROM) {
                        this.v.cdDisconnect();
                    }
                }
            }
        }
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/Connection.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */