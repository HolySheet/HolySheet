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
 *            "id": "abcdefghijklmnopqrstuvwxyz"
 *          }
 *      ]
 * }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>status</td><td><code>(PENDING\|UPLOADING\|COMPLETE)</code></td><td>The status of the upload. If complete, the </td></tr><tr><td>percentage</td><td>Double</td><td>The 0-1 percentage of the file upload. If pending, this value should be 0.</td></tr><tr><td>items</td><td>Item[]</td><td>A collection of files/items uploaded. This list is only populated if the status is <code>COMPLETE</code>. The Item object is outlined in the following 5 properties.</td></tr><tr><td>name</td><td>String</td><td>The name of the file</td></tr><tr><td>size</td><td>Long</td><td>The size of the file in bytes</td></tr><tr><td>sheets</td><td>Integer</td><td>The amount of sheets the file consists of</td></tr><tr><td>date</td><td>Long</td><td>The millisecond timestamp the file was created</td></tr><tr><td>id</td><td>String</td><td>The sheets-generated ID of the file</td></tr></tbody>
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
