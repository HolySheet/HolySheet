package com.uddernetworks.holysheet.encoding;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * An OutputStream (Backed by a BufferArrayOutputStream) that decodes the written data from a slightly altered Base91,
 * ignoring newline/carriage returns.
 *
 * Base91 decoding comes primarily from bwaldvogel
 * @see <a href="http://github.com/bwaldvogel/base91">bwaldvogel/base91</a>
 */
public class DecodingOutputStream<T extends OutputStream> extends ByteArrayFilteredOutputStream<T> {

    public static final byte[] ENCODING_TABLE;
    public static final byte[] DECODING_TABLE;
    public static final int BASE;

    static {
        // Diverted from Base91 spec:  . to -  and  , to '
        String ts = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&()*+'-/:;<=>?@[]^_`{|}~\"";
        ENCODING_TABLE = ts.getBytes(StandardCharsets.ISO_8859_1);
        BASE = ENCODING_TABLE.length;

        DECODING_TABLE = new byte[256];
        for (int i = 0; i < 256; ++i) {
            DECODING_TABLE[i] = -1;
        }

        for (int i = 0; i < BASE; ++i) {
            DECODING_TABLE[ENCODING_TABLE[i]] = (byte) i;
        }
    }

    private int dbq = 0;
    private int dn = 0;
    private int dv = -1;

    public DecodingOutputStream() {
        super();
    }

    public DecodingOutputStream(T out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        if (b == 10 || b == 13) return;

        if (dv == -1) {
            dv = DECODING_TABLE[b];
        } else {
            dv += DECODING_TABLE[b] * BASE;
            dbq |= dv << dn;
            dn += (dv & 8191) > 88 ? 13 : 14;
            do {
                out.write((byte) dbq);
                dbq >>= 8;
                dn -= 8;
            } while (dn > 7);
            dv = -1;
        }
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        for (int i = offset; i < length; ++i) {
            write(data[i]);
        }
    }

    @Override
    public void flush() throws IOException {
        if (dv != -1) {
            out.write((byte) (dbq | dv << dn));
        }

        super.flush();
    }

    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }
}
