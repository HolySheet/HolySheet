package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * A superclass for all json requests and responses. Example json is:
 * <pre>
 *     {
 *         "code": 1,
 *         "type": 1,
 *         "message": "Success"
 *     }
 * </pre>
 *
 * <b>code</b>: The response code of the payload, 1 being successful, <1 unsuccessful.<br>
 * <b>type</b>: The type of the response for non-dynamic languages like this one. Derived from the
 *      {@link com.uddernetworks.holysheet.socket.PayloadType} enum.<br>
 * <b>message</b>: Any extra details of the request/response, used for things like errors. The state of the request should not<br>
 *      depend on this text.
 */
public class BasicPayload {

    private int code;
    private PayloadType type;
    private String message;

    public BasicPayload(int code, PayloadType type, String message) {
        this.code = code;
        this.type = type;
        this.message = message;
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

    @Override
    public String toString() {
        return "BasicPayload{" +
                "code=" + code +
                ", type=" + type +
                ", message='" + message + '\'' +
                '}';
    }
}
