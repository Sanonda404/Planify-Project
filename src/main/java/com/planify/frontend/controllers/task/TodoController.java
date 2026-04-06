package com.planify.frontend.controllers.task;

import com.planify.frontend.controllers.Request.EditRequestController;
import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.SceneParent;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.models.project.ProjectSummary;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import com.planify.frontend.utils.data.personal.TaskDataManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @FXML private Button kanbanViewBtn;
    @FXML private Button listViewBtn;
    @FXML private HBox kanbanView;
    @FXML private VBox listView;
    @FXML private VBox listContainer;

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
    private List<ProjectSummary> projectSummaries = new ArrayList<>();

    private String currentView = "personal";
    private String timeFilter = "All Time";
    private String projectFilter = "All Projects";
    private String milestoneFilter = "All Milestones";
    private String categoryFilter = "All Categories";
    private boolean showCompleted = true;
    private boolean isKanbanView = true;

    @FXML private Circle connectionStatusCircle;
    @FXML private Label connectionStatusLabel;
    @FXML private ListView<NotificationResponse> notificationsList;
    @FXML public Button notifBtn;
    @FXML public VBox notifPanel;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupNotificationPanel();
        init();
        setupFilters();
        setupViewToggle();
        loadData();
        applyFilters();
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

    private void setupFilters() {
        timeFilterCombo.getItems().addAll("All Time", "Today", "This Week", "This Month");
        timeFilterCombo.setValue("All Time");
        timeFilterCombo.setOnAction(e -> { timeFilter = timeFilterCombo.getValue(); applyFilters(); });

        milestoneFilterCombo.getItems().add("All Milestones");
        milestoneFilterCombo.setValue("All Milestones");
        milestoneFilterCombo.setOnAction(e -> { milestoneFilter = milestoneFilterCombo.getValue(); applyFilters(); });

        categoryFilterCombo.getItems().add("All Categories");
        categoryFilterCombo.setValue("All Categories");
        categoryFilterCombo.setOnAction(e -> { categoryFilter = categoryFilterCombo.getValue(); applyFilters(); });

        showCompletedCheck.setSelected(true);
        showCompletedCheck.setOnAction(e -> {
            showCompleted = showCompletedCheck.isSelected();
            completedColumn.setVisible(showCompleted);
            completedColumn.setManaged(showCompleted);
            applyFilters();
        });

        projectFilterCombo.getItems().add("All Projects");
        projectFilterCombo.setValue("All Projects");
        projectFilterCombo.setOnAction(e -> { projectFilter = projectFilterCombo.getValue(); applyFilters(); });

        dailyTodoCombo.setItems(FXCollections.observableArrayList("All Tasks", "Daily Tasks", "Long Tasks"));
        dailyTodoCombo.setValue("All Tasks");
        dailyTodoCombo.setOnAction(e -> applyFilters());
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

    @FXML
    private void switchToKanbanView() {
        isKanbanView = true;
        kanbanView.setVisible(true);
        kanbanView.setManaged(true);
        listView.setVisible(false);
        listView.setManaged(false);
        kanbanViewBtn.getStyleClass().add("view-mode-active");
        listViewBtn.getStyleClass().remove("view-mode-active");
        applyFilters();
    }

    @FXML
    private void switchToListView() {
        isKanbanView = false;
        kanbanView.setVisible(false);
        kanbanView.setManaged(false);
        listView.setVisible(true);
        listView.setManaged(true);
        listViewBtn.getStyleClass().add("view-mode-active");
        kanbanViewBtn.getStyleClass().remove("view-mode-active");
        applyFilters();
    }

    private void loadData() {
        allTasks.clear();
        loadPersonalTasks();
        loadProjectTasks();
        loadProjects();
        loadMilestones();
        loadCategories();
    }

    private void loadPersonalTasks() {
        allTasks.addAll(TaskDataManager.getAllPersonalTasks()) ;
    }

    private void loadProjectTasks() {
        allTasks.addAll(GroupProjectDataManager.getAllTasks());
        allTasks.addAll(ProjectDataManager.getAllTasks());
    }

    private void loadProjects() {
        projectSummaries.clear();
        projectSummaries = GroupProjectDataManager.getGroupProjectSummary();
        projectSummaries.addAll(ProjectDataManager.getPersonalProjectSummary());
        updateProjectFilter();
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
        Set<String> milestones = allTasks.stream()
                .map(TaskDetails::getMilestoneName)
                .filter(Objects::nonNull)
                .filter(m -> !m.isBlank())
                .collect(Collectors.toSet());
        milestoneFilterCombo.getItems().addAll(milestones);
        milestoneFilterCombo.setValue("All Milestones");
    }

    private void loadCategories() {
        categoryFilterCombo.getItems().clear();
        categoryFilterCombo.getItems().add("All Categories");
        Set<String> categories = allTasks.stream()
                .map(TaskDetails::getCategory)
                .filter(Objects::nonNull)
                .filter(c -> !c.isBlank())
                .collect(Collectors.toSet());
        categoryFilterCombo.getItems().addAll(categories);
        categoryFilterCombo.setValue("All Categories");
    }

    @FXML
    private void clearFilters() {
        timeFilterCombo.setValue("All Time");
        projectFilterCombo.setValue("All Projects");
        milestoneFilterCombo.setValue("All Milestones");
        categoryFilterCombo.setValue("All Categories");
        showCompletedCheck.setSelected(true);
        dailyTodoCombo.setValue("All Tasks");
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
                .filter(this::matchesDailyTodoFilter)
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
        if ("All Time".equals(timeFilter)) return true;
        if (task.getDueDate() == null || task.getDueDate().isBlank()) return false;
        try {
            LocalDate dueDate = LocalDateTime.parse(task.getDueDate()).toLocalDate();
            LocalDate today = LocalDate.now();
            switch (timeFilter) {
                case "Today": return dueDate.equals(today);
                case "This Week":
                    LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
                    LocalDate weekEnd = weekStart.plusDays(6);
                    return !dueDate.isBefore(weekStart) && !dueDate.isAfter(weekEnd);
                case "This Month":
                    return dueDate.getYear() == today.getYear() && dueDate.getMonth() == today.getMonth();
                default: return true;
            }
        } catch (Exception e) { return false; }
    }

    private boolean matchesDailyTodoFilter(TaskDetails task) {
        String filter = dailyTodoCombo.getValue();
        if (filter == null || "All Tasks".equals(filter)) return true;
        boolean isDailyTask = task.isDaily();
        return "Daily Tasks".equals(filter) ? isDailyTask : !isDailyTask;
    }

    private boolean matchesProjectFilter(TaskDetails task) {
        String filter = projectFilterCombo.getValue();
        if ("All Projects".equals(filter)) return true;
        return filter != null && filter.equals(task.getProjectName());
    }

    private boolean matchesMilestoneFilter(TaskDetails task) {
        String filter = milestoneFilterCombo.getValue();
        if ("All Milestones".equals(milestoneFilter)) return true;
        return filter != null && filter.equals(task.getMilestoneName());
    }

    private boolean matchesCategoryFilter(TaskDetails task) {
        String filter = categoryFilterCombo.getValue();
        if ("All Categories".equals(categoryFilter)) return true;
        return filter != null && filter.equals(task.getCategory());
    }

    private boolean matchesCompletedFilter(TaskDetails task) {
        if (showCompleted) return true;
        return !"COMPLETED".equalsIgnoreCase(task.getStatus());
    }

    private void populateKanban(List<TaskDetails> tasks) {
        if (isKanbanView) {
            populateKanbanView(tasks);
        } else {
            populateListView(tasks);
        }
    }

    private void populateKanbanView(List<TaskDetails> tasks) {
        pendingContainer.getChildren().clear();
        inProgressContainer.getChildren().clear();
        completedContainer.getChildren().clear();

        int pendingCount = 0, progressCount = 0, completedCount = 0;

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

    private void populateListView(List<TaskDetails> tasks) {
        listContainer.getChildren().clear();
        if (tasks.isEmpty()) {
            listContainer.getChildren().add(createEmptyState());
            return;
        }

        for (TaskDetails task : tasks) {
            listContainer.getChildren().add(createListItem(task));
        }
    }

    private VBox createListItem(TaskDetails task) {
        VBox item = new VBox();
        item.getStyleClass().add("list-task-item");
        item.setOnMouseClicked(e -> showTaskDetails(task));

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-task-row");

        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("list-task-checkbox");
        checkBox.setSelected("COMPLETED".equalsIgnoreCase(task.getStatus()));
        checkBox.setOnAction(e -> {
            e.consume();
            if (checkBox.isSelected()) {
                task.setStatus("COMPLETED");
            } else {
                task.setStatus("PENDING");
            }
            updateTaskStatus(task);
            applyFilters();
        });

        VBox taskInfoBox = new VBox(4);
        taskInfoBox.setPrefWidth(280);
        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("list-task-title");
        if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
            titleLabel.getStyleClass().add("list-task-title-completed");
        }

        HBox chipsBox = new HBox(8);
        Label categoryChip = new Label(task.getCategory() != null ? task.getCategory() : "General");
        categoryChip.getStyleClass().add("list-task-chip");
        chipsBox.getChildren().add(categoryChip);
        if (task.getMilestoneName() != null && !task.getMilestoneName().isEmpty()) {
            Label milestoneChip = new Label("🎯 " + task.getMilestoneName());
            milestoneChip.getStyleClass().add("list-task-chip");
            chipsBox.getChildren().add(milestoneChip);
        }
        taskInfoBox.getChildren().addAll(titleLabel, chipsBox);

        Label statusLabel = new Label(task.getStatus());
        statusLabel.getStyleClass().addAll("list-task-status", getStatusBadgeClass(task.getStatus()));
        statusLabel.setPrefWidth(100);

        Label priorityLabel = new Label(getPriorityText(task));
        priorityLabel.getStyleClass().addAll("list-task-priority", getPriorityBadgeClass(task));
        priorityLabel.setPrefWidth(80);

        Label dueLabel = new Label(formatDate(task.getDueDate()));
        dueLabel.getStyleClass().add("list-task-due");
        dueLabel.setPrefWidth(100);
        if (isOverdue(task.getDueDate()) && !"COMPLETED".equalsIgnoreCase(task.getStatus())) {
            dueLabel.getStyleClass().add("list-task-due-overdue");
        }

        Label weightLabel = new Label("⚡ " + task.getWeight());
        weightLabel.getStyleClass().add("list-task-weight");
        weightLabel.setPrefWidth(60);

        HBox actionsBox = new HBox(8);
        Button viewBtn = new Button("👁");
        viewBtn.getStyleClass().add("list-action-btn");
        viewBtn.setOnAction(e -> { e.consume(); showTaskDetails(task); });
        Button editBtn = new Button("✏️");
        editBtn.getStyleClass().add("list-action-btn");
        editBtn.setOnAction(e -> { e.consume(); showEditTaskDialog(task); });
        actionsBox.getChildren().addAll(viewBtn, editBtn);
        actionsBox.setPrefWidth(70);

        row.getChildren().addAll(checkBox, taskInfoBox, statusLabel, priorityLabel, dueLabel, weightLabel, actionsBox);
        HBox.setHgrow(taskInfoBox, Priority.ALWAYS);
        item.getChildren().add(row);
        return item;
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(60));
        emptyState.getStyleClass().add("empty-state");
        Label iconLabel = new Label("📋");
        iconLabel.getStyleClass().add("empty-icon");
        Label titleLabel = new Label("No tasks found");
        titleLabel.getStyleClass().add("empty-text");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600;");
        Label messageLabel = new Label("Create a new task to get started");
        messageLabel.getStyleClass().add("empty-text");
        emptyState.getChildren().addAll(iconLabel, titleLabel, messageLabel);
        return emptyState;
    }

    private String getStatusBadgeClass(String status) {
        if (status == null) return "status-pending-badge";
        switch (status.toUpperCase()) {
            case "COMPLETED": return "status-completed-badge";
            case "IN_PROGRESS": return "status-in_progress-badge";
            default: return "status-pending-badge";
        }
    }

    private String getPriorityBadgeClass(TaskDetails task) {
        String priority = getPriorityText(task).toLowerCase();
        switch (priority) {
            case "high": return "priority-high-badge";
            case "medium": return "priority-medium-badge";
            case "low": return "priority-low-badge";
            default: return "priority-medium-badge";
        }
    }

    private String getPriorityText(TaskDetails task) {
        if (task.getPriority() != null && !task.getPriority().isEmpty()) return task.getPriority();
        if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
            try {
                LocalDate dueDate = LocalDate.parse(task.getDueDate());
                long daysUntil = LocalDate.now().until(dueDate).getDays();
                if (daysUntil < 0 || daysUntil <= 2) return "High";
                if (daysUntil <= 7) return "Medium";
            } catch (Exception e) {}
        }
        return "Medium";
    }

    private boolean isOverdue(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        try {
            return LocalDate.parse(dateStr).isBefore(LocalDate.now());
        } catch (Exception e) { return false; }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "No date";
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalDate today = LocalDate.now();
            if (date.equals(today)) return "Today";
            if (date.equals(today.plusDays(1))) return "Tomorrow";
            return date.format(dateFormatter);
        } catch (Exception e) { return dateStr; }
    }

    private void updateTaskStatus(TaskDetails task) {
        if (task.getUuid()==null || task.getUuid().trim().isEmpty()) {
            TaskDataManager.updatePersonalTaskStatus(task.getTitle(), task.getStatus());
        } else {
            EditRequestController.updateTaskStatus(task.getUuid(), task.getStatus(), this);
        }
    }

    private void advanceTaskState(TaskDetails task) {
        String currentStatus = task.getStatus().toUpperCase();
        String newStatus;
        switch (currentStatus) {
            case "PENDING": newStatus = "IN_PROGRESS"; break;
            case "IN_PROGRESS": newStatus = "COMPLETED"; break;
            default: return;
        }
        task.setStatus(newStatus);
        updateTaskStatus(task);
        applyFilters();
    }

    private void updateNavStats(List<TaskDetails> tasks) {
        int total = tasks.size();
        long completed = tasks.stream().filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus())).count();
        long progress = tasks.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        navTotalLabel.setText(String.valueOf(total));
        navCompletedLabel.setText(String.valueOf(completed));
        navProgressLabel.setText(String.valueOf(progress));
    }

    public void showTaskDetails(TaskDetails task) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Task Details");
        dialog.setHeaderText(null);

        // Apply CSS to dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/com/planify/frontend/css/Todo.css").toExternalForm()
        );
        dialogPane.getStyleClass().add("task-detail-dialog");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 16;");

        // Title Section
        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("detail-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        // Status Section
        HBox statusBox = new HBox(12);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.getStyleClass().add("status-box");

        Label statusLabel = new Label("Status:");
        statusLabel.getStyleClass().add("detail-label");

        Label statusValue = new Label(task.getStatus());
        String statusClass = "status-" + task.getStatus().toLowerCase().replace("_", "");
        statusValue.getStyleClass().addAll("detail-value", "status-badge", statusClass);

        statusBox.getChildren().addAll(statusLabel, statusValue);
        HBox.setHgrow(statusValue, Priority.ALWAYS);

        content.getChildren().addAll(titleLabel, new Separator(), statusBox);

        // Details Grid
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(16);
        detailsGrid.setVgap(12);
        detailsGrid.setPadding(new Insets(8, 0, 8, 0));

        int row = 0;

        // Description
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            addGridRow(detailsGrid, row++, "📝 Description:", task.getDescription());
        }

        // Due Date
        if (task.getDueDate() != null && !task.getDueDate().isBlank()) {
            addGridRow(detailsGrid, row++, "📅 Due Date:", formatDate(task.getDueDate()));
        }

        // Category
        if (task.getCategory() != null && !task.getCategory().isBlank()) {
            addGridRow(detailsGrid, row++, "🏷️ Category:", task.getCategory());
        }

        // Milestone
        if (task.getMilestoneName() != null && !task.getMilestoneName().isBlank()) {
            addGridRow(detailsGrid, row++, "🎯 Milestone:", task.getMilestoneName());
        }

        // Project
        if (task.getProjectName() != null && !task.getProjectName().isBlank()) {
            addGridRow(detailsGrid, row++, "📊 Project:", task.getProjectName());
        }

        // Weight & Priority
        HBox metaBox = new HBox(20);
        if (task.getWeight() > 0) {
            Label weightLabel = new Label("⚡ Weight: " + task.getWeight());
            weightLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #457b9d; -fx-font-weight: 600;");
            metaBox.getChildren().add(weightLabel);
        }
        if (task.getPriority() != null && !task.getPriority().isBlank()) {
            String priorityColor = getPriorityColor(task.getPriority());
            Label priorityLabel = new Label("🔴 Priority: " + task.getPriority());
            priorityLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + priorityColor + "; -fx-font-weight: 600;");
            metaBox.getChildren().add(priorityLabel);
        }
        if (!metaBox.getChildren().isEmpty()) {
            detailsGrid.add(metaBox, 0, row++, 2, 1);
        }

        content.getChildren().add(detailsGrid);

        // Creator Info
        if (task.getCreator() != null) {
            VBox creatorBox = new VBox(4);
            creatorBox.getStyleClass().add("content-box");
            Label creatorLabel = new Label("👤 Created By");
            creatorLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #64748b;");
            Label creatorValue = new Label(task.getCreator().getName() + " (" + task.getCreator().getEmail() + ")");
            creatorValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #1e293b;");
            creatorBox.getChildren().addAll(creatorLabel, creatorValue);
            content.getChildren().add(creatorBox);
        }

        // Assignees
        if (task.getAssigneeMembers() != null && !task.getAssigneeMembers().isEmpty()) {
            VBox assigneesBox = new VBox(8);
            assigneesBox.getStyleClass().add("assignees-container");

            Label assigneesHeader = new Label("👥 Assigned To");
            assigneesHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #64748b;");

            FlowPane assigneesFlow = new FlowPane(8, 6);
            for (MemberInfo assignee : task.getAssigneeMembers()) {
                Label chip = new Label(assignee.getName());
                chip.getStyleClass().add("assignee-chip");
                assigneesFlow.getChildren().add(chip);
            }

            assigneesBox.getChildren().addAll(assigneesHeader, assigneesFlow);
            content.getChildren().add(assigneesBox);
        }

        // Attachment
        if (task.getAttachmentUrl() != null && !task.getAttachmentUrl().isBlank()) {
            VBox attachBox = new VBox(8);
            attachBox.getStyleClass().add("content-box");

            Label attachHeader = new Label("📎 Attachment");
            attachHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #64748b;");

            Hyperlink link = new Hyperlink(task.getAttachmentUrl());
            link.getStyleClass().add("attachment-link");
            link.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(task.getAttachmentUrl()));
                } catch (Exception ex) {
                    System.out.println("Cannot open: " + task.getAttachmentUrl());
                }
            });

            attachBox.getChildren().addAll(attachHeader, link);
            content.getChildren().add(attachBox);
        }

        // ScrollPane
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(450);
        scroll.setMaxHeight(550);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.getStyleClass().add("detail-scroll-pane");

        dialogPane.setContent(scroll);
        dialogPane.getButtonTypes().addAll(
                new ButtonType("✏️ Edit", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CLOSE
        );

        // Style the buttons
        Node editButton = dialogPane.lookupButton(new ButtonType("✏️ Edit", ButtonBar.ButtonData.OK_DONE));
        if (editButton != null) {
            editButton.setStyle(
                    "-fx-background-color: linear-gradient(135deg, #457b9d, #1d3557);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: 600;" +
                            "-fx-padding: 8 20;" +
                            "-fx-background-radius: 8;" +
                            "-fx-cursor: hand;"
            );
        }

        Node closeButton = dialogPane.lookupButton(ButtonType.CLOSE);
        if (closeButton != null) {
            closeButton.setStyle(
                    "-fx-background-color: #f1f5f9;" +
                            "-fx-text-fill: #475569;" +
                            "-fx-font-weight: 600;" +
                            "-fx-padding: 8 20;" +
                            "-fx-background-radius: 8;" +
                            "-fx-cursor: hand;"
            );
        }

        dialog.showAndWait().ifPresent(response -> {
            if (response.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                showEditTaskDialog(task);
            }
        });
    }

    private void addGridRow(GridPane grid, int row, String label, String value) {
        if (value == null || value.isBlank()) return;

        Label labelWidget = new Label(label);
        labelWidget.getStyleClass().add("detail-label");

        Label valueWidget = new Label(value);
        valueWidget.getStyleClass().add("detail-value");
        valueWidget.setWrapText(true);

        grid.add(labelWidget, 0, row);
        grid.add(valueWidget, 1, row);

        GridPane.setHgrow(valueWidget, Priority.ALWAYS);
    }

    private String getPriorityColor(String priority) {
        switch (priority.toLowerCase()) {
            case "high": return "#dc2626";
            case "medium": return "#d97706";
            case "low": return "#10b981";
            default: return "#64748b";
        }
    }

    private void addDetailRow(VBox container, String label, String value) {
        if (value == null || value.isBlank()) return;
        VBox row = new VBox(5);
        row.getChildren().addAll(new Label(label), new Label(value));
        container.getChildren().add(row);
    }

    public void showEditTaskDialog(TaskDetails task) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/edit-todo-view.fxml"));
            Parent root = loader.load();
            EditTodoController controller = loader.getController();
            controller.setTask(task, projectSummaries, this);
            Stage stage = new Stage();
            stage.setTitle("Edit Task");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadData();
            applyFilters();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void toggleNotifications() {
        boolean isVisible = notificationPanel.isVisible();
        notificationPanel.setVisible(!isVisible);
        notificationPanel.setManaged(!isVisible);
    }

    @FXML private void openAddTask() {
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
            loadData();
            applyFilters();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void goBack() { SceneManager.switchScene("dashboard-view.fxml", "Dashboard"); }
    @FXML private void handleLogout() { SceneManager.switchScene("login-view.fxml", "Login"); }
    public void refresh() { loadData(); applyFilters(); }

}