package com.planify.frontend.controllers.project;

import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProjectOverviewController {

    @FXML
    private HBox teamMembersBox;
    @FXML private VBox milestonesBox;

    private ProjectDetails projectDetails;
    private ProjectDetailsController projectDetailsController;

    public void setParent(ProjectDetailsController projectDetailsController){
        this.projectDetailsController = projectDetailsController;
    }

    public void setProjectDetails(ProjectDetails details) {
        this.projectDetails = details;
        loadMembers();
        loadMilestones();
    }

    private void loadMembers() {
        teamMembersBox.getChildren().clear();
        for (MemberInfo member : projectDetails.getMembers()) {
            Label chip = new Label("👤 " + member.getName());
            chip.getStyleClass().add("po-member-chip");
            teamMembersBox.getChildren().add(chip);
        }
    }

    private void loadMilestones() {
        milestonesBox.getChildren().clear();
        for (MilestoneDetails milestone : projectDetails.getMilestones()) {
            if(milestone.getTitle().equals("Uncategorized"))continue;
            HBox card = createMilestoneCard(milestone);
            milestonesBox.getChildren().add(card);
        }
    }

    private HBox createMilestoneCard(MilestoneDetails milestone) {
        HBox card = new HBox(20);
        card.getStyleClass().add("po-milestone-card");
        card.setCursor(Cursor.HAND); // pointer cursor

        Label status = new Label(milestone.isCompleted() ? "✓" : milestone.getTitle().substring(0,1));
        status.getStyleClass().add(milestone.isCompleted() ? "po-milestone-done" : "po-milestone-pending");

        VBox textBox = new VBox(6);
        Label title = new Label(milestone.getTitle());
        title.getStyleClass().add(milestone.isCompleted() ? "po-milestone-title-po-strike" : "po-milestone-title");
        Label desc = new Label(milestone.getDescription());
        desc.getStyleClass().add("po-milestone-desc");
        Label date = new Label("Due: " + milestone.getDeadline());
        date.getStyleClass().add("po-milestone-date");
        textBox.getChildren().addAll(title, desc, date);

        card.getChildren().addAll(status, textBox);

        // Click handler → show popup
        card.setOnMouseClicked(e -> showMilestonePopup(milestone));

        return card;
    }

    private void showMilestonePopup(MilestoneDetails milestone) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Milestone Details");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.getChildren().add(new Label("Name: " + milestone.getTitle()));
        content.getChildren().add(new Label("Description: " + milestone.getDescription()));
        content.getChildren().add(new Label("Due Date: " + milestone.getDeadline()));

        content.getChildren().add(new Label("Tasks:"));
        if (milestone.getTasks() != null) {
            for (TaskDetails task : milestone.getTasks()) {
                CheckBox cb = new CheckBox(task.getTitle());
                cb.setSelected("COMPLETED".equalsIgnoreCase(task.getStatus()));
                cb.setDisable(true); // read-only
                content.getChildren().add(cb);
            }
        }

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }
}