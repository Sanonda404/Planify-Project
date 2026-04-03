package com.planify.frontend.utils.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.notification.ReminderTask;
import com.planify.frontend.utils.UserSession;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReminderManager {
    private static String DATA_PATH;
    private static String FILE_NAME;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static List<ReminderTask> reminderList = new ArrayList<>();

    public static void init() {
        DATA_PATH = System.getProperty("user.home") + "/.planify/" + UserSession.getInstance().getName() + "/reminders";
        FILE_NAME = DATA_PATH + "/active_reminders.json";
        reminderList = loadAll();
        removeExpiredReminders();
    }

    /**
     * Update a single reminder's trigger time by UUID
     */
    public static void updateReminder(String uuid, String newTime) {
        for (ReminderTask reminderTask : reminderList) {
            if (reminderTask.getUuid().equals(uuid)) {
                reminderTask.setTriggerAt(newTime);
                break;
            }
        }
        saveReminders();
    }

    /**
     * Update a reminder's trigger time and message by UUID
     */
    public static void updateReminder(String uuid, String newTime, String newMessage) {
        for (ReminderTask reminderTask : reminderList) {
            if (reminderTask.getUuid().equals(uuid)) {
                reminderTask.setTriggerAt(newTime);
                reminderTask.setMessage(newMessage);
                break;
            }
        }
        saveReminders();
    }

    /**
     * Update multiple reminders at once (batch update)
     * @param updatedReminders List of updated reminder tasks
     */
    public static void updateReminders(List<ReminderTask> updatedReminders) {
        if (updatedReminders == null || updatedReminders.isEmpty()) {
            return;
        }

        // Create a map for quick lookup
        for (ReminderTask updatedTask : updatedReminders) {
            for (int i = 0; i < reminderList.size(); i++) {
                if (reminderList.get(i).getUuid().equals(updatedTask.getUuid())) {
                    reminderList.set(i, updatedTask);
                    break;
                }
            }
        }
        saveReminders();
    }

    /**
     * Replace the entire reminder list (used by NotificationService after firing reminders)
     * @param newReminders New list of reminders
     */
    public static void updateRemindersList(List<ReminderTask> newReminders) {
        if (newReminders == null) {
            reminderList = new ArrayList<>();
        } else {
            reminderList = new ArrayList<>(newReminders);
        }
        saveReminders();
    }

    /**
     * Remove expired reminders (trigger time passed)
     * @return Number of reminders removed
     */
    public static int removeExpiredReminders() {
        LocalDateTime now = LocalDateTime.now();
        int initialSize = reminderList.size();

        reminderList = reminderList.stream()
                .filter(task -> {
                    try {
                        LocalDateTime trigger = LocalDateTime.parse(task.getTriggerAt());
                        return trigger.isAfter(now);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        int removed = initialSize - reminderList.size();
        if (removed > 0) {
            saveReminders();
        }
        return removed;
    }

    /**
     * Get reminders that are due to fire (trigger time is now or in the past)
     */
    public static List<ReminderTask> getDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        return reminderList.stream()
                .filter(task -> {
                    try {
                        LocalDateTime trigger = LocalDateTime.parse(task.getTriggerAt());
                        return !trigger.isAfter(now);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Get reminders that will trigger in the next X minutes
     * @param minutes Minutes from now
     */
    public static List<ReminderTask> getUpcomingReminders(int minutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusMinutes(minutes);

        return reminderList.stream()
                .filter(task -> {
                    try {
                        LocalDateTime trigger = LocalDateTime.parse(task.getTriggerAt());
                        return trigger.isAfter(now) && !trigger.isAfter(future);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Check if a reminder exists for a specific event/task
     */
    public static boolean hasReminder(String uuid) {
        return reminderList.stream().anyMatch(task -> task.getUuid().equals(uuid));
    }

    /**
     * Get reminder by UUID
     */
    public static ReminderTask getReminder(String uuid) {
        return reminderList.stream()
                .filter(task -> task.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Clear all reminders (useful for testing or logout)
     */
    public static void clearAllReminders() {
        reminderList.clear();
        saveReminders();
    }

    /**
     * Delete multiple reminders by UUIDs
     * @param uuids List of UUIDs to delete
     */
    public static void deleteReminders(List<String> uuids) {
        reminderList.removeIf(task -> uuids.contains(task.getUuid()));
        saveReminders();
    }

    /**
     * Delete reminders for a specific event type or title pattern
     * @param titlePattern Pattern to match in title (case-insensitive)
     * @return Number of reminders deleted
     */
    public static int deleteRemindersByTitle(String titlePattern) {
        int initialSize = reminderList.size();
        reminderList.removeIf(task -> task.getTitle().toLowerCase().contains(titlePattern.toLowerCase()));
        int deleted = initialSize - reminderList.size();
        if (deleted > 0) {
            saveReminders();
        }
        return deleted;
    }

    public static void deleteReminder(String uuid) {
        reminderList.removeIf(reminderTask -> reminderTask.getUuid().equals(uuid));
        saveReminders();
    }

    public static void saveReminders() {
        try {
            File dir = new File(DATA_PATH);
            if (!dir.exists()) dir.mkdirs();
            try (Writer writer = new FileWriter(FILE_NAME)) {
                gson.toJson(reminderList, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveReminder(String uuid, String title, String triggerAt, String actualTime, String type) {
        if (type.equalsIgnoreCase("NONE")) return;
        // type: none, alert, notification
        ReminderTask task = new ReminderTask(uuid, title, triggerAt, actualTime, type);
        reminderList.add(task);
        saveReminders();
    }

    public static void saveReminder(ReminderTask task) {
        // Check if reminder already exists to avoid duplicates
        if (!hasReminder(task.getUuid())) {
            reminderList.add(task);
            saveReminders();
        }
    }

    public static List<ReminderTask> getAllReminders() {
        return new ArrayList<>(reminderList); // Return a copy to prevent external modification
    }

    public static List<ReminderTask> loadAll() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<ReminderTask>>() {}.getType();
            List<ReminderTask> data = gson.fromJson(reader, listType);
            return data != null ? data : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Print debug information about current reminders
     */
    public static void debugPrintReminders() {
        System.out.println("=== Current Reminders ===");
        System.out.println("Total: " + reminderList.size());
        for (ReminderTask task : reminderList) {
            System.out.println("  - [" + task.getType() + "] " + task.getTitle() + " @ " + task.getTriggerAt());
        }
        System.out.println("=========================");
    }
}