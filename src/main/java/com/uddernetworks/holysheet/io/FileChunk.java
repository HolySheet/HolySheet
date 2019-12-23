package com.uddernetworks.holysheet.io;

import com.google.api.services.drive.model.File;

import java.util.Map;

public class FileChunk {

    private final File parent;
    private final byte[] bytes;
    private final int index;

    public FileChunk(File parent, byte[] bytes, int index) {
        this.parent = parent;
        this.bytes = bytes;
        this.index = index;
    }

    public Map<String, String> getProperties() {
        return Map.of(
                "index", String.valueOf(index),
                "size", String.valueOf(bytes.length)
        );
    }

    public File getParent() {
        return parent;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getIndex() {
        return index;
    }
}
