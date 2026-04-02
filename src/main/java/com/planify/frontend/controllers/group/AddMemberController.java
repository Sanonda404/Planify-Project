package com.planify.frontend.controllers.group;

import com.planify.frontend.controllers.Request.CreateRequestController;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AddMemberController {

    @FXML private VBox emailContainer;
    @FXML private Label inviteStatusLabel;

    private String grpUuid;
    private GroupDetailsController parentController;

    // Email validation regex
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private int emailRowCounter = 0;

    public void setGrpUuid(String grpUuid) {
        this.grpUuid = grpUuid;
    }

    public void setParentController(GroupDetailsController parent) {
        this.parentController = parent;
    }

    @FXML
    private void handleAddEmailField() {
        HBox row = createEmailRow();
        emailContainer.getChildren().add(row);

        // Focus on the new email field
        for (Node child : row.getChildren()) {
            if (child instanceof TextField) {
                child.requestFocus();
                break;
            }
        }
    }

    @FXML
    private void removeEmailRow(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        HBox row = (HBox) source.getParent();
        emailContainer.getChildren().remove(row);
        clearStatus();
    }

    private HBox createEmailRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("am-email-row");

        TextField emailField = new TextField();
        emailField.setPromptText("Enter email address");
        emailField.getStyleClass().add("am-email-input");
        emailField.setPrefWidth(Double.MAX_VALUE);
        HBox.setHgrow(emailField, javafx.scene.layout.Priority.ALWAYS);

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("am-remove-btn");
        removeBtn.setOnAction(this::removeEmailRow);

        row.getChildren().addAll(emailField, removeBtn);
        return row;
    }

    private void clearStatus() {
        inviteStatusLabel.setVisible(false);
        inviteStatusLabel.setManaged(false);
        inviteStatusLabel.getStyleClass().removeAll("am-status-success", "am-status-error");
    }

    private void showStatus(String message, boolean isSuccess) {
        inviteStatusLabel.setText(message);
        inviteStatusLabel.getStyleClass().removeAll("am-status-success", "am-status-error");
        inviteStatusLabel.getStyleClass().add(isSuccess ? "am-status-success" : "am-status-error");
        inviteStatusLabel.setVisible(true);
        inviteStatusLabel.setManaged(true);
    }

    @FXML
    private void handleSendInvites() {
        List<String> emails = extractEmails();

        if (emails.isEmpty()) {
            showStatus("Please add at least one email address.", false);
            return;
        }

        // Validate email formats
        List<String> invalidEmails = validateEmails(emails);
        if (!invalidEmails.isEmpty()) {
            showStatus("Invalid email format: " + String.join(", ", invalidEmails), false);
            return;
        }

        // Remove duplicates
        Set<String> uniqueEmails = new HashSet<>(emails);
        if (uniqueEmails.size() < emails.size()) {
            showStatus("Duplicate emails were removed.", true);
        }

        List<String> finalEmails = new ArrayList<>(uniqueEmails);

        System.out.println("going..");

        // Call backend
        CreateRequestController.handleAddMember(grpUuid, finalEmails, this);
        handleClose();
    }

    private List<String> extractEmails() {
        List<String> emails = new ArrayList<>();

        for (Node node : emailContainer.getChildren()) {
            if (node instanceof HBox row) {
                for (Node child : row.getChildren()) {
                    if (child instanceof TextField tf) {
                        String email = tf.getText().trim().toLowerCase();
                        if (!email.isEmpty()) {
                            emails.add(email);
                        }
                    }
                }
            }
        }
        return emails;
    }

    private List<String> validateEmails(List<String> emails) {
        List<String> invalid = new ArrayList<>();
        for (String email : emails) {
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                invalid.add(email);
            }
        }
        return invalid;
    }

    public void onInviteSuccess() {
        showStatus("Invitations sent successfully!", true);

        // Auto-close after 1.5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                javafx.application.Platform.runLater(() -> {
                    if (parentController != null) {
                        parentController.refresh();
                    }
                    handleClose();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void onInviteFailure(String error) {
        showStatus("Failed to send invitations: " + error, false);
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) inviteStatusLabel.getScene().getWindow();
        stage.close();
    }
}