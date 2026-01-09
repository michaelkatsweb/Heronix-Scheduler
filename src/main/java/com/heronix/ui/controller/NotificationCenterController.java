package com.heronix.ui.controller;

import com.heronix.model.domain.Notification;
import com.heronix.service.NotificationService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Notification Center
 * Displays and manages system notifications with filtering
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/NotificationCenterController.java
 */
@Component
public class NotificationCenterController {

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private MainController mainController;

    // ========== FXML Components ==========

    // Header
    @FXML private Label unreadCountLabel;

    // Filters
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> priorityFilterComboBox;
    @FXML private CheckBox showReadCheckbox;
    @FXML private ComboBox<String> timeRangeComboBox;

    // Notification List
    @FXML private VBox notificationList;
    @FXML private Label emptyStateLabel;

    // Summary Stats
    @FXML private Label totalCountLabel;
    @FXML private Label unreadStatLabel;
    @FXML private Label criticalCountLabel;
    @FXML private Label warningCountLabel;

    // Status
    @FXML private Label statusLabel;

    // ========== State ==========

    private List<Notification> allNotifications;
    private List<Notification> filteredNotifications;
    private Long currentUserId;

    /**
     * Get current user ID from security context
     */
    private Long getCurrentUserId() {
        if (currentUserId == null) {
            try {
                java.util.Optional<com.heronix.model.domain.User> userOptional =
                    com.heronix.security.SecurityContext.getCurrentUser();
                if (userOptional.isPresent()) {
                    currentUserId = userOptional.get().getId();
                } else {
                    // Fallback to ID 1 if no user in security context (for development/testing)
                    currentUserId = 1L;
                }
            } catch (Exception e) {
                // Fallback to ID 1 if security context not available
                currentUserId = 1L;
            }
        }
        return currentUserId;
    }

    // ========== Initialization ==========

    @FXML
    public void initialize() {
        setupFilters();
        loadNotifications();
    }

    private void setupFilters() {
        // Type Filter
        typeFilterComboBox.getItems().clear();
        typeFilterComboBox.getItems().add("All Types");
        for (Notification.NotificationType type : Notification.NotificationType.values()) {
            typeFilterComboBox.getItems().add(type.getDisplayName());
        }
        typeFilterComboBox.setValue("All Types");

        // Priority Filter
        priorityFilterComboBox.getItems().clear();
        priorityFilterComboBox.getItems().addAll(
                "All Priorities",
                "Critical (4)",
                "High (3)",
                "Medium (2)",
                "Low (1)"
        );
        priorityFilterComboBox.setValue("All Priorities");
    }

    // ========== Data Loading ==========

    private void loadNotifications() {
        Task<List<Notification>> loadTask = new Task<>() {
            @Override
            protected List<Notification> call() {
                return notificationService.getNotificationsForUser(getCurrentUserId());
            }
        };

        loadTask.setOnSucceeded(event -> {
            allNotifications = loadTask.getValue();
            applyFilters();
            updateSummaryStats();
            displayNotifications();
        });

        loadTask.setOnFailed(event -> {
            showError("Failed to load notifications: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    private void applyFilters() {
        filteredNotifications = allNotifications.stream()
                .filter(this::matchesTypeFilter)
                .filter(this::matchesPriorityFilter)
                .filter(this::matchesReadFilter)
                .filter(this::matchesTimeRangeFilter)
                .sorted((n1, n2) -> {
                    // Sort: unread first, then by priority, then by date
                    if (n1.getIsRead() != n2.getIsRead()) {
                        return n1.getIsRead() ? 1 : -1;
                    }
                    if (!n1.getPriority().equals(n2.getPriority())) {
                        return n2.getPriority().compareTo(n1.getPriority());
                    }
                    return n2.getCreatedAt().compareTo(n1.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    private boolean matchesTypeFilter(Notification notification) {
        String selectedType = typeFilterComboBox.getValue();
        if (selectedType == null || selectedType.equals("All Types")) {
            return true;
        }
        return notification.getType().getDisplayName().equals(selectedType);
    }

    private boolean matchesPriorityFilter(Notification notification) {
        String selectedPriority = priorityFilterComboBox.getValue();
        if (selectedPriority == null || selectedPriority.equals("All Priorities")) {
            return true;
        }

        Integer priority = notification.getPriority();
        switch (selectedPriority) {
            case "Critical (4)":
                return priority == 4;
            case "High (3)":
                return priority == 3;
            case "Medium (2)":
                return priority == 2;
            case "Low (1)":
                return priority == 1;
            default:
                return true;
        }
    }

    private boolean matchesReadFilter(Notification notification) {
        if (showReadCheckbox.isSelected()) {
            return true; // Show all
        }
        return !notification.getIsRead(); // Show only unread
    }

    private boolean matchesTimeRangeFilter(Notification notification) {
        String selectedRange = timeRangeComboBox.getValue();
        if (selectedRange == null || selectedRange.equals("All Time")) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = notification.getCreatedAt();

        switch (selectedRange) {
            case "24 Hours":
                return createdAt.isAfter(now.minusHours(24));
            case "7 Days":
                return createdAt.isAfter(now.minusDays(7));
            case "30 Days":
                return createdAt.isAfter(now.minusDays(30));
            default:
                return true;
        }
    }

    // ========== Display ==========

    private void displayNotifications() {
        Platform.runLater(() -> {
            notificationList.getChildren().clear();

            if (filteredNotifications == null || filteredNotifications.isEmpty()) {
                emptyStateLabel.setVisible(true);
                emptyStateLabel.setManaged(true);
                notificationList.getChildren().add(emptyStateLabel);
                return;
            }

            emptyStateLabel.setVisible(false);
            emptyStateLabel.setManaged(false);

            for (Notification notification : filteredNotifications) {
                VBox notificationCard = createNotificationCard(notification);
                notificationList.getChildren().add(notificationCard);
            }

            statusLabel.setText(String.format("Showing %d of %d notifications",
                    filteredNotifications.size(), allNotifications.size()));
        });
    }

    private VBox createNotificationCard(Notification notification) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle(getCardStyle(notification));

        // Header Row: Icon, Title, Time
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // Priority Icon
        Label icon = new Label(getPriorityIcon(notification.getPriority()));
        icon.setStyle("-fx-font-size: 18px;");

        // Title
        Label title = new Label(notification.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        if (!notification.getIsRead()) {
            title.setStyle(title.getStyle() + "-fx-text-fill: #000;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Time Ago
        Label timeAgo = new Label(notification.getTimeAgo());
        timeAgo.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        header.getChildren().addAll(icon, title, spacer, timeAgo);

        // Message
        Label message = new Label(notification.getMessage());
        message.setWrapText(true);
        message.setStyle("-fx-font-size: 12px; -fx-text-fill: #444;");

        // Type Badge
        Label typeBadge = new Label(notification.getType().getDisplayName());
        typeBadge.setStyle(getBadgeStyle(notification.getType().getSeverity()));

        // Action Buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (!notification.getIsRead()) {
            Button markReadBtn = new Button("✓ Mark Read");
            markReadBtn.getStyleClass().add("button-secondary");
            markReadBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
            markReadBtn.setOnAction(e -> handleMarkRead(notification));
            actions.getChildren().add(markReadBtn);
        }

        Button dismissBtn = new Button("✕ Dismiss");
        dismissBtn.getStyleClass().add("button-secondary");
        dismissBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
        dismissBtn.setOnAction(e -> handleDismiss(notification));
        actions.getChildren().add(dismissBtn);

        if (notification.getActionUrl() != null) {
            Button viewDetailsBtn = new Button("→ View Details");
            viewDetailsBtn.getStyleClass().add("button-primary");
            viewDetailsBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
            viewDetailsBtn.setOnAction(e -> handleViewDetails(notification));
            actions.getChildren().add(viewDetailsBtn);
        }

        card.getChildren().addAll(header, message, typeBadge, actions);

        return card;
    }

    private String getCardStyle(Notification notification) {
        String baseStyle = "-fx-background-color: white; " +
                          "-fx-border-color: #ddd; " +
                          "-fx-border-radius: 6; " +
                          "-fx-background-radius: 6; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);";

        if (!notification.getIsRead()) {
            baseStyle += "-fx-border-width: 2; -fx-border-color: #2196f3;";
        } else {
            baseStyle += "-fx-opacity: 0.85;";
        }

        return baseStyle;
    }

    private String getPriorityIcon(Integer priority) {
        if (priority == null) return "🔵";
        switch (priority) {
            case 4: return "🔴"; // Critical
            case 3: return "🟠"; // High
            case 2: return "🔵"; // Medium
            case 1: return "🟢"; // Low
            default: return "🔵";
        }
    }

    private String getBadgeStyle(String severity) {
        String baseStyle = "-fx-padding: 3 8; -fx-font-size: 10px; " +
                          "-fx-background-radius: 10; -fx-text-fill: white;";

        String bgColor;
        switch (severity) {
            case "danger":
                bgColor = "#f44336";
                break;
            case "warning":
                bgColor = "#ff9800";
                break;
            case "success":
                bgColor = "#4caf50";
                break;
            case "info":
            default:
                bgColor = "#2196f3";
                break;
        }

        return baseStyle + "-fx-background-color: " + bgColor + ";";
    }

    private void updateSummaryStats() {
        Platform.runLater(() -> {
            int total = allNotifications.size();
            long unread = allNotifications.stream().filter(n -> !n.getIsRead()).count();
            long critical = allNotifications.stream().filter(n -> n.getPriority() == 4).count();
            long warnings = allNotifications.stream().filter(n -> n.getPriority() == 3).count();

            totalCountLabel.setText(String.valueOf(total));
            unreadStatLabel.setText(String.valueOf(unread));
            criticalCountLabel.setText(String.valueOf(critical));
            warningCountLabel.setText(String.valueOf(warnings));

            unreadCountLabel.setText(unread + " Unread");
        });
    }

    // ========== Event Handlers ==========

    @FXML
    private void handleRefresh() {
        loadNotifications();
        showInfo("Notifications refreshed");
    }

    @FXML
    private void handleFilterChange() {
        applyFilters();
        displayNotifications();
    }

    @FXML
    private void handleMarkAllRead() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                notificationService.markAllAsRead(currentUserId);
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            showInfo("All notifications marked as read");
            loadNotifications();
        });

        new Thread(task).start();
    }

    @FXML
    private void handleDismissAll() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Dismiss All Notifications");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This will dismiss all notifications. This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        notificationService.dismissAllForUser(currentUserId);
                        return null;
                    }
                };

                task.setOnSucceeded(event -> {
                    showInfo("All notifications dismissed");
                    loadNotifications();
                });

                new Thread(task).start();
            }
        });
    }

    private void handleMarkRead(Notification notification) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                notificationService.markAsRead(notification.getId());
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            loadNotifications();
        });

        new Thread(task).start();
    }

    private void handleDismiss(Notification notification) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                notificationService.dismissNotification(notification.getId());
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            loadNotifications();
        });

        new Thread(task).start();
    }

    private void handleViewDetails(Notification notification) {
        String actionUrl = notification.getActionUrl();
        if (actionUrl == null || actionUrl.isEmpty()) {
            showInfo("No navigation target available");
            return;
        }

        // Mark as read when viewing details
        handleMarkRead(notification);

        // Navigate based on actionUrl pattern
        // Supported patterns: /fxml/ViewName.fxml or entity:id (e.g., teacher:123, student:456)
        if (mainController != null) {
            try {
                if (actionUrl.startsWith("/fxml/")) {
                    // Extract view name from FXML path and navigate
                    String viewName = actionUrl.replace("/fxml/", "").replace(".fxml", "").toLowerCase();
                    navigateToEntity(viewName);
                } else if (actionUrl.contains(":")) {
                    // Entity navigation pattern: entityType:id
                    String[] parts = actionUrl.split(":");
                    String entityType = parts[0].toLowerCase();
                    navigateToEntity(entityType);
                } else {
                    // Try to match common view names
                    navigateToEntity(actionUrl.toLowerCase());
                }
            } catch (Exception e) {
                showError("Navigation failed: " + e.getMessage());
            }
        } else {
            showInfo("Navigation to: " + actionUrl + " (MainController not available)");
        }
    }

    private void navigateToEntity(String entityType) {
        if (mainController == null) return;

        switch (entityType) {
            case "teacher":
            case "teachers":
                mainController.loadTeachers();
                break;
            case "student":
            case "students":
                mainController.loadStudents();
                break;
            case "course":
            case "courses":
                mainController.loadCourses();
                break;
            case "room":
            case "rooms":
                mainController.loadRooms();
                break;
            case "schedule":
            case "schedules":
                mainController.loadSchedules();
                break;
            case "conflict":
            case "conflicts":
                mainController.loadConflictDashboard();
                break;
            case "enrollment":
                mainController.loadStudentEnrollment();
                break;
            case "gradebook":
                mainController.loadGradebook();
                break;
            case "event":
            case "events":
                mainController.loadEvents();
                break;
            default:
                showInfo("Unknown entity type: " + entityType);
                return;
        }
        showInfo("Navigated to " + entityType);
    }

    @FXML
    private void handleRunCleanup() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                notificationService.cleanupOldNotifications();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            showInfo("Cleanup completed");
            loadNotifications();
        });

        new Thread(task).start();
    }

    // ========== Quick Filter Handlers ==========

    @FXML
    private void handleShowCritical() {
        priorityFilterComboBox.setValue("Critical (4)");
        showReadCheckbox.setSelected(false);
        handleFilterChange();
    }

    @FXML
    private void handleShowScheduleChanges() {
        typeFilterComboBox.setValue("Schedule Change");
        showReadCheckbox.setSelected(false);
        handleFilterChange();
    }

    @FXML
    private void handleShowConflicts() {
        typeFilterComboBox.setValue("Conflict Detected");
        showReadCheckbox.setSelected(false);
        handleFilterChange();
    }

    @FXML
    private void handleShowEnrollment() {
        // Show multiple enrollment-related types
        showReadCheckbox.setSelected(false);
        typeFilterComboBox.setValue("Over Enrollment");
        handleFilterChange();
    }

    // ========== Utility Methods ==========

    private void showInfo(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: #4caf50;");
        });

        // Reset after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    statusLabel.setText("Ready");
                    statusLabel.setStyle("-fx-text-fill: #000;");
                });
            } catch (InterruptedException e) {
                // Ignore
            }
        }).start();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("An error occurred");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
