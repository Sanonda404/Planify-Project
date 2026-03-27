package com.planify.frontend.controllers.project;

import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.SceneParent;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import com.planify.frontend.utils.helpers.ProjectSorter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ProjectDetailsController extends SceneParent implements Initializable {

    @FXML private Label projectTitle;
    @FXML private Label projectDesc;
    @FXML private Label groupChip;
    @FXML private Label projectProgress;
    @FXML private Label projectDue;
    @FXML private Label milestoneCount;
    @FXML private Label taskCount;
    @FXML private Label completedCount;
    @FXML private Label memberCount;
    @FXML private Label avgWeightLabel;

    @FXML private ToggleButton overviewTab;
    @FXML private ToggleButton tasksTab;
    @FXML private ToggleButton progressTab;
    @FXML private ToggleButton resourcesTab;

    @FXML private HBox tabContainer;
    @FXML private StackPane dynamicContent;
    @FXML private ScrollPane scrollPane;

    private ProjectDetails projectDetails;
    private ProjectTasksController projectTasksController;
    private ProjectOverviewController projectOverviewController;
    private ProjectProgressController projectProgressController;
    private ProjectResourcesController projectResourcesController;

    @FXML private Circle connectionStatusCircle;
    @FXML private Label connectionStatusLabel;
    @FXML private ListView<NotificationResponse> notificationsList;
    @FXML public Button notifBtn;
    @FXML public VBox notifPanel;

    public void init() {
        NotificationController.setStatusControls(connectionStatusCircle, connectionStatusLabel);
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

        ToggleGroup group = new ToggleGroup();
        overviewTab.setToggleGroup(group);
        tasksTab.setToggleGroup(group);
        progressTab.setToggleGroup(group);
        resourcesTab.setToggleGroup(group);

        Platform.runLater(() -> {
            scrollPane.setVvalue(0.0);
            scrollPane.setHvalue(0.0);
        });
    }

    public String getName() {
        return projectDetails.getName();
    }

    public void setProjectDetails(ProjectDetails projectDetails) {
        this.projectDetails = projectDetails;
        ProjectSorter.sortProject(projectDetails);
        populateHeader();
        calculateAverageWeight();
        loadOverview();

        overviewTab.setOnAction(e -> loadOverview());
        tasksTab.setOnAction(e -> loadTasks());
        progressTab.setOnAction(e -> loadProgress());
        resourcesTab.setOnAction(e -> loadResources());
    }

    private void calculateAverageWeight() {
        int totalWeight = 0;
        int taskCount = 0;
        for (MilestoneDetails milestone : projectDetails.getMilestones()) {
            for (TaskDetails task : milestone.getTasks()) {
                // Task weight is stored in the task object - you'll need to add weight field to TaskDetails
                // For now using a placeholder - you can get weight from task.getWeight() if available
                totalWeight += 5; // Placeholder - replace with actual task.getWeight()
                taskCount++;
            }
        }
        int avgWeight = taskCount > 0 ? totalWeight / taskCount : 0;
        avgWeightLabel.setText(String.valueOf(avgWeight));
    }

    public void refresh() {
        String uuid = projectDetails.getUuid();
        if (projectDetails.getUuid().trim().isEmpty()) {
            projectDetails = ProjectDataManager.getPersonalProjectDetails(projectDetails.getName());
        } else {
            projectDetails = GroupProjectDataManager.getGroupProjectDetails(uuid);
        }

        if (projectDetails != null) {
            ProjectSorter.sortProject(projectDetails);
            populateHeader();
            calculateAverageWeight();

            if (projectOverviewController != null) projectOverviewController.setProjectDetails(projectDetails);
            if (projectTasksController != null) projectTasksController.setProjectDetails(projectDetails);
            if (projectProgressController != null) projectProgressController.setProjectDetails(projectDetails);
            if (projectResourcesController != null) projectResourcesController.setProjectDetails(projectDetails);
        }
    }

    private void populateHeader() {
        projectTitle.setText(projectDetails.getName());
        projectDesc.setText(projectDetails.getDescription());
        groupChip.setText("📁 " + (projectDetails.getGroupName() != null ? projectDetails.getGroupName() : "Personal"));
        projectProgress.setText(projectDetails.getProgress() + "%");
        projectDue.setText("Due: " + (projectDetails.getDeadline() != null ? projectDetails.getDeadline() : "Not set"));

        milestoneCount.setText(String.valueOf(projectDetails.getTotalMilestones()));
        taskCount.setText(String.valueOf(projectDetails.getTotalTasks()));
        memberCount.setText(String.valueOf(projectDetails.getTotalMembers()));

        long completed = projectDetails.getMilestones().stream()
                .flatMap(m -> m.getTasks().stream())
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .count();
        completedCount.setText(completed + "/" + projectDetails.getTotalTasks());
    }

    private void loadOverview() {
        loadTab("/com/planify/frontend/fxmls/project-overview-view.fxml");
    }

    private void loadTasks() {
        loadTab("/com/planify/frontend/fxmls/project-tasks-view.fxml");
    }

    private void loadProgress() {
        loadTab("/com/planify/frontend/fxmls/project-progress-view.fxml");
    }

    private void loadResources() {
        loadTab("/com/planify/frontend/fxmls/project-resources-view.fxml");
    }

    private void loadTab(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Node content = loader.load();
            dynamicContent.getChildren().setAll(content);

            Object controller = loader.getController();
            if (controller instanceof ProjectOverviewController) {
                ((ProjectOverviewController) controller).setProjectDetails(projectDetails);
                ((ProjectOverviewController) controller).setParent(this);
                this.projectOverviewController = (ProjectOverviewController) controller;
            } else if (controller instanceof ProjectTasksController) {
                ((ProjectTasksController) controller).setProjectDetails(projectDetails);
                ((ProjectTasksController) controller).setProjectDetailsController(this);
                this.projectTasksController = (ProjectTasksController) controller;
            } else if (controller instanceof ProjectProgressController) {
                ((ProjectProgressController) controller).setProjectDetailsController(this);
                ((ProjectProgressController) controller).setProjectDetails(projectDetails);
                this.projectProgressController = (ProjectProgressController) controller;
            } else if (controller instanceof ProjectResourcesController) {
                ((ProjectResourcesController) controller).setProjectDetails(projectDetails);
                ((ProjectResourcesController) controller).setParentController(this);
                this.projectResourcesController = (ProjectResourcesController) controller;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        SceneManager.switchScene("project-view.fxml", "Projects");
    }

    @FXML
    private void goDashboard() {
        SceneManager.switchScene("dashboard-view.fxml", "Dashboard");
    }

    @FXML
    private void handleLogout() {
        SceneManager.switchScene("login-view.fxml", "Login");
    }
}