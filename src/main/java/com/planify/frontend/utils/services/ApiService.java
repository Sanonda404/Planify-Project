package com.planify.frontend.utils.services;

import com.planify.frontend.utils.UserSession;
import com.planify.frontend.utils.helpers.AlertCreator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class ApiService {
    public static final String BASE_URL = "http://localhost:8000/api";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static String post(String endpoint, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Authorization", "Bearer " + UserSession.getInstance().getToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("code: "+response.statusCode());
        System.out.println(response.body());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            AlertCreator.showSuccessAlert("Success");
            return response.body();
        } else {
            System.out.println("erorrrrr");
            AlertCreator.showErrorAlert(response.body());
            return "";
        }
    }

    public static String postLogin(String endpoint, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("code: "+response.statusCode());
        System.out.println(response.body());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            AlertCreator.showErrorAlert(response.body());
            return "";
        }
    }


    public static String get(String endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + UserSession.getInstance().getToken())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("GET code: " + response.statusCode());

        System.out.println(response.body());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            AlertCreator.showErrorAlert(response.body());
            return "";
        }
    }

    public static String patch(String endpoint, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + UserSession.getInstance().getToken())
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            AlertCreator.showErrorAlert(response.body());
            return "";
        }
    }

    public static String delete(String endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json") // Added for consistency
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + UserSession.getInstance().getToken())
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Delete code: "+response.statusCode());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            AlertCreator.showSuccessAlert("Successfully deleted");
            return response.body();
        } else {
            AlertCreator.showErrorAlert(response.body());
            return "";
        }
    }

}