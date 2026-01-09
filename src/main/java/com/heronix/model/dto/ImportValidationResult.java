package com.heronix.model.dto;

import java.util.*;

/**
 * Overall validation results for entire import
 */
public class ImportValidationResult {
    private int totalRows;
    private int validRows;
    private List<ValidationIssue> issues;

    public ImportValidationResult() {
        this.issues = new ArrayList<>();
        this.totalRows = 0;
        this.validRows = 0;
    }

    /**
     * Add a validation issue
     */
    public void addIssue(ValidationIssue issue) {
        if (issue != null) {
            issues.add(issue);
        }
    }

    /**
     * Check if there are any issues
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * Get count of issues
     */
    public int getIssueCount() {
        return issues.size();
    }

    /**
     * Get summary string
     */
    public String getSummary() {
        return String.format("Total: %d | Valid: %d | Issues: %d",
                totalRows, validRows, getIssueCount());
    }

    /**
     * Get all unfixed issues
     */
    public List<ValidationIssue> getUnfixedIssues() {
        List<ValidationIssue> unfixed = new ArrayList<>();
        for (ValidationIssue issue : issues) {
            if (!issue.isFixed()) {
                unfixed.add(issue);
            }
        }
        return unfixed;
    }

    // Getters and setters
    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public List<ValidationIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<ValidationIssue> issues) {
        this.issues = issues;
    }
}