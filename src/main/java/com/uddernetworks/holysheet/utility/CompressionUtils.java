package com.uddernetworks.holysheet.utility;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class CompressionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionUtils.class);

    public static Optional<InputStream> compress(InputStream inputStream) {
        try {
            var file = File.createTempFile("compressed-" + inputStream.hashCode(), ".zip");
            try (var zipOut = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                zipOut.putNextEntry(new ZipEntry("data"));
                IOUtils.copy(inputStream, zipOut);
                zipOut.closeEntry();
            }

            return Optional.of(new FileInputStream(file));
        } catch (IOException e) {
            LOGGER.error("An error occurred while compressing", e);
        }

        return Optional.empty();
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
