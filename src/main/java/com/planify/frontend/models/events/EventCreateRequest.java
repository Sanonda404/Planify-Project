package com.planify.frontend.models.events;

import java.util.List;

public class EventCreateRequest {
    private String title;
    private String description;
    private String type;
    private String color;
    private String startDateTime;
    private String endDateTime;
    private String groupUuid;
    private boolean mergeWithPersonal;
    private String repeatPattern;
    private List<String> excludedDays;
    private String monthlyDate;
    private int reminderMinutesBefore;
    private String reminderType;
    private String attachmentUrl;
    private String creatorEmail;

    public EventCreateRequest(String title, String description, String type, String color,
                              String startDateTime, String endDateTime,
                              String groupUuid, boolean mergeWithPersonal,
                              String repeatPattern, List<String> excludedDays, String monthlyDate,
                              int reminderMinutesBefore, String reminderType, String attachmentUrl, String creatorEmail) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.color = color;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.groupUuid = groupUuid;
        this.mergeWithPersonal = mergeWithPersonal;
        this.repeatPattern = repeatPattern;
        this.excludedDays = excludedDays;
        this.monthlyDate = monthlyDate;
        this.reminderMinutesBefore = reminderMinutesBefore;
        this.reminderType = reminderType;
        this.attachmentUrl = attachmentUrl;
        this.creatorEmail = creatorEmail;
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getColor() { return color; }
    public String getStartDateTime() { return startDateTime; }
    public String getEndDateTime() { return endDateTime; }
    public String getGroupUuid() { return groupUuid; }
    public boolean isMergeWithPersonal() { return mergeWithPersonal; }
    public String getRepeatPattern() { return repeatPattern; }
    public List<String> getExcludedDays() { return excludedDays; }
    public String getMonthlyDate() { return monthlyDate; }
    public int getReminderMinutesBefore() { return reminderMinutesBefore; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public String getCreatorEmail() { return creatorEmail; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setType(String type) { this.type = type; }
    public void setColor(String color) { this.color = color; }
    public void setStartDateTime(String startDateTime) { this.startDateTime = startDateTime; }
    public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }
    public void setGroupUuid(String groupUuid) { this.groupUuid = groupUuid; }
    public void setMergeWithPersonal(boolean mergeWithPersonal) { this.mergeWithPersonal = mergeWithPersonal; }
    public void setRepeatPattern(String repeatPattern) { this.repeatPattern = repeatPattern; }
    public void setExcludedDays(List<String> excludedDays) { this.excludedDays = excludedDays; }
    public void setMonthlyDate(String monthlyDate) { this.monthlyDate = monthlyDate; }
    public void setReminderMinutesBefore(int reminderMinutesBefore) { this.reminderMinutesBefore = reminderMinutesBefore; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public void setCreatorEmail(String creatorEmail) { this.creatorEmail = creatorEmail; }

    public String getReminderType() {
        return reminderType;
    }
}