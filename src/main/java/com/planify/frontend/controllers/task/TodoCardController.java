package com.planify.frontend.controllers.task;

import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.utils.helpers.DateTimeFormatter;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TodoCardController {

    @FXML private Region priorityIndicator;
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label categoryLabel;
    @FXML private Label milestoneLabel;
    @FXML private Label dueDateLabel;
    @FXML private Label projectLabel;
    @FXML private HBox assigneesBox;
    @FXML private Label assigneesLabel;
    @FXML private Label attachmentLabel;
    @FXML private Label priorityBadge;
    @FXML private Label dailyBadge;
    @FXML private Label weightLabel;
    @FXML private Button actionButton;

    private TaskDetails task;
    private Consumer<TaskDetails> onStateAdvance;
    private TodoController parentController;

    public void setData(TaskDetails task, Consumer<TaskDetails> onStateAdvance, TodoController parentController) {
        this.task = task;
        this.onStateAdvance = onStateAdvance;
        this.parentController = parentController;
        populateCard();
    }

    private void populateCard() {
        titleLabel.setText(task.getTitle());
        applyStatusStyle();

        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            String preview = task.getDescription().length() > 80 ? task.getDescription().substring(0, 77) + "..." : task.getDescription();
            descriptionLabel.setText(preview);
            descriptionLabel.setVisible(true);
            descriptionLabel.setManaged(true);
        }

        categoryLabel.setText(task.getCategory() != null ? task.getCategory() : "General");

        if (task.getMilestoneName() != null && !task.getMilestoneName().isBlank()) {
            milestoneLabel.setText("🎯 " + task.getMilestoneName());
            milestoneLabel.setVisible(true);
            milestoneLabel.setManaged(true);
        }

        if (task.getDueDate() != null && !task.getDueDate().isBlank()) {
            dueDateLabel.setText("📅 " + DateTimeFormatter.FormatDateTime(task.getDueDate()));
            if (isOverdue(task.getDueDate()) && !"COMPLETED".equalsIgnoreCase(task.getStatus())) {
                dueDateLabel.getStyleClass().add("task-overdue");
            }
        }

        if (task.getProjectName() != null && !task.getProjectName().isBlank()) {
            projectLabel.setText("📊 " + task.getProjectName());
            projectLabel.setVisible(true);
            projectLabel.setManaged(true);
        }

        if (task.getAssigneeMembers() != null && !task.getAssigneeMembers().isEmpty()) {
            String assignees = task.getAssigneeMembers().stream().map(MemberInfo::getName).limit(3).collect(Collectors.joining(", "));
            if (task.getAssigneeMembers().size() > 3) assignees += " +" + (task.getAssigneeMembers().size() - 3) + " more";
            assigneesLabel.setText(assignees);
            assigneesBox.setVisible(true);
            assigneesBox.setManaged(true);
        }

        if (task.getAttachmentUrl() != null && !task.getAttachmentUrl().isBlank()) {
            attachmentLabel.setVisible(true);
            attachmentLabel.setManaged(true);
        }

        if (task.isDaily()) {
            dailyBadge.setText("⚡ Daily Task");
            dailyBadge.setVisible(true);
            dailyBadge.setManaged(true);
        }

        if (task.getPriority() != null) {
            priorityBadge.setText(task.getPriority());
            priorityBadge.getStyleClass().add("priority-" + task.getPriority().toLowerCase() + "-badge");
            priorityBadge.setVisible(true);
            priorityBadge.setManaged(true);
        }

        if (task.getWeight() > 0) {
            weightLabel.setText("⚡ Weight: " + task.getWeight());
            weightLabel.setVisible(true);
            weightLabel.setManaged(true);
        }

        setPriorityIndicator();
        configureActionButton();
    }

    private void applyStatusStyle() {
        titleLabel.getStyleClass().removeAll("task-title-completed", "task-title-progress");
        switch (task.getStatus().toUpperCase()) {
            case "COMPLETED": titleLabel.getStyleClass().add("task-title-completed"); break;
            case "IN_PROGRESS": titleLabel.getStyleClass().add("task-title-progress"); break;
        }
    }

    private void setPriorityIndicator() {
        priorityIndicator.getStyleClass().removeAll("priority-high", "priority-medium", "priority-low", "priority-none");
        if (task.getDueDate() == null || task.getDueDate().isBlank() || "COMPLETED".equalsIgnoreCase(task.getStatus())) {
            priorityIndicator.getStyleClass().add("priority-none");
            return;
        }
        try {
            LocalDate dueDate = LocalDate.parse(task.getDueDate());
            long daysUntil = LocalDate.now().until(dueDate).getDays();
            if (daysUntil <= 2) priorityIndicator.getStyleClass().add("priority-high");
            else if (daysUntil <= 7) priorityIndicator.getStyleClass().add("priority-medium");
            else priorityIndicator.getStyleClass().add("priority-low");
        } catch (Exception e) { priorityIndicator.getStyleClass().add("priority-none"); }
    }

    private void configureActionButton() {
        switch (task.getStatus().toUpperCase()) {
            case "PENDING":
                actionButton.setText("▶ Start Task");
                actionButton.setVisible(true);
                actionButton.setOnAction(e -> { if (onStateAdvance != null) onStateAdvance.accept(task); });
                break;
            case "IN_PROGRESS":
                actionButton.setText("✓ Mark Complete");
                actionButton.setVisible(true);
                actionButton.setOnAction(e -> { if (onStateAdvance != null) onStateAdvance.accept(task); });
                break;
            default:
                actionButton.setVisible(false);
        }
    }

    private boolean isOverdue(String dateStr) {
        try { return LocalDate.parse(dateStr).isBefore(LocalDate.now()); }
        catch (Exception e) { return false; }
    }


    @FXML private void viewTask() { if (parentController != null) parentController.showTaskDetails(task); }
    @FXML private void editTask() { if (parentController != null) parentController.showEditTaskDialog(task); }
}