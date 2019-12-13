package com.uddernetworks.drivestore.utils;

import java.io.IOException;

public class Suppressed {

    @FunctionalInterface
    public interface SuppressedFunction<T, R> {
        R apply(T t) throws IOException;
    }

}
