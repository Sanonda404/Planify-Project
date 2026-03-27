package com.planify.frontend.controllers.project;

import com.planify.frontend.controllers.Request.DeleteRequestController;
import com.planify.frontend.controllers.Request.EditRequestController;
import com.planify.frontend.controllers.resources.AddResourceController;
import com.planify.frontend.controllers.task.AddTodoController;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.utils.UserSession;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import javafx.scene.shape.Rectangle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProjectTasksController {

    @FXML private VBox milestoneContainer;
    @FXML private VBox unassignedContainer;
    @FXML private VBox unassignedTasksBox;

    private ProjectDetails projectDetails;

    private List<MilestoneDetails> milestones;
    private final List<TaskDetails> allTasks = new ArrayList<>();
    private ProjectDetailsController projectDetailsController;

    public void setProjectDetailsController(ProjectDetailsController projectDetailsController){
        this.projectDetailsController = projectDetailsController;
    }

    public void setProjectDetails(ProjectDetails projectDetails) {
        this.projectDetails = projectDetails;
        this.milestones = projectDetails.getMilestones();
        loadMilestones();
        loadUnassignedTasks();
    }

    private void loadMilestones() {
        milestoneContainer.getChildren().clear();
        for (MilestoneDetails milestone : milestones) {
            if(!milestone.getTitle().equals("Uncategorized")){
                VBox card = createMilestoneCard(milestone);
                milestoneContainer.getChildren().add(card);
            }
            else{
                if(milestone.getTasks()==null)continue;
                allTasks.clear();
                allTasks.addAll(milestone.getTasks());
            }
        }
    }

    private void loadUnassignedTasks() {
        unassignedTasksBox.getChildren().clear();
        System.out.println(allTasks.size());

        for (TaskDetails task : allTasks) {
            HBox row = createTaskRowWithResourceButton(task);
            unassignedTasksBox.getChildren().add(row);
        }

        unassignedContainer.setVisible(!allTasks.isEmpty());
    }

    // Add this method to create milestone card with Add Task button
    private VBox createMilestoneCard(MilestoneDetails milestone) {
        VBox card = new VBox(16);
        card.getStyleClass().add("pt-milestone-card");
        card.setPadding(new Insets(24));
        card.setCursor(Cursor.HAND);

        // Header: left side (title, status, due date)
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(milestone.getTitle());
        title.getStyleClass().add("pt-milestone-title");

        Label status = new Label(milestone.isCompleted() ? "✓ Complete" : "🔄 In Progress");
        status.getStyleClass().add(milestone.isCompleted() ? "pt-status-complete" : "pt-status-pending");

        // Weight indicator (based on task weights)
        Label weightLabel = new Label("⚡ Weight: " + calculateMilestoneWeight(milestone));
        weightLabel.getStyleClass().add("pt-weight-badge");

        header.getChildren().addAll(title, status, weightLabel);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label dueDate = new Label("Due: " + milestone.getDeadline());
        dueDate.getStyleClass().add("pt-date");

        VBox leftHeader = new VBox(6, header, dueDate);
        HBox.setHgrow(leftHeader, Priority.ALWAYS);

        // Header: right side (percent + tasks count)
        Label percent = new Label(milestone.getCompletionRate() + "%");
        percent.getStyleClass().add("pt-big-percent");

        Label count = new Label(milestone.getCompletedTasks() + "/" + milestone.getTotalTasks() + " tasks");
        count.getStyleClass().add("pt-small-text");

        VBox rightHeader = new VBox(4, percent, count);
        rightHeader.setAlignment(Pos.CENTER_RIGHT);

        HBox topRow = new HBox(leftHeader, rightHeader);
        topRow.setAlignment(Pos.TOP_LEFT);

        // Progress bar
        ProgressBar progressBar = new ProgressBar(milestone.getCompletionRate() / 100.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add(milestone.isCompleted() ? "pt-progress-complete" : "pt-progress-pending");

        // Tasks preview with Add Task button per milestone
        VBox tasksBox = new VBox(10);

        HBox tasksHeader = new HBox(10);
        tasksHeader.setAlignment(Pos.CENTER_LEFT);
        Label tasksLabel = new Label("📝 Tasks");
        tasksLabel.getStyleClass().add("pt-tasks-header");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addTaskToMilestoneBtn = new Button("+ Add Task to Milestone");
        addTaskToMilestoneBtn.getStyleClass().add("pt-add-task-btn");
        addTaskToMilestoneBtn.setOnAction(e -> openAddTaskForMilestone(milestone));
        tasksHeader.getChildren().addAll(tasksLabel, spacer, addTaskToMilestoneBtn);
        tasksBox.getChildren().add(tasksHeader);

        if (milestone.getTasks() == null || milestone.getTasks().isEmpty()) {
            Label noTasks = new Label("No tasks assigned yet. Click 'Add Task' to get started.");
            noTasks.getStyleClass().add("pt-small-text");
            tasksBox.getChildren().add(noTasks);
        } else {
            int shown = 0;
            for (TaskDetails task : milestone.getTasks()) {
                if (shown == 5) break;
                tasksBox.getChildren().add(createTaskRowWithResourceButton(task));
                shown++;
            }
            if (milestone.getTasks().size() > 5) {
                Button expandBtn = new Button("Show More (" + (milestone.getTasks().size() - 5) + " more)");
                expandBtn.getStyleClass().add("pt-expand-btn");
                expandBtn.setOnAction(e -> {
                    tasksBox.getChildren().clear();
                    tasksBox.getChildren().add(tasksHeader);
                    for (TaskDetails t : milestone.getTasks()) {
                        tasksBox.getChildren().add(createTaskRowWithResourceButton(t));
                    }
                    expandBtn.setVisible(false);
                });
                tasksBox.getChildren().add(expandBtn);
            }
        }

        card.getChildren().addAll(topRow, progressBar, tasksBox);
        card.setOnMouseClicked(e -> showMilestonePopup(milestone));
        return card;
    }

    private int calculateMilestoneWeight(MilestoneDetails milestone) {
        int totalWeight = 0;
        for (TaskDetails task : milestone.getTasks()) {
            // Get task weight - you'll need to add weight field to TaskDetails
            // For now using placeholder based on priority
            totalWeight += getTaskWeight(task);
        }
        return totalWeight;
    }

    private int getTaskWeight(TaskDetails task) {
        // This should come from your TaskDetails model
        // For now returning default based on priority
        // You'll need to add weight field to TaskDetails
        return 5; // Placeholder
    }

    private HBox createTaskRowWithResourceButton(TaskDetails task) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(Cursor.HAND);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.getStyleClass().add(task.getStatus().equalsIgnoreCase("COMPLETED") ? "pt-task-row-done" : "pt-task-row-pending");

        // Left side: dot + title + weight
        Region dot = new Region();
        dot.getStyleClass().add(task.getStatus().equalsIgnoreCase("COMPLETED") ? "pt-dot-done" :
                (task.getStatus().equalsIgnoreCase("IN_PROGRESS") ? "pt-dot-progress" : "pt-dot-pending"));

        Label title = new Label(task.getTitle());
        title.getStyleClass().add(task.getStatus().equalsIgnoreCase("COMPLETED") ? "pt-task-title-done" : "pt-task-title");

        Label weightLabel = new Label("⚡ " + getTaskWeight(task));
        weightLabel.getStyleClass().add("pt-task-weight");

        HBox left = new HBox(12, dot, title, weightLabel);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        // Right side: category + assignees + resource button
        Label category = new Label(task.getCategory());
        category.getStyleClass().add("pt-task-chip");

        String assigneeNames = task.getAssigneeMembers().stream()
                .map(MemberInfo::getName)
                .collect(Collectors.joining(", "));
        Label assignees = new Label("👤 " + assigneeNames);
        assignees.getStyleClass().add("pt-task-assignee");

        Button resourceBtn = new Button("📎");
        resourceBtn.getStyleClass().add("pt-resource-btn");
        resourceBtn.setOnAction(e -> openAddResourceForTask(task));

        HBox right = new HBox(14, category, assignees, resourceBtn);

        row.getChildren().addAll(left, right);
        row.setOnMouseClicked(e -> showTaskPopup(task));
        return row;
    }

    private void openAddTaskForMilestone(MilestoneDetails milestone) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/add-todo-view.fxml"));
            Parent root = loader.load();

            AddTodoController controller = loader.getController();
            controller.setContextForMilestone(
                    projectDetails.getUuid(),
                    projectDetails.getName(),
                    milestone.getUuid(),
                    milestone.getTitle(),
                    projectDetails.getMembers(),
                    projectDetailsController
            );

            Stage stage = new Stage();
            stage.setTitle("Add Task to " + milestone.getTitle());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openAddResourceForTask(TaskDetails task) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/add-resource-view.fxml"));
            Parent root = loader.load();

            AddResourceController controller = loader.getController();
            controller.setContextForTask(
                    projectDetailsController,
                    task.getUuid(),
                    task.getTitle(),
                    projectDetails.getName(),
                    projectDetails.getUuid()
            );

            Stage stage = new Stage();
            stage.setTitle("Add Resource to Task");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void showMilestonePopup(MilestoneDetails milestone) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Milestone Details");

        // Custom button types
        ButtonType editButton = new ButtonType("✏️ Edit", ButtonBar.ButtonData.LEFT);
        ButtonType deleteButton = new ButtonType("🗑️ Delete", ButtonBar.ButtonData.LEFT);
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(editButton, deleteButton, closeButton);

        // Main content container
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.getStyleClass().add("milestone-dialog-content");

        // Header Section
        VBox headerSection = new VBox(8);
        headerSection.getStyleClass().add("dialog-header-section");

        Label titleLabel = new Label(milestone.getTitle());
        titleLabel.getStyleClass().add("milestone-title-label");

        Label descLabel = new Label(milestone.getDescription());
        descLabel.getStyleClass().add("milestone-description-label");
        descLabel.setWrapText(true);

        headerSection.getChildren().addAll(titleLabel, descLabel);

        // Info Section
        GridPane infoGrid = new GridPane();
        infoGrid.getStyleClass().add("info-grid");
        infoGrid.setHgap(15);
        infoGrid.setVgap(12);

        Label dueDateLabelKey = new Label("📅 Due Date:");
        dueDateLabelKey.getStyleClass().add("info-label-key");
        Label dueDateLabelValue = new Label(milestone.getDeadline());
        dueDateLabelValue.getStyleClass().add("info-label-value");

        Label completionLabelKey = new Label("📊 Completion:");
        completionLabelKey.getStyleClass().add("info-label-key");
        Label completionLabelValue = new Label(milestone.getCompletionRate() + "%");
        completionLabelValue.getStyleClass().add("info-label-value");

        Label tasksLabelKey = new Label("✅ Tasks:");
        tasksLabelKey.getStyleClass().add("info-label-key");
        Label tasksLabelValue = new Label(milestone.getCompletedTasks() + "/" + milestone.getTotalTasks());
        tasksLabelValue.getStyleClass().add("info-label-value");

        infoGrid.add(dueDateLabelKey, 0, 0);
        infoGrid.add(dueDateLabelValue, 1, 0);
        infoGrid.add(completionLabelKey, 0, 1);
        infoGrid.add(completionLabelValue, 1, 1);
        infoGrid.add(tasksLabelKey, 0, 2);
        infoGrid.add(tasksLabelValue, 1, 2);

        // Progress Bar
        StackPane progressContainer = new StackPane();
        progressContainer.getStyleClass().add("progress-container");

        Rectangle progressBg = new Rectangle(400, 12);
        progressBg.getStyleClass().add("progress-background");

        Rectangle progressFill = new Rectangle(400 * (milestone.getCompletionRate() / 100.0), 12);
        progressFill.getStyleClass().add("progress-fill");

        progressContainer.getChildren().addAll(progressBg, progressFill);
        StackPane.setAlignment(progressFill, Pos.CENTER_LEFT);

        // Tasks Section
        VBox tasksSection = new VBox(10);
        tasksSection.getStyleClass().add("tasks-section");

        Label tasksHeader = new Label("📝 Tasks in this Milestone:");
        tasksHeader.getStyleClass().add("section-header");

        ScrollPane tasksScroll = new ScrollPane();
        tasksScroll.setFitToWidth(true);
        tasksScroll.setPrefHeight(200);
        tasksScroll.getStyleClass().add("tasks-scroll");

        VBox tasksList = new VBox(8);
        tasksList.getStyleClass().add("tasks-list");
        tasksList.setPadding(new Insets(10));

        for (TaskDetails task : milestone.getTasks()) {
            HBox taskItem = new HBox(12);
            taskItem.getStyleClass().add("task-item");

            CheckBox cb = new CheckBox();
            cb.setSelected("COMPLETED".equalsIgnoreCase(task.getStatus()));
            cb.setDisable(true);
            cb.getStyleClass().add("task-checkbox-disabled");

            VBox taskInfo = new VBox(3);
            HBox.setHgrow(taskInfo, Priority.ALWAYS);

            Label taskTitle = new Label(task.getTitle());
            taskTitle.getStyleClass().add("task-title-label");
            if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
                taskTitle.getStyleClass().add("task-completed");
            }

            Label taskMeta = new Label("Due: " + task.getDueDate() + " • " + task.getCategory());
            taskMeta.getStyleClass().add("task-meta-label");

            taskInfo.getChildren().addAll(taskTitle, taskMeta);

            Label statusBadge = new Label(task.getStatus());
            statusBadge.getStyleClass().addAll("status-badge", "status-" + task.getStatus().toLowerCase());

            taskItem.getChildren().addAll(cb, taskInfo, statusBadge);
            tasksList.getChildren().add(taskItem);
        }

        tasksScroll.setContent(tasksList);
        tasksSection.getChildren().addAll(tasksHeader, tasksScroll);

        content.getChildren().addAll(headerSection, infoGrid, progressContainer, tasksSection);

        // Apply CSS
        dialog.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/com/planify/frontend/css/MilestoneTaskDialog.css")).toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("milestone-dialog");
        dialog.getDialogPane().setContent(content);

        // Handle button actions
        dialog.showAndWait().ifPresent(response -> {
            if (response == editButton) {
                openEditMilestoneDialog(milestone);
            }
            else if(response == deleteButton){
                deleteMilestone(milestone);
            }
        });
    }

    private void showTaskPopup(TaskDetails task) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Task Details");

        // Custom button types
        ButtonType editButton = new ButtonType("✏️ Edit", ButtonBar.ButtonData.LEFT);
        ButtonType deleteButton = new ButtonType("🗑️ Delete", ButtonBar.ButtonData.LEFT);
        ButtonType updateButton = new ButtonType("💾 Update Status", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(editButton, deleteButton, updateButton, closeButton);

        if (!task.getCreator().getEmail().equals(UserSession.getInstance().getEmail())) {
            Button editBtn = (Button) dialog.getDialogPane().lookupButton(editButton);
            if (editBtn != null) editBtn.setDisable(true);

            Button deleteBtn = (Button) dialog.getDialogPane().lookupButton(deleteButton);
            if (deleteBtn != null) deleteBtn.setDisable(true);
        }

        // Main content container
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.getStyleClass().add("task-dialog-content");

        // Header Section
        VBox headerSection = new VBox(8);
        headerSection.getStyleClass().add("dialog-header-section");

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("task-title-main");

        Label descLabel = new Label(task.getDescription());
        descLabel.getStyleClass().add("task-description-label");
        descLabel.setWrapText(true);

        headerSection.getChildren().addAll(titleLabel, descLabel);

        // Info Grid
        GridPane infoGrid = new GridPane();
        infoGrid.getStyleClass().add("info-grid");
        infoGrid.setHgap(15);
        infoGrid.setVgap(12);

        int row = 0;

        // Due Date
        addInfoRow(infoGrid, row++, "📅 Due Date:", task.getDueDate());

        // Category
        addInfoRow(infoGrid, row++, "📁 Category:", task.getCategory());

        // Milestone
        if (task.getMilestoneName() != null && !task.getMilestoneName().isEmpty()) {
            addInfoRow(infoGrid, row++, "🎯 Milestone:", task.getMilestoneName());
        }

        // Creator
        addInfoRow(infoGrid, row++, "👤 Creator:", task.getCreator().getName());

        // Assignees Section
        VBox assigneesSection = new VBox(8);
        assigneesSection.getStyleClass().add("assignees-section");

        Label assigneesHeader = new Label("👥 Assigned To:");
        assigneesHeader.getStyleClass().add("section-header-small");

        FlowPane assigneesFlow = new FlowPane(8, 8);
        assigneesFlow.getStyleClass().add("assignees-flow");

        for (MemberInfo assignee : task.getAssigneeMembers()) {
            HBox assigneeBox = new HBox(6);
            assigneeBox.getStyleClass().add("assignee-box");

            Label assigneeLabel = new Label(assignee.getName());
            assigneeLabel.getStyleClass().add("assignee-label");

            assigneeBox.getChildren().add(assigneeLabel);
            assigneesFlow.getChildren().add(assigneeBox);
        }

        assigneesSection.getChildren().addAll(assigneesHeader, assigneesFlow);

        // Attachment
        VBox attachmentSection = null;
        if (task.getAttachmentUrl() != null && !task.getAttachmentUrl().isEmpty()) {
            attachmentSection = new VBox(8);
            attachmentSection.getStyleClass().add("attachment-section");

            Label attachmentHeader = new Label("📎 Attachment:");
            attachmentHeader.getStyleClass().add("section-header-small");

            Hyperlink attachmentLink = new Hyperlink(task.getAttachmentUrl());
            attachmentLink.getStyleClass().add("attachment-link");
            attachmentLink.setOnAction(e -> {
                // TODO: Open URL in browser
                System.out.println("Opening: " + task.getAttachmentUrl());
            });

            attachmentSection.getChildren().addAll(attachmentHeader, attachmentLink);
        }

        // Status Update Section
        VBox statusSection = new VBox(10);
        statusSection.getStyleClass().add("status-section");

        Label statusHeader = new Label("🔄 Update Status:");
        statusHeader.getStyleClass().add("section-header-small");

        HBox statusBox = new HBox(12);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label currentStatusLabel = new Label("Current:");
        currentStatusLabel.getStyleClass().add("info-label-key");

        Label currentStatus = new Label(task.getStatus());
        currentStatus.getStyleClass().addAll("status-badge-large", "status-" + task.getStatus().toLowerCase());

        Label arrowLabel = new Label("→");
        arrowLabel.getStyleClass().add("status-arrow");

        ChoiceBox<String> statusChoice = new ChoiceBox<>();
        statusChoice.getItems().addAll("PENDING", "IN_PROGRESS", "COMPLETED");
        statusChoice.setValue(task.getStatus());
        statusChoice.getStyleClass().add("status-choicebox");

        statusBox.getChildren().addAll(currentStatusLabel, currentStatus, arrowLabel, statusChoice);
        statusSection.getChildren().addAll(statusHeader, statusBox);

        // Assemble content
        content.getChildren().addAll(headerSection, infoGrid, assigneesSection);
        if (attachmentSection != null) {
            content.getChildren().add(attachmentSection);
        }
        content.getChildren().add(statusSection);

        // Apply CSS
        dialog.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(SceneManager.class.getResource("/com/planify/frontend/css/MilestoneTaskDialog.css")).toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("task-dialog");
        dialog.getDialogPane().setContent(content);

        // Handle button actions
        dialog.showAndWait().ifPresent(response -> {
            if (response == editButton) {
                openEditTaskDialog(task);
            } else if (response == updateButton) {
                String newStatus = statusChoice.getValue();
                if (!newStatus.equals(task.getStatus())) {
                    updateTaskStatus(task, newStatus);
                }
            }else if(response== deleteButton){
                deleteTask(task);
            }
        });
    }

    // Helper method to add info rows
    private void addInfoRow(GridPane grid, int row, String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("info-label-key");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("info-label-value");

        grid.add(keyLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private void updateTaskStatus(TaskDetails task, String newStatus) {
        if(task.getUuid().trim().isEmpty()){
            ProjectDataManager.updatePersonalTaskStatus(task.getProjectName(), task.getMilestoneName(), task.getTitle(), newStatus);
            projectDetailsController.refresh();
            return;
        }
        // Update local model
        String oldStatus = task.getStatus();
        task.setStatus(newStatus);

        // TODO: Call backend API to update task status
        EditRequestController.updateTaskStatus(task.getUuid(), newStatus, projectDetailsController);

        System.out.println("Updated task status: " + task.getTitle() + " → " + oldStatus + " to " + newStatus);


        // Refresh view
        if (projectDetailsController != null) {
            projectDetailsController.refresh();
        }
    }

    private void openEditMilestoneDialog(MilestoneDetails milestone) {
        // TODO: Create edit milestone form (similar to Add Milestone)
        // TODO: Pre-populate with current milestone data
        // TODO: On save, call backend API to update milestone

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/planify/frontend/fxmls/edit-milestone-view.fxml"));
            Parent root = loader.load();

            EditMilestoneController controller = loader.getController();
            controller.setMilestone(milestone, projectDetailsController);

            Stage stage = new Stage();
            stage.setTitle("Edit Milestone");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void openEditTaskDialog(TaskDetails task) {
        // TODO: Create edit task form (similar to Add Task)
        // TODO: Pre-populate with current task data
        // TODO: On save, call backend API to update task

        System.out.println("TODO: Open edit task dialog for: " + task.getTitle());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/planify/frontend/fxmls/edit-task-view.fxml"));
            Parent root = loader.load();

            EditTaskController controller = loader.getController();
            controller.setTask(task, projectDetails.getUuid(), projectDetails.getMilestones(),
                    projectDetails.getMembers(), projectDetailsController);

            Stage stage = new Stage();
            stage.setTitle("Edit Task");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteMilestone(MilestoneDetails milestone){
        //personal project
        if(milestone.getUuid().trim().isEmpty()){
            ProjectDataManager.deletePersonalProjectMilestone(projectDetails.getName(), milestone.getTitle());
        }else{
            System.out.println("Deleting..");
            DeleteRequestController.deleteMilestone(milestone.getUuid(), LocalDataManager.getUserEmail(), projectDetailsController);
        }
    }

    private void deleteTask(TaskDetails task){
        if(task.getUuid().trim().isEmpty()){
            ProjectDataManager.deletePersonalProjectTask(projectDetails.getName(), task.getMilestoneName(), task.getTitle());
        }else{
            DeleteRequestController.deleteTask(task.getUuid(), LocalDataManager.getUserEmail(), projectDetailsController);
        }
    }

    @FXML
    private void openAddMilestone() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/add-milestone-view.fxml"));
            Parent root = loader.load();

            AddMilestoneController controller = loader.getController();
            // Pass unassigned tasks into the form
            List<TaskDetails> unassigned = new ArrayList<>();
            if(allTasks!=null)unassigned = allTasks.stream()
                    .filter(t -> t.getMilestoneUuid() == null || "Uncategorized".equalsIgnoreCase(t.getMilestoneName()))
                    .collect(Collectors.toList());
            controller.setUnassignedTasks(unassigned);
            controller.setProjectUuid(projectDetails.getUuid());
            controller.setParent(projectDetailsController);
            if(projectDetails.getUuid().trim().isEmpty())controller.setPersonalProjectName(projectDetails.getName());

            Stage stage = new Stage();
            stage.setTitle("Add Milestone");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAddTask() {
        System.out.println(projectDetailsController.getName());
      try {

            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/add-todo-view.fxml"));
            Parent root = loader.load();

            AddTodoController controller = loader.getController();
            controller.setContextForProject(projectDetails.getUuid(),
                    projectDetails.getName(),
                    projectDetails.getMilestones(),
                    projectDetails.getMembers(),
                    projectDetailsController
                    );
            Stage stage = new Stage();
            stage.setTitle("Add Task");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}