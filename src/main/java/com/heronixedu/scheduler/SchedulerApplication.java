package com.heronixedu.scheduler;

import com.heronixedu.model.User;
import com.heronixedu.util.TokenReader;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Heronix Class Scheduler
 * Demonstrates SSO integration with Heronix-Hub
 */
public class SchedulerApplication extends Application {

    @Override
    public void start(Stage stage) {
        // CHECK FOR SSO TOKEN - This is the key SSO integration!
        if (TokenReader.hasValidToken()) {
            User user = TokenReader.getUserFromToken();
            showMainScreen(stage, user);
        } else {
            showNoTokenScreen(stage);
        }
    }

    private void showMainScreen(Stage stage, User user) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #F3E5F5, #FFFFFF);");

        // Title
        Label title = new Label("HERONIX SCHEDULER");
        title.setFont(Font.font("System", FontWeight.BOLD, 36));
        title.setTextFill(Color.web("#6A1B9A"));

        // Subtitle
        Label subtitle = new Label("Class Scheduler");
        subtitle.setFont(Font.font("System", 16));
        subtitle.setTextFill(Color.web("#666666"));

        // Welcome message
        Label welcomeLabel = new Label("Welcome, " + user.getFullName() + "!");
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        welcomeLabel.setTextFill(Color.web("#7B1FA2"));

        // Role badge
        Label roleLabel = new Label("Role: " + user.getRole());
        roleLabel.setFont(Font.font("System", 14));
        roleLabel.setStyle(
            "-fx-background-color: #9C27B0; " +
            "-fx-text-fill: white; " +
            "-fx-padding: 8px 16px; " +
            "-fx-background-radius: 16px;"
        );

        // SSO indicator
        Label ssoLabel = new Label("✓ Logged in via Single Sign-On");
        ssoLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        ssoLabel.setTextFill(Color.web("#4CAF50"));

        // Info box
        VBox infoBox = new VBox(10);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setPadding(new Insets(20));
        infoBox.setStyle(
            "-fx-background-color: #F3E5F5; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: #9C27B0; " +
            "-fx-border-radius: 8px; " +
            "-fx-border-width: 2px;"
        );
        infoBox.setMaxWidth(500);

        Label infoTitle = new Label("SSO Demo Application");
        infoTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label infoText = new Label(
            "This is a placeholder application demonstrating Heronix-Hub SSO.\n\n" +
            "You were automatically logged in using your Heronix-Hub token!\n" +
            "No password needed - seamless experience across all Heronix products."
        );
        infoText.setWrapText(true);
        infoText.setMaxWidth(460);
        infoText.setFont(Font.font("System", 13));

        infoBox.getChildren().addAll(infoTitle, infoText);

        // Feature buttons (placeholders)
        VBox featuresBox = new VBox(10);
        featuresBox.setAlignment(Pos.CENTER);

        Label featuresLabel = new Label("Scheduler Features (Demo):");
        featuresLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        Button classesBtn = createFeatureButton("View Classes");
        Button scheduleBtn = createFeatureButton("Create Schedule");
        Button conflictsBtn = createFeatureButton("Check Conflicts");
        Button reportsBtn = createFeatureButton("Generate Reports");

        featuresBox.getChildren().addAll(
            featuresLabel,
            classesBtn,
            scheduleBtn,
            conflictsBtn,
            reportsBtn
        );

        // Close button
        Button closeButton = new Button("Close Application");
        closeButton.setStyle(
            "-fx-background-color: #9C27B0; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12px 30px; " +
            "-fx-background-radius: 4px;"
        );
        closeButton.setOnAction(e -> stage.close());

        root.getChildren().addAll(
            title, subtitle, welcomeLabel, roleLabel, ssoLabel,
            infoBox, featuresBox, closeButton
        );

        Scene scene = new Scene(root, 700, 700);
        stage.setScene(scene);
        stage.setTitle("Heronix Scheduler - Class Scheduling System");
        stage.show();
    }

    private Button createFeatureButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(200);
        btn.setStyle(
            "-fx-background-color: #AB47BC; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-padding: 10px 20px; " +
            "-fx-background-radius: 4px;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #9C27B0; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-padding: 10px 20px; " +
            "-fx-background-radius: 4px;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #AB47BC; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-padding: 10px 20px; " +
            "-fx-background-radius: 4px;"
        ));
        btn.setOnAction(e -> System.out.println("Feature clicked: " + text));
        return btn;
    }

    private void showNoTokenScreen(Stage stage) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #FFEBEE, #FFFFFF);");

        Label title = new Label("HERONIX SCHEDULER");
        title.setFont(Font.font("System", FontWeight.BOLD, 36));
        title.setTextFill(Color.web("#C62828"));

        Label subtitle = new Label("Class Scheduler");
        subtitle.setFont(Font.font("System", 16));

        Label errorIcon = new Label("⚠");
        errorIcon.setFont(Font.font(48));
        errorIcon.setTextFill(Color.web("#FF9800"));

        Label message = new Label("No SSO Token Found");
        message.setFont(Font.font("System", FontWeight.BOLD, 20));

        Label instructions = new Label(
            "Please login through Heronix-Hub first to access this application.\n\n" +
            "Steps:\n" +
            "1. Close this window\n" +
            "2. Open Heronix-Hub\n" +
            "3. Login with your credentials\n" +
            "4. Launch Heronix-Scheduler from the dashboard"
        );
        instructions.setStyle("-fx-text-align: center;");
        instructions.setWrapText(true);
        instructions.setMaxWidth(400);

        Button closeButton = new Button("Close");
        closeButton.setStyle(
            "-fx-background-color: #F44336; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 10px 30px; " +
            "-fx-background-radius: 4px;"
        );
        closeButton.setOnAction(e -> stage.close());

        root.getChildren().addAll(title, subtitle, errorIcon, message, instructions, closeButton);

        Scene scene = new Scene(root, 600, 450);
        stage.setScene(scene);
        stage.setTitle("Heronix Scheduler - Authentication Required");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
