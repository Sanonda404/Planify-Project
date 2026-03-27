package com.planify.frontend.models.group;

import java.time.LocalDateTime;

public class EventSummary {
    private String title;
    private String description;
    private String type;
    private String startDateTime;
    private String endDateTime;   // For span events

    public EventSummary(String title, String description, String type,
                      String startDateTime, String endDateTime) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }

    public String getType() {
        return type;
    }

    public String getStartDateTime() { return startDateTime; }
    public String getEndDateTime() { return endDateTime; }
}