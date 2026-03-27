package com.planify.frontend.models.project;

import com.planify.frontend.models.tasks.TaskDetails;

import java.util.ArrayList;
import java.util.List;

public class ProjectCategory {
    private String name;
    private List<TaskDetails>tasks = new ArrayList<>();
    public ProjectCategory(String name){
        this.name = name;
    }
    public void addTask(TaskDetails task){
        tasks.add(task);
    }

    public String getName() {
        return name;
    }

    public int getCompleted(){
        int completed = 0;
        for(TaskDetails taskDetails: tasks){
            if(taskDetails.getStatus().equalsIgnoreCase("COMPLETED"))completed++;
        }
        return completed;
    }

    public int getTotal(){
        return tasks.size();
    }
}
