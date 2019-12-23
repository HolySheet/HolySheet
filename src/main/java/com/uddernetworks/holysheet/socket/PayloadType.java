package com.uddernetworks.holysheet.socket;

import java.util.Arrays;
import java.util.Optional;

public enum PayloadType {
    LIST(1);

    private int type;

    PayloadType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static Optional<PayloadType> fromType(int type) {
        return Arrays.stream(values()).filter(payload -> payload.type == type).findFirst();
    }
}
