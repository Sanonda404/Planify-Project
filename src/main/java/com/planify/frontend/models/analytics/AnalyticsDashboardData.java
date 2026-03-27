package com.planify.frontend.models.analytics;

import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.utils.data.group.GroupDataManager;
import com.planify.frontend.utils.data.group.GroupEventDataManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.data.personal.EventDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import com.planify.frontend.utils.data.personal.TaskDataManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsDashboardData {

    // ========== BASIC STATS ==========
    private int totalTasks;
    private int completedTasks;
    private int totalEvents;
    private int totalProjects;
    private int totalGroups;
    private int totalDeadlines;
    private int completionRate;
    private int productivityScore;
    private int currentStreak;
    private int bestStreak;

    // ========== LISTS ==========
    private List<EventGetRequest> todayEvents;
    private List<Object> upcomingDeadlines;
    private int activeProjects;

    // ========== WEEKLY DATA ==========
    private WeeklyProductivity weeklyProductivity;
    private Map<String, Integer> dailyActivity;
    private Map<String, CategoryData> categoryBreakdown;
    private List<ProjectProgress> projectProgress;
    private List<GroupActivity> groupActivity;
    private List<Insight> insights;
    private int[][] heatMapData;

    // ========== TIME-BASED DATA ==========
    private int tasksLastWeek;
    private int eventsLastWeek;
    private int tasksLastMonth;
    private int eventsLastMonth;
    private int tasksLastYear;
    private int eventsLastYear;

    // ========== CONSTRUCTORS ==========
    public AnalyticsDashboardData() {
        this.todayEvents = new ArrayList<>();
        this.upcomingDeadlines = new ArrayList<>();
        this.dailyActivity = new LinkedHashMap<>();
        this.categoryBreakdown = new LinkedHashMap<>();
        this.projectProgress = new ArrayList<>();
        this.groupActivity = new ArrayList<>();
        this.insights = new ArrayList<>();
        this.heatMapData = new int[24][7];
        this.weeklyProductivity = new WeeklyProductivity();
    }

    // ========== DATA LOADING METHODS ==========

    /**
     * Load all data from personal and group sources
     */
    public static AnalyticsDashboardData loadAllData() {
        AnalyticsDashboardData data = new AnalyticsDashboardData();

        // Fetch all data from sources
        List<EventGetRequest> allEvents = data.fetchAllEvents();
        List<ProjectDetails> allProjects = data.fetchAllProjects();
        List<TaskDetails> allTasks = data.fetchAllTasks(allProjects);
        List<com.planify.frontend.models.group.GroupSummaryRequest> allGroups = data.fetchAllGroups();

        // Populate basic counts
        data.totalEvents = allEvents.size();
        data.totalTasks = allTasks.size();
        data.totalProjects = allProjects.size();
        data.totalGroups = allGroups != null ? allGroups.size() : 0;

        // Calculate completed tasks
        data.completedTasks = (int) allTasks.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .count();

        // Calculate completion rate
        data.completionRate = data.totalTasks > 0 ?
                (data.completedTasks * 100 / data.totalTasks) : 0;

        // Calculate deadlines
        data.totalDeadlines = (int) allEvents.stream()
                .filter(e -> "deadline".equalsIgnoreCase(e.getType()))
                .count();

        // Get today's events
        data.todayEvents = data.getTodayEvents(allEvents);

        // Get upcoming deadlines (next 7 days)
        data.upcomingDeadlines = data.getUpcomingDeadlines(allTasks, allEvents);

        // Get active projects
        data.activeProjects = (int) allProjects.stream()
                .filter(p -> !p.isCompleted())
                .count();

        // Get weekly productivity
        data.weeklyProductivity = data.getWeeklyProductivity(allTasks);

        // Get daily activity for last 7 days
        data.dailyActivity = data.getDailyActivity(allTasks, allEvents);

        // Get category breakdown
        data.categoryBreakdown = data.getCategoryBreakdown(allTasks);

        // Get project progress
        data.projectProgress = data.getProjectProgress(allProjects);

        // Get group activity
        data.groupActivity = data.getGroupActivity(allGroups, allEvents, allTasks);

        // Generate insights
        data.insights = data.generateInsights(allTasks, allEvents, allProjects);

        // Generate heat map data
        data.heatMapData = data.generateHeatMapData(allTasks, allEvents);

        // Calculate streak
        data.currentStreak = data.calculateCurrentStreak(allTasks);
        data.bestStreak = data.calculateBestStreak(allTasks);

        // Calculate productivity score
        data.productivityScore = data.calculateProductivityScore(allTasks, allEvents, allProjects);

        return data;
    }

    // ========== DATA FETCHING METHODS ==========

    private List<EventGetRequest> fetchAllEvents() {
        List<EventGetRequest> allEvents = new ArrayList<>();

        try {
            List<EventGetRequest> personalEvents = EventDataManager.getAll();
            if (personalEvents != null) allEvents.addAll(personalEvents);
        } catch (Exception e) {
            System.err.println("Error fetching personal events: " + e.getMessage());
        }

        try {
            List<EventGetRequest> groupEvents = GroupEventDataManager.getAll();
            if (groupEvents != null) allEvents.addAll(groupEvents);
        } catch (Exception e) {
            System.err.println("Error fetching group events: " + e.getMessage());
        }

        return allEvents;
    }

    private List<ProjectDetails> fetchAllProjects() {
        List<ProjectDetails> allProjects = new ArrayList<>();

        try {
            List<ProjectDetails> personalProjects = ProjectDataManager.getAllPersonalProjects();
            if (personalProjects != null) allProjects.addAll(personalProjects);
        } catch (Exception e) {
            System.err.println("Error fetching personal projects: " + e.getMessage());
        }

        try {
            List<ProjectDetails> groupProjects = GroupProjectDataManager.getAllGroupProjects();
            if (groupProjects != null) allProjects.addAll(groupProjects);
        } catch (Exception e) {
            System.err.println("Error fetching group projects: " + e.getMessage());
        }

        return allProjects;
    }

    private List<TaskDetails> fetchAllTasks(List<ProjectDetails> projects) {
        List<TaskDetails> allTasks = new ArrayList<>();

        try {
            List<TaskDetails> personalTasks = TaskDataManager.getAllPersonalTasks();
            if (personalTasks != null) allTasks.addAll(personalTasks);
        } catch (Exception e) {
            System.err.println("Error fetching personal tasks: " + e.getMessage());
        }

        try {
            List<TaskDetails> projectTasks = GroupProjectDataManager.getAllTasks();
            if (projectTasks != null) allTasks.addAll(projectTasks);
        } catch (Exception e) {
            System.err.println("Error fetching project tasks: " + e.getMessage());
        }

        // Also get tasks from projects
        for (ProjectDetails project : projects) {
            if (project.getMilestones() != null) {
                for (com.planify.frontend.models.project.MilestoneDetails milestone : project.getMilestones()) {
                    if (milestone.getTasks() != null) {
                        allTasks.addAll(milestone.getTasks());
                    }
                }
            }
        }

        return allTasks;
    }

    private List<com.planify.frontend.models.group.GroupSummaryRequest> fetchAllGroups() {
        try {
            return GroupDataManager.getGroupSummary();
        } catch (Exception e) {
            System.err.println("Error fetching groups: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ========== ANALYTICS CALCULATIONS ==========

    private List<EventGetRequest> getTodayEvents(List<EventGetRequest> events) {
        LocalDate today = LocalDate.now();
        return events.stream()
                .filter(e -> parseDateTime(e.getStartDateTime()) != null)
                .filter(e -> parseDateTime(e.getStartDateTime()).toLocalDate().equals(today))
                .collect(Collectors.toList());
    }

    private List<Object> getUpcomingDeadlines(List<TaskDetails> tasks, List<EventGetRequest> events) {
        List<Object> deadlines = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        // Add task deadlines
        for (TaskDetails task : tasks) {
            LocalDate dueDate = parseDate(task.getDueDate());
            if (dueDate != null && !dueDate.isBefore(today) && !dueDate.isAfter(nextWeek)) {
                deadlines.add(task);
            }
        }

        // Add event deadlines
        for (EventGetRequest event : events) {
            if ("deadline".equalsIgnoreCase(event.getType())) {
                LocalDate eventDate = parseDateTime(event.getStartDateTime()).toLocalDate();
                if (eventDate != null && !eventDate.isBefore(today) && !eventDate.isAfter(nextWeek)) {
                    deadlines.add(event);
                }
            }
        }

        deadlines.sort((a, b) -> {
            LocalDate dateA = a instanceof TaskDetails ?
                    parseDate(((TaskDetails) a).getDueDate()) :
                    parseDateTime(((EventGetRequest) a).getStartDateTime()).toLocalDate();
            LocalDate dateB = b instanceof TaskDetails ?
                    parseDate(((TaskDetails) b).getDueDate()) :
                    parseDateTime(((EventGetRequest) b).getStartDateTime()).toLocalDate();
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateA.compareTo(dateB);
        });

        return deadlines;
    }

    private WeeklyProductivity getWeeklyProductivity(List<TaskDetails> tasks) {
        WeeklyProductivity productivity = new WeeklyProductivity();
        LocalDate weekAgo = LocalDate.now().minusDays(7);

        for (TaskDetails task : tasks) {
            LocalDate dueDate = parseDate(task.getDueDate());
            if (dueDate != null && dueDate.isAfter(weekAgo)) {
                productivity.totalTasks++;
                switch (task.getStatus().toUpperCase()) {
                    case "COMPLETED":
                        productivity.completed++;
                        break;
                    case "IN_PROGRESS":
                        productivity.inProgress++;
                        break;
                    default:
                        productivity.pending++;
                        break;
                }
            }
        }

        productivity.completionRate = productivity.totalTasks > 0 ?
                (productivity.completed * 100 / productivity.totalTasks) : 0;

        return productivity;
    }

    private Map<String, Integer> getDailyActivity(List<TaskDetails> tasks, List<EventGetRequest> events) {
        Map<String, Integer> dailyActivity = new LinkedHashMap<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        for (String day : days) {
            dailyActivity.put(day, 0);
        }

        LocalDate weekAgo = LocalDate.now().minusDays(7);

        // Count tasks completed per day
        for (TaskDetails task : tasks) {
            if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
                LocalDate dueDate = parseDate(task.getDueDate());
                if (dueDate != null && dueDate.isAfter(weekAgo)) {
                    String dayName = dueDate.getDayOfWeek().toString().substring(0, 3);
                    dailyActivity.merge(dayName, 1, Integer::sum);
                }
            }
        }

        // Count events created per day
        for (EventGetRequest event : events) {
            LocalDate eventDate = parseDateTime(event.getStartDateTime()).toLocalDate();
            if (eventDate != null && eventDate.isAfter(weekAgo)) {
                String dayName = eventDate.getDayOfWeek().toString().substring(0, 3);
                dailyActivity.merge(dayName, 1, Integer::sum);
            }
        }

        return dailyActivity;
    }

    private Map<String, CategoryData> getCategoryBreakdown(List<TaskDetails> tasks) {
        Map<String, CategoryData> categories = new LinkedHashMap<>();
        String[] colors = {"#457b9d", "#10b981", "#f59e0b", "#e63946", "#8b5cf6", "#06b6d4"};
        int index = 0;

        for (TaskDetails task : tasks) {
            String category = task.getCategory() != null && !task.getCategory().isEmpty() ?
                    task.getCategory() : "Uncategorized";

            CategoryData data = categories.get(category);
            if (data == null) {
                data = new CategoryData(0, 0);
                data.setColor(colors[index % colors.length]);
                categories.put(category, data);
                index++;
            }

            data.setTotal(data.getTotal()+1);
            if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
                data.setCompleted(data.getCompleted()+1);
            }
        }

        return categories;
    }

    private List<ProjectProgress> getProjectProgress(List<ProjectDetails> projects) {
        List<ProjectProgress> progressList = new ArrayList<>();
        String[] colors = {"#457b9d", "#10b981", "#f59e0b", "#e63946", "#8b5cf6", "#06b6d4"};
        int index = 0;

        for (ProjectDetails project : projects) {
            if (!project.isCompleted()) {
                ProjectProgress progress = new ProjectProgress();
                progress.setName(project.getName());
                progress.setProgress(project.getProgress());
                progress.setCompletedTasks(project.getCompletedTasks());
                progress.setTotalTasks(project.getTotalTasks());
                progress.setColor(colors[index % colors.length]);
                progressList.add(progress);
                index++;
            }
        }

        return progressList;
    }

    private List<GroupActivity> getGroupActivity(
            List<com.planify.frontend.models.group.GroupSummaryRequest> groups,
            List<EventGetRequest> events,
            List<TaskDetails> tasks) {

        List<GroupActivity> activities = new ArrayList<>();

        if (groups == null) return activities;

        for (com.planify.frontend.models.group.GroupSummaryRequest group : groups) {
            GroupActivity activity = new GroupActivity();
            activity.setName(group.getName());
            activity.setTotalMembers(group.getTotalMembers());
            activity.setActiveMembers(calculateActiveMembersForGroup(group, events, tasks));
            activity.setActiveMembers(activity.getTotalMembers() > 0 ?
                    (activity.getActiveMembers() * 100 / activity.getTotalMembers()) : 0);
            activities.add(activity);
        }

        return activities;
    }

    private int calculateActiveMembersForGroup(
            com.planify.frontend.models.group.GroupSummaryRequest group,
            List<EventGetRequest> events,
            List<TaskDetails> tasks) {

        // For now, return a percentage of total members
        // In production, you would track actual member activity
        return group.getTotalMembers() > 0 ? Math.max(1, group.getTotalMembers() * 2 / 3) : 0;
    }

    private List<Insight> generateInsights(List<TaskDetails> tasks, List<EventGetRequest> events, List<ProjectDetails> projects) {
        List<Insight> insights = new ArrayList<>();

        // Productivity streak insight
        if (currentStreak >= 7) {
            insights.add(new Insight("🔥 Amazing Streak!",
                    "You've been productive for " + currentStreak + " days in a row! Keep up the momentum!", "positive"));
        } else if (currentStreak >= 3) {
            insights.add(new Insight("📈 Good Momentum",
                    "You're on a " + currentStreak + "-day streak. Consistency is key!", "positive"));
        }

        // Completion rate insight
        if (completionRate >= 80) {
            insights.add(new Insight("🎯 Excellent Completion Rate",
                    "You're completing " + completionRate + "% of your tasks. Outstanding performance!", "positive"));
        } else if (completionRate < 50) {
            insights.add(new Insight("⚠️ Low Completion Rate",
                    "Only " + completionRate + "% of tasks are completed. Consider breaking down large tasks.", "warning"));
        }

        // Most productive day
        if (!dailyActivity.isEmpty()) {
            String mostProductiveDay = dailyActivity.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Wednesday");
            insights.add(new Insight("⭐ Peak Performance Day",
                    "You're most productive on " + mostProductiveDay + "s. Schedule important tasks on this day!", "high"));
        }

        // Overdue tasks
        long overdueTasks = tasks.stream()
                .filter(t -> !"COMPLETED".equalsIgnoreCase(t.getStatus()))
                .filter(t -> parseDate(t.getDueDate()) != null &&
                        parseDate(t.getDueDate()).isBefore(LocalDate.now()))
                .count();

        if (overdueTasks > 0) {
            insights.add(new Insight("⚠️ Overdue Tasks",
                    "You have " + overdueTasks + " overdue " + (overdueTasks == 1 ? "task" : "tasks") +
                            ". Prioritize them to get back on track.", "warning"));
        }

        // Upcoming deadlines
        int upcomingDeadlinesCount = upcomingDeadlines.size();
        if (upcomingDeadlinesCount > 5) {
            insights.add(new Insight("📅 Busy Week Ahead",
                    "You have " + upcomingDeadlinesCount + " deadlines this week. Plan your time wisely!", "info"));
        }

        // Project progress insight
        long stalledProjects = projects.stream()
                .filter(p -> !p.isCompleted() && p.getProgress() < 30 && p.getProgress() > 0)
                .count();

        if (stalledProjects > 0) {
            insights.add(new Insight("🚀 Project Momentum Needed",
                    stalledProjects + " project(s) need attention. A small push can make a big difference!", "info"));
        }

        return insights;
    }

    private int[][] generateHeatMapData(List<TaskDetails> tasks, List<EventGetRequest> events) {
        int[][] heatMap = new int[24][7];

        for (TaskDetails task : tasks) {
            LocalDate dueDate = parseDate(task.getDueDate());
            if (dueDate != null && dueDate.isAfter(LocalDate.now().minusDays(30))) {
                int hour = getHourFromTask(task);
                int day = dueDate.getDayOfWeek().getValue() - 1;
                if (day >= 0 && day < 7 && hour >= 0 && hour < 24) {
                    heatMap[hour][day]++;
                }
            }
        }

        for (EventGetRequest event : events) {
            LocalDateTime eventTime = parseDateTime(event.getStartDateTime());
            if (eventTime != null && eventTime.isAfter(LocalDateTime.now().minusDays(30))) {
                int hour = eventTime.getHour();
                int day = eventTime.getDayOfWeek().getValue() - 1;
                if (day >= 0 && day < 7 && hour >= 0 && hour < 24) {
                    heatMap[hour][day]++;
                }
            }
        }

        return heatMap;
    }

    private int calculateCurrentStreak(List<TaskDetails> tasks) {
        int streak = 0;
        LocalDate today = LocalDate.now();

        Set<LocalDate> completionDates = tasks.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .map(t -> parseDate(t.getDueDate()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!completionDates.contains(today)) return 0;

        streak = 1;
        LocalDate checkDate = today.minusDays(1);

        while (completionDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    private int calculateBestStreak(List<TaskDetails> tasks) {
        int bestStreak = 0;
        int currentStreak = 0;
        LocalDate lastDate = null;

        List<LocalDate> completionDates = tasks.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .map(t -> parseDate(t.getDueDate()))
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        for (LocalDate date : completionDates) {
            if (lastDate != null && date.equals(lastDate.plusDays(1))) {
                currentStreak++;
            } else {
                currentStreak = 1;
            }
            bestStreak = Math.max(bestStreak, currentStreak);
            lastDate = date;
        }

        return bestStreak;
    }

    private int getHourFromTask(TaskDetails task) {
        if (task.getCategory() != null) {
            return Math.abs(task.getCategory().hashCode() % 24);
        }
        return new Random().nextInt(24);
    }

    // ========== UTILITY METHODS ==========

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateStr).toLocalDate();
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            return null;
        }
    }

    // ========== GETTERS & SETTERS ==========

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public int getTotalEvents() { return totalEvents; }
    public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }

    public int getTotalProjects() { return totalProjects; }
    public void setTotalProjects(int totalProjects) { this.totalProjects = totalProjects; }

    public int getTotalGroups() { return totalGroups; }
    public void setTotalGroups(int totalGroups) { this.totalGroups = totalGroups; }

    public int getTotalDeadlines() { return totalDeadlines; }
    public void setTotalDeadlines(int totalDeadlines) { this.totalDeadlines = totalDeadlines; }

    public int getCompletionRate() { return completionRate; }
    public void setCompletionRate(int completionRate) { this.completionRate = completionRate; }

    public int getProductivityScore() { return productivityScore; }
    public void setProductivityScore(int productivityScore) { this.productivityScore = productivityScore; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getBestStreak() { return bestStreak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }

    public List<EventGetRequest> getTodayEvents() { return todayEvents; }
    public void setTodayEvents(List<EventGetRequest> todayEvents) { this.todayEvents = todayEvents; }

    public List<Object> getUpcomingDeadlines() { return upcomingDeadlines; }
    public void setUpcomingDeadlines(List<Object> upcomingDeadlines) { this.upcomingDeadlines = upcomingDeadlines; }

    public int getActiveProjects() { return activeProjects; }
    public void setActiveProjects(int activeProjects) { this.activeProjects = activeProjects; }

    public WeeklyProductivity getWeeklyProductivity() { return weeklyProductivity; }
    public void setWeeklyProductivity(WeeklyProductivity weeklyProductivity) { this.weeklyProductivity = weeklyProductivity; }

    public Map<String, Integer> getDailyActivity() { return dailyActivity; }
    public void setDailyActivity(Map<String, Integer> dailyActivity) { this.dailyActivity = dailyActivity; }

    public Map<String, CategoryData> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(Map<String, CategoryData> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }

    public List<ProjectProgress> getProjectProgress() { return projectProgress; }
    public void setProjectProgress(List<ProjectProgress> projectProgress) { this.projectProgress = projectProgress; }

    public List<GroupActivity> getGroupActivity() { return groupActivity; }
    public void setGroupActivity(List<GroupActivity> groupActivity) { this.groupActivity = groupActivity; }

    public List<Insight> getInsights() { return insights; }
    public void setInsights(List<Insight> insights) { this.insights = insights; }

    public int[][] getHeatMapData() { return heatMapData; }
    public void setHeatMapData(int[][] heatMapData) { this.heatMapData = heatMapData; }

    public int getTasksLastWeek() { return tasksLastWeek; }
    public void setTasksLastWeek(int tasksLastWeek) { this.tasksLastWeek = tasksLastWeek; }

    public int getEventsLastWeek() { return eventsLastWeek; }
    public void setEventsLastWeek(int eventsLastWeek) { this.eventsLastWeek = eventsLastWeek; }

    public int getTasksLastMonth() { return tasksLastMonth; }
    public void setTasksLastMonth(int tasksLastMonth) { this.tasksLastMonth = tasksLastMonth; }

    public int getEventsLastMonth() { return eventsLastMonth; }
    public void setEventsLastMonth(int eventsLastMonth) { this.eventsLastMonth = eventsLastMonth; }

    public int getTasksLastYear() { return tasksLastYear; }
    public void setTasksLastYear(int tasksLastYear) { this.tasksLastYear = tasksLastYear; }

    public int getEventsLastYear() { return eventsLastYear; }
    public void setEventsLastYear(int eventsLastYear) { this.eventsLastYear = eventsLastYear; }

    // ========== INNER CLASSES ==========

    public static class WeeklyProductivity {
        private int totalTasks;
        private int completed;
        private int inProgress;
        private int pending;
        private int completionRate;

        public int getTotalTasks() { return totalTasks; }
        public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
        public int getCompleted() { return completed; }
        public void setCompleted(int completed) { this.completed = completed; }
        public int getInProgress() { return inProgress; }
        public void setInProgress(int inProgress) { this.inProgress = inProgress; }
        public int getPending() { return pending; }
        public void setPending(int pending) { this.pending = pending; }
        public int getCompletionRate() { return completionRate; }
        public void setCompletionRate(int completionRate) { this.completionRate = completionRate; }
    }

    private int calculateProductivityScore(List<TaskDetails> tasks, List<EventGetRequest> events, List<ProjectDetails> projects) {

        // ========== FACTOR 1: Task Completion Rate (40% weight) ==========
        double completionScore = 0;
        if (totalTasks > 0) {
            completionScore = (completedTasks * 100.0 / totalTasks);
        }
        double factor1 = completionScore * 0.4;

        // ========== FACTOR 2: Streak Consistency (20% weight) ==========
        double streakScore = 0;
        if (bestStreak > 0) {
            // 30+ days streak = 100 points
            streakScore = Math.min(100, (currentStreak * 100.0 / Math.max(bestStreak, 30)));
            if (currentStreak >= 7) streakScore = Math.min(100, streakScore + 10);
            if (currentStreak >= 14) streakScore = Math.min(100, streakScore + 10);
            if (currentStreak >= 30) streakScore = 100;
        }
        double factor2 = streakScore * 0.2;

        // ========== FACTOR 3: On-time Performance (15% weight) ==========
        double onTimeScore = 100;
        long onTimeTasks = 0;
        long totalTimedTasks = 0;

        for (TaskDetails task : tasks) {
            if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                totalTimedTasks++;
                LocalDate dueDate = parseDate(task.getDueDate());
                if (dueDate != null) {
                    boolean isCompleted = "COMPLETED".equalsIgnoreCase(task.getStatus());
                    if (isCompleted && !dueDate.isBefore(LocalDate.now())) {
                        onTimeTasks++;
                    } else if (isCompleted && dueDate.isBefore(LocalDate.now())) {
                        // Completed late - partial penalty
                        onTimeTasks += 0.5;
                    }
                }
            }
        }

        if (totalTimedTasks > 0) {
            onTimeScore = (onTimeTasks * 100.0 / totalTimedTasks);
        }
        double factor3 = onTimeScore * 0.15;

        // ========== FACTOR 4: Weekly Activity Consistency (15% weight) ==========
        double consistencyScore = calculateWeeklyConsistency(tasks, events);
        double factor4 = consistencyScore * 0.15;

        // ========== FACTOR 5: Project Progress (10% weight) ==========
        double projectProgressScore = 0;
        if (!projects.isEmpty()) {
            double avgProgress = projects.stream()
                    .filter(p -> !p.isCompleted())
                    .mapToInt(ProjectDetails::getProgress)
                    .average()
                    .orElse(0);
            projectProgressScore = avgProgress;
        }
        double factor5 = projectProgressScore * 0.1;

        // ========== BONUS: Weighted Task Completion (Optional) ==========
        double bonusScore = calculateWeightedBonus(tasks);

        // Calculate total score
        int totalScore = (int) (factor1 + factor2 + factor3 + factor4 + factor5 + bonusScore);

        // Ensure score is between 0 and 100
        return Math.min(100, Math.max(0, totalScore));
    }

    /**
     * Calculate weekly activity consistency
     * Measures how evenly tasks are distributed across days
     */
    private double calculateWeeklyConsistency(List<TaskDetails> tasks, List<EventGetRequest> events) {
        // Get activity for last 7 days
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        Map<String, Integer> dailyActivity = getDailyActivity(tasks, events);

        // Calculate variance in daily activity
        List<Integer> values = new ArrayList<>(dailyActivity.values());
        if (values.isEmpty()) return 0;

        double avg = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - avg, 2))
                .average()
                .orElse(0);

        // Lower variance = higher consistency
        double maxVariance = 100; // Maximum expected variance
        double consistencyScore = Math.max(0, 100 - (variance / maxVariance * 100));

        // Bonus for having at least one activity each day
        long activeDays = values.stream().filter(v -> v > 0).count();
        if (activeDays >= 5) consistencyScore += 10;
        if (activeDays >= 7) consistencyScore += 10;

        return Math.min(100, consistencyScore);
    }

    /**
     * Calculate weighted bonus based on task importance
     * Higher priority tasks give more points when completed
     */
    private double calculateWeightedBonus(List<TaskDetails> tasks) {
        double totalWeight = 0;
        double completedWeight = 0;

        for (TaskDetails task : tasks) {
            int weight = getTaskWeight(task);
            totalWeight += weight;
            if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
                completedWeight += weight;
            }
        }

        if (totalWeight == 0) return 0;

        double weightedCompletion = (completedWeight * 100.0 / totalWeight);
        // Bonus up to 5 points based on weighted completion
        return Math.min(5, weightedCompletion / 20);
    }

    /**
     * Get task weight (higher for more important tasks)
     */
    private int getTaskWeight(TaskDetails task) {
        int weight = 5; // Default weight

        // Check if task has priority field
        try {
            String priority = getTaskPriority(task);
            switch (priority.toLowerCase()) {
                case "high":
                    weight = 10;
                    break;
                case "medium":
                    weight = 7;
                    break;
                case "low":
                    weight = 3;
                    break;
                default:
                    weight = 5;
            }
        } catch (Exception e) {
            // Use default weight
        }

        // Add bonus weight for tasks with attachments
        if (task.getAttachmentUrl() != null && !task.getAttachmentUrl().isEmpty()) {
            weight += 1;
        }

        // Add bonus weight for tasks with description
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            weight += 1;
        }

        return weight;
    }

    /**
     * Helper to get task priority (needs to be added to TaskDetails)
     */
    private String getTaskPriority(TaskDetails task) {
        // You'll need to add priority field to TaskDetails
        // For now, return based on due date proximity
        if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
            LocalDate dueDate = parseDate(task.getDueDate());
            if (dueDate != null) {
                long daysUntil = LocalDate.now().until(dueDate).getDays();
                if (daysUntil < 0) return "high"; // Overdue
                if (daysUntil <= 2) return "high";
                if (daysUntil <= 7) return "medium";
            }
        }
        return "low";
    }

}