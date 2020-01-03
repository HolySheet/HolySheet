package com.uddernetworks.holysheet.utility;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Collection;
import java.util.Optional;

public class Utility {

    public static double round(double number, int places) {
        double scale = Math.pow(10, places);
        return Math.round(number * scale) / scale;
    }

    public static String progressBar(String text, int width, double percent) {
        return progressBar(text, "", width, percent);
    }

    public static String progressBar(String text, String beforePercent, int width, double percent) {
        if (!beforePercent.isBlank() && !beforePercent.startsWith(" ")) beforePercent = " " + beforePercent;
        var line = text + " [";

        percent = Math.min(1, percent);
        int filled = (int) Math.round(width * percent);

        line += "|".repeat(filled);
        line += " ".repeat(width - filled);
        line += "]" + beforePercent + " " + ((int) (percent * 100)) + "%";
        return line;
    }

    public static String getStackTrace() {
        return ExceptionUtils.getStackTrace(new RuntimeException()).replace("\r\n", "\n");
    }

    public static String getStackTrace(Throwable throwable) {
        return ExceptionUtils.getStackTrace(throwable).replace("\r\n", "\n");
    }

    /**
     * Gets a Collection Optional; a safe optional of the first element of a given collection.
     *
     * @param collection A collection
     * @param <T> The type
     * @return Either an empty optional if the collection is empty, or an optional of the first element
     */
    public static <T> Optional<T> getCollectionFirst(Collection<T> collection) {
        for (var t : collection) {
            return Optional.of(t);
        }

        return Optional.empty();
    }

    /**
     * Gets a human readable form of the given bytes, e.g. 1000 to 1 KB.
     * Source by aioobe on StackOverflow
     *
     * @param bytes The amount of bytes
     * @return The human-readable simplified form
     * @see <a href="https://stackoverflow.com/a/3758880/3929546">How to convert byte size into human readable format in Java? - aioobe</a>
     */
    public static String humanReadableByteCountSI(long bytes) {
        String s = bytes < 0 ? "-" : "";
        long b = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        return b < 1000L ? bytes + " B"
                : b < 999_950L ? String.format("%s%.1f kB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f MB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f GB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f TB", s, b / 1e3)
                : (b /= 1000) < 999_950L ? String.format("%s%.1f PB", s, b / 1e3)
                : String.format("%s%.1f EB", s, b / 1e6);
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
