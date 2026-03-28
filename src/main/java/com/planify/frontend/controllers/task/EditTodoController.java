package com.planify.frontend.controllers.task;

import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.project.ProjectSummary;
import com.planify.frontend.models.tasks.Category;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.data.personal.TaskDataManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class EditTodoController implements Initializable {

    // ========== FXML COMPONENTS ==========
    @FXML private TextField taskNameField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private DatePicker dueDatePicker;
    @FXML private Spinner<Integer> dueHour, dueMinute;
    @FXML private ComboBox<String> dueAmPm;
    @FXML private Slider weightSlider;
    @FXML private Label weightValueLabel;
    @FXML private ComboBox<String> priorityCombo;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private VBox newCategoryBox;
    @FXML private TextField newCategoryField;
    @FXML private VBox projectMilestoneBox;
    @FXML private TextField projectNameField;
    @FXML private TextField milestoneNameField;
    @FXML private VBox assigneesBox;
    @FXML private VBox assigneesContainer;
    @FXML private TextField attachmentField;
    @FXML private Button closeButton;

    // ========== DATA ==========
    private TaskDetails originalTask;
    private List<ProjectSummary> projectSummaries;
    private TodoController parentController;

    private List<CheckBox> assigneeCheckboxes = new ArrayList<>();
    private static final String CREATE_NEW_CATEGORY = "+ Create New Category";

    private final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTypeCombo();
        initializeTimeSpinners();
        initializeAmPmCombos();
        initializeWeightSlider();
        initializePriorityCombo();
        initializeCategoryCombo();
    }

    private void initializeTypeCombo() {
        typeCombo.setItems(FXCollections.observableArrayList("Daily (End of Day)", "Custom Date & Time"));
        typeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCustom = "Custom Date & Time".equals(newVal);
            dueDatePicker.setDisable(!isCustom);
            dueHour.setDisable(!isCustom);
            dueMinute.setDisable(!isCustom);
            dueAmPm.setDisable(!isCustom);
        });
    }

    private void initializeTimeSpinners() {
        dueHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 12));
        dueMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        dueMinute.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() == 1 && !newVal.isEmpty()) {
                dueMinute.getEditor().setText("0" + newVal);
            }
        });
    }

    private void initializeAmPmCombos() {
        dueAmPm.setItems(FXCollections.observableArrayList("AM", "PM"));
        dueAmPm.getSelectionModel().select("AM");
    }

    private void initializeWeightSlider() {
        weightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            weightValueLabel.setText(String.valueOf(newVal.intValue()));
        });
    }

    private void initializePriorityCombo() {
        priorityCombo.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
    }

    private void initializeCategoryCombo() {
        categoryCombo.getItems().add(CREATE_NEW_CATEGORY);
        categoryCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCreateNew = CREATE_NEW_CATEGORY.equals(newVal);
            newCategoryBox.setVisible(isCreateNew);
            newCategoryBox.setManaged(isCreateNew);
        });
    }

    public void setTask(TaskDetails task, List<ProjectSummary> projects, TodoController parent) {
        this.originalTask = task;
        this.projectSummaries = projects;
        this.parentController = parent;
        populateForm();
    }

    private void populateForm() {
        // Basic Info
        taskNameField.setText(originalTask.getTitle());
        descriptionField.setText(originalTask.getDescription());

        // Type
        boolean isDaily = originalTask.isDaily();
        typeCombo.setValue(isDaily ? "Daily (End of Day)" : "Custom Date & Time");

        // Due Date & Time
        if (originalTask.getDueDate() != null && !originalTask.getDueDate().isEmpty()) {
            try {
                LocalDateTime dueDateTime = LocalDateTime.parse(originalTask.getDueDate());
                dueDatePicker.setValue(dueDateTime.toLocalDate());
                int hour12 = convertTo12Hour(dueDateTime.getHour());
                dueHour.getValueFactory().setValue(hour12);
                dueMinute.getValueFactory().setValue(dueDateTime.getMinute());
                dueAmPm.setValue(dueDateTime.getHour() >= 12 ? "PM" : "AM");
            } catch (Exception e) {
                // Handle parse error
            }
        }

        // Weight
        weightSlider.setValue(originalTask.getWeight() > 0 ? originalTask.getWeight() : 5);

        // Priority
        if (originalTask.getPriority() != null && !originalTask.getPriority().isEmpty()) {
            priorityCombo.setValue(originalTask.getPriority());
        } else {
            priorityCombo.setValue("Medium");
        }

        // Category
        String category = originalTask.getCategory();
        if (category != null && !category.isEmpty()) {
            if (!categoryCombo.getItems().contains(category)) {
                categoryCombo.getItems().add(0, category);
            }
            categoryCombo.setValue(category);
        }

        // Project & Milestone (read-only)
        if (originalTask.getProjectName() != null && !originalTask.getProjectName().isEmpty()) {
            projectMilestoneBox.setVisible(true);
            projectMilestoneBox.setManaged(true);
            projectNameField.setText(originalTask.getProjectName());
            milestoneNameField.setText(originalTask.getMilestoneName() != null ? originalTask.getMilestoneName() : "None");
        }

        // Assignees (for project tasks)
        if (originalTask.getAssigneeMembers() != null && !originalTask.getAssigneeMembers().isEmpty()) {
            assigneesBox.setVisible(true);
            assigneesBox.setManaged(true);
            loadAssignees();
        }

        // Attachment
        if (originalTask.getAttachmentUrl() != null) {
            attachmentField.setText(originalTask.getAttachmentUrl());
        }

        // Load categories
        loadCategories();
    }

    private void loadCategories() {
        // Clear existing categories (keep only the special one)
        categoryCombo.getItems().clear();
        categoryCombo.getItems().add(CREATE_NEW_CATEGORY);

        // Add existing categories from personal tasks
        List<Category> categories = TaskDataManager.loadAllCategories();
        if (categories != null) {
            for (Category c: categories){
                categoryCombo.getItems().addAll(c.getCategoryNames());
            }
        }

        // Add original category if not present
        if (originalTask.getCategory() != null && !originalTask.getCategory().isEmpty()
                && !categoryCombo.getItems().contains(originalTask.getCategory())) {
            categoryCombo.getItems().add(0, originalTask.getCategory());
        }
    }

    private void loadAssignees() {
        assigneesContainer.getChildren().clear();
        assigneeCheckboxes.clear();

        for (MemberInfo member : originalTask.getAssigneeMembers()) {
            CheckBox cb = new CheckBox(member.getName());
            cb.setUserData(member);
            cb.setSelected(true);
            cb.getStyleClass().add("assignee-checkbox");
            assigneeCheckboxes.add(cb);
            assigneesContainer.getChildren().add(cb);
        }
    }

    private int convertTo12Hour(int hour24) {
        if (hour24 == 0 || hour24 == 12) return 12;
        return hour24 % 12;
    }

    private int convertTo24Hour(int hour12, String amPm) {
        if ("PM".equals(amPm) && hour12 != 12) return hour12 + 12;
        if ("AM".equals(amPm) && hour12 == 12) return 0;
        return hour12;
    }

    private LocalDateTime getDueDateTime() {
        String selectedType = typeCombo.getValue();

        if ("Daily (End of Day)".equals(selectedType)) {
            return LocalDate.now().atTime(23, 59, 59);
        } else {
            if (dueDatePicker.getValue() == null) return null;
            int hour24 = convertTo24Hour(dueHour.getValue(), dueAmPm.getValue());
            return dueDatePicker.getValue().atTime(hour24, dueMinute.getValue(), 0);
        }
    }

    private String getCategory() {
        String selected = categoryCombo.getValue();
        if (CREATE_NEW_CATEGORY.equals(selected)) {
            return newCategoryField.getText().trim().toLowerCase();
        }
        return selected;
    }

    private List<MemberInfo> getSelectedAssignees() {
        List<MemberInfo> selected = new ArrayList<>();
        for (CheckBox cb : assigneeCheckboxes) {
            if (cb.isSelected()) {
                selected.add((MemberInfo) cb.getUserData());
            }
        }
        return selected;
    }

    private boolean validateForm() {
        if (taskNameField.getText().trim().isEmpty()) {
            showAlert("Missing Name", "Please enter a task name.");
            return false;
        }

        if ("Custom Date & Time".equals(typeCombo.getValue()) && dueDatePicker.getValue() == null) {
            showAlert("Missing Due Date", "Please select a due date.");
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

    @FXML
    private void handleUpdateTask() {
        if (!validateForm()) return;

        String title = taskNameField.getText().trim();
        String description = descriptionField.getText();
        String category = getCategory();
        String dueDateTime = getDueDateTime().toString();
        int weight = (int) weightSlider.getValue();
        String priority = priorityCombo.getValue();
        String attachmentUrl = attachmentField.getText().trim();

        // Save new category if created
        if (CREATE_NEW_CATEGORY.equals(categoryCombo.getValue()) && !newCategoryField.getText().trim().isEmpty()) {
            TaskDataManager.saveCategory("", category);
        }

        // Personal task
      /*  if (originalTask.getUuid().trim().isEmpty()) {
            // Update in personal task manager
            TaskDataManager.updatePersonalTask(
                    originalTask.getTitle(), title, description, category, dueDateTime, attachmentUrl, weight, priority
            );

            // If it's part of a personal project
            if (originalTask.getProjectName() != null && !originalTask.getProjectName().isEmpty()) {
                ProjectDataManager.updatePersonalProjectTask(
                        originalTask.getProjectName(),
                        originalTask.getMilestoneName(),
                        originalTask.getTitle(),
                        title, description, category, dueDateTime, attachmentUrl
                );
            }
        }
        // Backend task
        else {
            List<MemberInfo> assignees = getSelectedAssignees();

            TaskRequest request = new TaskRequest(
                    title, description, category, dueDateTime,
                    originalTask.getProjectUuid(), originalTask.getMilestoneUuid(), weight, priority,
                    originalTask.getMilestoneName(), originalTask.getProjectName(),
                    LocalDataManager.getUserEmail(), assignees, attachmentUrl
            );

            EditRequestController.updateTask(originalTask.getUuid(), request, this);
        }

        AlertCreator.showSuccessAlert("Task updated successfully!");

        if (parentController != null) {
            parentController.refresh();
        }

        handleClose();*/
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}