package com.planify.frontend.utils.managers;

import com.planify.frontend.controllers.group.GroupDetailsController;
import com.planify.frontend.controllers.project.ProjectDetailsController;
import com.planify.frontend.models.group.GroupDetails;
import com.planify.frontend.models.project.ProjectDetails;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.function.Consumer;

public class SceneManager {
    public static Stage primaryStage;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchScene(String fxmlPath, String title) {
        try {
            // Use absolute path starting from the root of the resources folder
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/" + fxmlPath));
            Parent root = loader.load();
            primaryStage.setTitle(title + " - Planify");
            primaryStage.setScene(new Scene(root));
            primaryStage.centerOnScreen();
            primaryStage.show();
            primaryStage.setWidth(java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth());
            primaryStage.setHeight(java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight());
            primaryStage.setMaximized(true);
            primaryStage.setOnCloseRequest(e -> System.exit(0));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load FXML: " + fxmlPath);
        }
    }

    public static void switchScene(String fxmlPath, String title, GroupDetails groupDetails){
        try {
            // Use absolute path starting from the root of the resources folder
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/" + fxmlPath));
            Parent root = loader.load();

            GroupDetailsController ctrl = loader.getController();
            ctrl.setGroupDetails(groupDetails);

            primaryStage.setTitle(title + " - Planify");
            primaryStage.setScene(new Scene(root));
            primaryStage.centerOnScreen();
            primaryStage.show();
            primaryStage.setResizable(true);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load FXML: " + fxmlPath);
        }
    }

    public static void switchScene(String fxmlPath, String title, ProjectDetails projectDetails){
        try {
            // Use absolute path starting from the root of the resources folder
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/" + fxmlPath));
            Parent root = loader.load();

            ProjectDetailsController ctrl = loader.getController();
            ctrl.setProjectDetails(projectDetails);

            primaryStage.setTitle(title + " - Planify");
            primaryStage.setScene(new Scene(root));
            primaryStage.centerOnScreen();
            primaryStage.show();
            primaryStage.setResizable(true);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load FXML: " + fxmlPath);
        }
    }

    public static void switchSceneWithData(String fxml, String title, Consumer<Object> controllerConsumer) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/" + fxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            controllerConsumer.accept(controller);

            Stage stage = (Stage) Stage.getWindows().filtered(Window::isShowing).get(0);
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}