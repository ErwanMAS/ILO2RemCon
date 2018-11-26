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
    public static final int F3_120M_512 = 13;
    public static final int ImageFile = 100;
    public static final int NoRootDir = 1;
    public static final int Removable = 2;
    public static final int Fixed = 3;
    public static final int Remote = 4;
    public static final int CDROM = 5;
    public static final int Ramdisk = 6;
    public static String dllext = "";
    static int dio_setup = -1;

    DirectIO dio;
    File file;
    private RandomAccessFile randomAccessFile;
    private boolean targetIsDevice = false;
    private boolean readonly = false;
    int zero_offset = 0;

    public static void cleanup() {
        String sep = System.getProperty("file.separator");
        String tmpDir = System.getProperty("java.io.tmpdir");
        String osName = System.getProperty("os.name").toLowerCase();
        if (tmpDir == null) {
            tmpDir = osName.startsWith("windows") ? "C:\\TEMP" : "/tmp";
        }
        File fTmpDir = new File(tmpDir);
        String[] fileNames = fTmpDir.list();

        if (!tmpDir.endsWith(sep)) {
            tmpDir = tmpDir + sep;
        }
        for (String fileName : fileNames) {
            if ((fileName.startsWith("cpqma-")) && (fileName.endsWith(dllext))) {
                File currentFile = new File(tmpDir + fileName);
                currentFile.delete();
            }
        }
    }

    public int open(String path, int flags) throws IOException {
        this.targetIsDevice = (flags & 0x1) == 1;
        boolean isDiskImage = (flags & 0x2) == 2;


        this.zero_offset = 0;
        if (this.targetIsDevice) {
            if (dio_setup != 0) {
                throw new IOException("DirectIO not possible (" + dio_setup + ")");
            }
            if (this.dio == null)
                this.dio = new DirectIO();
            return this.dio.open(path);
        }
        this.readonly = false;
        this.file = new File(path);
        if ((!this.file.exists()) && !isDiskImage)
            throw new IOException("File " + path + " does not exist");
        if (this.file.isDirectory()) {
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
            this.zero_offset = (buf[14] | buf[15] << 8);
        }

        return 0;
    }

    public int close() throws IOException {
        if (this.targetIsDevice) {
            return this.dio.close();
        }
        this.randomAccessFile.close();

        return 0;
    }

    public void read(long offset, int length, byte[] data) throws IOException {
        offset += this.zero_offset;
        if (this.targetIsDevice) {
            int i = this.dio.read(offset, length, data);
            if (i != 0) {
                throw new IOException("DirectIO read error (" + this.dio.sysError(-i) + ")");
            }
        } else {
            this.randomAccessFile.seek(offset);
            this.randomAccessFile.read(data, 0, length);
        }
    }

    public void write(long offset, int length, byte[] data) throws IOException {
        offset += this.zero_offset;
        if (this.targetIsDevice) {
            int i = this.dio.write(offset, length, data);
            if (i != 0) {
                throw new IOException("DirectIO write error (" + this.dio.sysError(-i) + ")");
            }
        } else {
            this.randomAccessFile.seek(offset);
            this.randomAccessFile.write(data, 0, length);
        }
    }

    public long size() throws IOException {
        long size;
        if (this.targetIsDevice) {
            size = this.dio.size();
        } else {
            size = this.randomAccessFile.length() - this.zero_offset;
        }
        return size;
    }

    public int format(int mediaType, int startCylinder, int endCylinder, int startHead, int endHead) throws IOException {
        if (this.targetIsDevice) {
            this.dio.media_type = mediaType;
            this.dio.StartCylinder = startCylinder;
            this.dio.EndCylinder = endCylinder;
            this.dio.StartHead = startHead;
            this.dio.EndHead = endHead;
            return this.dio.format();
        }

        return 0;
    }

    public String[] devices() {
        if (dio_setup != 0)
            return null;
        if (this.dio == null)
            this.dio = new DirectIO();
        return this.dio.devices();
    }

    public int devtype(String paramString) {
        if (dio_setup != 0)
            return 0;
        if (this.dio == null)
            this.dio = new DirectIO();
        return this.dio.devtype(paramString);
    }

    public int scsi(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3) {
        return scsi(paramArrayOfByte1, paramInt1, paramInt2, paramArrayOfByte2, paramArrayOfByte3, 0);
    }

    public int scsi(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, int paramInt3) {
        int i;
        if (this.targetIsDevice) {
            i = this.dio.scsi(paramArrayOfByte1, paramInt1, paramInt2, paramArrayOfByte2, paramArrayOfByte3, paramInt3);
        } else {
            i = -1;
        }
        return i;
    }

    public boolean wp() {
        boolean wp;
        if (this.targetIsDevice) {
            wp = this.dio.wp == 1;
        } else {
            wp = this.readonly;
        }
        return wp;
    }

    public int type() {
        if ((this.targetIsDevice) && (this.dio != null))
            return this.dio.media_type;
        if (this.randomAccessFile != null) {
            return 100;
        }
        return 0;
    }

    public int dllExtract(String paramString1, String paramString2) {
        ClassLoader localClassLoader = MediaAccess.class.getClassLoader();


        byte[] buffer = new byte[4096];

        D.println(D.INFORM, "dllExtract trying " + paramString1);
        if (localClassLoader.getResource(paramString1) == null) {
            return -1;
        }
        D.println(D.INFORM, "Extracting" + localClassLoader.getResource(paramString1).toExternalForm() + " to " + paramString2);

        try {
            java.io.InputStream localInputStream = localClassLoader.getResourceAsStream(paramString1);
            java.io.FileOutputStream localFileOutputStream = new java.io.FileOutputStream(paramString2);
            int bytesRead;
            while ((bytesRead = localInputStream.read(buffer, 0, 4096)) != -1) {
                localFileOutputStream.write(buffer, 0, bytesRead);
            }
            localInputStream.close();
            localFileOutputStream.close();
        } catch (IOException e) {
            D.println(D.FATAL, "dllExtract: " + e);
            return -2;
        }

        return 0;
    }

    public int setup_DirectIO() {
        String sep = System.getProperty("file.separator");
        String tmpDir = System.getProperty("java.io.tmpdir");
        String osName = System.getProperty("os.name").toLowerCase();
        String vmName = System.getProperty("java.vm.name");
        String platform = "unknown";

        if (tmpDir == null) {
            tmpDir = osName.startsWith("windows") ? "C:\\TEMP" : "/tmp";
        }

        if (osName.startsWith("windows")) {
            if (vmName.contains("64")) {
                System.out.println("virt: Detected win 64bit jvm");
                platform = "x86-win64";
            } else {
                System.out.println("virt: Detected win 32bit jvm");
                platform = "x86-win32";
            }
            dllext = ".dll";
        } else if (osName.startsWith("linux")) {
            if (vmName.contains("64")) {
                System.out.println("virt: Detected 64bit linux jvm");
                platform = "x86-linux64";
            } else {
                System.out.println("virt: Detected 32bit linux jvm");
                platform = "x86-linux32";
            }
        }


        File fTmpDir = new File(tmpDir);
        if (!fTmpDir.exists()) {
            fTmpDir.mkdir();
        }

        if (!tmpDir.endsWith(sep)) {
            tmpDir = tmpDir + sep;
        }


        tmpDir = tmpDir + "cpqma-" + Integer.toHexString(virtdevs.UID) + dllext;

        System.out.println("Checking for " + tmpDir);
        File nativeLibrary = new File(tmpDir);
        if (nativeLibrary.exists()) {
            System.out.println("DLL present");
            dio_setup = 0;
            return 0;
        }
        System.out.println("DLL not present");

        int returnValue = dllExtract("com/hp/ilo2/virtdevs/cpqma-" + platform, tmpDir);

        dio_setup = returnValue;
        return returnValue;
    }
}