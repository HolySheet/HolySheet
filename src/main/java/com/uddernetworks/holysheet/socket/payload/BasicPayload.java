package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * A superclass for all json requests and responses. Example json is:
 * <pre>
 *     {
 *         "code": 1,
 *         "type": 1,
 *         "message": "Success",
 *         "state": "0317d1f0-6053-4cce-89ba-9e896784820a"
 *     }
 * </pre>
 *
 * <b>code</b>: The response code of the payload, 1 being successful, <1 unsuccessful.<br>
 * <b>type</b>: The type of the response for non-dynamic languages like this one. Derived from the
 *      {@link com.uddernetworks.holysheet.socket.PayloadType} enum.<br>
 * <b>message</b>: Any extra details of the request/response, used for things like errors. The state of the request should not<br>
 *      depend on this text.
 * <b>state</b>: A UUID state generated for a request, and reused for the requests's
 *     response, weather it be a proper response or error. This is to ensure the correct pairing of otherwise unordered requests and responses
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
