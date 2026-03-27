package com.planify.frontend;

import com.planify.frontend.utils.managers.ReminderManager;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.services.NotificationService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;

import static com.planify.frontend.utils.managers.SceneManager.primaryStage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        ReminderManager.init();
        NotificationService.startMonitoring();

        SceneManager.setStage(stage);
        SceneManager.switchScene("login-view.fxml", "Login");

        Platform.setImplicitExit(false);

        // Initialize the System Tray icon and menu
        initTray(stage);

        stage.setOnCloseRequest(event -> {
            event.consume();
            stage.hide();
        });
    }

    private void initTray(Stage stage) {
        if (!SystemTray.isSupported()) return;

        try {
            SystemTray tray = SystemTray.getSystemTray();
            // Ensure icon.png is in your project root or use a full path
            java.awt.Image image = java.awt.Toolkit.getDefaultToolkit().getImage("icon.png");

            PopupMenu menu = new PopupMenu();

            // 1. "Open Planify" Menu Item
            MenuItem openItem = new MenuItem("Open Planify");
            openItem.addActionListener(e -> Platform.runLater(stage::show));

            // 2. "Exit" Menu Item
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                Platform.exit();
                System.exit(0);
            });

            menu.add(openItem);
            menu.addSeparator();
            menu.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "Planify", menu);
            trayIcon.setImageAutoSize(true);

            // Double-click the icon to open the app
            trayIcon.addActionListener(e -> Platform.runLater(stage::show));

            tray.add(trayIcon);
        } catch (Exception e) {
            System.err.println("Could not initialize System Tray: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch();
    }
}