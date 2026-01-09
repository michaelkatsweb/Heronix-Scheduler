// Location: src/main/java/com/eduscheduler/ui/dialog/LoginDialog.java
package com.heronix.ui.dialog;

import com.heronix.model.domain.User;
import com.heronix.service.AuthenticationService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import lombok.extern.slf4j.Slf4j;

/**
 * Login Dialog for JavaFX Application
 * Provides username/password authentication
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
@Slf4j
public class LoginDialog extends Dialog<User> {

    private final AuthenticationService authenticationService;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Label errorLabel;

    public LoginDialog(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;

        setTitle("Heronix Scheduling System - Login");
        setHeaderText("Please log in to continue");

        // Create the dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Title
        Label titleLabel = new Label("Heronix Scheduling System");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));

        // Username
        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setPrefWidth(250);

        // Password
        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefWidth(250);

        // Error label
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);

        // Info label
        Label infoLabel = new Label("Default credentials:\nUsername: admin | Password: admin123");
        infoLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");

        // Add components to grid
        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(errorLabel, 1, 2);
        grid.add(infoLabel, 1, 3);

        // Wrap in VBox
        VBox content = new VBox(10);
        content.getChildren().addAll(titleLabel, grid);
        content.setPadding(new Insets(10));

        getDialogPane().setContent(content);

        // Add buttons
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(loginButtonType, cancelButtonType);

        // Get login button
        Button loginButton = (Button) getDialogPane().lookupButton(loginButtonType);
        loginButton.setDefaultButton(true);

        // Set focus to username field
        Platform.runLater(() -> usernameField.requestFocus());

        // Handle Enter key in password field
        passwordField.setOnAction(e -> {
            if (!usernameField.getText().trim().isEmpty() && !passwordField.getText().isEmpty()) {
                loginButton.fire();
            }
        });

        // Convert result
        setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return performLogin();
            }
            return null;
        });

        // Disable login button if fields are empty
        loginButton.disableProperty().bind(
                usernameField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );
    }

    /**
     * Perform login authentication
     */
    private User performLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        try {
            log.info("Attempting login for user: {}", username);
            User user = authenticationService.authenticate(username, password);
            log.info("Login successful for user: {}", username);
            return user;

        } catch (IllegalArgumentException e) {
            log.warn("Login failed for user: {} - {}", username, e.getMessage());

            // Show error message
            errorLabel.setText(e.getMessage());
            errorLabel.setVisible(true);

            // Clear password field
            passwordField.clear();
            passwordField.requestFocus();

            // Return null to keep dialog open
            return null;
        }
    }

    /**
     * Show login dialog and wait for result
     *
     * @return Authenticated user or null if login was cancelled
     */
    public static User showLoginDialog(AuthenticationService authenticationService) {
        LoginDialog dialog = new LoginDialog(authenticationService);

        // Keep showing dialog until successful login or cancel
        while (true) {
            User user = dialog.showAndWait().orElse(null);

            if (user != null) {
                // Successful login
                return user;
            }

            // Check if cancel button was clicked
            if (dialog.getResult() != null) {
                // Login failed but user didn't click cancel - try again
                continue;
            }

            // Cancel was clicked
            return null;
        }
    }
}
