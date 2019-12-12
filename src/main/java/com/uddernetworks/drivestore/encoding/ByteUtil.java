package com.uddernetworks.drivestore.encoding;

import java.nio.ByteBuffer;

public class ByteUtil {

    private static final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

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

    /**
     * Following two methods:
     * https://stackoverflow.com/a/29132118/3929546
     */
    public static byte[] longToBytes(long l) {
        var result = new byte[8];
        for (int i = 0; i <= 7; i++) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static long bytesToLong(byte[] bytes) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (bytes[i] & 0xFF);
        }
        return result;
    }
}
