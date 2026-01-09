package com.heronix.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sync status data transfer object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusDTO {
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSuccessfulSync;

    private Integer pendingGrades;
    private Integer pendingAttendance;
    private Integer conflictingGrades;
    private Integer conflictingStudents;

    private List<ConflictDTO> conflicts;

    private Boolean syncRequired;
    private String message;

    // Additional fields for Teacher Portal
    private Boolean serverOnline;
    private Boolean syncAvailable;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime serverTime;

    private String apiVersion;
}
