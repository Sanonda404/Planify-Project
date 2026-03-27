// utils/DateUtils.java
package com.planify.frontend.chatbot.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter READABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter READABLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

    public static LocalDate parseDate(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        try {
            if (dateTimeStr.contains(" ")) {
                return LocalDate.parse(dateTimeStr.split(" ")[0], DATE_FORMATTER);
            } else {
                return LocalDate.parse(dateTimeStr, DATE_FORMATTER);
            }
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        try {
            if (dateTimeStr.contains(" ")) {
                return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
            } else {
                return LocalDateTime.parse(dateTimeStr + " 00:00:00", DATE_TIME_FORMATTER);
            }
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String formatDate(String dateTimeStr) {
        LocalDate date = parseDate(dateTimeStr);
        return date != null ? date.format(READABLE_DATE_FORMATTER) : "N/A";
    }

    public static String formatDateTime(String dateTimeStr) {
        LocalDateTime dateTime = parseDateTime(dateTimeStr);
        return dateTime != null ? dateTime.format(READABLE_DATE_TIME_FORMATTER) : "N/A";
    }

    public static String formatTime(String dateTimeStr) {
        LocalDateTime dateTime = parseDateTime(dateTimeStr);
        return dateTime != null ? dateTime.format(TIME_FORMATTER) : "N/A";
    }

    public static String formatTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(TIME_FORMATTER) : "N/A";
    }

    public static LocalDate getStartOfWeek(LocalDate date) {
        return date.with(java.time.DayOfWeek.MONDAY);
    }

    public static LocalDate getEndOfWeek(LocalDate date) {
        return date.with(java.time.DayOfWeek.SUNDAY);
    }

    public static boolean isSameDay(LocalDateTime dt1, LocalDateTime dt2) {
        if (dt1 == null || dt2 == null) return false;
        return dt1.toLocalDate().equals(dt2.toLocalDate());
    }
}