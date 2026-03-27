package com.planify.frontend.utils.services;

import com.planify.frontend.models.notification.ReminderTask;
import com.planify.frontend.utils.managers.ReminderManager;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NotificationService{
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static ScheduledFuture<?> currentTask;
    private static int currentInterval = 60; // Default to 60s

    public static void startMonitoring() {
        // Determine starting interval
        int interval = ReminderManager.getAllReminders().isEmpty() ? 60 : 1;
        schedule(interval);
    }

    private static void schedule(int seconds) {
        if (currentTask != null) currentTask.cancel(false);
        currentInterval = seconds;

        currentTask = scheduler.scheduleAtFixedRate(() -> {
            checkReminders();
        }, 0, seconds, TimeUnit.SECONDS);

        System.out.println("Monitoring started with " + seconds + "s heartbeat.");
    }

    public static void wakeUp() {
        // If we are in 60s mode and a task is added, switch to 1s immediately
        if (currentInterval == 60 && !ReminderManager.getAllReminders().isEmpty()) {
            System.out.println("New task detected! Switching to High Precision (1s).");
            schedule(1);
        }
    }

    private static void checkReminders() {
        List<ReminderTask> activeTasks = ReminderManager.getAllReminders();
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;

        Iterator<ReminderTask> iterator = activeTasks.iterator();
        while (iterator.hasNext()) {
            ReminderTask task = iterator.next();
            try {
                LocalDateTime trigger = LocalDateTime.parse(task.getTriggerAt());
                if (now.isAfter(trigger)) {
                    // Decide Alert vs Notification
                    if ("ALERT".equalsIgnoreCase(task.getType())) {
                        showMobileAlert(task.getTitle(),"EMergency");
                    } else {
                        fireNativeNotification(task.getTitle(), "Event starting now!");
                    }
                    iterator.remove();
                    changed = true;
                }
            } catch (Exception e) {
                iterator.remove(); // Remove malformed dates
            }
        }

        if (changed) {
            ReminderManager.saveReminders();
            // If the list is now empty, throttle back down to 60s to save resources
            if (activeTasks.isEmpty()) {
                System.out.println("All tasks cleared. Throttling down to 60s.");
                schedule(60);
            }
        }
    }
    private static void fireNativeNotification(String title, String body) {
        if (!SystemTray.isSupported()) return;

        Platform.runLater(() -> {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
                TrayIcon trayIcon = new TrayIcon(image, "Planify");
                trayIcon.setImageAutoSize(true);

                tray.add(trayIcon);
                trayIcon.displayMessage(title, body, TrayIcon.MessageType.INFO);

                new Thread(() -> {
                    try { Thread.sleep(10000); } catch (Exception ignored) {}
                    tray.remove(trayIcon);
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void showMobileAlert(String titleText, String bodyText) {
        Platform.runLater(() -> {
            Stage alertStage = new Stage();
            alertStage.initStyle(StageStyle.UNDECORATED);
            alertStage.setAlwaysOnTop(true);

            VBox layout = new VBox(15);
            layout.setAlignment(Pos.CENTER);
            layout.setStyle("-fx-background-color: #2c3e50; -fx-padding: 25; " +
                    "-fx-border-color: #e74c3c; -fx-border-width: 3; -fx-background-radius: 10;");

            Label header = new Label("⚠️ " + titleText);
            header.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;");

            Label body = new Label(bodyText);
            body.setStyle("-fx-text-fill: white;");

            Button closeBtn = new Button("DISMISS");
            closeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            closeBtn.setOnAction(e -> alertStage.close());

            layout.getChildren().addAll(header, body, closeBtn);
            alertStage.setScene(new Scene(layout));
            alertStage.show();

            java.awt.Toolkit.getDefaultToolkit().beep();
        });
    }
}