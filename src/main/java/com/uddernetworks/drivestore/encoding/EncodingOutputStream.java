package com.uddernetworks.drivestore.encoding;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class EncodingOutputStream extends FilterOutputStream {

    public static final int CELL_WIDTH = 0x7FFF; // Half of 0xFFFF

    private int index = 0;

    public EncodingOutputStream(OutputStream out) {
        super(out);
    }

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

        out.write(b);
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        for (int i = offset; i < length; ++i) {
            write(data[i]);
        }
    }
}
