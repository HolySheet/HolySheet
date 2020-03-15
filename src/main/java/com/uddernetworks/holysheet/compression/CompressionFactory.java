package com.uddernetworks.holysheet.compression;

import com.uddernetworks.grpc.HolysheetService.UploadRequest.Compression;
import com.uddernetworks.holysheet.compression.implementations.ZstdCompression;

/**
 * @author Chris L R.
 * @version Mar 01, 2020
 */
public class CompressionFactory {

    public static CompressionAlgorithm getAlgorithm(Compression compression) {
        switch(compression) {
            case ZSTD:
                return new ZstdCompression();
            default:
                return null;
        }
    }

}
