package com.heronix.ui.controller;

import com.heronix.model.domain.User;
import com.heronix.model.enums.Role;
import com.heronix.security.SecurityContext;
import com.heronix.service.UserService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User Management Controller
 * Manages user CRUD operations, role assignment, and bulk operations
 *
 * Features:
 * - User list with search and filtering
 * - Create, edit, delete users
 * - Role and permission assignment
 * - Enable/disable/lock/unlock accounts
 * - Bulk operations
 * - Real-time statistics
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/UserManagementController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Slf4j
@Component
public class UserManagementController {

    // ========================================================================
    // FXML FIELDS
    // ========================================================================

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Button refreshButton;
    @FXML private Button addUserButton;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Boolean> selectColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> fullNameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, String> lastLoginColumn;
    @FXML private TableColumn<User, String> failedLoginsColumn;
    @FXML private TableColumn<User, Void> actionsColumn;

    @FXML private Label userCountLabel;
    @FXML private Label tableInfoLabel;
    @FXML private Label selectedCountLabel;

    @FXML private HBox bulkActionsBar;
    @FXML private Label bulkSelectionLabel;

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label adminsLabel;
    @FXML private Label lockedUsersLabel;
    @FXML private Label lastRefreshLabel;

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    @Autowired
    private UserService userService;

    @Autowired
    private UserDialogController userDialogController;

    // ========================================================================
    // STATE VARIABLES
    // ========================================================================

    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private ObservableList<User> filteredUsers = FXCollections.observableArrayList();
    private Set<User> selectedUsers = new HashSet<>();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing User Management Controller");

        // Check permissions
        if (!SecurityContext.hasAnyRole(Role.SUPER_ADMIN, Role.ADMIN)) {
            showError("Access Denied", "You don't have permission to access user management.");
            return;
        }

        // Setup table columns
        setupTableColumns();

        // Setup filters
        setupFilters();

        // Load users
        loadUsers();

        // Setup listeners
        setupListeners();
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        // Select column with checkbox
        selectColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            javafx.beans.property.BooleanProperty selected =
                new javafx.beans.property.SimpleBooleanProperty(selectedUsers.contains(user));

            selected.addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    selectedUsers.add(user);
                } else {
                    selectedUsers.remove(user);
                }
                updateBulkActionsBar();
            });

            return selected;
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        // Username column
        usernameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getUsername()));

        // Full Name column
        fullNameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFullName()));

        // Email column
        emailColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getEmail()));

        // Role column with styling
        roleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getRoleDisplayName()));
        roleColumn.setCellFactory(col -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    User user = getTableView().getItems().get(getIndex());
                    if (user.getPrimaryRole() == Role.SUPER_ADMIN) {
                        setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
                    } else if (user.getPrimaryRole() == Role.ADMIN) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Status column with badges
        statusColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            if (!user.getEnabled()) {
                return new SimpleStringProperty("Disabled");
            } else if (!user.getAccountNonLocked()) {
                return new SimpleStringProperty("Locked");
            } else {
                return new SimpleStringProperty("Active");
            }
        });
        statusColumn.setCellFactory(col -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Active":
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            break;
                        case "Disabled":
                            setStyle("-fx-text-fill: #999; -fx-font-weight: bold;");
                            break;
                        case "Locked":
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                            break;
                    }
                }
            }
        });

        // Last Login column
        lastLoginColumn.setCellValueFactory(cellData -> {
            LocalDateTime lastLogin = cellData.getValue().getLastLogin();
            return new SimpleStringProperty(
                lastLogin != null ? dateFormatter.format(lastLogin) : "Never"
            );
        });

        // Failed Logins column
        failedLoginsColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().getFailedLoginAttempts())));
        failedLoginsColumn.setCellFactory(col -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    int count = Integer.parseInt(item);
                    if (count >= 5) {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    } else if (count >= 3) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Actions column with buttons
        actionsColumn.setCellFactory(col -> new TableCell<User, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button roleButton = new Button("Role");
            private final Button deleteButton = new Button("Delete");
            private final HBox buttons = new HBox(5, editButton, roleButton, deleteButton);

            {
                buttons.setAlignment(Pos.CENTER);
                editButton.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                roleButton.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #2196F3; -fx-text-fill: white;");
                deleteButton.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #F44336; -fx-text-fill: white;");

                editButton.setOnAction(e -> handleEditUser(getTableRow().getItem()));
                roleButton.setOnAction(e -> handleAssignRole(getTableRow().getItem()));
                deleteButton.setOnAction(e -> handleDeleteUser(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableRow().getItem();
                    if (user != null) {
                        // Disable delete for super admins and current user
                        User currentUser = SecurityContext.requireCurrentUser();
                        deleteButton.setDisable(
                            user.getPrimaryRole() == Role.SUPER_ADMIN ||
                            user.getId().equals(currentUser.getId())
                        );
                    }
                    setGraphic(buttons);
                }
            }
        });

        userTable.setEditable(true);
    }

    /**
     * Setup filters
     */
    private void setupFilters() {
        // Populate role filter
        roleFilterCombo.getItems().addAll(
            "All Roles",
            "SUPER_ADMIN",
            "ADMIN",
            "PRINCIPAL",
            "COUNSELOR",
            "TEACHER",
            "STAFF",
            "PARENT"
        );
        roleFilterCombo.setValue("All Roles");

        // Populate status filter
        statusFilterCombo.getItems().addAll("All", "Active", "Disabled", "Locked");
        statusFilterCombo.setValue("All");

        // Add listeners
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
        roleFilterCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        statusFilterCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    /**
     * Setup listeners
     */
    private void setupListeners() {
        // Selection listener
        userTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                // Handle selection if needed
            }
        );
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    /**
     * Load all users
     */
    private void loadUsers() {
        log.info("Loading users...");

        try {
            List<User> users = userService.findAll();
            allUsers.setAll(users);
            applyFilters();
            updateStatistics();
            updateLastRefreshLabel();

            log.info("Loaded {} users", users.size());

        } catch (Exception e) {
            log.error("Error loading users", e);
            showError("Error", "Failed to load users: " + e.getMessage());
        }
    }

    /**
     * Apply filters to user list
     */
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        String roleFilter = roleFilterCombo.getValue();
        String statusFilter = statusFilterCombo.getValue();

        List<User> filtered = allUsers.stream()
            .filter(user -> {
                // Search filter
                if (!searchText.isEmpty()) {
                    boolean matches =
                        user.getUsername().toLowerCase().contains(searchText) ||
                        user.getFullName().toLowerCase().contains(searchText) ||
                        user.getEmail().toLowerCase().contains(searchText);
                    if (!matches) return false;
                }

                // Role filter
                if (!"All Roles".equals(roleFilter)) {
                    Role role = Role.valueOf(roleFilter);
                    if (user.getPrimaryRole() != role) return false;
                }

                // Status filter
                if (!"All".equals(statusFilter)) {
                    switch (statusFilter) {
                        case "Active":
                            if (!user.getEnabled() || !user.getAccountNonLocked()) return false;
                            break;
                        case "Disabled":
                            if (user.getEnabled()) return false;
                            break;
                        case "Locked":
                            if (user.getAccountNonLocked()) return false;
                            break;
                    }
                }

                return true;
            })
            .collect(Collectors.toList());

        filteredUsers.setAll(filtered);
        userTable.setItems(filteredUsers);

        // Update labels
        tableInfoLabel.setText(String.format("Showing %d of %d users",
            filteredUsers.size(), allUsers.size()));
        userCountLabel.setText(String.format("%d users", allUsers.size()));
    }

    /**
     * Update statistics
     */
    private void updateStatistics() {
        totalUsersLabel.setText(String.valueOf(allUsers.size()));

        long activeCount = allUsers.stream()
            .filter(u -> u.getEnabled() && u.getAccountNonLocked())
            .count();
        activeUsersLabel.setText(String.valueOf(activeCount));

        long adminCount = allUsers.stream()
            .filter(u -> u.getPrimaryRole() == Role.SUPER_ADMIN || u.getPrimaryRole() == Role.ADMIN)
            .count();
        adminsLabel.setText(String.valueOf(adminCount));

        long lockedCount = allUsers.stream()
            .filter(u -> !u.getAccountNonLocked())
            .count();
        lockedUsersLabel.setText(String.valueOf(lockedCount));
    }

    /**
     * Update last refresh label
     */
    private void updateLastRefreshLabel() {
        lastRefreshLabel.setText(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /**
     * Update bulk actions bar
     */
    private void updateBulkActionsBar() {
        int selectedCount = selectedUsers.size();

        bulkActionsBar.setVisible(selectedCount > 0);
        bulkActionsBar.setManaged(selectedCount > 0);

        bulkSelectionLabel.setText(selectedCount + " selected");
        selectedCountLabel.setText(selectedCount > 0 ? selectedCount + " users selected" : "");
    }

    // ========================================================================
    // ACTION HANDLERS
    // ========================================================================

    /**
     * Handle refresh
     */
    @FXML
    private void handleRefresh() {
        log.info("Refreshing user list");
        selectedUsers.clear();
        loadUsers();
        updateBulkActionsBar();
        showInfo("Refreshed", "User list has been refreshed.");
    }

    /**
     * Handle add user
     */
    @FXML
    private void handleAddUser() {
        log.info("Add user clicked");
        try {
            // Load FXML and open dialog
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/UserDialog.fxml"));
            loader.setControllerFactory(param -> userDialogController);

            javafx.scene.layout.BorderPane page = loader.load();

            // Create dialog stage
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Add New User");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(userTable.getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(page));

            // Set the dialog stage in controller
            userDialogController.setDialogStage(dialogStage);
            userDialogController.setupForCreate(); // Setup for new user

            // Show dialog and wait for response
            dialogStage.showAndWait();

            // If user was saved, reload the list
            if (userDialogController.isSaved()) {
                log.info("User created successfully");
                loadUsers();
                showInfo("Success", "User created successfully!");
            }
        } catch (Exception e) {
            log.error("Error opening user dialog", e);
            showError("Error", "Failed to open user dialog: " + e.getMessage());
        }
    }

    /**
     * Handle edit user
     */
    private void handleEditUser(User user) {
        if (user == null) return;
        log.info("Edit user: {}", user.getUsername());

        try {
            // Load FXML and open dialog
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/UserDialog.fxml"));
            loader.setControllerFactory(param -> userDialogController);

            javafx.scene.layout.BorderPane page = loader.load();

            // Create dialog stage
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Edit User: " + user.getUsername());
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(userTable.getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(page));

            // Set the dialog stage and user in controller
            userDialogController.setDialogStage(dialogStage);
            userDialogController.setupForEdit(user); // Pass user for editing

            // Show dialog and wait for response
            dialogStage.showAndWait();

            // If user was saved, reload the list
            if (userDialogController.isSaved()) {
                log.info("User updated successfully");
                loadUsers();
                showInfo("Success", "User updated successfully!");
            }
        } catch (Exception e) {
            log.error("Error opening user dialog", e);
            showError("Error", "Failed to open user dialog: " + e.getMessage());
        }
    }

    /**
     * Handle assign role
     */
    private void handleAssignRole(User user) {
        if (user == null) return;
        log.info("Assign role to user: {}", user.getUsername());

        // Create simple role assignment dialog
        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(
            user.getPrimaryRole() != null ? user.getPrimaryRole().toString() : "USER",
            "SUPER_ADMIN", "ADMIN", "STAFF", "USER"
        );

        dialog.setTitle("Assign Role");
        dialog.setHeaderText("Assign Role to: " + user.getUsername());
        dialog.setContentText("Select role:");

        dialog.showAndWait().ifPresent(roleString -> {
            try {
                Role newRole = Role.valueOf(roleString);
                user.setPrimaryRole(newRole);
                userService.updateUser(user);
                loadUsers();
                showInfo("Success", "Role updated to " + newRole + " for user " + user.getUsername());
            } catch (Exception e) {
                log.error("Error updating user role", e);
                showError("Error", "Failed to update user role: " + e.getMessage());
            }
        });
    }

    /**
     * Handle delete user
     */
    private void handleDeleteUser(User user) {
        if (user == null) return;

        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Delete user: " + user.getUsername());
        alert.setContentText("Are you sure you want to delete this user?\nThis action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.deleteUser(user.getId());
                    allUsers.remove(user);
                    applyFilters();
                    updateStatistics();
                    showInfo("Deleted", "User " + user.getUsername() + " has been deleted.");
                    log.info("User deleted: {}", user.getUsername());
                } catch (Exception e) {
                    log.error("Error deleting user", e);
                    showError("Error", "Failed to delete user: " + e.getMessage());
                }
            }
        });
    }

    // ========================================================================
    // BULK OPERATIONS
    // ========================================================================

    @FXML
    private void handleBulkEnable() {
        log.info("Bulk enable: {} users", selectedUsers.size());
        performBulkOperation("enable", user -> userService.enableAccount(user.getId()));
    }

    @FXML
    private void handleBulkDisable() {
        log.info("Bulk disable: {} users", selectedUsers.size());
        performBulkOperation("disable", user -> userService.disableAccount(user.getId()));
    }

    @FXML
    private void handleBulkUnlock() {
        log.info("Bulk unlock: {} users", selectedUsers.size());
        performBulkOperation("unlock", user -> userService.unlockAccount(user.getId()));
    }

    @FXML
    private void handleBulkDelete() {
        log.info("Bulk delete: {} users", selectedUsers.size());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Bulk Delete");
        alert.setHeaderText("Delete " + selectedUsers.size() + " users");
        alert.setContentText("Are you sure you want to delete these users?\nThis action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performBulkOperation("delete", user -> userService.deleteUser(user.getId()));
            }
        });
    }

    @FXML
    private void handleClearSelection() {
        selectedUsers.clear();
        userTable.refresh();
        updateBulkActionsBar();
    }

    /**
     * Perform bulk operation
     */
    private void performBulkOperation(String operation, BulkOperation op) {
        int successCount = 0;
        int failCount = 0;

        for (User user : selectedUsers) {
            try {
                op.execute(user);
                successCount++;
            } catch (Exception e) {
                log.error("Error during bulk " + operation + " for user: " + user.getUsername(), e);
                failCount++;
            }
        }

        selectedUsers.clear();
        loadUsers();
        updateBulkActionsBar();

        String message = String.format("%s completed: %d succeeded, %d failed",
            operation, successCount, failCount);
        showInfo("Bulk Operation", message);
    }

    @FunctionalInterface
    private interface BulkOperation {
        void execute(User user) throws Exception;
    }

    // ========================================================================
    // ALERT HELPERS
    // ========================================================================

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
