package com.planify.frontend.models.auth;

import com.planify.frontend.controllers.auth.DashboardController;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.data.group.GroupDataManager;
import com.planify.frontend.utils.data.group.GroupEventDataManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.data.personal.EventDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import com.planify.frontend.utils.data.personal.TaskDataManager;
import javafx.concurrent.Task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardSummary {
    private static int totalEvents;
    private static int totalDeadlines;
    private static int totalActiveProjects;
    private static int totalActiveGroups;
    private static int totalTodo;
    private static List<ProjectDetails> projects;
    private static List<EventGetRequest> events;
    private static List<TaskDetails> tasks;
    private static DashboardWeeklyProductivitySummary weeklyProductivitySummary;
    private static List<ProjectDetails> topProjects;
    private static List<EventGetRequest> topEvents;
    private static List<TaskDetails> topTasks;
    private static final int MAX_TOP_ITEMS = 5;

    public static void init(DashboardController parent){
        projects = new ArrayList<>();
        events = new ArrayList<>();
        tasks = new ArrayList<>();
        projects.addAll(GroupDataManager.getAllGroupProjects());
        projects.addAll(ProjectDataManager.getAllPersonalProjects());
        events.addAll(GroupEventDataManager.getAll());
        events.addAll(EventDataManager.getAll());
        tasks.addAll(GroupProjectDataManager.getAllTasks());
        tasks.addAll(TaskDataManager.getAllPersonalTasks());

        topProjects = extractTopProjects();
        topEvents = extractTopEventsToday();
        topTasks = extractTopTasksToday();

        weeklyProductivitySummary = new DashboardWeeklyProductivitySummary(tasks,true);

        parent.populateUI();
    }

    public static List<EventGetRequest> extractTopEventsToday() {
        LocalDate today = LocalDate.now();

        // Filter events for today
        List<EventGetRequest> todayEvents = events.stream()
                .filter(event -> {
                    try {
                        LocalDateTime eventDateTime = LocalDateTime.parse(event.getStartDateTime());
                        return eventDateTime.toLocalDate().equals(today);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        totalEvents = todayEvents.size();
        for(EventGetRequest e:todayEvents){
            if(e.getType().equalsIgnoreCase("DEADLINE"))totalDeadlines++;
        }

        todayEvents.sort((e1, e2) -> {
            boolean isDeadline1 = "deadline".equalsIgnoreCase(e1.getType());
            boolean isDeadline2 = "deadline".equalsIgnoreCase(e2.getType());

            // Deadlines come first
            if (isDeadline1 && !isDeadline2) return -1;
            if (!isDeadline1 && isDeadline2) return 1;

            // Both deadlines or both non-deadlines: sort by start time
            try {
                LocalDateTime time1 = LocalDateTime.parse(e1.getStartDateTime());
                LocalDateTime time2 = LocalDateTime.parse(e2.getStartDateTime());
                return time1.compareTo(time2);
            } catch (Exception e) {
                return 0;
            }
        });

        // Return top 5 (or all if less than 5)
        return todayEvents.stream()
                .limit(MAX_TOP_ITEMS)
                .collect(Collectors.toList());
    }

    /**
     * Get top 5 tasks ending today (due today)
     * Sorted by due time in ascending order
     */
    public static List<TaskDetails>extractTopTasksToday() {
        LocalDate today = LocalDate.now();
        List<TaskDetails> todayTasks = tasks.stream()
                .filter(task -> {
                    try {
                        if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
                            return false;
                        }
                        LocalDate dueDate = LocalDate.parse(task.getDueDate());
                        return dueDate.equals(today);
                    } catch (Exception e) {
                        return false;
                    }
                })
                // Exclude completed tasks if needed? Comment out if you want to show completed too
                .filter(task -> !"COMPLETED".equalsIgnoreCase(task.getStatus()))
                .collect(Collectors.toList());

        totalTodo = todayTasks.size();
        // Sort by due date/time (ascending)
        todayTasks.sort((t1, t2) -> {
            try {
                LocalDateTime time1 = LocalDateTime.parse(t1.getDueDate());
                LocalDateTime time2 = LocalDateTime.parse(t2.getDueDate());
                return time1.compareTo(time2);
            } catch (Exception e) {
                // If parsing fails, compare as strings
                return t1.getDueDate().compareTo(t2.getDueDate());
            }
        });

        // Return top 5 (or all if less than 5)
        return todayTasks.stream()
                .limit(MAX_TOP_ITEMS)
                .collect(Collectors.toList());
    }

    /**
     * Get top 5 active projects
     * Sorted by progress percentage (highest first) or by number of active tasks
     */
    public static List<ProjectDetails> extractTopProjects() {
        // Filter active projects (not completed and has some progress)
        List<ProjectDetails> activeProjects = projects.stream()
                .filter(project -> !project.isCompleted())
                .collect(Collectors.toList());

        totalActiveProjects = activeProjects.size();

        activeProjects.sort((p1, p2) -> {
            // Primary: Progress percentage
            int progressCompare = Integer.compare(p2.getProgress(), p1.getProgress());
            if (progressCompare != 0) return progressCompare;

            // Secondary: Number of pending tasks (higher means more work, might be priority)
            int pending1 = p1.getTotalTasks() - p1.getCompletedTasks();
            int pending2 = p2.getTotalTasks() - p2.getCompletedTasks();
            return Integer.compare(pending2, pending1);
        });

        // Return top 5 (or all if less than 5)
        return activeProjects.stream()
                .limit(MAX_TOP_ITEMS)
                .collect(Collectors.toList());
    }

    /**
     * Alternative: Get top 5 projects by deadline proximity
     * Projects with closest deadlines get priority
     */
    public static List<ProjectDetails> getTopProjectsByDeadline() {
        LocalDate today = LocalDate.now();

        // Filter active projects and sort by deadline
        List<ProjectDetails> sortedProjects = projects.stream()
                .filter(project -> !project.isCompleted())
                .filter(project -> project.getDeadline() != null && !project.getDeadline().isEmpty())
                .sorted((p1, p2) -> {
                    try {
                        LocalDate deadline1 = LocalDate.parse(p1.getDeadline());
                        LocalDate deadline2 = LocalDate.parse(p2.getDeadline());

                        // Overdue projects come first
                        boolean overdue1 = deadline1.isBefore(today);
                        boolean overdue2 = deadline2.isBefore(today);

                        if (overdue1 && !overdue2) return -1;
                        if (!overdue1 && overdue2) return 1;

                        // Then sort by deadline (closest first)
                        return deadline1.compareTo(deadline2);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());

        return sortedProjects.stream()
                .limit(MAX_TOP_ITEMS)
                .collect(Collectors.toList());
    }

    // Getters
    public static int getTotalEvents() {
        return totalEvents;
    }

    public static int getTotalDeadlines() {
        return totalDeadlines;
    }

    public static int getActiveProjects() {
        return totalActiveProjects;
    }

    public static int getActiveGroups() {
        return totalActiveGroups;
    }

    public static int getTotalTodo() {
        return totalTodo;
    }


    public static DashboardWeeklyProductivitySummary getWeeklyProductivitySummary() {
        return weeklyProductivitySummary;
    }

    public static List<EventGetRequest> getTopEvents() {
        return topEvents;
    }

    public static List<ProjectDetails> getTopProjects() {
        return topProjects;
    }

    public static List<TaskDetails> getTopTasks() {
        return topTasks;
    }

    public static int getWeeklyProgress(){
        int total = weeklyProductivitySummary.getTotalTasks();
        return (int)weeklyProductivitySummary.getCompleted()/total;
    }

    public static int getAverageProjectProgress() {
        double progress = 0;
        double total = 0;
        for(ProjectDetails p: projects){
            if(!p.isCompleted()){
                total+=100;
                progress +=p.getProgress();
            }
        }
        return (int)(progress/total*100);
    }
}
