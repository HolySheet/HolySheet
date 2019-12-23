package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

import java.util.List;

/**
 * Json sent to a client with a list of files. Example json (Excluding {@link BasicPayload}) is:
 * <pre>
 *     {
 *         "items": [
 *              {
 *                "name": "test.txt",
 *                "size": 42069,
 *                "sheets": 6,
 *                "date": 123456789,
 *                "id": "abcdefghijklmnopqrstuvwxyz"
 *              }
 *          ]
 *     }
 * </pre>
 *
 * <b>items</b>: A collection of files/items retrieved.<br><br>
 * The following descriptions are in the objects <b>items</b> contains.<br>
 * <b>name</b>: The name of the file<br>
 * <b>size</b>: The size of the file in bytes<br>
 * <b>sheets</b>: The amount of sheets the file consists of<br>
 * <b>date</b>: The millisecond timestamp the file was created<br>
 * <b>id</b>: The ID of the file<br>
 */
public class ListResponse extends BasicPayload {

    private List<ListItem> items;

    public ListResponse(int code, String message) {
        super(code, PayloadType.LIST, message);
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
