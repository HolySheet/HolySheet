package com.uddernetworks.holysheet.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class CompressionUtils {

    public static byte[] compress(byte[] input) {
        try (var out = new ByteArrayOutputStream();
             var zipOut = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zipOut.putNextEntry(new ZipEntry("data"));
            zipOut.write(input);
            zipOut.closeEntry();
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public static byte[] uncompress(byte[] input) {
        try (var inputStream = new ZipInputStream(new ByteArrayInputStream(input), StandardCharsets.UTF_8)) {
            inputStream.getNextEntry();
            return inputStream.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public static ByteArrayOutputStream uncompressToOutputStream(byte[] input) {
        var out = new ByteArrayOutputStream();
        try (var inputStream = new ZipInputStream(new ByteArrayInputStream(input), StandardCharsets.UTF_8)) {
            inputStream.getNextEntry();
            out.write(inputStream.readAllBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

}
