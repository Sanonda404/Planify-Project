package com.planify.frontend.controllers.group;

import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.SceneParent;
import com.planify.frontend.models.group.GroupDetails;
import com.planify.frontend.models.group.GroupSummaryRequest;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.data.group.GroupDataManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class GroupController extends SceneParent {

    @FXML private GridPane groupsGrid;
    @FXML private ListView<NotificationResponse> notificationsList;
    @FXML public Button notifBtn;
    @FXML public VBox notifPanel;
    @FXML private Circle connectionStatusCircle;
    @FXML private Label connectionStatusLabel;

    private List<GroupSummaryRequest> summaries;

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
    private void goBack() {
        SceneManager.switchScene("dashboard-view.fxml", "Dashboard");
    }

    @FXML
    private void goDashboard() {
        SceneManager.switchScene("dashboard-view.fxml", "Dashboard");
    }

    @FXML
    private void openAddGroupForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/planify/frontend/fxmls/add-group-view.fxml"));
            Parent root = loader.load();

            AddGroupController controller = loader.getController();
            controller.setParent(this);

            Stage stage = new Stage();
            stage.setTitle("Create/Join Group");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        refresh();
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
    }

    public void refresh() {
        summaries = GroupDataManager.getGroupSummary();
        groupsGrid.getChildren().clear();

        if (summaries == null || summaries.isEmpty()) {
            Label noGroupsLabel = new Label("✨ No groups yet. Create or join a group to start collaborating!");
            noGroupsLabel.getStyleClass().add("empty-message");
            groupsGrid.getChildren().add(noGroupsLabel);
        } else {
            int col = 0;
            int row = 0;
            for (GroupSummaryRequest summary : summaries) {
                VBox card = createGroupCard(summary);
                groupsGrid.add(card, col, row);
                GridPane.setMargin(card, new Insets(8));

                col++;
                if (col > 2) {
                    col = 0;
                    row++;
                }
            }
        }
    }

    private VBox createGroupCard(GroupSummaryRequest summary) {
        VBox card = new VBox(16);
        card.setAlignment(Pos.TOP_LEFT);
        card.getStyleClass().add("glassCard");
        card.setPadding(new Insets(20));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinWidth(320);

        String role = summary.getRole() != null ? summary.getRole().toLowerCase() : "member";

        // Top Row: Emoji Box + Info + Role
        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Emoji Box
        VBox emojiBox = new VBox();
        emojiBox.setAlignment(Pos.CENTER);
        emojiBox.getStyleClass().addAll("emoji-box", getEmojiBoxClass(summary));
        emojiBox.setMinWidth(65);
        emojiBox.setMinHeight(65);

        Label emojiLabel = new Label(getGroupEmoji(summary));
        emojiLabel.getStyleClass().add("emoji-icon");
        emojiBox.getChildren().add(emojiLabel);

        // Group Info
        VBox infoBox = new VBox(6);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(summary.getName() != null ? summary.getName() : "Unnamed Group");
        nameLabel.getStyleClass().add("group-name");
        nameLabel.setWrapText(true);

        Label membersLabel = new Label("👥 " + summary.getTotalMembers() + " members");
        membersLabel.getStyleClass().add("group-members");

        infoBox.getChildren().addAll(nameLabel, membersLabel);

        // Role Label
        Label roleLabel = new Label(role.toUpperCase());
        roleLabel.getStyleClass().addAll("role-label", getRoleClass(role));

        topRow.getChildren().addAll(emojiBox, infoBox, roleLabel);

        // Description
        Label descLabel = new Label(summary.getDescription() != null && !summary.getDescription().isBlank()
                ? summary.getDescription() : "No description provided");
        descLabel.getStyleClass().add("group-description");
        descLabel.setWrapText(true);

        // Events Row
        HBox eventsRow = new HBox(8);
        eventsRow.getStyleClass().addAll("info-row");
        eventsRow.setAlignment(Pos.CENTER_LEFT);

        Label eventsIcon = new Label("📅");
        eventsIcon.setStyle("-fx-font-size: 16px;");
        Label eventsLabel = new Label("Upcoming Events");
        eventsLabel.getStyleClass().add("info-title");
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Label eventsCount = new Label(String.valueOf(summary.getUpcomingEvents()));
        eventsCount.getStyleClass().add("info-count");

        eventsRow.getChildren().addAll(eventsIcon, eventsLabel, spacer1, eventsCount);

        // Projects Row
        HBox projectsRow = new HBox(8);
        projectsRow.getStyleClass().addAll("info-row", "info-row-bar");
        projectsRow.setAlignment(Pos.CENTER_LEFT);

        Label projectsIcon = new Label("🚀");
        projectsIcon.setStyle("-fx-font-size: 16px;");
        Label projectsLabel = new Label("Active Projects");
        projectsLabel.getStyleClass().add("info-title");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        Label projectsCount = new Label(String.valueOf(summary.getActiveProjects()));
        projectsCount.getStyleClass().add("info-count");

        projectsRow.getChildren().addAll(projectsIcon, projectsLabel, spacer2, projectsCount);

        // Details Button
        Button detailsBtn = new Button("View Details →");
        detailsBtn.getStyleClass().add("details-btn");

        // Style button based on role
        if (role.equals("admin")) {
            detailsBtn.getStyleClass().add("details-btn-admin");
        } else if (role.equals("owner")) {
            detailsBtn.getStyleClass().add("details-btn-owner");
        }

        detailsBtn.setOnAction(e -> openGroupDetails(summary.getUuid()));

        card.getChildren().addAll(topRow, descLabel, eventsRow, projectsRow, detailsBtn);

        return card;
    }

    private String getEmojiBoxClass(GroupSummaryRequest summary) {
        String role = summary.getRole() != null ? summary.getRole().toLowerCase() : "member";
        String name = summary.getName() != null ? summary.getName().toLowerCase() : "";

        if (role.equals("admin")) {
            return "emoji-box-pink";
        } else if (role.equals("owner")) {
            return "emoji-box-purple";
        } else {
            return "emoji-box-blue";
        }
    }

    private String getGroupEmoji(GroupSummaryRequest summary) {
        String name = summary.getName() != null ? summary.getName().toLowerCase() : "";

        if (name.contains("design")) return "🎨";
        if (name.contains("ai") || name.contains("machine")) return "🤖";
        if (name.contains("cse") || name.contains("computer")) return "💻";
        if (name.contains("study") || name.contains("learning")) return "📚";
        if (name.contains("project")) return "🛠️";
        if (name.contains("marketing")) return "📢";
        if (name.contains("research")) return "🔬";
        if (name.contains("sports")) return "⚽";
        if (name.contains("music")) return "🎵";
        if (name.contains("art")) return "🎭";
        return "👥";
    }

    private String getRoleClass(String role) {
        switch (role) {
            case "admin": return "role-admin";
            case "owner": return "role-owner";
            default: return "role-member";
        }
    }

    private void openGroupDetails(String groupUuid) {
        GroupDetails groupDetails = GroupDataManager.getGroupDetails(groupUuid);
        if (groupDetails == null) {
        } else {
            SceneManager.switchScene("group-details-view.fxml", "Group Details", groupDetails);
        }
    }
}