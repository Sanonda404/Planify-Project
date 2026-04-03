package com.planify.frontend.controllers;

import com.planify.frontend.controllers.Request.GetRequestController;
import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.SceneParent;
import com.planify.frontend.models.analytics.*;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.managers.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AnalyticsController extends SceneParent implements Initializable {

    // ========== FXML COMPONENTS ==========
    @FXML private Label completionRateLabel;
    @FXML private Label totalCompletedLabel;
    @FXML private Label productivityScoreLabel;
    @FXML private Label streakLabel;
    @FXML private Label completionTrendLabel;
    @FXML private Label completedTrendLabel;
    @FXML private Label scoreTrendLabel;
    @FXML private Label streakTrendLabel;
    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label totalEventsLabel;
    @FXML private Label totalProjectsLabel;
    @FXML private Label totalDeadlinesLabel;
    @FXML private Label completedPercentLabel;
    @FXML private Label inProgressPercentLabel;
    @FXML private Label pendingPercentLabel;
    @FXML private Label overduePercentLabel;
    @FXML private VBox dailyActivityContainer;
    @FXML private VBox categoryContainer;
    @FXML private VBox projectProgressContainer;
    @FXML private VBox groupActivityContainer;
    @FXML private VBox insightsContainer;
    @FXML private Canvas dailyActivityChart;
    @FXML private Canvas taskDistributionChart;
    @FXML private GridPane heatMapContainer;
    @FXML private Button weekBtn;
    @FXML private Button monthBtn;
    @FXML private Button yearBtn;
    @FXML private Circle connectionStatusCircle;
    @FXML private Label connectionStatusLabel;
    @FXML private ListView<NotificationResponse> notificationsList;
    @FXML public Button notifBtn;
    @FXML public VBox notifPanel;

    private AnalyticsService analyticsService;
    private AnalyticsDashboardData currentData;
    private String currentView = "week";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        analyticsService = AnalyticsService.getInstance();
        setupNotificationPanel();
        init();
        loadData();
    }

    private void setupNotificationPanel() {
        notificationsList.setItems(NotificationManager.getNotifications());
        notificationsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(NotificationResponse notif, boolean empty) {
                super.updateItem(notif, empty);
                if (empty || notif == null) {
                    setGraphic(null);
                } else {
                    setGraphic(createNotificationItem(notif));
                }
            }
        });
        NotificationManager.setParent(this);
    }

    public void init() {
        NotificationController.setStatusControls(connectionStatusCircle, connectionStatusLabel);
    }

    public void refresh() {
        loadData();
    }

    private void loadData() {
        showLoadingState();

        new Thread(() -> {
            try {
                AnalyticsDashboardData data;
                switch (currentView) {
                    case "month":
                        data = analyticsService.getMonthlyData();
                        break;
                    case "year":
                        data = analyticsService.getYearlyData();
                        break;
                    default:
                        data = analyticsService.getWeeklyData();
                        break;
                }

                Platform.runLater(() -> {
                    currentData = data;
                    populateUI();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showErrorState(e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void showLoadingState() {
        System.out.println("Loading analytics data...");
    }

    private void showErrorState(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error Loading Data");
        alert.setHeaderText("Could not load analytics data");
        alert.setContentText(error);
        alert.showAndWait();
    }

    private void populateUI() {
        if (currentData == null) return;
        populateKPIs();
        populateStats();
        drawDailyActivityChart();
        drawTaskDistributionChart();
        populateCategories();
        populateHeatMap();
        populateProjects();
        populateGroups();
        populateInsights();
    }

    private void populateKPIs() {
        completionRateLabel.setText(currentData.getCompletionRate() + "%");
        totalCompletedLabel.setText(String.valueOf(currentData.getCompletedTasks()));
        productivityScoreLabel.setText(String.valueOf(currentData.getProductivityScore()));
        streakLabel.setText(String.valueOf(currentData.getCurrentStreak()));

        completionTrendLabel.setText(currentData.getCompletionRate() >= 50 ?
                "↑ +" + currentData.getCompletionRate()/10 + "%" : "↓ -" + (50 - currentData.getCompletionRate())/10 + "%");
        completedTrendLabel.setText(currentData.getCompletedTasks()>= 20 ?
                "+" + currentData.getCompletedTasks()/5 : "-" + (20 - currentData.getCompletedTasks())/5);
        scoreTrendLabel.setText(currentData.getProductivityScore() >= 70 ?
                "↑ +" + (currentData.getProductivityScore() - 70)/5 : "↓ -" + (70 - currentData.getProductivityScore())/5);
        streakTrendLabel.setText("🔥 Best: " + currentData.getBestStreak() + " days");
    }

    private void populateStats() {
        totalTasksLabel.setText(String.valueOf(currentData.getTotalTasks()));
        completedTasksLabel.setText(String.valueOf(currentData.getCompletedTasks()));
        totalEventsLabel.setText(String.valueOf(currentData.getTotalEvents()));
        totalProjectsLabel.setText(String.valueOf(currentData.getTotalProjects()));
        totalDeadlinesLabel.setText(String.valueOf(currentData.getTotalDeadlines()));
    }

    private void drawDailyActivityChart() {
        if (currentData.getDailyActivity() == null || currentData.getDailyActivity().isEmpty()) return;

        GraphicsContext gc = dailyActivityChart.getGraphicsContext2D();
        double width = dailyActivityChart.getWidth();
        double height = dailyActivityChart.getHeight();
        gc.clearRect(0, 0, width, height);

        int maxValue = Collections.max(currentData.getDailyActivity().values());
        if (maxValue == 0) maxValue = 1;

        double barWidth = (width - 80) / currentData.getDailyActivity().size();
        double x = 50;

        int index = 0;
        for (Map.Entry<String, Integer> entry : currentData.getDailyActivity().entrySet()) {
            double barHeight = (entry.getValue() / (double) maxValue) * (height - 80);
            double y = height - 40 - barHeight;

            gc.setFill(Color.web("#457b9d"));
            gc.fillRoundRect(x, y, barWidth - 4, barHeight, 6, 6);
            gc.setFill(Color.web("#1d3557"));
            gc.fillText(String.valueOf(entry.getValue()), x + (barWidth - 4) / 2 - 8, y - 5);
            gc.fillText(entry.getKey(), x + (barWidth - 4) / 2 - 8, height - 20);
            x += barWidth;
        }

        gc.setStroke(Color.web("#a8dadc"));
        gc.setLineWidth(1.5);
        gc.strokeLine(45, 20, 45, height - 45);
        gc.strokeLine(45, height - 45, width - 20, height - 45);

        gc.setFill(Color.web("#457b9d"));
        for (int i = 0; i <= 4; i++) {
            int value = maxValue * i / 4;
            double yPos = height - 45 - (value / (double) maxValue) * (height - 80);
            gc.fillText(String.valueOf(value), 25, yPos + 3);
        }
    }

    private void drawTaskDistributionChart() {
        GraphicsContext gc = taskDistributionChart.getGraphicsContext2D();
        double width = taskDistributionChart.getWidth();
        double height = taskDistributionChart.getHeight();
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = 70;

        gc.clearRect(0, 0, width, height);

        int total = currentData.getTotalTasks();
        int completed = currentData.getCompletedTasks();
        int inProgress = currentData.getWeeklyProductivity() != null ? currentData.getWeeklyProductivity().getInProgress() : 0;
        int pending = currentData.getWeeklyProductivity() != null ? currentData.getWeeklyProductivity().getPending() : 0;
        int overdue = Math.max(0, total - (completed + inProgress + pending));

        if (total == 0) {
            gc.setFill(Color.web("#a8dadc"));
            gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            return;
        }

        double startAngle = 0;
        double completedAngle = (completed / (double) total) * 360;
        double inProgressAngle = (inProgress / (double) total) * 360;
        double pendingAngle = (pending / (double) total) * 360;
        double overdueAngle = (overdue / (double) total) * 360;

        gc.setFill(Color.web("#2e7d32"));
        gc.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, startAngle, completedAngle, javafx.scene.shape.ArcType.ROUND);
        startAngle += completedAngle;

        gc.setFill(Color.web("#e63946"));
        gc.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, startAngle, inProgressAngle, javafx.scene.shape.ArcType.ROUND);
        startAngle += inProgressAngle;

        gc.setFill(Color.web("#a8dadc"));
        gc.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, startAngle, pendingAngle, javafx.scene.shape.ArcType.ROUND);
        startAngle += pendingAngle;

        gc.setFill(Color.web("#1d3557"));
        gc.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, startAngle, overdueAngle, javafx.scene.shape.ArcType.ROUND);

        completedPercentLabel.setText(Math.round(completedAngle) + "%");
        inProgressPercentLabel.setText(Math.round(inProgressAngle) + "%");
        pendingPercentLabel.setText(Math.round(pendingAngle) + "%");
        overduePercentLabel.setText(Math.round(overdueAngle) + "%");
    }

    private void populateCategories() {
        categoryContainer.getChildren().clear();

        if (currentData.getCategoryBreakdown() == null || currentData.getCategoryBreakdown().isEmpty()) {
            categoryContainer.getChildren().add(createEmptyLabel("No category data available"));
            return;
        }

        String[] colors = {"#457b9d", "#10b981", "#f59e0b", "#e63946", "#8b5cf6", "#06b6d4"};
        int index = 0;

        for (Map.Entry<String, CategoryData> entry : currentData.getCategoryBreakdown().entrySet()) {
            String color = colors[index % colors.length];
            VBox item = createCategoryItem(entry.getKey(), entry.getValue(), color);
            categoryContainer.getChildren().add(item);
            index++;
        }
    }

    private VBox createCategoryItem(String name, CategoryData data, String color) {
        VBox item = new VBox(8);
        item.getStyleClass().add("category-item");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Region dot = new Region();
        dot.setMinSize(10, 10);
        dot.setMaxSize(10, 10);
        dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("category-name");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        double percent = data.getTotal() > 0 ? (data.getCompleted() * 100.0 / data.getTotal()) : 0;
        Label percentLabel = new Label(String.format("%.0f%%", percent));
        percentLabel.getStyleClass().add("category-percent");
        percentLabel.setTextFill(Color.web(color));

        header.getChildren().addAll(dot, nameLabel, percentLabel);

        StackPane barContainer = new StackPane();
        barContainer.setPrefHeight(6);
        barContainer.setMaxWidth(Double.MAX_VALUE);

        Rectangle bg = new Rectangle();
        bg.setHeight(6);
        bg.widthProperty().bind(barContainer.widthProperty());
        bg.setFill(Color.web("#e2e8f0"));
        bg.setArcWidth(4);
        bg.setArcHeight(4);

        Rectangle fill = new Rectangle();
        fill.setHeight(6);
        fill.widthProperty().bind(barContainer.widthProperty().multiply(percent / 100));
        fill.setFill(Color.web(color));
        fill.setArcWidth(4);
        fill.setArcHeight(4);

        barContainer.getChildren().addAll(bg, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        Label countLabel = new Label(data.getCompleted() + "/" + data.getTotal() + " tasks");
        countLabel.getStyleClass().add("category-count");

        item.getChildren().addAll(header, barContainer, countLabel);
        return item;
    }

    private void populateHeatMap() {
        heatMapContainer.getChildren().clear();
        if (currentData.getHeatMapData() == null) return;

        String[] hours = {"12a", "1a", "2a", "3a", "4a", "5a", "6a", "7a", "8a", "9a", "10a", "11a",
                "12p", "1p", "2p", "3p", "4p", "5p", "6p", "7p", "8p", "9p", "10p", "11p"};
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        for (int i = 0; i < hours.length; i++) {
            Label hourLabel = new Label(hours[i]);
            hourLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748b; -fx-min-width: 35;");
            heatMapContainer.add(hourLabel, 0, i + 1);
        }

        for (int j = 0; j < days.length; j++) {
            Label dayLabel = new Label(days[j]);
            dayLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #1d3557; -fx-min-width: 40;");
            heatMapContainer.add(dayLabel, j + 1, 0);
        }

        int[][] heatMap = currentData.getHeatMapData();
        for (int i = 0; i < Math.min(24, heatMap.length); i++) {
            for (int j = 0; j < Math.min(7, heatMap[i].length); j++) {
                int value = heatMap[i][j];
                StackPane cell = new StackPane();
                cell.getStyleClass().add("heatmap-cell");
                cell.setPrefWidth(40);
                cell.setPrefHeight(40);

                String color;
                if (value == 0) color = "#f1faee";
                else if (value < 3) color = "#a8dadc";
                else if (value < 6) color = "#457b9d";
                else if (value < 8) color = "#1d3557";
                else color = "#e63946";

                cell.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6;");
                Label valueLabel = new Label(String.valueOf(value));
                valueLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " +
                        (value > 5 ? "white" : "#1d3557") + ";");
                cell.getChildren().add(valueLabel);
                heatMapContainer.add(cell, j + 1, i + 1);
            }
        }
    }

    private void populateProjects() {
        projectProgressContainer.getChildren().clear();

        if (currentData.getProjectProgress() == null || currentData.getProjectProgress().isEmpty()) {
            projectProgressContainer.getChildren().add(createEmptyLabel("No active projects"));
            return;
        }

        for (ProjectProgress project : currentData.getProjectProgress()) {
            VBox item = new VBox(8);
            item.getStyleClass().add("project-item");
            item.setOnMouseClicked(e -> goToProject(project.getName()));

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(project.getName());
            nameLabel.getStyleClass().add("project-name");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Label percentLabel = new Label(project.getProgress() + "%");
            percentLabel.getStyleClass().add("project-percent");
            header.getChildren().addAll(nameLabel, percentLabel);

            StackPane barContainer = new StackPane();
            barContainer.setPrefHeight(6);
            barContainer.setMaxWidth(Double.MAX_VALUE);

            Rectangle bg = new Rectangle();
            bg.setHeight(6);
            bg.widthProperty().bind(barContainer.widthProperty());
            bg.setFill(Color.web("#e2e8f0"));
            bg.setArcWidth(4);
            bg.setArcHeight(4);

            Rectangle fill = new Rectangle();
            fill.setHeight(6);
            fill.widthProperty().bind(barContainer.widthProperty().multiply(project.getProgress() / 100.0));
            fill.setFill(Color.web("#2e7d32"));
            fill.setArcWidth(4);
            fill.setArcHeight(4);

            barContainer.getChildren().addAll(bg, fill);
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);

            Label tasksLabel = new Label(project.getCompletedTasks()+ "/" + project.getTotalTasks() + " tasks completed");
            tasksLabel.getStyleClass().add("project-tasks");

            item.getChildren().addAll(header, barContainer, tasksLabel);
            projectProgressContainer.getChildren().add(item);
        }
    }

    private void populateGroups() {
        groupActivityContainer.getChildren().clear();

        if (currentData.getGroupActivity() == null || currentData.getGroupActivity().isEmpty()) {
            groupActivityContainer.getChildren().add(createEmptyLabel("No group activity"));
            return;
        }

        for (GroupActivity group : currentData.getGroupActivity()) {
            HBox item = new HBox(12);
            item.setAlignment(Pos.CENTER_LEFT);
            item.getStyleClass().add("group-item");
            item.setOnMouseClicked(e -> goToGroup(group.getName()));

            Label icon = new Label("👥");
            icon.setStyle("-fx-font-size: 20px;");

            VBox infoBox = new VBox(3);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Label nameLabel = new Label(group.getName());
            nameLabel.getStyleClass().add("group-name");

            Label activityLabel = new Label(group.getActiveMembers() + "/" + group.getTotalMembers() + " active members");
            activityLabel.getStyleClass().add("group-activity");
            infoBox.getChildren().addAll(nameLabel, activityLabel);

            Label percentLabel = new Label(group.getActivePercentage() + "%");
            percentLabel.getStyleClass().add("group-percent");

            item.getChildren().addAll(icon, infoBox, percentLabel);
            groupActivityContainer.getChildren().add(item);
        }
    }

    private void populateInsights() {
        insightsContainer.getChildren().clear();

        if (currentData.getInsights() == null || currentData.getInsights().isEmpty()) {
            insightsContainer.getChildren().add(createEmptyLabel("No insights available"));
            return;
        }

        for (Insight insight : currentData.getInsights()) {
            HBox item = new HBox(12);
            item.setAlignment(Pos.CENTER_LEFT);
            item.getStyleClass().add("insight-item");

            String icon, badgeColor, badgeText;
            switch (insight.getType()) {
                case "high": icon = "🔥"; badgeColor = "#e63946"; badgeText = "Peak"; break;
                case "warning": icon = "⚠️"; badgeColor = "#e63946"; badgeText = "Alert"; break;
                case "positive": icon = "📈"; badgeColor = "#2e7d32"; badgeText = "Trend"; break;
                default: icon = "💡"; badgeColor = "#457b9d"; badgeText = "Insight";
            }

            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 22px;");

            VBox contentBox = new VBox(3);
            HBox.setHgrow(contentBox, Priority.ALWAYS);

            Label titleLabel = new Label(insight.getTitle());
            titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1d3557;");

            Label messageLabel = new Label(insight.getMessage());
            messageLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-wrap-text: true;");
            contentBox.getChildren().addAll(titleLabel, messageLabel);

            Label badge = new Label(badgeText);
            badge.setStyle("-fx-background-color: " + badgeColor + "; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 9px; -fx-font-weight: 600; -fx-text-fill: white;");

            item.getChildren().addAll(iconLabel, contentBox, badge);
            insightsContainer.getChildren().add(item);
        }
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state");
        return label;
    }
    // ========== EVENT HANDLERS ==========
    @FXML private void toggleNotifications() {
        boolean isVisible = notifPanel.isVisible();
        notifPanel.setVisible(!isVisible);
        notifPanel.setManaged(!isVisible);
    }

    @FXML private void showWeeklyView() {
        currentView = "week";
        updateButtonStyles(weekBtn, monthBtn, yearBtn);
        loadData();
    }

    @FXML private void showMonthlyView() {
        currentView = "month";
        updateButtonStyles(monthBtn, weekBtn, yearBtn);
        loadData();
    }

    @FXML private void showYearlyView() {
        currentView = "year";
        updateButtonStyles(yearBtn, weekBtn, monthBtn);
        loadData();
    }

    private void updateButtonStyles(Button active, Button... inactive) {
        active.getStyleClass().remove("date-range-btn");
        active.getStyleClass().add("date-range-btn-active");
        for (Button btn : inactive) {
            btn.getStyleClass().remove("date-range-btn-active");
            btn.getStyleClass().add("date-range-btn");
        }
    }

    @FXML private void goBack() { SceneManager.switchScene("dashboard-view.fxml", "Dashboard"); }
    @FXML private void goDashboard() { SceneManager.switchScene("dashboard-view.fxml", "Dashboard"); }
    @FXML private void handleLogout() { SceneManager.switchScene("login-view.fxml", "Login"); }

    private void goToProject(String projectName) { System.out.println("Navigate to project: " + projectName); }
    private void goToGroup(String groupName) { System.out.println("Navigate to group: " + groupName); }

    public void setNotificationManagerParent() { NotificationManager.setParent(this); }
}