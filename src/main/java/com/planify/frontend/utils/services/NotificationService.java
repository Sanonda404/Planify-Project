package com.planify.frontend.utils.services;

import com.planify.frontend.models.notification.ReminderTask;
import com.planify.frontend.utils.managers.ReminderManager;
import com.planify.frontend.utils.managers.SoundManager;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NotificationService {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> currentTask;
    private static int currentInterval = 1; // Always check every second
    private static boolean isRunning = false;
    private static final Set<String> firedReminders = Collections.synchronizedSet(new HashSet<>());

    public static void startMonitoring() {
        if (isRunning) {
            System.out.println("Notification monitoring already running.");
            return;
        }

        isRunning = true;
        schedule(1); // Check every second
        System.out.println("Notification monitoring started. Checking every 1 second.");
    }

    public static void stopMonitoring() {
        if (currentTask != null) {
            currentTask.cancel(false);
            isRunning = false;
            System.out.println("Notification monitoring stopped.");
        }
    }

    private static void schedule(int seconds) {
        if (currentTask != null) {
            currentTask.cancel(false);
        }
        currentInterval = seconds;

        currentTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                checkReminders();
            } catch (Exception e) {
                System.err.println("Error checking reminders: " + e.getMessage());
            }
        }, 0, seconds, TimeUnit.SECONDS);

        System.out.println("Monitoring with " + seconds + "s heartbeat.");
    }

    public static void wakeUp() {
        // Force immediate check when new reminder is added
        if (isRunning) {
            scheduler.submit(() -> checkReminders());
        }
    }

    private static void checkReminders() {
        List<ReminderTask> activeTasks = ReminderManager.getAllReminders();
        if (activeTasks == null || activeTasks.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;

        // Filter reminders that need to be fired
        List<ReminderTask> toFire = activeTasks.stream()
                .filter(task -> {
                    try {
                        LocalDateTime trigger = LocalDateTime.parse(task.getTriggerAt());
                        String reminderKey = task.getUuid() + "_" + task.getTriggerAt();

                        // Check if trigger time has passed and not already fired
                        boolean shouldFire = now.isAfter(trigger) || now.isEqual(trigger);
                        boolean notFired = !firedReminders.contains(reminderKey);

                        return shouldFire && notFired;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        // Fire each reminder
        for (ReminderTask task : toFire) {
            try {
                String reminderKey = task.getUuid() + "_" + task.getTriggerAt();
                firedReminders.add(reminderKey);

                String timeInfo = getRelativeTimeDescription(task.getTriggerAt(), task.getActualTime());

                if ("ALERT".equalsIgnoreCase(task.getType())) {
                    // Show mobile-style alert
                    showMobileAlert(task.getTitle(), "Event starting soon", task.getTriggerAt(), task.getActualTime());
                } else {
                    // Show system tray notification
                    fireNativeNotification(task.getTitle(), "Event starting soon", task.getTriggerAt(), task.getActualTime());
                }

                System.out.println("Reminder fired: " + task.getTitle() + " at " + task.getTriggerAt());
                changed = true;
            } catch (Exception e) {
                System.err.println("Error firing reminder: " + e.getMessage());
            }
        }

        // Clean up fired reminders from the list
        if (changed) {
            // Remove reminders that have been fired
            List<ReminderTask> remainingTasks = activeTasks.stream()
                    .filter(task -> {
                        String reminderKey = task.getUuid() + "_" + task.getTriggerAt();
                        return !firedReminders.contains(reminderKey);
                    })
                    .collect(Collectors.toList());

            ReminderManager.updateReminders(remainingTasks);

            // Clean up fired reminders set periodically (keep last 100)
            if (firedReminders.size() > 100) {
                synchronized (firedReminders) {
                    firedReminders.clear();
                }
            }
        }
    }

    // Helper method to get relative time description
    private static String getRelativeTimeDescription(String triggerAt, String actualTime) {
        try {
            LocalDateTime trigger = LocalDateTime.parse(triggerAt);
            LocalDateTime actual = LocalDateTime.parse(actualTime);

            Duration duration = Duration.between(trigger, actual);
            long minutes = duration.toMinutes();
            long seconds = duration.getSeconds();

            if (seconds <= 0) {
                return "Starting now! ⏰";
            } else if (minutes < 1) {
                return "Starting in " + seconds + " second" + (seconds != 1 ? "s" : "") + " ⏳";
            } else if (minutes < 60) {
                return "Starting in " + minutes + " minute" + (minutes != 1 ? "s" : "") + " ⏳";
            } else if (minutes < 1440) {
                long hours = minutes / 60;
                long remainingMinutes = minutes % 60;
                if (remainingMinutes == 0) {
                    return "Starting in " + hours + " hour" + (hours != 1 ? "s" : "") + " ⏳";
                } else {
                    return "Starting in " + hours + "h " + remainingMinutes + "m ⏳";
                }
            } else {
                long days = minutes / 1440;
                return "Starting in " + days + " day" + (days != 1 ? "s" : "") + " 📅";
            }
        } catch (Exception e) {
            return "Upcoming event ⏰";
        }
    }

    // System Tray Notification
    private static void fireNativeNotification(String title, String body, String triggerAt, String actualTime) {
        if (!SystemTray.isSupported()) {
            // Fallback to mobile alert if system tray not supported
            showMobileAlert(title, body, triggerAt, actualTime);
            return;
        }

        Platform.runLater(() -> {
            try {
                SystemTray tray = SystemTray.getSystemTray();

                // Create tray icon
                java.awt.Image image = Toolkit.getDefaultToolkit().createImage(
                        NotificationService.class.getResource("/com/planify/frontend/images/icon.png")
                );
                if (image == null) {
                    // Create a simple colored icon
                    //image = Toolkit.getDefaultToolkit().createImage(16, 16);
                }

                SoundManager.playNotificationSound();

                TrayIcon trayIcon = new TrayIcon(image, "Planify Reminder");
                trayIcon.setImageAutoSize(true);

                // Add click listener
                trayIcon.addActionListener(e -> {
                    Platform.runLater(() -> {
                        System.out.println("Notification clicked: " + title);
                        // You can bring your app to front here
                    });
                });

                tray.add(trayIcon);

                String timeInfo = getRelativeTimeDescription(triggerAt, actualTime);
                String fullBody = body + "\n\n" + timeInfo;

                trayIcon.displayMessage(title, fullBody, TrayIcon.MessageType.INFO);

                // Auto-remove tray icon after 10 seconds
                new Thread(() -> {
                    try { Thread.sleep(10000); } catch (Exception ignored) {}
                    Platform.runLater(() -> tray.remove(trayIcon));
                }).start();

            } catch (Exception e) {
                System.err.println("Failed to show native notification: " + e.getMessage());
                // Fallback to mobile alert
                showMobileAlert(title, body, triggerAt, actualTime);
            }
        });
    }

    // Mobile/Desktop Alert with Glass Morphism Design
    private static void showMobileAlert(String titleText, String bodyText, String triggerAt, String actualTime) {
        Platform.runLater(() -> {
            Stage alertStage = new Stage();
            alertStage.initStyle(StageStyle.TRANSPARENT);
            alertStage.setAlwaysOnTop(true);

            // Play system beep
            SoundManager.playAlertSound();

            // Get relative time
            String timeInfo = getRelativeTimeDescription(triggerAt, actualTime);
            String displayBody = bodyText + "\n" + timeInfo;

            // Determine alert type color
            String borderColor = "#e74c3c"; // Default red for alerts
            String accentColor = "#e74c3c";

            if (bodyText.toLowerCase().contains("deadline")) {
                borderColor = "#e74c3c";
                accentColor = "#e74c3c";
            } else if (bodyText.toLowerCase().contains("event")) {
                borderColor = "#3b82f6";
                accentColor = "#3b82f6";
            } else if (bodyText.toLowerCase().contains("task")) {
                borderColor = "#10b981";
                accentColor = "#10b981";
            }

            // Main container
            VBox layout = new VBox(20);
            layout.setAlignment(Pos.CENTER);
            layout.setMaxWidth(400);
            layout.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);" +
                            "-fx-background-radius: 24;" +
                            "-fx-border-color: " + borderColor + ";" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 24;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 25, 0, 0, 8);" +
                            "-fx-padding: 25;"
            );

            // Icon container with pulse animation
            StackPane iconContainer = new StackPane();
            iconContainer.setStyle(
                    "-fx-background-color: " + accentColor + "20;" +
                            "-fx-background-radius: 50;" +
                            "-fx-padding: 15;"
            );

            String iconText = getIconForType(bodyText);
            Label iconLabel = new Label(iconText);
            iconLabel.setStyle(
                    "-fx-font-size: 40px;" +
                            "-fx-text-fill: " + accentColor + ";"
            );
            iconContainer.getChildren().add(iconLabel);

            // Pulse animation
            ScaleTransition pulse = new ScaleTransition(javafx.util.Duration.millis(500), iconContainer);
            pulse.setFromX(1);
            pulse.setFromY(1);
            pulse.setToX(1.1);
            pulse.setToY(1.1);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(3);
            pulse.play();

            // Header
            Label header = new Label(titleText);
            header.setWrapText(true);
            header.setMaxWidth(320);
            header.setAlignment(Pos.CENTER);
            header.setStyle(
                    "-fx-text-fill: " + accentColor + ";" +
                            "-fx-font-weight: 800;" +
                            "-fx-font-size: 18px;" +
                            "-fx-font-family: 'Segoe UI';"
            );

            // Body text
            Label body = new Label(displayBody);
            body.setWrapText(true);
            body.setMaxWidth(320);
            body.setAlignment(Pos.CENTER);
            body.setStyle(
                    "-fx-text-fill: #cbd5e1;" +
                            "-fx-font-size: 13px;" +
                            "-fx-font-family: 'Segoe UI';" +
                            "-fx-line-spacing: 4px;"
            );

            // Time info badge
            HBox timeBadge = new HBox(8);
            timeBadge.setAlignment(Pos.CENTER);
            timeBadge.setStyle(
                    "-fx-background-color: " + accentColor + "15;" +
                            "-fx-background-radius: 20;" +
                            "-fx-padding: 6 12;"
            );

            Label timeIcon = new Label("⏰");
            timeIcon.setStyle("-fx-font-size: 12px;");

            Label timeLabel = new Label(timeInfo);
            timeLabel.setStyle(
                    "-fx-text-fill: #f59e0b;" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-weight: 500;"
            );

            timeBadge.getChildren().addAll(timeIcon, timeLabel);

            // Button container
            HBox buttonContainer = new HBox(15);
            buttonContainer.setAlignment(Pos.CENTER);

            // Dismiss button
            Button dismissBtn = new Button("Dismiss");
            dismissBtn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: #94a3b8;" +
                            "-fx-font-weight: 600;" +
                            "-fx-font-size: 13px;" +
                            "-fx-padding: 8 20;" +
                            "-fx-background-radius: 25;" +
                            "-fx-border-color: #475569;" +
                            "-fx-border-width: 1.5;" +
                            "-fx-border-radius: 25;" +
                            "-fx-cursor: hand;"
            );
            dismissBtn.setOnMouseEntered(e -> dismissBtn.setStyle(
                    "-fx-background-color: #2d2d3d;" +
                            "-fx-text-fill: #cbd5e1;" +
                            "-fx-font-weight: 600;" +
                            "-fx-font-size: 13px;" +
                            "-fx-padding: 8 20;" +
                            "-fx-background-radius: 25;" +
                            "-fx-border-color: #64748b;" +
                            "-fx-border-width: 1.5;" +
                            "-fx-border-radius: 25;" +
                            "-fx-cursor: hand;"
            ));
            dismissBtn.setOnMouseExited(e -> dismissBtn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: #94a3b8;" +
                            "-fx-font-weight: 600;" +
                            "-fx-font-size: 13px;" +
                            "-fx-padding: 8 20;" +
                            "-fx-background-radius: 25;" +
                            "-fx-border-color: #475569;" +
                            "-fx-border-width: 1.5;" +
                            "-fx-border-radius: 25;" +
                            "-fx-cursor: hand;"
            ));
            dismissBtn.setOnAction(e -> alertStage.close());

            // Snooze button (only for non-deadline alerts)
            boolean isDeadline = bodyText.toLowerCase().contains("deadline");
            if (!isDeadline) {
                Button snoozeBtn = new Button("Snooze 5 min");
                final String accent = accentColor;
                snoozeBtn.setStyle(
                        "-fx-background-color: " + accent + ";" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: 700;" +
                                "-fx-font-size: 13px;" +
                                "-fx-padding: 8 20;" +
                                "-fx-background-radius: 25;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);"
                );
                snoozeBtn.setOnMouseEntered(e -> snoozeBtn.setStyle(
                        "-fx-background-color: " + accent + "dd;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: 700;" +
                                "-fx-font-size: 13px;" +
                                "-fx-padding: 8 20;" +
                                "-fx-background-radius: 25;" +
                                "-fx-cursor: hand;"
                ));
                snoozeBtn.setOnMouseExited(e -> snoozeBtn.setStyle(
                        "-fx-background-color: " + accent+ ";" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: 700;" +
                                "-fx-font-size: 13px;" +
                                "-fx-padding: 8 20;" +
                                "-fx-background-radius: 25;" +
                                "-fx-cursor: hand;"
                ));
                snoozeBtn.setOnAction(e -> {
                    alertStage.close();
                    snoozeReminder(titleText, bodyText, triggerAt, actualTime);
                    System.out.println("Snoozed: " + titleText);
                });
                buttonContainer.getChildren().add(snoozeBtn);
            }

            buttonContainer.getChildren().add(dismissBtn);

            layout.getChildren().addAll(iconContainer, header, body, timeBadge, buttonContainer);

            Scene scene = new Scene(layout);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

            // Fade in animation
            layout.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.millis(200), layout);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            alertStage.setScene(scene);
            alertStage.show();

            // Auto-close after 10 seconds
            PauseTransition autoClose = new PauseTransition(javafx.util.Duration.seconds(10));
            autoClose.setOnFinished(e -> {
                if (alertStage.isShowing()) {
                    FadeTransition fadeOut = new FadeTransition(javafx.util.Duration.millis(200), layout);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(ev -> alertStage.close());
                    fadeOut.play();
                }
            });
            autoClose.play();
        });
    }

    private static String getIconForType(String bodyText) {
        String lowerBody = bodyText.toLowerCase();
        if (lowerBody.contains("deadline")) return "⚠️";
        if (lowerBody.contains("event")) return "📅";
        if (lowerBody.contains("task")) return "✅";
        if (lowerBody.contains("meeting")) return "🤝";
        return "⏰";
    }

    private static void snoozeReminder(String title, String body, String triggerAt, String actualTime) {
        try {
            LocalDateTime originalTrigger = LocalDateTime.parse(triggerAt);
            LocalDateTime newTrigger = originalTrigger.plusMinutes(5);

            ReminderTask snoozedTask = new ReminderTask(
                    UUID.randomUUID().toString(),
                    title,
                    newTrigger.toString(),
                    actualTime,
                    "ALERT"
            );
            snoozedTask.setMessage(body);

            ReminderManager.saveReminder(snoozedTask);
            System.out.println("Reminder snoozed until: " + newTrigger);
        } catch (Exception e) {
            System.err.println("Failed to snooze reminder: " + e.getMessage());
        }
    }

    // Overloaded methods for backward compatibility
    private static void fireNativeNotification(String title, String body) {
        fireNativeNotification(title, body, null, null);
    }

    private static void showMobileAlert(String titleText, String bodyText) {
        showMobileAlert(titleText, bodyText, null, null);
    }

    public static void shutdown() {
        stopMonitoring();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}