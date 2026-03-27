package com.planify.frontend.controllers.group;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.group.GroupDetails;
import com.planify.frontend.utils.managers.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class GroupInfoController implements Initializable {

    @FXML private Label descriptionLabel;
    @FXML private Label roleLabel;
    @FXML private Label groupTypeLabel;
    @FXML private Label postingAccessLabel;
    @FXML private Label createdLabel;
    @FXML private Label groupCodeDisplayLabel;

    private GroupDetails groupDetails;
    private GroupDetailsController parent;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialization
    }

    public void setGroupData(GroupDetails groupDetails) {
        if (groupDetails == null) return;

        this.groupDetails = groupDetails;

        // Description
        String description = groupDetails.getDescription() != null && !groupDetails.getDescription().isBlank()
                ? groupDetails.getDescription()
                : "No description provided. Click edit to add one.";
        descriptionLabel.setText(description);

        // Role
        String role = groupDetails.getRole() != null && !groupDetails.getRole().isBlank()
                ? capitalize(groupDetails.getRole())
                : "Member";
        roleLabel.setText(role);

        // Group Type
        groupTypeLabel.setText(groupDetails.getGroupType() != null ? groupDetails.getGroupType() : "Not specified");

        // Posting Access
        String postingAccess = groupDetails.getPostingAceess() != null ? groupDetails.getPostingAceess() : "All Members";
        if ("admin".equalsIgnoreCase(postingAccess)) {
            postingAccessLabel.setText("Admin Only");
        } else {
            postingAccessLabel.setText("All Members");
        }

        // Created Date
        try {
            LocalDateTime createdAt = LocalDateTime.parse(groupDetails.getCreatedAt());
            createdLabel.setText(createdAt.format(dateFormatter));
        } catch (Exception e) {
            createdLabel.setText(groupDetails.getCreatedAt());
        }

        // Group Code
        //groupCodeDisplayLabel.setText(groupDetails.getCode() != null ? groupDetails.getCode() : "N/A");
    }

    public void setParent(GroupDetailsController parent) {
        this.parent = parent;
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    @FXML
    private void handleCopyCode() {
        if (groupDetails != null && groupDetails.getCode() != null) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(groupDetails.getCode());
            clipboard.setContent(content);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Copied");
            alert.setHeaderText(null);
            alert.setContentText("Group code copied to clipboard!");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleLeaveGroup() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Leave Group");
        alert.setHeaderText("Are you sure you want to leave " + groupDetails.getName() + "?");
        alert.setContentText("You will lose access to all group content and will need to be reinvited to rejoin.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            CreateRequestController.handleLeaveGroup(groupDetails.getUuid(), this);
        }
    }

    public void onGroupLeft() {
        if (parent != null) {
            SceneManager.switchScene("group-view","Groups");
        }
    }
}