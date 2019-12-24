package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * <pre>Client --> Server</pre>
 * Json sent to the Java server to request the listing of files, with an
 * optional search query.
 * <pre>
 *     {
 *         "query": "Query"
 *     }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>query</td><td>String</td><td>A string payload to search files to list. This can be null.</td></tr></tbody>
 * </table>
 *
 * @see PayloadType#LIST_REQUEST
 */
public class ListRequest extends BasicPayload {

    private String query;

    public ListRequest(int code, String message, String state, String query) {
        super(code, PayloadType.LIST_REQUEST, message, state);
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "ListRequest{" +
                "query='" + query + '\'' +
                '}';
    }
}
