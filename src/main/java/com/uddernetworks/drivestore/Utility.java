package com.uddernetworks.drivestore;

public class Utility {

    public static double round(double number, int places) {
        double scale = Math.pow(10, places);
        return Math.round(number * scale) / scale;
    }

    public static String progressBar(String text, int width, double percent) {
        var line = text + " [";

        int filled = (int) Math.round(width * percent);

        line += "|".repeat(filled);
        line += " ".repeat(width - filled);
        line += "] " + ((int) (percent * 100)) + "%";
        return line;
    }
}
