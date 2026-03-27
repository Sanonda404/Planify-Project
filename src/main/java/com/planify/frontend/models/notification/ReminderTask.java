package com.planify.frontend.models.notification;

public class ReminderTask {
    private String uuid;
    private String title;
    private String triggerAt; // ISO String
    private String type;

    public ReminderTask(String uuid, String title, String triggerAt, String type) {
        this.uuid = uuid;
        this.title = title;
        this.triggerAt = triggerAt;
        this.type = type;
    }

    public String getUuid(){
        return uuid;
    }

    public String getTriggerAt() {
        return triggerAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setTriggerAt(String triggerAt) {
        this.triggerAt = triggerAt;
    }
}
