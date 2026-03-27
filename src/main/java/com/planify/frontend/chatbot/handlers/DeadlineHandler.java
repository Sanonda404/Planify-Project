// handlers/DeadlineHandler.java
package com.planify.frontend.chatbot.handlers;

import com.planify.frontend.chatbot.models.QueryContext;
import com.planify.frontend.chatbot.models.TimeRange;
import com.planify.frontend.chatbot.utils.DateUtils;
import com.planify.frontend.chatbot.utils.ResponseFormatter;
import com.planify.frontend.models.events.EventGetRequest;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DeadlineHandler {

    private final List<EventGetRequest> events;

    public DeadlineHandler(List<EventGetRequest> events) {
        this.events = events;
    }

    public String handle(QueryContext context) {
        List<EventGetRequest> deadlines = getDeadlines();

        if (deadlines.isEmpty()) {
            return "No deadlines found in your schedule.";
        }

        // Filter by time range
        deadlines = filterByTimeRange(deadlines, context.getTimeRange());

        if (deadlines.isEmpty()) {
            return getNoDeadlinesMessage(context);
        }

        // Sort by date
        deadlines.sort(Comparator.comparing(e -> e.getStartDateTime()));

        switch (context.getIntent()) {
            case DEADLINE_COUNT:
                return ResponseFormatter.formatDeadlineCount(deadlines, context.getTimeRange());
            case DEADLINE_LIST:
            case DEADLINE_THIS_WEEK:
                return ResponseFormatter.formatDeadlineList(deadlines, context.getTimeRange());
            case DEADLINE_TODAY:
                return ResponseFormatter.formatTodayDeadlines(deadlines);
            case DEADLINE_TOMORROW:
                return ResponseFormatter.formatTomorrowDeadlines(deadlines);
            case DEADLINE_UPCOMING:
                return ResponseFormatter.formatUpcomingDeadlines(deadlines);
            case ASSIGNMENT_LIST:
                return formatAssignments(deadlines);
            case CLASS_TEST_LIST:
                return formatClassTests(deadlines);
            default:
                return ResponseFormatter.formatDeadlineList(deadlines, context.getTimeRange());
        }
    }

    private List<EventGetRequest> getDeadlines() {
        return events.stream()
                .filter(e -> "Deadline".equalsIgnoreCase(e.getType()))
                .collect(Collectors.toList());
    }

    private List<EventGetRequest> filterByTimeRange(List<EventGetRequest> deadlines, TimeRange timeRange) {
        if (timeRange.getStartDate() == null) {
            return deadlines;
        }

        return deadlines.stream()
                .filter(e -> {
                    LocalDate eventDate = DateUtils.parseDate(e.getStartDateTime());
                    return eventDate != null && timeRange.contains(eventDate);
                })
                .collect(Collectors.toList());
    }

    private String formatAssignments(List<EventGetRequest> deadlines) {
        List<EventGetRequest> assignments = deadlines.stream()
                .filter(e -> e.getTitle().toLowerCase().contains("assignment") ||
                        (e.getDescription() != null && e.getDescription().toLowerCase().contains("assignment")))
                .collect(Collectors.toList());

        if (assignments.isEmpty()) {
            return "No assignments found.";
        }

        StringBuilder response = new StringBuilder();
        response.append("📚 **Assignments**\n\n");

        for (EventGetRequest assignment : assignments) {
            response.append("• **").append(assignment.getTitle()).append("**\n");
            response.append("  Due: ").append(DateUtils.formatDate(assignment.getStartDateTime())).append("\n");
            if (assignment.getDescription() != null && !assignment.getDescription().isEmpty()) {
                response.append("  Description: ").append(assignment.getDescription()).append("\n");
            }
            response.append("\n");
        }

        return response.toString();
    }

    private String formatClassTests(List<EventGetRequest> deadlines) {
        List<EventGetRequest> tests = deadlines.stream()
                .filter(e -> e.getTitle().toLowerCase().contains("test") ||
                        e.getTitle().toLowerCase().contains("ct") ||
                        e.getTitle().toLowerCase().contains("exam") ||
                        (e.getDescription() != null && e.getDescription().toLowerCase().contains("test")))
                .collect(Collectors.toList());

        if (tests.isEmpty()) {
            return "No upcoming tests or exams found.";
        }

        StringBuilder response = new StringBuilder();
        response.append("📝 **Upcoming Tests & Exams**\n\n");

        for (EventGetRequest test : tests) {
            response.append("• **").append(test.getTitle()).append("**\n");
            response.append("  Date: ").append(DateUtils.formatDateTime(test.getStartDateTime())).append("\n");
            if (test.getGroup() != null) {
                response.append("  Group: ").append(test.getGroup().getName()).append("\n");
            }
            response.append("\n");
        }

        return response.toString();
    }

    private String getNoDeadlinesMessage(QueryContext context) {
        TimeRange timeRange = context.getTimeRange();
        if (timeRange.getRelativeRange() != null) {
            switch (timeRange.getRelativeRange()) {
                case "today":
                    return "No deadlines for today! 🎉 You're all caught up!";
                case "tomorrow":
                    return "No deadlines for tomorrow. You can relax! 😊";
                case "this_week":
                    return "No deadlines this week. Great planning! 🌟";
                default:
                    return "No deadlines in the specified time period.";
            }
        }
        return "No deadlines found.";
    }
}