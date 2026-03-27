package com.planify.frontend.controllers.notification;

import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.managers.NotificationManager;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NotificationController {

    private static ObservableList<NotificationResponse> masterList = FXCollections.observableArrayList();;
    private static final String NOTIFICATION_SOUND = "/sounds/notification_pop.mp3";
    private static Shape statusIndicator; // This could be a Circle or Rectangle in your UI
    private static Label statusLabel;

    private static final List<Stage> activeToasts = new ArrayList<>();
    private static final double TOAST_HEIGHT = 80.0; // Height + Spacing
    private static final int MAX_TOASTS = 5;


    public static void setMasterList(ObservableList<NotificationResponse> list) {
        masterList = list;
    }


    /**
     * The main entry point when a WebSocket message arrives.
     */
    public static void handleNewNotification(NotificationResponse notif) {
        Platform.runLater(() -> {
            // 1. Play Sound
            boolean exists = masterList.stream()
                    .anyMatch(n -> n.getUuid().equals(notif.getUuid()));

            if (!exists) {
                System.out.println("New Notification Received: " + notif.getUuid());

                // 2. Play Sound
                playSound();

                // 3. Update the List
                masterList.addFirst(notif);

                NotificationManager.addNotification(notif);

                // 4. Show the Stacked Toast
                showToast(notif);
            } else {
                System.out.println("Ignored duplicate notification: " + notif.getUuid());
            }
        });
    }

    private static void playSound() {
        try {
            AudioClip plop = new AudioClip(Objects.requireNonNull(
                    SceneManager.class.getResource(NOTIFICATION_SOUND)).toExternalForm());
            plop.play();
        } catch (Exception e) {
            System.err.println("Could not play notification sound: " + e.getMessage());
        }
    }

    private static void showToast(NotificationResponse notif) {
        if (activeToasts.size() >= MAX_TOASTS) {
            activeToasts.getFirst().close();
            activeToasts.removeFirst();
            shiftToastsDown();
        }
        Stage toastStage = new Stage();
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);

        // UI Layout for Toast
        VBox root = new VBox(5);
        root.getStyleClass().add("notification-toast");
        root.setMinWidth(250);

        Label title = new Label(notif.getSender() + " (" + notif.getType() + ")");
        title.getStyleClass().add("toast-title");

        Label message = new Label(notif.getMessage());
        message.getStyleClass().add("toast-message");
        message.setWrapText(true);

        root.getChildren().addAll(title, message);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        // Load your CSS here
        scene.getStylesheets().add(Objects.requireNonNull(
                SceneManager.class.getResource("/com/planify/frontend/css/style.css")).toExternalForm());

        toastStage.setScene(scene);

        // Position: Bottom Right of screen
        double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        toastStage.setX(screenWidth - 270);
        toastStage.setY(screenHeight - 120);

        // --- ANIMATIONS ---

        // 1. Slide In from bottom
        root.setTranslateY(50);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), root);
        slideIn.setToY(0);

        // 2. Fade In
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // 3. Stay and then Fade Out
        FadeTransition fadeOut = new FadeTransition(Duration.millis(800), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(4)); // Visible for 4 seconds
        fadeOut.setOnFinished(e -> toastStage.close());

        root.setOnMouseClicked(e -> {
            toastStage.close();
            activeToasts.remove(toastStage);
            shiftToastsDown();
            // Optional: MainController.getInstance().openNotificationPanel();
        });

        activeToasts.add(toastStage);

        toastStage.show();
        slideIn.play();
        fadeIn.play();
        fadeOut.play();
    }

    public static void setStatusControls(Shape indicator, Label label) {
        statusIndicator = indicator;
        statusLabel = label;
    }

    public static void updateStatus(boolean isOnline) {
        Platform.runLater(() -> {
            if (statusIndicator == null) return;

            if (isOnline) {
                statusIndicator.setFill(Color.web("#00FFCC")); // Planify Cyan
                if (statusLabel != null) statusLabel.setText("Connected");

                // Remove any "Warning" animations
                statusIndicator.setEffect(null);
            } else {
                statusIndicator.setFill(Color.web("#FF4B4B")); // Error Red
                if (statusLabel != null) statusLabel.setText("Offline - Reconnecting...");

                // Add a "Glow" pulse to show it's trying to reconnect
                applyPulseAnimation(statusIndicator);
            }
        });
    }

    private static void applyPulseAnimation(Shape node) {
        FadeTransition pulse = new FadeTransition(Duration.seconds(1), node);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.3);
        pulse.setCycleCount(FadeTransition.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    private static void setupAutoClose(Stage stage) {
        PauseTransition delay = new PauseTransition(Duration.seconds(4));
        delay.setOnFinished(e -> {
            // Fade out animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                stage.close();
                activeToasts.remove(stage);
                shiftToastsDown(); // Move remaining toasts down
            });
            fadeOut.play();
        });
        delay.play();
    }

    private static void shiftToastsDown() {
        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        for (int i = 0; i < activeToasts.size(); i++) {
            Stage s = activeToasts.get(i);
            double targetY = (screenHeight - 120) - (i * TOAST_HEIGHT);

            // Update stage position
            s.setY(targetY);
        }
    }
}