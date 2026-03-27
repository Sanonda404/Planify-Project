package com.planify.frontend.controllers.project;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.milestone.MilestoneCreateRequest;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import com.planify.frontend.utils.helpers.AlertCreator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AddMilestoneController {

    @FXML private TextField milestoneNameField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker deadlinePicker;
    @FXML private VBox taskListBox;

    private List<TaskDetails> unassignedTasks = new ArrayList<>();
    private String projectUuid;
    private String personalProjectName;
    private ProjectDetailsController projectDetailsController;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @FXML
    public void initialize() {
        // Set default deadline to 2 weeks from now
        deadlinePicker.setValue(LocalDate.now().plusWeeks(2));
    }

    public void setParent(ProjectDetailsController projectDetailsController) {
        this.projectDetailsController = projectDetailsController;
    }

    /**
     * Called by parent controller when opening the form
     */
    public void setUnassignedTasks(List<TaskDetails> tasks) {
        this.unassignedTasks.clear();
        if (tasks != null) {
            this.unassignedTasks.addAll(tasks);
        }
        loadTaskCheckboxes();
    }

    public void setProjectUuid(String projectUuid) {
        this.projectUuid = projectUuid;
    }

    public void setPersonalProjectName(String personalProjectName) {
        this.personalProjectName = personalProjectName;
    }

    private void loadTaskCheckboxes() {
        taskListBox.getChildren().clear();

        if (unassignedTasks == null || unassignedTasks.isEmpty()) {
            Label emptyLabel = new Label("No unassigned tasks available");
            emptyLabel.getStyleClass().add("am-empty-tasks");
            taskListBox.getChildren().add(emptyLabel);
            return;
        }

        for (TaskDetails task : unassignedTasks) {
            VBox taskItem = createStyledTaskItem(task);
            taskListBox.getChildren().add(taskItem);
        }
    }

    /**
     * Create a styled task item with checkbox, title, due date, and priority
     */
    private VBox createStyledTaskItem(TaskDetails task) {
        VBox item = new VBox(6);
        item.getStyleClass().add("am-task-item");
        item.setPadding(new Insets(12));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("am-task-checkbox");
        checkBox.setUserData(task);

        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("am-task-title");

        HBox metaBox = new HBox(12);
        Label dueLabel = new Label("📅 Due: " + formatDate(task.getDueDate()));
        dueLabel.getStyleClass().add("am-task-meta");

        Label priorityLabel = new Label(getPriorityText(task));
        priorityLabel.getStyleClass().addAll("am-task-priority", getPriorityClass(task));

        metaBox.getChildren().addAll(dueLabel, priorityLabel);
        infoBox.getChildren().addAll(titleLabel, metaBox);

        header.getChildren().addAll(checkBox, infoBox);
        item.getChildren().add(header);

        return item;
    }

    /**
     * Get priority text from task
     */
    private String getPriorityText(TaskDetails task) {
        // If task has priority field, use it
        try {
            String priority = task.getPriority();
            if (priority != null && !priority.isEmpty()) {
                return priority;
            }
        } catch (Exception e) {
            // Priority field might not exist yet
        }

        // Default based on due date
        if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
            try {
                LocalDate dueDate = LocalDate.parse(task.getDueDate());
                LocalDate today = LocalDate.now();
                long daysUntil = today.until(dueDate).getDays();
                if (daysUntil < 0) return "Overdue";
                if (daysUntil <= 2) return "High";
                if (daysUntil <= 7) return "Medium";
            } catch (Exception e) {
                // Ignore
            }
        }
        return "Medium";
    }

    /**
     * Get CSS class for priority badge
     */
    private String getPriorityClass(TaskDetails task) {
        String priority = getPriorityText(task).toLowerCase();
        switch (priority) {
            case "high":
            case "overdue":
                return "priority-high";
            case "medium":
                return "priority-medium";
            case "low":
                return "priority-low";
            default:
                return "priority-medium";
        }
    }

    /**
     * Format date for display
     */
    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "No date";
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(dateFormatter);
        } catch (Exception e) {
            return dateStr;
        }
    }

    @FXML
    private void createMilestone() {
        // Validate inputs
        String name = milestoneNameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Missing Name", "Please enter a milestone name.");
            return;
        }

        if (deadlinePicker.getValue() == null) {
            showAlert("Missing Deadline", "Please select a deadline date.");
            return;
        }

        String description = descriptionArea.getText();
        String deadline = deadlinePicker.getValue().format(isoFormatter);

        // Collect selected tasks
        List<TaskDetails> selectedTasks = new ArrayList<>();
        for (javafx.scene.Node node : taskListBox.getChildren()) {
            if (node instanceof VBox taskItem) {
                for (javafx.scene.Node child : taskItem.getChildren()) {
                    if (child instanceof HBox header) {
                        for (javafx.scene.Node headerChild : header.getChildren()) {
                            if (headerChild instanceof CheckBox cb && cb.isSelected()) {
                                TaskDetails task = (TaskDetails) cb.getUserData();
                                selectedTasks.add(task);
                            }
                        }
                    }
                }
            } else if (node instanceof CheckBox cb && cb.isSelected()) {
                // Fallback for simple checkboxes
                TaskDetails task = (TaskDetails) cb.getUserData();
                selectedTasks.add(task);
            }
        }

        // Personal project
        if (personalProjectName != null && !personalProjectName.isEmpty()) {
            saveMilestoneLocally(name, description, deadline, selectedTasks);
        }
        // Backend project
        else if (projectUuid != null && !projectUuid.trim().isEmpty()) {
            saveMilestoneToBackend(name, description, deadline, selectedTasks);
        }
        // Fallback - no project context
        else {
            showAlert("Error", "No project context found. Please try again.");
        }
    }

    private void saveMilestoneLocally(String name, String description, String deadline, List<TaskDetails> selectedTasks) {
        List<String> taskNames = new ArrayList<>();
        for (TaskDetails task : selectedTasks) {
            taskNames.add(task.getTitle());
        }

        if (!taskNames.isEmpty()) {
            ProjectDataManager.savePersonalProjectMilestone(
                    name, description, deadline, personalProjectName, taskNames
            );
        } else {
            ProjectDataManager.savePersonalProjectMilestone(
                    name, description, deadline, personalProjectName
            );
        }

        closeWindow();
        if (projectDetailsController != null) {
            projectDetailsController.refresh();
        }
        AlertCreator.showSuccessAlert("Milestone Created Successfully!");
    }

    private void saveMilestoneToBackend(String name, String description, String deadline, List<TaskDetails> selectedTasks) {
        List<String> taskUuids = new ArrayList<>();
        for (TaskDetails task : selectedTasks) {
            taskUuids.add(task.getUuid());
        }

        MilestoneCreateRequest request = new MilestoneCreateRequest(
                name, description, deadline, taskUuids, projectUuid
        );

        CreateRequestController.handleCreateMilestone(request, projectDetailsController);
        closeWindow();

        if (projectDetailsController != null) {
            projectDetailsController.refresh();
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) milestoneNameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}