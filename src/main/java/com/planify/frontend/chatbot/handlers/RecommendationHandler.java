// handlers/RecommendationHandler.java
package com.planify.frontend.chatbot.handlers;

import com.planify.frontend.chatbot.models.PriorityTask;
import com.planify.frontend.chatbot.models.QueryContext;
import com.planify.frontend.chatbot.utils.DateUtils;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.tasks.TaskDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationHandler {

    private final List<EventGetRequest> events;
    private final List<TaskDetails> tasks;

    public RecommendationHandler(List<EventGetRequest> events, List<TaskDetails> tasks) {
        this.events = events;
        this.tasks = tasks;
    }

    public String handle(QueryContext context) {
        switch (context.getIntent()) {
            case FREE_TIME:
                return findFreeTime();
            case RECOMMENDATION:
                return getSmartRecommendations();
            default:
                return getSmartRecommendations();
        }
    }

    private String findFreeTime() {
        LocalDate today = LocalDate.now();
        List<EventGetRequest> todayEvents = events.stream()
                .filter(e -> {
                    LocalDate eventDate = DateUtils.parseDate(e.getStartDateTime());
                    return eventDate != null && eventDate.equals(today);
                })
                .filter(e -> "Slot".equalsIgnoreCase(e.getType()))
                .sorted(Comparator.comparing(e -> e.getStartDateTime()))
                .collect(Collectors.toList());

        StringBuilder response = new StringBuilder();
        response.append("⏰ **Free Time Suggestions for Today**\n\n");

        if (todayEvents.isEmpty()) {
            response.append("You have no scheduled events today! ");
            response.append("Here are some suggestions:\n");
            response.append("• Morning (9 AM - 12 PM): Great time for deep work\n");
            response.append("• Afternoon (2 PM - 5 PM): Good for meetings or focused tasks\n");
            response.append("• Evening (7 PM - 9 PM): Perfect for personal tasks or relaxation\n");
            return response.toString();
        }

        // Find gaps between events
        List<FreeSlot> freeSlots = new ArrayList<>();
        LocalTime workStart = LocalTime.of(8, 0);
        LocalTime workEnd = LocalTime.of(22, 0);

        LocalDateTime current = LocalDateTime.of(today, workStart);

        for (EventGetRequest event : todayEvents) {
            LocalDateTime eventStart = DateUtils.parseDateTime(event.getStartDateTime());
            if (eventStart == null) continue;

            if (current.isBefore(eventStart)) {
                freeSlots.add(new FreeSlot(current, eventStart));
            }

            LocalDateTime eventEnd = DateUtils.parseDateTime(event.getEndDateTime());
            if (eventEnd != null) {
                current = eventEnd;
            } else {
                current = eventStart.plusHours(1);
            }
        }

        LocalDateTime endOfDay = LocalDateTime.of(today, workEnd);
        if (current.isBefore(endOfDay)) {
            freeSlots.add(new FreeSlot(current, endOfDay));
        }

        if (freeSlots.isEmpty()) {
            response.append("Your schedule is packed today! Consider:\n");
            response.append("• Early morning before your first event\n");
            response.append("• Late evening after your last event\n");
            response.append("• Short breaks between events\n");
            return response.toString();
        }

        response.append("**Available Time Slots**\n\n");
        for (FreeSlot slot : freeSlots) {
            long durationMinutes = slot.getDurationMinutes();
            if (durationMinutes >= 30) {
                response.append("• ").append(DateUtils.formatTime(slot.start));
                response.append(" - ").append(DateUtils.formatTime(slot.end));
                response.append(" (").append(formatDuration(durationMinutes)).append(")\n");

                // Suggest activity based on duration
                if (durationMinutes >= 120) {
                    response.append("  💡 Great for: Deep work, project milestones, studying\n");
                } else if (durationMinutes >= 60) {
                    response.append("  💡 Good for: Meetings, focused tasks, exercise\n");
                } else if (durationMinutes >= 30) {
                    response.append("  💡 Ideal for: Quick tasks, emails, planning\n");
                }
                response.append("\n");
            }
        }

        return response.toString();
    }

    private String getSmartRecommendations() {
        List<PriorityTask> priorityTasks = getPriorityTasks();

        StringBuilder response = new StringBuilder();
        response.append("🤖 **Smart Recommendations**\n\n");

        // 1. Time management suggestion
        response.append("**Time Management**\n");
        List<EventGetRequest> upcomingEvents = events.stream()
                .filter(e -> {
                    LocalDateTime eventTime = DateUtils.parseDateTime(e.getStartDateTime());
                    return eventTime != null && eventTime.isAfter(LocalDateTime.now());
                })
                .sorted(Comparator.comparing(e -> e.getStartDateTime()))
                .limit(3)
                .collect(Collectors.toList());

        if (!upcomingEvents.isEmpty()) {
            response.append("• Your next event is ");
            response.append(upcomingEvents.get(0).getTitle());
            response.append(" at ");
            response.append(DateUtils.formatTime(upcomingEvents.get(0).getStartDateTime()));
            response.append("\n");
        }
        response.append("\n");

        // 2. Task prioritization
        response.append("**Task Prioritization**\n");
        List<PriorityTask> urgentTasks = priorityTasks.stream()
                .filter(t -> t.getTotalPriority() >= 70)
                .limit(3)
                .collect(Collectors.toList());

        if (!urgentTasks.isEmpty()) {
            response.append("• Focus on these urgent tasks:\n");
            for (PriorityTask task : urgentTasks) {
                response.append("  - ").append(task.getTitle());
                if (task.getDueDate() != null) {
                    if (task.getDueDate().isBefore(LocalDate.now())) {
                        response.append(" (OVERDUE!)");
                    } else if (task.getDueDate().equals(LocalDate.now())) {
                        response.append(" (DUE TODAY!)");
                    }
                }
                response.append("\n");
            }
        } else {
            response.append("• No urgent tasks. Great time to get ahead on upcoming work!\n");
        }
        response.append("\n");

        // 3. Work-life balance
        response.append("**Work-Life Balance**\n");
        long completedToday = tasks.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .filter(t -> t.getDueDate() != null)
                .filter(t -> {
                    LocalDate dueDate = DateUtils.parseDate(t.getDueDate());
                    return dueDate != null && dueDate.equals(LocalDate.now());
                })
                .count();

        if (completedToday > 3) {
            response.append("• You've completed ").append(completedToday);
            response.append(" tasks today! Take a well-deserved break.\n");
        } else if (completedToday == 0 && !priorityTasks.isEmpty()) {
            response.append("• Start with one small task to build momentum.\n");
        } else {
            response.append("• You're making good progress. Remember to take short breaks!\n");
        }
        response.append("\n");

        // 4. Productivity tip
        response.append("**Productivity Tip**\n");
        response.append(getRandomProductivityTip());

        return response.toString();
    }

    private List<PriorityTask> getPriorityTasks() {
        List<PriorityTask> priorityTasks = new ArrayList<>();

        tasks.stream()
                .filter(t -> !"COMPLETED".equalsIgnoreCase(t.getStatus()))
                .forEach(t -> priorityTasks.add(new PriorityTask(t)));

        events.stream()
                .filter(e -> "Deadline".equalsIgnoreCase(e.getType()))
                .forEach(e -> priorityTasks.add(new PriorityTask(e)));

        return priorityTasks;
    }

    private String formatDuration(long minutes) {
        if (minutes >= 60) {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            if (remainingMinutes > 0) {
                return hours + "h " + remainingMinutes + "m";
            }
            return hours + " hours";
        }
        return minutes + " minutes";
    }

    private String getRandomProductivityTip() {
        String[] tips = {
                "Break large tasks into smaller, manageable chunks to avoid overwhelm.",
                "Use the Pomodoro Technique: 25 minutes focused work, 5 minutes break.",
                "Start with your most challenging task when your energy is highest.",
                "Schedule buffer time between events to avoid burnout.",
                "Review your goals each morning to stay focused on what matters.",
                "Batch similar tasks together to maintain flow state.",
                "Limit distractions by putting your phone on silent during focus sessions.",
                "Celebrate small wins to maintain motivation.",
                "Take regular breaks to maintain mental clarity.",
                "End your day by planning tomorrow's priorities."
        };

        Random random = new Random();
        return tips[random.nextInt(tips.length)];
    }

    private static class FreeSlot {
        LocalDateTime start;
        LocalDateTime end;

        FreeSlot(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }

        long getDurationMinutes() {
            return java.time.Duration.between(start, end).toMinutes();
        }
    }
}