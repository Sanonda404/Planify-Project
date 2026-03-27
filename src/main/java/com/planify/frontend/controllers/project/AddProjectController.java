package com.planify.frontend.controllers.project;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.group.GroupSummaryRequest;
import com.planify.frontend.models.project.ProjectCreateRequest;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.data.group.GroupDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import com.planify.frontend.utils.helpers.AlertCreator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AddProjectController {

    @FXML private TextField taskNameField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> projectTypeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker deadlinePicker;
    @FXML private ComboBox<String> groupSelectCombo;

    // Time Spinners
    @FXML private Spinner<Integer> startHour, startMinute;
    @FXML private Spinner<Integer> deadlineHour, deadlineMinute;

    // AM/PM Combo Boxes
    @FXML private ComboBox<String> startAmPm;
    @FXML private ComboBox<String> deadlineAmPm;

    @FXML private VBox projectTypeContainer;
    @FXML private VBox groupSelectionContainer;

    private List<GroupSummaryRequest> groups = new ArrayList<>();
    private Object parent;
    private String grdUuid = "";

    @FXML
    private void initialize() {
        fetchGroupsFromBackend();

        // Initialize time spinners (1-12 hour format)
        initializeTimeSpinners();

        // Initialize AM/PM combo boxes
        initializeAmPmCombos();

        // Setup project type combo
        projectTypeCombo.getItems().addAll("Personal", "Group");
        projectTypeCombo.getSelectionModel().selectFirst();

        // Set default dates
        startDatePicker.setValue(LocalDate.now());
        deadlinePicker.setValue(LocalDate.now().plusMonths(1));

        // Set default times
        startHour.getValueFactory().setValue(9);
        startMinute.getValueFactory().setValue(0);
        startAmPm.getSelectionModel().select("AM");

        deadlineHour.getValueFactory().setValue(5);
        deadlineMinute.getValueFactory().setValue(0);
        deadlineAmPm.getSelectionModel().select("PM");

        // Add listener for project type change
        projectTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Group".equalsIgnoreCase(newVal)) {
                loadGroups();
                groupSelectionContainer.setVisible(true);
                groupSelectionContainer.setManaged(true);
            } else {
                groupSelectCombo.getItems().clear();
                groupSelectionContainer.setVisible(false);
                groupSelectionContainer.setManaged(false);
            }
        });

        // If group context is provided, hide project type selection
        if (!grdUuid.isEmpty()) {
            projectTypeCombo.getSelectionModel().select("Group");
            projectTypeContainer.setVisible(false);
            projectTypeContainer.setManaged(false);
            groupSelectionContainer.setVisible(false);
            groupSelectionContainer.setManaged(false);
        }
    }

    private void initializeTimeSpinners() {
        // Hour spinners (1-12 for AM/PM format)
        startHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 9));
        startMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        deadlineHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 5));
        deadlineMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        // Format with leading zeros for minutes
        startMinute.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() == 1 && !newVal.isEmpty()) {
                startMinute.getEditor().setText("0" + newVal);
            }
        });

        deadlineMinute.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() == 1 && !newVal.isEmpty()) {
                deadlineMinute.getEditor().setText("0" + newVal);
            }
        });
    }

    private void initializeAmPmCombos() {
        List<String> amPmOptions = List.of("AM", "PM");
        startAmPm.setItems(javafx.collections.FXCollections.observableArrayList(amPmOptions));
        deadlineAmPm.setItems(javafx.collections.FXCollections.observableArrayList(amPmOptions));
        startAmPm.getSelectionModel().select("AM");
        deadlineAmPm.getSelectionModel().select("PM");
    }

    public void setParent(Object parent) {
        this.parent = parent;
    }

    private void loadGroups() {
        if (groups.isEmpty()) {
            showNoGroupFoundMessage();
            groupSelectCombo.setVisible(false);
        } else {
            groupSelectCombo.getItems().clear();
            for (GroupSummaryRequest grp : groups) {
                groupSelectCombo.getItems().add(grp.getName());
            }
            groupSelectCombo.setVisible(true);
            groupSelectCombo.getSelectionModel().selectFirst();
        }
    }

    private void fetchGroupsFromBackend() {
        try {
            groups = GroupDataManager.getGroupSummary();
            if (groups == null) {
                groups = new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Error fetching groups: " + e.getMessage());
            groups = new ArrayList<>();
        }
    }

    private int convertTo24Hour(int hour12, String amPm) {
        if ("PM".equals(amPm) && hour12 != 12) {
            return hour12 + 12;
        } else if ("AM".equals(amPm) && hour12 == 12) {
            return 0;
        }
        return hour12;
    }

    @FXML
    private void addProject() {
        String name = taskNameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Validation Error", "Please enter a project name.");
            return;
        }

        String description = descriptionArea.getText();

        LocalDate startDate = startDatePicker.getValue();
        LocalDate deadlineDate = deadlinePicker.getValue();

        if (startDate == null) {
            showAlert("Validation Error", "Please select a start date.");
            return;
        }

        if (deadlineDate == null) {
            showAlert("Validation Error", "Please select a deadline date.");
            return;
        }

        if (deadlineDate.isBefore(startDate)) {
            showAlert("Validation Error", "Deadline cannot be before start date.");
            return;
        }

        // Convert 12-hour format to 24-hour format
        int startHour24 = convertTo24Hour(startHour.getValue(), startAmPm.getValue());
        int deadlineHour24 = convertTo24Hour(deadlineHour.getValue(), deadlineAmPm.getValue());

        LocalTime startTime = LocalTime.of(startHour24, startMinute.getValue());
        LocalTime deadlineTime = LocalTime.of(deadlineHour24, deadlineMinute.getValue());

        LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
        LocalDateTime deadlineDateTime = LocalDateTime.of(deadlineDate, deadlineTime);

        String type = projectTypeCombo.getValue();

        if ("Group".equalsIgnoreCase(type)) {
            String selectedGroupName = groupSelectCombo.getValue();

            // If group context is already set (from group details page)
            if (grdUuid.isEmpty() && selectedGroupName == null) {
                showAlert("Validation Error", "Please select a group.");
                return;
            }

            String groupUuid = grdUuid;

            // If no pre-set group, find by name
            if (groupUuid.isEmpty() && selectedGroupName != null) {
                GroupSummaryRequest selectedPair = groups.stream()
                        .filter(p -> p.getName().equals(selectedGroupName))
                        .findFirst()
                        .orElse(null);

                if (selectedPair == null) {
                    showAlert("Error", "Selected group not found.");
                    return;
                }
                groupUuid = selectedPair.getUuid();
            }

            ProjectCreateRequest request = new ProjectCreateRequest(
                    name, description,
                    startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    deadlineDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    groupUuid, LocalDataManager.getUserEmail()
            );
            saveToBackend(request);
        } else {
            // Personal project
            saveLocally(name, description,
                    startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    deadlineDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        closeWindow();
    }

    private void saveToBackend(ProjectCreateRequest request) {
        CreateRequestController.handleCreateProject(request, parent);
    }

    private void saveLocally(String name, String description, String start, String deadline) {
        ProjectDataManager.savePersonalProject(name, description, start, deadline);
        if (parent instanceof ProjectController) {
            ((ProjectController) parent).refresh();
            AlertCreator.showSuccessAlert("Project Created Successfully!!");
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) taskNameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showNoGroupFoundMessage() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("No Groups Found");
        alert.setHeaderText(null);
        alert.setContentText("No group found. Please create or join a group.");

        ButtonType createBtn = new ButtonType("Create/Join Group", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(createBtn, ButtonType.CANCEL);

        alert.showAndWait().ifPresent(response -> {
            if (response == createBtn) {
                SceneManager.switchScene("add-group-view.fxml", "Add Group");
            }
        });
    }

    public void setGroupContext(String groupUuid, String groupName) {
        this.grdUuid = groupUuid;

        // Hide project type and group selection since group is already known
        projectTypeContainer.setVisible(false);
        projectTypeContainer.setManaged(false);
        groupSelectionContainer.setVisible(false);
        groupSelectionContainer.setManaged(false);

        // Set the type to Group
        projectTypeCombo.getSelectionModel().select("Group");
    }

    public void setGroupContext(String groupUuid) {
        setGroupContext(groupUuid, null);
    }
}