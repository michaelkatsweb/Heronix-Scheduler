package com.heronix.ui.controller;

import com.heronix.model.domain.User;
import com.heronix.model.enums.Role;
import com.heronix.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * User Dialog Controller
 * Handles user creation and editing
 *
 * Features:
 * - User information form
 * - Password validation with real-time feedback
 * - Role selection with descriptions
 * - Account settings
 * - Duplicate username/email checking
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/UserDialogController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Slf4j
@Component
public class UserDialogController {

    // ========================================================================
    // FXML FIELDS
    // ========================================================================

    @FXML private Label titleLabel;

    @FXML private TextField usernameField;
    @FXML private Label usernameErrorLabel;

    @FXML private TextField fullNameField;

    @FXML private TextField emailField;
    @FXML private Label emailErrorLabel;

    @FXML private TextField phoneField;

    @FXML private ComboBox<String> roleCombo;
    @FXML private Label roleDescriptionLabel;

    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordErrorLabel;

    @FXML private Label req1Label;
    @FXML private Label req2Label;
    @FXML private Label req3Label;
    @FXML private Label req4Label;
    @FXML private Label req5Label;

    @FXML private CheckBox enabledCheckBox;
    @FXML private CheckBox accountNonExpiredCheckBox;
    @FXML private CheckBox accountNonLockedCheckBox;
    @FXML private CheckBox credentialsNonExpiredCheckBox;

    @FXML private Label errorMessageLabel;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    @Autowired
    private UserService userService;

    // ========================================================================
    // STATE VARIABLES
    // ========================================================================

    private Stage dialogStage;
    private User user;
    private boolean editMode = false;
    private boolean saved = false;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing User Dialog Controller");

        // Populate role combo
        roleCombo.getItems().addAll(
            "SUPER_ADMIN",
            "ADMIN",
            "PRINCIPAL",
            "COUNSELOR",
            "TEACHER",
            "STAFF",
            "PARENT"
        );

        // Setup role combo listener
        roleCombo.valueProperty().addListener((obs, old, newVal) -> updateRoleDescription(newVal));

        // Setup password field listeners for real-time validation
        passwordField.textProperty().addListener((obs, old, newVal) -> validatePasswordStrength(newVal));
        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> validatePasswordMatch());

        // Setup username field listener
        usernameField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                // Convert to lowercase and remove spaces
                String cleaned = newVal.toLowerCase().replaceAll("\\s+", "");
                if (!newVal.equals(cleaned)) {
                    usernameField.setText(cleaned);
                }
            }
            clearError(usernameErrorLabel);
        });

        // Setup email field listener
        emailField.textProperty().addListener((obs, old, newVal) -> clearError(emailErrorLabel));

        // Set default role
        roleCombo.setValue("TEACHER");
    }

    /**
     * Setup for creating a new user
     */
    public void setupForCreate() {
        editMode = false;
        titleLabel.setText("Create User");
        passwordField.setDisable(false);
        confirmPasswordField.setDisable(false);
        usernameField.setDisable(false);
    }

    /**
     * Setup for editing an existing user
     */
    public void setupForEdit(User user) {
        this.user = user;
        editMode = true;
        titleLabel.setText("Edit User: " + user.getUsername());

        // Populate fields
        usernameField.setText(user.getUsername());
        usernameField.setDisable(true); // Username cannot be changed

        fullNameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhoneNumber());

        if (user.getPrimaryRole() != null) {
            roleCombo.setValue(user.getPrimaryRole().name());
        }

        enabledCheckBox.setSelected(user.getEnabled());
        accountNonExpiredCheckBox.setSelected(user.getAccountNonExpired());
        accountNonLockedCheckBox.setSelected(user.getAccountNonLocked());
        credentialsNonExpiredCheckBox.setSelected(user.getCredentialsNonExpired());

        // In edit mode, password is optional
        passwordField.setPromptText("Leave blank to keep current password");
        confirmPasswordField.setPromptText("Leave blank to keep current password");
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Validate all fields
     */
    private boolean validateFields() {
        clearAllErrors();

        boolean valid = true;

        // Validate username
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showError(usernameErrorLabel, "Username is required");
            valid = false;
        } else if (!editMode && userService.userExists(username)) {
            showError(usernameErrorLabel, "Username already exists");
            valid = false;
        } else if (!username.matches("[a-z0-9_]+")) {
            showError(usernameErrorLabel, "Username can only contain lowercase letters, numbers, and underscores");
            valid = false;
        }

        // Validate full name
        if (fullNameField.getText().trim().isEmpty()) {
            showErrorMessage("Full name is required");
            valid = false;
        }

        // Validate email
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showError(emailErrorLabel, "Email is required");
            valid = false;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError(emailErrorLabel, "Invalid email format");
            valid = false;
        } else if (!editMode && userService.emailExists(email)) {
            showError(emailErrorLabel, "Email already exists");
            valid = false;
        } else if (editMode && !email.equalsIgnoreCase(user.getEmail()) && userService.emailExists(email)) {
            showError(emailErrorLabel, "Email already exists");
            valid = false;
        }

        // Validate role
        if (roleCombo.getValue() == null) {
            showErrorMessage("Role is required");
            valid = false;
        }

        // Validate password (required for new users, optional for edit)
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!editMode || !password.isEmpty()) {
            if (password.isEmpty()) {
                showError(passwordErrorLabel, "Password is required");
                valid = false;
            } else if (!userService.isPasswordValid(password)) {
                showError(passwordErrorLabel, "Password does not meet requirements");
                valid = false;
            } else if (!password.equals(confirmPassword)) {
                showError(passwordErrorLabel, "Passwords do not match");
                valid = false;
            }
        }

        return valid;
    }

    /**
     * Validate password strength and update UI
     */
    private void validatePasswordStrength(String password) {
        if (password == null) password = "";

        // Requirement 1: At least 8 characters
        boolean req1 = password.length() >= 8;
        updateRequirement(req1Label, req1);

        // Requirement 2: At least one uppercase
        boolean req2 = Pattern.compile("[A-Z]").matcher(password).find();
        updateRequirement(req2Label, req2);

        // Requirement 3: At least one lowercase
        boolean req3 = Pattern.compile("[a-z]").matcher(password).find();
        updateRequirement(req3Label, req3);

        // Requirement 4: At least one digit
        boolean req4 = Pattern.compile("[0-9]").matcher(password).find();
        updateRequirement(req4Label, req4);

        // Requirement 5: At least one special character
        boolean req5 = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find();
        updateRequirement(req5Label, req5);

        validatePasswordMatch();
    }

    /**
     * Validate password match
     */
    private void validatePasswordMatch() {
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!password.isEmpty() && !confirmPassword.isEmpty()) {
            if (!password.equals(confirmPassword)) {
                showError(passwordErrorLabel, "Passwords do not match");
            } else {
                clearError(passwordErrorLabel);
            }
        } else {
            clearError(passwordErrorLabel);
        }
    }

    /**
     * Update requirement label
     */
    private void updateRequirement(Label label, boolean met) {
        if (met) {
            label.setText(label.getText().replaceFirst("✗", "✓"));
            label.setStyle("-fx-font-size: 11px; -fx-text-fill: #4CAF50;");
        } else {
            label.setText(label.getText().replaceFirst("✓", "✗"));
            label.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
        }
    }

    /**
     * Update role description
     */
    private void updateRoleDescription(String roleStr) {
        if (roleStr == null) return;

        try {
            Role role = Role.valueOf(roleStr);
            roleDescriptionLabel.setText(role.getDescription());
        } catch (IllegalArgumentException e) {
            roleDescriptionLabel.setText("");
        }
    }

    // ========================================================================
    // ACTION HANDLERS
    // ========================================================================

    /**
     * Handle save
     */
    @FXML
    private void handleSave() {
        log.info("Save button clicked");

        if (!validateFields()) {
            return;
        }

        try {
            if (editMode) {
                updateUser();
            } else {
                createUser();
            }

            saved = true;
            if (dialogStage != null) {
                dialogStage.close();
            }

        } catch (Exception e) {
            log.error("Error saving user", e);
            showErrorMessage("Failed to save user: " + e.getMessage());
        }
    }

    /**
     * Create new user
     */
    private void createUser() {
        log.info("Creating new user");

        User newUser = User.builder()
                .username(usernameField.getText().trim())
                .fullName(fullNameField.getText().trim())
                .email(emailField.getText().trim())
                .phoneNumber(phoneField.getText().trim())
                .primaryRole(Role.valueOf(roleCombo.getValue()))
                .enabled(enabledCheckBox.isSelected())
                .accountNonExpired(accountNonExpiredCheckBox.isSelected())
                .accountNonLocked(accountNonLockedCheckBox.isSelected())
                .credentialsNonExpired(credentialsNonExpiredCheckBox.isSelected())
                .build();

        // Add legacy roles for backward compatibility
        newUser.addRole("ROLE_USER");
        if (newUser.getPrimaryRole() == Role.SUPER_ADMIN || newUser.getPrimaryRole() == Role.ADMIN) {
            newUser.addRole("ROLE_ADMIN");
        }

        userService.createUser(newUser, passwordField.getText());

        log.info("User created successfully: {}", newUser.getUsername());
    }

    /**
     * Update existing user
     */
    private void updateUser() {
        log.info("Updating user: {}", user.getUsername());

        user.setFullName(fullNameField.getText().trim());
        user.setEmail(emailField.getText().trim());
        user.setPhoneNumber(phoneField.getText().trim());
        user.setPrimaryRole(Role.valueOf(roleCombo.getValue()));
        user.setEnabled(enabledCheckBox.isSelected());
        user.setAccountNonExpired(accountNonExpiredCheckBox.isSelected());
        user.setAccountNonLocked(accountNonLockedCheckBox.isSelected());
        user.setCredentialsNonExpired(credentialsNonExpiredCheckBox.isSelected());

        // Update password if provided
        String newPassword = passwordField.getText();
        if (!newPassword.isEmpty()) {
            user.setPassword(newPassword); // Will be encrypted by service
            userService.changePassword(user.getId(), "", newPassword); // Empty old password for admin reset
        }

        userService.updateUser(user);

        log.info("User updated successfully: {}", user.getUsername());
    }

    /**
     * Handle cancel
     */
    @FXML
    private void handleCancel() {
        log.info("Cancel button clicked");
        saved = false;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    // ========================================================================
    // ERROR HANDLING
    // ========================================================================

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearError(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    private void showErrorMessage(String message) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(true);
        errorMessageLabel.setManaged(true);
    }

    private void clearAllErrors() {
        clearError(usernameErrorLabel);
        clearError(emailErrorLabel);
        clearError(passwordErrorLabel);
        errorMessageLabel.setVisible(false);
        errorMessageLabel.setManaged(false);
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaved() {
        return saved;
    }
}
