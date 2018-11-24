package com.hp.ilo2.virtdevs;

import java.io.File;
import java.io.IOException;

public class DirectIOMediaAccess extends BaseMediaAccess {
    public static String dllext = "";

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

    DirectIO dio;
    static int dio_setup = -1;

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
        for (int i = 0; i < fileNames.length; i++) {
            if ((fileNames[i].startsWith("cpqma-")) && (fileNames[i].endsWith(dllext))) {
                File currentFile = new File(tmpDir + fileNames[i]);
                currentFile.delete();
            }
        }
    }

    public int open(String path) throws IOException {
        this.zeroOffset = 0;

        if (dio_setup != 0) {
            throw new IOException("DirectIO not possible (" + dio_setup + ")");
        }
        if (this.dio == null)
            this.dio = new DirectIO();
        return this.dio.open(path);
    }

    public int close() {
        return this.dio.close();
    }

    public void read(long offset, int length, byte[] data) throws IOException {
        offset += this.zeroOffset;

        int i = this.dio.read(offset, length, data);
        if (i != 0) {
            throw new IOException("DirectIO read error (" + this.dio.sysError(-i) + ")");
        }
    }

    public void write(long offset, int length, byte[] data) throws IOException {
        offset += this.zeroOffset;

        int result = this.dio.write(offset, length, data);
        if (result != 0) {
            throw new IOException("DirectIO write error (" + this.dio.sysError(-result) + ")");
        }
    }

    public long size() throws IOException {
        return this.dio.size();
    }

    public int format(int mediaType, int startCylinder, int endCylinder, int startHead, int endHead) {
        this.dio.media_type = mediaType;
        this.dio.StartCylinder = startCylinder;
        this.dio.EndCylinder = endCylinder;
        this.dio.StartHead = startHead;
        this.dio.EndHead = endHead;
        return this.dio.format();
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
        return this.dio.scsi(paramArrayOfByte1, paramInt1, paramInt2, paramArrayOfByte2, paramArrayOfByte3, paramInt3);
    }

    public boolean wp() {
        return this.dio.wp == 1;
    }

    public int type() {
        if ((this.dio != null))
            return this.dio.media_type;
        else return 0;
    }

    public static int dllExtract(String paramString1, String paramString2) {
        ClassLoader localClassLoader = DirectIOMediaAccess.class.getClassLoader();


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

    public static int setup_DirectIO() {
        int i = 0;
        String sep = System.getProperty("file.separator");
        String tmpDir = System.getProperty("java.io.tmpdir");
        String osName = System.getProperty("os.name").toLowerCase();
        String vmName = System.getProperty("java.vm.name");
        String archStr = "unknown";

        if (tmpDir == null) {
            tmpDir = osName.startsWith("windows") ? "C:\\TEMP" : "/tmp";
        }

        if (osName.startsWith("windows")) {
            if (vmName.contains("64")) {
                System.out.println("virt: Detected win 64bit jvm");
                archStr = "x86-win64";
            } else {
                System.out.println("virt: Detected win 32bit jvm");
                archStr = "x86-win32";
            }
            dllext = ".dll";
        } else if (osName.startsWith("linux")) {
            if (vmName.contains("64")) {
                System.out.println("virt: Detected 64bit linux jvm");
                archStr = "x86-linux64";
            } else {
                System.out.println("virt: Detected 32bit linux jvm");
                archStr = "x86-linux32";
            }
        }


        File localFile1 = new File(tmpDir);
        if (!localFile1.exists()) {
            localFile1.mkdir();
        }

        if (!tmpDir.endsWith(sep)) {
            tmpDir = tmpDir + sep;
        }
        tmpDir = tmpDir + "cpqma-" + Integer.toHexString(virtdevs.UID) + dllext;


        System.out.println("Checking for " + tmpDir);
        File localFile2 = new File(tmpDir);
        if (localFile2.exists()) {
            System.out.println("DLL present");
            dio_setup = 0;
            return 0;
        }
        System.out.println("DLL not present");

        i = dllExtract("com/hp/ilo2/virtdevs/cpqma-" + archStr, tmpDir);

        dio_setup = i;
        return i;
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/MediaAccess.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */