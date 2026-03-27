package com.planify.frontend.controllers.task;

import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.models.auth.MemberInfo;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    @FXML private Label attachmentLabel,priorityBadge,dailyBadge,weightLabel;
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
        // Title
        titleLabel.setText(task.getTitle());

        // Apply status styling
        applyStatusStyle();

        // Description (preview)
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            String preview = task.getDescription().length() > 80 ?
                    task.getDescription().substring(0, 77) + "..." : task.getDescription();
            descriptionLabel.setText(preview);
            descriptionLabel.setVisible(true);
            descriptionLabel.setManaged(true);
        } else {
            descriptionLabel.setVisible(false);
            descriptionLabel.setManaged(false);
        }

        // Category
        if (task.getCategory() != null && !task.getCategory().isBlank()) {
            categoryLabel.setText(task.getCategory());
        } else {
            categoryLabel.setText("General");
        }

        // Milestone
        if (task.getMilestoneName() != null && !task.getMilestoneName().isBlank()) {
            milestoneLabel.setText("🎯 " + task.getMilestoneName());
            milestoneLabel.setVisible(true);
            milestoneLabel.setManaged(true);
        } else {
            milestoneLabel.setVisible(false);
            milestoneLabel.setManaged(false);
        }

        // Due Date
        if (task.getDueDate() != null && !task.getDueDate().isBlank()) {
            dueDateLabel.setText("📅 " + formatDate(task.getDueDate()));

            // Highlight overdue tasks
            if (isOverdue(task.getDueDate()) && !"COMPLETED".equalsIgnoreCase(task.getStatus())) {
                dueDateLabel.getStyleClass().add("task-overdue");
            }
        } else {
            dueDateLabel.setText("No due date");
        }

        // Project
        if (task.getProjectName() != null && !task.getProjectName().isBlank()) {
            projectLabel.setText("📊 " + task.getProjectName());
            projectLabel.setVisible(true);
            projectLabel.setManaged(true);
        } else {
            projectLabel.setVisible(false);
            projectLabel.setManaged(false);
        }

        // Assignees
        if (task.getAssigneeMembers() != null && !task.getAssigneeMembers().isEmpty()) {
            String assignees = task.getAssigneeMembers().stream()
                    .map(MemberInfo::getName)
                    .limit(3)
                    .collect(Collectors.joining(", "));

            if (task.getAssigneeMembers().size() > 3) {
                assignees += " +" + (task.getAssigneeMembers().size() - 3) + " more";
            }

            assigneesLabel.setText(assignees);
            assigneesBox.setVisible(true);
            assigneesBox.setManaged(true);
        } else {
            assigneesBox.setVisible(false);
            assigneesBox.setManaged(false);
        }

        // Attachment
        if (task.getAttachmentUrl() != null && !task.getAttachmentUrl().isBlank()) {
            attachmentLabel.setVisible(true);
            attachmentLabel.setManaged(true);
        } else {
            attachmentLabel.setVisible(false);
            attachmentLabel.setManaged(false);
        }

      /*  if (isDailyTask()) {
            dailyBadge.setText("⚡ Daily Task");
            dailyBadge.setVisible(true);
            dailyBadge.setManaged(true);
        }*/

        // Show priority badge
        if (task.getPriority() != null) {
            priorityBadge.setText(task.getPriority());
            priorityBadge.getStyleClass().add("priority-" + task.getPriority().toLowerCase() + "-badge");
            priorityBadge.setVisible(true);
            priorityBadge.setManaged(true);
        }

        // Show weight
        if (task.getWeight() > 0) {
            weightLabel.setText("⚡ Weight: " + task.getWeight());
            weightLabel.setVisible(true);
            weightLabel.setManaged(true);
        }


        // Priority Indicator (based on due date proximity)
        setPriorityIndicator();

        // Action Button
        configureActionButton();
    }

    private void applyStatusStyle() {
        // Remove existing status classes
        titleLabel.getStyleClass().removeAll("task-title-completed", "task-title-progress");

        String status = task.getStatus().toUpperCase();
        switch (status) {
            case "COMPLETED":
                titleLabel.getStyleClass().add("task-title-completed");
                break;
            case "IN_PROGRESS":
                titleLabel.getStyleClass().add("task-title-progress");
                break;
        }
    }

    private void setPriorityIndicator() {
        // Remove existing priority classes
        priorityIndicator.getStyleClass().removeAll(
                "priority-high", "priority-medium", "priority-low", "priority-none"
        );

        if (task.getDueDate() == null || task.getDueDate().isBlank()) {
            priorityIndicator.getStyleClass().add("priority-none");
            return;
        }

        if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
            priorityIndicator.getStyleClass().add("priority-none");
            return;
        }

        try {
            LocalDate dueDate = LocalDate.parse(task.getDueDate());
            LocalDate today = LocalDate.now();
            long daysUntil = today.until(dueDate).getDays();

            if (daysUntil < 0) {
                // Overdue
                priorityIndicator.getStyleClass().add("priority-high");
            } else if (daysUntil <= 2) {
                // Due in 2 days or less
                priorityIndicator.getStyleClass().add("priority-high");
            } else if (daysUntil <= 7) {
                // Due this week
                priorityIndicator.getStyleClass().add("priority-medium");
            } else {
                // Due later
                priorityIndicator.getStyleClass().add("priority-low");
            }
        } catch (Exception e) {
            priorityIndicator.getStyleClass().add("priority-none");
        }
    }

    private void configureActionButton() {
        String status = task.getStatus().toUpperCase();

        switch (status) {
            case "PENDING":
                actionButton.setText("▶ Start Task");
                actionButton.setVisible(true);
                actionButton.setManaged(true);
                break;
            case "IN_PROGRESS":
                actionButton.setText("✓ Mark Complete");
                actionButton.setVisible(true);
                actionButton.setManaged(true);
                break;
            case "COMPLETED":
                actionButton.setVisible(false);
                actionButton.setManaged(false);
                break;
            default:
                actionButton.setText("Update");
                actionButton.setVisible(true);
                actionButton.setManaged(true);
        }

        actionButton.setOnAction(e -> {
            if (onStateAdvance != null) {
                onStateAdvance.accept(task);
            }
        });
    }

    private boolean isOverdue(String dateStr) {
        try {
            LocalDate dueDate = LocalDate.parse(dateStr);
            return dueDate.isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private String formatDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalDate today = LocalDate.now();

            if (date.equals(today)) {
                return "Today";
            } else if (date.equals(today.plusDays(1))) {
                return "Tomorrow";
            } else if (date.equals(today.minusDays(1))) {
                return "Yesterday";
            } else {
                return date.format(DateTimeFormatter.ofPattern("MMM dd"));
            }
        } catch (Exception e) {
            return dateStr;
        }
    }

    // ========== QUICK ACTIONS ==========

    @FXML
    private void viewTask() {
        if (parentController != null) {
            parentController.showTaskDetails(task);
        }
    }

    @FXML
    private void editTask() {
        if (parentController != null) {
            parentController.showEditTaskDialog(task);
        }
    }
}
