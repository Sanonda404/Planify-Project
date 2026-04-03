package com.planify.frontend.utils.managers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.planify.frontend.controllers.Request.GetRequestController;
import com.planify.frontend.controllers.project.ProjectController;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.group.DetailedGroup;
import com.planify.frontend.models.group.GroupDetails;
import com.planify.frontend.models.group.GroupMember;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.resources.ResourceDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.InitApp;
import com.planify.frontend.utils.data.group.GroupDataManager;
import com.planify.frontend.utils.data.group.GroupEventDataManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class NotificationManager {
    private static final ObservableList<NotificationResponse> notifications =
            FXCollections.observableArrayList();
    private final static Gson gson = new Gson();
    private static Object parent;
    public static void init(){
        GetRequestController.getAllNotifications((notification)->{
            // Update UI with summaries
            Platform.runLater(() -> {
                System.out.println("Got notifications!");
                if (notification != null) {
                    notifications.addAll(notification);
                    System.out.println(notification.size());
                    System.out.println(parent.getClass().getName());
                    InitApp.initialize();
                }

            });
        });
    }

    public static void setParent(Object par){
        parent = par;
        if(parent instanceof ProjectController) System.out.println("mumu");
    }

    public static void addNotification(NotificationResponse notif){
        notifications.addFirst(notif);
        doTask(notif);
    }

    public static ObservableList<NotificationResponse> getNotifications() {
        return notifications;
    }

    private static void doTask(NotificationResponse notif){
        System.out.println("Notif type: " + notif.getType());
        System.out.println("Parent: "+parent.getClass().getName());
        System.out.println("uuid: "+notif.getTargetUuid());
        System.out.println(notif.getUpdatedData().getClass().getName());
        JsonElement dataJson = gson.toJsonTree(notif.getUpdatedData());
        switch (notif.getType().toUpperCase()){
            case "PROJECT_ADDED":
                ProjectDetails project = gson.fromJson(dataJson, ProjectDetails.class);
                GroupProjectDataManager.saveGroupProject(project,parent);
                break;
            case "TASK_ADDED":
                TaskDetails taskDetails = gson.fromJson(dataJson, TaskDetails.class);
                GroupProjectDataManager.saveGroupProjectTask(taskDetails,parent);
                break;
            case "MILESTONE_ADDED":
                MilestoneDetails milestoneDetails = gson.fromJson(dataJson, MilestoneDetails.class);
                GroupProjectDataManager.saveGroupProjectMilestone(milestoneDetails,parent);
                break;
            case "MILESTONE_UPDATED":
                milestoneDetails = gson.fromJson(dataJson, MilestoneDetails.class);
                GroupProjectDataManager.updateGroupProjectMilestone(milestoneDetails,parent);
                break;
            case "PROJECT_UPDATED":
                //todo: doooo
                break;
            case "TASK_UPDATED":
                taskDetails = gson.fromJson(dataJson, TaskDetails.class);
                GroupProjectDataManager.updateGroupProjectTask(taskDetails,parent);
                break;
            case "TASK_DELETED":
                taskDetails = gson.fromJson(dataJson, TaskDetails.class);
                GroupProjectDataManager.deleteGroupProjectTask(notif.getTargetUuid(),parent);
                break;
            case "TASK_COMPLETED":
                taskDetails = gson.fromJson(dataJson, TaskDetails.class);
                GroupProjectDataManager.updateGroupProjectTaskStatus(taskDetails,parent);
                break;
            case "MILESTONE_DELETED":
                GroupProjectDataManager.deleteGroupProjectMilestone(notif.getTargetUuid(),parent);
                break;
            case "PROJECT_DELETED":
                break;
            case "GROUP_ADDED":
                DetailedGroup groupDetails = gson.fromJson(dataJson, DetailedGroup.class);
                GroupDataManager.saveGroup(groupDetails,parent);
                break;
            case "MEMBER_JOINED":
                GroupMember member = gson.fromJson(dataJson, GroupMember.class);
                GroupDataManager.saveNewGroupMember(member,notif.getTargetUuid(),parent);
                break;
            case "MEMBER_LEFT":
                member = gson.fromJson(dataJson, GroupMember.class);
                GroupDataManager.handleLeaveMember(notif.getTargetUuid(),member,parent);
                break;
            case "MEMBER_ROLE_UPDATED":
                member = gson.fromJson(dataJson, GroupMember.class);
                GroupDataManager.updateGroupMemberRole(member,notif.getTargetUuid(),parent);
                break;
            case "MEMBER_KICKED":
                System.out.println("kickedd");
                member = gson.fromJson(dataJson, GroupMember.class);
                GroupDataManager.handleLeaveMember(notif.getTargetUuid(),member,parent);
                break;
            case "GROUP_EVENT_ADDED":
                EventGetRequest event = gson.fromJson(dataJson, EventGetRequest.class);
                GroupEventDataManager.saveOrUpdateGroupEvent(event, parent);
                break;
            case "GROUP_EVENT_UPDATED":
                event = gson.fromJson(dataJson, EventGetRequest.class);
                GroupEventDataManager.saveOrUpdateGroupEvent(event, parent);
                break;
            case "GROUP_EVENT_DELETED":
                GroupEventDataManager.deleteGroupEvent(notif.getTargetUuid(), parent);
                break;
            case "GROUP_EVENT_SCHEDULE_CONFLICT":
                break;
            case "RESOURCE_ADDED":
                ResourceDetails resourceDetails = gson.fromJson(dataJson,ResourceDetails.class);
                GroupProjectDataManager.saveGroupProjectResource(resourceDetails,parent);
                break;
            default:
                break;
        }
    }
}
