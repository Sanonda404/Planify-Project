package com.planify.frontend.utils.data.personal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.events.EventCreateRequest;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.utils.UserSession;
import com.planify.frontend.utils.helpers.TimingCalculationHelper;
import com.planify.frontend.utils.managers.CalendarDataManager;
import com.planify.frontend.utils.managers.ReminderManager;
import com.planify.frontend.utils.data.group.GroupEventDataManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.helpers.AlertCreator;
import javafx.application.Platform;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventDataManager {
    private static String DATA_PATH;
    private static String FILE_NAME;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static List<EventGetRequest> personalEvents = new ArrayList<>();

    // --- Core JSON Helpers ---

    public static void init(){
        DATA_PATH = System.getProperty("user.home") + "/.planify/"+UserSession.getInstance().getName()+"/personal/events";
        FILE_NAME  = DATA_PATH + "/events.json";
        personalEvents = loadAll();
    }

    public static void clearData(){
        personalEvents = new ArrayList<>();
    }

    private static List<EventGetRequest> loadAll() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<EventGetRequest>>() {}.getType();
            List<EventGetRequest> data = gson.fromJson(reader, listType);
            return data != null ? data : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static void saveAll() {
        File directory = new File(DATA_PATH);
        if (!directory.exists()) directory.mkdirs();
        try (Writer writer = new FileWriter(FILE_NAME)) {
            gson.toJson(personalEvents, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addEvent(EventCreateRequest request, Object refresher) {
        // Convert CreateRequest to GetRequest for storage
        String uuid = getUuid();
        MemberInfo self = new MemberInfo(UserSession.getInstance().getName(), UserSession.getInstance().getEmail());
        EventGetRequest newEvent = new EventGetRequest(
                uuid, request.getTitle(), request.getDescription(), request.getType(),
                request.getColor(), request.getStartDateTime(), request.getEndDateTime(),
                null, request.isMergeWithPersonal(), request.getRepeatPattern(),
                request.getExcludedDays(), request.getMonthlyDate(),
                request.getReminderMinutesBefore(), request.getReminderType().toUpperCase(),
                request.getAttachmentUrl(), self, true
        );

        // Conflict Check: Check both Personal AND Group events
        if (isConflictingGlobally(newEvent)) {
            AlertCreator.showErrorAlert("Schedule Conflict", "This slot overlaps with an existing event in your calendar.");
            return;
        }

        String triggerAt = TimingCalculationHelper.calculateTriggerTime(newEvent.getStartDateTime(), newEvent.getReminderMinutesBefore());

        ReminderManager.saveReminder(uuid, newEvent.getTitle(), triggerAt, newEvent.getReminderType()); // Update reminder file
        GroupProjectDataManager.refresh(refresher);
        personalEvents.add(newEvent);
        saveAll();
        CalendarDataManager.rebuildMergedCalendar();
    }

    private static String getUuid() {
        int num = personalEvents.isEmpty()
                ? 1
                : Integer.parseInt(personalEvents.getLast().getUuid().replace("PER", "")) + 1;
        return "PER" + num;
    }

    public static void updateEvent(EventGetRequest updatedEvent, Runnable refresher) {
        for (int i = 0; i < personalEvents.size(); i++) {
            if (personalEvents.get(i).getUuid().equals(updatedEvent.getUuid())) {
                // Check if the new time conflicts with OTHER events
                if (isConflictingGlobally(updatedEvent)) {
                    AlertCreator.showErrorAlert("Update Failed", "The new time overlaps with another event.");
                    return;
                }
                personalEvents.set(i, updatedEvent);
                saveAll();
                ReminderManager.updateReminder(updatedEvent.getUuid(),updatedEvent.getEndDateTime());
                if (refresher != null) Platform.runLater(refresher);
                return;
            }
        }
    }

    public static boolean isConflictingGlobally(EventGetRequest newEvent) {
        if (!"SLOT".equals(newEvent.getType())) return false;

        // Check against Personal
        boolean personalConflict = personalEvents.stream()
                .anyMatch(existing -> compareEvents(newEvent, existing));

        // Check against Group (only if merged)
        boolean groupConflict = GroupEventDataManager.getAll().stream()
                .filter(EventGetRequest::isMergeWithPersonal)
                .anyMatch(existing -> compareEvents(newEvent, existing));

        return personalConflict || groupConflict;
    }

    private static boolean compareEvents(EventGetRequest e1, EventGetRequest e2) {
        if (e1.getUuid().equals(e2.getUuid())) return false;
        if (!"SLOT".equals(e2.getType())) return false;

        LocalDateTime s1 = LocalDateTime.parse(e1.getStartDateTime());
        LocalDateTime end1 = LocalDateTime.parse(e1.getEndDateTime());
        LocalDateTime s2 = LocalDateTime.parse(e2.getStartDateTime());
        LocalDateTime end2 = LocalDateTime.parse(e2.getEndDateTime());

        return s1.isBefore(end2) && end1.isAfter(s2);
    }


    public static void deleteEvent(String title) {
        personalEvents.removeIf(e -> e.getTitle().equals(title));
        saveAll();
        CalendarDataManager.rebuildMergedCalendar();
    }

    public static List<EventGetRequest> getAll() { return personalEvents; }
}
