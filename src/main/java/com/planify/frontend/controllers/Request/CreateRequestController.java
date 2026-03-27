package com.planify.frontend.controllers.Request;

import com.google.gson.reflect.TypeToken;
import com.planify.frontend.controllers.auth.AuthController;
import com.planify.frontend.controllers.events.SchedulesController;
import com.planify.frontend.controllers.project.ProjectController;
import com.planify.frontend.controllers.project.ProjectDetailsController;
import com.planify.frontend.controllers.task.TodoController;
import com.planify.frontend.models.auth.LoginRequest;
import com.planify.frontend.models.auth.LoginResponse;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.auth.SignupDetails;
import com.planify.frontend.models.events.EventCreateRequest;
import com.planify.frontend.models.group.GroupCreateRequest;
import com.planify.frontend.models.group.GroupJoinRequest;
import com.planify.frontend.models.milestone.MilestoneCreateRequest;
import com.planify.frontend.models.project.ProjectCreateRequest;
import com.planify.frontend.models.resources.ResourceCreateRequest;
import com.planify.frontend.models.tasks.TaskRequest;
import com.planify.frontend.utils.services.ApiService;
import com.google.gson.Gson;
import com.planify.frontend.utils.UserSession;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.data.group.GroupDataManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.lang.reflect.Type;
import java.util.List;

public class CreateRequestController {
    private final static Gson gson = new Gson();

    public static void handleSignUp(SignupDetails req, Object refresher){
        sendRequest("/auth/register",req, refresher);
    }

    public static void handleLogin(LoginRequest req, Object refresher){
        sendRequest("/auth/login",req, refresher);
    }

    public static void handleCreateGroup(GroupCreateRequest grp, Object refresher) {
        sendRequest("/groups/create", grp, refresher);
    }

    public static void handleJoinGroup(GroupJoinRequest joinRequest, Object refresher){
        sendRequest("/groups/join", joinRequest, refresher);
    }

    public static void handleAcceptJoinRequest(String grpUuid, MemberInfo memberInfo, Object refresher){
        sendRequest("/groups/"+grpUuid+"/join/accept", memberInfo, refresher);
    }

    public static void handleAcceptInvitation(String grpUuid, Object refesher){
        sendRequest("/groups/"+grpUuid+"/accept",grpUuid,refesher);
    }

    public static void handleAddMember(String grpUuid, List<String>emails, Object refresher){
        sendRequest("/groups/"+grpUuid+"/invite", emails, refresher);
    }

    public static void handlePromoteMember(String grpUuid, MemberInfo memberInfo, Object refresher){
        sendRequest("/groups/"+grpUuid+"/member/promote",memberInfo, refresher);
    }

    public static void handleRemoveMember(String grpUuid, MemberInfo memberInfo, Object refresher){
        sendRequest("/groups/"+grpUuid+"/member/remove",memberInfo, refresher);
    }

    public static void handleLeaveGroup(String grpUuid, Object refresher){
        sendRequest("/groups/"+grpUuid+"/member/leave",null, refresher);
    }

    public static void handleCreateProject(ProjectCreateRequest projectCreateRequest, Object refresher) {
        sendRequest("/projects/create",projectCreateRequest,refresher);
    }

    public static void handleCreateTask(TaskRequest task, Object refresher) {
        sendRequest("/tasks/create",task, refresher);
    }

    public static void handleCreateMilestone(MilestoneCreateRequest milestone, Object refresher){
        sendRequest("/milestones/create", milestone, refresher);
    }

    public static void handleCreateEvent(EventCreateRequest event, Object refresher){
        sendRequest("/events/create", event, refresher);
    }

    public static void handleCreateResource(ResourceCreateRequest request, Object parentController) {
        sendRequest("/resources/create", request, parentController);
    }


    // Helper method to keep code clean
    private static void sendRequest(String endpoint, Object data, Object refresher) {
        String jsonBody = gson.toJson(data);
        new Thread(() -> {
            try {
                System.out.println("Sending to " + endpoint + ": " + jsonBody);
                String response;
                if(refresher instanceof AuthController)response = ApiService.postLogin(endpoint,jsonBody);
                else response = ApiService.post(endpoint, jsonBody);
                Platform.runLater(() -> {
                    if(refresher instanceof AuthController && data instanceof SignupDetails){
                        ((AuthController)refresher).signUpSuccessful();
                        Type listType = new TypeToken<LoginResponse>(){}.getType();
                        LoginResponse res = gson.fromJson(response, listType);
                        System.out.println(res.getEmail());
                        LocalDataManager.saveUserDataLocally(res.getName(), res.getEmail(), res.getToken());
                        UserSession.init(res.getName(),res.getEmail(),res.getToken());
                        ((AuthController)refresher).loginSuccessful();
                        GroupDataManager.init();
                    }
                    if(refresher instanceof AuthController && data instanceof LoginRequest){
                        System.out.println("hiii");
                        Type listType = new TypeToken<LoginResponse>(){}.getType();
                        LoginResponse res = gson.fromJson(response, listType);
                        System.out.println(res.getEmail());
                        LocalDataManager.saveUserDataLocally(res.getName(), res.getEmail(), res.getToken());
                        UserSession.init(res.getName(),res.getEmail(),res.getToken());
                        ((AuthController)refresher).loginSuccessful();
                        GroupDataManager.init();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                   System.out.println(e.getMessage());
                }
                );
            }
        }).start();
    }


    private static void showErrorAlert(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText("Got into this error:");
        alert.setContentText(error);
        alert.showAndWait();
    }

}