package com.planify.frontend.controllers.project;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.tasks.TaskRequest;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import com.planify.frontend.utils.data.personal.TaskDataManager;
import com.planify.frontend.utils.helpers.AlertCreator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class AddTaskController implements Initializable {

    // ========== FXML COMPONENTS ==========

    @FXML private Button closeButton;
    @FXML private TextField taskNameField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<String> milestoneCombo;
    @FXML private TextField attachmentField;

    // Category Components
    @FXML private ComboBox<String> categoryCombo;
    @FXML private VBox newCategoryBox;
    @FXML private TextField newCategoryField;

    // Assignee Components
    @FXML private ToggleGroup assigneeToggleGroup;
    @FXML private RadioButton assignAllRadio;
    @FXML private RadioButton assignSpecificRadio;
    @FXML private VBox specificMembersBox;
    @FXML private VBox membersCheckboxContainer;
    @FXML private Label selectedCountLabel;
    @FXML private Spinner<Integer> targetHour, targetMinute;

    // ========== DATA ==========

    private String projectUuid;
    private List<MilestoneDetails> milestones = new ArrayList<>();
    private List<MemberInfo> members = new ArrayList<>();
    private ProjectDetailsController projectDetailsController;

    private List<CheckBox> memberCheckboxes = new ArrayList<>();
    private static final String CREATE_NEW_CATEGORY = "+ Create New Category";

    // TODO: Load from backend
    private List<String> existingCategories = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    // ========== CONTEXT SETUP ==========

    public void setContext(String projectUuid, List<MilestoneDetails> milestones,
                           List<MemberInfo> members, ProjectDetailsController projectDetailsController) {
        this.projectUuid = projectUuid;
        this.milestones = milestones;
        this.members = members;
        this.projectDetailsController = projectDetailsController;

        populateMilestones();
        populateMembers();
        setupCategoryComboBox();
        setupAssigneeListeners();
    }

    // ========== INITIALIZATION ==========

    private void setupCategoryComboBox() {
        categoryCombo.getItems().clear();

        // TODO: Backend Integration - Load categories from database
        existingCategories.addAll(TaskDataManager.getProjectCategories(projectDetailsController.getName()));
        categoryCombo.getItems().addAll(existingCategories);

        // Add "Create New" option at the end
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

    private void setupAssigneeListeners() {
        // Listen to radio button changes
        assigneeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSpecific = assignSpecificRadio.isSelected();
            specificMembersBox.setVisible(isSpecific);
            specificMembersBox.setManaged(isSpecific);

            if (isSpecific) {
                updateSelectedCount();
            }
        });
    }

    // ========== POPULATION ==========

    private void populateMilestones() {
        milestoneCombo.getItems().clear();
        milestoneCombo.getItems().add("None"); // Optional milestone
        milestoneCombo.getItems().addAll(
                milestones.stream()
                        .map(MilestoneDetails::getTitle)
                        .collect(Collectors.toList())
        );
        milestoneCombo.getSelectionModel().selectFirst();
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
        checkbox.setUserData(member); // Store member object

        // Update count on change
        checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> updateSelectedCount());

        return checkbox;
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

        // 1. Task Name
        if (taskNameField.getText() == null || taskNameField.getText().trim().isEmpty()) {
            errors.add("Task name is required");
        }

        // 2. Category
        String category = getSelectedCategory();
        if (category == null || category.trim().isEmpty()) {
            if (CREATE_NEW_CATEGORY.equals(categoryCombo.getValue())) {
                errors.add("Please enter a new category name");
            } else {
                errors.add("Category is required");
            }
        }

        // 3. Due Date
        if (dueDatePicker.getValue() == null) {
            errors.add("Due date is required");
        }

        // 4. Assignees
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

    private List<MemberInfo> getAssigneeEmails() {
        if (assignAllRadio.isSelected()) {
            // Return all member emails
            return members;
        } else {
            // Return selected member emails
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

    // ========== EVENT HANDLERS ==========

    @FXML
    private void addTask() {
        // Validate form
        if (!validateForm()) {
            return;
        }

        // Gather data
        String title = taskNameField.getText().trim();
        String description = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
        String category = getSelectedCategory();
        String dueDate = getDateTime(dueDatePicker, targetHour, targetMinute).toString();
        String milestoneUuid = getMilestoneUuid();
        List<MemberInfo> assigneeEmails = getAssigneeEmails();
        String attachmentUrl = attachmentField.getText() != null ? attachmentField.getText().trim() : "";

        // Create request
        TaskRequest request = new TaskRequest(
                title,
                description,
                category,
                dueDate,
                projectUuid,
                milestoneUuid,
                false,
                0,
                "Medium",
                LocalDataManager.getUserEmail(),
                milestoneCombo.getValue(),
                projectDetailsController.getName(),
                assigneeEmails,
                attachmentUrl
        );

        // Save to backend
        if(projectUuid.trim().isEmpty())saveLocally(title,description,category, dueDate,false, milestoneCombo.getValue(), projectDetailsController.getName(), attachmentUrl);
        else saveToBackend(request);

        //Save new category
        if (CREATE_NEW_CATEGORY.equals(categoryCombo.getValue()) && category != null) {
            saveCategory(category);
        }

        // Close window
        closeWindow();
    }

    private LocalDateTime getDateTime(DatePicker datePicker, Spinner<Integer> hourSpinner, Spinner<Integer> minuteSpinner) {
        if (datePicker.getValue() == null) {
            return null;
        }
        return datePicker.getValue().atTime(
                (hourSpinner.getValue()==null?12:hourSpinner.getValue()),
                (minuteSpinner.getValue()==null?0:minuteSpinner.getValue()),
                15
        );
    }

    private void saveLocally(String title, String description,String category, String dueDate, boolean isDaily, String milestoneName, String projectName, String attachmentUrl){
        ProjectDataManager.savePersonalProjectTask(title, description, category, dueDate, isDaily, 0, "Medium", projectName, milestoneName, attachmentUrl);
        projectDetailsController.refresh();
        AlertCreator.showSuccessAlert("Task Created Successfully!!");
    }

    // ========== BACKEND INTEGRATION ==========

    /**
     * TODO: Backend Integration - Save task and optionally save new category
     */
    private void saveToBackend(TaskRequest request) {
        // Save task
        CreateRequestController.handleCreateTask(request, projectDetailsController);
        System.out.println("Task created: " + request);

        // TODO: If category is new, save it for future use


        // Refresh project details
        if (projectDetailsController != null) {
            projectDetailsController.refresh();
        }
    }


    private void saveCategory(String category) {
        TaskDataManager.saveCategory(projectDetailsController.getName(),category);
    }

    @FXML
    private void closeWindow() {
        if (projectDetailsController != null) {
            projectDetailsController.refresh();
        }
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
