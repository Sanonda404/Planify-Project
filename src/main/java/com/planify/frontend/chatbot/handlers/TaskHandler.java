// handlers/TaskHandler.java
package com.planify.frontend.chatbot.handlers;

import com.planify.frontend.chatbot.models.Intent;
import com.planify.frontend.chatbot.models.QueryContext;
import com.planify.frontend.chatbot.models.TimeRange;
import com.planify.frontend.chatbot.utils.DateUtils;
import com.planify.frontend.chatbot.utils.ResponseFormatter;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TaskHandler {

    private final List<TaskDetails> tasks;
    private final List<ProjectDetails> projects;

    public TaskHandler(List<TaskDetails> tasks, List<ProjectDetails> projects) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.projects = projects != null ? projects : new ArrayList<>();
    }

    public String handle(QueryContext context) {
        System.out.println("DEBUG: TaskHandler handling intent: " + context.getIntent());
        System.out.println("DEBUG: Total tasks available: " + tasks.size());

        List<TaskDetails> filteredTasks = filterTasks(context);

        System.out.println("DEBUG: Filtered tasks count: " + filteredTasks.size());

        if (filteredTasks.isEmpty()) {
            return getNoTasksMessage(context);
        }

        switch (context.getIntent()) {
            case TASK_DAILY:
                return formatDailyTasks(filteredTasks);
            case TASK_PENDING:
                return formatPendingTasks(filteredTasks);
            case TASK_COMPLETED:
                return formatCompletedTasks(filteredTasks);
            case TASK_IN_PROGRESS:
                return formatInProgressTasks(filteredTasks);
            case TASK_OVERDUE:
                return formatOverdueTasks(filteredTasks);
            case TASK_BY_PROJECT:
                return formatTasksByProject(filteredTasks, context);
            case TASK_BY_CATEGORY:
                return formatTasksByCategory(filteredTasks, context);
            default:
                return formatTaskList(filteredTasks);
        }
    }

    private List<TaskDetails> filterTasks(QueryContext context) {
        List<TaskDetails> result = new ArrayList<>(tasks);

        // For DAILY tasks, we want tasks that are either:
        // 1. Marked as "Daily todo" category
        // 2. Or tasks with due date today
        // 3. Or tasks that are pending and don't have a future due date
        if (context.getIntent() == Intent.TASK_DAILY) {
            result = result.stream()
                    .filter(t -> {
                        // Check if it's a daily todo by category
                        boolean isDailyCategory = t.getCategory() != null &&
                                (t.getCategory().equalsIgnoreCase("Daily todo") ||
                                        t.getCategory().toLowerCase().contains("daily"));

                        // Check if due date is today or earlier (pending tasks)
                        boolean isDueTodayOrEarlier = false;
                        if (t.getDueDate() != null) {
                            LocalDate dueDate = DateUtils.parseDate(t.getDueDate());
                            if (dueDate != null) {
                                isDueTodayOrEarlier = !dueDate.isAfter(LocalDate.now());
                            }
                        }

                        // Don't show completed tasks
                        boolean isNotCompleted = !"COMPLETED".equalsIgnoreCase(t.getStatus());

                        return isNotCompleted && (isDailyCategory || isDueTodayOrEarlier);
                    })
                    .collect(Collectors.toList());
        }

        // Filter by status for other intents
        else {
            switch (context.getIntent()) {
                case TASK_PENDING:
                case TASK_OVERDUE:
                    result = result.stream()
                            .filter(t -> "PENDING".equalsIgnoreCase(t.getStatus()))
                            .collect(Collectors.toList());
                    break;
                case TASK_COMPLETED:
                    result = result.stream()
                            .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                            .collect(Collectors.toList());
                    break;
                case TASK_IN_PROGRESS:
                    result = result.stream()
                            .filter(t -> "IN-PROGRESS".equalsIgnoreCase(t.getStatus()) ||
                                    "IN_PROGRESS".equalsIgnoreCase(t.getStatus()))
                            .collect(Collectors.toList());
                    break;
            }
        }

        // Filter by project
        if (context.getTargetProject() != null) {
            result = result.stream()
                    .filter(t -> t.getProjectName() != null &&
                            t.getProjectName().toLowerCase().contains(context.getTargetProject().toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Filter by category
        if (context.getTargetCategory() != null) {
            result = result.stream()
                    .filter(t -> t.getCategory() != null &&
                            t.getCategory().toLowerCase().contains(context.getTargetCategory().toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Filter by due date range
        TimeRange timeRange = context.getTimeRange();
        if (timeRange != null && timeRange.getStartDate() != null && context.getIntent() != Intent.TASK_DAILY) {
            result = result.stream()
                    .filter(t -> {
                        if (t.getDueDate() == null) return false;
                        LocalDate dueDate = DateUtils.parseDate(t.getDueDate());
                        return dueDate != null && timeRange.contains(dueDate);
                    })
                    .collect(Collectors.toList());
        }

        // Sort by due date (soonest first)
        result.sort((t1, t2) -> {
            LocalDate d1 = DateUtils.parseDate(t1.getDueDate());
            LocalDate d2 = DateUtils.parseDate(t2.getDueDate());
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d1.compareTo(d2);
        });

        return result;
    }

    private String formatDailyTasks(List<TaskDetails> tasks) {
        if (tasks.isEmpty()) {
            return "📝 **No daily tasks for today!**\n\nYou're all caught up! 🎉\n\n" +
                    "If you have pending tasks, make sure they're marked as 'Daily todo' category " +
                    "or have a due date set for today.";
        }

        StringBuilder response = new StringBuilder();
        response.append("📝 **Your Daily Tasks for Today**\n\n");

        // Separate into categories
        List<TaskDetails> dailyCategoryTasks = tasks.stream()
                .filter(t -> t.getCategory() != null &&
                        (t.getCategory().equalsIgnoreCase("Daily todo") ||
                                t.getCategory().toLowerCase().contains("daily")))
                .collect(Collectors.toList());

        List<TaskDetails> dueTodayTasks = tasks.stream()
                .filter(t -> t.getDueDate() != null &&
                        DateUtils.parseDate(t.getDueDate()) != null &&
                        DateUtils.parseDate(t.getDueDate()).equals(LocalDate.now()))
                .filter(t -> !dailyCategoryTasks.contains(t))
                .collect(Collectors.toList());

        if (!dailyCategoryTasks.isEmpty()) {
            response.append("**Daily Tasks**\n");
            for (TaskDetails task : dailyCategoryTasks) {
                response.append(formatTaskItem(task));
            }
            response.append("\n");
        }

        if (!dueTodayTasks.isEmpty()) {
            response.append("**Tasks Due Today**\n");
            for (TaskDetails task : dueTodayTasks) {
                response.append(formatTaskItem(task));
            }
        }

        return response.toString();
    }

    private String formatTaskItem(TaskDetails task) {
        StringBuilder sb = new StringBuilder();
        sb.append("• **").append(task.getTitle()).append("**\n");

        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            sb.append("  📄 ").append(task.getDescription()).append("\n");
        }

        if (task.getDueDate() != null) {
            LocalDate dueDate = DateUtils.parseDate(task.getDueDate());
            LocalDate today = LocalDate.now();
            if (dueDate != null) {
                if (dueDate.isBefore(today)) {
                    sb.append("  ⚠️ **OVERDUE** (Was due: ").append(DateUtils.formatDate(task.getDueDate())).append(")\n");
                } else if (dueDate.equals(today)) {
                    sb.append("  🔴 **DUE TODAY**\n");
                } else {
                    sb.append("  📅 Due: ").append(DateUtils.formatDate(task.getDueDate())).append("\n");
                }
            }
        }

        if (task.getStatus() != null && !"PENDING".equalsIgnoreCase(task.getStatus())) {
            String statusIcon = getStatusIcon(task.getStatus());
            sb.append("  ").append(statusIcon).append(" Status: ").append(task.getStatus()).append("\n");
        }

        if (task.getProjectName() != null && !task.getProjectName().isEmpty()) {
            sb.append("  📁 Project: ").append(task.getProjectName()).append("\n");
        }

        if (task.getAssigneeMembers() != null && !task.getAssigneeMembers().isEmpty()) {
            sb.append("  👥 Assigned to: ");
            sb.append(task.getAssigneeMembers().stream()
                    .map(m -> m.getName())
                    .collect(Collectors.joining(", ")));
            sb.append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    private String formatPendingTasks(List<TaskDetails> tasks) {
        if (tasks.isEmpty()) {
            return "No pending tasks. Everything is complete! 🎯";
        }

        return ResponseFormatter.formatTaskList(tasks, "⏳ Pending Tasks");
    }

    private String formatCompletedTasks(List<TaskDetails> tasks) {
        if (tasks.isEmpty()) {
            return "No completed tasks yet. Start working on your tasks! 💪";
        }

        return ResponseFormatter.formatTaskList(tasks, "✅ Completed Tasks");
    }

    private String formatInProgressTasks(List<TaskDetails> tasks) {
        if (tasks.isEmpty()) {
            return "No tasks currently in progress.";
        }

        return ResponseFormatter.formatTaskList(tasks, "🔄 Tasks In Progress");
    }

    private String formatOverdueTasks(List<TaskDetails> tasks) {
        LocalDate today = LocalDate.now();
        List<TaskDetails> overdueTasks = tasks.stream()
                .filter(t -> {
                    LocalDate dueDate = DateUtils.parseDate(t.getDueDate());
                    return dueDate != null && dueDate.isBefore(today) &&
                            !"COMPLETED".equalsIgnoreCase(t.getStatus());
                })
                .collect(Collectors.toList());

        if (overdueTasks.isEmpty()) {
            return "No overdue tasks! Great job staying on top of things! 🌟";
        }

        StringBuilder response = new StringBuilder();
        response.append("⚠️ **OVERDUE TASKS**\n");
        response.append("These tasks need your immediate attention!\n\n");

        for (TaskDetails task : overdueTasks) {
            response.append("• **").append(task.getTitle()).append("**\n");
            response.append("  Due: ").append(DateUtils.formatDate(task.getDueDate())).append(" (OVERDUE)\n");
            if (task.getProjectName() != null) {
                response.append("  Project: ").append(task.getProjectName()).append("\n");
            }
            response.append("\n");
        }

        return response.toString();
    }

    private String formatTasksByProject(List<TaskDetails> tasks, QueryContext context) {
        if (tasks.isEmpty()) {
            return "No tasks found for project: " + context.getTargetProject();
        }

        Map<String, List<TaskDetails>> tasksByProject = tasks.stream()
                .filter(t -> t.getProjectName() != null)
                .collect(Collectors.groupingBy(TaskDetails::getProjectName));

        StringBuilder response = new StringBuilder();
        response.append("📊 **Tasks by Project**\n\n");

        for (Map.Entry<String, List<TaskDetails>> entry : tasksByProject.entrySet()) {
            response.append("**").append(entry.getKey()).append("**\n");
            for (TaskDetails task : entry.getValue()) {
                response.append("  • ").append(task.getTitle());
                if (task.getDueDate() != null) {
                    response.append(" (Due: ").append(DateUtils.formatDate(task.getDueDate())).append(")");
                }
                response.append(" - ").append(task.getStatus()).append("\n");
            }
            response.append("\n");
        }

        return response.toString();
    }

    private String formatTasksByCategory(List<TaskDetails> tasks, QueryContext context) {
        if (tasks.isEmpty()) {
            return "No tasks found for category: " + context.getTargetCategory();
        }

        return ResponseFormatter.formatTaskList(tasks, "📁 Tasks in " + context.getTargetCategory());
    }

    private String formatTaskList(List<TaskDetails> tasks) {
        return ResponseFormatter.formatTaskList(tasks, "📋 Tasks");
    }

    private String getNoTasksMessage(QueryContext context) {
        switch (context.getIntent()) {
            case TASK_DAILY:
                return "📝 **No daily tasks for today!** 🎉\n\n" +
                        "You're all caught up! To add daily tasks:\n" +
                        "• Use the 'Add Todo' form with category 'Daily todo'\n" +
                        "• Or set a due date for today on your tasks";
            case TASK_PENDING:
                return "No pending tasks. You're all caught up! 🎉";
            case TASK_COMPLETED:
                return "No completed tasks yet. Start working! 💪";
            default:
                return "No tasks found.";
        }
    }

    private String getStatusIcon(String status) {
        if (status == null) return "❓";
        switch (status.toUpperCase()) {
            case "COMPLETED": return "✅";
            case "IN-PROGRESS":
            case "IN_PROGRESS": return "🔄";
            case "PENDING": return "⏳";
            default: return "📋";
        }
    }
}