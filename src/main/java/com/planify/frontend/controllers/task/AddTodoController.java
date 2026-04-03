package com.planify.frontend.controllers.task;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.project.MilestoneSummary;
import com.planify.frontend.models.project.ProjectSummary;
import com.planify.frontend.models.tasks.TaskRequest;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
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
import java.util.*;
import java.util.stream.Collectors;

public class AddTodoController implements Initializable {

    // ========== FXML COMPONENTS ==========
    @FXML private Button closeButton;
    @FXML private TextField taskNameField, attachmentField, newCategoryField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<String> categoryCombo, milestoneCombo, typeCombo, todoForCombo, projectCombo, priorityCombo;
    @FXML private ComboBox<String> targetAmPm;
    @FXML private Slider weightSlider;
    @FXML private Label weightValueLabel;

    // Layout Containers
    @FXML private VBox newCategoryBox, specificMembersBox, membersCheckboxContainer;
    @FXML private VBox projectSelectionBox, milestoneSelectionBox, assignToSection, typeContainer, addToContainer;
    @FXML private VBox targetTimeContainer;

    @FXML private Label selectedCountLabel;
    @FXML private Spinner<Integer> targetHour, targetMinute;
    @FXML private ToggleGroup assigneeToggleGroup;
    @FXML private RadioButton assignAllRadio, assignSpecificRadio;

    // ========== DATA ==========
    private List<ProjectSummary> summaries;
    private Object parentController;
    private List<CheckBox> memberCheckboxes = new ArrayList<>();
    private List<MilestoneDetails> milestoneDetails = new ArrayList<>();

    // Task context (for editing/updating from project)
    private String existingProjectUuid;
    private String existingProjectName;
    private String existingMilestoneUuid;
    private String existingMilestoneName;
    private boolean isProjectTask = false;

    private static final String CREATE_NEW_CATEGORY = "+ Create New Category";
    private static final String TYPE_DAILY = "Daily (End of Day)";
    private static final String TYPE_CUSTOM = "Custom Date & Time";
    private static final String FOR_PERSONAL = "Personal";
    private static final String FOR_PROJECT = "Project";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupInitialData();
        setupListeners();
    }

    /**
     * Set context for creating a task (without pre-existing project/milestone)
     */
    public void setContext(List<ProjectSummary> projectSummaries, Object parent) {
        this.summaries = projectSummaries;
        this.parentController = parent;
        this.isProjectTask = false;
        this.existingProjectUuid = null;
        this.existingProjectName = null;
        this.existingMilestoneUuid = null;
        this.existingMilestoneName = null;

        // Show all selection options
        addToContainer.setVisible(true);
        addToContainer.setManaged(true);
        projectSelectionBox.setVisible(true);
        projectSelectionBox.setManaged(true);
        milestoneSelectionBox.setVisible(true);
        milestoneSelectionBox.setManaged(true);
        typeContainer.setVisible(true);
        typeContainer.setManaged(true);
        assignToSection.setVisible(true);
        assignToSection.setManaged(true);

        // Populate project list
        if (summaries != null) {
            projectCombo.getItems().setAll(
                    summaries.stream().map(ProjectSummary::getName).collect(Collectors.toList())
            );
        }
    }

    /**
     * Set context for creating a task from a specific project/milestone
     * This hides project/milestone selection since they're already known
     */
    public void setContextForProject(String projectUuid, String projectName,
                                     List<MilestoneDetails>milestoneDetails, List<MemberInfo> projectMembers,
                                     Object parent) {
        this.parentController = parent;
        this.isProjectTask = true;
        this.existingProjectUuid = projectUuid;
        this.existingProjectName = projectName;
        this.summaries = null;

        // Hide Add To, Project, Milestone selection since they're pre-defined
        addToContainer.setVisible(false);
        addToContainer.setManaged(false);
        projectSelectionBox.setVisible(false);
        projectSelectionBox.setManaged(false);
        existingMilestoneName = null;
        existingMilestoneUuid  = null;

        // Show type container (always show for project tasks - they need due date)
        typeContainer.setVisible(true);
        typeContainer.setManaged(true);
        assignToSection.setVisible(true);
        assignToSection.setManaged(true);

        if(milestoneDetails!=null && !milestoneDetails.isEmpty()){
            populateMilestoneDetails(milestoneDetails);
            this.milestoneDetails.addAll(milestoneDetails);
        }

        // Populate members for this project
        if (projectMembers != null && !projectMembers.isEmpty()) {
            populateMembers(projectMembers);
        }
    }

    public void setContextForMilestone(String projectUuid, String projectName,
                                       String milestoneUuid, String milestoneName,
                                       List<MemberInfo> projectMembers,
                                       Object parent) {
        this.parentController = parent;
        this.isProjectTask = true;
        this.existingProjectUuid = projectUuid;
        this.existingProjectName = projectName;
        this.existingMilestoneUuid = milestoneUuid;
        this.existingMilestoneName = milestoneName;
        this.summaries = null;

        // Hide Add To, Project, Milestone selection since they're pre-defined
        addToContainer.setVisible(false);
        addToContainer.setManaged(false);
        projectSelectionBox.setVisible(false);
        projectSelectionBox.setManaged(false);
        milestoneSelectionBox.setVisible(false);
        milestoneSelectionBox.setManaged(false);

        // Show type container (always show for project tasks - they need due date)
        typeContainer.setVisible(true);
        typeContainer.setManaged(true);
        assignToSection.setVisible(true);
        assignToSection.setManaged(true);

        // Populate members for this project
        if (projectMembers != null && !projectMembers.isEmpty()) {
            populateMembers(projectMembers);
        }
    }


    // ========== INITIALIZATION ==========

    private void setupInitialData() {
        // Task Type Combo
        typeCombo.setItems(FXCollections.observableArrayList(TYPE_DAILY, TYPE_CUSTOM));
        typeCombo.getSelectionModel().select(TYPE_DAILY);

        // Add To Combo
        todoForCombo.setItems(FXCollections.observableArrayList(FOR_PERSONAL, FOR_PROJECT));
        todoForCombo.getSelectionModel().select(FOR_PERSONAL);

        // Priority Combo
        priorityCombo.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
        priorityCombo.getSelectionModel().select("Medium");

        // Weight Slider
        weightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            weightValueLabel.setText(String.valueOf(newVal.intValue()));
        });

        // Time Spinners
        targetHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 12));
        targetMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        // AM/PM Combo
        targetAmPm.setItems(FXCollections.observableArrayList("AM", "PM"));
        targetAmPm.getSelectionModel().select("AM");

        // Category List
        updateCategoryList();

        // Initial visibility
        targetTimeContainer.setVisible(false);
        targetTimeContainer.setManaged(false);
    }

    private void setupListeners() {
        // Type change: Daily vs Custom
        typeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCustom = TYPE_CUSTOM.equals(newVal);
            targetTimeContainer.setVisible(isCustom);
            targetTimeContainer.setManaged(isCustom);
        });

        // Add To change (only visible when not in project context)
        todoForCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isProjectTask) {
                boolean isProject = FOR_PROJECT.equals(newVal);
                projectSelectionBox.setVisible(isProject);
                projectSelectionBox.setManaged(isProject);
                milestoneSelectionBox.setVisible(isProject);
                milestoneSelectionBox.setManaged(isProject);
                assignToSection.setVisible(isProject);
                assignToSection.setManaged(isProject);
                updateCategoryList();
            }
        });

        // Project selection change
        projectCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateProjectDetails(newVal);
                updateCategoryList();
            }
        });

        // Category creation
        categoryCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCreateNew = CREATE_NEW_CATEGORY.equals(newVal);
            newCategoryBox.setVisible(isCreateNew);
            newCategoryBox.setManaged(isCreateNew);
        });

        // Assignee selection
        assigneeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSpecific = assignSpecificRadio.isSelected();
            specificMembersBox.setVisible(isSpecific);
            specificMembersBox.setManaged(isSpecific);
        });
    }

    // ========== DATA POPULATION ==========

    private void updateCategoryList() {
        categoryCombo.getItems().clear();

        if (isProjectTask) {
            categoryCombo.getItems().addAll(TaskDataManager.getProjectCategories(existingProjectName));
        } else if (FOR_PERSONAL.equals(todoForCombo.getValue())) {
            categoryCombo.getItems().addAll(TaskDataManager.getProjectCategories(""));
        } else if (FOR_PROJECT.equals(todoForCombo.getValue()) && projectCombo.getValue() != null) {
            categoryCombo.getItems().addAll(TaskDataManager.getProjectCategories(projectCombo.getValue()));
        }

        // Always ensure only one "+ Create New Category"
        if (!categoryCombo.getItems().contains(CREATE_NEW_CATEGORY)) {
            categoryCombo.getItems().add(CREATE_NEW_CATEGORY);
        }

        // Default selection logic
        if (categoryCombo.getItems().size() == 1 && categoryCombo.getItems().contains(CREATE_NEW_CATEGORY)) {
            // Only "Create New Category" exists
            categoryCombo.getSelectionModel().select(CREATE_NEW_CATEGORY);
            newCategoryBox.setVisible(true);
            newCategoryBox.setManaged(true);
        } else {
            categoryCombo.getSelectionModel().selectFirst();
        }
    }

    private void populateProjectDetails(String projectName) {
        ProjectSummary project = summaries.stream()
                .filter(p -> p.getName().equals(projectName))
                .findFirst().orElse(null);

        if (project != null) {
            // Milestones
            milestoneCombo.getItems().clear();
            milestoneCombo.getItems().add("None");
            milestoneCombo.getItems().addAll(
                    project.getMilestones().stream()
                            .map(MilestoneSummary::getTitle)
                            .collect(Collectors.toList())
            );

            // Team Members
            populateMembers(project.getMembers());
        }
    }

    private void populateMilestoneDetails(List<MilestoneDetails>milestones) {
        // Milestones
        milestoneCombo.getItems().clear();
        milestoneCombo.getItems().add("None");
        milestoneCombo.getItems().addAll(
                milestones.stream()
                        .map(MilestoneDetails::getTitle)
                        .collect(Collectors.toList())
        );
    }

    private void populateMembers(List<MemberInfo> members) {
        membersCheckboxContainer.getChildren().clear();
        memberCheckboxes.clear();

        for (MemberInfo member : members) {
            CheckBox cb = createMemberCheckbox(member);
            memberCheckboxes.add(cb);
            membersCheckboxContainer.getChildren().add(cb);
        }

        updateSelectedCount();
    }

    private CheckBox createMemberCheckbox(MemberInfo member) {
        CheckBox checkbox = new CheckBox();
        checkbox.getStyleClass().add("member-checkbox");

        VBox labelBox = new VBox(2);
        Label nameLabel = new Label(member.getName());
        nameLabel.getStyleClass().add("member-name");
        Label emailLabel = new Label(member.getEmail());
        emailLabel.getStyleClass().add("member-email");
        labelBox.getChildren().addAll(nameLabel, emailLabel);

        checkbox.setGraphic(labelBox);
        checkbox.setUserData(member);
        checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> updateSelectedCount());

        return checkbox;
    }

    private void updateSelectedCount() {
        long count = memberCheckboxes.stream().filter(CheckBox::isSelected).count();
        selectedCountLabel.setText(count + " member" + (count != 1 ? "s" : "") + " selected");
    }

    // ========== HELPER METHODS ==========

    private LocalDateTime getDueDateTime() {
        String selectedType = typeCombo.getSelectionModel().getSelectedItem();

        if (TYPE_DAILY.equals(selectedType)) {
            return LocalDate.now().atTime(23, 59, 59);
        } else {
            if (dueDatePicker.getValue() == null) {
                return null;
            }
            int hour = targetHour.getValue();
            String ampm = targetAmPm.getValue();
            int hour24 = convertTo24Hour(hour, ampm);
            return dueDatePicker.getValue().atTime(hour24, targetMinute.getValue(), 0);
        }
    }

    private int convertTo24Hour(int hour, String ampm) {
        if ("PM".equals(ampm) && hour != 12) {
            return hour + 12;
        } else if ("AM".equals(ampm) && hour == 12) {
            return 0;
        }
        return hour;
    }

    private String getCategory() {
        String selected = categoryCombo.getValue();
        if (CREATE_NEW_CATEGORY.equals(selected)) {
            return newCategoryField.getText().trim().toLowerCase();
        }
        return selected;
    }

    private List<MemberInfo> getAssigneeEmails() {
        if (assignAllRadio.isSelected() || memberCheckboxes.isEmpty()) {
            return memberCheckboxes.stream()
                    .map(cb -> ((MemberInfo) cb.getUserData()))
                    .collect(Collectors.toList());
        }
        return memberCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> ((MemberInfo) cb.getUserData()))
                .collect(Collectors.toList());
    }

    // ========== VALIDATION ==========

    private boolean validateForm() {
        if (taskNameField.getText().trim().isEmpty()) {
            showAlert("Empty Task Name", "Please enter a task name.");
            return false;
        }

        // Check due date/time
        LocalDateTime dueDateTime = getDueDateTime();
        if (dueDateTime == null) {
            showAlert("Missing Due Date", "Please select a due date for the task.");
            return false;
        }

        // For project tasks, ensure assignees are selected
        if (isProjectTask && assignSpecificRadio.isSelected() && getAssigneeEmails().isEmpty()) {
            showAlert("No Assignees", "Please select at least one team member to assign this task.");
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
    private void addTask() {
        if (!validateForm()) return;

        String title = taskNameField.getText().trim();
        String description = descriptionField.getText();
        String category = getCategory();
        String dueDateTime = getDueDateTime().toString();
        int weight = (int) weightSlider.getValue();
        String priority = priorityCombo.getValue();
        String attachmentUrl = attachmentField.getText().trim();

        // Save new category if needed
        if (CREATE_NEW_CATEGORY.equals(categoryCombo.getValue()) && !newCategoryField.getText().trim().isEmpty()) {
            String projectContext = isProjectTask ? existingProjectName :
                    (FOR_PROJECT.equals(todoForCombo.getValue()) ? projectCombo.getValue() : "");
            TaskDataManager.saveCategory(projectContext, category);
        }

        if (isProjectTask) {
            if(existingMilestoneUuid==null){
                if (milestoneCombo.getValue() != null && !"None".equals(milestoneCombo.getValue())) {
                    MilestoneDetails milestone = milestoneDetails.stream()
                            .filter(m -> m.getTitle().equals(milestoneCombo.getValue()))
                            .findFirst().orElse(null);
                    if (milestone != null) {
                        existingMilestoneUuid = milestone.getUuid();
                        existingMilestoneName = milestone.getTitle();
                    }
                }
            }
            // Task for existing project
            TaskRequest request = new TaskRequest(
                    title, description, category, dueDateTime,
                    existingProjectUuid, existingMilestoneUuid,
                    typeCombo.getSelectionModel().getSelectedItem().equals(TYPE_DAILY),
                    weight, priority,
                    existingMilestoneName, existingProjectName,
                    LocalDataManager.getUserEmail(), getAssigneeEmails(), attachmentUrl
            );
            CreateRequestController.handleCreateTask(request, parentController);
        } else if (FOR_PERSONAL.equals(todoForCombo.getValue())) {
            // Personal task
            TaskDataManager.saveTask(title, description, category, dueDateTime, attachmentUrl, weight, priority, typeCombo.getSelectionModel().getSelectedItem().equals(TYPE_DAILY));
        } else {
            // Project task with project selection
            ProjectSummary project = summaries.stream()
                    .filter(p -> p.getName().equals(projectCombo.getValue()))
                    .findFirst().orElse(null);

            if (project != null) {
                MilestoneSummary milestone = null;
                System.out.println(milestoneCombo.getValue());
                String milestoneUuid = null;
                String milestoneName = null;

                if (milestoneCombo.getValue() != null && !"None".equals(milestoneCombo.getValue())) {
                    milestone = project.getMilestones().stream()
                            .filter(m -> m.getTitle().equals(milestoneCombo.getValue()))
                            .findFirst().orElse(null);
                    if (milestone != null) {
                        System.out.println(milestone.getUuid());
                        milestoneUuid = milestone.getUuid();
                        milestoneName = milestone.getTitle();
                    }else{
                        System.out.println("can't");
                    }
                }

                if (project.getUuid().trim().isEmpty()) {
                    // Personal project
                    ProjectDataManager.savePersonalProjectTask(
                            title, description, category, dueDateTime,
                            typeCombo.getSelectionModel().getSelectedItem().equals(TYPE_DAILY), weight, priority,
                            project.getName(), milestoneName, attachmentUrl
                    );
                } else {
                    // Backend project
                    TaskRequest request = new TaskRequest(
                            title, description, category, dueDateTime,
                            project.getUuid(), milestoneUuid, typeCombo.getSelectionModel().getSelectedItem().equals(TYPE_DAILY),
                            weight, priority,
                            milestoneName, project.getName(),
                            LocalDataManager.getUserEmail(), getAssigneeEmails(), attachmentUrl
                    );
                    CreateRequestController.handleCreateTask(request, parentController);
                }
            }
        }

        closeWindow();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}