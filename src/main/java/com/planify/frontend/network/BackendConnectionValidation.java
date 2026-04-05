package com.planify.frontend.network;

import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.planify.frontend.utils.services.ApiService.BASE_URL;

public class BackendConnectionValidation {
    private static final String SERVER_URL = "http://localhost:8000";
    public static boolean isNetworkAvailable() {
        try {
            // Try connecting to a reliable public address
            URL url = new URL("https://www.google.com");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(2000); // 2 seconds timeout
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isServerUp() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/check"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // We only care about the status code
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean canConnectToServer() {
        return isServerUp();
    }
}
