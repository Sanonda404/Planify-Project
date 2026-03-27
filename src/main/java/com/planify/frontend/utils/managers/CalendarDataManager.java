package com.planify.frontend.utils.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.utils.data.group.GroupEventDataManager;
import com.planify.frontend.utils.data.personal.EventDataManager;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CalendarDataManager {
    private static final String DATA_PATH = System.getProperty("user.home") + "/.planify/calendar";
    private static final String FILE_NAME = DATA_PATH + "/display_events.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void rebuildMergedCalendar() {
        List<EventGetRequest> mergedEvents = new ArrayList<>();

        // 1. Add all personal events
        mergedEvents.addAll(EventDataManager.getAll());

        // 2. Add group events that have the merge flag set to true
        for (EventGetRequest gEvent : GroupEventDataManager.getAll()) {
            if (gEvent.isMergeWithPersonal()) {
                mergedEvents.add(gEvent);
            }
        }

        saveToPersonalCalendar(mergedEvents);
    }

    private static void saveToPersonalCalendar(List<EventGetRequest> events) {
        try {
            File dir = new File(DATA_PATH);
            if (!dir.exists()) dir.mkdirs();
            try (Writer writer = new FileWriter(FILE_NAME)) {
                gson.toJson(events, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save merged calendar: " + e.getMessage());
        }
    }

    public static List<EventGetRequest> getCalendarEvents() {
        return loadCalendar();
    }

    private static List<EventGetRequest> loadCalendar() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, new com.google.gson.reflect.TypeToken<List<EventGetRequest>>(){}.getType());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
    public static List<EventGetRequest> getMergedEvents() {
        List<EventGetRequest> mergedList = new ArrayList<>();

        // 1. Add Personal Events
        mergedList.addAll(EventDataManager.getAll());

        // 2. Add Group Events (Filtering by merge flag)
        List<EventGetRequest> groupEvents = GroupEventDataManager.getAll().stream()
                .filter(EventGetRequest::isMergeWithPersonal)
                .collect(Collectors.toList());

        mergedList.addAll(groupEvents);

        saveToCalendarFile(mergedList);
        return mergedList;
    }

    private static void saveToCalendarFile(List<EventGetRequest> list) {
        try {
            File dir = new File(DATA_PATH);
            if (!dir.exists()) dir.mkdirs();
            try (Writer writer = new FileWriter(FILE_NAME)) {
                gson.toJson(list, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}