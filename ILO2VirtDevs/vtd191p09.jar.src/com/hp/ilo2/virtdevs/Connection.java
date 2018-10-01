package com.hp.ilo2.virtdevs;

import java.io.IOException;
import java.net.Socket;
import javax.swing.Timer;

public class Connection implements Runnable, java.awt.event.ActionListener {
    public static final int FLOPPY = 1;
    public static final int CDROM = 2;
    public static final int USBKEY = 3;
    String host;
    int port;
    int device;
    String target;
    byte[] pre;
    byte[] key;
    private Socket socket;
    private java.io.InputStream in;
    private java.io.BufferedOutputStream out;
    private boolean targetIsDevice;
    private SCSI scsi;
    private boolean writeProtect = false;
    private boolean changing_disks;
    private virtdevs v;
    private VMD5 digest;

    public Connection(String host, int port, int device, String target, int paramInt3, byte[] pre, byte[] key, virtdevs virtdevs)
            throws IOException {
        this.host = host;
        this.port = port;
        this.device = device;
        this.target = target;
        this.pre = pre;
        this.key = key;
        this.v = virtdevs;

        MediaAccess localMediaAccess = new MediaAccess();
        int i = localMediaAccess.devtype(target);
        if ((i == 2) || (i == 5)) {
            this.targetIsDevice = true;
            D.println(D.FATAL, "Got CD or removable connection\n");
        } else {
            this.targetIsDevice = false;
            D.println(D.FATAL, "Got NO CD or removable connection\n");
        }

        int j = localMediaAccess.open(target, this.targetIsDevice);
        long l = localMediaAccess.size();
        localMediaAccess.close();

        if ((this.device == FLOPPY) && (l > 2949120L)) {
            this.device = USBKEY;
        }
        this.digest = new VMD5();
    }

    public int connect() throws java.net.UnknownHostException, IOException {
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
        if (this.targetIsDevice) {
            int tmp227_226 = 1;
            buffer[tmp227_226] = ((byte) (buffer[tmp227_226] | 0xFFFFFF80));
        }
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
                Timer localTimer = new Timer(2000, this);
                localTimer.setRepeats(false);
                localTimer.start();
                this.scsi.change_disk();
                localTimer.stop();
            } catch (Exception localException) {
                this.scsi.change_disk();
            }
        } else {
            internal_close();
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent paramActionEvent) {
        try {
            internal_close();
        } catch (Exception ignored) {}
    }

    public void internal_close() throws IOException {
        if (this.socket != null) this.socket.close();
        this.socket = null;
        this.in = null;
        this.out = null;
    }

    public void setWriteProt(boolean paramBoolean) {
        this.writeProtect = paramBoolean;
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
            localMediaAccess.open(paramString, 0);
            localMediaAccess.close();
        }
        this.target = paramString;
        this.targetIsDevice = i;
        this.changing_disks = true;
        this.scsi.change_disk();
    }

    public void run() {
        do {
            this.changing_disks = false;
            try {
                if ((this.device == FLOPPY) || (this.device == USBKEY)) {
                    this.scsi = new SCSIFloppy(this.socket, this.in, this.out, this.target, this.targetIsDevice);
                } else if (this.device == CDROM) {
                    if (this.targetIsDevice) {
                        this.scsi = new SCSIcdrom(this.socket, this.in, this.out, this.target);
                    } else {
                        this.scsi = new SCSIcdimage(this.socket, this.in, this.out, this.target, this.v);
                    }
                } else {
                    D.println(D.FATAL, "Unsupported virtual device " + this.device);

                    return;
                }
            } catch (Exception e) {
                D.println(D.FATAL, "Exception while opening " + this.target + "(" + e + ")");
            }


            this.scsi.setWriteProtect(this.writeProtect);
            for (; ; ) {
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
                        if (!this.changing_disks) internal_close();
                    } catch (IOException localIOException2) {
                        D.println(D.FATAL, "Exception closing connection " + localIOException2);
                    }
                    this.scsi = null;
                    if ((this.device == FLOPPY) || (this.device == USBKEY)) {
                        this.v.readOnlyCheckbox.setEnabled(true);
                        this.v.readOnlyCheckbox.repaint();
                    }
                }
            }
        } while (this.changing_disks);
        /*if ((this.device == FLOPPY) || (this.device == USBKEY)) {
          this.v.fdDisconnect();
        } else if (this.device == CDROM) {
          this.v.cdDisconnect();
        }*/
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/Connection.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */