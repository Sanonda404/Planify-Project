package com.planify.frontend.models.tasks;

import java.util.List;

public class PersonalTaskResponse {
    private String title;
    private String description;
    private String category;
    private String dueDateTime;
    private String status;
    private int weight;
    private String priority;
    private String attachmentUrl;
    public  PersonalTaskResponse(String title, String description, String category, String dueDateTime, String status, int weight, String priority, String attachmentUrl){
        this.title = title;
        this.description = description;
        this.category = category;
        this.dueDateTime = dueDateTime;
        this.status = status;
        this.weight = weight;
        this.priority = priority;
        this.attachmentUrl = attachmentUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public String getDueDateTime() {
        return dueDateTime;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDueDateTime(String dueDateTime) {
        this.dueDateTime = dueDateTime;
    }

    public String getPriority() {
        return priority;
    }

    public int getWeight() {
        return weight;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
