package com.uddernetworks.holysheet.compression;

import com.uddernetworks.grpc.HolysheetService;

import java.io.*;
import java.nio.file.Path;

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
     * Decompress a given {@link File} and store its decompressed contents in the given
     * {@link Path}.
     *
     * @param file        File to decompress.
     * @param destination Destination path to decompress this {@code file} to.
     * @return boolean indicating success.
     */
    boolean decompressFile(File file, Path destination);

    /**
     * Decompress a given {@link File} and store its decompressed contents in a temporary
     * file; this temporary file will be deleted when the program exits.
     *
     * @param file        File to decompress.
     * @return {@link Path} of temporary file, or null if unsuccessful operation.
     */
    Path decompressToTemp(File file);

    /**
     * Decompress a given {@link File} and overwrite its contents with the
     * decompressed data. If the process was not successful, then the original file is
     * not overwritten.
     *
     * @param file File to decompress.
     * @return boolean indicating success.
     */
    boolean decompressFile(File file);

    /**
     * Compress a given {@link File} and store its compressed contents in the given
     * {@link Path}.
     *
     * @param file        File to compress.
     * @param destination Destination path to compress this {@code file} to.
     * @return boolean indicating success.
     */
    boolean compressFile(File file, Path destination);

    /**
     * Compress a given {@link File} and store its compressed contents in a temporary
     * file; this temporary file will be deleted when the program exits.
     *
     * @param file        File to compress.
     * @return {@link Path} of temporary file, or null if unsuccessful operation.
     */
    Path compressToTemp(File file);

    /**
     * Compress a given {@link File} and overwrite its contents with the
     * compressed data. If the process was not successful, then the original file is
     * not overwritten.
     *
     * @param file File to compress.
     * @return boolean indicating success.
     */
    boolean compressFile(File file);

}
