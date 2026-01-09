package com.heronix.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Grade data transfer object for API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeDTO {
    private Long id;
    private Long studentId;
    private Long assignmentId;
    private Double score;
    private String letterGrade;
    private Double gpaPoints;
    private String notes;
    private Boolean excused;
    private Boolean late;
    private Boolean missing;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateEntered;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime modifiedDate;

    // Alias for compatibility
    public LocalDateTime getLastModified() {
        return modifiedDate;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.modifiedDate = lastModified;
    }

    // Additional fields for Teacher Portal sync
    private String comments;
    private java.time.LocalDate gradedDate;
}
