package com.planify.frontend.models.group;

public class GroupSummaryRequest {
    private String uuid;
    private String name;
    private int totalMembers;
    private String groupType;
    private int upcomingEvents;
    private int activeProjects;
    private String role;
    private String description;
    public GroupSummaryRequest(String uuid, String name,String description, int totalMembers, String groupType, int upcomingEvents, int activeProjects, String role){
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.totalMembers = totalMembers;
        this.groupType = groupType;
        this.upcomingEvents = upcomingEvents;
        this.activeProjects = activeProjects;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getActiveProjects() {
        return activeProjects;
    }

    public int getTotalMembers() {
        return totalMembers;
    }

    public int getUpcomingEvents() {
        return upcomingEvents;
    }

    public String getGroupType(){
        return groupType;
    }

    public String getRole() {
        return role;
    }

    public String getUuid() {
        return uuid;
    }
}
