package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

import java.util.List;

/**
 * <pre>Client <-- Server</pre>
 * A status update saying how far along an upload is.
 * <pre>
 * {
 *     "status": "UPLOADING",
 *     "percentage": 0.856,
 *     "items": [
 *          {
 *            "name": "test.txt",
 *            "size": 54321,
 *            "sheets": 6,
 *            "date": 1577200502088,
 *            "id": "abcdefghijklmnopqrstuvwxyz",
 *            "selfOwned": false,
 *            "owner": "Some Owner",
 *            "driveLink": "https://drive.google.com/..."
 *          }
 *      ]
 * }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>status</td><td><code>(PENDING\|DOWNLOADING\|COMPLETE)</code></td><td>The status of the download</td></tr><tr><td>percentage</td><td>Double</td><td>The 0-1 percentage of the file download. If pending, this value should be 0.</td></tr></tbody>
 * </table>
 *
 * @see PayloadType#UPLOAD_STATUS_RESPONSE
 */
public class UploadStatusResponse extends BasicPayload {

    private String status;
    private double percentage;
    private List<ListItem> items;

    public UploadStatusResponse(int code, String message, String state, String status, double percentage, List<ListItem> items) {
        super(code, PayloadType.UPLOAD_STATUS_RESPONSE, message, state);
        this.status = status;
        this.percentage = percentage;
        this.items = items;
    }

    public String getStatus() {
        return status;
    }

    public double getPercentage() {
        return percentage;
    }

    public List<ListItem> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "UploadStatusResponse{" +
                "status='" + status + '\'' +
                ", percentage=" + percentage +
                ", items=" + items +
                '}';
    }
}
