package com.codicesoftware.plugins.hudson.util;

import hudson.Util;

public class StringUtil {

    private StringUtil() { }

    public static int tryParse(String value) {
        return tryParse(value, 0);
    }

    public static int tryParse(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String singleLine(String value) {
        return Util.fixNull(value).replaceAll("[\\n\\r\\t]", " ").trim();
    }

    public static String removeNewLines(String value) {
        return Util.fixNull(value).replaceAll("[\\n\\r]", "").trim();
    }

}
