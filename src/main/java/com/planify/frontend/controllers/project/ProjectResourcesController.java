package com.planify.frontend.controllers.project;

import com.planify.frontend.controllers.resources.AddResourceController;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.resources.ResourceDetails;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ProjectResourcesController implements Initializable {

    @FXML private VBox taskResourcesContainer;
    @FXML private VBox projectResourcesContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private VBox emptyState;

    private ProjectDetails projectDetails;
    private ProjectDetailsController parentController;
    private List<ResourceDetails> allResources = new ArrayList<>();
    private FilteredList<ResourceDetails> filteredResources;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeFilter.setItems(FXCollections.observableArrayList("All Types", "LINK", "FILE", "IMAGE", "DOCUMENT"));
        typeFilter.getSelectionModel().select("All Types");

        setupFilters();
    }

    public void setProjectDetails(ProjectDetails details) {
        this.projectDetails = details;
        loadResources();
    }

    public void setParentController(ProjectDetailsController controller) {
        this.parentController = controller;
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyFilters() {
        if (filteredResources == null) return;

        String searchText = searchField.getText().toLowerCase();
        String selectedType = typeFilter.getValue();

        filteredResources.setPredicate(resource -> {
            if (!searchText.isEmpty() && !resource.getName().toLowerCase().contains(searchText) &&
                    !resource.getSourceName().toLowerCase().contains(searchText)) {
                return false;
            }
            if (!"All Types".equals(selectedType) && !resource.getType().equals(selectedType)) {
                return false;
            }
            return true;
        });

        refreshDisplay();
    }

    private void loadResources() {
        allResources.clear();
        // Load project-level resources
        // TODO: Load from backend when available
        if(!projectDetails.getResources().isEmpty()){
            allResources.addAll(projectDetails.getResources());
        }

        filteredResources = new FilteredList<>(FXCollections.observableArrayList(allResources), p -> true);
        refreshDisplay();
    }

    private String detectType(String url) {
        if (url.contains("drive.google.com")) return "LINK";
        if (url.endsWith(".pdf")) return "DOCUMENT";
        if (url.endsWith(".jpg") || url.endsWith(".png") || url.endsWith(".jpeg")) return "IMAGE";
        if (url.endsWith(".zip") || url.endsWith(".rar")) return "FILE";
        return "LINK";
    }

    private void refreshDisplay() {
        taskResourcesContainer.getChildren().clear();
        projectResourcesContainer.getChildren().clear();

        List<ResourceDetails> taskResources = filteredResources.stream()
                .filter(r -> !r.getSourceUuid().trim().isEmpty())
                .toList();
        List<ResourceDetails> projectResources = filteredResources.stream()
                .filter(r -> r.getSourceUuid().trim().isEmpty())
                .toList();

        if (taskResources.isEmpty() && projectResources.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        // Group task resources by milestone
        for (MilestoneDetails milestone : projectDetails.getMilestones()) {
            List<ResourceDetails> milestoneResources = taskResources.stream()
                    .filter(r -> GroupProjectDataManager.getMilestoneName(r.getSourceUuid()).equals(milestone.getTitle()))
                    .toList();

            if (!milestoneResources.isEmpty()) {
                VBox milestoneGroup = createMilestoneResourceGroup(milestone, milestoneResources);
                taskResourcesContainer.getChildren().add(milestoneGroup);
            }
        }

        // Add remaining ungrouped task resources
        List<ResourceDetails> ungrouped = taskResources.stream()
                .filter(r -> GroupProjectDataManager.getMilestoneName(r.getSourceUuid()) == null || GroupProjectDataManager.getMilestoneName(r.getSourceUuid()).isEmpty())
                .toList();
        if (!ungrouped.isEmpty()) {
            VBox ungroupedGroup = createUngroupedResourceGroup(ungrouped);
            taskResourcesContainer.getChildren().add(ungroupedGroup);
        }

        // Add project resources
        for (ResourceDetails resource : projectResources) {
            projectResourcesContainer.getChildren().add(createResourceCard(resource));
        }
    }

    private VBox createMilestoneResourceGroup(MilestoneDetails milestone, List<ResourceDetails> resources) {
        VBox group = new VBox(10);
        group.getStyleClass().add("pr-resource-group");

        Label milestoneLabel = new Label("📌 " + milestone.getTitle());
        milestoneLabel.getStyleClass().add("pr-milestone-label");

        VBox resourcesBox = new VBox(8);
        for (ResourceDetails resource : resources) {
            resourcesBox.getChildren().add(createResourceCard(resource));
        }

        group.getChildren().addAll(milestoneLabel, resourcesBox);
        return group;
    }

    private VBox createUngroupedResourceGroup(List<ResourceDetails> resources) {
        VBox group = new VBox(10);
        group.getStyleClass().add("pr-resource-group");

        Label label = new Label("📌 Uncategorized");
        label.getStyleClass().add("pr-milestone-label");

        VBox resourcesBox = new VBox(8);
        for (ResourceDetails resource : resources) {
            resourcesBox.getChildren().add(createResourceCard(resource));
        }

        group.getChildren().addAll(label, resourcesBox);
        return group;
    }

    private HBox createResourceCard(ResourceDetails resource) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("pr-resource-card");
        card.setPadding(new Insets(12, 16, 12, 16));

        // Icon based on type
        Label icon = new Label(getResourceIcon(resource.getType().toUpperCase()));
        icon.getStyleClass().add("pr-resource-icon");

        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(resource.getName());
        nameLabel.getStyleClass().add("pr-resource-name");

        HBox metaBox = new HBox(12);
        Label typeLabel = new Label(resource.getType());
        typeLabel.getStyleClass().addAll("pr-resource-type", "pr-type-" + resource.getType().toUpperCase());

        Label taskLabel = new Label("📌 " + resource.getSourceName());
        taskLabel.getStyleClass().add("pr-resource-task");

        metaBox.getChildren().addAll(typeLabel, taskLabel);
        infoBox.getChildren().addAll(nameLabel, metaBox);

        // URL Link
        Hyperlink urlLink = new Hyperlink("Open");
        urlLink.getStyleClass().add("pr-resource-link");
        urlLink.setOnAction(e -> openUrl(resource.getUrl()));

        // Delete Button
        /*Button deleteBtn = new Button("🗑️");
        deleteBtn.getStyleClass().add("pr-delete-btn");
        deleteBtn.setOnAction(e -> deleteResource(resource));*/

        card.getChildren().addAll(icon, infoBox, urlLink);
        return card;
    }

    private String getResourceIcon(String type) {
        switch (type) {
            case "LINK": return "🔗";
            case "FILE": return "📄";
            case "IMAGE": return "🖼️";
            case "DOCUMENT": return "📑";
            default: return "📎";
        }
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteResource(ResourceDetails resource) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Resource");
        confirm.setHeaderText("Delete " + resource.getName() + "?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Call backend to delete resource
                allResources.remove(resource);
                filteredResources = new FilteredList<>(FXCollections.observableArrayList(allResources), p -> true);
                applyFilters();
                if (parentController != null) parentController.refresh();
            }
        });
    }

    @FXML
    private void openAddResource() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/add-resource-view.fxml"));
            Parent root = loader.load();

            AddResourceController controller = loader.getController();
            controller.setContextForProject(projectDetails.getUuid(), projectDetails.getName(),
                    GroupProjectDataManager.getGroupProjectTasks(projectDetails.getUuid()), parentController);

            Stage stage = new Stage();
            stage.setTitle("Add Resource");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadResources();
            if (parentController != null) parentController.refresh();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}