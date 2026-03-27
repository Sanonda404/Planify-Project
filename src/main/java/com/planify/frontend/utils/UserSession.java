package com.planify.frontend.utils;


public class UserSession {
    private static UserSession instance;
    private String name;
    private String email;
    private String token = "";

    // Private constructor
    private UserSession(String name, String email, String token) {
        this.name = name;
        this.email = email;
        this.token = token;
    }

    // Factory method to initialize the singleton
    public static void init(String name, String email, String token) {
        instance = new UserSession(name, email, token);
    }

    public static UserSession getInstance() {
        return instance;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public static void logout() {
        instance = null;
    }
}