package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * <pre>Client <-- Server</pre>
 * A status update saying how far along a download is.
 * <pre>
 *      {
 *          "status": "DOWNLOADING",
 *          "percentage": "0.856"
 *      }
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
public class DownloadStatusResponse extends BasicPayload {

    private String status;
    private double percentage;

    public DownloadStatusResponse(int code, String message, String state, String status, double percentage) {
        super(code, PayloadType.DOWNLOAD_STATUS_RESPONSE, message, state);
        this.status = status;
        this.percentage = percentage;
    }

    public String getStatus() {
        return status;
    }

    public double getPercentage() {
        return percentage;
    }

    @Override
    public String toString() {
        return "DownloadStatusResponse{" +
                "status='" + status + '\'' +
                ", percentage=" + percentage +
                '}';
    }
}
