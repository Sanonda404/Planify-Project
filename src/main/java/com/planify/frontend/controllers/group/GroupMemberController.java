package com.planify.frontend.controllers.group;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.group.GroupMember;
import com.planify.frontend.utils.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class GroupMemberController {

    @FXML private VBox membersContainer;
    @FXML private Button addMemberBtn;

    private String grpUuid;
    private String role = "";
    private GroupDetailsController parent;
    private List<GroupMember> currentMembers;
    private String currentUserEmail = UserSession.getInstance().getEmail();

    public void setGrpUuid(String grpUuid) {
        this.grpUuid = grpUuid;
    }

    public void setRole(String role) {
        this.role = role;
        // Only admins/owners can add members
        if (!"admin".equalsIgnoreCase(role) && !"owner".equalsIgnoreCase(role)) {
            addMemberBtn.setDisable(true);
            addMemberBtn.setOpacity(0.5);
        }
    }

    public void setParent(GroupDetailsController parent) {
        this.parent = parent;
    }

    @FXML
    private void initialize() {
        // Initialization
    }

    public void setMembers(List<GroupMember> members) {
        this.currentMembers = members;
        refresh(members);
    }

    public void refresh(List<GroupMember> members) {
        System.out.println("Refreshing Members..");
        membersContainer.getChildren().clear();

        if (members == null || members.isEmpty()) {
            VBox emptyState = createEmptyState();
            membersContainer.getChildren().add(emptyState);
            return;
        }

        for (GroupMember member : members) {
            System.out.println("member: "+member.getName()+" "+member.getRole());
            HBox card = createMemberCard(member);
            membersContainer.getChildren().add(card);
        }
    }

    private HBox createMemberCard(GroupMember member) {
        HBox card = new HBox(18);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("member-card");
        card.setPadding(new Insets(20, 24, 20, 24));

        // Avatar
        StackPane avatarPane = new StackPane();
        avatarPane.getStyleClass().add("member-avatar");
        Label avatarIcon = new Label(getAvatarEmoji(member.getName()));
        avatarIcon.getStyleClass().add("member-avatar-icon");
        avatarPane.getChildren().add(avatarIcon);

        // Name + email + role
        VBox infoBox = new VBox(4);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(member.getName());
        nameLabel.getStyleClass().add("member-name");

        // Add "(You)" indicator for current user
        if (member.getEmail() != null && member.getEmail().equals(currentUserEmail)) {
            Label youLabel = new Label(" (You)");
            youLabel.setStyle("-fx-text-fill: #457b9d; -fx-font-size: 12px;");
            HBox nameBox = new HBox(2);
            nameBox.getChildren().addAll(nameLabel, youLabel);
            infoBox.getChildren().add(nameBox);
        } else {
            infoBox.getChildren().add(nameLabel);
        }

        Label emailLabel = new Label(member.getEmail());
        emailLabel.getStyleClass().add("member-email");

        infoBox.getChildren().add(emailLabel);

        // Role badge
        String memberRole = member.getRole() != null ? member.getRole().toLowerCase() : "member";
        Label roleLabel;
        if ("admin".equals(memberRole) || "owner".equals(memberRole)) {
            roleLabel = new Label("⭐ Admin");
            roleLabel.getStyleClass().add("badge-admin");
        } else {
            roleLabel = new Label("Member");
            roleLabel.getStyleClass().add("badge-member");
        }

        // Actions (only for admins/owners, and not for self)
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        if ("Admin".equalsIgnoreCase(role) &&
                !member.getEmail().equals(currentUserEmail)) {

            if ("member".equals(memberRole)) {
                Button promoteBtn = new Button("⭐ Promote");
                promoteBtn.getStyleClass().add("btn-promote");
                promoteBtn.setOnAction(e -> promoteMember(member));
                actionsBox.getChildren().add(promoteBtn);
            }

            Button removeBtn = new Button("✕ Remove");
            removeBtn.getStyleClass().add("btn-remove");
            removeBtn.setOnAction(e -> removeMember(member));
            actionsBox.getChildren().add(removeBtn);
        }

        actionsBox.getChildren().add(roleLabel);

        card.getChildren().addAll(avatarPane, infoBox, actionsBox);
        return card;
    }

    private String getAvatarEmoji(String name) {
        if (name == null || name.isEmpty()) return "👤";
        char firstChar = Character.toLowerCase(name.charAt(0));
        if (firstChar >= 'a' && firstChar <= 'z') {
            return String.valueOf(Character.toUpperCase(firstChar));
        }
        return "👤";
    }

    private void promoteMember(GroupMember member) {
        CreateRequestController.handlePromoteMember(grpUuid,
                new MemberInfo(member.getName(), member.getEmail()), this);
    }

    public void removeMember(GroupMember member) {
        CreateRequestController.handleRemoveMember(grpUuid,
                new MemberInfo(member.getName(), member.getEmail()), this);
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(60));
        emptyState.getStyleClass().add("empty-state");

        Label iconLabel = new Label("👥");
        iconLabel.getStyleClass().add("empty-icon");

        Label textLabel = new Label("No members yet");
        textLabel.getStyleClass().add("empty-text");

        Label subLabel = new Label("Invite members to join your group");
        subLabel.getStyleClass().add("empty-text");
        subLabel.setStyle("-fx-font-size: 12px;");

        emptyState.getChildren().addAll(iconLabel, textLabel, subLabel);
        return emptyState;
    }

    @FXML
    private void openAddMember() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/planify/frontend/fxmls/add-member-view.fxml"));
            Parent root = loader.load();

            AddMemberController controller = loader.getController();
            controller.setGrpUuid(grpUuid);

            Stage stage = new Stage();
            stage.setTitle("Invite Member");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}