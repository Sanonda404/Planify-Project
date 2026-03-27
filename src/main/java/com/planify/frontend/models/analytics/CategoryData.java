package com.planify.frontend.models.analytics;

public class CategoryData {
    private int total;
    private int completed;
    private String color;

    public CategoryData(int total, int completed) {
        this.total = total;
        this.completed = completed;
        this.color = "#457b9d";
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getCompleted() { return completed; }
    public void setCompleted(int completed) { this.completed = completed; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}

