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
 *            "id": "abcdefghijklmnopqrstuvwxyz"
 *          }
 *      ]
 * }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Example Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>items</td><td>Array of below items in an object</td><td>A collection of files/items retrieved.</td></tr><tr><td>name</td><td>String</td><td>The name of the file</td></tr><tr><td>size</td><td>Long</td><td>The size of the file in bytes</td></tr><tr><td>sheets</td><td>Integer</td><td>The amount of sheets the file consists of</td></tr><tr><td>date</td><td>Long</td><td>The millisecond timestamp the file was created</td></tr><tr><td>id</td><td>String</td><td>The sheets-generated ID of the file</td></tr></tbody>
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

    public static class ListItem {
        private String name;
        private long size;
        private int sheets;
        private long date;
        private String id;

        public ListItem(String name, long size, int sheets, long date, String id) {
            this.name = name;
            this.size = size;
            this.sheets = sheets;
            this.date = date;
            this.id = id;
        }

        @Override
        public String toString() {
            return "ListItem{" +
                    "name='" + name + '\'' +
                    ", size=" + size +
                    ", sheets=" + sheets +
                    ", date=" + date +
                    ", id='" + id + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ListResponse{" +
                "items=" + items +
                '}';
    }
}
