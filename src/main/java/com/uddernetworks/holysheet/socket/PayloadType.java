package com.uddernetworks.holysheet.socket;

import java.util.Arrays;
import java.util.Optional;

public enum PayloadType {
    ERROR(0, false),
    LIST_REQUEST(1, true),
    LIST_RESPONSE(2, false),
    UPLOAD_REQUEST(3, true),
    UPLOAD_STATUS_RESPONSE(4, false),
    DOWNLOAD_REQUEST(5, true),
    DOWNLOAD_STATUS_RESPONSE(6, false),
    REMOVE_REQUEST(7, true),
    REMOVE_STATUS_RESPONSE(8, false),
    CODE_EXECUTION_REQUEST(9, true),
    CODE_EXECUTION_RESPONSE(10, false),
    CODE_EXECUTION_CALLBACK_RESPONSE(11, false);

    private int type;
    private boolean receivable;

    PayloadType(int type, boolean receivable) {
        this.type = type;
        this.receivable = receivable;
    }

    /**
     * Gets the serializable type of the payload.
     *
     * @return The int type
     */
    public int getType() {
        return type;
    }

    /**
     * Gets if the request cna be received from this (The Java) program. False indicates that nothing will occur when
     * the request is received.
     *
     * @return If the request is receivable.
     */
    public boolean isReceivable() {
        return receivable;
    }

    public static Optional<PayloadType> fromType(int type) {
        return Arrays.stream(values()).filter(payload -> payload.type == type).findFirst();
    }
}
