package com.planify.frontend.utils.helpers;

import java.time.LocalDateTime;

public class TimingCalculationHelper {
    public static String calculateTriggerTime(String startDateTime, int minutesBefore) {
        // 1. Parse the ISO string into a LocalDateTime object
        LocalDateTime eventStart = LocalDateTime.parse(startDateTime);

        // 2. Subtract the minutes
        LocalDateTime triggerTime = eventStart.minusMinutes(minutesBefore);

        // 3. Convert back to String for your ReminderTask model
        return triggerTime.toString();
    }
}
