package com.planify.frontend.controllers.Request;

import com.google.gson.Gson;
import com.planify.frontend.controllers.events.EventDetailController;
import com.planify.frontend.controllers.project.ProjectDetailsController;
import com.planify.frontend.utils.services.ApiService;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.json.JSONObject;

public class DeleteRequestController {

    private static final Gson gson = new Gson();


    public static void deleteTask(String taskUuid,  Object refresher) {
        // 1. Create a background task
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String endpoint = "/tasks/delete/" + taskUuid;
                ApiService.delete(endpoint);
                return null;
            }
        };

        // 2. Handle Success
        task.setOnSucceeded(e -> {
            System.out.println("Successfully deleted task");
            // Optional: Trigger a UI refresh or show a notification
            if(refresher instanceof ProjectDetailsController){
                ((ProjectDetailsController)refresher).refresh();
            }
        });

        // 3. Handle Failure
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            // Use Platform.runLater to show an alert if not already on FX thread
            JSONObject errorJson = new JSONObject(error.getMessage());
            String msg = errorJson.getString("error");
        });

        // 4. Run it on a background thread
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Ensures the thread closes if the app exits
        thread.start();
    }

    public static void deleteMilestone(String milestoneUuid, String email, Object refresher) {
        // 1. Create a background task
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String endpoint = "/milestones/delete/" + milestoneUuid + "?email=" + email;
                ApiService.delete(endpoint);
                return null;
            }
        };

        // 2. Handle Success
        task.setOnSucceeded(e -> {
            System.out.println("Successfully deleted Milestone");
            // Optional: Trigger a UI refresh or show a notification
            if(refresher instanceof ProjectDetailsController){
                ((ProjectDetailsController)refresher).refresh();
            }
        });

        // 3. Handle Failure
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            // Use Platform.runLater to show an alert if not already on FX thread
            JSONObject errorJson = new JSONObject(error.getMessage());
            String msg = errorJson.getString("error");
            System.out.println(msg);
        });

        // 4. Run it on a background thread
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Ensures the thread closes if the app exits
        thread.start();
    }

    public static void deleteEvent(String eventUuid, Object refresher) {
        System.out.println(eventUuid);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String endpoint = "/events/delete/" + eventUuid;
                ApiService.delete(endpoint);
                return null;
            }
        };

        // 2. Handle Success
        task.setOnSucceeded(e -> {
            System.out.println("Successfully deleted Milestone");
        });

        // 3. Handle Failure
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            // Use Platform.runLater to show an alert if not already on FX thread
            JSONObject errorJson = new JSONObject(error.getMessage());
            String msg = errorJson.getString("error");
            System.out.println(msg);
        });

        // 4. Run it on a background thread
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Ensures the thread closes if the app exits
        thread.start();
    }

    public static void deleteProject(String projectUuid, Object refresher) {
        // 1. Create a background task
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String endpoint = "/projects/delete/" + projectUuid;
                ApiService.delete(endpoint);
                return null;
            }
        };

        // 2. Handle Success
        task.setOnSucceeded(e -> {

        });

        // 3. Handle Failure
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            // Use Platform.runLater to show an alert if not already on FX thread
            JSONObject errorJson = new JSONObject(error.getMessage());
            String msg = errorJson.getString("error");
            System.out.println(msg);
        });

        // 4. Run it on a background thread
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Ensures the thread closes if the app exits
        thread.start();
    }
}