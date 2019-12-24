package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * <pre>Client <-- Server</pre>
 * Json sent to a client wrapping {@link BasicPayload} with preset values in the event of an error with a message.
 * <pre>
 *     {
 *         "code": 0,
 *         "type": 0,
 *         "message": "An error has occurred",
 *         "stacktrace": "...stacktrace..."
 *     }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>code</td><td><code>0</code></td><td>Code of 0, indicating an error has occurred</td></tr><tr><td>type</td><td><code>0</code></td><td>The <a href='https://github.com/RubbaBoy/HolySheet/blob/master/src/main/java/com/uddernetworks/holysheet/socket/PayloadType.java#L7'>PayloadType#ERROR</a> type</td></tr><tr><td>message</td><td>String</td><td>Displayable error message, if known</td></tr><tr><td>stacktrace</td><td>String</td><td>Stacktrace of error</td></tr></tbody>
 * </table>
 *
 * @see PayloadType#ERROR
 */
public class ErrorPayload extends BasicPayload {

    private String stacktrace;

    public ErrorPayload(String message, String state, String stacktrace) {
        super(0, PayloadType.ERROR, message, state);
        this.stacktrace = stacktrace;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    @Override
    public String toString() {
        return "ListRequest{" +
                "stacktrace='" + stacktrace + '\'' +
                '}';
    }
}
