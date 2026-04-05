package com.planify.frontend.utils.managers;

import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.utils.UserSession;

import java.io.*;
import java.nio.file.*;

public class LocalDataManager {

    private static String DATA_PATH;

    public static void initDataPathAndSave(String username, String email, String hashedPassword){
        DATA_PATH = System.getProperty("user.home") + "/.planify/" + UserSession.getInstance().getEmail()+ "/user_config.txt";
        saveUserDataLocally(username,email,hashedPassword);
    }

    public static void initDataPathForOffline(String email){
        DATA_PATH = System.getProperty("user.home") + "/.planify/" + email+ "/user_config.txt";
    }

    public static void saveUserDataLocally(String username, String email, String hashedPassword) {
        try {
            File file = new File(DATA_PATH);
            file.getParentFile().mkdirs(); // Create directory if it doesn't exist

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("Username:" + username + "\n");
                writer.write("Email:" + email + "\n");
                writer.write("Key:" + hashedPassword + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String getUserEmail() {
        if (!isUserSavedLocally()) return "";

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Email:")) {
                    String email = line.substring("Email:".length()).trim();
                    return email;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getUserName() {
        if (!isUserSavedLocally()) return "";

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Username:")) {
                    System.out.println(line.substring("Username:".length()).trim());
                    return line.substring("Username:".length()).trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getKey() {
        if (!isUserSavedLocally()) return "";

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Key:")) {
                    System.out.println(line.substring("Key:".length()).trim());
                    return line.substring("Key:".length()).trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isUserSavedLocally() {
        return Files.exists(Paths.get(DATA_PATH));
    }

    public static void saveNotification(NotificationResponse notification) {

    }
}