package com.hiresphere.util;

/**
 * ANSI Color and formatting constants for enhanced CLI presentation.
 */
public class ConsoleColors {
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String UNDERLINE = "\u001B[4m";

    // Text Colors
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bright Colors
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_RED = "\u001B[91m";

    public static String green(String text) {
        return GREEN + text + RESET;
    }

    public static String cyan(String text) {
        return CYAN + text + RESET;
    }

    public static String yellow(String text) {
        return YELLOW + text + RESET;
    }

    public static String red(String text) {
        return RED + text + RESET;
    }

    public static String bold(String text) {
        return BOLD + text + RESET;
    }

    public static String scoreBadge(double score) {
        if (score >= 80.0) {
            return BRIGHT_GREEN + BOLD + String.format("%5.1f%% [TIER 1]", score) + RESET;
        } else if (score >= 60.0) {
            return BRIGHT_YELLOW + BOLD + String.format("%5.1f%% [TIER 2]", score) + RESET;
        } else {
            return BRIGHT_RED + BOLD + String.format("%5.1f%% [TIER 3]", score) + RESET;
        }
    }

    public static String statusBadge(String status) {
        if (status == null) return "-";
        switch (status.toUpperCase()) {
            case "SHORTLISTED":
            case "HIRED":
                return BRIGHT_GREEN + BOLD + "[" + status + "]" + RESET;
            case "SCREENED":
            case "APPLIED":
                return BRIGHT_CYAN + "[" + status + "]" + RESET;
            case "REJECTED":
                return BRIGHT_RED + "[" + status + "]" + RESET;
            case "OPEN":
                return GREEN + "[OPEN]" + RESET;
            case "CLOSED":
                return RED + "[CLOSED]" + RESET;
            default:
                return "[" + status + "]";
        }
    }
}

