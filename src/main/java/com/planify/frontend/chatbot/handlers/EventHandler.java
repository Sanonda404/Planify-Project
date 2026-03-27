// handlers/EventHandler.java
package com.planify.frontend.chatbot.handlers;

import com.planify.frontend.chatbot.models.QueryContext;
import com.planify.frontend.chatbot.models.TimeRange;
import com.planify.frontend.chatbot.utils.DateUtils;
import com.planify.frontend.chatbot.utils.ResponseFormatter;
import com.planify.frontend.models.events.EventGetRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class EventHandler {

    private final List<EventGetRequest> events;

    public EventHandler(List<EventGetRequest> events) {
        this.events = events;
    }

    public String handle(QueryContext context) {
        List<EventGetRequest> filteredEvents = filterEvents(context);

        if (filteredEvents.isEmpty()) {
            return getNoEventsMessage(context);
        }

        filteredEvents.sort(Comparator.comparing(e -> e.getStartDateTime()));

        switch (context.getIntent()) {
            case EVENT_TODAY:
                return ResponseFormatter.formatTodayEvents(filteredEvents);
            case EVENT_TOMORROW:
                return ResponseFormatter.formatTomorrowEvents(filteredEvents);
            case EVENT_THIS_WEEK:
                return ResponseFormatter.formatWeekEvents(filteredEvents, context.getTimeRange());
            case EVENT_SPECIFIC_TIME:
                return formatEventsAtTime(filteredEvents, context);
            case EVENT_SEARCH:
                return formatSearchedEvents(filteredEvents, context);
            default:
                return ResponseFormatter.formatEvents(filteredEvents, context.getTimeRange());
        }
    }

    private List<EventGetRequest> filterEvents(QueryContext context) {
        List<EventGetRequest> result = new ArrayList<>(events);

        // Filter by time range
        TimeRange timeRange = context.getTimeRange();
        if (timeRange.getStartDate() != null) {
            result = result.stream()
                    .filter(e -> {
                        LocalDate eventDate = DateUtils.parseDate(e.getStartDateTime());
                        return eventDate != null && timeRange.contains(eventDate);
                    })
                    .collect(Collectors.toList());
        }

        // Filter by specific time
        if (context.getReferenceDateTime() != null) {
            result = result.stream()
                    .filter(e -> {
                        LocalDateTime eventTime = DateUtils.parseDateTime(e.getStartDateTime());
                        return eventTime != null && isWithinHour(eventTime, context.getReferenceDateTime());
                    })
                    .collect(Collectors.toList());
        }

        // Filter by group
        if (context.getTargetGroup() != null) {
            result = result.stream()
                    .filter(e -> e.getGroup() != null &&
                            e.getGroup().getName().toLowerCase().contains(context.getTargetGroup().toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Filter by search keywords
        if (!context.getKeywords().isEmpty()) {
            result = result.stream()
                    .filter(e -> matchesKeywords(e, context.getKeywords()))
                    .collect(Collectors.toList());
        }

        return result;
    }

    private boolean isWithinHour(LocalDateTime eventTime, LocalDateTime targetTime) {
        if (eventTime == null || targetTime == null) return false;
        LocalDateTime start = targetTime.minusHours(1);
        LocalDateTime end = targetTime.plusHours(1);
        return !eventTime.isBefore(start) && !eventTime.isAfter(end);
    }

    private boolean matchesKeywords(EventGetRequest event, List<String> keywords) {
        String searchText = (event.getTitle() + " " +
                (event.getDescription() != null ? event.getDescription() : "")).toLowerCase();
        return keywords.stream().anyMatch(searchText::contains);
    }

    private String formatEventsAtTime(List<EventGetRequest> events, QueryContext context) {
        if (events.isEmpty()) {
            return "No events scheduled around " +
                    DateUtils.formatTime(context.getReferenceDateTime()) + ".";
        }

        StringBuilder response = new StringBuilder();
        response.append("🕐 **Events around ")
                .append(DateUtils.formatTime(context.getReferenceDateTime()))
                .append("**\n\n");

        for (EventGetRequest event : events) {
            response.append("• **").append(event.getTitle()).append("**\n");
            response.append("  Time: ").append(DateUtils.formatDateTime(event.getStartDateTime()));
            if (event.getEndDateTime() != null && !event.getEndDateTime().isEmpty()) {
                response.append(" - ").append(DateUtils.formatTime(event.getEndDateTime()));
            }
            response.append("\n");
            response.append("  Type: ").append(event.getType()).append("\n");
            response.append("\n");
        }

        return response.toString();
    }

    private String formatSearchedEvents(List<EventGetRequest> events, QueryContext context) {
        if (events.isEmpty()) {
            return "No events found matching your search.";
        }

        StringBuilder response = new StringBuilder();
        response.append("🔍 **Search Results**\n\n");

        for (EventGetRequest event : events) {
            response.append("• **").append(event.getTitle()).append("**\n");
            response.append("  When: ").append(DateUtils.formatDateTime(event.getStartDateTime())).append("\n");
            if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                response.append("  Description: ").append(event.getDescription()).append("\n");
            }
            response.append("\n");
        }

        return response.toString();
    }

    private String getNoEventsMessage(QueryContext context) {
        TimeRange timeRange = context.getTimeRange();
        if (timeRange.getRelativeRange() != null) {
            switch (timeRange.getRelativeRange()) {
                case "today":
                    return "No events scheduled for today. Enjoy your free time! 😊";
                case "tomorrow":
                    return "No events scheduled for tomorrow.";
                case "this_week":
                    return "No events scheduled this week.";
                default:
                    return "No events found in the specified time period.";
            }
        }
        return "No events found.";
    }
}