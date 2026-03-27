package com.planify.frontend;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.util.UUID;

public class NotificationTestLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 40; -fx-background-color: #f4f4f4;");

        Label label = new Label("Planify Notification Tester");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button btnNotify = new Button("Test System Notification");
        btnNotify.setOnAction(e -> showTrayNotification("Meeting Started", "Your 'Frontend Sync' is starting now."));

        Button btnAlert = new Button("Test Mobile-Style Alert");
        btnAlert.setOnAction(e -> showMobileAlert("URGENT DEADLINE", "Project 'Planify' submission in 10 minutes!"));

        root.getChildren().addAll(label, btnNotify, btnAlert);

        primaryStage.setTitle("Planify Test Tool");
        primaryStage.setScene(new Scene(root, 400, 300));
        primaryStage.show();
    }

    /**
     * TEST 1: Standard System Tray Notification
     */
    private void showTrayNotification(String title, String message) {
        if (!SystemTray.isSupported()) {
            System.out.println("System Tray not supported!");
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();
            // Create a dummy 16x16 icon if file is missing
            Image image = Toolkit.getDefaultToolkit().createImage(new byte[0]);
            TrayIcon trayIcon = new TrayIcon(image, "Planify");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);

            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);

            // Clean up icon after 5 seconds
            new Thread(() -> {
                try { Thread.sleep(5000); } catch (Exception ignored) {}
                tray.remove(trayIcon);
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * TEST 2: High-Priority Modal Alert (Always on Top)
     */
    private void showMobileAlert(String titleText, String bodyText) {
        Platform.runLater(() -> {
            Stage alertStage = new Stage();
            alertStage.initStyle(StageStyle.UNDECORATED); // No title bar
            alertStage.setAlwaysOnTop(true);

            VBox layout = new VBox(15);
            layout.setAlignment(Pos.CENTER);
            layout.setStyle("-fx-background-color: #2c3e50; -fx-padding: 25; " +
                    "-fx-border-color: #e74c3c; -fx-border-width: 3; -fx-background-radius: 10; -fx-border-radius: 10;");

            Label header = new Label(titleText);
            header.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;");

            Label body = new Label(bodyText);
            body.setStyle("-fx-text-fill: white; -fx-text-alignment: center;");
            body.setWrapText(true);

            Button closeBtn = new Button("DISMISS");
            closeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
            closeBtn.setOnAction(e -> alertStage.close());

            layout.getChildren().addAll(header, body, closeBtn);

            alertStage.setScene(new Scene(layout));
            alertStage.show();

            // Optional: Play system beep
            Toolkit.getDefaultToolkit().beep();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
