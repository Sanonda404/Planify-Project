package com.planify.frontend.models.analytics;

public class GroupActivity {
    private String name;
    private int activeMembers;
    private int totalMembers;
    private int activePercentage;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getActiveMembers() { return totalMembers; }
    public void setActiveMembers(int activeMembers) { this.activeMembers = activeMembers; }
    public int getTotalMembers() { return totalMembers; }
    public void setTotalMembers(int totalMembers) { this.totalMembers = totalMembers; }
    public int getActivePercentage() { return activePercentage; }
    public void setActivePercentage(int activePercentage) { this.activePercentage = activePercentage; }
}

