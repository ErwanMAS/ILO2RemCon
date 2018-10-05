package com.hp.ilo2.virtdevs;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
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
    RandomAccessFile raf;
    boolean dev = false;
    boolean readonly = false;
    int zero_offset = 0;

    public static void cleanup() {
        String str1 = System.getProperty("file.separator");
        String str2 = System.getProperty("java.io.tmpdir");
        String str3 = System.getProperty("os.name").toLowerCase();
        if (str2 == null) {
            str2 = str3.startsWith("windows") ? "C:\\TEMP" : "/tmp";
        }
        File localFile1 = new File(str2);
        String[] arrayOfString = localFile1.list();

        if (!str2.endsWith(str1)) {
            str2 = str2 + str1;
        }
        for (int i = 0; i < arrayOfString.length; i++) {
            if ((arrayOfString[i].startsWith("cpqma-")) && (arrayOfString[i].endsWith(dllext))) {
                File localFile2 = new File(str2 + arrayOfString[i]);
                localFile2.delete();
            }
        }
    }

    public int open(String paramString, int paramInt) throws IOException {
        this.dev = ((paramInt & 0x1) == 1);
        int i = (paramInt & 0x2) == 2 ? 1 : 0;


        this.zero_offset = 0;
        if (this.dev) {
            if (dio_setup != 0) {
                throw new IOException("DirectIO not possible (" + dio_setup + ")");
            }
            if (this.dio == null)
                this.dio = new DirectIO();
            return this.dio.open(paramString);
        }
        this.readonly = false;
        this.file = new File(paramString);
        if ((!this.file.exists()) && (i == 0))
            throw new IOException("File " + paramString + " does not exist");
        if (this.file.isDirectory()) {
            throw new IOException("File " + paramString + " is a directory");
        }


        try {
            this.raf = new RandomAccessFile(paramString, "rw");
        } catch (IOException localIOException) {
            if (i == 0) {
                this.raf = new RandomAccessFile(paramString, "r");
                this.readonly = true;
            } else {
                throw localIOException;
            }
        }

        byte[] arrayOfByte = new byte['Ȁ'];


        read(0L, 512, arrayOfByte);
        if ((arrayOfByte[0] == 67) && (arrayOfByte[1] == 80) && (arrayOfByte[2] == 81) && (arrayOfByte[3] == 82) && (arrayOfByte[4] == 70) && (arrayOfByte[5] == 66) && (arrayOfByte[6] == 76) && (arrayOfByte[7] == 79)) {


            this.zero_offset = (arrayOfByte[14] | arrayOfByte[15] << 8);
        }
        arrayOfByte = null;

        return 0;
    }

    public int close() throws IOException {
        if (this.dev) {
            return this.dio.close();
        }
        this.raf.close();

        return 0;
    }

    public void read(long paramLong, int paramInt, byte[] paramArrayOfByte) throws IOException {
        paramLong += this.zero_offset;
        if (this.dev) {
            int i = this.dio.read(paramLong, paramInt, paramArrayOfByte);
            if (i != 0) {
                throw new IOException("DirectIO read error (" + this.dio.sysError(-i) + ")");
            }
        } else {
            this.raf.seek(paramLong);
            this.raf.read(paramArrayOfByte, 0, paramInt);
        }
    }

    public void write(long paramLong, int paramInt, byte[] paramArrayOfByte) throws IOException {
        paramLong += this.zero_offset;
        if (this.dev) {
            int i = this.dio.write(paramLong, paramInt, paramArrayOfByte);
            if (i != 0) {
                throw new IOException("DirectIO write error (" + this.dio.sysError(-i) + ")");
            }
        } else {
            this.raf.seek(paramLong);
            this.raf.write(paramArrayOfByte, 0, paramInt);
        }
    }

    public long size() throws IOException {
        long l;
        if (this.dev) {
            l = this.dio.size();
        } else {
            l = this.raf.length() - this.zero_offset;
        }
        return l;
    }

    public int format(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5) throws IOException {
        if (this.dev) {
            this.dio.media_type = paramInt1;
            this.dio.StartCylinder = paramInt2;
            this.dio.EndCylinder = paramInt3;
            this.dio.StartHead = paramInt4;
            this.dio.EndHead = paramInt5;
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
        if (this.dev) {
            i = this.dio.scsi(paramArrayOfByte1, paramInt1, paramInt2, paramArrayOfByte2, paramArrayOfByte3, paramInt3);
        } else {
            i = -1;
        }
        return i;
    }

    public boolean wp() {
        boolean bool;
        if (this.dev) {
            bool = this.dio.wp == 1;
        } else {
            bool = this.readonly;
        }
        return bool;
    }

    public int type() {
        if ((this.dev) && (this.dio != null))
            return this.dio.media_type;
        if (this.raf != null) {
            return 100;
        }
        return 0;
    }

    public int dllExtract(String paramString1, String paramString2) {
        ClassLoader localClassLoader = getClass().getClassLoader();


        byte[] arrayOfByte = new byte['က'];

        D.println(D.INFORM, "dllExtract trying " + paramString1);
        if (localClassLoader.getResource(paramString1) == null) {
            return -1;
        }
        D.println(D.INFORM, "Extracting" + localClassLoader.getResource(paramString1).toExternalForm() + " to " + paramString2);

        try {
            java.io.InputStream localInputStream = localClassLoader.getResourceAsStream(paramString1);
            java.io.FileOutputStream localFileOutputStream = new java.io.FileOutputStream(paramString2);
            int i;
            while ((i = localInputStream.read(arrayOfByte, 0, 4096)) != -1) {
                localFileOutputStream.write(arrayOfByte, 0, i);
            }
            localInputStream.close();
            localFileOutputStream.close();
        } catch (IOException localIOException) {
            D.println(D.FATAL, "dllExtract: " + localIOException);
            return -2;
        }

        return 0;
    }

    public int setup_DirectIO() {
        int i = 0;
        String str1 = System.getProperty("file.separator");
        String str2 = System.getProperty("java.io.tmpdir");
        String str3 = System.getProperty("os.name").toLowerCase();
        String str4 = System.getProperty("java.vm.name");
        String str5 = "unknown";

        if (str2 == null) {
            str2 = str3.startsWith("windows") ? "C:\\TEMP" : "/tmp";
        }

        if (str3.startsWith("windows")) {
            if (str4.indexOf("64") != -1) {
                System.out.println("virt: Detected win 64bit jvm");
                str5 = "x86-win64";
            } else {
                System.out.println("virt: Detected win 32bit jvm");
                str5 = "x86-win32";
            }
            dllext = ".dll";
        } else if (str3.startsWith("linux")) {
            if (str4.indexOf("64") != -1) {
                System.out.println("virt: Detected 64bit linux jvm");
                str5 = "x86-linux64";
            } else {
                System.out.println("virt: Detected 32bit linux jvm");
                str5 = "x86-linux32";
            }
        }


        File localFile1 = new File(str2);
        if (!localFile1.exists()) {
            localFile1.mkdir();
        }

        if (!str2.endsWith(str1)) {
            str2 = str2 + str1;
        }
        str2 = str2 + "cpqma-" + Integer.toHexString(virtdevs.UID) + dllext;


        System.out.println("Checking for " + str2);
        File localFile2 = new File(str2);
        if (localFile2.exists()) {
            System.out.println("DLL present");
            dio_setup = 0;
            return 0;
        }
        System.out.println("DLL not present");

        i = dllExtract("com/hp/ilo2/virtdevs/cpqma-" + str5, str2);

        dio_setup = i;
        return i;
    }
}