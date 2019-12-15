package com.uddernetworks.drivestore.encoding;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.uddernetworks.drivestore.encoding.DecodingOutputStream.BASE;
import static com.uddernetworks.drivestore.encoding.DecodingOutputStream.ENCODING_TABLE;

/**
 * An OutputStream (Backed by a BufferArrayOutputStream) that encodes the written data to a slightly altered Base91,
 * adding a newline every 0x7FFF bytes (Due to Google Sheets' restrictions)
 *
 * Base91 encoding comes primarily from bwaldvogel
 * @see <a href="http://github.com/bwaldvogel/base91">bwaldvogel/base91</a>
 */
public class EncodingOutputStream extends FilterOutputStream {

//    public static final int CELL_WIDTH = 0x7FFF; // Half of 0xFFFF
    public static final int CELL_WIDTH = 5; // Half of 0xFFFF

    private final int maxLines;

    private int length = 0;
    private int index = 0;

    private int ebq = 0;
    private int en = 0;

    private int currLine = 0;
    private ByteArrayOutputStream buffer;
    private List<byte[]> chunks = new ArrayList<>();

    public EncodingOutputStream(int maxLines) {
        super(new ByteArrayOutputStream());
        this.maxLines = maxLines;
        buffer = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) {
        if (++index % CELL_WIDTH == 0) {
            if (b != '=' && b != '\'') {
                index = 0;
                currLine++;

                if (currLine >= maxLines) {
                    currLine = 0;
                    chunks.add(buffer.toByteArray());
//                    chunks.add(buffer.array());
//                    buffer.clear();
                    buffer.reset();
                } else {
//                    buffer.put((byte) '\n');
                    buffer.write('\n');
                    length++;
                }
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
//            out.write(ENCODING_TABLE[ev % BASE]);
//            out.write(ENCODING_TABLE[ev / BASE]);

            buffer.write(ENCODING_TABLE[ev % BASE]);
            buffer.write(ENCODING_TABLE[ev / BASE]);
            length += 2;
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
        if (index > 0) {
            chunks.add(buffer.toByteArray());
        }
        super.flush();
    }

    public int getLength() {
        return length;
    }

    public List<byte[]> getChunks() {
        return chunks;
    }

    public static EncodingOutputStream encode(byte[] data, int maxLines) throws IOException {
        var encodingOut = new EncodingOutputStream(maxLines);
        encodingOut.write(data);
        encodingOut.flush();
        return encodingOut;
    }
}
