package com.planify.frontend.models.group;

public class GroupCreateRequest {
    private String name;
    private String description;
    private String groupType;
    private boolean allowCodeJoin;
    private boolean onlyAdminCanPost;
    private String adminEmail;
    public GroupCreateRequest(String name, String description, String groupType, boolean allowCodeJoin, boolean onlyAdminCanPost, String adminEmail){
        System.out.println(adminEmail);
        this.name = name;
        this.description = description;
        this.groupType = groupType;
        this.allowCodeJoin = allowCodeJoin;
        this.onlyAdminCanPost = onlyAdminCanPost;
        this.adminEmail = adminEmail;
    }
}
