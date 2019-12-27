package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * <pre>Client <--> Server</pre>
 * A superclass for all json requests and responses. Example json is:
 * <pre>
 * {
 *     "code": 1,
 *     "type": 1,
 *     "message": "Success",
 *     "state": "0317d1f0-6053-4cce-89ba-9e896784820a"
 * }
 * </pre>
 *
 * <table>
 * <thead>
 * <tr><th>Key</th><th>Value</th><th>Description</th></tr></thead>
 * <tbody><tr><td>code</td><td>Integer</td><td>The response code of the payload, 1 being successful, &lt;1 unsuccessful.</td></tr><tr><td>type</td><td>Integer</td><td>The type of the response for non-dynamic languages like this one. Derived from the <a href='https://github.com/RubbaBoy/HolySheet/blob/master/src/main/java/com/uddernetworks/holysheet/socket/PayloadType.java'>PayloadType</a> enum.</td></tr><tr><td>message</td><td>String</td><td>Any extra details of the request/response, used for things like errors. The state of the request should not depend on this text.</td></tr><tr><td>state</td><td>Untrimmed UUID</td><td>A UUID state generated for a request, and reused for the request&#39;s response, weather it be a proper response or error. This is to ensure the correct pairing of otherwise unordered requests and responses.</td></tr></tbody>
 * </table>
 */
public class BasicPayload {

    private int code;
    private PayloadType type;
    private String message;
    private String state;

    public BasicPayload(int code, PayloadType type, String message, String state) {
        this.code = code;
        this.type = type;
        this.message = message;
        this.state = state;
    }

    public int getCode() {
        return code;
    }

    public PayloadType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getState() {
        return state;
    }

    @Override
    public String toString() {
        return "BasicPayload{" +
                "code=" + code +
                ", type=" + type +
                ", message='" + message + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
