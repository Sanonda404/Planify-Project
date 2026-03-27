package com.planify.frontend.models.tasks;

import com.planify.frontend.models.auth.MemberInfo;

import java.util.List;

public class TaskRequest {
    private String title;
    private String description;
    private String category;
    private String dueDate;
    private boolean isDaily;
    private int weight;
    private String priority;
    private String projectUuid;
    private String milestoneUuid;
    private String milestoneName;
    private String projectName;
    private String creatorEmail;
    private List<MemberInfo>assigneeEmails;
    private String attachmentUrl;
    public  TaskRequest(String title, String description, String category, String dueDate,
                        String projectUuid, String milestoneUuid, boolean isDaily, int weight, String priority,
                        String milestoneName, String projectName, String creatorEmail, List<MemberInfo>assigneeEmails, String attachmentUrl){
        this.title = title;
        this.description = description;
        this.category = category;
        this.dueDate = dueDate;
        this.isDaily = isDaily;
        this.weight = weight;
        this.priority = priority;
        this.projectUuid = projectUuid;
        this.milestoneUuid = milestoneUuid;
        this.milestoneName = milestoneName;
        this.projectName = projectName;
        this.attachmentUrl = attachmentUrl;
        this.creatorEmail = creatorEmail;
        this.assigneeEmails = assigneeEmails;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getMilestoneUuid() {
        return milestoneUuid;
    }

    public String getProjectName() {
        return projectName;
    }

    public List<MemberInfo> getAssigneeEmails() {
        return assigneeEmails;
    }

    public String getMilestoneName() {
        return milestoneName;
    }

    public boolean isDaily() {
        return isDaily;
    }
}
