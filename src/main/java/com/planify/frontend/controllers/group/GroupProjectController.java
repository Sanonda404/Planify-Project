package com.planify.frontend.controllers.group;

import com.planify.frontend.controllers.project.AddProjectController;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.utils.managers.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class GroupProjectController {

    @FXML private VBox projectsContainer;
    private String groupUuid;
    private String groupName;
    private int totalMembers;

    @FXML
    private void initialize() {
        // Initialization
    }

    public void setGroupContext(String uuid, String name, int totalMembers) {
        this.groupUuid = uuid;
        this.groupName = name;
        this.totalMembers = totalMembers;
    }

    public void setProjects(List<ProjectDetails> projects) {
        projectsContainer.getChildren().clear();

        if (projects == null || projects.isEmpty()) {
            VBox emptyState = createEmptyState();
            projectsContainer.getChildren().add(emptyState);
            return;
        }

        for (ProjectDetails project : projects) {
            VBox card = createProjectCard(project);
            projectsContainer.getChildren().add(card);
        }
    }

    private VBox createProjectCard(ProjectDetails project) {
        VBox card = new VBox(18);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("project-card");
        card.setPadding(new Insets(26, 30, 26, 30));

        // Title row
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(project.getName());
        titleLabel.getStyleClass().add("project-title");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        int percent = project.getProgress();
        Label percentLabel = new Label(percent + "%");
        percentLabel.getStyleClass().add(getPercentClass(percent));

        titleRow.getChildren().addAll(titleLabel, spacer1, percentLabel);

        // Progress bar
        ProgressBar progressBar = new ProgressBar(percent / 100.0);
        progressBar.setPrefHeight(10);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add(getProgressClass(percent));

        // Stats row
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        Label tasksLabel = new Label("📝 " + project.getCompletedTasks() + "/" + project.getTotalTasks() + " tasks");
        tasksLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        Label membersLabel = new Label("👥 " + totalMembers + " members");
        membersLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        statsRow.getChildren().addAll(tasksLabel, membersLabel);

        // Deadline row
        HBox deadlineRow = new HBox();
        deadlineRow.setAlignment(Pos.CENTER_LEFT);

        Label deadlineLabel = new Label("📅 Deadline: " + (project.getDeadline() != null ? project.getDeadline() : "Not set"));
        deadlineLabel.getStyleClass().add("project-deadline");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button viewBtn = new Button("View Project →");
        viewBtn.getStyleClass().add("project-view-btn");
        viewBtn.setOnAction(e -> openProjectDetails(project));

        deadlineRow.getChildren().addAll(deadlineLabel, spacer2, viewBtn);

        card.getChildren().addAll(titleRow, progressBar, statsRow, deadlineRow);
        return card;
    }

    private String getPercentClass(int percent) {
        if (percent >= 70) return "project-percent-green";
        else if (percent >= 30) return "project-percent-orange";
        else return "project-percent-red";
    }

    private String getProgressClass(int percent) {
        if (percent >= 70) return "project-progress-green";
        else if (percent >= 30) return "project-progress-orange";
        else return "project-progress-red";
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(60));
        emptyState.getStyleClass().add("empty-state");

        Label iconLabel = new Label("🚀");
        iconLabel.getStyleClass().add("empty-icon");

        Label textLabel = new Label("No projects yet");
        textLabel.getStyleClass().add("empty-text");

        Label subLabel = new Label("Create a project to start collaborating");
        subLabel.getStyleClass().add("empty-text");
        subLabel.setStyle("-fx-font-size: 12px;");

        emptyState.getChildren().addAll(iconLabel, textLabel, subLabel);
        return emptyState;
    }

    @FXML
    private void openAddProject() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/add-project-view.fxml"));
            Parent root = loader.load();

            AddProjectController controller = loader.getController();
            controller.setGroupContext(groupUuid);

            Stage stage = new Stage();
            stage.setTitle("Add Project");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openProjectDetails(ProjectDetails project) {
        // TODO: Navigate to project details
        System.out.println("Opening project: " + project.getName());
    }
}