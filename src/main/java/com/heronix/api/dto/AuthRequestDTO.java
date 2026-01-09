package com.heronix.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication request from teacher app
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {
    @NotBlank(message = "Employee ID is required")
    @Size(min = 1, max = 50, message = "Employee ID must be between 1 and 50 characters")
    private String employeeId;

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 255, message = "Password must be between 1 and 255 characters")
    private String password;
}
