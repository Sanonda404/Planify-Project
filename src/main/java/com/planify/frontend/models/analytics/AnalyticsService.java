package com.planify.frontend.models.analytics;

public class AnalyticsService {

    private static AnalyticsService instance;

    private AnalyticsService() {}

    public static AnalyticsService getInstance() {
        if (instance == null) {
            instance = new AnalyticsService();
        }
        return instance;
    }

    public AnalyticsDashboardData getWeeklyData() {
        AnalyticsDashboardData data = AnalyticsDashboardData.loadAllData();
        data.setTasksLastWeek(data.getTotalTasks());
        data.setEventsLastWeek(data.getTotalEvents());
        return data;
    }

    public AnalyticsDashboardData getMonthlyData() {
        AnalyticsDashboardData data = AnalyticsDashboardData.loadAllData();
        // Multiply by ~4 for monthly approximation
        data.setTasksLastMonth(data.getTotalTasks() * 4);
        data.setEventsLastMonth(data.getTotalEvents() * 4);
        return data;
    }

    public AnalyticsDashboardData getYearlyData() {
        AnalyticsDashboardData data = AnalyticsDashboardData.loadAllData();
        // Multiply by ~52 for yearly approximation
        data.setTasksLastYear(data.getTotalTasks() * 52);
        data.setEventsLastYear(data.getTotalEvents() * 52);
        return data;
    }

    public AnalyticsDashboardData getDashboardData() {
        return AnalyticsDashboardData.loadAllData();
    }
}