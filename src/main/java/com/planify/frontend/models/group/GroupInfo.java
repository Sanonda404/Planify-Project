package com.planify.frontend.models.group;

public class GroupInfo {
    private String description;
    private String groupType;
    private String postingAceess;
    private String role;
    private String createdAt;
    public GroupInfo(String description, String groupType, String postingAceess, String role, String createdAt){
        this.description = description;
        this.groupType = groupType;
        this.postingAceess = postingAceess;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public String getRole() {
        return role;
    }

    public String getGroupType() {
        return groupType;
    }

    public String getPostingAceess() {
        return postingAceess;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
