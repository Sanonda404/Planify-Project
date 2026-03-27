package com.planify.frontend.models.project;

public class ProjectCreateRequest {
    private String name;
    private String description;
    private String startDate;
    private String finalDeadline;
    private String groupUuid;
    private String creatorEmail;
    public ProjectCreateRequest(String name, String description, String startDate, String finalDeadline, String groupUuid, String creatorEmail){
        this.name = name;
        this.description = description;
        this.startDate =  startDate;
        this.finalDeadline = finalDeadline;
        this.groupUuid = groupUuid;
        this.creatorEmail = creatorEmail;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getFinalDeadline() {
        return finalDeadline;
    }
}
