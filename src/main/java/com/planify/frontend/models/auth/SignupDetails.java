package com.planify.frontend.models.auth;

public class SignupDetails {
    private String name;
    private String email;
    private String password;
    public SignupDetails(String name, String email, String password){
        this.email = email;
        this.name = name;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
