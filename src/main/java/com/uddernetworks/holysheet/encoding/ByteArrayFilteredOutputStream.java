package com.uddernetworks.holysheet.encoding;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;

public abstract class ByteArrayFilteredOutputStream<T extends OutputStream> extends FilterOutputStream {
    public ByteArrayFilteredOutputStream() {
        super(new ByteArrayOutputStream());
    }

    public ByteArrayFilteredOutputStream(T out) {
        super(out);
    }

    public T getOut() {
        return (T) out;
    }
}
