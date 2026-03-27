package com.planify.frontend.controllers.task;

import com.planify.frontend.controllers.Request.EditRequestController;
import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.SceneParent;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.models.project.ProjectSummary;
import com.planify.frontend.models.tasks.PersonalTaskResponse;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import com.planify.frontend.utils.data.personal.TaskDataManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TodoController extends SceneParent implements Initializable {

    // ========== NAVBAR ==========
    @FXML private Label navTotalLabel;
    @FXML private Label navCompletedLabel;
    @FXML private Label navProgressLabel;
    @FXML private Button notificationBtn;
    @FXML private VBox notificationPanel;
    @FXML private VBox notificationList;

    // ========== FILTER BAR ==========
    @FXML private Button personalTasksBtn;
    @FXML private Button projectTasksBtn;
    @FXML private ComboBox<String> timeFilterCombo;
    @FXML private VBox projectFilterBox;
    @FXML private ComboBox<String> projectFilterCombo;
    @FXML private ComboBox<String> milestoneFilterCombo;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private CheckBox showCompletedCheck;
    @FXML private ComboBox<String> dailyTodoCombo;

    // ========== KANBAN COLUMNS ==========
    @FXML private VBox pendingContainer;
    @FXML private VBox inProgressContainer;
    @FXML private VBox completedContainer;
    @FXML private VBox completedColumn;
    @FXML private Label pendingBadge;
    @FXML private Label inProgressBadge;
    @FXML private Label completedBadge;

    // ========== DATA ==========
    private final List<TaskDetails> allTasks = new ArrayList<>();
    private final List<PersonalTaskResponse> personalTasks = new ArrayList<>();
    private List<ProjectSummary> projectSummaries = new ArrayList<>();

    private String currentView = "personal"; // "personal" or "project"
    private String timeFilter = "All Time";
    private String projectFilter = "All Projects";
    private String milestoneFilter = "All Milestones";
    private String categoryFilter = "All Categories";
    private boolean showCompleted = true;

    @FXML private Circle connectionStatusCircle;

    @FXML private Label connectionStatusLabel;
    @FXML private ListView<NotificationResponse>notificationsList;
    @FXML public Button notifBtn;
    @FXML public VBox notifPanel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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

        init();
        NotificationManager.setParent(this);
        setupFilters();
        setupViewToggle();
        loadData();
        applyFilters();
    }

    public void init(){
        NotificationController.setStatusControls(connectionStatusCircle,connectionStatusLabel);
    }

    // ========== INITIALIZATION ==========

    private void setupFilters() {
        // Time Filter
        timeFilterCombo.getItems().addAll("All Time", "Today", "This Week", "This Month");
        timeFilterCombo.setValue("All Time");
        timeFilterCombo.setOnAction(e -> {
            timeFilter = timeFilterCombo.getValue();
            applyFilters();
        });

        // Milestone Filter
        milestoneFilterCombo.getItems().add("All Milestones");
        milestoneFilterCombo.setValue("All Milestones");
        milestoneFilterCombo.setOnAction(e -> {
            milestoneFilter = milestoneFilterCombo.getValue();
            applyFilters();
        });

        // Category Filter
        categoryFilterCombo.getItems().add("All Categories");
        categoryFilterCombo.setValue("All Categories");
        categoryFilterCombo.setOnAction(e -> {
            categoryFilter = categoryFilterCombo.getValue();
            applyFilters();
        });

        // Show Completed
        showCompletedCheck.setSelected(true);
        showCompletedCheck.setOnAction(e -> {
            showCompleted = showCompletedCheck.isSelected();
            completedColumn.setVisible(showCompleted);
            completedColumn.setManaged(showCompleted);
            applyFilters();
        });

        // Project Filter
        projectFilterCombo.getItems().add("All Projects");
        projectFilterCombo.setValue("All Projects");
        projectFilterCombo.setOnAction(e -> {
            projectFilter = projectFilterCombo.getValue();
            applyFilters();
        });

        dailyTodoCombo.setValue("All Tasks");
        dailyTodoCombo.setOnAction(e -> {
            applyFilters();
        });
    }

    private void setupViewToggle() {
        personalTasksBtn.getStyleClass().add("active");

        personalTasksBtn.setOnAction(e -> {
            currentView = "personal";
            personalTasksBtn.getStyleClass().add("active");
            projectTasksBtn.getStyleClass().remove("active");
            projectFilterBox.setVisible(false);
            projectFilterBox.setManaged(false);
            applyFilters();
        });

        projectTasksBtn.setOnAction(e -> {
            currentView = "project";
            projectTasksBtn.getStyleClass().add("active");
            personalTasksBtn.getStyleClass().remove("active");
            projectFilterBox.setVisible(true);
            projectFilterBox.setManaged(true);
            applyFilters();
        });
    }

    // ========== DATA LOADING ==========

    private void loadData() {
        loadPersonalTasks();
        loadProjectTasks();
        loadProjects();
        loadMilestones();
        loadCategories();
    }

    private void loadPersonalTasks() {
       allTasks.addAll(TaskDataManager.getAllPersonalTasks());
    }

    /**
     * TODO: Backend Integration - Load project tasks
     */
    private void loadProjectTasks() {
        // TODO: Load from backend
        allTasks.addAll(GroupProjectDataManager.getAllTasks());

        // Load from local storage
        allTasks.addAll(ProjectDataManager.getAllTasks());
        // Call: GET /api/tasks/projects
    }

    /**
     * TODO: Backend Integration - Load projects
     */
    private void loadProjects() {
        projectSummaries.clear();

        projectSummaries = GroupProjectDataManager.getGroupProjectSummary();

        projectSummaries.addAll(ProjectDataManager.getPersonalProjectSummary());
        updateProjectFilter();

        System.out.println("TODO: Fetch user projects from backend");
        // Call: GET /api/projects/user
    }

    private void updateProjectFilter() {
        projectFilterCombo.getItems().clear();
        projectFilterCombo.getItems().add("All Projects");

        for (ProjectSummary project : projectSummaries) {
            projectFilterCombo.getItems().add(project.getName());
        }

        projectFilterCombo.setValue("All Projects");
    }

    private void loadMilestones() {
        milestoneFilterCombo.getItems().clear();
        milestoneFilterCombo.getItems().add("All Milestones");

        // Get unique milestones from tasks
        Set<String> milestones = allTasks.stream()
                .map(TaskDetails::getMilestoneName)
                .filter(Objects::nonNull)
                .filter(m -> !m.isBlank())
                .collect(Collectors.toSet());

        milestoneFilterCombo.getItems().addAll(milestones);
        milestoneFilterCombo.setValue("All Milestones");

        System.out.println("TODO: Fetch milestones from backend");
        // Call: GET /api/milestones/all
    }

    private void loadCategories() {
        categoryFilterCombo.getItems().clear();
        categoryFilterCombo.getItems().add("All Categories");

        // Get unique categories from tasks
        Set<String> categories = allTasks.stream()
                .map(TaskDetails::getCategory)
                .filter(Objects::nonNull)
                .filter(c -> !c.isBlank())
                .collect(Collectors.toSet());

        categoryFilterCombo.getItems().addAll(categories);
        categoryFilterCombo.setValue("All Categories");

        System.out.println("TODO: Fetch categories from backend");
        // Call: GET /api/categories/all
    }

    /**
     * TODO: Backend Integration - Load notifications
     */

    // ========== FILTERING ==========

    @FXML
    private void clearFilters() {
        timeFilterCombo.setValue("All Time");
        projectFilterCombo.setValue("All Projects");
        milestoneFilterCombo.setValue("All Milestones");
        categoryFilterCombo.setValue("All Categories");
        showCompletedCheck.setSelected(true);

        timeFilter = "All Time";
        projectFilter = "All Projects";
        milestoneFilter = "All Milestones";
        categoryFilter = "All Categories";
        showCompleted = true;

        completedColumn.setVisible(true);
        completedColumn.setManaged(true);

        applyFilters();
    }

    private void applyFilters() {
        List<TaskDetails> filtered = allTasks.stream()
                .filter(this::matchesViewFilter)
                .filter(this::matchesTimeFilter)
                .filter(this::matchesDailyTodoFilter)  // Add this
                .filter(this::matchesProjectFilter)
                .filter(this::matchesMilestoneFilter)
                .filter(this::matchesCategoryFilter)
                .filter(this::matchesCompletedFilter)
                .collect(Collectors.toList());

        populateKanban(filtered);
        updateNavStats(filtered);
    }

    private boolean matchesViewFilter(TaskDetails task) {
        if ("personal".equals(currentView)) {
            return task.getProjectName() == null || task.getProjectName().isBlank();
        } else {
            return task.getProjectName() != null && !task.getProjectName().isBlank();
        }
    }

    private boolean matchesTimeFilter(TaskDetails task) {
        if ("All Time".equals(timeFilter)) {
            return true;
        }

        if (task.getDueDate() == null || task.getDueDate().isBlank()) {
            return false;
        }

        try {
            LocalDate dueDate = LocalDate.parse(task.getDueDate());
            LocalDate today = LocalDate.now();

            switch (timeFilter) {
                case "Today":
                    return dueDate.equals(today);
                case "This Week":
                    LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
                    LocalDate weekEnd = weekStart.plusDays(6);
                    return !dueDate.isBefore(weekStart) && !dueDate.isAfter(weekEnd);
                case "This Month":
                    return dueDate.getYear() == today.getYear() &&
                            dueDate.getMonth() == today.getMonth();
                default:
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchesProjectFilter(TaskDetails task) {
        if ("All Projects".equals(projectFilter)) {
            return true;
        }
        return projectFilter.equals(task.getProjectName());
    }

    private boolean matchesMilestoneFilter(TaskDetails task) {
        if ("All Milestones".equals(milestoneFilter)) {
            return true;
        }
        return milestoneFilter.equals(task.getMilestoneName());
    }

    private boolean matchesCategoryFilter(TaskDetails task) {
        if ("All Categories".equals(categoryFilter)) {
            return true;
        }
        return categoryFilter.equals(task.getCategory());
    }

    private boolean matchesCompletedFilter(TaskDetails task) {
        if (showCompleted) {
            return true;
        }
        return !"COMPLETED".equalsIgnoreCase(task.getStatus());
    }

    private boolean matchesDailyTodoFilter(TaskDetails task) {
        String filter = dailyTodoCombo.getValue();
        if (filter == null || "All Tasks".equals(filter)) {
            return true;
        }

        boolean isDailyTask = isDailyTask(task);

        if ("Daily Tasks".equals(filter)) {
            return isDailyTask;
        } else if ("Long Tasks".equals(filter)) {
            return !isDailyTask;
        }

        return true;
    }

    // Helper method to determine if task is daily (duration <= 24 hours)
    private boolean isDailyTask(TaskDetails task) {
        if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
            return false;
        }

        try {
            // If task has start date and due date, check duration
            /*if (task.getStartDate() != null && !task.getStartDate().isEmpty()) {

            }*/
            LocalDate startDate = LocalDate.now();
            LocalDate dueDate = LocalDate.parse(task.getDueDate());
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, dueDate);
            return daysBetween <= 1;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== KANBAN POPULATION ==========

    private void populateKanban(List<TaskDetails> tasks) {
        pendingContainer.getChildren().clear();
        inProgressContainer.getChildren().clear();
        completedContainer.getChildren().clear();

        int pendingCount = 0;
        int progressCount = 0;
        int completedCount = 0;

        for (TaskDetails task : tasks) {
            VBox card = loadTaskCard(task);
            if (card == null) continue;

            String status = task.getStatus().toUpperCase();
            switch (status) {
                case "PENDING":
                    pendingContainer.getChildren().add(card);
                    pendingCount++;
                    break;
                case "IN_PROGRESS":
                    inProgressContainer.getChildren().add(card);
                    progressCount++;
                    break;
                case "COMPLETED":
                    completedContainer.getChildren().add(card);
                    completedCount++;
                    break;
            }
        }

        pendingBadge.setText(String.valueOf(pendingCount));
        inProgressBadge.setText(String.valueOf(progressCount));
        completedBadge.setText(String.valueOf(completedCount));
    }

    private VBox loadTaskCard(TaskDetails task) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/todo-card-view.fxml"));
            VBox card = loader.load();

            TodoCardController controller = loader.getController();
            controller.setData(task, this::advanceTaskState, this);

            return card;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateNavStats(List<TaskDetails> tasks) {
        int total = tasks.size();
        long completed = tasks.stream().filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus())).count();
        long progress = tasks.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();

        navTotalLabel.setText(String.valueOf(total));
        navCompletedLabel.setText(String.valueOf(completed));
        navProgressLabel.setText(String.valueOf(progress));
    }

    // ========== TASK ACTIONS ==========

    private void advanceTaskState(TaskDetails task) {
        String currentStatus = task.getStatus().toUpperCase();

        switch (currentStatus) {
            case "PENDING":
                task.setStatus("IN_PROGRESS");
                break;
            case "IN_PROGRESS":
                task.setStatus("COMPLETED");
                break;
            case "COMPLETED":
                // Already completed, no action
                return;
        }

        if(task.getUuid().trim().isEmpty()){
            TaskDataManager.updatePersonalTaskStatus(task.getTitle(),task.getStatus());
        }
        else EditRequestController.updateTaskStatus(task.getUuid(),task.getStatus(),this);

        applyFilters();
    }


    public void showTaskDetails(TaskDetails task) {
        // Create dialog with full task details
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Task Details");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/TodoProfessional.css").toExternalForm()
        );

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("task-detail-dialog");

        // Title
        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("detail-title");
        titleLabel.setWrapText(true);

        // Status
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        Label statusLabel = new Label("Status:");
        statusLabel.getStyleClass().add("detail-label");
        Label statusValue = new Label(task.getStatus());
        statusValue.getStyleClass().addAll("detail-value", "status-badge", "status-" + task.getStatus().toLowerCase());
        statusBox.getChildren().addAll(statusLabel, statusValue);

        content.getChildren().addAll(titleLabel, new Separator(), statusBox);

        // Description
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            addDetailRow(content, "Description:", task.getDescription());
        }

        // Due Date
        if (task.getDueDate() != null && !task.getDueDate().isBlank()) {
            addDetailRow(content, "Due Date:", formatDate(task.getDueDate()));
        }

        // Category
        if (task.getCategory() != null && !task.getCategory().isBlank()) {
            addDetailRow(content, "Category:", task.getCategory());
        }

        // Milestone
        if (task.getMilestoneName() != null && !task.getMilestoneName().isBlank()) {
            addDetailRow(content, "Milestone:", task.getMilestoneName());
        }

        // Project
        if (task.getProjectName() != null && !task.getProjectName().isBlank()) {
            addDetailRow(content, "Project:", task.getProjectName());
        }

        // Creator
        if (task.getCreator() != null) {
            addDetailRow(content, "Created By:",
                    task.getCreator().getName() + " (" + task.getCreator().getEmail() + ")");
        }

        // Assignees
        if (task.getAssigneeMembers() != null && !task.getAssigneeMembers().isEmpty()) {
            String assignees = task.getAssigneeMembers().stream()
                    .map(MemberInfo::getName)
                    .collect(Collectors.joining(", "));
            addDetailRow(content, "Assignees:", assignees);
        }

        // Attachment
        if (task.getAttachmentUrl() != null && !task.getAttachmentUrl().isBlank()) {
            Hyperlink link = new Hyperlink(task.getAttachmentUrl());
            link.setOnAction(e -> {
                // TODO: Open URL in browser
                System.out.println("Open attachment: " + task.getAttachmentUrl());
            });
            VBox attachBox = new VBox(5);
            Label attachLabel = new Label("Attachment:");
            attachLabel.getStyleClass().add("detail-label");
            attachBox.getChildren().addAll(attachLabel, link);
            content.getChildren().add(attachBox);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);

        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Edit", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CLOSE
        );

        dialog.showAndWait().ifPresent(response -> {
            if (response.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                showEditTaskDialog(task);
            }
        });
    }

    private void addDetailRow(VBox container, String label, String value) {
        VBox row = new VBox(5);
        Label labelWidget = new Label(label);
        labelWidget.getStyleClass().add("detail-label");
        Label valueWidget = new Label(value);
        valueWidget.getStyleClass().add("detail-value");
        valueWidget.setWrapText(true);
        row.getChildren().addAll(labelWidget, valueWidget);
        container.getChildren().add(row);
    }

    public void showEditTaskDialog(TaskDetails task) {
        // TODO: Open edit dialog similar to AddTask
        // For now, show alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Edit Task");
        alert.setHeaderText("Edit: " + task.getTitle());
        alert.setContentText("Edit dialog will be implemented with full task editing capabilities.");
        alert.showAndWait();

        System.out.println("TODO: Implement edit task dialog");
    }

    private String formatDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }

    // ========== EVENT HANDLERS ==========

    @FXML
    private void toggleNotifications() {
        boolean isVisible = notificationPanel.isVisible();
        notificationPanel.setVisible(!isVisible);
        notificationPanel.setManaged(!isVisible);
    }

    @FXML
    private void openAddTask() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/add-todo-view.fxml"));
            Parent root = loader.load();

            AddTodoController controller = loader.getController();
            controller.setContext(projectSummaries, this);

            Stage stage = new Stage();
            stage.setTitle("Add Task");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Refresh after adding
            loadData();
            applyFilters();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        SceneManager.switchScene("dashboard-view.fxml", "Dashboard");
    }

    @FXML
    private void handleLogout() {
        //LocalDataManager.clearUserData();
        SceneManager.switchScene("login-view.fxml", "Login");
    }

    public void refresh() {
        loadData();
        applyFilters();
    }

    // ========== DATA CLASSES ==========

}
