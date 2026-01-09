package com.heronix.ui.controller;

import com.heronix.model.domain.Student;
import com.heronix.repository.StudentRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.print.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for Student QR Code Print Dialog
 *
 * Displays student's unique QR code for:
 * - Printing on student ID cards
 * - Attendance scanning
 * - Authentication verification
 *
 * Features:
 * - Generate/regenerate QR codes
 * - Multiple size options
 * - Print to printer
 * - Save as PNG image
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025
 */
@Slf4j
@Component
public class StudentQRCodeDialogController {

    @Autowired
    private StudentRepository studentRepository;

    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label qrCodeIdLabel;
    @FXML private Label generatedDateLabel;
    @FXML private Label statusLabel;

    @FXML private ImageView qrCodeImageView;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML private RadioButton sizeSmallRadio;
    @FXML private RadioButton sizeMediumRadio;
    @FXML private RadioButton sizeLargeRadio;
    @FXML private ToggleGroup sizeToggleGroup;

    @FXML private ComboBox<String> printLayoutCombo;
    @FXML private javafx.scene.layout.HBox customSizeBox;
    @FXML private TextField customSizeField;

    @FXML private Button regenerateButton;
    @FXML private Button saveButton;
    @FXML private Button printButton;
    @FXML private Button closeButton;

    @Setter
    private Stage dialogStage;

    private Student student;
    private byte[] currentQRCodeImage;

    /**
     * Initialize the controller
     */
    @FXML
    private void initialize() {
        log.info("Initializing Student QR Code Dialog Controller");

        // Set up size change listener
        sizeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && student != null) {
                generateQRCode();
            }
        });

        // Set up print layout options
        printLayoutCombo.getItems().addAll(
            "ID Card (25mm × 25mm)",
            "Small Badge (50mm × 50mm)",
            "Standard Card (85mm × 54mm - Credit Card Size)",
            "Half Page (100mm × 100mm)",
            "Quarter Page A4 (70mm × 70mm)",
            "Full Page A4 (150mm × 150mm)",
            "Custom Size..."
        );
        printLayoutCombo.setValue("Standard Card (85mm × 54mm - Credit Card Size)");

        // Show/hide custom size input based on selection
        printLayoutCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCustom = newVal != null && newVal.equals("Custom Size...");
            customSizeBox.setVisible(isCustom);
            customSizeBox.setManaged(isCustom);
        });

        // Default custom size
        customSizeField.setText("50");
    }

    /**
     * Set the student for this dialog
     */
    public void setStudent(Student student) {
        this.student = student;

        if (student != null) {
            // Update labels
            studentNameLabel.setText(student.getFirstName() + " " + student.getLastName());
            studentIdLabel.setText("Student ID: " + student.getStudentId());

            // Ensure student has QR code ID
            if (student.getQrCodeId() == null || student.getQrCodeId().isEmpty()) {
                generateNewQRCodeId();
            } else {
                qrCodeIdLabel.setText(student.getQrCodeId());
            }

            // Set generated date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            generatedDateLabel.setText("Generated: " + LocalDateTime.now().format(formatter));

            // Generate QR code
            generateQRCode();
        }
    }

    /**
     * Generate QR code image
     */
    private void generateQRCode() {
        if (student == null) return;

        loadingIndicator.setVisible(true);
        qrCodeImageView.setVisible(false);

        // Run in background thread
        new Thread(() -> {
            try {
                // Get selected size
                int size = getSelectedSize();

                // Generate QR code content
                // Format: STUDENT_ID:EMAIL:QR_CODE_ID
                String qrContent = String.format("%s:%s:%s",
                    student.getStudentId(),
                    student.getEmail() != null ? student.getEmail() : "",
                    student.getQrCodeId());

                // Generate QR code image
                currentQRCodeImage = generateQRCodeImage(qrContent, size, size);

                // Convert to JavaFX image
                ByteArrayInputStream bis = new ByteArrayInputStream(currentQRCodeImage);
                Image image = new Image(bis);

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    qrCodeImageView.setImage(image);
                    qrCodeImageView.setVisible(true);
                    loadingIndicator.setVisible(false);

                    log.info("QR code generated for student: {} (size: {}px)",
                        student.getStudentId(), size);
                });

            } catch (Exception e) {
                log.error("Error generating QR code", e);
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    showError("Failed to generate QR code: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Get selected QR code size
     */
    private int getSelectedSize() {
        if (sizeSmallRadio.isSelected()) return 200;
        if (sizeLargeRadio.isSelected()) return 400;
        return 300; // Medium (default)
    }

    /**
     * Generate new QR code ID for student
     */
    private void generateNewQRCodeId() {
        String shortUUID = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String newQRCodeId = String.format("QR-%s-%s", student.getStudentId(), shortUUID);

        student.setQrCodeId(newQRCodeId);
        studentRepository.save(student);

        qrCodeIdLabel.setText(newQRCodeId);
        log.info("Generated new QR code ID: {}", newQRCodeId);
    }

    /**
     * Generate QR code image as byte array
     */
    private byte[] generateQRCodeImage(String content, int width, int height)
            throws WriterException, IOException {

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

        return outputStream.toByteArray();
    }

    /**
     * Handle regenerate button click
     */
    @FXML
    private void handleRegenerate() {
        log.info("Regenerating QR code for student: {}", student.getStudentId());

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Regenerate QR Code");
        confirmation.setHeaderText("Regenerate QR Code?");
        confirmation.setContentText("This will generate a new QR code ID. The old QR code will no longer work.\n\nAre you sure?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                generateNewQRCodeId();
                generateQRCode();
                showSuccess("QR code regenerated successfully");
            }
        });
    }

    /**
     * Handle save button click
     */
    @FXML
    private void handleSave() {
        if (currentQRCodeImage == null) {
            showError("No QR code to save. Please generate a QR code first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save QR Code Image");
        fileChooser.setInitialFileName(student.getStudentId() + "-QRCode.png");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PNG Images", "*.png")
        );

        File file = fileChooser.showSaveDialog(dialogStage);

        if (file != null) {
            try {
                // Convert byte array to BufferedImage
                ByteArrayInputStream bis = new ByteArrayInputStream(currentQRCodeImage);
                BufferedImage bufferedImage = ImageIO.read(bis);

                // Save to file
                ImageIO.write(bufferedImage, "PNG", file);

                showSuccess("QR code saved to: " + file.getAbsolutePath());
                log.info("QR code saved to: {}", file.getAbsolutePath());

            } catch (IOException e) {
                log.error("Error saving QR code image", e);
                showError("Failed to save QR code: " + e.getMessage());
            }
        }
    }

    /**
     * Handle print button click
     */
    @FXML
    private void handlePrint() {
        if (qrCodeImageView.getImage() == null) {
            showError("No QR code to print. Please generate a QR code first.");
            return;
        }

        log.info("Printing QR code for student: {}", student.getStudentId());

        // Create printer job
        PrinterJob printerJob = PrinterJob.createPrinterJob();

        if (printerJob != null) {
            // Show print dialog
            boolean proceed = printerJob.showPrintDialog(dialogStage);

            if (proceed) {
                // Create printable node
                VBox printNode = createPrintNode();

                // Get page layout
                PageLayout pageLayout = printerJob.getJobSettings().getPageLayout();

                // Scale to fit page
                double scaleX = pageLayout.getPrintableWidth() / printNode.getBoundsInParent().getWidth();
                double scaleY = pageLayout.getPrintableHeight() / printNode.getBoundsInParent().getHeight();
                double scale = Math.min(scaleX, scaleY);

                Scale scaleTransform = new Scale(scale, scale);
                printNode.getTransforms().add(scaleTransform);

                // Print
                boolean printed = printerJob.printPage(printNode);

                if (printed) {
                    printerJob.endJob();
                    showSuccess("QR code printed successfully");
                    log.info("QR code printed successfully");
                } else {
                    showError("Failed to print QR code");
                }
            }
        } else {
            showError("No printer available");
        }
    }

    /**
     * Create printable node with student info and QR code based on selected layout
     */
    private VBox createPrintNode() {
        String selectedLayout = printLayoutCombo.getValue();
        int qrSizeMM = getQRSizeInMM(selectedLayout);

        // Convert MM to points (1 mm = 2.83465 points)
        double qrSizePoints = qrSizeMM * 2.83465;

        VBox printBox = new VBox(10);
        printBox.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-alignment: center;");

        // Determine layout style based on size
        if (qrSizeMM <= 30) {
            // ID Card layout - minimal text, small QR
            return createIDCardLayout(qrSizePoints);
        } else if (qrSizeMM <= 70) {
            // Badge/Card layout - compact
            return createCompactLayout(qrSizePoints);
        } else {
            // Full page layout - detailed
            return createFullPageLayout(qrSizePoints);
        }
    }

    /**
     * ID Card layout - minimal, fits on small badge
     */
    private VBox createIDCardLayout(double qrSizePoints) {
        VBox printBox = new VBox(5);
        printBox.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-alignment: center;");

        // Student name (small)
        Label nameLabel = new Label(student.getFirstName() + " " + student.getLastName());
        nameLabel.setStyle("-fx-font-size: 8pt; -fx-font-weight: bold;");

        // QR code image (small)
        ImageView printImageView = new ImageView(qrCodeImageView.getImage());
        printImageView.setFitWidth(qrSizePoints);
        printImageView.setFitHeight(qrSizePoints);
        printImageView.setPreserveRatio(true);

        // Student ID (tiny)
        Label idLabel = new Label(student.getStudentId());
        idLabel.setStyle("-fx-font-size: 6pt;");

        printBox.getChildren().addAll(nameLabel, printImageView, idLabel);
        return printBox;
    }

    /**
     * Compact layout - for badges and standard cards
     */
    private VBox createCompactLayout(double qrSizePoints) {
        VBox printBox = new VBox(8);
        printBox.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-alignment: center;");

        // Student name
        Label nameLabel = new Label(student.getFirstName() + " " + student.getLastName());
        nameLabel.setStyle("-fx-font-size: 12pt; -fx-font-weight: bold;");

        // Student ID
        Label idLabel = new Label("ID: " + student.getStudentId());
        idLabel.setStyle("-fx-font-size: 10pt;");

        // QR code image
        ImageView printImageView = new ImageView(qrCodeImageView.getImage());
        printImageView.setFitWidth(qrSizePoints);
        printImageView.setFitHeight(qrSizePoints);
        printImageView.setPreserveRatio(true);

        // QR code ID (small)
        Label qrIdLabel = new Label(student.getQrCodeId());
        qrIdLabel.setStyle("-fx-font-size: 8pt; -fx-text-fill: gray;");

        printBox.getChildren().addAll(nameLabel, idLabel, printImageView, qrIdLabel);
        return printBox;
    }

    /**
     * Full page layout - detailed information
     */
    private VBox createFullPageLayout(double qrSizePoints) {
        VBox printBox = new VBox(20);
        printBox.setStyle("-fx-padding: 40; -fx-background-color: white; -fx-alignment: center;");

        // Title
        Label titleLabel = new Label("Student QR Code");
        titleLabel.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold;");

        // Student info
        Label nameLabel = new Label(student.getFirstName() + " " + student.getLastName());
        nameLabel.setStyle("-fx-font-size: 18pt;");

        Label idLabel = new Label("ID: " + student.getStudentId());
        idLabel.setStyle("-fx-font-size: 14pt;");

        // Grade level if available
        if (student.getGradeLevel() != null) {
            Label gradeLabel = new Label("Grade: " + student.getGradeLevel());
            gradeLabel.setStyle("-fx-font-size: 12pt; -fx-text-fill: gray;");
            printBox.getChildren().add(gradeLabel);
        }

        // QR code image
        ImageView printImageView = new ImageView(qrCodeImageView.getImage());
        printImageView.setFitWidth(qrSizePoints);
        printImageView.setFitHeight(qrSizePoints);
        printImageView.setPreserveRatio(true);

        // QR code ID
        Label qrIdLabel = new Label("QR Code ID: " + student.getQrCodeId());
        qrIdLabel.setStyle("-fx-font-size: 12pt;");

        // Instructions
        Label instructionsLabel = new Label("Scan this QR code for attendance and authentication");
        instructionsLabel.setStyle("-fx-font-size: 10pt; -fx-text-fill: gray;");

        printBox.getChildren().addAll(
            titleLabel,
            nameLabel,
            idLabel,
            printImageView,
            qrIdLabel,
            instructionsLabel
        );

        return printBox;
    }

    /**
     * Get QR code size in millimeters based on selected layout
     */
    private int getQRSizeInMM(String layout) {
        if (layout == null) return 70; // Default

        if (layout.contains("Custom")) {
            try {
                return Integer.parseInt(customSizeField.getText());
            } catch (NumberFormatException e) {
                return 50; // Default custom size
            }
        }

        // Extract size from layout string
        if (layout.contains("25mm")) return 25;
        if (layout.contains("50mm")) return 50;
        if (layout.contains("85mm")) return 70; // QR code size on standard card
        if (layout.contains("100mm")) return 100;
        if (layout.contains("70mm")) return 70;
        if (layout.contains("150mm")) return 150;

        return 70; // Default
    }

    /**
     * Handle close button click
     */
    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #10B981;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);

        // Hide after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    statusLabel.setVisible(false);
                    statusLabel.setManaged(false);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("QR Code Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
