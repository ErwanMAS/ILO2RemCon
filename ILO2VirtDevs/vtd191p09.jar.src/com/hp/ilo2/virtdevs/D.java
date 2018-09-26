package com.hp.ilo2.virtdevs;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;


public class D {
    public static final int NONE = -1;
    public static final int FATAL = 0;
    public static final int INFORM = 1;
    public static final int WARNING = 2;
    public static final int VERBOSE = 3;
    public static int debug = 0;
    public static PrintStream out;

    static {
        String str = virtdevs.prop.getProperty("com.hp.ilo2.virtdevs.debugfile");
        try {
            if (str == null) {
                out = System.out;
            } else {
                out = new PrintStream(new FileOutputStream(str));
            }
        } catch (FileNotFoundException e) {
            out = System.out;
            out.println("Exception trying to open debug trace\n" + e);
        }
        str = virtdevs.prop.getProperty("com.hp.ilo2.virtdevs.debug");

        if (str != null) {
            debug = Integer.valueOf(str);
        }
    }

    public static void println(int level, String message) {
        if (debug >= level) out.println(message);
    }

    public static void print(int level, String message) {
        if (debug >= level) out.println(message);
    }


    public static String hex(byte value, int padToLength) {
        return hex(value & 0xFF, padToLength);
    }

    public static String hex(short value, int padToLength) {
        return hex(value & 0xFFFF, padToLength);
    }

    public static String hex(int value, int padToLength) {
        String str = Integer.toHexString(value);
        while (str.length() < padToLength) {
            str = "0" + str;
        }
        return str;
    }

    public static String hex(long value, int padToLength) {
        String str = Long.toHexString(value);
        while (str.length() < padToLength) {
            str = "0" + str;
        }
        return str;
    }

    public static void hexdump(int level, byte[] data, int length) {
        if (debug < level) {
            return;
        }
        if (length == 0) length = data.length;
        for (int i = 0; i < length; i++) {
            if (i % 16 == 0)
                out.print("\n");
            out.print(hex(data[i], 2) + " ");
        }
        out.print("\n");
    }
}