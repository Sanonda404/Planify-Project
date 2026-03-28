package com.planify.frontend.models.tasks;

import com.planify.frontend.models.auth.MemberInfo;

import java.util.List;

public class TaskDetails {
    private String uuid;
    private String title;
    private String description;
    private String dueDate;
    private String status;
    private String category;
    private boolean isDaily;
    private int weight;
    private String priority;
    private String completedAt;
    private String milestoneUuid;
    private String projectUuid;
    private String milestoneName;
    private String projectName;
    private MemberInfo creator;
    private List<MemberInfo> assigneeMembers;
    private String attachmentUrl;

    public TaskDetails(){

    }

    // Constructor
    public TaskDetails(String uuid, String title, String description, String dueDate, String status, String category,
                       boolean isDaily, int weight, String priority, String completedAt, String milestoneUuid, String projectUuid, String milestoneName, String projectName, MemberInfo creator, List<MemberInfo> assigneeMembers, String attachmentUrl) {
        this.uuid = uuid;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = status;
        this.category = category;
        this.isDaily = isDaily;
        this.weight = weight;
        this.priority = priority;
        this.completedAt = completedAt;
        this.milestoneUuid = milestoneUuid;
        this.projectUuid = projectUuid;
        this.milestoneName = milestoneName;
        this.projectName = projectName;
        this.creator = creator;
        this.assigneeMembers = assigneeMembers;
        this.attachmentUrl = attachmentUrl;
    }

    // Getters
    public String getUuid(){
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }

    public String getCategory() {
        return category;
    }

    public String getMilestoneUuid() {
        return milestoneUuid;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public String getMilestoneName() {
        return milestoneName;
    }

    public String getProjectName() {
        return projectName;
    }

    public MemberInfo getCreator() {
        return creator;
    }

    public List<MemberInfo> getAssigneeMembers() {
        return assigneeMembers;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public int getWeight() {
        return weight;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public String getPriority() {
        return priority;
    }

    public void setStatus(String value) {
        status = value;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public void setMilestoneName(String milestoneName) {
        this.milestoneName = milestoneName;
    }

    public void setMilestoneUuid(String milestoneUuid) {
        this.milestoneUuid = milestoneUuid;
    }

    public void setAssigneeMembers(List<MemberInfo> assigneeMembers) {
        this.assigneeMembers = assigneeMembers;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isDaily(){
        return isDaily;
    }
}