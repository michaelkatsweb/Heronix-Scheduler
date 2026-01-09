package com.heronix.model.dto;

import java.util.*;

/**
 * Represents a single row with validation issues
 */
public class ValidationIssue {
    private String entityType; // "Student", "Teacher", etc.
    private int rowNumber;
    private Map<String, String> originalData;
    private List<String> missingFields;
    private List<String> invalidFields;
    private boolean fixed; // User has corrected this issue

    /**
     * Constructor
     */
    public ValidationIssue(String entityType, int rowNumber,
            Map<String, String> originalData,
            List<String> missingFields,
            List<String> invalidFields) {
        this.entityType = entityType;
        this.rowNumber = rowNumber;
        this.originalData = new HashMap<>(originalData);
        this.missingFields = new ArrayList<>(missingFields);
        this.invalidFields = new ArrayList<>(invalidFields);
        this.fixed = false;
    }

    /**
     * Get summary of this issue
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Row ").append(rowNumber).append(": ");

        if (!missingFields.isEmpty()) {
            sb.append("Missing: ").append(String.join(", ", missingFields));
        }
        if (!invalidFields.isEmpty()) {
            if (!missingFields.isEmpty())
                sb.append(" | ");
            sb.append("Invalid: ").append(String.join(", ", invalidFields));
        }

        return sb.toString();
    }

    /**
     * Check if this issue has been fixed
     */
    public boolean isFixed() {
        return fixed;
    }

    /**
     * Mark as fixed
     */
    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    // Getters
    public String getEntityType() {
        return entityType;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public Map<String, String> getOriginalData() {
        return originalData;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public List<String> getInvalidFields() {
        return invalidFields;
    }

    // Setters
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public void setOriginalData(Map<String, String> originalData) {
        this.originalData = originalData;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public void setInvalidFields(List<String> invalidFields) {
        this.invalidFields = invalidFields;
    }
}