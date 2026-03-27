package com.planify.frontend.models.milestone;

import java.util.List;

public class MilestoneCreateRequest {
    private String title;
    private String description;
    private String deadline;
    private List<String> taskUuids;
    private String projectUuid;
    public MilestoneCreateRequest(String title, String description, String deadline, List<String>taskUuids, String projectUuid){
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.taskUuids = taskUuids;
        this.projectUuid = projectUuid;
    }

    public String getDescription() {
        return description;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public String getTitle() {
        return title;
    }

    public String getDeadline() {
        return deadline;
    }

    public List<String> getTaskUuids() {
        return taskUuids;
    }
}
