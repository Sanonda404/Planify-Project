package com.planify.frontend.utils.services;

import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.utils.managers.CalendarDataManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class PlanifyBotService {

    public static String ask(String query) {
        String input = query.toLowerCase();
        List<EventGetRequest> allEvents = CalendarDataManager.getMergedEvents();

        // 1. Determine Timeframe
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start;

        if (input.contains("today")) {
            end = start.withHour(23).withMinute(59);
        } else if (input.contains("tomorrow")) {
            start = start.plusDays(1).withHour(0).withMinute(0);
            end = start.withHour(23).withMinute(59);
        } else if (input.contains("next week") || input.contains("this week")) {
            end = start.plusDays(7);
        } else {
            return "I'm not sure which timeframe you mean. Try 'today', 'tomorrow', or 'next week'.";
        }

        // 2. Determine Type (Deadline vs Slot)
        String filterType = "";
        if (input.contains("deadline")) filterType = "DEADLINE";
        else if (input.contains("meeting") || input.contains("slot") || input.contains("event")) filterType = "SLOT";

        // 3. Filter the List
        final LocalDateTime finalStart = start;
        final LocalDateTime finalEnd = end;
        final String type = filterType;

        List<EventGetRequest> results = allEvents.stream()
                .filter(e -> {
                    LocalDateTime eventTime = LocalDateTime.parse(e.getStartDateTime());
                    boolean inTime = (eventTime.isAfter(finalStart) || eventTime.isEqual(finalStart)) && eventTime.isBefore(finalEnd);
                    boolean matchesType = type.isEmpty() || e.getType().equalsIgnoreCase(type);
                    return inTime && matchesType;
                })
                .collect(Collectors.toList());

        return formatResponse(results, query);
    }

    private static String formatResponse(List<EventGetRequest> events, String query) {
        if (events.isEmpty()) return "You have no matches for that query. You're free!";

        StringBuilder sb = new StringBuilder("Here is what I found:\n");
        for (EventGetRequest e : events) {
            sb.append("• [").append(e.getType()).append("] ")
                    .append(e.getTitle()).append(" at ")
                    .append(e.getStartDateTime().replace("T", " "))
                    .append("\n");
        }
        return sb.toString();
    }
}