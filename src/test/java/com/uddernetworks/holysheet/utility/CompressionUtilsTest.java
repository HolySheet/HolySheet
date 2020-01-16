//package com.uddernetworks.holysheet.utility;
//
//import org.junit.jupiter.api.Test;
//
//import java.nio.charset.StandardCharsets;
//import java.util.concurrent.ThreadLocalRandom;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class CompressionUtilsTest {
//
//    private static final String staticText = "This is some text";
//    // Compressed bytes of the string above, with the time cleared
//    private static final byte[] staticCompressed = new byte[] {80, 75, 3, 4, 20, 0, 8, 8, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 100, 97, 116, 97, 11, -55, -56, 44, 86, 0, -94, -30, -4, -36, 84, -123, -110, -44, -118, 18, 0, 80, 75, 7, 8, 123, 127, 75, -97, 17, 0, 0, 0, 17, 0, 0, 0};
//
//    @Test
//    public void compressStatic() {
//        var compressedBytes = CompressionUtils.compress(staticText.getBytes(StandardCharsets.UTF_8));
//        assertArrayEquals(clearTime(compressedBytes), staticCompressed);
//    }
//
//    @Test
//    public void uncompressStatic() {
//        var uncompressedBytes = CompressionUtils.uncompress(staticCompressed);
//        assertArrayEquals(uncompressedBytes, staticText.getBytes(StandardCharsets.UTF_8));
//    }
//
//    @Test
//    public void zipCompare() {
//        for (int i = 0; i < 10; i++) {
//            var bytes = new byte[ThreadLocalRandom.current().nextInt(10000)];
//            ThreadLocalRandom.current().nextBytes(bytes);
//            var compressed = CompressionUtils.compress(bytes);
//            var uncompressed = CompressionUtils.uncompress(compressed);
//            assertArrayEquals(uncompressed, bytes);
//        }
//    }
//
//    /**
//     * Clears the last modification date/time of the Zip header, offsets 10-13 as per the zip spec.
//     *
//     * @param zipBytes The zip bytes
//     * @return The non-time specific time bytes
//     * @see <a href="https://en.wikipedia.org/wiki/Zip_(file_format)#Local_file_header">https://en.wikipedia.org/wiki/Zip_(file_format)#Local_file_header</a>
//     */
//    private byte[] clearTime(byte[] zipBytes) {
//        for (int i = 10; i <= 13; i++) {
//            zipBytes[i] = 0;
//        }
//        return zipBytes;
//    }
//}