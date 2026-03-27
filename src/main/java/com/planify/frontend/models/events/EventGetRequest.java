package com.planify.frontend.models.events;

import com.planify.frontend.models.auth.MemberInfo;

import java.util.List;

public class EventGetRequest {
    private String uuid;
    private String title;
    private String description;
    private String type;
    private String color;
    private String startDateTime;
    private String endDateTime;
    //{name:group_name, email:group_uuid}
    private MemberInfo group;
    private boolean mergeWithPersonal;
    private String repeatPattern;
    private List<String> excludedDays;
    private String monthlyDate;
    private int reminderMinutesBefore;
    private String reminderType;
    private String attachmentUrl;
    private MemberInfo creator;
    private boolean editingPermission;
    public EventGetRequest(String uuid, String title, String description, String type, String color,
                              String startDateTime, String endDateTime,
                              MemberInfo group, boolean mergeWithPersonal,
                              String repeatPattern, List<String> excludedDays, String monthlyDate,
                              int reminderMinutesBefore, String reminderType, String attachmentUrl, MemberInfo creator,
                           boolean editingPermission) {
        this.uuid = uuid;
        this.title = title;
        this.description = description;
        this.type = type;
        this.color = color;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.group = group;
        this.mergeWithPersonal = mergeWithPersonal;
        this.repeatPattern = repeatPattern;
        this.excludedDays = excludedDays;
        this.monthlyDate = monthlyDate;
        this.reminderMinutesBefore = reminderMinutesBefore;
        this.reminderType = reminderType;
        this.attachmentUrl = attachmentUrl;
        this.creator = creator;
        this.editingPermission = editingPermission;
    }

    public String getUuid() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public boolean isMergeWithPersonal() {
        return mergeWithPersonal;
    }

    public int getReminderMinutesBefore() {
        return reminderMinutesBefore;
    }

    public List<String> getExcludedDays() {
        return excludedDays;
    }

    public MemberInfo getGroup() {
        return group;
    }

    public String getColor() {
        return color;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public String getMonthlyDate() {
        return monthlyDate;
    }

    public MemberInfo getCreator() {
        return creator;
    }

    public boolean isEditingPermission() {
        return editingPermission;
    }

    public String getRepeatPattern(){
        return this.repeatPattern;
    }

    public String getReminderType() {
        return reminderType;
    }
}
