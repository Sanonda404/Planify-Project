package com.planify.frontend.models.analytics;

public class Insight {
    private String title;
    private String message;
    private String type;

    public Insight(String title, String message, String type) {
        this.title = title;
        this.message = message;
        this.type = type;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
}
