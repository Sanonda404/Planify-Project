package com.planify.frontend.models.auth;

public class MemberInfo {
    private String name;
    private String email;

    public MemberInfo() {}

    public MemberInfo(String name, String email){
        this.name = name;
        this.email = email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return name;
    }
}
