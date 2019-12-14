package com.uddernetworks.drivestore.encoding;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DecodingOutputStream extends FilterOutputStream {

    public DecodingOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        if (b == 10 || b == 13) return;
        out.write(b);
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        for (int i = offset; i < length; ++i) {
            write(data[i]);
        }
    }
}
