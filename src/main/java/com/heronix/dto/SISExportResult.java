package com.heronix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of SIS export operation
 *
 * Location: src/main/java/com/eduscheduler/dto/SISExportResult.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SISExportResult {

    /**
     * Export type/format
     */
    private String exportType;

    /**
     * File name/path of exported file
     */
    private String fileName;

    /**
     * File path (absolute)
     */
    private String filePath;

    /**
     * Number of records exported
     */
    private int recordsExported;

    /**
     * Number of validation errors
     */
    private int errorCount;

    /**
     * List of validation errors
     */
    private List<String> errors = new ArrayList<>();

    /**
     * List of warnings
     */
    private List<String> warnings = new ArrayList<>();

    /**
     * Success status
     */
    private boolean success;

    /**
     * Export timestamp
     */
    private LocalDateTime exportTime;

    /**
     * Additional metadata
     */
    private String metadata;

    /**
     * Add an error
     */
    public void addError(String error) {
        this.errors.add(error);
        this.errorCount++;
    }

    /**
     * Add a warning
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * Check if export has errors
     */
    public boolean hasErrors() {
        return errorCount > 0;
    }

    /**
     * Get summary message
     */
    public String getSummary() {
        if (success) {
            return String.format("Successfully exported %d records to %s. %d warnings.",
                    recordsExported, fileName, warnings.size());
        } else {
            return String.format("Export failed with %d errors. %d records exported.",
                    errorCount, recordsExported);
        }
    }
}
