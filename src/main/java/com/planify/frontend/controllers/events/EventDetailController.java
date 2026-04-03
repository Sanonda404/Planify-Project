package com.planify.frontend.controllers.events;

import com.planify.frontend.controllers.Request.DeleteRequestController;
import com.planify.frontend.controllers.Request.EditRequestController;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.utils.data.personal.EventDataManager;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.helpers.AlertCreator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventDetailController {

    @FXML private Label eventIcon;
    @FXML private Label titleLabel;
    @FXML private Label typeBadge;
    @FXML private Label repeatBadge;
    @FXML private Label groupLabel;
    @FXML private Label typeLabel;
    @FXML private Label startLabel;
    @FXML private Label endLabel;
    @FXML private Label endLabelHeader;
    @FXML private Label durationLabel;
    @FXML private Label durationLabelHeader;
    @FXML private Label descriptionLabel;
    @FXML private Label repeatPatternLabel;
    @FXML private Label excludedDaysLabel;
    @FXML private Label excludedLabelHeader;
    @FXML private Label monthlyDateLabel;
    @FXML private Label monthlyLabelHeader;
    @FXML private Label reminderLabel;
    @FXML private Hyperlink attachmentLink;
    @FXML private Label creatorLabel;
    @FXML private Label createdAtLabel;
    @FXML private VBox repeatInfoBox;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    private EventGetRequest event;
    private Runnable onRefreshCallback;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • h:mm a");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

    @FXML
    public void initialize() {
        // Initial setup
    }

    public void setEvent(EventGetRequest event) {
        this.event = event;
        System.out.println(event.getUuid());
        populateEventDetails();
        configurePermissions();
    }

    public void setOnRefreshCallback(Runnable callback) {
        this.onRefreshCallback = callback;
    }

    private void populateEventDetails() {
        // Set icon based on event type
        setEventIcon();

        // Title
        titleLabel.setText(event.getTitle());

        // Type Badge
        String type = event.getType() != null ? event.getType().toLowerCase() : "slot";
        typeBadge.setText(event.getType() != null ? event.getType() : "Slot");
        typeBadge.getStyleClass().add(getTypeBadgeClass(type));

        // Repeat Badge
        if (event.getRepeatPattern() != null && !"NO_REPEAT".equalsIgnoreCase(event.getRepeatPattern())) {
            repeatBadge.setText(getRepeatPatternDisplay(event.getRepeatPattern()));
            repeatBadge.setVisible(true);
            repeatBadge.setManaged(true);
        }

        // Group
        if (event.getGroup() != null) {
            groupLabel.setText(event.getGroup().getName());
        } else {
            groupLabel.setText("Personal");
        }

        // Type
        typeLabel.setText(event.getType() != null ? event.getType() : "Slot");

        // Start Time
        if (event.getStartDateTime() != null && !event.getStartDateTime().isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(event.getStartDateTime());
            startLabel.setText(start.format(dateTimeFormatter));
        }

        // End Time & Duration
        if (event.getEndDateTime() != null && !event.getEndDateTime().isEmpty()) {
            LocalDateTime end = LocalDateTime.parse(event.getEndDateTime());
            endLabel.setText(end.format(dateTimeFormatter));
            endLabelHeader.setVisible(true);
            endLabel.setVisible(true);

            // Calculate duration for Slot events
            if ("slot".equalsIgnoreCase(event.getType())) {
                LocalDateTime start = LocalDateTime.parse(event.getStartDateTime());
                long minutes = java.time.Duration.between(start, end).toMinutes();
                if (minutes < 60) {
                    durationLabel.setText(minutes + " minutes");
                } else if (minutes == 60) {
                    durationLabel.setText("1 hour");
                } else {
                    long hours = minutes / 60;
                    long mins = minutes % 60;
                    durationLabel.setText(hours + " hour" + (hours > 1 ? "s" : "") +
                            (mins > 0 ? " " + mins + " min" : ""));
                }
                durationLabelHeader.setVisible(true);
                durationLabel.setVisible(true);
            }
        } else {
            // Deadline/Marker event
            endLabelHeader.setText("Target");
            endLabel.setText(startLabel.getText());
            endLabelHeader.setVisible(true);
            endLabel.setVisible(true);
        }

        // Description
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            descriptionLabel.setText(event.getDescription());
        } else {
            descriptionLabel.setText("No description provided");
        }

        // Repeat Info
        populateRepeatInfo();

        // Reminder
        populateReminderInfo();

        // Attachment
        if (event.getAttachmentUrl() != null && !event.getAttachmentUrl().isEmpty()) {
            attachmentLink.setText(event.getAttachmentUrl());
            attachmentLink.setOnAction(e -> openUrl(event.getAttachmentUrl()));
        } else {
            attachmentLink.setText("No attachment");
            attachmentLink.setDisable(true);
        }

        // Creator
        if (event.getCreator() != null) {
            creatorLabel.setText(event.getCreator().getName());
            if (event.getCreator().getEmail() != null) {
                creatorLabel.setTooltip(new Tooltip(event.getCreator().getEmail()));
            }
        }
    }

    private void setEventIcon() {
        String type = event.getType() != null ? event.getType().toLowerCase() : "slot";
        switch (type) {
            case "slot":
                eventIcon.setText("📅");
                break;
            case "span":
                eventIcon.setText("📊");
                break;
            case "deadline":
                eventIcon.setText("⏰");
                break;
            case "marker":
                eventIcon.setText("📍");
                break;
            default:
                eventIcon.setText("📌");
        }
    }

    private String getTypeBadgeClass(String type) {
        switch (type) {
            case "slot": return "event-type-slot";
            case "span": return "event-type-span";
            case "deadline": return "event-type-deadline";
            case "marker": return "event-type-marker";
            default: return "event-type-slot";
        }
    }

    private String getRepeatPatternDisplay(String pattern) {
        switch (pattern.toUpperCase()) {
            case "DAILY": return "Daily";
            case "WEEKLY": return "Weekly";
            case "MONTHLY": return "Monthly";
            default: return pattern;
        }
    }


    private void populateRepeatInfo() {
        if (event.getRepeatPattern() != null && !"NO_REPEAT".equalsIgnoreCase(event.getRepeatPattern())) {
            repeatInfoBox.setVisible(true);
            repeatInfoBox.setManaged(true);

            repeatPatternLabel.setText(getRepeatPatternDisplay(event.getRepeatPattern()));

            // Excluded Days
            if (event.getExcludedDays() != null && !event.getExcludedDays().isEmpty()) {
                excludedLabelHeader.setVisible(true);
                String excluded = String.join(", ", event.getExcludedDays());
                excludedDaysLabel.setText(excluded);
            }

            // Monthly Date
            if (event.getMonthlyDate() != null && !event.getMonthlyDate().isEmpty()) {
                monthlyLabelHeader.setVisible(true);
                try {
                    LocalDateTime monthly = LocalDateTime.parse(event.getMonthlyDate());
                    monthlyDateLabel.setText(monthly.format(dateFormatter));
                } catch (Exception e) {
                    monthlyDateLabel.setText(event.getMonthlyDate());
                }
            }
        }
    }

    private void populateReminderInfo() {
        int minutes = event.getReminderMinutesBefore();
        if (minutes <= 0) {
            reminderLabel.setText("At start time");
        } else if (minutes < 60) {
            reminderLabel.setText(minutes + " minutes before");
        } else if (minutes == 60) {
            reminderLabel.setText("1 hour before");
        } else if (minutes < 1440) {
            int hours = minutes / 60;
            reminderLabel.setText(hours + " hours before");
        } else {
            int days = minutes / 1440;
            reminderLabel.setText(days + " day" + (days > 1 ? "s" : "") + " before");
        }
    }

    private void configurePermissions() {
        boolean canEdit = event.isEditingPermission();

        // Check if current user is creator
        boolean isCreator = event.getCreator() != null &&
                event.getCreator().getEmail() != null &&
                event.getCreator().getEmail().equals(LocalDataManager.getUserEmail());

        // Only show edit/delete if user has permission
        if (!canEdit && !isCreator) {
            editButton.setDisable(true);
            deleteButton.setDisable(true);
            editButton.setOpacity(0.5);
            deleteButton.setOpacity(0.5);
        }
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            AlertCreator.showErrorAlert("Cannot open link", "Unable to open URL: " + url);
        }
    }

    @FXML
    private void handleEdit() {
        // TODO: Open edit event dialog with pre-filled data
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/planify/frontend/fxmls/edit-event-view.fxml"));
            Parent root = loader.load();

            EditEventController controller = loader.getController();
            controller.setEvent(event);
            controller.setParentCallback(this::refreshAndClose);

            Stage stage = new Stage();
            stage.setTitle("Edit Event");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            AlertCreator.showErrorAlert("Error", "Could not open edit form.");
        }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Event");
        confirm.setHeaderText("Delete " + event.getTitle() + "?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Call backend to delete event
                //personal event
                if(event.getUuid().startsWith("PER")) EventDataManager.deleteEvent(event.getTitle());
                else DeleteRequestController.deleteEvent(event.getUuid(), this);
                refreshAndClose();
            }
        });
    }

    private void deleteEventLocally() {
        // TODO: Remove from local data manager
        EventDataManager.deleteEvent(event.getUuid());

        refreshAndClose();
        AlertCreator.showSuccessAlert("Event deleted successfully!");
    }

    private void refreshAndClose() {
        if (onRefreshCallback != null) {
            onRefreshCallback.run();
        }
        handleClose();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}