package com.planify.frontend.models.auth;

public class DashboardProjectSummary {
    private String name;
    private int progress;
    private int totalTasks;
    private int completedTasks;
    public DashboardProjectSummary(String name, int progress, int totalTasks, int completedTasks){
        this.name = name;
        this.progress = progress;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
    }

    public String getName() {
        return name;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public int getProgress() {
        return progress;
    }
}
