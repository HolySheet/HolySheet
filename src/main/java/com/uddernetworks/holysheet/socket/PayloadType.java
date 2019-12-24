package com.uddernetworks.holysheet.socket;

import java.util.Arrays;
import java.util.Optional;

public enum PayloadType {
    ERROR(0, false),
    LIST_REQUEST(1, true),
    LIST_RESPONSE(2, false);

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
