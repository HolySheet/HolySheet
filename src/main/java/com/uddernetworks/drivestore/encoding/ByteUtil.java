package com.uddernetworks.drivestore.encoding;

import java.nio.ByteBuffer;

public class ByteUtil {

    public static byte getBit(long number, int offset) {
        return (byte) getLongBit(number, offset);
    }

    public static long getLongBit(long number, int offset) {
        return (number >> offset) & 1;
    }

    public static byte getBitRange(long number, int offset, int length) {
        return (byte) getLongBitRange(number, offset, length);
    }

    public static long getLongBitRange(long number, int offset, int length) {
        long rightShifted = number >>> offset;
        long mask = (1L << length) - 1L;
        return rightShifted & mask;
    }

    public static byte[] longToBytes(long l) {
        return ByteBuffer.allocate(Long.BYTES).putLong(l).array();
    }

    public static long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }

    public static String humanReadableByteCountSI(long bytes) {
        String s = bytes < 0 ? "-" : "";
        long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        return b < 1000L ? bytes + " B"
                : b < 999_950L ? String.format("%s%.1f kB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f MB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f GB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f TB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f PB", s, b / 1e3)
                : String.format("%s%.1f EB", s, b / 1e6);
    }
}
