// core/IntentRecognizer.java
package com.planify.frontend.chatbot.core;

import com.planify.frontend.chatbot.models.Intent;
import com.planify.frontend.chatbot.models.QueryContext;

import java.util.*;
import java.util.regex.Pattern;

public class IntentRecognizer {

    private static final Map<Intent, List<Pattern>> intentPatterns = new HashMap<>();
    private static final Map<Intent, List<String>> intentKeywords = new HashMap<>();
    private static final Map<Intent, Integer> intentPriority = new HashMap<>();

    static {
        initializePatterns();
        initializeKeywords();
        initializePriority();
    }

    private static void initializePatterns() {
        // Task patterns - HIGHEST PRIORITY for task-related questions
        intentPatterns.put(Intent.TASK_DAILY, Arrays.asList(
                Pattern.compile("daily\\s*tasks?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("tasks?\\s*for\\s*today", Pattern.CASE_INSENSITIVE),
                Pattern.compile("what.*(?:daily|todo|to\\s*do).*today", Pattern.CASE_INSENSITIVE),
                Pattern.compile("today'?s\\s*tasks?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("my\\s*daily\\s*tasks?", Pattern.CASE_INSENSITIVE)
        ));

        intentPatterns.put(Intent.TASK_PENDING, Arrays.asList(
                Pattern.compile("pending\\s*tasks?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("remaining\\s*tasks?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("tasks?\\s*remaining", Pattern.CASE_INSENSITIVE),
                Pattern.compile("what.*(?:pending|remaining|left)", Pattern.CASE_INSENSITIVE)
        ));

        intentPatterns.put(Intent.TASK_COMPLETED, Arrays.asList(
                Pattern.compile("completed\\s*tasks?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("tasks?\\s*completed", Pattern.CASE_INSENSITIVE),
                Pattern.compile("what.*(?:done|finished|completed)", Pattern.CASE_INSENSITIVE)
        ));

        intentPatterns.put(Intent.TASK_IN_PROGRESS, Arrays.asList(
                Pattern.compile("in\\s*progress\\s*tasks?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("ongoing\\s*tasks?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("working\\s*on", Pattern.CASE_INSENSITIVE)
        ));

        intentPatterns.put(Intent.TASK_OVERDUE, Arrays.asList(
                Pattern.compile("overdue\\s*tasks?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("late\\s*tasks?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("past\\s*due", Pattern.CASE_INSENSITIVE)
        ));

        // Deadline patterns - LOWER priority than tasks
        intentPatterns.put(Intent.DEADLINE_TODAY, Arrays.asList(
                Pattern.compile("deadlines?\\s*today", Pattern.CASE_INSENSITIVE),
                Pattern.compile("today'?s\\s*deadlines?", Pattern.CASE_INSENSITIVE)
        ));

        intentPatterns.put(Intent.DEADLINE_THIS_WEEK, Arrays.asList(
                Pattern.compile("deadlines?\\s*this\\s*week", Pattern.CASE_INSENSITIVE),
                Pattern.compile("deadlines?\\s*for\\s*this\\s*week", Pattern.CASE_INSENSITIVE),
                Pattern.compile("how many deadlines?\\s*this\\s*week", Pattern.CASE_INSENSITIVE)
        ));
    }

    private static void initializeKeywords() {
        // Task keywords - HIGH PRIORITY
        intentKeywords.put(Intent.TASK_DAILY, Arrays.asList(
                "daily", "todo", "to do", "today's tasks", "tasks for today", "daily tasks"
        ));

        intentKeywords.put(Intent.TASK_PENDING, Arrays.asList(
                "pending", "remaining", "not done", "left", "unfinished", "incomplete"
        ));

        intentKeywords.put(Intent.TASK_COMPLETED, Arrays.asList(
                "completed", "done", "finished", "accomplished"
        ));

        intentKeywords.put(Intent.TASK_IN_PROGRESS, Arrays.asList(
                "in progress", "ongoing", "working on", "started"
        ));

        intentKeywords.put(Intent.TASK_OVERDUE, Arrays.asList(
                "overdue", "late", "past due", "delayed"
        ));

        intentKeywords.put(Intent.TASK_BY_PROJECT, Arrays.asList(
                "project tasks", "tasks in project", "tasks for project"
        ));

        // Deadline keywords - MEDIUM PRIORITY
        intentKeywords.put(Intent.DEADLINE_TODAY, Arrays.asList(
                "deadline today", "today's deadline", "due today"
        ));

        intentKeywords.put(Intent.DEADLINE_THIS_WEEK, Arrays.asList(
                "deadline this week", "deadlines this week", "due this week"
        ));

        intentKeywords.put(Intent.ASSIGNMENT_LIST, Arrays.asList(
                "assignment", "assignments", "homework", "coursework"
        ));

        intentKeywords.put(Intent.CLASS_TEST_LIST, Arrays.asList(
                "ct", "cts", "class test", "test", "exam", "midterm", "quiz"
        ));

        // Project keywords
        intentKeywords.put(Intent.PROJECT_PROGRESS, Arrays.asList(
                "progress", "status", "how is", "going"
        ));

        // Priority keywords
        intentKeywords.put(Intent.WHAT_TO_DO_FIRST, Arrays.asList(
                "first", "priority", "important", "urgent", "should i do"
        ));
    }

    private static void initializePriority() {
        // Tasks have highest priority
        intentPriority.put(Intent.TASK_DAILY, 100);
        intentPriority.put(Intent.TASK_PENDING, 95);
        intentPriority.put(Intent.TASK_OVERDUE, 95);
        intentPriority.put(Intent.TASK_IN_PROGRESS, 90);
        intentPriority.put(Intent.TASK_COMPLETED, 85);
        intentPriority.put(Intent.TASK_BY_PROJECT, 80);

        // Project related
        intentPriority.put(Intent.PROJECT_PROGRESS, 70);
        intentPriority.put(Intent.PROJECT_LIST, 65);

        // Priority/Recommendations
        intentPriority.put(Intent.WHAT_TO_DO_FIRST, 75);
        intentPriority.put(Intent.URGENT_TASKS, 75);

        // Deadlines
        intentPriority.put(Intent.DEADLINE_TODAY, 60);
        intentPriority.put(Intent.DEADLINE_THIS_WEEK, 55);
        intentPriority.put(Intent.ASSIGNMENT_LIST, 50);
        intentPriority.put(Intent.CLASS_TEST_LIST, 50);

        // Events
        intentPriority.put(Intent.EVENT_TODAY, 45);
        intentPriority.put(Intent.EVENT_TOMORROW, 40);
        intentPriority.put(Intent.EVENT_THIS_WEEK, 35);

        // Default priority
        intentPriority.put(Intent.UNKNOWN, 0);
    }

    public Intent recognize(QueryContext context) {
        String query = context.getOriginalQuery().toLowerCase();

        System.out.println("DEBUG: Recognizing intent for: " + query);

        // First, check patterns (highest confidence)
        for (Map.Entry<Intent, List<Pattern>> entry : intentPatterns.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                java.util.regex.Matcher matcher = pattern.matcher(query);
                if (matcher.find()) {
                    System.out.println("DEBUG: Pattern matched intent: " + entry.getKey());

                    // Extract time if present
                    if (entry.getKey() == Intent.SCHEDULE_AT_TIME && matcher.groupCount() >= 1) {
                        context.addEntity("time", matcher.group(1));
                    }
                    return entry.getKey();
                }
            }
        }

        // Then check keywords with priority scoring
        Map<Intent, Integer> scoreMap = new HashMap<>();

        for (Map.Entry<Intent, List<String>> entry : intentKeywords.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (query.contains(keyword.toLowerCase())) {
                    // Give higher score for exact matches of multi-word keywords
                    if (keyword.contains(" ") && query.contains(keyword.toLowerCase())) {
                        score += 3;
                    } else {
                        score += 1;
                    }
                }
            }

            // Boost score if multiple keywords from same intent are found
            if (score > 0) {
                scoreMap.put(entry.getKey(), score);
                System.out.println("DEBUG: " + entry.getKey() + " scored " + score);
            }
        }

        // Find the best match based on score and priority
        Intent bestMatch = Intent.UNKNOWN;
        int bestScore = 0;
        int bestPriority = 0;

        for (Map.Entry<Intent, Integer> entry : scoreMap.entrySet()) {
            Intent intent = entry.getKey();
            int score = entry.getValue();
            int priority = intentPriority.getOrDefault(intent, 0);

            // Combine score and priority for ranking
            int totalScore = score + priority;

            if (totalScore > bestScore + bestPriority ||
                    (totalScore == bestScore + bestPriority && priority > bestPriority)) {
                bestScore = score;
                bestPriority = priority;
                bestMatch = intent;
            }
        }

        System.out.println("DEBUG: Best match intent: " + bestMatch + " (score: " + bestScore + ", priority: " + bestPriority + ")");

        // Special case: If query contains "tasks" but no specific task intent matched
        if (bestMatch == Intent.UNKNOWN && query.contains("task")) {
            System.out.println("DEBUG: Falling back to TASK_PENDING for task-related query");
            return Intent.TASK_PENDING;
        }

        // Special case: If query contains "daily" or "todo"
        if (bestMatch == Intent.UNKNOWN && (query.contains("daily") || query.contains("todo") || query.contains("to do"))) {
            System.out.println("DEBUG: Falling back to TASK_DAILY for daily/todo query");
            return Intent.TASK_DAILY;
        }

        // Special case: If query contains "deadline" or "due"
        if (bestMatch == Intent.UNKNOWN && (query.contains("deadline") || query.contains("due"))) {
            System.out.println("DEBUG: Falling back to DEADLINE_THIS_WEEK for deadline query");
            return Intent.DEADLINE_THIS_WEEK;
        }

        // Greeting detection
        if (query.matches(".*\\b(hi|hello|hey|greetings)\\b.*")) {
            return Intent.GREETING;
        }

        // Help detection
        if (query.contains("help") || query.contains("what can you do")) {
            return Intent.HELP;
        }

        return bestMatch;
    }
}