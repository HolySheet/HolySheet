package com.uddernetworks.holysheet.encoding;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;

import static com.uddernetworks.holysheet.encoding.DecodingOutputStream.BASE;
import static com.uddernetworks.holysheet.encoding.DecodingOutputStream.ENCODING_TABLE;

/**
 * An OutputStream (Backed by a BufferArrayOutputStream) that encodes the written data to a slightly altered Base91,
 * adding a newline every 0x7FFF bytes (Due to Google Sheets' restrictions)
 *
 * Base91 encoding comes primarily from bwaldvogel
 * @see <a href="http://github.com/bwaldvogel/base91">bwaldvogel/base91</a>
 */
public class EncodingOutputStream extends FilterOutputStream {

    public static final int CELL_WIDTH = 0x3FFF; // Half of 0x7FFF
//    public static final int CELL_WIDTH = 0x7FFF; // Half of 0xFFFF
//    public static final int CELL_WIDTH = 5; // Half of 0xFFFF

    private final long maxLength;
    private int chunkIndex = 0;

    private BiConsumer<Integer, byte[]> chunkConsumer;
    private Runnable onClose;

    private long length = 0;
    private int bufferLength = 0;
    private int lineIndex = 0;

    private int ebq = 0;
    private int en = 0;

    private ByteArrayOutputStream buffer;

    public EncodingOutputStream(long maxLength) {
        this(maxLength, null);
    }

    public EncodingOutputStream(long maxLength, BiConsumer<Integer, byte[]> chunkConsumer) {
        super(new ByteArrayOutputStream());
        this.maxLength = maxLength;
        this.chunkConsumer = chunkConsumer;
        this.buffer = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) {
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

            var first = ENCODING_TABLE[ev % BASE];
            var second = ENCODING_TABLE[ev / BASE];

            if (++lineIndex % CELL_WIDTH == 0) {
                if (first != '=' && first != '\'') {
                    lineIndex = 0;

                    if (bufferLength >= maxLength) {
                        bufferLength = 0;
                        chunkConsumer.accept(chunkIndex++, buffer.toByteArray());
                        buffer.reset();
                    } else {
                        buffer.write('\n');
                        length++;
                        bufferLength++;
                    }
                } else {
                    lineIndex--;
                }
            }

            writeBuffer(first);
            writeBuffer(second);
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
        if (en > 0) {
            writeBuffer(ENCODING_TABLE[ebq % BASE]);
            if (en > 7 || ebq > 90) {
                writeBuffer(ENCODING_TABLE[ebq / BASE]);
            }
        }

        if (en > 0 || lineIndex > 0) {
            chunkConsumer.accept(chunkIndex++, buffer.toByteArray());
        }

        super.flush();

        if (onClose != null) {
            onClose.run();
        }
    }

    private void writeBuffer(byte b) {
        if (b == '"') {
            writeBuffer((byte) '\\');
        }

        buffer.write(b);
        length++;
        bufferLength++;
    }

    public void setChunkConsumer(BiConsumer<Integer, byte[]> chunkConsumer) {
        this.chunkConsumer = chunkConsumer;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public long getLength() {
        return length;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getBufferLength() {
        return bufferLength;
    }

    public static EncodingOutputStream encode(InputStream inputStream, long maxLength, BiConsumer<Integer, byte[]> chunkConsumer) throws IOException {
        var encodingOut = new EncodingOutputStream(maxLength, chunkConsumer);
        IOUtils.copy(inputStream, encodingOut);
        encodingOut.flush();
        return encodingOut;
    }
}
