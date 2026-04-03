package com.planify.frontend.models;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.utils.InitApp;
import com.planify.frontend.utils.managers.SceneManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

public abstract class SceneParent {

    public VBox createNotificationItem(NotificationResponse notif) {
        VBox item = new VBox(8); // Increased spacing
        boolean isRead = "READ".equalsIgnoreCase(notif.getStatus());
        item.getStyleClass().addAll("notif-item", isRead ? "notif-read" : "notif-unread");
        item.setPadding(new Insets(15));

        // Title
        Label titleLabel = new Label(notif.getTitle());
        titleLabel.getStyleClass().add("notif-item-title");

        // Message
        Label textLabel = new Label(notif.getMessage());
        textLabel.getStyleClass().add("notif-item-text");
        textLabel.setWrapText(true);

        item.getChildren().addAll(titleLabel, textLabel);

        // ACTION BUTTONS (Only for Invites)
        if (("GROUP_INVITE".equalsIgnoreCase(notif.getType()) || "JOIN_REQUEST".equalsIgnoreCase(notif.getType())) && !isRead) {
            HBox actions = new HBox(10);
            actions.setPadding(new Insets(5, 0, 0, 0));

            Button acceptBtn = new Button("Accept");
            acceptBtn.getStyleClass().add("btn-accept");
            acceptBtn.setOnAction(e -> handleInviteAction(notif, "ACCEPT", item));

            Button declineBtn = new Button("Decline");
            declineBtn.getStyleClass().add("btn-decline");
            declineBtn.setOnAction(e -> handleInviteAction(notif, "DECLINE", item));

            actions.getChildren().addAll(acceptBtn, declineBtn);
            item.getChildren().add(actions);
        }

        // Time Label (Optional but professional)
        Label timeLabel = new Label(com.planify.frontend.utils.helpers.DateTimeFormatter.FormatDateTime(LocalDateTime.parse(notif.getCreatedAt()))); // Use a formatter if available
        timeLabel.getStyleClass().add("notif-time");
        item.getChildren().add(timeLabel);

        return item;
    }

    private void handleInviteAction(NotificationResponse notif, String action, VBox uiItem) {
        // 1. Call your Backend API
        System.out.println("Sending " + action + " for Group: " + notif.getTargetUuid());

        // 2. Disable buttons so they can't click twice
        uiItem.getChildren().removeIf(node -> node instanceof HBox);

        // 3. Mark as read visually
        uiItem.getStyleClass().remove("notif-unread");
        uiItem.getStyleClass().add("notif-read");

        // TODO: Add your GroupServiceClient.respondToInvite(notif.getReferenceId(), action) call here
        if(notif.getType().equalsIgnoreCase("GROUP_INVITE")) CreateRequestController.handleAcceptInvitation(notif.getTargetUuid(),this);
        else if (notif.getType().equalsIgnoreCase("JOIN_REQUEST")) {
            CreateRequestController.handleAcceptJoinRequest(notif.getTargetUuid(), notif.getSender(), this);
        }
    }

    @FXML
    private void handleLogout() {
        // TODO: Clear session data
        InitApp.clearData();
        SceneManager.switchScene("login-view.fxml", "Login");
    }

    protected abstract void refresh();
}
