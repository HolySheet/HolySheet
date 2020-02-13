package com.uddernetworks.holysheet.utility;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Utility {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utility.class);

    public static double round(double number, int places) {
        double scale = Math.pow(10, places);
        return Math.round(number * scale) / scale;
    }

    public static int tryParse(String number) {
        return tryParse(number, -1);
    }

    public static int tryParse(String number, int def) {
        if (!StringUtils.isNumeric(number)) {
            return def;
        }

        return Integer.parseInt(number);
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
     * @param list A list
     * @param <T>  The type
     * @return Either an empty optional if the collection is empty, or an optional of the first element
     */
    public static <T> Optional<T> getCollectionFirst(List<T> list) {
        if (list.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(list.get(0));
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

    /**
     * Returns a string reader of google-issues JSON credentials.
     *
     * @param creds A .json file path, environment variable, or JSON itself
     * @return A reader of the credentials
     * @throws FileNotFoundException If the file path is not found
     */
    public static Reader credentialsReader(String creds) throws FileNotFoundException {
        if (creds.contains("{")) {
            LOGGER.info("Using credentials from direct JSON");
            return new StringReader(creds);
        }

        if (!creds.contains(".json")) {
            LOGGER.info("Using credentials from environment variable \"{}\"", creds);
            return new StringReader(System.getenv(creds));
        }

        var file = new File(creds);
        LOGGER.info("Using credentials from file \"{}\"", file.getAbsolutePath());
        if (!file.exists()) {
            throw new FileNotFoundException("Couldn't find credentials file " + creds);
        }

        return new FileReader(file);
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
