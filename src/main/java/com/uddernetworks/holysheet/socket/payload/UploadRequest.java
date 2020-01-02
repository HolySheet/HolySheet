package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * <pre>Client --> Server</pre>
 * A request to upload a given file.
 * <pre>
 * {
 *     "file": "file:///c:/file.txt",
 *     "upload": "multipart",
 *     "compression": "zip",
 *     "sheetSize": "10000000" // 10MB
 * }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>file</td><td>URL</td><td>The URL of the file to upload</td></tr><tr><td>id</td><td>String</td><td>The Google Drive ID of the file to download and upload</td></tr><tr><td>upload</td><td><code>(multipart\|direct)</code></td><td>Toggles multipart or direct uploading (Multipart recommended)</td></tr><tr><td>compression</td><td><code>(none\|zip)</code></td><td>The compression algorithm to use, if any.</td></tr><tr><td>sheetSize</td><td>Integer</td><td>The max size (in bytes) each sheet can be. There is no max to this documented by Google, however in private testing RubbaBoy was able to achieve up to 25.9MB without 500&#39;s, however is often limited for long time periods down to 10MB.</td></tr></tbody>
 * </table>
 *
 * @see PayloadType#UPLOAD_REQUEST
 */
public class UploadRequest extends BasicPayload {

    private String file;
    private String id;
    private String upload;
    private String compression;
    private int sheetSize;

    public UploadRequest(int code, String message, String state) {
        super(code, PayloadType.UPLOAD_REQUEST, message, state);
    }

    public String getFile() {
        return file;
    }

    public String getId() {
        return id;
    }

    public String getUpload() {
        return upload;
    }

    public String getCompression() {
        return compression;
    }

    public int getSheetSize() {
        return sheetSize;
    }

    @Override
    public String toString() {
        return "UploadRequest{" +
                "file='" + file + '\'' +
                ", id='" + id + '\'' +
                ", upload='" + upload + '\'' +
                ", compression='" + compression + '\'' +
                ", sheetSize=" + sheetSize +
                '}';
    }
}
