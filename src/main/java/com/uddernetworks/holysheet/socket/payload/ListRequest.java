package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * Json sent to the Java server to request the listing of files, with an
 * optional search query. Example json (Excluding {@link BasicPayload}) is:
 * <pre>
 *     {
 *         "query": "Query"
 *     }
 * </pre>
 *
 * <b>query</b>: A string payload to search files to list. This can be null.
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
