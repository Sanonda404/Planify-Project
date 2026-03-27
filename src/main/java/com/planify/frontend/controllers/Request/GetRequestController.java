package com.planify.frontend.controllers.Request;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.planify.frontend.models.auth.DashboardSummary;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.group.DetailedGroup;
import com.planify.frontend.models.group.GroupSummaryRequest;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.project.MilestoneSummary;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.project.ProjectSummary;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.services.ApiService;
import com.google.gson.Gson;
import com.planify.frontend.utils.LocalDateTimeAdapter;
import com.planify.frontend.utils.MemberInfoAdapter;
import javafx.application.Platform;
import javafx.event.ActionEvent;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GetRequestController {
    private static final Gson gson = new GsonBuilder().
            registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).registerTypeAdapter(MemberInfo.class, new MemberInfoAdapter())
    .create();
        /*GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();*/

    public static void getUserDashboardSummaries(Consumer<DashboardSummary> callback) {

        new Thread(() -> {
            try {
                // 2. Encode the email in case it has special characters like '+'
                String endpoint = "/dashboard/summary";

                // 3. Make the API call
                String json = ApiService.get(endpoint);

                // 4. Parse JSON
                Type listType = new TypeToken<DashboardSummary>(){}.getType();
                DashboardSummary summaries = gson.fromJson(json, listType);

                // 5. Send the result back to the UI thread
                Platform.runLater(() -> {
                    callback.accept(summaries);
                });

            } catch (Exception e) {

                Platform.runLater(() -> {
                    System.err.println("Error loading dashboard: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(null);
                });
            }
        }).start();
    }


    public static void getUserGroupSummaries(Consumer<List<GroupSummaryRequest>> callback) {

        new Thread(() -> {
            try {
                // 2. Encode the email in case it has special characters like '+'
                String endpoint = "/groups/summary";

                // 3. Make the API call
                String json = ApiService.get(endpoint);

                // 4. Parse JSON
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<GroupSummaryRequest>>(){}.getType();
                List<GroupSummaryRequest> summaries = gson.fromJson(json, listType);

                // 5. Send the result back to the UI thread
                Platform.runLater(() -> {
                    callback.accept(summaries);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading dashboard: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(new ArrayList<>());
                });
            }
        }).start();
    }

    public static void getAllUserGroup(Consumer<List<DetailedGroup>> callback) {

        new Thread(() -> {
            try {
                // 2. Encode the email in case it has special characters like '+'
                String endpoint = "/groups/details/all";

                // 3. Make the API call
                String json = ApiService.get(endpoint);

                // 4. Parse JSON
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<DetailedGroup>>(){}.getType();
                List<DetailedGroup> summaries = gson.fromJson(json, listType);

                // 5. Send the result back to the UI thread
                Platform.runLater(() -> {
                    callback.accept(summaries);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading Details: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(new ArrayList<>());
                });
            }
        }).start();
    }

    public static void getAllNotifications(Consumer<List<NotificationResponse>> callback){
        new Thread(() -> {
            try {
                // 2. Encode the email in case it has special characters like '+'
                String endpoint = "/notifications/all";

                // 3. Make the API call
                String json = ApiService.get(endpoint);

                // 4. Parse JSON
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<NotificationResponse>>(){}.getType();
                List<NotificationResponse> summaries = gson.fromJson(json, listType);

                // 5. Send the result back to the UI thread
                Platform.runLater(() -> {
                    callback.accept(summaries);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading Details: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(new ArrayList<>());
                });
            }
        }).start();
    }

    public static void getUserProjectSummaries(String email, Consumer<List<ProjectSummary>> callback) {

        new Thread(() -> {
            try {
                // 2. Encode the email in case it has special characters like '+'
                String encodedEmail = java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
                String endpoint = "/projects/summary?email=" + encodedEmail;

                // 3. Make the API call
                String json = ApiService.get(endpoint);

                // 4. Parse JSON
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<ProjectSummary>>(){}.getType();
                List<ProjectSummary> summaries = gson.fromJson(json, listType);

                // 5. Send the result back to the UI thread
                Platform.runLater(() -> {
                    callback.accept(summaries);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(new ArrayList<>());
                });
            }
        }).start();
    }

    public static void getProjectMilestones(String email, String uuid, Consumer<List<MilestoneSummary>> callback) {

        new Thread(() -> {
            try {
                // 2. Encode the email in case it has special characters like '+'
                String encodedEmail = java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
                String endpoint = "/milestones/summary/" + uuid + "?email=" + encodedEmail;

                // 3. Make the API call
                String json = ApiService.get(endpoint);

                // 4. Parse JSON
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<MilestoneSummary>>(){}.getType();
                List<MilestoneSummary> summaries = gson.fromJson(json, listType);

                // 5. Send the result back to the UI thread
                Platform.runLater(() -> {
                    callback.accept(summaries);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading dashboard: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(new ArrayList<>());
                });
            }
        }).start();
    }

    public static void getGroupDetails(String uuid, Consumer<DetailedGroup> callback) {

        new Thread(() -> {
            try {
                String url = "/groups/details/" + uuid;
                String json = ApiService.get(url);

                DetailedGroup details = gson.fromJson(json, DetailedGroup.class);

                Platform.runLater(() -> {
                    callback.accept(details);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading dashboard: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(null);
                });
            }
        }).start();
    }

    public static void getProjectDetails(String uuid, Consumer<ProjectDetails> callback) {

        new Thread(() -> {
            try {
                String url = "/projects/details/" + uuid;
                String json = ApiService.get(url);

                ProjectDetails details = gson.fromJson(json, ProjectDetails.class);

                Platform.runLater(() -> {
                    callback.accept(details);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading Project Details: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(null);
                });
            }
        }).start();
    }

    public static void getTaskDetails(String uuid, Consumer<TaskDetails> callback) {

        new Thread(() -> {
            try {
                String url = "/tasks/details/" + uuid;
                String json = ApiService.get(url);

                TaskDetails details = gson.fromJson(json, TaskDetails.class);

                Platform.runLater(() -> {
                    callback.accept(details);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading Task Details: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(null);
                });
            }
        }).start();
    }

    public static void getMilestoneDetails(String uuid, Consumer<MilestoneDetails> callback) {

        new Thread(() -> {
            try {
                String url = "/milestones/details/" + uuid;
                String json = ApiService.get(url);

                MilestoneDetails details = gson.fromJson(json, MilestoneDetails.class);

                Platform.runLater(() -> {
                    callback.accept(details);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading Task Details: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(null);
                });
            }
        }).start();
    }

    public static void getAllTasks(String email, Consumer<List<TaskDetails>> callback){
        new Thread(() -> {
            try {
                // 2. Encode the email in case it has special characters like '+'
                String encodedEmail = java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
                String endpoint = "/tasks/summary?email=" + encodedEmail;

                // 3. Make the API call
                String json = ApiService.get(endpoint);

                // 4. Parse JSON
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<TaskDetails>>(){}.getType();
                List<TaskDetails> summaries = gson.fromJson(json, listType);

                // 5. Send the result back to the UI thread
                Platform.runLater(() -> {
                    callback.accept(summaries);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading dashboard: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(new ArrayList<>());
                });
            }
        }).start();
    }

    public static void getAllEvents(String email, Consumer<List<EventGetRequest>> callback){
        new Thread(() -> {
            try {
                // 2. Encode the email in case it has special characters like '+'
                String encodedEmail = java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
                String endpoint = "/events/summary?email=" + encodedEmail;

                // 3. Make the API call
                String json = ApiService.get(endpoint);

                // 4. Parse JSON
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<EventGetRequest>>(){}.getType();
                List<EventGetRequest> summaries = gson.fromJson(json, listType);

                // 5. Send the result back to the UI thread
                Platform.runLater(() -> {
                    callback.accept(summaries);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("Error loading Events: " + e.getMessage());
                    // Optionally pass an empty list so the UI doesn't crash
                    callback.accept(new ArrayList<>());
                });
            }
        }).start();
    }


    // Helper method to keep code clean
    private void sendRequest(String endpoint, Object data) {
        String jsonBody = gson.toJson(data);
        new Thread(() -> {
            try {
                System.out.println("Sending to " + endpoint + ": " + jsonBody);
                String response = ApiService.post(endpoint, jsonBody);
                Platform.runLater(() -> System.out.println("SUCCESS [" + endpoint + "]: " + response));
            } catch (Exception e) {
                Platform.runLater(() -> System.err.println("FAILED [" + endpoint + "]: " + e.getMessage()));
            }
        }).start();
    }

    public void handleJoinGroup(ActionEvent actionEvent) {
    }

    public void handleCreateMilestone(ActionEvent actionEvent) {
    }

    public void handleCreateEvent(ActionEvent actionEvent) {
    }

    // You can add the remaining handlers (Event, Milestone, etc.) following this pattern
}