package com.planify.frontend.utils.services;

import com.google.gson.Gson;
import com.planify.frontend.models.notification.ReminderTask;
import com.planify.frontend.utils.managers.ReminderManager;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

public class ReminderBackgroundService {
    private static final Gson gson = new Gson();
    private static boolean running = true;

    public static void start() {
        Thread serviceThread = new Thread(() -> {
            while (running) {
                try {
                    checkAndNotify();
                    // Poll every 1 minute to save battery/CPU
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        serviceThread.setDaemon(true); // Ensures it doesn't block app shutdown
        serviceThread.start();
    }

    private static void checkAndNotify() {
        List<ReminderTask> Entrys = ReminderManager.loadAll(); // Make loadEntrys public in ReminderManager
        if (Entrys.isEmpty()) return;

        boolean changed = false;
        LocalDateTime now = LocalDateTime.now();
        Iterator<ReminderTask> iterator = Entrys.iterator();

        while (iterator.hasNext()) {
            ReminderTask Entry = iterator.next();
            LocalDateTime trigger = LocalDateTime.parse(Entry.getTriggerAt());

            // If it's time (or we missed it slightly)
            if (now.isAfter(trigger)) {
                showSystemNotification(Entry.getTitle(), "Reminder for your " + Entry.getType());
                iterator.remove(); // Don't notify again
                changed = true;
            }
        }

        if (changed) {
            ReminderManager.saveReminders(); // Save the cleaned list
        }
    }

    private static void showSystemNotification(String title, String message) {
        if (!SystemTray.isSupported()) return;

        try {
            SystemTray tray = SystemTray.getSystemTray();
            // You need a small icon file in your resources
            Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
            TrayIcon trayIcon = new TrayIcon(image, "Planify");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);

            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);

            // Remove the icon after a few seconds so they don't stack up
            new Thread(() -> {
                try { Thread.sleep(5000); } catch (Exception ignored) {}
                tray.remove(trayIcon);
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}