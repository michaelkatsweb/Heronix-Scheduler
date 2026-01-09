package com.heronix.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Conflict data transfer object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDTO {
    private Long id;
    private String entityType;
    private Long entityId;
    private String fieldName;
    private String field;  // Alias for fieldName

    private String localValue;
    private String serverValue;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime localTimestamp;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime serverTimestamp;

    private String description;
    private String resolution;

    // Aliases for Teacher Portal compatibility
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime clientTimestamp;  // Alias for localTimestamp
}
