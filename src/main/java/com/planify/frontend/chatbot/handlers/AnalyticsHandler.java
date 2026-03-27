// handlers/AnalyticsHandler.java
package com.planify.frontend.chatbot.handlers;

import com.planify.frontend.chatbot.models.QueryContext;
import com.planify.frontend.chatbot.utils.DateUtils;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsHandler {

    private final List<TaskDetails> tasks;
    private final List<ProjectDetails> projects;
    private final List<EventGetRequest> events;

    public AnalyticsHandler(List<TaskDetails> tasks, List<ProjectDetails> projects, List<EventGetRequest> events) {
        this.tasks = tasks;
        this.projects = projects;
        this.events = events;
    }

    public String handle(QueryContext context) {
        switch (context.getIntent()) {
            case PRODUCTIVITY_ANALYSIS:
                return getProductivityAnalysis();
            case WEEKLY_SUMMARY:
                return getWeeklySummary();
            case TRENDS:
                return getTrends();
            default:
                return getProductivityAnalysis();
        }
    }

    private String getProductivityAnalysis() {
        int totalTasks = tasks.size();
        int completedTasks = (int) tasks.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .count();
        int inProgressTasks = (int) tasks.stream()
                .filter(t -> "IN-PROGRESS".equalsIgnoreCase(t.getStatus()) ||
                        "IN_PROGRESS".equalsIgnoreCase(t.getStatus()))
                .count();
        int pendingTasks = totalTasks - completedTasks - inProgressTasks;

        double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;

        // Calculate tasks completed in last 7 days
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        long recentCompletions = tasks.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .filter(t -> t.getDueDate() != null &&
                        DateUtils.parseDate(t.getDueDate()) != null &&
                        !DateUtils.parseDate(t.getDueDate()).isBefore(weekAgo))
                .count();

        StringBuilder response = new StringBuilder();
        response.append("📊 **Productivity Analysis**\n\n");

        response.append("**Task Completion**\n");
        response.append("• Total Tasks: ").append(totalTasks).append("\n");
        response.append("• Completed: ").append(completedTasks).append(" ✅\n");
        response.append("• In Progress: ").append(inProgressTasks).append(" 🔄\n");
        response.append("• Pending: ").append(pendingTasks).append(" ⏳\n");
        response.append("• Completion Rate: ").append(String.format("%.1f", completionRate)).append("%\n\n");

        response.append("**Recent Activity (Last 7 Days)**\n");
        response.append("• Tasks Completed: ").append(recentCompletions).append("\n");

        // Calculate daily average
        double dailyAverage = recentCompletions / 7.0;
        response.append("• Daily Average: ").append(String.format("%.1f", dailyAverage)).append(" tasks/day\n\n");

        // Project progress
        if (!projects.isEmpty()) {
            double avgProjectProgress = projects.stream()
                    .mapToInt(ProjectDetails::getProgress)
                    .average()
                    .orElse(0);

            int completedProjects = (int) projects.stream()
                    .filter(ProjectDetails::isCompleted)
                    .count();

            response.append("**Project Progress**\n");
            response.append("• Active Projects: ").append(projects.size() - completedProjects).append("\n");
            response.append("• Completed Projects: ").append(completedProjects).append("\n");
            response.append("• Average Progress: ").append(String.format("%.1f", avgProjectProgress)).append("%\n\n");
        }

        // Productivity rating
        response.append("**Productivity Rating**\n");
        if (completionRate >= 80) {
            response.append("🌟 Excellent! You're crushing your goals! Keep up the great work!\n");
        } else if (completionRate >= 60) {
            response.append("👍 Good progress! You're on the right track.\n");
        } else if (completionRate >= 40) {
            response.append("📈 You're making steady progress. Try to tackle more tasks daily.\n");
        } else {
            response.append("💪 Time to pick up the pace! Start with small, achievable tasks.\n");
        }

        return response.toString();
    }

    private String getWeeklySummary() {
        LocalDate startOfWeek = DateUtils.getStartOfWeek(LocalDate.now());
        LocalDate endOfWeek = DateUtils.getEndOfWeek(LocalDate.now());

        // Tasks completed this week
        long tasksCompletedThisWeek = tasks.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .filter(t -> t.getDueDate() != null)
                .filter(t -> {
                    LocalDate dueDate = DateUtils.parseDate(t.getDueDate());
                    return dueDate != null && !dueDate.isBefore(startOfWeek) && !dueDate.isAfter(endOfWeek);
                })
                .count();

        // Deadlines this week
        long deadlinesThisWeek = events.stream()
                .filter(e -> "Deadline".equalsIgnoreCase(e.getType()))
                .filter(e -> {
                    LocalDate eventDate = DateUtils.parseDate(e.getStartDateTime());
                    return eventDate != null && !eventDate.isBefore(startOfWeek) && !eventDate.isAfter(endOfWeek);
                })
                .count();

        // New tasks created this week (approximation using due dates)
        long newTasksThisWeek = tasks.stream()
                .filter(t -> t.getDueDate() != null)
                .filter(t -> {
                    LocalDate dueDate = DateUtils.parseDate(t.getDueDate());
                    return dueDate != null && !dueDate.isBefore(startOfWeek) && !dueDate.isAfter(endOfWeek);
                })
                .count();

        StringBuilder response = new StringBuilder();
        response.append("📅 **Weekly Summary**\n");
        response.append("Week of ").append(startOfWeek).append(" - ").append(endOfWeek).append("\n");
        response.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        response.append("**This Week's Activity**\n");
        response.append("• Tasks Completed: ").append(tasksCompletedThisWeek).append("\n");
        response.append("• Tasks Created: ").append(newTasksThisWeek).append("\n");
        response.append("• Deadlines: ").append(deadlinesThisWeek).append("\n\n");

        // Upcoming next week
        LocalDate nextWeekStart = endOfWeek.plusDays(1);
        LocalDate nextWeekEnd = nextWeekStart.plusDays(6);

        long upcomingDeadlines = events.stream()
                .filter(e -> "Deadline".equalsIgnoreCase(e.getType()))
                .filter(e -> {
                    LocalDate eventDate = DateUtils.parseDate(e.getStartDateTime());
                    return eventDate != null && !eventDate.isBefore(nextWeekStart) && !eventDate.isAfter(nextWeekEnd);
                })
                .count();

        response.append("**Looking Ahead**\n");
        response.append("• Upcoming deadlines next week: ").append(upcomingDeadlines).append("\n\n");

        // Motivation message
        if (tasksCompletedThisWeek > 0) {
            response.append("🎉 Great job completing ").append(tasksCompletedThisWeek);
            response.append(" tasks this week! Keep the momentum going!\n");
        } else {
            response.append("💪 This is a fresh start! Set some small goals for the coming week.\n");
        }

        return response.toString();
    }

    private String getTrends() {
        // Calculate completion trends over last 4 weeks
        Map<Integer, Long> weeklyCompletions = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 3; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);

            long completions = tasks.stream()
                    .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                    .filter(t -> t.getDueDate() != null)
                    .filter(t -> {
                        LocalDate dueDate = DateUtils.parseDate(t.getDueDate());
                        return dueDate != null && !dueDate.isBefore(weekStart) && !dueDate.isAfter(weekEnd);
                    })
                    .count();

            weeklyCompletions.put(i, completions);
        }

        StringBuilder response = new StringBuilder();
        response.append("📈 **Productivity Trends**\n\n");

        response.append("**Weekly Task Completion**\n");
        String[] weekNames = {"3 Weeks Ago", "2 Weeks Ago", "Last Week", "This Week"};
        int index = 0;
        for (Map.Entry<Integer, Long> entry : weeklyCompletions.entrySet()) {
            response.append("• ").append(weekNames[index]).append(": ");
            response.append(entry.getValue()).append(" tasks\n");

            // Add mini bar chart
            int barLength = (int) Math.min(entry.getValue() / 2, 20);
            response.append("  ").append("█".repeat(barLength));
            if (entry.getValue() > 0) response.append(" ");
            response.append("\n");
            index++;
        }

        // Calculate trend
        List<Long> values = new ArrayList<>(weeklyCompletions.values());
        if (values.size() >= 2) {
            long lastWeek = values.get(values.size() - 2);
            long thisWeek = values.get(values.size() - 1);

            response.append("\n**Trend Analysis**\n");
            if (thisWeek > lastWeek) {
                long increase = thisWeek - lastWeek;
                response.append("📈 Upward trend! You completed ").append(increase);
                response.append(" more tasks than last week. Great improvement!\n");
            } else if (thisWeek < lastWeek) {
                long decrease = lastWeek - thisWeek;
                response.append("📉 Slight decrease this week (").append(decrease);
                response.append(" fewer tasks). Let's pick up the pace!\n");
            } else {
                response.append("➡️ Steady performance. Try to increase your output this week!\n");
            }
        }

        return response.toString();
    }
}