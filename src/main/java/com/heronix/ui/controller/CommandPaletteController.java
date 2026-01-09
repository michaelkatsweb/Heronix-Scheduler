package com.heronix.ui.controller;

import com.heronix.model.domain.*;
import com.heronix.service.GlobalSearchService;
import com.heronix.service.GlobalSearchService.SearchResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Command Palette Controller - Global Search (Ctrl+K)
 * Provides quick access to entities and actions across the application
 *
 * @author Heronix Scheduler Team
 * @version 1.0
 */
@Component
public class CommandPaletteController {

    private static final Logger log = LoggerFactory.getLogger(CommandPaletteController.class);

    @FXML private VBox rootContainer;
    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private Label searchTimeLabel;

    // Filter toggle buttons
    @FXML private ToggleButton allFilter;
    @FXML private ToggleButton studentsFilter;
    @FXML private ToggleButton teachersFilter;
    @FXML private ToggleButton coursesFilter;
    @FXML private ToggleButton roomsFilter;
    @FXML private ToggleButton actionsFilter;

    @FXML private ListView<SearchResult> resultsListView;

    @Autowired
    private GlobalSearchService globalSearchService;

    private Stage dialogStage;
    private String currentCategory = "ALL";
    private ObservableList<SearchResult> searchResults = FXCollections.observableArrayList();

    // Callback for when a result is selected
    private Consumer<SearchResult> onResultSelected;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        setupSearchField();
        setupResultsList();
        setupFilterButtons();
        setupKeyboardNavigation();

        // Load default suggestions
        Platform.runLater(() -> {
            searchField.requestFocus();
            performSearch("");
        });
    }

    /**
     * Setup search field with real-time search
     */
    private void setupSearchField() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            performSearch(newValue);
        });

        // Handle Enter key to select first result
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                selectFirstResult();
            } else if (event.getCode() == KeyCode.DOWN) {
                // Move focus to results list
                if (!resultsListView.getItems().isEmpty()) {
                    resultsListView.requestFocus();
                    resultsListView.getSelectionModel().selectFirst();
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                closeDialog();
            }
        });
    }

    /**
     * Setup results ListView with custom cell factory
     */
    private void setupResultsList() {
        resultsListView.setItems(searchResults);

        // Custom cell factory for rich display
        resultsListView.setCellFactory(listView -> new ListCell<SearchResult>() {
            @Override
            protected void updateItem(SearchResult result, boolean empty) {
                super.updateItem(result, empty);

                if (empty || result == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    // Create rich text display
                    setText(result.getFullDisplayText());
                    setStyle("-fx-padding: 8 12; -fx-font-size: 13px;");

                    // Add subtle type indicator
                    if ("ACTION".equals(result.getType())) {
                        setStyle(getStyle() + "-fx-background-color: rgba(100, 200, 255, 0.1);");
                    }
                }
            }
        });

        // Handle double-click or Enter to select
        resultsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                selectCurrentResult();
            }
        });

        resultsListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                selectCurrentResult();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                closeDialog();
            } else if (event.getCode() == KeyCode.UP && resultsListView.getSelectionModel().getSelectedIndex() == 0) {
                // Move focus back to search field
                searchField.requestFocus();
                event.consume();
            }
        });
    }

    /**
     * Setup filter toggle buttons
     */
    private void setupFilterButtons() {
        ToggleGroup filterGroup = new ToggleGroup();
        allFilter.setToggleGroup(filterGroup);
        studentsFilter.setToggleGroup(filterGroup);
        teachersFilter.setToggleGroup(filterGroup);
        coursesFilter.setToggleGroup(filterGroup);
        roomsFilter.setToggleGroup(filterGroup);
        actionsFilter.setToggleGroup(filterGroup);

        allFilter.setSelected(true);
    }

    /**
     * Setup keyboard navigation shortcuts
     */
    private void setupKeyboardNavigation() {
        rootContainer.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                closeDialog();
            }
        });
    }

    /**
     * Handle filter button changes
     */
    @FXML
    private void handleFilterChange() {
        if (allFilter.isSelected()) {
            currentCategory = "ALL";
        } else if (studentsFilter.isSelected()) {
            currentCategory = "STUDENTS";
        } else if (teachersFilter.isSelected()) {
            currentCategory = "TEACHERS";
        } else if (coursesFilter.isSelected()) {
            currentCategory = "COURSES";
        } else if (roomsFilter.isSelected()) {
            currentCategory = "ROOMS";
        } else if (actionsFilter.isSelected()) {
            currentCategory = "ACTIONS";
        }

        performSearch(searchField.getText());
    }

    /**
     * Perform search with current query and category
     */
    private void performSearch(String query) {
        long startTime = System.currentTimeMillis();

        try {
            List<SearchResult> results = globalSearchService.search(query, currentCategory);

            searchResults.clear();
            searchResults.addAll(results);

            // Update result count
            int count = results.size();
            if (query == null || query.trim().isEmpty()) {
                resultCountLabel.setText("");
            } else {
                resultCountLabel.setText(count + " result" + (count != 1 ? "s" : ""));
            }

            // Update search time
            long searchTime = System.currentTimeMillis() - startTime;
            searchTimeLabel.setText(searchTime + "ms");

            // Auto-select first result
            if (!results.isEmpty()) {
                resultsListView.getSelectionModel().selectFirst();
            }

        } catch (Exception e) {
            log.error("Search error: {}", e.getMessage(), e);
        }
    }

    /**
     * Select the first result in the list
     */
    private void selectFirstResult() {
        if (!searchResults.isEmpty()) {
            resultsListView.getSelectionModel().selectFirst();
            selectCurrentResult();
        }
    }

    /**
     * Select the currently highlighted result
     */
    private void selectCurrentResult() {
        SearchResult selected = resultsListView.getSelectionModel().getSelectedItem();
        if (selected != null && onResultSelected != null) {
            onResultSelected.accept(selected);
            closeDialog();
        }
    }

    /**
     * Close the command palette dialog
     */
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Set the dialog stage (called from parent controller)
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;

        // Setup ESC key to close dialog
        dialogStage.getScene().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                closeDialog();
            }
        });
    }

    /**
     * Set callback for when a result is selected
     */
    public void setOnResultSelected(Consumer<SearchResult> callback) {
        this.onResultSelected = callback;
    }

    /**
     * Get the root container (for adding to scene)
     */
    public VBox getRootContainer() {
        return rootContainer;
    }
}
