// models/TimeRange.java
package com.planify.frontend.chatbot.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TimeRange {
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String relativeRange; // "today", "tomorrow", "this_week", "next_week", "this_month"

    public TimeRange() {
        this.relativeRange = "today";
    }

    public TimeRange(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public String getRelativeRange() { return relativeRange; }
    public void setRelativeRange(String relativeRange) { this.relativeRange = relativeRange; }

    public boolean contains(LocalDate date) {
        if (startDate == null || endDate == null) return false;
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean contains(LocalDateTime dateTime) {
        if (startDateTime == null || endDateTime == null) return false;
        return !dateTime.isBefore(startDateTime) && !dateTime.isAfter(endDateTime);
    }
}