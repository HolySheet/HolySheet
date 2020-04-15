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
public class ZstdCompression extends AbstractCompressionAlgorithm {

    @Override
    public Compression getCompressionType() {
        return Compression.ZSTD;
    }

    /**
     * Decompress a {@link File} source, and store it in a {@link Path} destination.
     *
     * @param source      {@link File} to decompress.
     * @param destination {@link Path} to destination.
     * @throws IOException This whole method could throw IOExceptions.
     */
    private void internalDecompressFile(File source, Path destination) throws IOException {
        final var fileInput = new BufferedInputStream(new FileInputStream(source));

        try (
                final var zstdInput = new ZstdInputStream(fileInput);
                final var fileOut = new BufferedOutputStream(new FileOutputStream(destination.toFile()))
        ) {
            zstdInput.transferTo(fileOut);
            fileOut.flush();
        }
    }

    /**
     * Compress a {@link File} source, and store it in a {@link Path} destination.
     *
     * @param source      {@link File} to compress.
     * @param destination {@link Path} to destination.
     * @throws IOException This whole method could throw IOExceptions.
     */
    private void internalCompressFile(File source, Path destination) throws IOException {
        final var fileOutput = new BufferedOutputStream(new FileOutputStream(destination.toFile()));

        try (
                final var zstdOut = new ZstdOutputStream(fileOutput);
                final var fileInput = new BufferedInputStream(new FileInputStream(source))
        ) {
            zstdOut.setCloseFrameOnFlush(true);
            fileInput.transferTo(zstdOut);
        }
    }

    @Override
    public boolean decompressFile(File file, Path destination) {
        if (!file.exists()) {
            return false;
        }

        try {
            internalDecompressFile(file, destination);
            return true;
        } catch (IOException exception) {
            exception.printStackTrace();
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
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

}
