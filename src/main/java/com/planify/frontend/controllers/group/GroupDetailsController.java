package com.planify.frontend.controllers.group;

import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.SceneParent;
import com.planify.frontend.models.group.GroupDetails;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.data.group.GroupDataManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.IOException;

public class GroupDetailsController extends SceneParent {

    @FXML private Label groupNameLabel;
    @FXML private Label groupMetaLabel;
    @FXML private Label groupCodeLabel;
    @FXML private Label roleBadgeLabel;
    @FXML private Label heroEmojiLabel;

    // Stats Labels
    @FXML private Label memberCountLabel;
    @FXML private Label eventCountLabel;
    @FXML private Label projectCountLabel;
    @FXML private Label adminCountLabel;

    @FXML private ToggleButton membersTab;
    @FXML private ToggleButton eventsTab;
    @FXML private ToggleButton projectsTab;
    @FXML private ToggleButton infoTab;

    @FXML private StackPane dynamicContent;

    private GroupDetails groupDetails;
    private GroupMemberController groupMemberController;
    private GroupEventController groupEventController;
    private GroupProjectController groupProjectController;
    private GroupInfoController groupInfoController;

    @FXML private Circle connectionStatusCircle;
    @FXML private Label connectionStatusLabel;
    @FXML private ListView<NotificationResponse> notificationsList;
    @FXML public Button notifBtn;
    @FXML public VBox notifPanel;

    public void init() {
        NotificationController.setStatusControls(connectionStatusCircle, connectionStatusLabel);
    }

    @FXML
    private void toggleNotifications() {
        boolean isVisible = notifPanel.isVisible();
        notifPanel.setVisible(!isVisible);
        notifPanel.setManaged(!isVisible);
    }

    @FXML
    private void initialize() {
        notificationsList.setItems(NotificationManager.getNotifications());
        notificationsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(NotificationResponse notif, boolean empty) {
                super.updateItem(notif, empty);
                if (empty || notif == null) {
                    setGraphic(null);
                } else {
                    setGraphic(createNotificationItem(notif));
                }
            }
        });

        init();
        NotificationManager.setParent(this);

        ToggleGroup group = new ToggleGroup();
        membersTab.setToggleGroup(group);
        eventsTab.setToggleGroup(group);
        projectsTab.setToggleGroup(group);
        infoTab.setToggleGroup(group);
    }

    public void setGroupDetails(GroupDetails details) {
        this.groupDetails = details;

        // Update header
        groupNameLabel.setText(details.getName());
        groupMetaLabel.setText(details.getGroupType() + " • " +
                (details.getMembers() != null ? details.getMembers().size() : 0) + " members");
        groupCodeLabel.setText(details.getCode());

        // Update role badge
        String role = details.getRole() != null ? details.getRole().toLowerCase() : "member";
        roleBadgeLabel.getStyleClass().removeAll("role-badge-admin", "role-badge-owner", "role-badge-member");
        switch (role) {
            case "admin":
                roleBadgeLabel.getStyleClass().add("role-badge-admin");
                roleBadgeLabel.setText("⭐ Admin");
                break;
            case "owner":
                roleBadgeLabel.getStyleClass().add("role-badge-owner");
                roleBadgeLabel.setText("👑 Owner");
                break;
            default:
                roleBadgeLabel.getStyleClass().add("role-badge-member");
                roleBadgeLabel.setText("👤 Member");
                break;
        }

        // Update hero emoji
        heroEmojiLabel.setText(getGroupEmoji(details.getName()));

        // Update stats
        updateStats();

        // Default tab
        membersTab.setSelected(true);
        loadMembersTab();

        // Add listeners
        membersTab.setOnAction(e -> loadMembersTab());
        eventsTab.setOnAction(e -> loadEventsTab());
        projectsTab.setOnAction(e -> loadProjectsTab());
        infoTab.setOnAction(e -> loadInfoTab());
    }

    private void updateStats() {
        memberCountLabel.setText(String.valueOf(groupDetails.getMembers() != null ? groupDetails.getMembers().size() : 0));
        eventCountLabel.setText(String.valueOf(groupDetails.getEvents() != null ? groupDetails.getEvents().size() : 0));
        projectCountLabel.setText(String.valueOf(groupDetails.getProjects() != null ? groupDetails.getProjects().size() : 0));

        long adminCount = groupDetails.getMembers() != null ?
                groupDetails.getMembers().stream()
                        .filter(m -> "admin".equalsIgnoreCase(m.getRole()) || "owner".equalsIgnoreCase(m.getRole()))
                        .count() : 0;
        adminCountLabel.setText(String.valueOf(adminCount));
    }

    private String getGroupEmoji(String name) {
        if (name == null) return "👥";
        String lowerName = name.toLowerCase();
        if (lowerName.contains("design")) return "🎨";
        if (lowerName.contains("ai") || lowerName.contains("machine")) return "🤖";
        if (lowerName.contains("cse") || lowerName.contains("computer")) return "💻";
        if (lowerName.contains("study") || lowerName.contains("learning")) return "📚";
        if (lowerName.contains("project")) return "🛠️";
        if (lowerName.contains("marketing")) return "📢";
        if (lowerName.contains("research")) return "🔬";
        if (lowerName.contains("sports")) return "⚽";
        if (lowerName.contains("music")) return "🎵";
        return "👥";
    }

    public void refresh() {
        Platform.runLater(()->{
            groupDetails = GroupDataManager.getGroupDetails(groupDetails.getUuid());
            if (groupDetails != null) {
                updateStats();

                if (groupMemberController != null) {
                    groupMemberController.setRole(groupDetails.getRole());
                    groupMemberController.setGrpUuid(groupDetails.getUuid());
                    groupMemberController.setMembers(groupDetails.getMembers());
                }

                if (groupEventController != null) {
                    groupEventController.setEvents(groupDetails.getEvents());
                }

                if (groupProjectController != null) {
                    groupProjectController.setProjects(groupDetails.getProjects());
                }

                if (groupInfoController != null) {
                    groupInfoController.setGroupData(groupDetails);
                }
            }
        });
    }

    private void loadMembersTab() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(
                    "/com/planify/frontend/fxmls/group-members-view.fxml"));
            Node content = loader.load();
            groupMemberController = loader.getController();
            groupMemberController.setRole(groupDetails.getRole());
            groupMemberController.setGrpUuid(groupDetails.getUuid());
            groupMemberController.setMembers(groupDetails.getMembers());
            groupMemberController.setParent(this);

            dynamicContent.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadEventsTab() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(
                    "/com/planify/frontend/fxmls/group-events-view.fxml"));
            Node content = loader.load();
            groupEventController = loader.getController();
            groupEventController.setEvents(groupDetails.getEvents());
            groupEventController.setParent(this);
            groupEventController.setGroupUuid(groupDetails.getUuid());

            dynamicContent.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadProjectsTab() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(
                    "/com/planify/frontend/fxmls/group-project-view.fxml"));
            Node content = loader.load();
            groupProjectController = loader.getController();
            groupProjectController.setProjects(groupDetails.getProjects());
            groupProjectController.setGroupContext(groupDetails.getUuid(), groupDetails.getName(), groupDetails.getMembers().size());

            dynamicContent.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadInfoTab() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(
                    "/com/planify/frontend/fxmls/group-info-view.fxml"));
            Node content = loader.load();
            groupInfoController = loader.getController();
            groupInfoController.setGroupData(groupDetails);
            groupInfoController.setParent(this);

            dynamicContent.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        SceneManager.switchScene("group-view.fxml", "Groups");
    }

    @FXML
    private void goDashboard() {
        SceneManager.switchScene("dashboard-view.fxml", "Dashboard");
    }

}