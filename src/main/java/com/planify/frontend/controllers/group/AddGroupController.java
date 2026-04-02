package com.planify.frontend.controllers.group;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.group.GroupCreateRequest;
import com.planify.frontend.models.group.GroupJoinRequest;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.helpers.AlertCreator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class AddGroupController {

    // --- CREATE TAB FIELDS ---
    @FXML private Button createTabBtn;
    @FXML private Button joinTabBtn;
    @FXML private VBox createSection;
    @FXML private VBox joinSection;
    @FXML private TextField groupNameField;
    @FXML private ComboBox<String> groupTypeCombo;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> postingAccessCombo;
    @FXML private CheckBox allowJoinCodeCheck;

    // --- JOIN TAB FIELDS ---
    @FXML private TextField joinCodeField;
    @FXML private TextArea joinMessageArea;

    private Object parent;

    public void setParent(Object parent) {
        this.parent = parent;
    }

    // --- TAB SWITCHING ---
    @FXML
    private void showCreateTab() {
        createSection.setVisible(true);
        createSection.setManaged(true);
        joinSection.setVisible(false);
        joinSection.setManaged(false);

        createTabBtn.getStyleClass().setAll("gm-tab-active");
        joinTabBtn.getStyleClass().setAll("gm-tab-inactive");
    }

    @FXML
    private void showJoinTab() {
        joinSection.setVisible(true);
        joinSection.setManaged(true);
        createSection.setVisible(false);
        createSection.setManaged(false);

        joinTabBtn.getStyleClass().setAll("gm-tab-active");
        createTabBtn.getStyleClass().setAll("gm-tab-inactive");

    }


    // --- ACTIONS ---
    @FXML
    private void createGroup() {
        String name = groupNameField.getText().trim();
        if (name.isEmpty()) {
            AlertCreator.showErrorAlert("Missing Name", "Please enter a group name.");
            return;
        }

        String type = groupTypeCombo.getValue();
        if (type == null) {
            AlertCreator.showErrorAlert("Missing Type", "Please select a group type.");
            return;
        }

        String description = descriptionArea.getText().trim();
        if (description.isEmpty()) {
            description = "A " + type.toLowerCase() + " group";
        }

        String postingAccess = postingAccessCombo.getValue();
        if (postingAccess == null) {
            AlertCreator.showErrorAlert("Missing Permission", "Please select posting access permission.");
            return;
        }

        boolean onlyAdminCanPost = "Admins Only".equals(postingAccess);
        boolean allowJoinCode = allowJoinCodeCheck.isSelected();

        GroupCreateRequest group = new GroupCreateRequest(
                name, description, type, allowJoinCode, onlyAdminCanPost,
                LocalDataManager.getUserEmail());

        CreateRequestController.handleCreateGroup(group, parent);
    }

    @FXML
    private void joinGroup() {
        String code = joinCodeField.getText().trim().toUpperCase();
        if (code.isEmpty()) {
            AlertCreator.showErrorAlert("Missing Code", "Please enter the group code.");
            return;
        }


        String message = joinMessageArea.getText() == null ? "" : joinMessageArea.getText().trim();

        GroupJoinRequest request = new GroupJoinRequest(code, message);
        CreateRequestController.handleJoinGroup(request, this);
        closeWindow();
    }

    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    public void onJoinSuccess() {
        AlertCreator.showSuccessAlert("Successfully joined the group!");
        closeWindow();
    }

    public void onJoinFailure(String error) {
        AlertCreator.showErrorAlert("Join Failed", error);
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) groupNameField.getScene().getWindow();
        stage.close();
        if (parent != null && parent instanceof GroupController) {
            ((GroupController) parent).refresh();
        }
    }

    // --- INITIALIZATION ---
    @FXML
    private void initialize() {
        // Populate combo boxes
        groupTypeCombo.getItems().addAll("Personal", "Academic", "Work", "Project", "Social", "Other");
        postingAccessCombo.getItems().addAll("All Members", "Admins Only");

        // Set default values
        groupTypeCombo.getSelectionModel().selectFirst();
        postingAccessCombo.getSelectionModel().selectFirst();
        allowJoinCodeCheck.setSelected(true);

        // Default to Create tab
        showCreateTab();
    }
}