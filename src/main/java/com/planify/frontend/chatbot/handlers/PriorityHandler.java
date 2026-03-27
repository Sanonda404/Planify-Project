// handlers/PriorityHandler.java
package com.planify.frontend.chatbot.handlers;

import com.planify.frontend.chatbot.models.PriorityTask;
import com.planify.frontend.chatbot.models.QueryContext;
import com.planify.frontend.chatbot.utils.DateUtils;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.tasks.TaskDetails;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class PriorityHandler {

    private final List<EventGetRequest> events;
    private final List<TaskDetails> tasks;

    public PriorityHandler(List<EventGetRequest> events, List<TaskDetails> tasks) {
        this.events = events;
        this.tasks = tasks;
    }

    public String handle(QueryContext context) {
        List<PriorityTask> priorityTasks = getPriorityTasks();

        if (priorityTasks.isEmpty()) {
            return "No pending tasks or deadlines. You're all caught up! 🎉";
        }

        // Sort by priority score
        priorityTasks.sort((t1, t2) -> Integer.compare(t2.getTotalPriority(), t1.getTotalPriority()));

        switch (context.getIntent()) {
            case WHAT_TO_DO_FIRST:
                return formatTopPriority(priorityTasks);
            case URGENT_TASKS:
                return formatUrgentTasks(priorityTasks);
            case RECOMMENDATION:
                return formatRecommendations(priorityTasks);
            default:
                return formatTopPriority(priorityTasks);
        }
    }

    private List<PriorityTask> getPriorityTasks() {
        List<PriorityTask> priorityTasks = new ArrayList<>();

        // Add pending tasks
        tasks.stream()
                .filter(t -> !"COMPLETED".equalsIgnoreCase(t.getStatus()))
                .forEach(t -> priorityTasks.add(new PriorityTask(t)));

        // Add upcoming deadlines
        events.stream()
                .filter(e -> "Deadline".equalsIgnoreCase(e.getType()))
                .forEach(e -> priorityTasks.add(new PriorityTask(e)));

        return priorityTasks;
    }

    private String formatTopPriority(List<PriorityTask> tasks) {
        if (tasks.isEmpty()) {
            return "No tasks to prioritize. Enjoy your free time! 😊";
        }

        PriorityTask topTask = tasks.get(0);

        StringBuilder response = new StringBuilder();
        response.append("🎯 **Your Top Priority**\n\n");
        response.append("**").append(topTask.getTitle()).append("**\n");
        response.append("Priority Score: ").append(topTask.getTotalPriority()).append("/100\n");

        if (topTask.getDueDate() != null) {
            long daysUntil = LocalDate.now().until(topTask.getDueDate()).getDays();
            if (daysUntil < 0) {
                response.append("⚠️ **OVERDUE** - This should be done immediately!\n");
            } else if (daysUntil == 0) {
                response.append("⚠️ **DUE TODAY** - Complete this as soon as possible!\n");
            } else {
                response.append("📅 Due in ").append(daysUntil).append(" days\n");
            }
        }

        response.append("📋 Type: ").append(topTask.getType()).append("\n");

        if (topTask.getProjectName() != null) {
            response.append("📁 Project: ").append(topTask.getProjectName()).append("\n");
        }

        response.append("\n**Why this is important:**\n");
        response.append(getPriorityReason(topTask));

        return response.toString();
    }

    private String formatUrgentTasks(List<PriorityTask> tasks) {
        List<PriorityTask> urgentTasks = tasks.stream()
                .filter(t -> t.getTotalPriority() >= 70)
                .collect(Collectors.toList());

        if (urgentTasks.isEmpty()) {
            return "No urgent tasks at the moment. You're doing great! 🌟";
        }

        StringBuilder response = new StringBuilder();
        response.append("🚨 **Urgent Tasks**\n\n");

        for (PriorityTask task : urgentTasks) {
            response.append("• **").append(task.getTitle()).append("**");
            if (task.getDueDate() != null) {
                long daysUntil = LocalDate.now().until(task.getDueDate()).getDays();
                if (daysUntil < 0) {
                    response.append(" (OVERDUE!)");
                } else if (daysUntil == 0) {
                    response.append(" (DUE TODAY!)");
                } else if (daysUntil <= 3) {
                    response.append(" (Due in ").append(daysUntil).append(" days)");
                }
            }
            response.append("\n");
            response.append("  Priority: ").append(task.getTotalPriority()).append("/100\n");
            response.append("\n");
        }

        return response.toString();
    }

    private String formatRecommendations(List<PriorityTask> tasks) {
        StringBuilder response = new StringBuilder();
        response.append("💡 **Smart Recommendations**\n\n");

        // Check for overdue tasks
        List<PriorityTask> overdue = tasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());

        if (!overdue.isEmpty()) {
            response.append("⚠️ You have ").append(overdue.size()).append(" overdue ");
            response.append(overdue.size() == 1 ? "task" : "tasks");
            response.append(". Focus on these first:\n");
            overdue.stream().limit(3).forEach(t ->
                    response.append("  • ").append(t.getTitle()).append("\n"));
            response.append("\n");
        }

        // Check for tasks due today
        List<PriorityTask> dueToday = tasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().equals(LocalDate.now()))
                .collect(Collectors.toList());

        if (!dueToday.isEmpty()) {
            response.append("📅 You have ").append(dueToday.size()).append(" ");
            response.append(dueToday.size() == 1 ? "task" : "tasks");
            response.append(" due today. Complete these by the end of the day.\n\n");
        }

        // Check for upcoming deadlines
        long upcomingCount = tasks.stream()
                .filter(t -> t.getDueDate() != null &&
                        t.getDueDate().isAfter(LocalDate.now()) &&
                        t.getDueDate().isBefore(LocalDate.now().plusDays(3)))
                .count();

        if (upcomingCount > 0) {
            response.append("🎯 You have ").append(upcomingCount);
            response.append(" tasks due in the next 3 days. ");
            response.append("Consider working on them to avoid last-minute rush.\n\n");
        }

        // Productivity suggestion
        if (tasks.size() > 10) {
            response.append("📊 You have many pending tasks. ");
            response.append("Try breaking them down into smaller, manageable chunks. ");
            response.append("Focus on one task at a time to maintain momentum.\n");
        } else if (tasks.size() > 5) {
            response.append("📊 You're making good progress. ");
            response.append("Keep up the momentum by completing smaller tasks first for quick wins!\n");
        }

        return response.toString();
    }

    private String getPriorityReason(PriorityTask task) {
        if (task.getDueDate() != null) {
            long daysUntil = LocalDate.now().until(task.getDueDate()).getDays();
            if (daysUntil < 0) {
                return "This task is overdue and should be completed immediately to avoid negative consequences.";
            } else if (daysUntil == 0) {
                return "This task is due today. Completing it now will prevent last-minute stress.";
            } else if (daysUntil <= 3) {
                return "This task is due in " + daysUntil + " days. Early completion allows buffer time.";
            }
        }

        if (task.getType().equals("deadline")) {
            return "Deadlines are time-sensitive and missing them can have significant impacts.";
        }

        if (task.getProjectName() != null && !task.getProjectName().isEmpty()) {
            return "This task is part of project '" + task.getProjectName() + "' and affects team progress.";
        }

        return "This task has been identified as a priority based on its urgency and importance.";
    }
}