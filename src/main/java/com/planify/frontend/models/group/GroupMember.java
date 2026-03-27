package com.planify.frontend.models.group;

public class GroupMember {
    private String name;
    private String email;
    private String role;
    public GroupMember(String name, String email, String role){
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
