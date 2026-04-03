package com.planify.frontend.models.project;

import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.resources.ResourceDetails;
import com.planify.frontend.models.tasks.TaskDetails;

import java.util.ArrayList;
import java.util.List;

public class ProjectDetails {
    private String name;
    private String description;
    private String startDate;
    private String deadline;
    private String groupName;
    private String groupUuid;
    private String uuid;
    private int totalMilestones;
    private int totalTasks;
    private int totalMembers;
    private int progress;
    private List<MilestoneDetails>milestones = new ArrayList();
    private List<ResourceDetails>resources = new ArrayList<>();
    private List<MemberInfo>members;
    private boolean isCompleted;
    public ProjectDetails(String name, String description, String startDate, String deadline, String groupName,
                          String groupUuid, String uuid, int totalMilestones, int totalTasks, int totalMembers,
                          int progress, List<MilestoneDetails>milestones, List<ResourceDetails>resources,
                          List<MemberInfo>members, boolean isCompleted){
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.deadline = deadline;
        this.groupName = groupName;
        this.groupUuid = groupUuid;
        this.uuid = uuid;
        this.totalMembers = totalMembers;
        this.totalTasks = totalTasks;
        this.totalMilestones = totalMilestones;
        this.progress = progress;
        this.milestones = milestones;
        this.resources = resources;
        this.members = members;
        this.isCompleted = isCompleted;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUuid() {
        return uuid;
    }

    public int getTotalMembers() {
        return totalMembers;
    }

    public int getProgress() {
        return progress;
    }

    public int getTotalMilestones() {
        return totalMilestones;
    }

    public List<MilestoneDetails> getMilestones() {
        return milestones;
    }

    public List<ResourceDetails> getResources(){
        return resources;
    }

    public String getGroupName() {
        return groupName;
    }

    public List<MemberInfo> getMembers() {
        return members;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public String getDeadline() {
        return deadline;
    }

    public int getCompletedTasks(){
        int completed = 0;
        for(MilestoneDetails milestone: milestones){
            for(TaskDetails task: milestone.getTasks()){
                if(task.getStatus().equalsIgnoreCase("COMPLETED"))completed++;
            }
        }
        return completed;
    }

    public int getCompletedMilestones(){
        int completed = 0;
        for(MilestoneDetails milestone: milestones){
            if(milestone.isCompleted())completed++;
        }
        return completed;
    }

    public int getInProgressMilestones(){
        int completed = 0;
        for(MilestoneDetails milestone: milestones){
            int taken = 0;
            for(TaskDetails task: milestone.getTasks()){
                if(task.getStatus().equalsIgnoreCase("COMPLETED"))taken++;
            }
            if(taken>0 && taken<milestone.getTasks().size())completed++;
        }
        return completed;
    }

    public int getPendingMilestones(){
        int completed = 0;
        for(MilestoneDetails milestone: milestones){
            int taken = 0;
            for(TaskDetails task: milestone.getTasks()){
                if(task.getStatus().equalsIgnoreCase("COMPLETED"))taken++;
            }
            if(taken==0)completed++;
        }
        return completed;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setMilestones(List<MilestoneDetails> milestones){
        this.milestones = milestones;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setProgress(int progress){
        this.progress = progress;
    }
}
