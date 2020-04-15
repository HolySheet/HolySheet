package com.uddernetworks.holysheet.compression;

import com.uddernetworks.grpc.HolysheetService.UploadRequest.Compression;
import com.uddernetworks.holysheet.compression.implementations.TarballGZipCompression;
import com.uddernetworks.holysheet.compression.implementations.ZstdCompression;
import com.uddernetworks.holysheet.utility.Utility;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Chris L R.
 * @version Mar 01, 2020
 */
public class CompressionFactory {

    /**
     * Parse the compression property to a {@link Compression} enumeration.
     * This can be done by its id, e.g. ZSTD = 2, ZIP = 1, NONE = 0, etc etc.
     * Or it can be done by its name, or pretty name.
     *
     * @param compression Compression format as a string
     * @return Compression represented; if an invalid string is passed then NONE is returned.
     */
    public static Compression parseCompression(String compression) {
        if (compression == null)
            return Compression.NONE;

        Compression comp;
        if (StringUtils.isNumeric(compression)) {
            var num = Utility.tryParse(compression, 0);
            comp = Compression.forNumber(Math.max(num, 0));
        } else {
            compression = compression.toUpperCase().replace("-", "_"); // e.g. tarball-gzip => TARBALL_GZIP
            comp = Compression.valueOf(compression.toUpperCase());
        }

        return comp != null ? comp : Compression.NONE;
    }

    public static CompressionAlgorithm getAlgorithm(Compression compression) {
        switch(compression) {
            case ZSTD:
                return new ZstdCompression();
            case TARBALL_GZIP:
                return new TarballGZipCompression();
            default:
                return null;
        }
    }

    /**
     * Translate a {@link Compression} enum to a pretty name, that
     * can be displayed to the terminal. All underscores go to dashes,
     * and it's in all lower case.
     *
     * E.g. TARBALL_GZIP => tarball-gzip
     *
     * @param compression {@link Compression} enum.
     * @return {@link String} pretty name, never null.
     */
    public static String prettyName(Compression compression) {
        return compression.name().toLowerCase().replace("_", "-");
    }

}
