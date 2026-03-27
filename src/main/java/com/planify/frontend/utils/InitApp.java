package com.planify.frontend.utils;

import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.controllers.websocket.WebSocketController;
import com.planify.frontend.models.auth.DashboardSummary;
import com.planify.frontend.utils.data.personal.TaskDataManager;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.data.group.GroupDataManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.data.personal.EventDataManager;
import com.planify.frontend.utils.managers.ReminderManager;

public class InitApp {

    public static void init(){
        GroupProjectDataManager.init();
        NotificationManager.init();
    }

    public static void initialize(){
        NotificationController.updateStatus(false);
        WebSocketController webSocketController = new WebSocketController();
        webSocketController.start();
        EventDataManager.init();
        ReminderManager.init();
        TaskDataManager.init();
    }
}
