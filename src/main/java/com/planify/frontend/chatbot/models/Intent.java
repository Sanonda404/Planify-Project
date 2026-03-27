// models/Intent.java (updated with all task intents)
package com.planify.frontend.chatbot.models;

public enum Intent {
    // Task related - HIGHEST PRIORITY
    TASK_DAILY("task_daily"),
    TASK_PENDING("task_pending"),
    TASK_COMPLETED("task_completed"),
    TASK_IN_PROGRESS("task_in_progress"),
    TASK_OVERDUE("task_overdue"),
    TASK_BY_PROJECT("task_by_project"),
    TASK_BY_CATEGORY("task_by_category"),

    // Deadline related
    DEADLINE_COUNT("deadline_count"),
    DEADLINE_LIST("deadline_list"),
    DEADLINE_TODAY("deadline_today"),
    DEADLINE_TOMORROW("deadline_tomorrow"),
    DEADLINE_THIS_WEEK("deadline_this_week"),
    DEADLINE_UPCOMING("deadline_upcoming"),

    // Assignment/Test related
    ASSIGNMENT_LIST("assignment_list"),
    CLASS_TEST_LIST("class_test_list"),

    // Event related
    EVENT_TODAY("event_today"),
    EVENT_TOMORROW("event_tomorrow"),
    EVENT_THIS_WEEK("event_this_week"),
    EVENT_SPECIFIC_TIME("event_specific_time"),
    EVENT_SEARCH("event_search"),

    // Project related
    PROJECT_PROGRESS("project_progress"),
    PROJECT_LIST("project_list"),
    PROJECT_DETAILS("project_details"),
    PROJECT_MILESTONES("project_milestones"),
    PROJECT_VELOCITY("project_velocity"),
    PROJECT_COMPLETION_DATE("project_completion_date"),

    // Group related
    GROUP_LIST("group_list"),
    GROUP_MEMBERS("group_members"),
    GROUP_EVENTS("group_events"),
    GROUP_ROLE("group_role"),

    // Priority/Actionable
    WHAT_TO_DO_FIRST("what_to_do_first"),
    URGENT_TASKS("urgent_tasks"),
    RECOMMENDATION("recommendation"),

    // Analytics
    PRODUCTIVITY_ANALYSIS("productivity_analysis"),
    WEEKLY_SUMMARY("weekly_summary"),
    TRENDS("trends"),

    // Time specific
    SCHEDULE_AT_TIME("schedule_at_time"),
    FREE_TIME("free_time"),
    BUSY_TIME("busy_time"),

    // General
    GREETING("greeting"),
    HELP("help"),
    UNKNOWN("unknown");

    private final String value;

    Intent(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}