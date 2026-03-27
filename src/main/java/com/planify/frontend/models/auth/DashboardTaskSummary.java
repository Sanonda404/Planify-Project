package com.planify.frontend.models.auth;

public class DashboardTaskSummary {
    private String name;
    private String time;
    private String status;
    public DashboardTaskSummary(String name, String time, String status){
        this.name = name;
        this.time = time;
        this.status = status;
    }

    public String getTime() {
        return time;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
}
