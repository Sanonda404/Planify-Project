package com.planify.frontend.utils.data.group;

import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.utils.managers.ReminderManager;
import com.planify.frontend.utils.data.personal.EventDataManager;
import com.planify.frontend.utils.helpers.AlertCreator;

import java.util.ArrayList;
import java.util.List;

public class GroupEventDataManager {
    private static List<EventGetRequest> groupEvents = new ArrayList<>();

    // --- Core JSON Helpers ---

    public static void init() {
        groupEvents = GroupDataManager.getAllGroupEvents();
    }

    public static void saveOrUpdateGroupEvent(EventGetRequest event, Object refresher) {
        boolean found = false;
        for (int i = 0; i < groupEvents.size(); i++) {
            if (groupEvents.get(i).getUuid().equals(event.getUuid())) {
                groupEvents.set(i, event);
                found = true;
                break;
            }
        }
        if (!found) {
            groupEvents.add(event);
            GroupDataManager.saveNewGroupEvent(event,event.getGroup().getEmail());
        }

        if (event.isMergeWithPersonal() && EventDataManager.isConflictingGlobally(event)) {
            AlertCreator.showErrorAlert("Group Event Conflict",
                    "The new group event '" + event.getTitle() + "' conflicts with your schedule.");
        }

        if(found){
            GroupDataManager.updateGroupEvent(event.getGroup().getEmail(),event);
            ReminderManager.updateReminder(event.getUuid(),event.getEndDateTime());
        }
        if (refresher != null) GroupProjectDataManager.refresh(refresher);
    }

    public static void deleteGroupEvent(String uuid, Object refresher) {
        groupEvents.removeIf(e -> e.getUuid().equals(uuid));
        GroupDataManager.deleteGroupEvent(uuid);
        ReminderManager.deleteReminder(uuid);
        if (refresher != null) GroupProjectDataManager.refresh(refresher);
    }

    public static List<EventGetRequest> getAll() { return groupEvents; }
}
