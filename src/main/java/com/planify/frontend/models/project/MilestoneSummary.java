package com.planify.frontend.models.project;

import java.time.LocalDate;

public class MilestoneSummary {
    private String title;
    private String description;
    private String uuid;
    private String deadline;
    private boolean isCompleted;
    public MilestoneSummary(String title, String description, String uuid, String deadline, boolean isCompleted){
        this.title = title;
        this.description = description;
        this.uuid = uuid;
        this.deadline = deadline;
        this.isCompleted = isCompleted;
    }

    public boolean isCompleted() {
        return isCompleted;
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

    public String getDeadline() {
        return deadline;
    }
}
