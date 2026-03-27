package com.planify.frontend.models.auth;

public class DashboardEventSummary {
    private String name;
    private String time;
    private String groupName;
    private String color;
    public DashboardEventSummary(String name, String time, String groupName, String color){
        this.name = name;
        this.time = time;
        this.groupName = groupName;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getTime() {
        return time;
    }

    public String getColor() {
        return color;
    }
}
