package com.planify.frontend.utils.helpers;

import com.planify.frontend.utils.managers.SceneManager;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

public class AlertCreator {
   public static void showSuccessAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("✅ Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                SceneManager.class.getResource("/com/planify/frontend/css/alerts.css").toExternalForm()
        );
        dialogPane.getStyleClass().add("success-alert");
        alert.showAndWait();
   }
    public static void showErrorAlert(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("❌ Validation Error");
        alert.setHeaderText("Got into this error:");
        alert.setContentText(error);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                SceneManager.class.getResource("/com/planify/frontend/css/alerts.css").toExternalForm()
        );
        dialogPane.getStyleClass().add("error-alert");
        alert.showAndWait();
    }

    public static void showErrorAlert(String title, String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Got into this error:");
        alert.setContentText(error);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                SceneManager.class.getResource("/com/planify/frontend/css/alerts.css").toExternalForm()
        );
        dialogPane.getStyleClass().add("error-alert");
        alert.showAndWait();
    }
}
