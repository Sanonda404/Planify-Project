package com.planify.frontend.controllers.auth;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.auth.LoginRequest;
import com.planify.frontend.models.auth.SignupDetails;
import com.planify.frontend.network.BackendConnectionValidation;
import com.planify.frontend.utils.UserSession;
import com.planify.frontend.utils.data.group.GroupDataManager;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.PasswordHasher;
import com.planify.frontend.utils.managers.SceneManager;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;

public class AuthController {

    // ===== FXML COMPONENTS =====

    // Tab System
    @FXML private Pane tabIndicator;
    @FXML private Button loginTabBtn;
    @FXML private Button signupTabBtn;

    // Forms
    @FXML private VBox loginForm;
    @FXML private VBox signupForm;

    // Login Fields
    @FXML private TextField loginNameOrEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Button loginButton;
    @FXML private Label loginErrorLabel;

    // Signup Fields
    @FXML private TextField signupName;
    @FXML private TextField signupEmail;
    @FXML private PasswordField signupPassword;
    @FXML private CheckBox termsCheckbox;
    @FXML private Button signupButton;
    @FXML private Label signupErrorLabel;
    @FXML private StackPane leftSection;
    @FXML private StackPane rightSection;
    @FXML private TextField signupSecurityQuestion;
    @FXML private TextField signupSecurityAnswer;
    @FXML private VBox formCard;

    private boolean entryPlayed = false;
    private Timeline borderAnimation;
    // ===== INITIALIZATION =====

    @FXML
    public void initialize() {
        // Set initial tab indicator position (Login tab)
        tabIndicator.setTranslateX(0);
        loginTabBtn.getStyleClass().add("active");

        // Show login form by default
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        signupForm.setVisible(false);
        signupForm.setManaged(false);
        setupEntryAnimationAfterWindowShown();
    }

    // ===== TAB SWITCHING =====

    @FXML
    private void showLogin() {
        // Show login form
        loginForm.setVisible(true);
        loginForm.setManaged(true);

        // Hide signup form
        signupForm.setVisible(false);
        signupForm.setManaged(false);

        // Move indicator to login tab
        moveIndicator(0);

        // Update active styles
        loginTabBtn.getStyleClass().add("active");
        signupTabBtn.getStyleClass().remove("active");

        // Clear error messages
        clearErrors();
        stopSignupBorderAnimation();
    }
    private void setupEntryAnimationAfterWindowShown() {
        if (formCard == null) return;

        formCard.setOpacity(0);
        formCard.setTranslateY(35);

        formCard.sceneProperty().addListener((obsScene, oldScene, newScene) -> {
            if (newScene == null) return;

            newScene.windowProperty().addListener((obsWindow, oldWindow, newWindow) -> {
                if (newWindow == null) return;

                newWindow.showingProperty().addListener((obsShowing, wasShowing, isShowing) -> {
                    if (!isShowing || entryPlayed) return;

                    PauseTransition delay = new PauseTransition(Duration.millis(120));
                    delay.setOnFinished(e -> playEntryAnimation());
                    delay.play();
                });
            });

            Window existingWindow = newScene.getWindow();
            if (existingWindow != null && existingWindow.isShowing() && !entryPlayed) {
                PauseTransition delay = new PauseTransition(Duration.millis(120));
                delay.setOnFinished(e -> playEntryAnimation());
                delay.play();
            }
        });

        Scene existingScene = formCard.getScene();
        if (existingScene != null) {
            Window existingWindow = existingScene.getWindow();
            if (existingWindow != null && existingWindow.isShowing() && !entryPlayed) {
                PauseTransition delay = new PauseTransition(Duration.millis(120));
                delay.setOnFinished(e -> playEntryAnimation());
                delay.play();
            }
        }
    }

    private void playEntryAnimation() {
        if (entryPlayed || formCard == null) return;
        entryPlayed = true;

        FadeTransition fade = new FadeTransition(Duration.millis(950), formCard);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(950), formCard);
        slide.setFromY(35);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition entry = new ParallelTransition(fade, slide);
        entry.play();
    }


    @FXML
    private void showSignup() {
        // Show signup form
        signupForm.setVisible(true);
        signupForm.setManaged(true);

        // Hide login form
        loginForm.setVisible(false);
        loginForm.setManaged(false);

        // Move indicator to signup tab (140px is the button width)
        moveIndicator(140);

        // Update active styles
        signupTabBtn.getStyleClass().add("active");
        loginTabBtn.getStyleClass().remove("active");

        // Clear error messages
        clearErrors();
        playSignupBorderAnimation();
    }
    @FXML
    private void openForgotPassword() {
        SceneManager.switchScene("forgot-pass.fxml", "Forgot Password");
    }

    /**
     * Animate the tab indicator
     */
    private void moveIndicator(double toX) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), tabIndicator);
        transition.setToX(toX);
        transition.play();
    }

    // ===== LOGIN LOGIC =====

    @FXML
    private void handleLogin() {
        // Validate fields
        if (!validateFields(loginNameOrEmail, loginPassword)) {
            showError(loginErrorLabel, "All fields must be filled.");
            return;
        }

        String identity = loginNameOrEmail.getText().trim();
        String inputPassword = loginPassword.getText();

        System.out.println("log innn");

        // Check backend connection
        System.out.println(BackendConnectionValidation.canConnectToServer());
        if (BackendConnectionValidation.canConnectToServer()) {
            // Online login
            CreateRequestController.handleLogin(new LoginRequest(identity, inputPassword), this);
        } else {
            System.out.println("Offline...");
            // Offline login - validate against local storage
            LocalDataManager.initDataPathForOffline(identity);
            String savedUsername = LocalDataManager.getUserName();
            String savedEmail = LocalDataManager.getUserEmail();
            String savedPass = LocalDataManager.getKey();

            if (identity.equals(savedEmail) || !savedPass.isEmpty()) {
                UserSession.init(savedUsername,savedEmail,savedPass);
                OfflineLogin();
            } else {
                showError(loginErrorLabel, "No account found locally. Please ensure you signed up online first.");
            }
        }
    }

    // ===== SIGNUP LOGIC =====

    @FXML
    private void handleSignup() {
        // Validate fields
        if (!validateFields(signupName, signupEmail, signupPassword)) {
            showError(signupErrorLabel, "All fields must be filled.");
            return;
        }

        // Validate terms checkbox
        if (termsCheckbox != null && !termsCheckbox.isSelected()) {
            showError(signupErrorLabel, "You must agree to the Terms & Conditions.");
            return;
        }

        String username = signupName.getText().trim();
        String email = signupEmail.getText().trim();
        String password = signupPassword.getText();
        String securityQuestion = signupSecurityQuestion.getText().trim();
        String securityAnswer = signupSecurityAnswer.getText().trim();


        System.out.println("pass: ");

        // Validate password length
        if (password.length() < 6 || password.length() > 20) {
            showError(signupErrorLabel, "Password must be 6-20 characters.");
            return;
        }

        // Validate email format
        if (!isValidEmail(email)) {
            showError(signupErrorLabel, "Please enter a valid email address.");
            return;
        }
        SignupDetails req = new SignupDetails(username, email, password, securityQuestion, securityAnswer.toLowerCase());
        CreateRequestController.handleSignUp(req, this);
    }

    public void loginSuccessful() {
        // Navigate to dashboard
        //SceneManager.switchScene("dashboard-view.fxml", "Dashboard");
    }

    private void OfflineLogin(){
        GroupDataManager.init();
    }


    // ===== VALIDATION HELPERS =====

    private boolean validateFields(TextField... fields) {
        for (TextField field : fields) {
            if (field.getText() == null || field.getText().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidEmail(String email) {
        // Simple email validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    // ===== ERROR HANDLING =====

    private void showError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void clearErrors() {
        if (loginErrorLabel != null) {
            loginErrorLabel.setVisible(false);
            loginErrorLabel.setManaged(false);
            loginErrorLabel.setText("");
        }
        if (signupErrorLabel != null) {
            signupErrorLabel.setVisible(false);
            signupErrorLabel.setManaged(false);
            signupErrorLabel.setText("");
        }
    }


    private void playSignupBorderAnimation() {
        if (formCard == null) return;

        stopSignupBorderAnimation();

        // 🔹 very soft base shadow
        DropShadow baseShadow = new DropShadow();
        baseShadow.setColor(Color.rgb(80, 60, 140, 0.9));
        baseShadow.setRadius(26);
        baseShadow.setOffsetX(0);
        baseShadow.setOffsetY(5);

        // 🔹 soft glow (less visible)
        DropShadow glow = new DropShadow();
        glow.setRadius(50);
        glow.setSpread(0.5);
        glow.setOffsetX(0);
        glow.setOffsetY(0);
        glow.setInput(baseShadow);

        formCard.setEffect(glow);

        borderAnimation = new Timeline(

                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.colorProperty(), Color.web("#c4b5fd", 0.35)), // light purple
                        new KeyValue(glow.radiusProperty(), 18),
                        new KeyValue(glow.spreadProperty(), 0.09)
                ),

                new KeyFrame(Duration.seconds(2),
                        new KeyValue(glow.colorProperty(), Color.web("#f9a8d4", 0.50)), // light pink
                        new KeyValue(glow.radiusProperty(), 22),
                        new KeyValue(glow.spreadProperty(), 0.09)
                ),

                new KeyFrame(Duration.seconds(4),
                        new KeyValue(glow.colorProperty(), Color.web("#93c5fd", 0.50)), // light blue
                        new KeyValue(glow.radiusProperty(), 20),
                        new KeyValue(glow.spreadProperty(), 0.08)
                ),

                new KeyFrame(Duration.seconds(6),
                        new KeyValue(glow.colorProperty(), Color.web("#c4b5fd", 0.35)),
                        new KeyValue(glow.radiusProperty(), 18),
                        new KeyValue(glow.spreadProperty(), 0.07)
                )
        );

        borderAnimation.setCycleCount(Animation.INDEFINITE);

        borderAnimation.play();

    }

    private void stopSignupBorderAnimation() {
        if (borderAnimation != null) {
            borderAnimation.stop();
            borderAnimation = null;
        }

        if (formCard != null) {
            formCard.setEffect(null);
        }
    }

}
