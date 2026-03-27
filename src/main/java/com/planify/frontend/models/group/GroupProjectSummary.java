package com.planify.frontend.models.group;

public class GroupProjectSummary {
    private String name;
    private String deadline;
    private int completedPercentage;
    public GroupProjectSummary(String name, String deadline, int completedPercentage){
        this.name = name;
        this.deadline = deadline;
        this.completedPercentage = completedPercentage;
    }

    public String getName() {
        return name;
    }

    public int getCompletedPercentage() {
        return completedPercentage;
    }

    public String getDeadline() {
        return deadline;
    }
}
