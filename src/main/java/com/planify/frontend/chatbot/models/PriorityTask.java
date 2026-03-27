// models/PriorityTask.java
package com.planify.frontend.chatbot.models;

import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.models.events.EventGetRequest;

import java.time.LocalDate;

public class PriorityTask {
    private String title;
    private String type; // "task" or "deadline"
    private LocalDate dueDate;
    private int urgencyScore;
    private int importanceScore;
    private String description;
    private String projectName;
    private String assignee;
    private String status;

    public PriorityTask(TaskDetails task) {
        this.title = task.getTitle();
        this.type = "task";
        this.description = task.getDescription();
        this.projectName = task.getProjectName();
        this.status = task.getStatus();
        if (task.getDueDate() != null) {
            this.dueDate = LocalDate.parse(task.getDueDate());
        }
        calculateScores();
    }

    public PriorityTask(EventGetRequest deadline) {
        this.title = deadline.getTitle();
        this.type = "deadline";
        this.description = deadline.getDescription();
        if (deadline.getStartDateTime() != null) {
            this.dueDate = LocalDate.parse(deadline.getStartDateTime().split(" ")[0]);
        }
        calculateScores();
    }

    private void calculateScores() {
        // Calculate urgency score based on days until due
        if (dueDate != null) {
            long daysUntil = LocalDate.now().until(dueDate).getDays();
            if (daysUntil < 0) {
                urgencyScore = 100; // Overdue
            } else if (daysUntil == 0) {
                urgencyScore = 95; // Due today
            } else if (daysUntil <= 3) {
                urgencyScore = 80;
            } else if (daysUntil <= 7) {
                urgencyScore = 60;
            } else if (daysUntil <= 14) {
                urgencyScore = 40;
            } else {
                urgencyScore = 20;
            }
        } else {
            urgencyScore = 10;
        }

        // Calculate importance score based on various factors
        importanceScore = 50; // Base score

        if (type.equals("deadline")) {
            importanceScore += 30; // Deadlines are inherently important
        }

        if (projectName != null && !projectName.isEmpty()) {
            importanceScore += 10; // Project tasks are important
        }

        if (status != null && "PENDING".equals(status)) {
            importanceScore += 10;
        }
    }

    public int getTotalPriority() {
        return (urgencyScore + importanceScore) / 2;
    }

    // Getters
    public String getTitle() { return title; }
    public String getType() { return type; }
    public LocalDate getDueDate() { return dueDate; }
    public int getUrgencyScore() { return urgencyScore; }
    public int getImportanceScore() { return importanceScore; }
    public String getDescription() { return description; }
    public String getProjectName() { return projectName; }
    public String getAssignee() { return assignee; }
    public String getStatus() { return status; }
}