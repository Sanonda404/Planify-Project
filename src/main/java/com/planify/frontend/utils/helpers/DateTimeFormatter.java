package com.planify.frontend.utils.helpers;

import java.time.LocalDateTime;

public class DateTimeFormatter {
    public static String FormatDateTime(LocalDateTime dateTime){
        // Month names manually
        String[] months = {
                "January","February","March","April","May","June",
                "July","August","September","October","November","December"
        };

        int day = dateTime.getDayOfMonth();
        String month = months[dateTime.getMonthValue() - 1];
        int year = dateTime.getYear();

        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();

        // Convert to 12-hour format
        String ampm = (hour >= 12) ? "Pm" : "Am";
        int hour12 = hour % 12;
        if (hour12 == 0) hour12 = 12;

        // Pad minutes with leading zero if needed
        String minuteStr = (minute < 10) ? "0" + minute : String.valueOf(minute);

        return day + " " + month + ", " + year + " at " + hour12 + ":" + minuteStr + " " + ampm;
    }

    public static String FormatDateTime(String date){
        LocalDateTime dateTime = LocalDateTime.parse(date);
        // Month names manually
        String[] months = {
                "January","February","March","April","May","June",
                "July","August","September","October","November","December"
        };

        int day = dateTime.getDayOfMonth();
        String month = months[dateTime.getMonthValue() - 1];
        int year = dateTime.getYear();

        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();

        // Convert to 12-hour format
        String ampm = (hour >= 12) ? "Pm" : "Am";
        int hour12 = hour % 12;
        if (hour12 == 0) hour12 = 12;

        // Pad minutes with leading zero if needed
        String minuteStr = (minute < 10) ? "0" + minute : String.valueOf(minute);

        return day + " " + month + ", " + year + " at " + hour12 + ":" + minuteStr + " " + ampm;
    }


}
