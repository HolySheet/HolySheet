package com.uddernetworks.drivestore.encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.uddernetworks.drivestore.encoding.DecodingOutputStream.BASE;
import static com.uddernetworks.drivestore.encoding.DecodingOutputStream.ENCODING_TABLE;

/**
 * An OutputStream (Backed by a BufferArrayOutputStream) that encodes the written data to a slightly altered Base91,
 * adding a newline every 0x7FFF bytes (Due to Google Sheets' restrictions)
 *
 * Base91 encoding comes primarily from bwaldvogel
 * @see <a href="http://github.com/bwaldvogel/base91">bwaldvogel/base91</a>
 */
public class EncodingOutputStream extends ByteArrayFilteredOutputStream<ByteArrayOutputStream> {

    public static final int CELL_WIDTH = 0x7FFF; // Half of 0xFFFF

    private int index = 0;

    private int ebq = 0;
    private int en = 0;

    @Override
    public void write(int b) throws IOException {
        if (++index % CELL_WIDTH == 0) {
            if (b != '=' && b != '\'') {
                index = 0;
                out.write('\n');
            } else {
                index--;
            }
        }

        ebq |= (b & 255) << en;
        en += 8;
        if (en > 13) {
            int ev = ebq & 8191;

            if (ev > 88) {
                ebq >>= 13;
                en -= 13;
            } else {
                ev = ebq & 16383;
                ebq >>= 14;
                en -= 14;
            }
            out.write(ENCODING_TABLE[ev % BASE]);
            out.write(ENCODING_TABLE[ev / BASE]);
        }
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        for (int i = offset; i < length; ++i) {
            write(data[i]);
        }
    }

    public static byte[] encode(byte[] data) throws IOException {
        var encodingOut = new EncodingOutputStream();
        encodingOut.write(data);
        encodingOut.flush();
        return encodingOut.getOut().toByteArray();
    }
}
