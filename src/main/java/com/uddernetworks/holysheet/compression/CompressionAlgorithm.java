package com.uddernetworks.holysheet.compression;

import com.uddernetworks.grpc.HolysheetService;

import java.io.*;

/**
 * @author Chris L R.
 * @version Mar 15, 2020
 */
public interface CompressionAlgorithm {

    /**
     * Return the compression type, an enumeration defined in the holysheet_service.proto file.
     *
     * @return {@link com.uddernetworks.grpc.HolysheetService.UploadRequest.Compression} type.
     */
    HolysheetService.UploadRequest.Compression getCompressionType();

    /**
     * Decompress a {@link File}, saving it in the same location, and return the length of the new file,
     * in bytes.
     *
     * @param file {@link File} to decompress and save to.
     * @return {@code long} amount of bytes.
     * @throws IOException From writing and decompressing.
     */
    long decompressFile(File file) throws IOException;

    /**
     * Create an {@link InputStream} that decompresses all data that is read from another {@link InputStream}.
     * Wrapping the other one.
     *
     * @param inputStream {@link InputStream} to decompress & wrap.
     * @return {@link InputStream} that decompresses all data read from it.
     * @throws IOException From decompressing.
     */
    InputStream decompressStream(InputStream inputStream) throws IOException;

}
