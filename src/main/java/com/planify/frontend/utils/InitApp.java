package com.planify.frontend.utils;

import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.controllers.websocket.WebSocketController;
import com.planify.frontend.models.auth.DashboardSummary;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import com.planify.frontend.utils.data.personal.TaskDataManager;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.data.group.GroupDataManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.data.personal.EventDataManager;
import com.planify.frontend.utils.managers.ReminderManager;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.services.NotificationService;

public class InitApp {

    public static void init(){
        System.out.println("inini");
        GroupProjectDataManager.init();
        NotificationManager.init();
    }

    public static void initialize(){
        NotificationController.updateStatus(false);
        WebSocketController webSocketController = new WebSocketController();
        webSocketController.start();
        ProjectDataManager.init();
        EventDataManager.init();
        ReminderManager.init();
        TaskDataManager.init();
        ReminderManager.init();
        NotificationService.startMonitoring();
        SceneManager.switchScene("dashboard-view.fxml","Dashboard");
    }

    public static void clearData(){
        EventDataManager.clearData();
        ProjectDataManager.clearData();
        TaskDataManager.clearData();
    }
}
