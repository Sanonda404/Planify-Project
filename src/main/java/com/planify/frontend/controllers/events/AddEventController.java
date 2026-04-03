package com.planify.frontend.controllers.events;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.events.EventCreateRequest;
import com.planify.frontend.models.group.GroupSummaryRequest;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.data.group.GroupDataManager;
import com.planify.frontend.utils.data.personal.EventDataManager;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.collections.FXCollections;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class AddEventController implements Initializable {
    // Header
    @FXML private Button closeButton;

    // Basic Info
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ColorPicker colorPicker;
    @FXML private Label colorLabel;

    // Slot Timing
    @FXML private VBox slotTimingBox;
    @FXML private DatePicker slotStartDate;
    @FXML private Spinner<Integer> slotStartHour, slotStartMinute;
    @FXML private ComboBox<String> slotDurationUnit;
    @FXML private TextField slotDurationValue;

    @FXML private ComboBox<String> slotStartAmPm;
    @FXML private ComboBox<String> spanStartAmPm;
    @FXML private ComboBox<String> spanEndAmPm;
    @FXML private ComboBox<String> targetAmPm;
    @FXML private ComboBox<String> monthlyAmPm;


    // Span Timing
    @FXML private VBox spanTimingBox;
    @FXML private DatePicker spanStartDate, spanEndDate;
    @FXML private Spinner<Integer> spanStartHour, spanStartMinute, spanEndHour, spanEndMinute;

    // Target Timing (Deadline/Marker)
    @FXML private VBox targetTimingBox;
    @FXML private DatePicker targetDate;
    @FXML private Spinner<Integer> targetHour, targetMinute, monthlyHour, monthlyMinute;

    // Add To
    @FXML private VBox groupSelectionBox, mergeCheckboxBox, dailyRepeatBox, monthlyRepeatBox, addToContainer;
    @FXML private CheckBox mergeWithPersonalCheckbox,
            mondayCheckbox, tuesdayCheckbox, wednesdayCheckbox,
            thursdayCheckbox, fridayCheckbox, saturdayCheckbox, sundayCheckbox;
    @FXML private DatePicker monthlyDatePicker;

    // Reminder
    @FXML private ToggleGroup reminderToggleGroup;
    @FXML private RadioButton reminderAtStartRadio, reminderCustomRadio;
    @FXML private HBox customReminderBox;
    @FXML private TextField reminderValue, attachmentField, titleField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> reminderUnit, repeatComboBox, addToComboBox, reminderTypeComboBox;
    @FXML private ComboBox<MemberInfo> groupComboBox;

    // ========== DATA ==========

    private Map<String, Color> defaultColors;

    private LocalDateTime startDateTime, endDateTime, monthlyDate;
    List<GroupSummaryRequest>summaries;

    // TODO: Replace with actual backend data
    private final List<MemberInfo> userGroupsWithPostingAccess = new ArrayList<>();

    private Object parent;
    private String grpUuid = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeDefaultColors();
        initializeTypeComboBox();
        initializeReminderTypeComboBox();
        initializeTimeSpinners();
        initializeDurationUnits();
        initializeAddToComboBox();
        initializeRepeatComboBox();
        initializeReminderUnits();
        setupListeners();
        initializeAmPmCombos();
    }

    // ========== INITIALIZATION ==========
    public void setParent(Object parent){
        this.parent = parent;
    }

    public void setGroupUuid(String grpUuid){
        this.grpUuid = grpUuid;
        if (grpUuid != null && !grpUuid.trim().isEmpty()) {
            addToContainer.setManaged(false);
            addToContainer.setVisible(false);
            addToComboBox.getSelectionModel().selectLast();
            groupSelectionBox.setVisible(false);
            groupSelectionBox.setManaged(false);
        }

    }

    private void initializeAmPmCombos() {
        ObservableList<String> amPmOptions = FXCollections.observableArrayList("AM", "PM");

        // Initialize all AM/PM combo boxes
        slotStartAmPm.setItems(amPmOptions);
        spanStartAmPm.setItems(amPmOptions);
        spanEndAmPm.setItems(amPmOptions);
        targetAmPm.setItems(amPmOptions);
        monthlyAmPm.setItems(amPmOptions);

        // Set default selections
        slotStartAmPm.getSelectionModel().select("AM");
        spanStartAmPm.getSelectionModel().select("AM");
        spanEndAmPm.getSelectionModel().select("AM");
        targetAmPm.getSelectionModel().select("AM");
        monthlyAmPm.getSelectionModel().select("AM");

        // Add listeners to ensure values are displayed properly
        addAmPmDisplayListener(slotStartAmPm);
        addAmPmDisplayListener(spanStartAmPm);
        addAmPmDisplayListener(spanEndAmPm);
        addAmPmDisplayListener(targetAmPm);
        addAmPmDisplayListener(monthlyAmPm);
    }

    private void addAmPmDisplayListener(ComboBox<String> comboBox) {
        // Custom cell factory for dropdown list
        comboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: 500;");
                }
            }
        });

        // Custom button cell for showing selected value
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #685AFF;");
                }
            }
        });
    }
    private void initializeDefaultColors() {
        defaultColors = new HashMap<>();
        defaultColors.put("Slot", Color.web("#3c98fa"));       // Blue
        defaultColors.put("Span", Color.web("#00ba28"));       // Green
        defaultColors.put("Deadline", Color.web("#ba0034"));   // Red
        defaultColors.put("Marker", Color.web("#f59e0b"));     // Yellow
    }

    private void initializeTypeComboBox() {
        typeComboBox.setItems(FXCollections.observableArrayList(
                "Slot", "Span", "Deadline", "Marker"
        ));
        typeComboBox.getSelectionModel().selectFirst();
        updateTimingBoxVisibility("Slot");
        updateColorPicker("Slot");
    }

    private void initializeReminderTypeComboBox(){
        reminderTypeComboBox.setItems(FXCollections.observableArrayList(
                "None", "Alert", "Notification"
        ));
        reminderTypeComboBox.getSelectionModel().selectFirst();
    }

    private void initializeTimeSpinners() {
        // Hour spinners (1-12 for AM/PM format)
        setupTimeSpinner(slotStartHour, 1, 12, 9);
        setupTimeSpinner(spanStartHour, 1, 12, 9);
        setupTimeSpinner(spanEndHour, 1, 12, 17);
        setupTimeSpinner(targetHour, 1, 12, 12);
        setupTimeSpinner(monthlyHour, 1, 12, 12);

        // Minute spinners (0-59)
        setupTimeSpinner(slotStartMinute, 0, 59, 0);
        setupTimeSpinner(spanStartMinute, 0, 59, 0);
        setupTimeSpinner(spanEndMinute, 0, 59, 0);
        setupTimeSpinner(targetMinute, 0, 59, 0);
        setupTimeSpinner(monthlyMinute, 0, 59, 0);
    }

    private void setupTimeSpinner(Spinner<Integer> spinner, int min, int max, int initial) {
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial);
        valueFactory.setWrapAround(true);
        spinner.setValueFactory(valueFactory);

        // Format with leading zeros for minutes
        if (min == 0 && max == 59) {
            spinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    spinner.getEditor().setText(oldVal);
                } else if (newVal.length() == 1 && !newVal.isEmpty()) {
                    spinner.getEditor().setText("0" + newVal);
                }
            });
        }

        spinner.setEditable(true);
    }

    private void initializeDurationUnits() {
        slotDurationUnit.setItems(FXCollections.observableArrayList(
                "minutes", "hours"));
        slotDurationUnit.getSelectionModel().selectFirst();
        slotDurationValue.setText("60");
    }

    private void initializeAddToComboBox() {
        addToComboBox.setItems(FXCollections.observableArrayList(
                "Personal", "Group"
        ));
        addToComboBox.getSelectionModel().selectFirst();
        if(!grpUuid.trim().isEmpty()){
            addToContainer.setManaged(false);
            addToContainer.setVisible(false);
            addToComboBox.getSelectionModel().selectLast();
        }
    }

    private void initializeRepeatComboBox() {
        repeatComboBox.setItems(FXCollections.observableArrayList(
                "No repeat", "Daily", "Weekly", "Monthly"
        ));
        repeatComboBox.getSelectionModel().selectFirst();
    }

    private void initializeReminderUnits() {
        reminderUnit.setItems(FXCollections.observableArrayList(
                "minutes", "hours", "days"
        ));
        reminderUnit.getSelectionModel().selectFirst();
        reminderValue.setText("10");
    }

    // ========== LISTENERS ==========

    private void setupListeners() {
        // Type change listener
        typeComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        updateTimingBoxVisibility(newVal);
                        updateColorPicker(newVal);
                    }
                }
        );

        // Add To change listener
        addToComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    boolean isGroup = "Group".equals(newVal);
                    groupSelectionBox.setVisible(isGroup);
                    groupSelectionBox.setManaged(isGroup);
                    mergeCheckboxBox.setVisible(isGroup);
                    mergeCheckboxBox.setManaged(isGroup);

                    if (isGroup) {
                        loadGroupsWithPostingAccess();
                    }
                }
        );

        // Repeat change listener
        repeatComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    dailyRepeatBox.setVisible("Daily".equals(newVal));
                    dailyRepeatBox.setManaged("Daily".equals(newVal));
                    monthlyRepeatBox.setVisible("Monthly".equals(newVal));
                    monthlyRepeatBox.setManaged("Monthly".equals(newVal));
                }
        );

        // Reminder toggle listener
        reminderToggleGroup.selectedToggleProperty().addListener(
                (obs, oldVal, newVal) -> {
                    boolean isCustom = reminderCustomRadio.isSelected();
                    customReminderBox.setDisable(!isCustom);
                }
        );

    }

    // ========== HELPER METHODS ==========

    private void updateTimingBoxVisibility(String eventType) {
        // Hide all timing boxes
        slotTimingBox.setVisible(false);
        slotTimingBox.setManaged(false);
        spanTimingBox.setVisible(false);
        spanTimingBox.setManaged(false);
        targetTimingBox.setVisible(false);
        targetTimingBox.setManaged(false);

        // Show appropriate timing box
        switch (eventType) {
            case "Slot":
                slotTimingBox.setVisible(true);
                slotTimingBox.setManaged(true);
                break;
            case "Span":
                spanTimingBox.setVisible(true);
                spanTimingBox.setManaged(true);
                break;
            case "Deadline":
            case "Marker":
                targetTimingBox.setVisible(true);
                targetTimingBox.setManaged(true);
                break;
        }
    }

    private void updateColorPicker(String eventType) {
        Color defaultColor = defaultColors.getOrDefault(eventType, Color.web("#3c98fa"));
        colorPicker.setValue(defaultColor);

        String colorName = switch (eventType) {
            case "Slot" -> "Blue";
            case "Span" -> "Green";
            case "Deadline" -> "Red";
            case "Marker" -> "Yellow";
            default -> "Blue";
        };

        colorLabel.setText("Default: " + colorName);
    }

    // ========== BACKEND INTEGRATION (TODO) ==========

    /**
     * TODO: Backend Integration - Load groups where user has posting access
     * Replace demo data with actual API call
     */
    private void loadGroupsWithPostingAccess() {
        // TODO: Call backend API to fetch groups
        if(!grpUuid.isEmpty())return;
        summaries = GroupDataManager.getGroupSummary();
        if (summaries.isEmpty()) {
            Label noGroupsLabel = new Label("No groups found. Create or join a group.");
            noGroupsLabel.getStyleClass().add("empty-message");
        }else{
            for (GroupSummaryRequest summary : summaries) {
                userGroupsWithPostingAccess.add(new MemberInfo(summary.getName(),summary.getUuid()));
            }

            groupComboBox.setItems(FXCollections.observableArrayList(userGroupsWithPostingAccess));

            if (!userGroupsWithPostingAccess.isEmpty()) {
                groupComboBox.getSelectionModel().selectFirst();
            }
        }

    }

    /**
     * TODO: Backend Integration - Create event and save to database
     */
    private void saveEventToBackend(EventCreateRequest eventData) {
        // TODO: Call backend API to save event

        System.out.println("TODO: Save event to backend");
        System.out.println("Event Data: " + eventData);
        CreateRequestController.handleCreateEvent(eventData,parent);
    }

    // ========== EVENT HANDLERS ==========

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleCreateEvent() {
        // Validate form
        if (!validateForm()) {
            return;
        }
        gatherEventData();
        // Close modal
        handleClose();
    }

    // ========== VALIDATION ==========

    private boolean validateForm() {
        List<String> errors = new ArrayList<>();

        // Validate title
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            errors.add("Event title is required");
        }

        // Validate type
        if (typeComboBox.getSelectionModel().getSelectedItem() == null) {
            errors.add("Event type is required");
        }

        // Validate timing based on type
        String type = typeComboBox.getSelectionModel().getSelectedItem();
        if (type != null) {
            switch (type) {
                case "Slot":
                    if (slotStartDate.getValue() == null) {
                        errors.add("Start date is required for Slot events");
                    }
                    if (slotDurationValue.getText() == null || slotDurationValue.getText().trim().isEmpty()) {
                        errors.add("Duration is required for Slot events");
                    }
                    break;
                case "Span":
                    if (spanStartDate.getValue() == null) {
                        errors.add("Start date is required for Span events");
                    }
                    if (spanEndDate.getValue() == null) {
                        errors.add("End date is required for Span events");
                    }
                    break;
                case "Deadline":
                case "Marker":
                    if (targetDate.getValue() == null) {
                        errors.add("Target date is required");
                    }
                    break;
            }
        }

        if(grpUuid==null || grpUuid.trim().isEmpty()){
            // Validate Add To
            if (addToComboBox.getSelectionModel().getSelectedItem() == null) {
                errors.add("Please select where to add the event");
            }

            // If Group is selected, validate group selection
            if ("Group".equals(addToComboBox.getSelectionModel().getSelectedItem())) {
                if (groupComboBox.getSelectionModel().getSelectedItem() == null && !grpUuid.isEmpty()) {
                    errors.add("Please select a group");
                }
            }
        }


        // Show errors if any
        if (!errors.isEmpty()) {
            showErrorAlert(errors);
            return false;
        }

        return true;
    }

    // ========== DATA GATHERING ==========

    private void gatherEventData() {

        // Basic info
        String type = typeComboBox.getSelectionModel().getSelectedItem();
        String color = colorToHex(colorPicker.getValue());
        String reminderType = reminderTypeComboBox.getSelectionModel().getSelectedItem();

        // Timing based on type
        switch (type) {
            case "Slot":
                startDateTime = getDateTime(slotStartDate, slotStartHour, slotStartMinute, slotStartAmPm);
                Duration duration = parseDuration(slotDurationValue.getText(), slotDurationUnit.getValue());
                endDateTime = startDateTime.plus(duration);
                break;
            case "Span":
                startDateTime = getDateTime(spanStartDate, spanStartHour, spanStartMinute, spanStartAmPm);
                endDateTime = getDateTime(spanEndDate, spanEndHour, spanEndMinute, spanEndAmPm);
                break;
            case "Deadline":
            case "Marker":
                startDateTime = getDateTime(targetDate, targetHour, targetMinute, targetAmPm);
                break;
        }

        String repeatPattern = repeatComboBox.getSelectionModel().getSelectedItem();
        if(repeatPattern.equals("No repeat"))repeatPattern = "NO_REPEAT";
        List<String>excludedDays = new ArrayList<>();
        if ("Daily".equals(repeatPattern)) {
            excludedDays = getExcludedDays();
        } else if ("Monthly".equals(repeatPattern)) {
            if (monthlyDatePicker != null && monthlyDatePicker.getValue() != null) {
                monthlyDate = getDateTime(monthlyDatePicker, monthlyHour, monthlyMinute, monthlyAmPm);
            }
        }

        // Reminder
        int reminderMinutesBefore;
        if (reminderAtStartRadio.isSelected()) {
            reminderMinutesBefore = 0;
        } else {
            reminderMinutesBefore = parseReminderToMinutes(
                    reminderValue.getText(),
                    reminderUnit.getValue()
            );
        }

        // Attachment
        String attachmentUrl = attachmentField.getText().trim();

        String start = (startDateTime==null?"":startDateTime.toString());
        String end = (endDateTime==null?"":endDateTime.toString());
        String monthly = (monthlyDate==null?"":monthlyDate.toString());

        System.out.println(start+" "+end);

        EventCreateRequest data = new EventCreateRequest(
                titleField.getText().trim(),
                descriptionArea.getText().trim(),
                type,
                color,
                start,
                end,
                "890",
                mergeWithPersonalCheckbox.isSelected(),
                repeatPattern.toUpperCase(),
                excludedDays,
                monthly,
                reminderMinutesBefore,
                reminderType,
                attachmentUrl,
                LocalDataManager.getUserEmail()
                );

        // Add To
        String addTo = addToComboBox.getSelectionModel().getSelectedItem();
        if ("Group".equals(addTo)) {
            if(!grpUuid.isEmpty()){
                data.setGroupUuid(grpUuid);
                saveEventToBackend(data);
            }else{
                MemberInfo selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();
                if (selectedGroup != null) {
                    data.setGroupUuid(selectedGroup.getEmail()); // use the actual UUID field
                    saveEventToBackend(data);
                }
            }
        }
        else{
            saveEventLocally(data);
        }
    }

    private void saveEventLocally(EventCreateRequest event){
        //Todo: Save the data locally
        EventDataManager.addEvent(event, parent);
    }

    // ========== UTILITY METHODS ==========

    // Helper method to convert 12-hour time to 24-hour
    private int convertTo24Hour(int hour, String amPm) {
        if (hour == 12) {
            return "AM".equals(amPm) ? 0 : 12;
        }
        return "PM".equals(amPm) ? hour + 12 : hour;
    }

    // Updated getDateTime method with AM/PM support
    private LocalDateTime getDateTime(DatePicker datePicker, Spinner<Integer> hourSpinner,
                                      Spinner<Integer> minuteSpinner, ComboBox<String> amPmCombo) {
        if (datePicker.getValue() == null) {
            return null;
        }

        // Get the AM/PM value, default to "AM" if null
        String amPm = amPmCombo.getValue();
        if (amPm == null) {
            amPm = "AM";
            amPmCombo.getSelectionModel().select("AM");
        }

        int hour12 = hourSpinner.getValue();
        int hour24 = convertTo24Hour(hour12, amPm);

        return datePicker.getValue().atTime(hour24, minuteSpinner.getValue(), 0);
    }
    private Duration parseDuration(String value, String unit) {
        try {
            long numValue = Long.parseLong(value);

            switch (unit.toLowerCase()) {
                case "hours":
                    return Duration.ofHours(numValue);
                case "minutes":
                    return Duration.ofMinutes(numValue);
                case "seconds":
                    return Duration.ofSeconds(numValue);
                case "days":
                    return Duration.ofDays(numValue);
                default:
                    // Default to minutes if unit is unknown
                    return Duration.ofMinutes(numValue);
            }
        } catch (NumberFormatException e) {
            // Default to 60 minutes if parsing fails
            return Duration.ofMinutes(60);
        }
    }

    private int parseReminderToMinutes(String value, String unit) {
        try {
            int numValue = Integer.parseInt(value);
            return switch (unit) {
                case "hours" -> numValue * 60;
                case "days" -> numValue * 60 * 24;
                default -> numValue; // minutes
            };
        } catch (NumberFormatException e) {
            return 10; // Default 10 minutes
        }
    }

    private List<String> getExcludedDays() {
        List<String> excluded = new ArrayList<>();
        if (!mondayCheckbox.isSelected()) excluded.add("Monday");
        if (!tuesdayCheckbox.isSelected()) excluded.add("Tuesday");
        if (!wednesdayCheckbox.isSelected()) excluded.add("Wednesday");
        if (!thursdayCheckbox.isSelected()) excluded.add("Thursday");
        if (!fridayCheckbox.isSelected()) excluded.add("Friday");
        if (!saturdayCheckbox.isSelected()) excluded.add("Saturday");
        if (!sundayCheckbox.isSelected()) excluded.add("Sunday");
        return excluded;
    }

    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }

    // ========== ALERTS ==========

    private void showErrorAlert(List<String> errors) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText("Please fix the following errors:");
        alert.setContentText(String.join("\n• ", errors));
        alert.showAndWait();
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("Event created successfully!");
        alert.showAndWait();
    }

}