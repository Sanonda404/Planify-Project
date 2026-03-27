package com.planify.frontend.models.notification;

import com.planify.frontend.models.auth.MemberInfo;

public class NotificationResponse {
    private String uuid;
    private String recipient;   // Email of the person receiving it
    private MemberInfo sender;      // Who sent it (e.g., "Arif")
    private String title;
    private String message;     // "Invited you to Group X"
    private String type;        // "GROUP_INVITE", "PROJECT_ADDED", "TASK_UPDATE"
    private String status;
    private String targetUuid;  // The UUID of the Group or Project related to this
    private String createdAt;
    private Object updatedData;

    public NotificationResponse(String uuid, String recipient, MemberInfo sender,
                                String title, String message, String type,
                                String status, String targetUuid, String createdAt, Object updatedData) {
        this.uuid = uuid;
        this.recipient = recipient;
        this.sender = sender;
        this.title = title;
        this.message = message;
        this.type = type;
        this.status = status;
        this.targetUuid = targetUuid;
        this.createdAt = createdAt;
        this.updatedData = updatedData;
    }

    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getRecipient() {
        return recipient;
    }
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public MemberInfo getSender() {
        return sender;
    }
    public void setSender(MemberInfo sender) {
        this.sender = sender;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getTargetUuid() {
        return targetUuid;
    }
    public void setTargetUuid(String targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Object getUpdatedData(){
        return updatedData;
    }
}

