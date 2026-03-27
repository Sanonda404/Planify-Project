package com.planify.frontend.models.auth;

import com.planify.frontend.models.tasks.TaskDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardWeeklyProductivitySummary {

    private int completed;
    private int inProgress;
    private int pending;
    private int completedWeight;
    private int totalWeight;

    /**
     * Constructor with weight support
     *
     * @param allTasks List of all tasks to analyze
     * @param useWeight If true, calculate weighted productivity
     */
    public DashboardWeeklyProductivitySummary(List<TaskDetails> allTasks, boolean useWeight) {
        this.completed = 0;
        this.inProgress = 0;
        this.pending = 0;
        this.completedWeight = 0;
        this.totalWeight = 0;

        if (allTasks == null || allTasks.isEmpty()) {
            return;
        }

        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

        for (TaskDetails task : allTasks) {
            LocalDate taskDate = getTaskDate(task);

            // Only count tasks from the last week
            if (taskDate != null && !taskDate.isBefore(oneWeekAgo)) {
                countTask(task, useWeight);
            } else if (taskDate == null) {
                countTask(task, useWeight);
            }
        }
    }

    /**
     * Get the task's relevant date (due date or creation date)
     */
    private LocalDate getTaskDate(TaskDetails task) {
        if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
            try {
                LocalDateTime dueDateTime = LocalDateTime.parse(task.getDueDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return dueDateTime.toLocalDate();
            } catch (Exception e) {
                try {
                    return LocalDate.parse(task.getDueDate());
                } catch (Exception ex) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Count task by status with optional weight
     */
    private void countTask(TaskDetails task, boolean useWeight) {
        String status = task.getStatus() != null ? task.getStatus().toUpperCase() : "PENDING";
        int weight = getTaskWeight(task);

        if (useWeight) {
            totalWeight += weight;
        }

        switch (status) {
            case "COMPLETED":
                completed++;
                if (useWeight) completedWeight += weight;
                break;
            case "IN_PROGRESS":
                inProgress++;
                break;
            default:
                pending++;
                break;
        }
    }

    /**
     * Get task weight (default to 5 if not available)
     */
    private int getTaskWeight(TaskDetails task) {
        return task.getWeight();
    }

    public int getCompleted() {
        return completed;
    }

    public int getInProgress() {
        return inProgress;
    }

    public int getPending() {
        return pending;
    }

    public int getTotalTasks() {
        return completed + inProgress + pending;
    }

    public int getCompletionRate() {
        int total = getTotalTasks();
        return total > 0 ?(int) (completed * 100.0 / total) : 0;
    }

    public int getCompletedWeight() {
        return completedWeight;
    }

    public int getTotalWeight() {
        return totalWeight;
    }

    public int getWeightedCompletionRate() {
        return totalWeight > 0 ? (int)(completedWeight * 100.0 / totalWeight) : 0;
    }

    @Override
    public String toString() {
        return String.format("Weekly Productivity: Tasks: %d/%d (%.1f%%) | Weight: %d/%d (%.1f%%)",
                completed, getTotalTasks(), getCompletionRate(),
                completedWeight, totalWeight, getWeightedCompletionRate());
    }
}