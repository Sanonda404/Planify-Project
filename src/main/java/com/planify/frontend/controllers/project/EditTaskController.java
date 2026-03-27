package com.planify.frontend.controllers.project;

import com.planify.frontend.controllers.Request.EditRequestController;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import com.planify.frontend.utils.helpers.AlertCreator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class EditTaskController implements Initializable {

    // ========== FXML COMPONENTS ==========
    
    @FXML private Button closeButton;
    @FXML private TextField taskNameField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<String> milestoneCombo;
    @FXML private TextField attachmentField;
    @FXML private ComboBox<String> statusCombo;
    
    // Category Components
    @FXML private ComboBox<String> categoryCombo;
    @FXML private VBox newCategoryBox;
    @FXML private TextField newCategoryField;
    
    // Creator Info
    @FXML private Label creatorLabel;
    
    // Assignee Components
    @FXML private ToggleGroup assigneeToggleGroup;
    @FXML private RadioButton assignAllRadio;
    @FXML private RadioButton assignSpecificRadio;
    @FXML private VBox specificMembersBox;
    @FXML private VBox membersCheckboxContainer;
    @FXML private Label selectedCountLabel;
    @FXML private Spinner<Integer> targetHour, targetMinute;

    // ========== DATA ==========
    
    private TaskDetails task;
    private String projectUuid;
    private List<MilestoneDetails> milestones = new ArrayList<>();
    private List<MemberInfo> members = new ArrayList<>();
    private ProjectDetailsController projectDetailsController;
    
    private List<CheckBox> memberCheckboxes = new ArrayList<>();
    private static final String CREATE_NEW_CATEGORY = "+ Create New Category";
    
    // TODO: Load from backend
    private List<String> existingCategories = Arrays.asList(
        "Development",
        "Design",
        "Testing",
        "Documentation",
        "Bug Fix",
        "Feature"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCategoryComboBox();
        setupAssigneeListeners();
        setupStatusComboBox();
    }

    // ========== SETUP ==========
    
    /**
     * Set the task to edit and pre-populate all fields
     */
    public void setTask(TaskDetails task, String projectUuid, 
                       List<MilestoneDetails> milestones, List<MemberInfo> members,
                       ProjectDetailsController projectDetailsController) {
        this.task = task;
        this.projectUuid = projectUuid;
        this.milestones = milestones;
        this.members = members;
        this.projectDetailsController = projectDetailsController;
        
        populateMilestones();
        populateMembers();
        populateTaskData();
    }
    
    private void setupCategoryComboBox() {
        categoryCombo.getItems().clear();
        
        // TODO: Backend Integration - Load categories from database
        categoryCombo.getItems().addAll(existingCategories);
        categoryCombo.getItems().add(CREATE_NEW_CATEGORY);
        
        // Listen for selection changes
        categoryCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCreateNew = CREATE_NEW_CATEGORY.equals(newVal);
            newCategoryBox.setVisible(isCreateNew);
            newCategoryBox.setManaged(isCreateNew);
            
            if (!isCreateNew) {
                newCategoryField.clear();
            }
        });
    }
    
    private void setupStatusComboBox() {
        statusCombo.getItems().addAll("PENDING", "IN_PROGRESS", "COMPLETED");
    }
    
    private void setupAssigneeListeners() {
        assigneeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSpecific = assignSpecificRadio.isSelected();
            specificMembersBox.setVisible(isSpecific);
            specificMembersBox.setManaged(isSpecific);
            
            if (isSpecific) {
                updateSelectedCount();
            }
        });
    }
    
    private void populateMilestones() {
        milestoneCombo.getItems().clear();
        milestoneCombo.getItems().add("None");
        milestoneCombo.getItems().addAll(
            milestones.stream()
                .map(MilestoneDetails::getTitle)
                .collect(Collectors.toList())
        );
    }
    
    private void populateMembers() {
        membersCheckboxContainer.getChildren().clear();
        memberCheckboxes.clear();
        
        for (MemberInfo member : members) {
            CheckBox checkbox = createMemberCheckbox(member);
            memberCheckboxes.add(checkbox);
            membersCheckboxContainer.getChildren().add(checkbox);
        }
        
        updateSelectedCount();
    }
    
    private CheckBox createMemberCheckbox(MemberInfo member) {
        CheckBox checkbox = new CheckBox();
        checkbox.getStyleClass().add("member-checkbox");
        
        // Create custom graphic with name and email
        VBox labelBox = new VBox(2);
        Label nameLabel = new Label(member.getName());
        nameLabel.getStyleClass().add("member-name");
        Label emailLabel = new Label(member.getEmail());
        emailLabel.getStyleClass().add("member-email");
        labelBox.getChildren().addAll(nameLabel, emailLabel);
        
        checkbox.setGraphic(labelBox);
        checkbox.setUserData(member);
        
        // Update count on change
        checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> updateSelectedCount());
        
        return checkbox;
    }
    
    private void populateTaskData() {
        // Basic fields
        taskNameField.setText(task.getTitle());
        descriptionField.setText(task.getDescription());
        
        // Due date
        try {
            LocalDate dueDate = LocalDateTime.parse(task.getDueDate()).toLocalDate();
            LocalTime dueTime = LocalDateTime.parse(task.getDueDate()).toLocalTime();
            SpinnerValueFactory<Integer> valueHour =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23,dueTime.getHour()); // min, max, initial
            SpinnerValueFactory<Integer> valueMinute =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59,dueTime.getMinute());
            targetHour.setValueFactory(valueHour);
            targetMinute.setValueFactory(valueMinute);
            dueDatePicker.setValue(dueDate);
        } catch (Exception e) {
            System.err.println("Error parsing due date: " + e.getMessage());
        }
        
        // Category
        if (existingCategories.contains(task.getCategory())) {
            categoryCombo.setValue(task.getCategory());
        } else {
            // Custom category - add it to list
            categoryCombo.getItems().add(0, task.getCategory());
            categoryCombo.setValue(task.getCategory());
        }
        
        // Status
        statusCombo.setValue(task.getStatus());
        
        // Milestone
        if (task.getMilestoneName() != null && !task.getMilestoneName().isEmpty()) {
            milestoneCombo.setValue(task.getMilestoneName());
        } else {
            milestoneCombo.setValue("None");
        }
        
        // Creator
        creatorLabel.setText(task.getCreator().getName() + " (" + task.getCreator().getEmail() + ")");
        
        // Attachment
        if (task.getAttachmentUrl() != null) {
            attachmentField.setText(task.getAttachmentUrl());
        }
        
        // Assignees
        populateAssignees();
    }
    
    private void populateAssignees() {
        List<String> assigneeEmails = task.getAssigneeMembers().stream()
            .map(MemberInfo::getEmail)
            .collect(Collectors.toList());
        
        // Check if all members are assigned
        boolean allAssigned = assigneeEmails.size() == members.size() &&
                             assigneeEmails.containsAll(members.stream()
                                 .map(MemberInfo::getEmail)
                                 .collect(Collectors.toList()));
        
        if (allAssigned) {
            assignAllRadio.setSelected(true);
            specificMembersBox.setVisible(false);
            specificMembersBox.setManaged(false);
        } else {
            assignSpecificRadio.setSelected(true);
            specificMembersBox.setVisible(true);
            specificMembersBox.setManaged(true);
            
            // Check the assigned members
            for (CheckBox checkbox : memberCheckboxes) {
                MemberInfo member = (MemberInfo) checkbox.getUserData();
                checkbox.setSelected(assigneeEmails.contains(member.getEmail()));
            }
            
            updateSelectedCount();
        }
    }
    
    private void updateSelectedCount() {
        long count = memberCheckboxes.stream()
            .filter(CheckBox::isSelected)
            .count();
        selectedCountLabel.setText(count + " member" + (count != 1 ? "s" : "") + " selected");
    }

    // ========== VALIDATION ==========
    
    private boolean validateForm() {
        List<String> errors = new ArrayList<>();
        
        // Task Name
        if (taskNameField.getText() == null || taskNameField.getText().trim().isEmpty()) {
            errors.add("Task name is required");
        }
        
        // Category
        String category = getSelectedCategory();
        if (category == null || category.trim().isEmpty()) {
            if (CREATE_NEW_CATEGORY.equals(categoryCombo.getValue())) {
                errors.add("Please enter a new category name");
            } else {
                errors.add("Category is required");
            }
        }
        
        // Due Date
        if (dueDatePicker.getValue() == null || targetHour.getValue() ==null || targetMinute == null) {
            errors.add("Due date and Time is required");
        }
        
        // Status
        if (statusCombo.getValue() == null) {
            errors.add("Status is required");
        }
        
        // Assignees
        if (assignSpecificRadio.isSelected()) {
            long selectedCount = memberCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .count();
            
            if (selectedCount == 0) {
                errors.add("Please select at least one member to assign the task");
            }
        }
        
        // Show errors if any
        if (!errors.isEmpty()) {
            showValidationAlert(errors);
            return false;
        }
        
        return true;
    }
    
    private void showValidationAlert(List<String> errors) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation Error");
        alert.setHeaderText("Please fix the following errors:");
        
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            content.append((i + 1)).append(". ").append(errors.get(i));
            if (i < errors.size() - 1) {
                content.append("\n");
            }
        }
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    // ========== DATA GATHERING ==========
    
    private String getSelectedCategory() {
        if (CREATE_NEW_CATEGORY.equals(categoryCombo.getValue())) {
            String newCategory = newCategoryField.getText();
            if (newCategory != null && !newCategory.trim().isEmpty()) {
                return newCategory.trim();
            }
            return null;
        }
        return categoryCombo.getValue();
    }
    
    private List<MemberInfo> getAssignees() {
        if (assignAllRadio.isSelected()) {
            return members;
        } else {
            List<MemberInfo>newAssignee = new ArrayList<>();
            return memberCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> ((MemberInfo) cb.getUserData()))
                .collect(Collectors.toList());
        }
    }
    
    private String getMilestoneUuid() {
        String selectedMilestone = milestoneCombo.getValue();
        
        if (selectedMilestone == null || "None".equals(selectedMilestone)) {
            return null;
        }
        
        return milestones.stream()
            .filter(m -> m.getTitle().equals(selectedMilestone))
            .findFirst()
            .map(MilestoneDetails::getUuid)
            .orElse(null);
    }
    
    private boolean hasChanges() {
        String currentTitle = taskNameField.getText().trim();
        String currentDescription = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
        String currentCategory = getSelectedCategory();
        String currentDueDate = getDateTime(dueDatePicker, targetHour, targetMinute).toString();
        String currentStatus = statusCombo.getValue();
        String currentMilestone = getMilestoneUuid();
        String currentAttachment = attachmentField.getText() != null ? attachmentField.getText().trim() : "";
        List<MemberInfo> currentAssignees = getAssignees();
        
        // Compare with original task
        boolean titleChanged = !currentTitle.equals(task.getTitle());
        boolean descriptionChanged = !currentDescription.equals(task.getDescription() != null ? task.getDescription() : "");
        boolean categoryChanged = !currentCategory.equals(task.getCategory());
        boolean dueDateChanged = !currentDueDate.equals(task.getDueDate());
        boolean statusChanged = !currentStatus.equals(task.getStatus());
        boolean attachmentChanged = !currentAttachment.equals(task.getAttachmentUrl() != null ? task.getAttachmentUrl() : "");
        boolean milestoneChanged = !currentMilestone.equals(task.getMilestoneName() !=null ? task.getMilestoneName(): "");
        // Check assignees
        List<String> originalAssignees = task.getAssigneeMembers().stream()
            .map(MemberInfo::getEmail)
            .sorted()
            .collect(Collectors.toList());
        List<MemberInfo> newAssignees = currentAssignees.stream()
            .sorted()
            .collect(Collectors.toList());
        boolean assigneesChanged = !originalAssignees.equals(newAssignees);
        
        return titleChanged || descriptionChanged || categoryChanged || 
               dueDateChanged || statusChanged || attachmentChanged || assigneesChanged || milestoneChanged;
    }

    private LocalDateTime getDateTime(DatePicker datePicker, Spinner<Integer> hourSpinner, Spinner<Integer> minuteSpinner) {
        if (datePicker.getValue() == null) {
            return null;
        }
        return datePicker.getValue().atTime(
                hourSpinner.getValue(),
                minuteSpinner.getValue(),
                15
        );
    }

    // ========== EVENT HANDLERS ==========
    
    @FXML
    private void handleSave() {
        // Validate form
        if (!validateForm()) {
            return;
        }
        
        // Check if anything changed
        if (!hasChanges()) {
            showInfoAlert("No changes detected", "No modifications were made to the task.");
            return;
        }
        
        // Gather updated data
        String updatedTitle = taskNameField.getText().trim();
        String updatedDescription = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
        String updatedCategory = getSelectedCategory();
        String updatedDueDate = getDateTime(dueDatePicker, targetHour, targetMinute).toString();
        String updatedStatus = statusCombo.getValue();
        String updatedMilestoneUuid = getMilestoneUuid();
        List<MemberInfo> updatedAssignees = getAssignees();
        String updatedAttachment = attachmentField.getText() != null ? attachmentField.getText().trim() : "";
        
        // Update task object
        task.setTitle(updatedTitle);
        task.setDescription(updatedDescription);
        task.setCategory(updatedCategory);
        task.setDueDate(updatedDueDate);
        task.setStatus(updatedStatus);
        task.setAttachmentUrl(updatedAttachment);
        task.setMilestoneUuid(updatedMilestoneUuid);
        task.setMilestoneName(milestoneCombo.getValue());
        task.setAssigneeMembers(updatedAssignees);
        task.setCategory(updatedCategory);
        
        // Save
        if(task.getProjectUuid().trim().isEmpty())saveLocally(task);
        else saveToBackend(task);
        
        // Close window
        handleClose();
    }

    private void saveLocally(TaskDetails task){
        ProjectDataManager.updatePersonalTask(task);
        projectDetailsController.refresh();
        AlertCreator.showSuccessAlert("Task Updated Successfully!!");
    }
    
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    // ========== BACKEND INTEGRATION ==========
    
    /**
     * TODO: Backend Integration - Update task in database
     */
    private void saveToBackend(TaskDetails task) {
        // TODO: Call backend API to update task
        // Example: taskService.updateTask(task.getUuid(), taskUpdateRequest);
        EditRequestController.updateTask(task.getUuid(), LocalDataManager.getUserEmail(), task, projectDetailsController);

        
        // Refresh parent view
        if (projectDetailsController != null) {
            projectDetailsController.refresh();
        }
    }
    
    /**
     * TODO: Backend Integration - Save new category to database
     */
    private void saveCategoryToBackend(String category) {
        // TODO: Call backend API to save category
        System.out.println("TODO: Save new category to backend: " + category);
    }

    /**
     * Show info notification
     */
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
