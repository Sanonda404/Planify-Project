package com.planify.frontend.controllers.resources;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.resources.ResourceCreateRequest;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AddResourceController implements Initializable {

    // ========== FXML COMPONENTS ==========
    @FXML private Button closeButton;
    @FXML private TextField nameField;
    @FXML private TextField urlField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> typeCombo;

    // Task Selection
    @FXML private ToggleGroup taskToggleGroup;
    @FXML private RadioButton noTaskRadio;
    @FXML private RadioButton selectTaskRadio;
    @FXML private VBox taskSelectionBox;
    @FXML private TextField taskSearchField;
    @FXML private VBox taskListContainer;
    @FXML private Label selectedTaskLabel;

    // Project Selection (for resources not linked to tasks)
    @FXML private ToggleGroup projectToggleGroup;
    @FXML private RadioButton noProjectRadio;
    @FXML private RadioButton selectProjectRadio;
    @FXML private VBox projectSelectionBox;
    @FXML private VBox projectContextBox;
    @FXML private ComboBox<String> projectCombo;

    // ========== DATA ==========
    private Object parentController;
    private List<TaskDetails> allTasks = new ArrayList<>();
    private FilteredList<TaskDetails> filteredTasks;
    private TaskDetails selectedTask;
    private String selectedProjectName;
    private String selectedProjectUuid;

    // Context for pre-linked task (when opening from a task)
    private String preLinkedTaskUuid = "";
    private String preLinkedTaskName = "";
    private String preLinkedTaskProjectName = "";
    private boolean isPreLinked = false;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - h:mm a");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupInitialData();
        setupListeners();
    }

    /**
     * Set context for standalone resource (no pre-linked task)
     */
    public void setContextForProject(String projectUuid, String preLinkedTaskProjectName, List<TaskDetails>tasks, Object parent) {
        this.parentController = parent;
        this.selectedProjectUuid = projectUuid;
        this.preLinkedTaskProjectName = preLinkedTaskProjectName;
        this.allTasks.addAll(tasks);

        setupTaskFilter();

        // Show project context box
        projectContextBox.setVisible(false);
        projectContextBox.setManaged(false);
    }

    /**
     * Set context for resource linked to a specific task
     */
    public void setContextForTask(Object parent, String taskUuid, String taskName, String projectName, String projectUuid) {
        this.parentController = parent;
        this.preLinkedTaskUuid = taskUuid;
        this.preLinkedTaskName = taskName;
        this.preLinkedTaskProjectName = projectName;
        this.selectedProjectUuid = projectUuid;
        this.selectedProjectName = projectName;
        this.isPreLinked = true;

        // Hide task selection since it's pre-linked
        noTaskRadio.setVisible(false);
        noTaskRadio.setManaged(false);
        selectTaskRadio.setVisible(false);
        selectTaskRadio.setManaged(false);
        taskSelectionBox.setVisible(false);
        taskSelectionBox.setManaged(false);

        // Show selected task info
        selectedTaskLabel.setVisible(true);
        selectedTaskLabel.setManaged(true);
        selectedTaskLabel.setText("📌 Linked to: " + taskName);
        selectedTaskLabel.getStyleClass().add("prelinked-task-label");

        // Hide project context (already determined by task)
        projectContextBox.setVisible(false);
        projectContextBox.setManaged(false);

        // Set selected task
        preLinkedTaskName = taskName;
        preLinkedTaskUuid = taskUuid;
    }

    // ========== INITIALIZATION ==========

    private void setupInitialData() {
        // Resource types
        typeCombo.setItems(FXCollections.observableArrayList("LINK", "FILE", "IMAGE", "DOCUMENT"));
        typeCombo.getSelectionModel().selectFirst();

        // Task toggle group
        taskToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean showTaskSelection = selectTaskRadio.isSelected();
            taskSelectionBox.setVisible(showTaskSelection);
            taskSelectionBox.setManaged(showTaskSelection);
            taskSearchField.setVisible(showTaskSelection);
            taskSearchField.setManaged(showTaskSelection);

            if (!showTaskSelection) {
                selectedTask = null;
                selectedTaskLabel.setVisible(false);
                selectedTaskLabel.setManaged(false);
            }
        });

        // Project toggle group
        projectToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean showProjectSelection = selectProjectRadio.isSelected();
            projectSelectionBox.setVisible(showProjectSelection);
            projectSelectionBox.setManaged(showProjectSelection);
        });

        // Search field listener
        taskSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredTasks != null) {
                filteredTasks.setPredicate(task -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    String searchLower = newVal.toLowerCase();
                    return task.getTitle().toLowerCase().contains(searchLower) ||
                            (task.getProjectName() != null && task.getProjectName().toLowerCase().contains(searchLower));
                });
                updateTaskList();
            }
        });
    }

    private void setupListeners() {
        // No additional listeners needed
    }

    private void setupTaskFilter() {
        if (allTasks == null) {
            allTasks = GroupProjectDataManager.getGroupProjectTasks(selectedProjectUuid);
        }

        if (!allTasks.isEmpty()) {
            filteredTasks = new FilteredList<>(FXCollections.observableArrayList(allTasks), task -> true);
            updateTaskList();
        } else {
            taskListContainer.getChildren().clear();
            Label loadingLabel = new Label("No tasks available");
            loadingLabel.getStyleClass().add("empty-tasks-label");
            taskListContainer.getChildren().add(loadingLabel);
        }
    }

    private void updateTaskList() {
        taskListContainer.getChildren().clear();

        if (filteredTasks == null || filteredTasks.isEmpty()) {
            Label emptyLabel = new Label("No tasks found");
            emptyLabel.getStyleClass().add("empty-tasks-label");
            taskListContainer.getChildren().add(emptyLabel);
            return;
        }

        for (TaskDetails task : filteredTasks) {
            VBox taskItem = createTaskItem(task);
            taskListContainer.getChildren().add(taskItem);
        }
    }

    private VBox createTaskItem(TaskDetails task) {
        VBox item = new VBox(4);
        item.getStyleClass().add("task-item");
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setCursor(javafx.scene.Cursor.HAND);

        // Task title
        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("task-item-title");

        // Task details row
        HBox detailsRow = new HBox(12);
        detailsRow.setAlignment(Pos.CENTER_LEFT);

        Label projectLabel = new Label("📁 " + (task.getProjectName() != null ? task.getProjectName() : "No Project"));
        projectLabel.getStyleClass().add("task-item-project");

        Label dueLabel = new Label("📅 " + formatDate(task.getDueDate()));
        dueLabel.getStyleClass().add("task-item-due");

        // Status badge
        Label statusLabel = new Label(task.getStatus());
        statusLabel.getStyleClass().addAll("task-status-badge", getStatusClass(task.getStatus()));

        detailsRow.getChildren().addAll(projectLabel, dueLabel, statusLabel);

        item.getChildren().addAll(titleLabel, detailsRow);

        item.setOnMouseClicked(e -> selectTask(task));

        return item;
    }

    private void selectTask(TaskDetails task) {
        this.selectedTask = task;
        selectedTaskLabel.setVisible(true);
        selectedTaskLabel.setManaged(true);
        selectedTaskLabel.setText("✓ Selected: " + task.getTitle() + " (" + task.getProjectName() + ")");
        selectedTaskLabel.getStyleClass().add("selected-task-label");

        // Auto-fill project info from task
        if (task.getProjectName() != null) {
            selectedProjectName = task.getProjectName();
            selectedProjectUuid = task.getProjectUuid();
        }

        // Close task selection
        selectTaskRadio.setSelected(false);
        noTaskRadio.setSelected(true);
    }

    private String getStatusClass(String status) {
        if (status == null) return "status-pending";
        switch (status.toUpperCase()) {
            case "COMPLETED": return "status-completed";
            case "IN_PROGRESS": return "status-in-progress";
            default: return "status-pending";
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "No date";
        try {
            LocalDateTime date = LocalDateTime.parse(dateStr);
            return date.format(formatter);
        } catch (Exception e) {
            return dateStr;
        }
    }

    // ========== VALIDATION ==========

    private boolean validateForm() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showAlert("Missing Name", "Please enter a resource name.");
            return false;
        }

        if (urlField.getText() == null || urlField.getText().trim().isEmpty()) {
            showAlert("Missing URL", "Please enter a URL or file path.");
            return false;
        }

        if (typeCombo.getValue() == null) {
            showAlert("Missing Type", "Please select a resource type.");
            return false;
        }

        return true;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========== SUBMISSION ==========

    @FXML
    private void handleAddResource() {
        if (!validateForm()) return;

        String name = nameField.getText().trim();
        String description = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
        String type = typeCombo.getValue();
        String url = urlField.getText().trim();
        String sourceUuid = null;
        String sourceName = null;

        // Determine source (task or project)
        if (isPreLinked) {
            sourceUuid = preLinkedTaskUuid;
            sourceName = preLinkedTaskName;
        } else if (selectTaskRadio.isSelected() && selectedTask != null) {
            sourceUuid = selectedTask.getUuid();
            sourceName = selectedTask.getTitle();
        } else if (selectProjectRadio.isSelected() && projectCombo.getValue() != null) {
            // Resource linked to project, not a specific task
            sourceUuid = selectedProjectUuid;
            sourceName = projectCombo.getValue();
        }

        MemberInfo creator = new MemberInfo(LocalDataManager.getUserName(), LocalDataManager.getUserEmail());
        String addedAt = LocalDateTime.now().toString();

        System.out.println("SOurce: "+sourceUuid+"Name: "+sourceName);

        ResourceCreateRequest request = new ResourceCreateRequest(
                name, description, type, url, creator, addedAt, selectedProjectUuid, sourceUuid, sourceName
        );

        // TODO: Call your backend API
        System.out.println("Creating resource: " + request);
        CreateRequestController.handleCreateResource(request, parentController);

        closeWindow();
    }

    @FXML
    private void handleClose() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}