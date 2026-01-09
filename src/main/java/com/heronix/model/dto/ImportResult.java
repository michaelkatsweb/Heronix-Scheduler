// Location: src/main/java/com/eduscheduler/model/dto/ImportResult.java
package com.heronix.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Import Result DTO - COMPLETE with Issue Tracking
 * Location: src/main/java/com/eduscheduler/model/dto/ImportResult.java
 * 
 * @author Heronix Scheduling System Team
 * @version 6.0.1 - Fixed initialization
 * @since 2025-10-25
 */
@Data
public class ImportResult {

    private String filename;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Counts
    private int successCount = 0;
    private int errorCount = 0;
    private int skippedCount = 0;
    private int warningCount = 0;

    // Details
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> skippedItems = new ArrayList<>();
    private List<DataIssue> incompleteRecords = new ArrayList<>();

    private boolean completed = false;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public ImportResult() {
        // Default constructor
    }

    public ImportResult(String filename) {
        this.filename = filename;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.skippedItems = new ArrayList<>();
        this.incompleteRecords = new ArrayList<>();
    }

    public ImportResult(int successCount, int errorCount, int skippedCount) {
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.skippedCount = skippedCount;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.skippedItems = new ArrayList<>();
        this.incompleteRecords = new ArrayList<>();
        this.completed = false;
    }

    // ========================================================================
    // LIFECYCLE METHODS
    // ========================================================================

    public void start() {
        this.startTime = LocalDateTime.now();
    }

    public void complete() {
        this.endTime = LocalDateTime.now();
        this.completed = true;
    }

    // ========================================================================
    // SUCCESS TRACKING
    // ========================================================================

    public void addSuccess() {
        this.successCount++;
    }

    public void addSuccess(int count) {
        this.successCount += count;
    }

    public void incrementSuccess() {
        this.successCount++;
    }

    // ========================================================================
    // ERROR TRACKING
    // ========================================================================

    public void addError(String error) {
        this.errorCount++;
        this.errors.add(error);
    }

    public void incrementError() {
        this.errorCount++;
    }

    public void addErrorMessage(String message) {
        this.errors.add(message);
    }

    // ========================================================================
    // SKIPPED TRACKING
    // ========================================================================

    public void addSkipped(String reason) {
        this.skippedCount++;
        this.skippedItems.add(reason);
    }

    public void incrementSkipped() {
        this.skippedCount++;
    }

    // ========================================================================
    // WARNING TRACKING
    // ========================================================================

    public void addWarning(String warning) {
        this.warningCount++;
        this.warnings.add(warning);
    }

    public void incrementWarning() {
        this.warningCount++;
    }

    // ========================================================================
    // INCOMPLETE DATA TRACKING
    // ========================================================================

    public void addIncompleteRecord(DataIssue issue) {
        this.warningCount++;
        this.incompleteRecords.add(issue);
        this.warnings.add(issue.getSummary());
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    public int getTotalProcessed() {
        return successCount + errorCount + skippedCount;
    }

    public boolean isSuccess() {
        return completed && errorCount == 0;
    }

    public boolean hasWarnings() {
        return warningCount > 0;
    }

    public long getDurationSeconds() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).getSeconds();
        }
        return 0;
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Import Results: ");
        sb.append(successCount).append(" successful, ");
        sb.append(errorCount).append(" errors, ");
        sb.append(skippedCount).append(" skipped");

        if (warningCount > 0) {
            sb.append(", ").append(warningCount).append(" warnings");
        }

        return sb.toString();
    }
}