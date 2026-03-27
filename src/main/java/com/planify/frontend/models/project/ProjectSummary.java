package com.planify.frontend.models.project;

import com.planify.frontend.models.auth.MemberInfo;

import java.util.ArrayList;
import java.util.List;

public class ProjectSummary {
    private String name;
    private String description;
    private String groupName;
    private String uuid;
    private int totalMilestones;
    private int totalMembers;
    private int progress;
    private List<MilestoneSummary>milestones = new ArrayList();
    //{Name,Email}
    private List<MemberInfo>members;
    public ProjectSummary(String name, String description, String groupName, String uuid, int totalMilestones, int totalMembers, int progress, List<MilestoneSummary>milestones, List<MemberInfo>members){
        this.name = name;
        this.description = description;
        this.groupName = groupName;
        this.uuid = uuid;
        this.totalMembers = totalMembers;
        this.totalMilestones = totalMilestones;
        this.progress = progress;
        this.milestones = milestones;
        this.members = members;
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

    public List<MilestoneSummary> getMilestones() {
        return milestones;
    }

    public String getGroupName() {
        return groupName;
    }

    public List<MemberInfo> getMembers() {
        return members;
    }

    public void setMilestones(List<MilestoneSummary> milestones) {
        this.milestones = milestones;
    }

    public void setTotalMilestones(int totalMilestones) {
        this.totalMilestones = totalMilestones;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
