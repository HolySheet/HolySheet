package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * <pre>Client --> Server</pre>
 * A request to download the given remote file from Sheets to a destination.
 * <pre>
 * {
 *     "id": "1KLruEf0d8GJgf7JGaYUiNnW_Pe0Zumvq",
 *     "path": "E:\\file.mp4"
 * }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>id</td><td>String</td><td>The Sheets-generated ID of the file to download</td></tr><tr><td>path</td><td>String</td><td>The file path to save the file to</td></tr></tbody>
 * </table>
 *
 * @see PayloadType#DOWNLOAD_REQUEST
 */
public class DownloadRequest extends BasicPayload {

    private String id;
    private String path;

    public DownloadRequest(int code, String message, String state, String id, String path) {
        super(code, PayloadType.DOWNLOAD_REQUEST, message, state);
        this.id = id;
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "DownloadRequest{" +
                "id='" + id + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
