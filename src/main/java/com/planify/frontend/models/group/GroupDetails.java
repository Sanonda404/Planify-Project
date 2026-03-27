package com.planify.frontend.models.group;

import com.planify.frontend.models.project.ProjectDetails;

import java.util.List;

public class GroupDetails {
    private String uuid;
    private String name;
    private String code;
    private String groupType; // e.g. "professional • 5 members"
    private String role;
    private String description;
    private String postingAceess;
    private String createdAt;

    // TODO: fetch these lists from backend
    private List<GroupMember> members;
    private List<EventSummary> events;
    private List<ProjectDetails> projects;

    public GroupDetails(String uuid, String name, String description,String groupType, String code, String postingAceess, String role, String createdAt, List<GroupMember>members, List<EventSummary> events, List<ProjectDetails>projects) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.code = code;
        this.groupType = groupType;
        this.postingAceess = postingAceess;
        this.createdAt = createdAt;
        this.events = events;
        this.projects = projects;
        this.members = members;
        this.role = role;
    }

    public String getName() { return name; }
    public String getCode() { return code; }
    public String getGroupType() { return groupType; }
    public String getRole() { return role; }

    public List<GroupMember> getMembers() { return members; }
    public void setMembers(List<GroupMember> members) { this.members = members; }

    public List<EventSummary> getEvents() { return events; }
    public void setEvents(List<EventSummary> events) { this.events = events; }

    public List<ProjectDetails> getProjects() { return projects; }
    public void setProjects(List<ProjectDetails> projects) { this.projects = projects; }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public String getPostingAceess() {
        return postingAceess;
    }

    public String getUuid() {
        return uuid;
    }
}