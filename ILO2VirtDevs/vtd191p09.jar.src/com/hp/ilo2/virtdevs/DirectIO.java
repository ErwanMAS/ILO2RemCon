package com.hp.ilo2.virtdevs;

public class DirectIO {
    public static int keydrive = 1;

    static {
        String dllPathProperty = "cpqma-" + Integer.toHexString(virtdevs.UID) + MediaAccess.dllext;

        String sep = System.getProperty("file.separator");
        String dllPath = System.getProperty("java.io.tmpdir");
        String osName = System.getProperty("os.name").toLowerCase();

        if (dllPath == null) {
            dllPath = osName.startsWith("windows") ? "C:\\TEMP" : "/tmp";
        }

        if (!dllPath.endsWith(sep)) {
            dllPath = dllPath + sep;
        }
        dllPath = dllPath + dllPathProperty;


        dllPathProperty = virtdevs.prop.getProperty("com.hp.ilo2.virtdevs.dll");
        String keydrive = virtdevs.prop.getProperty("com.hp.ilo2.virtdevs.keydrive", "true");
        DirectIO.keydrive = Boolean.valueOf(keydrive) ? 1 : 0;

        if (dllPathProperty != null) dllPath = dllPathProperty;
        System.out.println("Loading " + dllPath);
        System.load(dllPath);
    }

    public int media_type;
    public int StartCylinder;
    public int EndCylinder;
    public int StartHead;
    public int EndHead;
    public int Cylinders;
    public int TracksPerCyl;
    public int SecPerTrack;
    public int BytesPerSec;
    public int media_size;
    public int filehandle = -1;
    public int aux_handle = -1;
    public long bufferaddr;
    public int wp;
    public int misc0;
    public int PhysicalDevice;

    public native int open(String paramString);

    public native int close();

    public native int read(long paramLong, int paramInt, byte[] paramArrayOfByte);

    public native int write(long paramLong, int paramInt, byte[] paramArrayOfByte);

    public native long size();

    public native int format();

    public native String[] devices();

    public native int devtype(String paramString);

    public native int scsi(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, int paramInt3);

    public native String sysError(int paramInt);

    protected void finalize() {
        if (this.filehandle != -1)
            close();
    }
}


/* Location:              /Users/fridtjof/Coding/rc175p11_dec/vtd191p09.jar!/com/hp/ilo2/virtdevs/DirectIO.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */