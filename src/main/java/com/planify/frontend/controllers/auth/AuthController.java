package com.planify.frontend.controllers.auth;

import com.planify.frontend.controllers.Request.CreateRequestController;
import com.planify.frontend.models.auth.LoginRequest;
import com.planify.frontend.models.auth.SignupDetails;
import com.planify.frontend.network.BackendConnectionValidation;
import com.planify.frontend.utils.managers.LocalDataManager;
import com.planify.frontend.utils.PasswordHasher;
import com.planify.frontend.utils.managers.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.animation.TranslateTransition;
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

        // Check backend connection
        if (BackendConnectionValidation.canConnectToServer()) {
            // Online login
            CreateRequestController.handleLogin(new LoginRequest(identity, inputPassword), this);
        } else {
            // Offline login - validate against local storage
            String savedUsername = LocalDataManager.getUserName();
            String savedEmail = LocalDataManager.getUserEmail();
            String savedPass = LocalDataManager.getKey();

            if ((identity.equals(savedEmail) || identity.equals(savedUsername)) && inputPassword.equals(savedPass)) {
                System.out.println("Validating locally for offline access...");
                loginSuccessful();
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

        // Hash password and create account
        String hashedPassword = PasswordHasher.hashPassword(password);
        SignupDetails req = new SignupDetails(username, email, hashedPassword);
        CreateRequestController.handleSignUp(req, this);
    }

    // ===== SUCCESS HANDLERS =====

    public void signUpSuccessful() {
        String username = signupName.getText().trim();
        String email = signupEmail.getText().trim();
        String password = signupPassword.getText();
        String hashedPassword = PasswordHasher.hashPassword(password);

        // Save locally for offline access
        LocalDataManager.saveUserDataLocally(username, email, hashedPassword);

        // Navigate to dashboard
        SceneManager.switchScene("dashboard-view.fxml", "Dashboard");
    }

    public void loginSuccessful() {
        // Navigate to dashboard
        //SceneManager.switchScene("dashboard-view.fxml", "Dashboard");
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

    public void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
