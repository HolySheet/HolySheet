package com.uddernetworks.holysheet.socket.payload;

import com.uddernetworks.holysheet.socket.PayloadType;

/**
 * Json sent to a client wrapping {@link BasicPayload} with preset values in the event of an error with a message.
 * Example json:
 * <pre>
 *     {
 *         "code": 0,
 *         "type": 0,
 *         "message": "An error has occurred",
 *         "stacktrace": "...stacktrace..."
 *     }
 * </pre>
 *
 * <b>code</b>: Code of 0, indicating an error has occurred<br>
 * <b>type</b>: The {@link PayloadType#ERROR} type<br>
 * <b>message</b>: Displayable error message, if known<br>
 * <b>stacktrace</b>: Stacktrace of error
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
