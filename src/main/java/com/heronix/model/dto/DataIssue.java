package com.heronix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Issue - Tracks incomplete or problematic import records
 * Location: src/main/java/com/eduscheduler/model/dto/DataIssue.java
 * 
 * Used to flag records that imported successfully but have missing/invalid
 * data.
 * These records are saved to the database but flagged for review.
 * 
 * Example:
 * - Room imported with missing room number (auto-generated)
 * - Teacher imported with missing email
 * - Student with invalid phone format
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataIssue {

    /**
     * Type of entity (Room, Teacher, Student, Course)
     */
    private String entityType;

    /**
     * Database ID of the imported record
     */
    private Long entityId;

    /**
     * Row number from Excel/CSV file
     */
    private int rowNumber;

    /**
     * Human-readable identifier (room number, employee ID, student ID)
     */
    private String identifier;

    /**
     * List of missing required fields
     */
    private List<String> missingFields = new ArrayList<>();

    /**
     * List of invalid fields with details
     */
    private List<String> invalidFields = new ArrayList<>();

    /**
     * Severity of the issue
     */
    private IssueSeverity severity = IssueSeverity.WARNING;

    /**
     * Issue severity levels
     */
    public enum IssueSeverity {
        /**
         * Warning - Record usable but needs attention
         */
        WARNING,

        /**
         * Error - Critical issue that may affect functionality
         */
        ERROR
    }

    /**
     * Constructor without severity (defaults to WARNING)
     */
    public DataIssue(String entityType, Long entityId, int rowNumber,
            String identifier, List<String> missingFields,
            List<String> invalidFields) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.rowNumber = rowNumber;
        this.identifier = identifier;
        this.missingFields = missingFields != null ? missingFields : new ArrayList<>();
        this.invalidFields = invalidFields != null ? invalidFields : new ArrayList<>();
        this.severity = IssueSeverity.WARNING;
    }

    /**
     * Get a human-readable summary
     * 
     * Example: "Room 101 (Row 5): Missing: capacity, building"
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        // Entity type and identifier
        sb.append(entityType).append(" ");
        if (identifier != null && !identifier.isEmpty()) {
            sb.append(identifier);
        } else {
            sb.append("(No ID)");
        }

        // Row number
        sb.append(" (Row ").append(rowNumber).append("): ");

        // Missing fields
        if (missingFields != null && !missingFields.isEmpty()) {
            sb.append("Missing: ").append(String.join(", ", missingFields));
        }

        // Invalid fields
        if (invalidFields != null && !invalidFields.isEmpty()) {
            if (missingFields != null && !missingFields.isEmpty()) {
                sb.append("; ");
            }
            sb.append("Invalid: ").append(String.join(", ", invalidFields));
        }

        return sb.toString();
    }

    /**
     * Check if this is a critical issue
     */
    public boolean isCritical() {
        return severity == IssueSeverity.ERROR;
    }

    /**
     * Get total number of issues
     */
    public int getIssueCount() {
        int count = 0;
        if (missingFields != null)
            count += missingFields.size();
        if (invalidFields != null)
            count += invalidFields.size();
        return count;
    }
}