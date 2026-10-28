package com.hiresphere.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for rendering aligned, formatted ASCII tables in console applications.
 */
public class TableFormatter {

    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();

    public TableFormatter(String... headers) {
        this.headers = headers;
    }

    public void addRow(String... row) {
        rows.add(row);
    }

    public void print() {
        int columns = headers.length;
        int[] maxColWidths = new int[columns];

        for (int i = 0; i < columns; i++) {
            maxColWidths[i] = stripAnsi(headers[i]).length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < Math.min(row.length, columns); i++) {
                int len = stripAnsi(row[i]).length();
                if (len > maxColWidths[i]) {
                    maxColWidths[i] = len;
                }
            }
        }

        // Add padding
        for (int i = 0; i < columns; i++) {
            maxColWidths[i] += 2;
        }

        // Build horizontal separator
        StringBuilder sep = new StringBuilder("+");
        for (int w : maxColWidths) {
            sep.append("-".repeat(w)).append("+");
        }
        String separatorLine = sep.toString();

        System.out.println(separatorLine);

        // Print header
        StringBuilder headerLine = new StringBuilder("|");
        for (int i = 0; i < columns; i++) {
            headerLine.append(centerText(ConsoleColors.bold(headers[i]), maxColWidths[i])).append("|");
        }
        System.out.println(headerLine);
        System.out.println(separatorLine);

        // Print rows
        if (rows.isEmpty()) {
            StringBuilder emptyLine = new StringBuilder("|");
            int totalWidth = separatorLine.length() - 2;
            emptyLine.append(centerText("No records found", totalWidth)).append("|");
            System.out.println(emptyLine);
        } else {
            for (String[] row : rows) {
                StringBuilder rowLine = new StringBuilder("|");
                for (int i = 0; i < columns; i++) {
                    String val = (i < row.length && row[i] != null) ? row[i] : "";
                    rowLine.append(padText(val, maxColWidths[i])).append("|");
                }
                System.out.println(rowLine);
            }
        }

        System.out.println(separatorLine);
    }

    private static String stripAnsi(String s) {
        if (s == null) return "";
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    private static String padText(String text, int width) {
        int visibleLength = stripAnsi(text).length();
        int spaces = Math.max(0, width - visibleLength - 1);
        return " " + text + " ".repeat(spaces);
    }

    private static String centerText(String text, int width) {
        int visibleLength = stripAnsi(text).length();
        int totalSpaces = Math.max(0, width - visibleLength);
        int padLeft = totalSpaces / 2;
        int padRight = totalSpaces - padLeft;
        return " ".repeat(padLeft) + text + " ".repeat(padRight);
    }
}

