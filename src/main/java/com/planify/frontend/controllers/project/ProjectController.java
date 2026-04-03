package com.planify.frontend.controllers.project;

import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.SceneParent;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.models.project.ProjectSummary;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ProjectController extends SceneParent implements Initializable {

    @FXML private FlowPane projectGridContainer; // Changed from VBox to FlowPane
    @FXML private Button allBtn, backButton, dashboardButton;
    @FXML private Button personalBtn;
    @FXML private Button groupBtn;

    private final List<ProjectSummary> allProjects = new ArrayList<>();
    private AddProjectController addProjectController;

    @FXML private Circle connectionStatusCircle;

    @FXML private Label connectionStatusLabel;
    @FXML private ListView<NotificationResponse> notificationsList;
    @FXML public Button notifBtn;
    @FXML public VBox notifPanel;

    public void init(){
        NotificationController.setStatusControls(connectionStatusCircle,connectionStatusLabel);
    }

    @FXML
    private void toggleNotifications() {
        boolean isVisible = notifPanel.isVisible();
        notifPanel.setVisible(!isVisible);
        notifPanel.setManaged(!isVisible);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        notificationsList.setItems(NotificationManager.getNotifications());

        notificationsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(NotificationResponse notif, boolean empty) {
                super.updateItem(notif, empty);

                if (empty || notif == null) {
                    setGraphic(null);
                } else {
                    setGraphic(createNotificationItem(notif));
                }
            }
        });

        init();
        NotificationManager.setParent(this);
        fetchProjects();
    }

    public void refresh(){
        System.out.println("Refreshing projects...");
        fetchProjects();
    }

    private void fetchProjects() {
        allProjects.clear();
        allProjects.addAll(GroupProjectDataManager.getGroupProjectSummary());
        addPersonalProjects();
        renderProjects(allProjects);
    }

    private void renderProjects(List<ProjectSummary> projects) {
        projectGridContainer.getChildren().clear();

        for (ProjectSummary project : projects) {
            try {
                FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/project-card-view.fxml"));
                VBox card = loader.load();

                ProjectCardController controller = loader.getController();
                controller.setProject(project);

                projectGridContainer.getChildren().add(card);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addPersonalProjects(){
        allProjects.addAll(ProjectDataManager.getPersonalProjectSummary());
    }

    @FXML
    private void showAllProjects() {
        renderProjects(allProjects);
        setActiveFilter(allBtn,personalBtn,groupBtn);
    }

    @FXML
    private void showPersonalProjects() {
        List<ProjectSummary> filtered = new ArrayList<>();
        for (ProjectSummary project : allProjects) {
            if ("personal".equalsIgnoreCase(project.getGroupName())) {
                filtered.add(project);
            }
        }
        renderProjects(filtered);
        setActiveFilter(personalBtn, allBtn, groupBtn);
    }

    @FXML
    private void showGroupProjects() {
        List<ProjectSummary> filtered = new ArrayList<>();
        for (ProjectSummary project : allProjects) {
            if (!"personal".equalsIgnoreCase(project.getGroupName())) {
                filtered.add(project);
            }
        }
        renderProjects(filtered);
        setActiveFilter(groupBtn, allBtn, personalBtn);
    }

    private void setActiveFilter(Button activeButton, Button... otherButtons) {
        // Set active button style
        activeButton.getStyleClass().remove("filter-btn");
        activeButton.getStyleClass().add("filter-btn-active");

        // Set other buttons to inactive style
        for (Button button : otherButtons) {
            button.getStyleClass().remove("filter-btn-active");
            button.getStyleClass().add("filter-btn");
        }
    }

    @FXML
    private void goDashboard() {
        SceneManager.switchScene("dashboard-view.fxml","Dashboard");
    }

    @FXML
    private void openAddProject() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/planify/frontend/fxmls/add-project-view.fxml"));
            Parent root = loader.load();

            AddProjectController controller = loader.getController();
            controller.setParent(this);

            Stage stage = new Stage();
            stage.setTitle("Add Project");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}