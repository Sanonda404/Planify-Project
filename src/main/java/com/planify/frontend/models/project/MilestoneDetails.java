package com.planify.frontend.models.project;

import com.planify.frontend.models.tasks.TaskDetails;

import java.util.ArrayList;
import java.util.List;

public class MilestoneDetails {
    private String title;
    private String description;
    private String uuid;
    private String projectUuid;
    private boolean completed;
    private String deadline;
    private int completionRate;
    private List<TaskDetails>tasks = new ArrayList<>();
    public MilestoneDetails(String title, String description, String uuid, String projectUuid, boolean completed, String deadline, int completionRate, List<TaskDetails>tasks){
        this.title = title;
        this.description = description;
        this.uuid = uuid;
        this.projectUuid  = projectUuid;
        this.completed = completed;
        this.deadline = deadline;
        this.completionRate = completionRate;
        this.tasks = tasks;
    }

    public List<TaskDetails> getTasks() {
        return tasks;
    }

    public String getDescription() {
        return description;
    }

    public String getUuid() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getDeadline() {
        return deadline;
    }

    public int getCompletionRate() {
        return completionRate;
    }


    public void setCompletionRate(int completionRate) {
        this.completionRate = completionRate;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCompletedTasks(){
        if(tasks==null)return 0;
        int completed = 0;
        for(TaskDetails task: tasks){
            if(task.getStatus().equalsIgnoreCase("COMPLETED"))completed++;
        }
        return completed;
    }

    public int getTotalTasks(){
        return tasks.size();
    }

    public void setTasks(List<TaskDetails> tasks) {
        this.tasks = tasks;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

}
