package com.planify.frontend.controllers.auth;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.auth.DashboardSummary;
import com.planify.frontend.models.auth.DashboardWeeklyProductivitySummary;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.InitApp;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.managers.NotificationManager;
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

public class DashboardController implements Initializable {

    // ========== FXML COMPONENTS ==========
    @FXML private Label welcomeLabel;
    @FXML private Label eventCountLabel;
    @FXML private Label todoRatioLabel;
    @FXML private Label groupCountLabel;
    @FXML private Label projectCountLabel;
    @FXML private Label deadlineCountLabel;
    @FXML private Label eventTrendLabel;
    @FXML private Label completionRateLabel;
    @FXML private Label groupTrendLabel;
    @FXML private Label projectProgressLabel;
    @FXML private Label urgentLabel;
    @FXML private VBox upcomingEventsList;
    @FXML private VBox recentTasksList;
    @FXML private VBox activeProjectsList;
    @FXML private Canvas productivityCircle;
    @FXML private Label productivityPercentLabel;
    @FXML private Label completedThisWeekLabel;
    @FXML private Label inProgressThisWeekLabel;
    @FXML private Label pendingThisWeekLabel;
    @FXML private Label weightedProgressLabel;
    @FXML private GridPane mainContainer;
    @FXML private Label notificationBadge;
    @FXML private Button deleteAllBtn;
    @FXML private ListView<NotificationResponse> notificationsList;
    @FXML public Button notifBtn;
    @FXML public VBox notifPanel;
    @FXML private Circle connectionStatusCircle;
    @FXML private Label connectionStatusLabel;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        init();
        setupNotificationPanel();
        loadDashboardData();
    }

    private void init() {
        NotificationController.setStatusControls(connectionStatusCircle, connectionStatusLabel);
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

    private VBox createNotificationItem(NotificationResponse notif) {
        VBox item = new VBox(6);
        boolean isRead = "READ".equalsIgnoreCase(notif.getStatus());
        item.getStyleClass().addAll("notif-item", isRead ? "notif-read" : "notif-unread");
        item.setPadding(new Insets(12));

        Label titleLabel = new Label(notif.getTitle());
        titleLabel.getStyleClass().add("notif-item-title");

        Label textLabel = new Label(notif.getMessage());
        textLabel.getStyleClass().add("notif-item-text");
        textLabel.setWrapText(true);

        item.getChildren().addAll(titleLabel, textLabel);

        if (("GROUP_INVITE".equalsIgnoreCase(notif.getType()) || "JOIN_REQUEST".equalsIgnoreCase(notif.getType())) && !isRead) {
            HBox actions = new HBox(8);
            Button acceptBtn = new Button("Accept");
            acceptBtn.getStyleClass().add("btn-accept");
            acceptBtn.setOnAction(e -> handleInviteAction(notif, "ACCEPT", item));

            Button declineBtn = new Button("Decline");
            declineBtn.getStyleClass().add("btn-decline");
            declineBtn.setOnAction(e -> handleInviteAction(notif, "DECLINE", item));

            actions.getChildren().addAll(acceptBtn, declineBtn);
            item.getChildren().add(actions);
        }

        Label timeLabel = new Label(formatTime(notif.getCreatedAt()));
        timeLabel.getStyleClass().add("notif-time");
        item.getChildren().add(timeLabel);

        return item;
    }

    private void handleInviteAction(NotificationResponse notif, String action, VBox uiItem) {
        uiItem.getChildren().removeIf(node -> node instanceof HBox);
        uiItem.getStyleClass().remove("notif-unread");
        uiItem.getStyleClass().add("notif-read");

        if (notif.getType().equalsIgnoreCase("GROUP_INVITE")) {
            CreateRequestController.handleAcceptInvitation(notif.getTargetUuid(), this);
        } else if (notif.getType().equalsIgnoreCase("JOIN_REQUEST")) {
            CreateRequestController.handleAcceptJoinRequest(notif.getTargetUuid(), notif.getSender(), this);
        }
    }

    private String formatTime(String dateStr) {
        try {
            LocalDateTime date = LocalDateTime.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("MMM dd, h:mm a"));
        } catch (Exception e) {
            return dateStr;
        }
    }

    @FXML
    private void handleDeleteAllNotifications() {
        // TODO: Implement delete all
        notificationsList.setItems(NotificationManager.getNotifications());
        updateNotificationBadge();
    }

    @FXML
    private void goToPlanifyBot() {
        SceneManager.switchScene("planify-bot-view.fxml", "Planify AI Assistant");
    }

    private void updateNotificationBadge() {
        int unreadCount = 0; // NotificationManager.getUnreadCount()
        notificationBadge.setVisible(unreadCount > 0);
        notificationBadge.setManaged(unreadCount > 0);
        if (unreadCount > 0) notificationBadge.setText(String.valueOf(unreadCount));
    }

    @FXML
    private void handleLogout() {
        SceneManager.switchScene("login-view.fxml", "Login");
    }

    @FXML
    private void toggleNotifications() {
        boolean isVisible = notifPanel.isVisible();
        notifPanel.setVisible(!isVisible);
        notifPanel.setManaged(!isVisible);
    }

    public void setNotificationManagerParent() {
        NotificationManager.setParent(this);
    }

    public void refresh() {
        loadDashboardData();
    }

    private void loadDashboardData() {
        DashboardSummary.init(this);
    }

   public void populateUI() {
        populateWelcomeMessage();
        populateStatistics();
        populateUpcomingEvents();
        populateRecentTasks();
        populateActiveProjects();
        populateProductivityCircle();
    }

    private void populateWelcomeMessage() {
        welcomeLabel.setText("Welcome, " + LocalDataManager.getUserName() + "! 👋");
    }

    private void populateStatistics() {
        eventCountLabel.setText(String.valueOf(DashboardSummary.getTotalEvents()));
        todoRatioLabel.setText(DashboardSummary.getTotalTodo() + "/" + DashboardSummary.getTotalTodo());
        groupCountLabel.setText(String.valueOf(DashboardSummary.getActiveGroups()));
        projectCountLabel.setText(String.valueOf(DashboardSummary.getActiveProjects()));
        deadlineCountLabel.setText(String.valueOf(DashboardSummary.getTotalDeadlines()));

        eventTrendLabel.setText("Today's events");
        completionRateLabel.setText(DashboardSummary.getWeeklyProductivitySummary().getCompletionRate() + "% completion");
        //groupTrendLabel.setText("Total members: " + DashboardSummary.getTotalGroupMembers());
        projectProgressLabel.setText("Avg progress: " + DashboardSummary.getAverageProjectProgress() + "%");
        urgentLabel.setText("Deadlines Today: " + DashboardSummary.getTotalDeadlines());
    }

    private void populateUpcomingEvents() {
        upcomingEventsList.getChildren().clear();
        List<EventGetRequest> events = DashboardSummary.getTopEvents();

        if (events.isEmpty()) {
            upcomingEventsList.getChildren().add(createEmptyLabel("No events scheduled for today 🎉"));
            return;
        }

        for (EventGetRequest event : events) {
            upcomingEventsList.getChildren().add(createEventItem(event));
        }
    }

    private HBox createEventItem(EventGetRequest event) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("event-item");
        item.setPadding(new Insets(12));

        LocalDateTime startTime = LocalDateTime.parse(event.getStartDateTime());
        String timeStr = startTime.format(timeFormatter);

        StackPane timeBox = new StackPane();
        timeBox.getStyleClass().addAll("time-box", "time-" + (event.getType().equalsIgnoreCase("deadline") ? "red" : "blue"));
        timeBox.setMinWidth(60);
        Label timeLabel = new Label(timeStr);
        timeLabel.getStyleClass().add("time-text");
        timeBox.getChildren().add(timeLabel);

        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label titleLabel = new Label(event.getTitle());
        titleLabel.getStyleClass().add("event-title");

        Label groupLabel = new Label(event.getGroup() != null ? event.getGroup().getName() : "Personal");
        groupLabel.getStyleClass().add("event-group");

        infoBox.getChildren().addAll(titleLabel, groupLabel);

        item.getChildren().addAll(timeBox, infoBox);
        return item;
    }

    private void populateRecentTasks() {
        recentTasksList.getChildren().clear();
        List<TaskDetails> tasks = DashboardSummary.getTopTasks();

        if (tasks.isEmpty()) {
            recentTasksList.getChildren().add(createEmptyLabel("No tasks due today ✨"));
            return;
        }

        for (TaskDetails task : tasks) {
            recentTasksList.getChildren().add(createTaskItem(task));
        }
    }

    private HBox createTaskItem(TaskDetails task) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("task-item");
        item.setPadding(new Insets(12));

        Region statusDot = new Region();
        statusDot.getStyleClass().addAll("status-dot", "status-" + task.getStatus().toLowerCase());
        statusDot.setMinSize(8, 8);

        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("task-title");
        if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
            titleLabel.getStyleClass().add("task-completed");
        }

        String dueStr = formatDueDate(task.getDueDate());
        Label metaLabel = new Label("Due: " + dueStr);
        metaLabel.getStyleClass().add("task-meta");

        infoBox.getChildren().addAll(titleLabel, metaLabel);

        Label statusLabel = new Label(task.getStatus());
        statusLabel.getStyleClass().addAll("task-status-badge", "badge-" + task.getStatus().toLowerCase());

        item.getChildren().addAll(statusDot, infoBox, statusLabel);
        return item;
    }

    private void populateActiveProjects() {
        activeProjectsList.getChildren().clear();
        List<ProjectDetails> projects = DashboardSummary.getTopProjects();

        if (projects.isEmpty()) {
            activeProjectsList.getChildren().add(createEmptyLabel("No active projects 📋"));
            return;
        }

        for (ProjectDetails project : projects) {
            activeProjectsList.getChildren().add(createProjectItem(project));
        }
    }

    private VBox createProjectItem(ProjectDetails project) {
        VBox item = new VBox(8);
        item.getStyleClass().add("project-item");
        item.setPadding(new Insets(12));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(project.getName());
        titleLabel.getStyleClass().add("project-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label progressLabel = new Label(project.getProgress() + "%");
        progressLabel.getStyleClass().add("project-progress-label");

        header.getChildren().addAll(titleLabel, progressLabel);

        StackPane progressBar = new StackPane();
        progressBar.getStyleClass().add("project-progress-bar");
        progressBar.setPrefHeight(6);

        Rectangle bg = new Rectangle();
        bg.setHeight(6);
        bg.widthProperty().bind(progressBar.widthProperty());
        bg.getStyleClass().add("progress-bar-bg");

        Rectangle fill = new Rectangle();
        fill.setHeight(6);
        fill.widthProperty().bind(progressBar.widthProperty().multiply(project.getProgress() / 100.0));
        fill.getStyleClass().add("progress-bar-fill");

        progressBar.getChildren().addAll(bg, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        Label tasksLabel = new Label(project.getCompletedTasks() + "/" + project.getTotalTasks() + " tasks completed");
        tasksLabel.getStyleClass().add("project-tasks-label");

        item.getChildren().addAll(header, progressBar, tasksLabel);
        return item;
    }

    private void populateProductivityCircle() {
        DashboardWeeklyProductivitySummary summary = DashboardSummary.getWeeklyProductivitySummary();

        int completed = summary.getCompleted();
        int inProgress = summary.getInProgress();
        int pending = summary.getPending();
        int total = summary.getTotalTasks();

        productivityPercentLabel.setText(summary.getCompletionRate() + "%");
        completedThisWeekLabel.setText(String.valueOf(completed));
        inProgressThisWeekLabel.setText(String.valueOf(inProgress));
        pendingThisWeekLabel.setText(String.valueOf(pending));
        weightedProgressLabel.setText(summary.getWeightedCompletionRate() + "%");

        drawProductivityCircle(summary.getCompletionRate());
    }

    private void drawProductivityCircle(double percent) {
        GraphicsContext gc = productivityCircle.getGraphicsContext2D();
        double width = productivityCircle.getWidth();
        double height = productivityCircle.getHeight();
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = 65;
        double lineWidth = 12;

        gc.clearRect(0, 0, width, height);

        gc.setStroke(Color.rgb(226, 232, 240));
        gc.setLineWidth(lineWidth);
        gc.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90, 360, javafx.scene.shape.ArcType.OPEN);

        double progressAngle = (percent / 100.0) * 360;
        gc.setStroke(Color.web("#457b9d"));
        gc.setLineWidth(lineWidth);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90, -progressAngle, javafx.scene.shape.ArcType.OPEN);
    }

    private String formatDueDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "No date";
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalDate today = LocalDate.now();
            if (date.equals(today)) return "Today";
            if (date.equals(today.plusDays(1))) return "Tomorrow";
            return date.format(dateFormatter);
        } catch (Exception e) {
            return dateStr;
        }
    }


    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state-label");
        return label;
    }

    // ========== EVENT HANDLERS ==========
    @FXML private void goToSchedules() { SceneManager.switchScene("schedule-view.fxml", "Schedules & Events"); }
    @FXML private void goToTodos() { SceneManager.switchScene("todo-view.fxml", "Task Management"); }
    @FXML private void goToGroups() { SceneManager.switchScene("group-view.fxml", "Groups"); }
    @FXML private void goToProjects() { SceneManager.switchScene("project-view.fxml", "Collaborative Projects"); }
    @FXML private void goToAnalytics() { SceneManager.switchScene("analytics-view.fxml", "Analytics"); }
}