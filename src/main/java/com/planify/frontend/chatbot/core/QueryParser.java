// core/QueryParser.java
package com.planify.frontend.chatbot.core;

import com.planify.frontend.chatbot.models.QueryContext;
import com.planify.frontend.chatbot.models.TimeRange;
import com.planify.frontend.chatbot.utils.DateUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;

public class QueryParser {

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}))?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, String> relativeDateMap = new HashMap<>();

    static {
        relativeDateMap.put("today", "today");
        relativeDateMap.put("tomorrow", "tomorrow");
        relativeDateMap.put("yesterday", "yesterday");
        relativeDateMap.put("this week", "this_week");
        relativeDateMap.put("next week", "next_week");
        relativeDateMap.put("this month", "this_month");
        relativeDateMap.put("next month", "next_month");
    }

    public QueryContext parse(String query, QueryContext context) {
        context.setOriginalQuery(query);

        // Extract time range
        extractTimeRange(query, context);

        // Extract project name
        extractProjectName(query, context);

        // Extract group name
        extractGroupName(query, context);

        // Extract category
        extractCategory(query, context);

        // Extract specific time
        extractSpecificTime(query, context);

        return context;
    }

    private void extractTimeRange(String query, QueryContext context) {
        String lowerQuery = query.toLowerCase();
        TimeRange timeRange = context.getTimeRange();

        // Check for specific date patterns
        java.util.regex.Matcher dateMatcher = DATE_PATTERN.matcher(query);
        if (dateMatcher.find()) {
            int month = Integer.parseInt(dateMatcher.group(1));
            int day = Integer.parseInt(dateMatcher.group(2));
            int year = dateMatcher.group(3) != null ? Integer.parseInt(dateMatcher.group(3)) : LocalDate.now().getYear();
            LocalDate specificDate = LocalDate.of(year, month, day);
            timeRange.setStartDate(specificDate);
            timeRange.setEndDate(specificDate);
            context.setReferenceDate(specificDate);
            return;
        }

        // Check for relative dates
        for (Map.Entry<String, String> entry : relativeDateMap.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                timeRange.setRelativeRange(entry.getValue());
                LocalDate now = LocalDate.now();
                switch (entry.getValue()) {
                    case "today":
                        timeRange.setStartDate(now);
                        timeRange.setEndDate(now);
                        context.setReferenceDate(now);
                        break;
                    case "tomorrow":
                        LocalDate tomorrow = now.plusDays(1);
                        timeRange.setStartDate(tomorrow);
                        timeRange.setEndDate(tomorrow);
                        context.setReferenceDate(tomorrow);
                        break;
                    case "yesterday":
                        LocalDate yesterday = now.minusDays(1);
                        timeRange.setStartDate(yesterday);
                        timeRange.setEndDate(yesterday);
                        context.setReferenceDate(yesterday);
                        break;
                    case "this_week":
                        timeRange.setStartDate(DateUtils.getStartOfWeek(now));
                        timeRange.setEndDate(DateUtils.getEndOfWeek(now));
                        break;
                    case "next_week":
                        LocalDate nextWeek = now.plusWeeks(1);
                        timeRange.setStartDate(DateUtils.getStartOfWeek(nextWeek));
                        timeRange.setEndDate(DateUtils.getEndOfWeek(nextWeek));
                        break;
                    case "this_month":
                        timeRange.setStartDate(now.withDayOfMonth(1));
                        timeRange.setEndDate(now.withDayOfMonth(now.lengthOfMonth()));
                        break;
                }
                return;
            }
        }

        // Default to today if no time range specified
        if (timeRange.getStartDate() == null) {
            timeRange.setStartDate(LocalDate.now());
            timeRange.setEndDate(LocalDate.now());
            timeRange.setRelativeRange("today");
        }
    }

    private void extractProjectName(String query, QueryContext context) {
        Pattern projectPattern = Pattern.compile(
                "(?:project|for)\\s+(?:['\"])?([A-Za-z0-9\\s]+)(?:['\"])?(?:\\s|$)",
                Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = projectPattern.matcher(query);
        if (matcher.find()) {
            context.setTargetProject(matcher.group(1).trim());
        }
    }

    private void extractGroupName(String query, QueryContext context) {
        Pattern groupPattern = Pattern.compile(
                "(?:group|team)\\s+(?:['\"])?([A-Za-z0-9\\s]+)(?:['\"])?(?:\\s|$)",
                Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = groupPattern.matcher(query);
        if (matcher.find()) {
            context.setTargetGroup(matcher.group(1).trim());
        }
    }

    private void extractCategory(String query, QueryContext context) {
        Pattern categoryPattern = Pattern.compile(
                "(?:category|type)\\s+(?:['\"])?([A-Za-z0-9\\s]+)(?:['\"])?(?:\\s|$)",
                Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = categoryPattern.matcher(query);
        if (matcher.find()) {
            context.setTargetCategory(matcher.group(1).trim());
        }
    }

    private void extractSpecificTime(String query, QueryContext context) {
        java.util.regex.Matcher matcher = TIME_PATTERN.matcher(query);
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            String ampm = matcher.group(3);

            if (ampm != null) {
                if (ampm.equalsIgnoreCase("pm") && hour < 12) hour += 12;
                if (ampm.equalsIgnoreCase("am") && hour == 12) hour = 0;
            }

            LocalTime time = LocalTime.of(hour, minute);
            LocalDateTime dateTime = LocalDateTime.of(context.getReferenceDate(), time);
            context.setReferenceDateTime(dateTime);
            context.addEntity("specific_time", dateTime);
        }
    }
}