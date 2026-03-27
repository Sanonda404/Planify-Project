package com.planify.frontend.utils.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.notification.ReminderTask;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReminderManager {
    private static final String DATA_PATH = System.getProperty("user.home") + "/.planify/reminders";
    private static final String FILE_NAME = DATA_PATH + "/active_reminders.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static List<ReminderTask> reminderList = new ArrayList<>();

    public static void init(){
        reminderList = loadAll();
    }

    public static void updateReminder(String uuid, String newTime) {
       for(ReminderTask reminderTask: reminderList){
           if (reminderTask.getUuid().equals(uuid)){
               reminderTask.setTriggerAt(newTime);
           }
       }
       saveReminders();
    }

    public static void deleteReminder(String uuid){
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

    public static void saveReminder(String uuid, String title, String triggerAt, String type){
        if(type.equalsIgnoreCase("NONE"))return;
        //type: none, alert, notification
        ReminderTask task = new ReminderTask(
                uuid, title, triggerAt, type);
        reminderList.add(task);
        saveReminders();
    }


    public static List<ReminderTask>getAllReminders(){
        return reminderList;
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
}