package com.planify.frontend.controllers.project;

import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.models.auth.MemberInfo;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.animation.*;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ProjectProgressController implements Initializable {

    @FXML private Label overallProgressLabel;
    @FXML private Label tasksCompletedLabel;
    @FXML private Label weightProgressLabel;
    @FXML private Label teamSizeLabel;
    @FXML private Label circleProgressLabel;
    @FXML private Label progressTrendLabel;
    @FXML private Label completionRateLabel;
    @FXML private Label avgWeightLabel;
    @FXML private Label activeMembersLabel;
    @FXML private Label healthScoreLabel;
    @FXML private Label totalMilestonesLabel;
    @FXML private Label completedMilestonesLabel;
    @FXML private Label inProgressMilestonesLabel;
    @FXML private Label pendingMilestonesLabel;

    @FXML private ProgressBar completedProgressBar;
    @FXML private ProgressBar inProgressProgressBar;
    @FXML private ProgressBar pendingProgressBar;

    @FXML private Canvas progressCircleCanvas;
    @FXML private VBox milestoneTimeline;
    @FXML private VBox categoryContainer;
    @FXML private VBox teamContainer;
    @FXML private HBox phaseBarsContainer;

    private ProjectDetails projectDetails;
    private ProjectDetailsController projectDetailsController;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialization
    }

    public void setProjectDetailsController(ProjectDetailsController projectDetailsController) {
        this.projectDetailsController = projectDetailsController;
    }

    public void setProjectDetails(ProjectDetails details) {
        this.projectDetails = details;
        populateAllData();
    }

    private void populateAllData() {
        populateHeroStats();
        drawCircularProgress();
        populateMilestoneTimeline();
        populateCategories();
        populateTeam();
        animatePhaseBars();
        calculateHealthScore();
    }

    private void populateHeroStats() {
        int totalTasks = projectDetails.getTotalTasks();
        int completedTasks = projectDetails.getCompletedTasks();
        int completionRate = totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0;

        overallProgressLabel.setText(projectDetails.getProgress() + "%");
        circleProgressLabel.setText(projectDetails.getProgress() + "%");

        tasksCompletedLabel.setText(completedTasks + "/" + totalTasks);
        completionRateLabel.setText(completionRate + "% completion");

        // Weight calculations
        int totalWeight = calculateTotalWeight();
        int completedWeight = calculateCompletedWeight();
        weightProgressLabel.setText(completedWeight + "/" + totalWeight);
        avgWeightLabel.setText("Avg: " + (totalTasks > 0 ? totalWeight / totalTasks : 0));

        teamSizeLabel.setText(String.valueOf(projectDetails.getTotalMembers()));

        int activeMembers = calculateActiveMembers();
        activeMembersLabel.setText("Active: " + activeMembers);

        int totalMilestones = projectDetails.getTotalMilestones();
        int completedMilestones = projectDetails.getCompletedMilestones();
        int inProgressMilestones = projectDetails.getInProgressMilestones();
        int pendingMilestones = projectDetails.getPendingMilestones();

        totalMilestonesLabel.setText(totalMilestones + " Total");
        completedMilestonesLabel.setText(String.valueOf(completedMilestones));
        inProgressMilestonesLabel.setText(String.valueOf(inProgressMilestones));
        pendingMilestonesLabel.setText(String.valueOf(pendingMilestones));

        // Update progress bars
        completedProgressBar.setProgress(totalMilestones > 0 ? (double) completedMilestones / totalMilestones : 0);
        inProgressProgressBar.setProgress(totalMilestones > 0 ? (double) inProgressMilestones / totalMilestones : 0);
        pendingProgressBar.setProgress(totalMilestones > 0 ? (double) pendingMilestones / totalMilestones : 0);

        // Calculate progress trend (mock - would compare with previous week)
        progressTrendLabel.setText("↑ " + (projectDetails.getProgress() / 10) + "% this week");
    }

    private int calculateTotalWeight() {
        int total = 0;
        for (MilestoneDetails milestone : projectDetails.getMilestones()) {
            for (TaskDetails task : milestone.getTasks()) {
                total += getTaskWeight(task);
            }
        }
        return total;
    }

    private int calculateCompletedWeight() {
        int completed = 0;
        for (MilestoneDetails milestone : projectDetails.getMilestones()) {
            for (TaskDetails task : milestone.getTasks()) {
                if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
                    completed += getTaskWeight(task);
                }
            }
        }
        return completed;
    }

    private int calculateActiveMembers() {
        // Count members with any activity in last 7 days
        // For now, return all members as active
        return projectDetails.getMembers() != null ? projectDetails.getMembers().size() : 0;
    }

    private void calculateHealthScore() {
        int progress = projectDetails.getProgress();
        int completionRate = projectDetails.getTotalTasks() > 0 ?
                (projectDetails.getCompletedTasks() * 100 / projectDetails.getTotalTasks()) : 0;
        int milestoneRate = projectDetails.getTotalMilestones() > 0 ?
                (projectDetails.getCompletedMilestones() * 100 / projectDetails.getTotalMilestones()) : 0;

        int score = (progress + completionRate + milestoneRate) / 3;
        healthScoreLabel.setText("Score: " + score + "/100");

        // Color based on score
        String color;
        if (score >= 80) color = "#10b981";
        else if (score >= 50) color = "#f59e0b";
        else color = "#e63946";
        healthScoreLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    private void drawCircularProgress() {
        GraphicsContext gc = progressCircleCanvas.getGraphicsContext2D();
        double width = progressCircleCanvas.getWidth();
        double height = progressCircleCanvas.getHeight();
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = 90;
        double lineWidth = 16;

        gc.clearRect(0, 0, width, height);

        // Draw background circle
        gc.setStroke(Color.rgb(226, 232, 240));
        gc.setLineWidth(lineWidth);
        gc.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90, 360, javafx.scene.shape.ArcType.OPEN);

        // Draw progress arc with gradient
        double progressAngle = (projectDetails.getProgress() / 100.0) * 360;
        gc.setStroke(Color.web("#457b9d"));
        gc.setLineWidth(lineWidth);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90, -progressAngle, javafx.scene.shape.ArcType.OPEN);
    }

    private void populateMilestoneTimeline() {
        milestoneTimeline.getChildren().clear();

        for (int i = 0; i < projectDetails.getMilestones().size(); i++) {
            MilestoneDetails milestone = projectDetails.getMilestones().get(i);
            if (milestone.getTitle().equals("Uncategorized")) continue;

            HBox item = new HBox(15);
            item.setAlignment(Pos.TOP_LEFT);
            item.getStyleClass().add("milestone-item");

            // Badge
            StackPane badge = new StackPane();
            badge.setMinSize(44, 44);
            badge.setMaxSize(44, 44);
            badge.getStyleClass().add("milestone-badge");

            String status = determineStatus(milestone);
            Label badgeLabel = new Label();

            if ("completed".equals(status)) {
                badge.getStyleClass().add("milestone-badge-completed");
                badgeLabel.setText("✓");
            } else if ("in-progress".equals(status)) {
                badge.getStyleClass().add("milestone-badge-progress");
                badgeLabel.setText(String.valueOf(i + 1));
            } else {
                badge.getStyleClass().add("milestone-badge-pending");
                badgeLabel.setText(String.valueOf(i + 1));
            }

            badgeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
            badge.getChildren().add(badgeLabel);

            // Card
            VBox card = new VBox(12);
            card.getStyleClass().add("milestone-card");
            HBox.setHgrow(card, Priority.ALWAYS);

            // Header
            HBox header = new HBox(20);
            header.setAlignment(Pos.CENTER_LEFT);

            VBox titleBox = new VBox(8);
            HBox.setHgrow(titleBox, Priority.ALWAYS);

            Label title = new Label(milestone.getTitle());
            title.getStyleClass().add("milestone-title");

            HBox infoBox = new HBox(20);
            Label dateInfo = new Label("📅 Due: " + formatDate(milestone.getDeadline()));
            dateInfo.getStyleClass().add("milestone-info");
            int weight = calculateMilestoneWeight(milestone);
            Label weightInfo = new Label("⚡ Weight: " + weight);
            weightInfo.getStyleClass().add("milestone-info");
            infoBox.getChildren().addAll(dateInfo, weightInfo);

            titleBox.getChildren().addAll(title, infoBox);

            VBox progressBox = new VBox(5);
            progressBox.setAlignment(Pos.CENTER_RIGHT);
            Label progressLabel = new Label(milestone.getCompletionRate() + "%");
            progressLabel.getStyleClass().add("milestone-progress-value");
            progressBox.getChildren().add(progressLabel);

            header.getChildren().addAll(titleBox, progressBox);

            // Progress bar
            StackPane progressBarContainer = new StackPane();
            progressBarContainer.getStyleClass().add("progress-bar-container");
            progressBarContainer.setPrefHeight(8);
            progressBarContainer.setMaxWidth(Double.MAX_VALUE);

            Rectangle progressBar = new Rectangle();
            progressBar.setHeight(8);
            progressBar.widthProperty().bind(progressBarContainer.widthProperty().multiply(milestone.getCompletionRate() / 100.0));
            progressBar.setArcWidth(6);
            progressBar.setArcHeight(6);
            progressBar.getStyleClass().add("progress-bar-fill");

            progressBarContainer.getChildren().add(progressBar);
            StackPane.setAlignment(progressBar, Pos.CENTER_LEFT);

            card.getChildren().addAll(header, progressBarContainer);
            item.getChildren().addAll(badge, card);
            milestoneTimeline.getChildren().add(item);
        }
    }

    private int calculateMilestoneWeight(MilestoneDetails milestone) {
        int weight = 0;
        for (TaskDetails task : milestone.getTasks()) {
            weight += getTaskWeight(task);
        }
        return weight;
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "Not set";
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(dateFormatter);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String determineStatus(MilestoneDetails milestone) {
        if (milestone.getCompletionRate() >= 100) {
            return "completed";
        } else if (milestone.getCompletionRate() > 0) {
            return "in-progress";
        } else {
            return "pending";
        }
    }

    private void populateCategories() {
        categoryContainer.getChildren().clear();

        Map<String, CategoryAnalytics> categoryMap = new LinkedHashMap<>();

        for (MilestoneDetails milestone : projectDetails.getMilestones()) {
            for (TaskDetails task : milestone.getTasks()) {
                String category = task.getCategory() != null && !task.getCategory().isEmpty() ?
                        task.getCategory().toUpperCase() : "UNCATEGORIZED";
                int weight = getTaskWeight(task);

                CategoryAnalytics analytics = categoryMap.computeIfAbsent(category, CategoryAnalytics::new);
                analytics.addTask(task, weight);
            }
        }

        String[] colors = {"#457b9d", "#10b981", "#f59e0b", "#e63946", "#8b5cf6", "#06b6d4"};
        int index = 0;

        for (CategoryAnalytics cat : categoryMap.values()) {
            String color = colors[index % colors.length];
            VBox item = createCategoryItem(cat, color);
            categoryContainer.getChildren().add(item);
            index++;
        }
    }

    private VBox createCategoryItem(CategoryAnalytics cat, String color) {
        VBox item = new VBox(10);
        item.getStyleClass().add("category-item");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Region dot = new Region();
        dot.setMinSize(12, 12);
        dot.setMaxSize(12, 12);
        dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6;");

        Label name = new Label(cat.name);
        name.getStyleClass().add("category-name");
        HBox.setHgrow(name, Priority.ALWAYS);

        int percentage = cat.totalWeight > 0 ? (cat.completedWeight * 100 / cat.totalWeight) : 0;
        Label stats = new Label(cat.completedWeight + "/" + cat.totalWeight + " weight (" + percentage + "%)");
        stats.getStyleClass().add("category-stats");

        header.getChildren().addAll(dot, name, stats);

        // Progress bar
        StackPane barContainer = new StackPane();
        barContainer.setPrefHeight(8);
        barContainer.setMaxWidth(Double.MAX_VALUE);

        Rectangle bg = new Rectangle();
        bg.setHeight(8);
        bg.widthProperty().bind(barContainer.widthProperty());
        bg.setFill(Color.web("#e2e8f0"));
        bg.setArcWidth(4);
        bg.setArcHeight(4);

        Rectangle fill = new Rectangle();
        fill.setHeight(8);
        fill.widthProperty().bind(barContainer.widthProperty().multiply(percentage / 100.0));
        fill.setFill(Color.web(color));
        fill.setArcWidth(4);
        fill.setArcHeight(4);

        barContainer.getChildren().addAll(bg, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        Label taskCount = new Label(cat.totalTasks + " tasks | " + cat.completedTasks + " completed");
        taskCount.getStyleClass().add("category-task-count");

        item.getChildren().addAll(header, barContainer, taskCount);
        return item;
    }

    private int getTaskWeight(TaskDetails task) {
        // Get weight from task if available, otherwise based on priority
        try {
            return task.getWeight();
        } catch (Exception e) {
            return 5;
        }
    }

    private void populateTeam() {
        teamContainer.getChildren().clear();

        if (projectDetails.getMembers() == null || projectDetails.getMembers().isEmpty()) {
            Label emptyLabel = new Label("No team members");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 20;");
            teamContainer.getChildren().add(emptyLabel);
            return;
        }

        String[] avatars = {"👨‍💻", "👩‍💻", "👨‍🎨", "👩‍🔧", "👨‍💼", "👩‍🔬"};

        for (int i = 0; i < projectDetails.getMembers().size(); i++) {
            MemberInfo member = projectDetails.getMembers().get(i);
            VBox card = new VBox(10);
            card.getStyleClass().add("team-member-card");

            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);

            StackPane avatar = new StackPane();
            avatar.setMinSize(44, 44);
            avatar.setMaxSize(44, 44);
            avatar.getStyleClass().add("team-avatar");
            Label avatarLabel = new Label(avatars[i % avatars.length]);
            avatarLabel.setStyle("-fx-font-size: 22px;");
            avatar.getChildren().add(avatarLabel);

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label name = new Label(member.getName());
            name.getStyleClass().add("team-name");

            Label email = new Label(member.getEmail());
            email.getStyleClass().add("team-tasks");

            info.getChildren().addAll(name, email);

            // Mock productivity - calculate based on tasks completed
            int tasksCompleted = calculateMemberTasksCompleted(member);
            int totalTasks = calculateMemberTotalTasks(member);
            int productivity = totalTasks > 0 ? (tasksCompleted * 100 / totalTasks) : 0;

            VBox prodBox = new VBox(2);
            prodBox.setAlignment(Pos.CENTER_RIGHT);
            Label productivityLabel = new Label(productivity + "%");
            productivityLabel.getStyleClass().add("team-productivity");
            Label effLabel = new Label("completed");
            effLabel.getStyleClass().add("team-efficiency-label");
            prodBox.getChildren().addAll(productivityLabel, effLabel);

            header.getChildren().addAll(avatar, info, prodBox);

            // Progress bar
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
            fill.widthProperty().bind(barContainer.widthProperty().multiply(productivity / 100.0));
            fill.setFill(Color.web("#457b9d"));
            fill.setArcWidth(4);
            fill.setArcHeight(4);

            barContainer.getChildren().addAll(bg, fill);
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);

            card.getChildren().addAll(header, barContainer);
            teamContainer.getChildren().add(card);
        }
    }

    private int calculateMemberTasksCompleted(MemberInfo member) {
        int completed = 0;
        for (MilestoneDetails milestone : projectDetails.getMilestones()) {
            for (TaskDetails task : milestone.getTasks()) {
                if (task.getAssigneeMembers() != null) {
                    for (MemberInfo assignee : task.getAssigneeMembers()) {
                        if (assignee.getEmail().equals(member.getEmail()) &&
                                "COMPLETED".equalsIgnoreCase(task.getStatus())) {
                            completed++;
                        }
                    }
                }
            }
        }
        return completed;
    }

    private int calculateMemberTotalTasks(MemberInfo member) {
        int total = 0;
        for (MilestoneDetails milestone : projectDetails.getMilestones()) {
            for (TaskDetails task : milestone.getTasks()) {
                if (task.getAssigneeMembers() != null) {
                    for (MemberInfo assignee : task.getAssigneeMembers()) {
                        if (assignee.getEmail().equals(member.getEmail())) {
                            total++;
                        }
                    }
                }
            }
        }
        return total;
    }

    private void animatePhaseBars() {
        phaseBarsContainer.getChildren().clear();

        try {
            long totalDays = ChronoUnit.DAYS.between(
                    LocalDateTime.parse(projectDetails.getStartDate()),
                    LocalDateTime.parse(projectDetails.getDeadline())
            );
            long phaseLength = Math.max(1, totalDays / 5);
            String[] phaseNames = {"Planning", "Development", "Testing", "Review", "Deployment"};

            for (int i = 0; i < Math.min(5, phaseNames.length); i++) {
                LocalDate phaseStart = LocalDateTime.parse(projectDetails.getStartDate()).toLocalDate().plusDays(i * phaseLength);
                LocalDate phaseEnd = (i == 4) ?
                        LocalDateTime.parse(projectDetails.getDeadline()).toLocalDate() :
                        LocalDateTime.parse(projectDetails.getStartDate()).toLocalDate().plusDays((i + 1) * phaseLength);

                double phaseProgress = calculatePhaseProgress(phaseStart, phaseEnd);

                VBox column = new VBox(8);
                column.setAlignment(Pos.CENTER);
                column.getStyleClass().add("phase-column");

                Label phaseLabel = new Label(phaseNames[i]);
                phaseLabel.getStyleClass().add("phase-label");

                StackPane barContainer = new StackPane();
                barContainer.setAlignment(Pos.BOTTOM_CENTER);
                barContainer.getStyleClass().add("phase-bar-container");

                Rectangle bgBar = new Rectangle(80, 200);
                bgBar.getStyleClass().add("phase-bar-bg");
                bgBar.setArcWidth(8);
                bgBar.setArcHeight(8);

                Rectangle fillBar = new Rectangle(80, 0);
                fillBar.getStyleClass().add("phase-bar-fill");
                fillBar.setArcWidth(8);
                fillBar.setArcHeight(8);

                Label valueLabel = new Label(String.format("%.0f%%", phaseProgress));
                valueLabel.getStyleClass().add("phase-bar-value");
                valueLabel.setTranslateY(-15);

                barContainer.getChildren().addAll(bgBar, fillBar, valueLabel);
                StackPane.setAlignment(valueLabel, Pos.BOTTOM_CENTER);

                column.getChildren().addAll(phaseLabel, barContainer);
                phaseBarsContainer.getChildren().add(column);

                double targetHeight = (phaseProgress / 100.0) * 200;
                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(fillBar.heightProperty(), 0)),
                        new KeyFrame(Duration.millis(800), new KeyValue(fillBar.heightProperty(), targetHeight))
                );
                timeline.setDelay(Duration.millis(i * 150));
                timeline.play();
            }
        } catch (Exception e) {
            // Handle missing dates
            Label errorLabel = new Label("Phase data unavailable");
            errorLabel.setStyle("-fx-text-fill: #94a3b8;");
            phaseBarsContainer.getChildren().add(errorLabel);
        }
    }

    private double calculatePhaseProgress(LocalDate start, LocalDate end) {
        var tasksInPhase = projectDetails.getMilestones().stream()
                .flatMap(m -> m.getTasks().stream())
                .filter(t -> {
                    try {
                        LocalDate due = LocalDate.parse(t.getDueDate());
                        return !due.isBefore(start) && !due.isAfter(end);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();

        if (tasksInPhase.isEmpty()) return 0;
        long completed = tasksInPhase.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .count();
        return (completed / (double) tasksInPhase.size()) * 100;
    }

    @FXML
    private void handleRefresh() {
        if (projectDetailsController != null) {
            projectDetailsController.refresh();
        }
    }

    @FXML
    private void handleAddTask() {
        if (projectDetailsController != null && projectDetails != null) {
            // This would open add task dialog - handled in parent
        }
    }
    

    private static class CategoryAnalytics {
        String name;
        int totalWeight;
        int completedWeight;
        int totalTasks;
        int completedTasks;

        CategoryAnalytics(String name) {
            this.name = name;
        }

        void addTask(TaskDetails task, int weight) {
            totalWeight += weight;
            totalTasks++;
            if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
                completedWeight += weight;
                completedTasks++;
            }
        }
    }
}