package com.uddernetworks.holysheet.compression.implementations;

import com.github.luben.zstd.ZstdDecompressCtx;
import com.github.luben.zstd.ZstdDirectBufferDecompressingStream;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.uddernetworks.grpc.HolysheetService.UploadRequest.Compression;
import com.uddernetworks.holysheet.compression.CompressionAlgorithm;

import java.io.*;
import java.nio.channels.FileChannel;
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

    private Path createTempPath(File file) throws IOException {
        Path path = Path.of(file.getAbsolutePath()).getParent();
        return Files.createTempFile(path, "holysheet-", "-zstd");
    }

    private Path createTempPath() throws IOException {
        return Files.createTempFile("holysheet-", "-zstd");
    }

    private void internalDecompressFile(File source, Path destination) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(source);
        ZstdInputStream zstdInputStream = new ZstdInputStream(fileInputStream);

        FileOutputStream outputStream = new FileOutputStream(destination.toFile());
        zstdInputStream.transferTo(outputStream);

        outputStream.close();
        zstdInputStream.close();
    }

    private void internalCompressFile(File source, Path destination) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(source);
        FileOutputStream outputStream = new FileOutputStream(destination.toFile());

        ZstdOutputStream zstdOutputStream = new ZstdOutputStream(outputStream);
        zstdOutputStream.setCloseFrameOnFlush(true);

        fileInputStream.transferTo(zstdOutputStream);
        zstdOutputStream.flush();

        fileInputStream.close();
        zstdOutputStream.close();
    }

    @Override
    public boolean decompressFile(File file, Path destination) {
        if (!file.exists()) {
            return false;
        }

        try {
            internalDecompressFile(file, destination);
            return true;
        } catch(IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public Path decompressToTemp(File file) {
        try {
            Path tempPath = createTempPath(file);
            tempPath.toFile().deleteOnExit();

            if (decompressFile(file, tempPath)) {
                return tempPath;
            }
        } catch(IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean decompressFile(File file) {
        Path tempPath = decompressToTemp(file);

        if (tempPath != null) {
            File tempFile = tempPath.toFile();
            return file.delete() && tempFile.renameTo(file);
        }

        return false;
    }

    @Override
    public boolean compressFile(File file, Path destination) {
        if (!file.exists()) {
            return false;
        }

        try {
            internalCompressFile(file, destination);
            return true;
        } catch(IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public Path compressToTemp(File file) {
        try {
            Path tempPath = createTempPath(file);
            tempPath.toFile().deleteOnExit();

            if (compressFile(file, tempPath)) {
                return tempPath;
            }
        } catch(IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean compressFile(File file) {
        try {
            Path tempPath = createTempPath(file);
            File tempFile = tempPath.toFile();

            tempFile.deleteOnExit();

            if (compressFile(file, tempPath)) {
                return file.delete() && tempFile.renameTo(file);
            }
        } catch(IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

}
