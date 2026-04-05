package com.planify.frontend.controllers.auth;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.controllers.Request.GetRequestController;
import com.planify.frontend.models.auth.LoginRequest;
import com.planify.frontend.models.auth.SecurityAnswerVerification;
import com.planify.frontend.utils.managers.SceneManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class ForgotPassController {
    @FXML private ImageView illustrationImage;

    // Left Panel Components
    @FXML private StackPane leftPanel;
    @FXML private Circle blob1, blob2, blob3;
    @FXML private Button backBtn;

    // Step Indicators
    @FXML private Pane dot1, dot2;
    @FXML private Label stepLabel;

    // Step 1 Views
    @FXML private VBox step1View;
    @FXML private TextField usernameField;
    @FXML private VBox questionBox;
    @FXML private Label securityQuestionLabel;
    @FXML private TextField answerField;
    @FXML private Label step1ErrorLabel;
    @FXML private Button step1Btn;

    // Step 2 Views
    @FXML private VBox step2View;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button toggleNewPw;
    @FXML private Button toggleConfirmPw;
    @FXML private Pane strengthBarFill;
    @FXML private Label strengthLabel;
    @FXML private Label step2ErrorLabel;
    @FXML private Button step2Btn;

    // Success View
    @FXML private VBox successView;
    @FXML private Button goToLoginBtn;

    // ========== DATA ==========
    private String verifiedUsername;
    private String securityQuestion;
    private String securityAnswer;
    private boolean isVerified = false;

    // ========== INITIALIZATION ==========

    @FXML
    public void initialize() {
        setupAnimations();
        setupPasswordStrengthListener();
        setupEnterKeyHandlers();
    }

    private void setupAnimations() {
        // Animate blobs on startup
        Timeline blobAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(blob1.translateXProperty(), 0),
                        new KeyValue(blob1.translateYProperty(), 0),
                        new KeyValue(blob2.translateXProperty(), 0),
                        new KeyValue(blob2.translateYProperty(), 0),
                        new KeyValue(blob3.translateXProperty(), 0),
                        new KeyValue(blob3.translateYProperty(), 0)
                ),
                new KeyFrame(Duration.seconds(8),
                        new KeyValue(blob1.translateXProperty(), 30),
                        new KeyValue(blob1.translateYProperty(), -20),
                        new KeyValue(blob2.translateXProperty(), -25),
                        new KeyValue(blob2.translateYProperty(), 15),
                        new KeyValue(blob3.translateXProperty(), 20),
                        new KeyValue(blob3.translateYProperty(), 25)
                ),
                new KeyFrame(Duration.seconds(16),
                        new KeyValue(blob1.translateXProperty(), -15),
                        new KeyValue(blob1.translateYProperty(), 10),
                        new KeyValue(blob2.translateXProperty(), 20),
                        new KeyValue(blob2.translateYProperty(), -10),
                        new KeyValue(blob3.translateXProperty(), -10),
                        new KeyValue(blob3.translateYProperty(), -15)
                )
        );
        blobAnimation.setCycleCount(Timeline.INDEFINITE);
        blobAnimation.setAutoReverse(true);
        blobAnimation.play();
    }

    private void setupPasswordStrengthListener() {
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
        });
    }

    private void setupEnterKeyHandlers() {
        usernameField.setOnAction(e -> handleLookup());
        answerField.setOnAction(e -> handleVerify());
        newPasswordField.setOnAction(e -> handleReset());
        confirmPasswordField.setOnAction(e -> handleReset());
    }

    // ========== PASSWORD STRENGTH ==========

    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            strengthBarFill.setPrefWidth(0);
            strengthLabel.setText("6–20 characters");
            strengthLabel.setStyle("-fx-text-fill: #94a3b8;");
            return;
        }

        int strength = calculatePasswordStrength(password);
        double widthPercentage = strength / 100.0;
        double maxWidth = 380; // Approximate max width of the bar

        strengthBarFill.setPrefWidth(maxWidth * widthPercentage);

        if (strength < 40) {
            strengthBarFill.setStyle("-fx-background-color: #ef4444;");
            strengthLabel.setText("Weak password");
            strengthLabel.setStyle("-fx-text-fill: #ef4444;");
        } else if (strength < 70) {
            strengthBarFill.setStyle("-fx-background-color: #f59e0b;");
            strengthLabel.setText("Medium password");
            strengthLabel.setStyle("-fx-text-fill: #f59e0b;");
        } else {
            strengthBarFill.setStyle("-fx-background-color: #10b981;");
            strengthLabel.setText("Strong password");
            strengthLabel.setStyle("-fx-text-fill: #10b981;");
        }
    }

    private int calculatePasswordStrength(String password) {
        int score = 0;

        // Length check
        if (password.length() >= 8) score += 25;
        else if (password.length() >= 6) score += 15;

        // Contains uppercase
        if (Pattern.compile("[A-Z]").matcher(password).find()) score += 20;

        // Contains lowercase
        if (Pattern.compile("[a-z]").matcher(password).find()) score += 15;

        // Contains digit
        if (Pattern.compile("[0-9]").matcher(password).find()) score += 20;

        // Contains special character
        if (Pattern.compile("[^a-zA-Z0-9]").matcher(password).find()) score += 20;

        return Math.min(score, 100);
    }

    // ========== STEP 1: LOOKUP USER ==========

    @FXML
    private void handleLookup() {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            showError(step1ErrorLabel, "Please enter your username or email.");
            return;
        }

        // Disable button and show loading state
        step1Btn.setDisable(true);
        step1Btn.setText("Checking...");
        clearError(step1ErrorLabel);

        // TODO: Call backend API to get security question
        GetRequestController.getSecurityQues(username,(response)->{
            Platform.runLater(() -> {
                System.out.println(username);
                if (response==null) {
                    showError(step1ErrorLabel,"Couldn't find question");
                }else{
                    securityQuestion = response.getPassword();
                    verifiedUsername = username;
                    showSecurityQuestion();
                }

            });

        });

    }

    private void simulateLookupSuccess(String username) {
        // Simulate API call delay
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> {
            securityQuestion = "What was the name of your first pet?";
            securityAnswer = "max"; // In real app, this would be a hash
            verifiedUsername = username;
            showSecurityQuestion();
            step1Btn.setDisable(false);
            step1Btn.setText("Find My Account");
        });
        delay.play();
    }

    private void showSecurityQuestion() {
        securityQuestionLabel.setText(securityQuestion);
        questionBox.setVisible(true);
        questionBox.setManaged(true);
        step1Btn.setText("Verify Answer");
        step1Btn.setDisable(false);
        step1Btn.setOnAction(e -> handleVerify());

        // Animate the question box appearance
        questionBox.setOpacity(0);
        questionBox.setScaleX(0.95);
        questionBox.setScaleY(0.95);

        Timeline appear = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(questionBox.opacityProperty(), 0),
                        new KeyValue(questionBox.scaleXProperty(), 0.95),
                        new KeyValue(questionBox.scaleYProperty(), 0.95)
                ),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(questionBox.opacityProperty(), 1),
                        new KeyValue(questionBox.scaleXProperty(), 1),
                        new KeyValue(questionBox.scaleYProperty(), 1)
                )
        );
        appear.play();
    }

    // ========== STEP 1: VERIFY ANSWER ==========

    @FXML
    private void handleVerify() {
        String answer = answerField.getText().trim().toLowerCase();

        if (answer.isEmpty()) {
            showError(step1ErrorLabel, "Please answer the security question.");
            return;
        }

        step1Btn.setDisable(true);
        step1Btn.setText("Verifying...");
        clearError(step1ErrorLabel);

        // TODO: Call backend API to verify answer
        SecurityAnswerVerification req = new SecurityAnswerVerification(verifiedUsername,answer);
        CreateRequestController.verifySecurityAnswer(req,this);
    }

    public void simulateVerificationSuccess() {
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(0.8));
        delay.setOnFinished(e -> {
            isVerified = true;
            transitionToStep2();
            step1Btn.setDisable(false);
        });
        delay.play();
    }

    // ========== TRANSITION TO STEP 2 ==========

    private void transitionToStep2() {
        // Update step dots
        dot1.getStyleClass().remove("step-dot-active");
        dot1.getStyleClass().add("step-dot-completed");
        dot2.getStyleClass().remove("step-dot-inactive");
        dot2.getStyleClass().add("step-dot-active");
        stepLabel.setText("Step 2 of 2");

        // Fade out step 1
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), step1View);
        fadeOut.setOnFinished(e -> {
            step1View.setVisible(false);
            step1View.setManaged(false);

            // Show step 2
            step2View.setVisible(true);
            step2View.setManaged(true);
            step2View.setOpacity(0);
            step2View.setTranslateX(20);

            // Animate step 2 in
            Timeline slideIn = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(step2View.opacityProperty(), 0),
                            new KeyValue(step2View.translateXProperty(), 20)
                    ),
                    new KeyFrame(Duration.millis(300),
                            new KeyValue(step2View.opacityProperty(), 1),
                            new KeyValue(step2View.translateXProperty(), 0)
                    )
            );
            slideIn.play();

            // Focus on new password field
            newPasswordField.requestFocus();
        });
        fadeOut.play();
    }

    // ========== STEP 2: RESET PASSWORD ==========

    @FXML
    private void handleReset() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate passwords
        if (newPassword.isEmpty()) {
            showError(step2ErrorLabel, "Please enter a new password.");
            return;
        }

        if (newPassword.length() < 6) {
            showError(step2ErrorLabel, "Password must be at least 6 characters long.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError(step2ErrorLabel, "Passwords do not match.");
            return;
        }

        step2Btn.setDisable(true);
        step2Btn.setText("Resetting...");
        clearError(step2ErrorLabel);

        // TODO: Call backend API to reset password
        LoginRequest req = new LoginRequest(verifiedUsername, newPassword);
        CreateRequestController.handleResetPassword(req,this);

        simulateResetSuccess();
    }

    public void simulateResetSuccess() {
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> {
            step2Btn.setDisable(false);
            showSuccess();
        });
        delay.play();
    }

    // ========== SHOW SUCCESS VIEW ==========

    private void showSuccess() {
        // Hide step 2
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), step2View);
        fadeOut.setOnFinished(e -> {
            step2View.setVisible(false);
            step2View.setManaged(false);

            // Show success view
            successView.setVisible(true);
            successView.setManaged(true);
            successView.setOpacity(0);
            successView.setScaleX(0.9);
            successView.setScaleY(0.9);

            // Animate success view
            Timeline scaleIn = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(successView.opacityProperty(), 0),
                            new KeyValue(successView.scaleXProperty(), 0.9),
                            new KeyValue(successView.scaleYProperty(), 0.9)
                    ),
                    new KeyFrame(Duration.millis(300),
                            new KeyValue(successView.opacityProperty(), 1),
                            new KeyValue(successView.scaleXProperty(), 1),
                            new KeyValue(successView.scaleYProperty(), 1)
                    )
            );
            scaleIn.play();

            // Update step dots to completed
            dot2.getStyleClass().remove("step-dot-active");
            dot2.getStyleClass().add("step-dot-completed");
            stepLabel.setText("Complete");
        });
        fadeOut.play();
    }

    // ========== PASSWORD TOGGLE ==========

    @FXML
    private void toggleNewPassword() {
        togglePasswordVisibility(newPasswordField, toggleNewPw);
    }

    @FXML
    private void toggleConfirmPassword() {
        togglePasswordVisibility(confirmPasswordField, toggleConfirmPw);
    }

    private void togglePasswordVisibility(PasswordField passwordField, Button toggleButton) {
        if (passwordField.getScene() != null) {
            // This is a simplified approach - in production, you'd need to replace the field
            // with a TextField temporarily or use a different approach
            if (toggleButton.getText().equals("Show")) {
                toggleButton.setText("Hide");
                // Note: Full implementation would require replacing PasswordField with TextField
                // For now, this is a placeholder
            } else {
                toggleButton.setText("Show");
            }
        }
    }

    // ========== NAVIGATION ==========

    @FXML
    private void handleBack() {
        if (successView.isVisible()) {
            SceneManager.switchScene("login-view.fxml", "Log in");
        } else if (step2View.isVisible()) {
            // Go back to step 1
            transitionToStep1();
        } else {
            SceneManager.switchScene("login-view.fxml", "Log in");
        }
    }

    private void transitionToStep1() {
        // Reset step dots
        dot2.getStyleClass().remove("step-dot-active");
        dot2.getStyleClass().add("step-dot-inactive");
        dot1.getStyleClass().remove("step-dot-completed");
        dot1.getStyleClass().add("step-dot-active");
        stepLabel.setText("Step 1 of 2");

        // Hide step 2
        step2View.setVisible(false);
        step2View.setManaged(false);

        // Clear step 2 fields
        newPasswordField.clear();
        confirmPasswordField.clear();
        clearError(step2ErrorLabel);

        // Show step 1
        step1View.setVisible(true);
        step1View.setManaged(true);
        step1View.setOpacity(1);

        // Reset question box visibility
        questionBox.setVisible(false);
        questionBox.setManaged(false);
        answerField.clear();
        step1Btn.setText("Find My Account");
        step1Btn.setOnAction(e -> handleLookup());
    }

    @FXML
    private void handleGoToLogin() {
        SceneManager.switchScene("login-view.fxml", "Log in");
    }

    // ========== UTILITY METHODS ==========

    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        // Shake animation for error
        Timeline shake = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(errorLabel.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(50), new KeyValue(errorLabel.translateXProperty(), -10)),
                new KeyFrame(Duration.millis(100), new KeyValue(errorLabel.translateXProperty(), 10)),
                new KeyFrame(Duration.millis(150), new KeyValue(errorLabel.translateXProperty(), -5)),
                new KeyFrame(Duration.millis(200), new KeyValue(errorLabel.translateXProperty(), 5)),
                new KeyFrame(Duration.millis(250), new KeyValue(errorLabel.translateXProperty(), 0))
        );
        shake.play();
    }

    private void clearError(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
    }
}