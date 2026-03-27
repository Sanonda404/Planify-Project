// utils/ResponseFormatter.java
package com.planify.frontend.chatbot.utils;

import com.planify.frontend.chatbot.models.TimeRange;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.tasks.TaskDetails;

import java.time.LocalDate;
import java.util.List;

public class ResponseFormatter {

    public static String formatDeadlineCount(List<EventGetRequest> deadlines, TimeRange timeRange) {
        if (deadlines.isEmpty()) {
            return "No deadlines " + getTimeRangeDescription(timeRange) + ".";
        }
        return "You have **" + deadlines.size() + "** deadline" +
                (deadlines.size() > 1 ? "s" : "") + " " + getTimeRangeDescription(timeRange) + ".";
    }

    public static String formatDeadlineList(List<EventGetRequest> deadlines, TimeRange timeRange) {
        if (deadlines.isEmpty()) {
            return "No deadlines " + getTimeRangeDescription(timeRange) + ".";
        }

        StringBuilder response = new StringBuilder();
        response.append("📅 **Deadlines ").append(getTimeRangeDescription(timeRange)).append("**\n\n");

        for (EventGetRequest deadline : deadlines) {
            response.append("• **").append(deadline.getTitle()).append("**\n");
            response.append("  Due: ").append(DateUtils.formatDateTime(deadline.getStartDateTime())).append("\n");
            if (deadline.getDescription() != null && !deadline.getDescription().isEmpty()) {
                response.append("  ").append(deadline.getDescription()).append("\n");
            }
            response.append("\n");
        }

        return response.toString();
    }

    public static String formatTodayDeadlines(List<EventGetRequest> deadlines) {
        if (deadlines.isEmpty()) {
            return "No deadlines for today! 🎉";
        }

        StringBuilder response = new StringBuilder();
        response.append("⚠️ **Deadlines for TODAY**\n\n");

        for (EventGetRequest deadline : deadlines) {
            response.append("• **").append(deadline.getTitle()).append("**\n");
            response.append("  Time: ").append(DateUtils.formatTime(deadline.getStartDateTime())).append("\n");
            response.append("\n");
        }

        return response.toString();
    }

    public static String formatTomorrowDeadlines(List<EventGetRequest> deadlines) {
        if (deadlines.isEmpty()) {
            return "No deadlines for tomorrow. You're ahead of schedule! 🌟";
        }

        StringBuilder response = new StringBuilder();
        response.append("📌 **Deadlines for TOMORROW**\n\n");

        for (EventGetRequest deadline : deadlines) {
            response.append("• **").append(deadline.getTitle()).append("**\n");
            response.append("  Time: ").append(DateUtils.formatTime(deadline.getStartDateTime())).append("\n");
            response.append("\n");
        }

        return response.toString();
    }

    public static String formatUpcomingDeadlines(List<EventGetRequest> deadlines) {
        if (deadlines.isEmpty()) {
            return "No upcoming deadlines.";
        }

        StringBuilder response = new StringBuilder();
        response.append("⏰ **Upcoming Deadlines**\n\n");

        LocalDate today = LocalDate.now();
        for (EventGetRequest deadline : deadlines) {
            LocalDate dueDate = DateUtils.parseDate(deadline.getStartDateTime());
            if (dueDate != null) {
                long daysUntil = today.until(dueDate).getDays();
                String urgency = "";
                if (daysUntil < 0) urgency = " (OVERDUE!)";
                else if (daysUntil == 0) urgency = " (TODAY!)";
                else if (daysUntil <= 3) urgency = " (Soon!)";

                response.append("• **").append(deadline.getTitle()).append("**");
                response.append(urgency).append("\n");
                response.append("  Due: ").append(DateUtils.formatDateTime(deadline.getStartDateTime())).append("\n");
                response.append("\n");
            }
        }

        return response.toString();
    }

    public static String formatTodayEvents(List<EventGetRequest> events) {
        if (events.isEmpty()) {
            return "No events scheduled for today. Enjoy your free time! 😊";
        }

        StringBuilder response = new StringBuilder();
        response.append("📋 **Today's Schedule**\n\n");

        for (EventGetRequest event : events) {
            response.append("• **[").append(event.getType()).append("] ").append(event.getTitle()).append("**\n");
            response.append("  Time: ").append(DateUtils.formatDateTime(event.getStartDateTime()));
            if (event.getEndDateTime() != null && !event.getEndDateTime().isEmpty()) {
                response.append(" - ").append(DateUtils.formatTime(event.getEndDateTime()));
            }
            response.append("\n");
            if (event.getGroup() != null) {
                response.append("  Group: ").append(event.getGroup().getName()).append("\n");
            }
            response.append("\n");
        }

        return response.toString();
    }

    public static String formatTomorrowEvents(List<EventGetRequest> events) {
        if (events.isEmpty()) {
            return "No events scheduled for tomorrow.";
        }

        StringBuilder response = new StringBuilder();
        response.append("📋 **Tomorrow's Schedule**\n\n");

        for (EventGetRequest event : events) {
            response.append("• **").append(event.getTitle()).append("**\n");
            response.append("  Time: ").append(DateUtils.formatTime(event.getStartDateTime()));
            if (event.getEndDateTime() != null && !event.getEndDateTime().isEmpty()) {
                response.append(" - ").append(DateUtils.formatTime(event.getEndDateTime()));
            }
            response.append("\n");
            response.append("\n");
        }

        return response.toString();
    }

    public static String formatWeekEvents(List<EventGetRequest> events, TimeRange timeRange) {
        if (events.isEmpty()) {
            return "No events scheduled this week.";
        }

        StringBuilder response = new StringBuilder();
        response.append("📅 **This Week's Schedule**\n");
        response.append(timeRange.getStartDate() + " - " + timeRange.getEndDate() + "\n\n");

        // Group events by day
        LocalDate currentDate = timeRange.getStartDate();
        while (!currentDate.isAfter(timeRange.getEndDate())) {
            final LocalDate date = currentDate;
            List<EventGetRequest> dayEvents = events.stream()
                    .filter(e -> {
                        LocalDate eventDate = DateUtils.parseDate(e.getStartDateTime());
                        return eventDate != null && eventDate.equals(date);
                    })
                    .toList();

            if (!dayEvents.isEmpty()) {
                response.append("**").append(date.getDayOfWeek()).append(" (").append(date).append(")**\n");
                for (EventGetRequest event : dayEvents) {
                    response.append("  • [").append(event.getType()).append("] ");
                    response.append(event.getTitle());
                    response.append(" - ").append(DateUtils.formatTime(event.getStartDateTime()));
                    response.append("\n");
                }
                response.append("\n");
            }

            currentDate = currentDate.plusDays(1);
        }

        return response.toString();
    }

    public static String formatEvents(List<EventGetRequest> events, TimeRange timeRange) {
        if (events.isEmpty()) {
            return "No events found.";
        }

        StringBuilder response = new StringBuilder();
        response.append("📅 **Events**\n\n");

        for (EventGetRequest event : events) {
            response.append("• **").append(event.getTitle()).append("**\n");
            response.append("  When: ").append(DateUtils.formatDateTime(event.getStartDateTime())).append("\n");
            response.append("  Type: ").append(event.getType()).append("\n");
            response.append("\n");
        }

        return response.toString();
    }

    public static String formatTaskList(List<TaskDetails> tasks, String title) {
        if (tasks.isEmpty()) {
            return title + ": None found.";
        }

        StringBuilder response = new StringBuilder();
        response.append(title).append("\n\n");

        for (TaskDetails task : tasks) {
            response.append("• **").append(task.getTitle()).append("**\n");
            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                response.append("  ").append(task.getDescription()).append("\n");
            }
            if (task.getDueDate() != null) {
                response.append("  Due: ").append(DateUtils.formatDate(task.getDueDate())).append("\n");
            }
            if (task.getStatus() != null) {
                String statusIcon = getStatusIcon(task.getStatus());
                response.append("  Status: ").append(statusIcon).append(" ").append(task.getStatus()).append("\n");
            }
            if (task.getProjectName() != null) {
                response.append("  Project: ").append(task.getProjectName()).append("\n");
            }
            response.append("\n");
        }

        return response.toString();
    }

    public static String createProgressBar(int percentage) {
        int barLength = 20;
        int filledLength = (int) Math.round(barLength * percentage / 100.0);

        StringBuilder bar = new StringBuilder("[");
        bar.append("█".repeat(filledLength));
        bar.append("░".repeat(barLength - filledLength));
        bar.append("]");

        return bar.toString();
    }

    private static String getStatusIcon(String status) {
        if (status == null) return "❓";
        switch (status.toUpperCase()) {
            case "COMPLETED": return "✅";
            case "IN-PROGRESS":
            case "IN_PROGRESS": return "🔄";
            case "PENDING": return "⏳";
            default: return "📋";
        }
    }

    private static String getTimeRangeDescription(TimeRange timeRange) {
        if (timeRange.getRelativeRange() != null) {
            switch (timeRange.getRelativeRange()) {
                case "today": return "today";
                case "tomorrow": return "tomorrow";
                case "this_week": return "this week";
                case "next_week": return "next week";
                default: return "in the specified period";
            }
        }
        return "in the specified period";
    }
}