package com.uddernetworks.drivestore;

import com.google.api.services.drive.model.File;

public class DownloadedSheet {
    private File file;
    private byte[] bytes;

    public DownloadedSheet(File file, byte[] bytes) {
        this.file = file;
        this.bytes = bytes;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
