package com.planify.frontend.controllers.project;

import com.planify.frontend.controllers.Request.EditRequestController;
import com.planify.frontend.models.project.MilestoneDetails;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EditMilestoneController implements Initializable {

    // ========== FXML COMPONENTS ==========
    
    @FXML private Button closeButton;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker dueDatePicker;
    
    // Progress Info (Read-only)
    @FXML private Label completionRateLabel;
    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Rectangle progressBackground;
    @FXML private Rectangle progressFill;
    
    // Warning Box
    @FXML private HBox warningBox;
    
    // ========== DATA ==========
    
    private MilestoneDetails milestone;
    private ProjectDetailsController projectDetailsController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup will be done in setMilestone()
    }

    // ========== SETUP ==========
    
    /**
     * Set the milestone to edit and pre-populate all fields
     */
    public void setMilestone(MilestoneDetails milestone, ProjectDetailsController projectDetailsController) {
        this.milestone = milestone;
        this.projectDetailsController = projectDetailsController;
        
        populateFields();
        displayProgressInfo();
        checkTasksWarning();
    }

    
    private void populateFields() {
        // Populate basic fields
        titleField.setText(milestone.getTitle());
        descriptionArea.setText(milestone.getDescription());
        
        // Parse and set due date
        try {
            LocalDate dueDate = LocalDate.parse(milestone.getDeadline());
            dueDatePicker.setValue(dueDate);
        } catch (Exception e) {
            System.err.println("Error parsing due date: " + e.getMessage());
        }
    }
    
    private void displayProgressInfo() {
        // Set progress labels
        completionRateLabel.setText(milestone.getCompletionRate() + "%");
        totalTasksLabel.setText(String.valueOf(milestone.getTotalTasks()));
        completedTasksLabel.setText(String.valueOf(milestone.getCompletedTasks()));
        
        // Update progress bar
        double progressPercentage = milestone.getCompletionRate() / 100.0;
        
        progressBackground.setWidth(400);
        progressFill.widthProperty().bind(
            progressBackground.widthProperty().multiply(progressPercentage)
        );
    }
    
    private void checkTasksWarning() {
        // Show warning if milestone has tasks
        if (milestone.getTotalTasks() > 0) {
            warningBox.setVisible(true);
            warningBox.setManaged(true);
        }
    }

    // ========== VALIDATION ==========
    
    private boolean validateForm() {
        List<String> errors = new ArrayList<>();
        
        // Title
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            errors.add("Milestone title is required");
        }
        
        // Description
        if (descriptionArea.getText() == null || descriptionArea.getText().trim().isEmpty()) {
            errors.add("Milestone description is required");
        }
        
        // Due Date
        if (dueDatePicker.getValue() == null) {
            errors.add("Due date is required");
        }
        
        // Show errors if any
        if (!errors.isEmpty()) {
            showValidationAlert(errors);
            return false;
        }
        
        return true;
    }
    
    private void showValidationAlert(List<String> errors) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation Error");
        alert.setHeaderText("Please fix the following errors:");
        
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            content.append((i + 1)).append(". ").append(errors.get(i));
            if (i < errors.size() - 1) {
                content.append("\n");
            }
        }
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    // ========== EVENT HANDLERS ==========
    
    @FXML
    private void handleSave() {
        // Validate form
        if (!validateForm()) {
            return;
        }
        
        // Check if anything changed
        if (!hasChanges()) {
            showInfoAlert("No changes detected", "No modifications were made to the milestone.");
            return;
        }
        
        // Gather updated data
        String updatedTitle = titleField.getText().trim();
        String updatedDescription = descriptionArea.getText().trim();
        String updatedDueDate = dueDatePicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        // Confirm save if due date changed and has tasks
        if (!updatedDueDate.equals(milestone.getDeadline()) && milestone.getTotalTasks() > 0) {
            if (!confirmDueDateChange()) {
                return;
            }
        }
        
        // Update milestone object
        milestone.setTitle(updatedTitle);
        milestone.setDescription(updatedDescription);
        milestone.setDeadline(updatedDueDate);
        
        // Save to backend
        if(milestone.getUuid().trim().isEmpty())saveLocally(milestone);
        else saveToBackend(milestone);
        
        // Close window
        handleClose();
    }
    
    private boolean hasChanges() {
        String currentTitle = titleField.getText().trim();
        String currentDescription = descriptionArea.getText().trim();
        String currentDueDate = dueDatePicker.getValue() != null ? 
            dueDatePicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : "";
        
        return !currentTitle.equals(milestone.getTitle()) ||
               !currentDescription.equals(milestone.getDescription()) ||
               !currentDueDate.equals(milestone.getDeadline());
    }
    
    private boolean confirmDueDateChange() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Due Date Change");
        alert.setHeaderText("This milestone has " + milestone.getTotalTasks() + " tasks.");
        alert.setContentText("Changing the due date may affect task schedules. Do you want to continue?");
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void saveLocally(MilestoneDetails milestone){
        ProjectDataManager.updatePersonalMilestone(milestone);
        projectDetailsController.refresh();
    }

    // ========== BACKEND INTEGRATION ==========
    
    /**
     * TODO: Backend Integration - Update milestone in database
     */
    private void saveToBackend(MilestoneDetails milestone) {
        // TODO: Call backend API to update milestone
        // Example: milestoneService.updateMilestone(milestone.getUuid(), milestone);

        EditRequestController.updateMilestone(milestone.getUuid(), LocalDataManager.getUserEmail(), milestone, projectDetailsController);
    }

    
    /**
     * Show info notification
     */
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
