package com.heronix.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Assignment data transfer object for API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDTO {
    private Long id;
    private Long courseId;
    private String courseName;
    private String name;
    private String description;
    private String category;
    private Double maxPoints;
    private Double weight;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private Boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime modifiedDate;
}
