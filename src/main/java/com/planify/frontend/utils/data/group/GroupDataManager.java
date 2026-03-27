package com.planify.frontend.utils.data.group;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.planify.frontend.controllers.Request.GetRequestController;
import com.planify.frontend.controllers.group.GroupDetailsController;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.group.*;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.utils.InitApp;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.UserSession;
import com.planify.frontend.utils.managers.LocalDataManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupDataManager {
    private static final String DATA_PATH = System.getProperty("user.home") + "/.planify/group";
    private static final String FILE_NAME = DATA_PATH + "/groups.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static List<DetailedGroup>detailedGroups = new ArrayList<>();

    // --- Core JSON Helpers ---

    public static void init(){
        GetRequestController.getAllUserGroup((summaries)->{
            // Update UI with summaries
            Platform.runLater(() -> {
                System.out.println("Got details!!!");
                if (summaries==null) {
                    detailedGroups = new ArrayList<>();
                }else{
                    detailedGroups = summaries;
                    saveAll(detailedGroups);
                    GroupProjectDataManager.init();
                    GroupEventDataManager.init();
                    InitApp.init();
                    SceneManager.switchScene("dashboard-view.fxml","Dashboard");
                }

            });

        });
        System.out.println("GroupController initialized.");
    }

    private static List<DetailedGroup> loadAll() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<DetailedGroup>>() {}.getType();
            List<DetailedGroup> data = gson.fromJson(reader, listType);
            return data != null ? data : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static void saveAll(List<DetailedGroup> groups) {
        File directory = new File(DATA_PATH);
        if (!directory.exists()) directory.mkdirs();
        try (Writer writer = new FileWriter(FILE_NAME)) {
            gson.toJson(groups, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Creators ---

    //Group created by user
    public static void saveGroup(String name, String description, String groupType, String code, boolean allowCodeJoin, boolean onlyAdminCanPost) {
        List<GroupMember> members = new ArrayList<>();
        GroupMember self = new GroupMember(LocalDataManager.getUserName(),LocalDataManager.getUserEmail(), "Admin");
        members.add(self);
        DetailedGroup grp = new DetailedGroup("", name, description, groupType, code,
                (onlyAdminCanPost?"Admin":"All members"), "Admin", LocalDateTime.now().toString(), members,
                new ArrayList<>(), new ArrayList<>());
        detailedGroups.add(grp);
        saveAll(detailedGroups);
    }

    //Group created by other user
    public static void saveGroup(Object data, Object refresher){
        if (data instanceof DetailedGroup group) {
            detailedGroups.add(group);
            saveAll(detailedGroups);
            GroupProjectDataManager.refresh(refresher);
        }
    }

    //Save all groups got from backend
    public static void saveAllGroups(List<DetailedGroup> groups){
        saveAll(groups);
    }

    //saves newly created group project
    public static void saveNewGroupProject(ProjectDetails project, String groupUuid){
        for(DetailedGroup g: detailedGroups){
            if(g.getUuid().equals(groupUuid)){
                g.getProjects().add(project);
            }
        }
        saveAll(detailedGroups);
    }

    public static void saveNewGroupEvent(EventGetRequest event, String groupUuid){
        for(DetailedGroup g: detailedGroups){
            if(g.getUuid().equals(groupUuid)){
                g.getEvents().add(event);
            }
        }
        saveAll(detailedGroups);
    }

    //Add member to an existing group
    public static void saveNewGroupMember(Object data, String groupUuid, Object refresher){
        if (data instanceof GroupMember member) {
            for(DetailedGroup g: detailedGroups){
                if(g.getUuid().equals(groupUuid)){
                    g.getMembers().add(member);
                }
            }
            saveAll(detailedGroups);
            GroupProjectDataManager.refresh(refresher);
        }
    }

    // --- Detailed Getters ---

    /**
     * Gets the all ProjectDetails for a specific group
     */
    public static List<DetailedGroup> getAllGroups(){
        return detailedGroups;
    }

    //returns list of all projects associated with that particular group
    public static List<ProjectDetails> getGroupProjects(String groupName) {
        for(DetailedGroup g: detailedGroups){
            if(g.getName().equals(groupName)){
                return g.getProjects();
            }
        }
        return new ArrayList<>();
    }
    //returns all projects list
    public static List<ProjectDetails> getAllGroupProjects(){
        List<ProjectDetails> projectDetails = new ArrayList<>();
        for(DetailedGroup g: detailedGroups){
            projectDetails.addAll(g.getProjects());
        }
        return projectDetails;
    }

    public static List<EventGetRequest> getAllGroupEvents(){
        List<EventGetRequest> events = new ArrayList<>();
        for(DetailedGroup g: detailedGroups){
            events.addAll(g.getEvents());
        }
        return events;
    }

    /**
     * Gets all events for a group
     */
    public static List<EventGetRequest> getGroupEvents(String groupName) {
        for(DetailedGroup g: detailedGroups){
            if(g.getName().equals(groupName)){
                return g.getEvents();
            }
        }
        return new ArrayList<>();
    }

    /**
     * Gets all members of a group
     */
    public static List<GroupMember> getGroupMembers(String groupName) {
        for(DetailedGroup g: detailedGroups){
            if(g.getName().equals(groupName)){
                return g.getMembers();
            }
        }
        return new ArrayList<>();
    }

    //map to group Summary request
    public static List<GroupSummaryRequest> getGroupSummary() {
        //Todo: update the logic to get count of upcoming events and projects
        List<GroupSummaryRequest> groupSummaryRequests = new ArrayList<>();
        for (DetailedGroup g: detailedGroups){
            GroupSummaryRequest grp = new GroupSummaryRequest(g.getUuid(), g.getName(),
                    g.getDescription(), g.getMembers().size(), g.getGroupType(), g.getEvents().size(), g.getProjects().size(), g.getRole());
            groupSummaryRequests.add(grp);
        }
        return groupSummaryRequests;
    }

    //map to group details
    public static GroupDetails getGroupDetails(String uuid) {
        //Todo: update the logic to get count of upcoming events and projects
        for (DetailedGroup g: detailedGroups){
            if(g.getUuid().equals(uuid)){
                List<EventSummary>events = new ArrayList<>();
                for(EventGetRequest e: g.getEvents()){
                    EventSummary eventSummary = new EventSummary(e.getTitle(),
                            e.getDescription(), e.getType(), e.getStartDateTime(),
                            e.getEndDateTime());
                    events.add(eventSummary);
                }

                return new GroupDetails(g.getUuid(), g.getName(), g.getDescription(), g.getGroupType(),
                        g.getCode(), g.getPostingAceess(), g.getRole(), g.getCreatedAt(),
                        g.getMembers(), events, g.getProjects());
            }
        }
        return null;
    }


    // --- Updates ---

    public static void updateGroup(Object data, Object refresher){
        if(data instanceof DetailedGroup detailedGroup){
            Platform.runLater(() -> {
                for (int i = 0; i < detailedGroups.size(); i++) {
                    if (detailedGroups.get(i).getUuid().equals(detailedGroup.getUuid())) {
                        detailedGroups.set(i, detailedGroup);
                        System.out.println(detailedGroup.getMembers().size());
                        break;
                    }
                }
                GroupProjectDataManager.refresh(refresher);
            });
        }

    }

    //update a group project, may be a new milestone/task is added/updated, as done on group project, backend connection is required, so uuid is needed
    public static void updateGroupProject(String groupUuid, ProjectDetails project) {
        if(groupUuid.trim().isEmpty() || project.getUuid().trim().isEmpty())return;
        for(DetailedGroup g: detailedGroups){
            if(g.getUuid().equals(groupUuid)){
                List<ProjectDetails>projects = new ArrayList<>();
                for(ProjectDetails p: g.getProjects()){
                    if(p.getUuid().equals(project.getUuid())){
                        projects.add(project);
                    }else{
                        projects.add(p);
                    }
                }
                g.setProjects(projects);
            }
        }
        saveAll(detailedGroups);
    }

    //update an group event
    public static void updateGroupEvent(String groupUuid, EventGetRequest event) {
        if(groupUuid.trim().isEmpty() || event.getUuid().trim().isEmpty())return;
        for(DetailedGroup g: detailedGroups){
            if(g.getUuid().equals(groupUuid)){
                List<EventGetRequest>events = new ArrayList<>();
                for(EventGetRequest e: g.getEvents()){
                    if(e.getUuid().equals(event.getUuid())){
                        events.add(event);
                    }else{
                        events.add(e);
                    }
                }
                g.setEvents(events);
            }
        }
        saveAll(detailedGroups);
    }

    //update role of a member
    public static void updateGroupMemberRole(Object data, String groupUuid, Object refresher) {
        if (data instanceof GroupMember updatedMember) {
            for(DetailedGroup g: detailedGroups){
                if(g.getUuid().equals(groupUuid)){
                    List<GroupMember>members = new ArrayList<>();
                    for(GroupMember m: g.getMembers()){
                        if(m.getEmail().equals(updatedMember.getEmail())){
                            members.add(updatedMember);
                        }else{
                            members.add(m);
                        }
                    }
                    g.setMembers(members);
                }
            }
            GroupProjectDataManager.refresh(refresher);
            saveAll(detailedGroups);
        }
    }

    // --- Deletes ---

    public static void deleteGroupProject(String groupUuid, String projectUuid) {
        if(groupUuid.trim().isEmpty() || projectUuid.trim().isEmpty())return;
        for(DetailedGroup g: detailedGroups){
            if(g.getUuid().equals(groupUuid)){
                List<ProjectDetails>projects = new ArrayList<>();
                for(ProjectDetails p: g.getProjects()){
                    if(!p.getUuid().equals(projectUuid)){
                        projects.add(p);
                    }
                }
                g.setProjects(projects);
            }
        }
        saveAll(detailedGroups);
    }

    public static void deleteGroupEvent (String eventUuid) {
        for(DetailedGroup g: detailedGroups){
            List<EventGetRequest>events = new ArrayList<>();
            for(EventGetRequest e: g.getEvents()){
                if(!e.getUuid().equals(eventUuid)){
                    events.add(e);
                }
            }
            g.setEvents(events);

        }

        saveAll(detailedGroups);
    }

    public static void handleLeaveMember(String groupUuid, Object data, Object refresher) {
        if(groupUuid.trim().isEmpty())return;
        if (data instanceof GroupMember groupMember) {
            if(groupMember.getEmail().equals(UserSession.getInstance().getEmail())){
                handleDeleteGroup(groupUuid);
                if(refresher instanceof GroupDetailsController){
                    SceneManager.switchScene("group-view","Groups");
                }
            }
            else {
                for(DetailedGroup g: detailedGroups){
                    if(g.getUuid().equals(groupUuid)){
                        List<GroupMember>members = new ArrayList<>();
                        for(GroupMember m: g.getMembers()){
                            if(!m.getEmail().equals(groupMember.getEmail())){
                                members.add(m);
                            }
                        }
                        g.setMembers(members);
                    }
                }
                saveAll(detailedGroups);
                GroupProjectDataManager.refresh(refresher);
            }

        }
    }

    public static void handleDeleteGroup(String groupUuid){
        if(groupUuid.trim().isEmpty())return;
        for(DetailedGroup g: detailedGroups){
            if(g.getUuid().equals(groupUuid)){
                detailedGroups.remove(g);
            }
        }
        saveAll(detailedGroups);
    }

    private static void showErrorAlert(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText("Got into this error:");
        alert.setContentText(error);
        alert.showAndWait();
    }

    private static void showSuccessAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
