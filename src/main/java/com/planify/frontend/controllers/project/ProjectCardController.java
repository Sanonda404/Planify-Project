package com.planify.frontend.controllers.project;

import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.project.MilestoneSummary;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.project.ProjectSummary;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;

public class ProjectCardController {

    @FXML private Label titleLabel;
    @FXML private Label descLabel;
    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label milestoneCountLabel;
    @FXML private Label teamCountLabel;
    @FXML private GridPane milestoneGrid;
    @FXML private FlowPane teamPane;
    @FXML private Button viewDetailsBtn;
    @FXML private Button addTaskBtn;

    private ProjectSummary project;

    public void setProject(ProjectSummary project) {
        this.project = project;

        // Basic info
        titleLabel.setText(project.getName());
        descLabel.setText(project.getDescription());
        progressLabel.setText(project.getProgress() + "%");
        progressBar.setProgress(project.getProgress() / 100.0);

        milestoneCountLabel.setText("Milestones (" + (project.getMilestones()==null?0:project.getMilestones().size()) + ")");
        teamCountLabel.setText("Team (" + (project.getMembers()==null?0:project.getMembers().size()) + ")");


        loadMilestones();
        loadMembers();
    }

    private void loadMilestones() {
        milestoneGrid.getChildren().clear();

        int col = 0;
        int row = 0;

        for (MilestoneSummary milestone : project.getMilestones()) {
            if(milestone.getTitle().equals("Uncategorized"))continue;
            HBox box = createMilestoneBox(milestone);
            box.setMaxWidth(Double.MAX_VALUE);

            milestoneGrid.add(box, col, row);
            GridPane.setHgrow(box, Priority.ALWAYS);

            col++;
            if (col == 2) {
                col = 0;
                row++;
            }
        }
    }

    private HBox createMilestoneBox(MilestoneSummary milestone) {
        HBox root = new HBox();
        root.setSpacing(10);
        root.setPadding(new Insets(14));
        root.setMinHeight(76);
        root.setMaxWidth(Double.MAX_VALUE);

        if (milestone.isCompleted()) {
            root.getStyleClass().add("milestoneDone");
        } else {
            root.getStyleClass().add("milestoneBox");
        }

        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label title = new Label(milestone.getTitle());
        if (milestone.isCompleted()) {
            title.getStyleClass().add("milestoneTitleStrike");
        } else {
            title.getStyleClass().add("milestoneTitle");
        }

        Label date = new Label("Due: " + milestone.getDeadline());
        date.getStyleClass().add("milestoneDate");

        textBox.getChildren().addAll(title, date);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane circle = new StackPane();
        circle.getStyleClass().add("statusCircle");

        if (milestone.isCompleted()) {
            circle.getStyleClass().add("statusDone");
            Label tick = new Label("✓");
            tick.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            circle.getChildren().add(tick);
        } else {
            circle.getStyleClass().add("statusPending");
        }

        root.getChildren().addAll(textBox, spacer, circle);
        return root;
    }

    private void loadMembers() {
        teamPane.getChildren().clear();

        for (MemberInfo member : project.getMembers()) {
            Label chip = new Label("👤 " + member.getName());
            chip.getStyleClass().add("memberChip");
            teamPane.getChildren().add(chip);
        }
    }

    @FXML
    private void handleViewDetails() {
        // TODO: navigate to project details view
        System.out.println("View details: " + project.getName());
        System.out.println(project.getUuid());
        if(project!=null){
            if(project.getUuid().trim().isEmpty()){
                showPersonalProject();
            }
            else{
                showGroupProject();
            }
        }

    }

    private void showPersonalProject(){
        System.out.println("loading personal...");
        ProjectDetails projectDetails = ProjectDataManager.getPersonalProjectDetails(project.getName());
        System.out.println(projectDetails);
        if (projectDetails == null) {
            Label noGroupsLabel = new Label("No Details found");
            System.out.println("null");
            //groupsGrid.getChildren().add(noGroupsLabel);
        } else {
            SceneManager.switchScene("project-details-view.fxml","Project Details",projectDetails);
        }
    }

    private void showGroupProject(){
        if (project != null) {
            String uuid = project.getUuid();
            System.out.println("View details for project UUID: " + uuid);
            ProjectDetails projectDetails = GroupProjectDataManager.getGroupProjectDetails(uuid);
            if (projectDetails == null) {
                Label noGroupsLabel = new Label("No Details found");
                System.out.println("null");
                //groupsGrid.getChildren().add(noGroupsLabel);
            } else {
                SceneManager.switchScene("project-details-view.fxml","Project Details",projectDetails);
            }

        }
    }

    @FXML
    private void handleAddTask() {
        // TODO: open add-task view with project context
        System.out.println("Add task: " + project.getName());
    }
}