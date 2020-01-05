package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

import java.util.List;

/**
 * <pre>Client <-- Server</pre>
 * Json sent to a client with a list of files.
 * <pre>
 * {
 *     "items": [
 *          {
 *            "name": "test.txt",
 *            "size": 42069,
 *            "sheets": 6,
 *            "date": 123456789,
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
 * <tr><th>Key</th><th>Example Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>items</td><td>Item[]</td><td>A collection of files/items retrieved. The Item object is outlined in the following 5 properties.</td></tr><tr><td>name</td><td>String</td><td>The name of the file</td></tr><tr><td>size</td><td>Long</td><td>The size of the file in bytes</td></tr><tr><td>sheets</td><td>Integer</td><td>The amount of sheets the file consists of</td></tr><tr><td>date</td><td>Long</td><td>The millisecond timestamp the file was created</td></tr><tr><td>id</td><td>String</td><td>The sheets-generated ID of the file</td></tr><tr><td>selfOwned</td><td>Boolean</td><td>If the logged-in user owns the file</td></tr><tr><td>owner</td><td>String</td><td>The name of the user who owns the file</td></tr><tr><td>driveLink</td><td>String</td><td>The webViewLink to view the file in Google Drive</td></tr></tbody>
 * </table>
 *
 * @see PayloadType#LIST_RESPONSE
 */
public class ListResponse extends BasicPayload {

    private List<ListItem> items;

    public ListResponse(int code, String message, String state, List<ListItem> items) {
        super(code, PayloadType.LIST_RESPONSE, message, state);
        this.items = items;
    }

    public List<ListItem> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "ListResponse{" +
                "items=" + items +
                '}';
    }
}
