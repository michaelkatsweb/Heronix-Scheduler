package com.heronix.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CleanupResult {
    private String operation;
    private boolean success;
    private int deletedCount;
    private LocalDateTime timestamp;
    private List<String> deletedEntityTypes = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private String errorMessage;

    public CleanupResult(String operation) {
        this.operation = operation;
        this.timestamp = LocalDateTime.now();
    }

    public void addDeletedEntityType(String entityType, int count) {
        deletedEntityTypes.add(entityType + ": " + count);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public String getSummary() {
        return String.format("Operation: %s\nStatus: %s\nDeleted: %d records\nTime: %s",
                operation, success ? "SUCCESS" : "FAILED", deletedCount, timestamp);
    }
}