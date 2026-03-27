package com.planify.frontend.controllers.events;

import com.planify.frontend.controllers.Request.EditRequestController;
import com.planify.frontend.models.events.EventCreateRequest;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.utils.data.personal.EventDataManager;
import com.planify.frontend.utils.helpers.AlertCreator;
import com.planify.frontend.utils.managers.LocalDataManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EditEventController implements Initializable {

    // ========== FXML COMPONENTS ==========
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ColorPicker colorPicker;
    @FXML private Label colorLabel;

    // Slot Timing
    @FXML private VBox slotTimingBox;
    @FXML private DatePicker slotStartDate;
    @FXML private Spinner<Integer> slotStartHour, slotStartMinute;
    @FXML private ComboBox<String> slotStartAmPm;
    @FXML private TextField slotDurationValue;
    @FXML private ComboBox<String> slotDurationUnit;

    // Span Timing
    @FXML private VBox spanTimingBox;
    @FXML private DatePicker spanStartDate, spanEndDate;
    @FXML private Spinner<Integer> spanStartHour, spanStartMinute;
    @FXML private Spinner<Integer> spanEndHour, spanEndMinute;
    @FXML private ComboBox<String> spanStartAmPm, spanEndAmPm;

    // Target Timing
    @FXML private VBox targetTimingBox;
    @FXML private DatePicker targetDate;
    @FXML private Spinner<Integer> targetHour, targetMinute;
    @FXML private ComboBox<String> targetAmPm;

    // Repeat
    @FXML private ComboBox<String> repeatComboBox;
    @FXML private VBox dailyRepeatBox;
    @FXML private CheckBox mondayCheckbox, tuesdayCheckbox, wednesdayCheckbox,
            thursdayCheckbox, fridayCheckbox, saturdayCheckbox, sundayCheckbox;
    @FXML private VBox monthlyRepeatBox;
    @FXML private DatePicker monthlyDatePicker;
    @FXML private Spinner<Integer> monthlyHour, monthlyMinute;
    @FXML private ComboBox<String> monthlyAmPm;

    // Reminder
    @FXML private ToggleGroup reminderToggleGroup;
    @FXML private RadioButton reminderAtStartRadio, reminderCustomRadio;
    @FXML private HBox customReminderBox;
    @FXML private TextField reminderValue;
    @FXML private ComboBox<String> reminderUnit, reminderTypeComboBox;

    @FXML private TextField attachmentField;
    @FXML private Button closeButton;

    private EventGetRequest originalEvent;
    private Runnable onRefreshCallback;

    private final Map<String, Color> defaultColors = new HashMap<>();
    private final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeDefaultColors();
        initializeTypeComboBox();
        initializeTimeSpinners();
        initializeDurationUnits();
        initializeRepeatComboBox();
        initializeReminderUnits();
        setupListeners();
        initializeAmPmCombos();
    }

    private void initializeDefaultColors() {
        defaultColors.put("Slot", Color.web("#3c98fa"));
        defaultColors.put("Span", Color.web("#00ba28"));
        defaultColors.put("Deadline", Color.web("#ba0034"));
        defaultColors.put("Marker", Color.web("#f59e0b"));
    }

    private void initializeTypeComboBox() {
        typeComboBox.setItems(FXCollections.observableArrayList("Slot", "Span", "Deadline", "Marker"));
        typeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateTimingBoxVisibility(newVal);
                updateColorPicker(newVal);
            }
        });
    }

    private void initializeTimeSpinners() {
        setupTimeSpinner(slotStartHour, 1, 12, 9);
        setupTimeSpinner(slotStartMinute, 0, 59, 0);
        setupTimeSpinner(spanStartHour, 1, 12, 9);
        setupTimeSpinner(spanStartMinute, 0, 59, 0);
        setupTimeSpinner(spanEndHour, 1, 12, 17);
        setupTimeSpinner(spanEndMinute, 0, 59, 0);
        setupTimeSpinner(targetHour, 1, 12, 12);
        setupTimeSpinner(targetMinute, 0, 59, 0);
        setupTimeSpinner(monthlyHour, 1, 12, 12);
        setupTimeSpinner(monthlyMinute, 0, 59, 0);
    }

    private void setupTimeSpinner(Spinner<Integer> spinner, int min, int max, int initial) {
        SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial);
        factory.setWrapAround(true);
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
    }

    private void initializeDurationUnits() {
        slotDurationUnit.setItems(FXCollections.observableArrayList("minutes", "hours", "days"));
        slotDurationUnit.getSelectionModel().select("hours");
        slotDurationValue.setText("1");
    }

    private void initializeRepeatComboBox() {
        repeatComboBox.setItems(FXCollections.observableArrayList("No repeat", "Daily", "Weekly", "Monthly"));
        repeatComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            dailyRepeatBox.setVisible("Daily".equals(newVal));
            dailyRepeatBox.setManaged("Daily".equals(newVal));
            monthlyRepeatBox.setVisible("Monthly".equals(newVal));
            monthlyRepeatBox.setManaged("Monthly".equals(newVal));
        });
    }

    private void initializeReminderUnits() {
        reminderUnit.setItems(FXCollections.observableArrayList("minutes", "hours", "days"));
        reminderUnit.getSelectionModel().select("minutes");
        reminderValue.setText("10");
    }

    private void initializeAmPmCombos() {
        List<String> amPmOptions = Arrays.asList("AM", "PM");
        slotStartAmPm.setItems(FXCollections.observableArrayList(amPmOptions));
        spanStartAmPm.setItems(FXCollections.observableArrayList(amPmOptions));
        spanEndAmPm.setItems(FXCollections.observableArrayList(amPmOptions));
        targetAmPm.setItems(FXCollections.observableArrayList(amPmOptions));
        monthlyAmPm.setItems(FXCollections.observableArrayList(amPmOptions));

        slotStartAmPm.getSelectionModel().select("AM");
        spanStartAmPm.getSelectionModel().select("AM");
        spanEndAmPm.getSelectionModel().select("AM");
        targetAmPm.getSelectionModel().select("AM");
        monthlyAmPm.getSelectionModel().select("AM");
    }

    private void setupListeners() {
        reminderToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            customReminderBox.setDisable(!reminderCustomRadio.isSelected());
        });
    }

    private void updateTimingBoxVisibility(String eventType) {
        slotTimingBox.setVisible(false);
        slotTimingBox.setManaged(false);
        spanTimingBox.setVisible(false);
        spanTimingBox.setManaged(false);
        targetTimingBox.setVisible(false);
        targetTimingBox.setManaged(false);

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
        colorLabel.setText("Default: " + eventType);
    }

    public void setEvent(EventGetRequest event) {
        this.originalEvent = event;
        populateForm();
    }

    public void setParentCallback(Runnable callback) {
        this.onRefreshCallback = callback;
    }

    private void populateForm() {
        // Basic Info
        titleField.setText(originalEvent.getTitle());
        descriptionArea.setText(originalEvent.getDescription());

        String type = originalEvent.getType() != null ? originalEvent.getType() : "Slot";
        typeComboBox.setValue(type);

        if (originalEvent.getColor() != null && !originalEvent.getColor().isEmpty()) {
            colorPicker.setValue(Color.web(originalEvent.getColor()));
        }

        // Timing based on type
        LocalDateTime start = LocalDateTime.parse(originalEvent.getStartDateTime());

        switch (type) {
            case "Slot":
                populateSlotTiming(start);
                break;
            case "Span":
                populateSpanTiming(start);
                break;
            case "Deadline":
            case "Marker":
                populateTargetTiming(start);
                break;
        }

        // Repeat Info
        if (originalEvent.getRepeatPattern() != null && !"NO_REPEAT".equalsIgnoreCase(originalEvent.getRepeatPattern())) {
            String pattern = originalEvent.getRepeatPattern();
            repeatComboBox.setValue(pattern.equalsIgnoreCase("NO_REPEAT") ? "No repeat" : pattern);

            if ("Daily".equalsIgnoreCase(pattern) && originalEvent.getExcludedDays() != null) {
                setExcludedDays(originalEvent.getExcludedDays());
            }

            if ("Monthly".equalsIgnoreCase(pattern) && originalEvent.getMonthlyDate() != null) {
                try {
                    LocalDateTime monthly = LocalDateTime.parse(originalEvent.getMonthlyDate());
                    monthlyDatePicker.setValue(monthly.toLocalDate());
                    monthlyHour.getValueFactory().setValue(convertTo12Hour(monthly.getHour()));
                    monthlyMinute.getValueFactory().setValue(monthly.getMinute());
                    monthlyAmPm.setValue(monthly.getHour() >= 12 ? "PM" : "AM");
                } catch (Exception e) {
                    // Handle parse error
                }
            }
        }

        // Reminder
        int minutes = originalEvent.getReminderMinutesBefore();
        if (minutes <= 0) {
            reminderAtStartRadio.setSelected(true);
        } else {
            reminderCustomRadio.setSelected(true);
            if (minutes < 60) {
                reminderValue.setText(String.valueOf(minutes));
                reminderUnit.setValue("minutes");
            } else if (minutes < 1440) {
                reminderValue.setText(String.valueOf(minutes / 60));
                reminderUnit.setValue("hours");
            } else {
                reminderValue.setText(String.valueOf(minutes / 1440));
                reminderUnit.setValue("days");
            }
        }

        // Attachment
        if (originalEvent.getAttachmentUrl() != null) {
            attachmentField.setText(originalEvent.getAttachmentUrl());
        }
    }

    private void populateSlotTiming(LocalDateTime start) {
        slotStartDate.setValue(start.toLocalDate());
        slotStartHour.getValueFactory().setValue(convertTo12Hour(start.getHour()));
        slotStartMinute.getValueFactory().setValue(start.getMinute());
        slotStartAmPm.setValue(start.getHour() >= 12 ? "PM" : "AM");

        if (originalEvent.getEndDateTime() != null && !originalEvent.getEndDateTime().isEmpty()) {
            LocalDateTime end = LocalDateTime.parse(originalEvent.getEndDateTime());
            long minutes = Duration.between(start, end).toMinutes();

            if (minutes < 60) {
                slotDurationValue.setText(String.valueOf(minutes));
                slotDurationUnit.setValue("minutes");
            } else if (minutes % 60 == 0) {
                slotDurationValue.setText(String.valueOf(minutes / 60));
                slotDurationUnit.setValue("hours");
            } else {
                slotDurationValue.setText(String.valueOf(minutes));
                slotDurationUnit.setValue("minutes");
            }
        }
    }

    private void populateSpanTiming(LocalDateTime start) {
        spanStartDate.setValue(start.toLocalDate());
        spanStartHour.getValueFactory().setValue(convertTo12Hour(start.getHour()));
        spanStartMinute.getValueFactory().setValue(start.getMinute());
        spanStartAmPm.setValue(start.getHour() >= 12 ? "PM" : "AM");

        if (originalEvent.getEndDateTime() != null && !originalEvent.getEndDateTime().isEmpty()) {
            LocalDateTime end = LocalDateTime.parse(originalEvent.getEndDateTime());
            spanEndDate.setValue(end.toLocalDate());
            spanEndHour.getValueFactory().setValue(convertTo12Hour(end.getHour()));
            spanEndMinute.getValueFactory().setValue(end.getMinute());
            spanEndAmPm.setValue(end.getHour() >= 12 ? "PM" : "AM");
        }
    }

    private void populateTargetTiming(LocalDateTime target) {
        targetDate.setValue(target.toLocalDate());
        targetHour.getValueFactory().setValue(convertTo12Hour(target.getHour()));
        targetMinute.getValueFactory().setValue(target.getMinute());
        targetAmPm.setValue(target.getHour() >= 12 ? "PM" : "AM");
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

    private void setExcludedDays(List<String> excludedDays) {
        mondayCheckbox.setSelected(!excludedDays.contains("Monday"));
        tuesdayCheckbox.setSelected(!excludedDays.contains("Tuesday"));
        wednesdayCheckbox.setSelected(!excludedDays.contains("Wednesday"));
        thursdayCheckbox.setSelected(!excludedDays.contains("Thursday"));
        fridayCheckbox.setSelected(!excludedDays.contains("Friday"));
        saturdayCheckbox.setSelected(!excludedDays.contains("Saturday"));
        sundayCheckbox.setSelected(!excludedDays.contains("Sunday"));
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

    private LocalDateTime getDateTime(DatePicker datePicker, Spinner<Integer> hourSpinner,
                                      Spinner<Integer> minuteSpinner, ComboBox<String> amPmCombo) {
        if (datePicker.getValue() == null) return null;
        int hour24 = convertTo24Hour(hourSpinner.getValue(), amPmCombo.getValue());
        return datePicker.getValue().atTime(hour24, minuteSpinner.getValue(), 0);
    }

    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    @FXML
    private void handleUpdateEvent() {
        if (!validateForm()) return;

        String type = typeComboBox.getValue();
        String color = colorToHex(colorPicker.getValue());
        String reminderType = reminderTypeComboBox.getValue();
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        switch (type) {
            case "Slot":
                startDateTime = getDateTime(slotStartDate, slotStartHour, slotStartMinute, slotStartAmPm);
                if (startDateTime == null) return;

                long durationMinutes = parseDurationToMinutes(
                        slotDurationValue.getText(), slotDurationUnit.getValue());
                endDateTime = startDateTime.plusMinutes(durationMinutes);
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

        String repeatPattern = repeatComboBox.getValue();
        if ("No repeat".equals(repeatPattern)) repeatPattern = "NO_REPEAT";

        List<String> excludedDays = "Daily".equals(repeatPattern) ? getExcludedDays() : null;
        String monthlyDate = null;

        if ("Monthly".equals(repeatPattern) && monthlyDatePicker.getValue() != null) {
            LocalDateTime monthly = getDateTime(monthlyDatePicker, monthlyHour, monthlyMinute, monthlyAmPm);
            if (monthly != null) monthlyDate = monthly.format(isoFormatter);
        }

        int reminderMinutes = 0;
        if (reminderCustomRadio.isSelected()) {
            reminderMinutes = parseReminderToMinutes(reminderValue.getText(), reminderUnit.getValue());
        }

        EventCreateRequest updatedEvent = new EventCreateRequest(
                titleField.getText().trim(),
                descriptionArea.getText(),
                type,
                color,
                startDateTime != null ? startDateTime.format(isoFormatter) : "",
                endDateTime != null ? endDateTime.format(isoFormatter) : "",
                originalEvent.getGroup() != null ? originalEvent.getGroup().getEmail() : "",
                originalEvent.isMergeWithPersonal(),
                repeatPattern,
                excludedDays,
                monthlyDate,
                reminderMinutes,
                reminderType,
                attachmentField.getText().trim(),
                LocalDataManager.getUserEmail()
        );

        updateEventInBackend(updatedEvent);
    }

    private long parseDurationToMinutes(String value, String unit) {
        try {
            long num = Long.parseLong(value);
            switch (unit) {
                case "hours": return num * 60;
                case "days": return num * 24 * 60;
                default: return num;
            }
        } catch (NumberFormatException e) {
            return 60;
        }
    }

    private int parseReminderToMinutes(String value, String unit) {
        try {
            int num = Integer.parseInt(value);
            switch (unit) {
                case "hours": return num * 60;
                case "days": return num * 24 * 60;
                default: return num;
            }
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    private void updateEventInBackend(EventCreateRequest updatedEvent) {
        // TODO: Call backend API
        // if (originalEvent.getUuid() == null || originalEvent.getUuid().isEmpty()) {
        //     EventDataManager.updatePersonalEvent(originalEvent.getUuid(), updatedEvent);
        // } else {
        //     EditRequestController.updateEvent(originalEvent.getUuid(), updatedEvent, this);
        // }

        // For now, show success and close
        AlertCreator.showSuccessAlert("Event updated successfully!");
        if (onRefreshCallback != null) onRefreshCallback.run();
        handleClose();
    }

    private boolean validateForm() {
        if (titleField.getText().trim().isEmpty()) {
            showAlert("Missing Title", "Please enter an event title.");
            return false;
        }

        if (typeComboBox.getValue() == null) {
            showAlert("Missing Type", "Please select an event type.");
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
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}