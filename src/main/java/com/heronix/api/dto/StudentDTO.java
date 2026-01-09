package com.heronix.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Student data transfer object for API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private Long id;
    private String studentId;
    private String firstName;
    private String lastName;
    private String gradeLevel; // Changed from Integer to String to match Student entity
    private String email;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private Double currentGpa;
    private Boolean active;
    private Boolean hasIep;
    private Boolean has504;
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastModified;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;
}
