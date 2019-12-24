package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * <pre>Client --> Server</pre>
 * A request to remove the given remote Sheets file.
 * <pre>
 *      {
 *          "id": "1KLruEf0d8GJgf7JGaYUiNnW_Pe0Zumvq"
 *      }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>id</td><td>String</td><td>The Sheets-generated ID of the file to remove</td></tr></tbody>
 * </table>
 *
 * @see PayloadType#REMOVE_REQUEST
 */
public class RemoveRequest extends BasicPayload {

    private String id;

    public RemoveRequest(int code, String message, String state, String id) {
        super(code, PayloadType.REMOVE_REQUEST, message, state);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "RemoveRequest{" +
                "id='" + id + '\'' +
                '}';
    }
}
