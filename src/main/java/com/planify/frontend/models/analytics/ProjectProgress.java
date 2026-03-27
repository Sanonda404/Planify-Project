package com.planify.frontend.models.analytics;


public class ProjectProgress {
    private String name;
    private int progress;
    private int completedTasks;
    private int totalTasks;
    private String color;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }
    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
