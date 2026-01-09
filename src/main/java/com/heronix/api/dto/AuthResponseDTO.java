package com.heronix.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Authentication response for Teacher Portal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private Boolean success;
    private String message;
    private String token;
    private Long teacherId;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private LocalDateTime lastSync;

    // Legacy field (kept for backwards compatibility)
    private String teacherName;
}
