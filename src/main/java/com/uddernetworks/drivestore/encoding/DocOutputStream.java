package com.uddernetworks.drivestore.encoding;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DocOutputStream extends FilterOutputStream {

    private byte bits = 0;
    private long bitValue = 0;

    private List<DataChunk> chunks = new ArrayList<>();

    public DocOutputStream() {
        super(new ChunkOutputStream());
    }

    @Override
    public void write(int b) {
        bitValue |= (b & 255L) << bits; // Slap current 8 bytes (b) to the left of byteValue
        bits += 8; // b is 8 bytes, so add it to iterated
        if (bits >= 64) {
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
            chunks.add(DataChunk.constructChunk(bitValue));
        }
        super.flush();
    }

    public List<DataChunk> getChunks() {
        return chunks;
    }
}
