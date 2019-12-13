package com.uddernetworks.drivestore.encoding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Long.toBinaryString;

public class DocOutputStream extends FilterOutputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocOutputStream.class);

    private static final int BITS = 64;
    private byte bits = 0;
    private long bitValue = 0;

    private List<DataChunk> chunks = new ArrayList<>();

    public DocOutputStream() {
        super(new ChunkOutputStream());
    }

    @Override
    public void write(int b) {
        bitValue <<= 8;
        bitValue |= b & 255L;
        bits += 8; // b is 8 bytes, so add it to iterated
        if (bits >= BITS) {
            chunks.add(DataChunk.constructChunk(bitValue));
            bits = 0;
            bitValue = 0;
        }
    }

    @Override
    public void write(byte[] data, int offset, int length) {
        for (int i = offset; i < length; ++i) {
            write(data[i]);
        }
    }

    @Override
    public void flush() throws IOException {
        if (bits > 0) {
            bitValue <<= 64 - bits;
            chunks.add(DataChunk.constructChunk(bitValue));
        }
        super.flush();
    }

    public List<DataChunk> getChunks() {
        return chunks;
    }
}
