package io.papermc.typewriter.utils;

public final class Formatting {

    public static int countOccurrences(String value, char match) {
        int count = 0;
        for (int i = 0, len = value.length(); i < len; i++) {
            if (value.charAt(i) == match) {
                count++;
            }
        }
        return count;
    }

    public static String quoted(String value) {
        return "\"" + value + "\"";
    }

    private Formatting() {
    }
}
