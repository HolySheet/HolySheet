package com.uddernetworks.holysheet.compression.implementations;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.uddernetworks.grpc.HolysheetService.UploadRequest.Compression;
import com.uddernetworks.holysheet.compression.CompressionAlgorithm;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Chris L R.
 * @version Mar 15, 2020
 */
public class ZstdCompression implements CompressionAlgorithm {

    @Override
    public Compression getCompressionType() {
        return Compression.ZSTD;
    }

    private void internalDecompressFile(File source, File destination) throws IOException {
        FileInputStream inputStream = new FileInputStream(source);
        ZstdInputStream zstdInputStream = new ZstdInputStream(inputStream);

        FileOutputStream outputStream = new FileOutputStream(destination);
        zstdInputStream.transferTo(outputStream);

        zstdInputStream.close();
        outputStream.close();
    }

    private Path createTempPath(File file) throws IOException {
        Path path = Path.of(file.getAbsolutePath()).getParent();
        return Files.createTempFile(path, "holysheet-", "-zstd");
    }

    @Override
    public long decompressFile(File file) throws IOException {
        if (!file.exists()) {
            return -1;
        }

        Path tempPath = createTempPath(file);
        File tempFile = tempPath.toFile();

        internalDecompressFile(file, tempFile);

        if (file.delete() && tempFile.renameTo(file)) {
            tempPath.toFile().delete();
            return file.length();
        } else {
            return tempFile.length();
        }
    }

    public InputStream decompressStream(InputStream inputStream) throws IOException {
        return new ZstdInputStream(inputStream);
    }

}
