package com.planify.frontend.models.notification;

public class ReminderTask {
    private String uuid;
    private String title;
    private String triggerAt;
    private String actualTime;
    private String type;
    private String message;

    public ReminderTask(String uuid, String title, String triggerAt, String actualTime, String type) {
        this.uuid = uuid;
        this.title = title;
        this.triggerAt = triggerAt;
        this.actualTime = actualTime;
        this.type = type;
        this.message = "";
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

    public String getActualTime(){
        return actualTime;
    }

    public void setMessage(String body) {
        this.message = message;
    }
}
