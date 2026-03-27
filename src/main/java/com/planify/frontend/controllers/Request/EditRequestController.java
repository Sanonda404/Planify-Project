package com.planify.frontend.controllers.Request;

import com.google.gson.Gson;
import com.planify.frontend.controllers.project.ProjectDetailsController;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.services.ApiService;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;

import java.util.List;

public class EditRequestController {
    private static final Gson gson = new Gson();


    public static void updateTaskStatus(String taskUuid, String status, Object refresher) {
        // 1. Create a background task
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String endpoint = "/tasks/" + taskUuid + "/update/status?status=" + status;
                ApiService.patch(endpoint, "");
                return null;
            }
        };

        // 2. Handle Success
        task.setOnSucceeded(e -> {
            System.out.println("Status updated successfully!");
            showSuccessAlert("Successfully Changed Status");
            // Optional: Trigger a UI refresh or show a notification
            if(refresher instanceof ProjectDetailsController){
                ((ProjectDetailsController)refresher).refresh();
            }
        });

        // 3. Handle Failure
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            // Use Platform.runLater to show an alert if not already on FX thread
            showErrorAlert("Failed to update status"+ error.getMessage());
        });

        // 4. Run it on a background thread
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Ensures the thread closes if the app exits
        thread.start();
    }


    public static void updateTask(String uuid, String email, TaskDetails data, Object refresher){
        // Run this in a Thread/Task!
        // 1. Create a background task
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String endpoint = "/tasks/update/" + uuid + "?email=" + email;
                String jsonBody = gson.toJson(data);
                ApiService.patch(endpoint, jsonBody);
                return null;
            }
        };

        // 2. Handle Success
        task.setOnSucceeded(e -> {
            System.out.println("Status updated successfully!");
            showSuccessAlert("Successfully Updated task");
            if(refresher instanceof ProjectDetailsController){
                ((ProjectDetailsController)refresher).refresh();
            }
            // Optional: Trigger a UI refresh or show a notification
        });

        // 3. Handle Failure
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            // Use Platform.runLater to show an alert if not already on FX thread
            showErrorAlert("Failed to update status"+ error.getMessage());
        });

        // 4. Run it on a background thread
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Ensures the thread closes if the app exits
        thread.start();

    }

    public static void updateMilestone(String uuid, String email, MilestoneDetails data, Object refresher){
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String endpoint = "/milestones/update/" + uuid + "?email=" + email;
                String jsonBody = gson.toJson(data);
                ApiService.patch(endpoint, jsonBody);
                return null;
            }
        };

        // 2. Handle Success
        task.setOnSucceeded(e -> {
            System.out.println("Status updated successfully!");
            showSuccessAlert("Successfully Updated Milestone");
            if(refresher instanceof ProjectDetailsController){
                ((ProjectDetailsController)refresher).refresh();
            }
            // Optional: Trigger a UI refresh or show a notification
        });

        // 3. Handle Failure
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            // Use Platform.runLater to show an alert if not already on FX thread
            showErrorAlert("Failed to update status"+ error.getMessage());
        });

        // 4. Run it on a background thread
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Ensures the thread closes if the app exits
        thread.start();

    }

    private static void showErrorAlert(List<String> errors) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText("Please fix the following errors:");
        alert.setContentText(String.join("\n• ", errors));
        alert.showAndWait();
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
