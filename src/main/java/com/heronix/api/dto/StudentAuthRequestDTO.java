package com.heronix.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Student Authentication Request DTO
 * Used by EduPro-Student portal for login
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - Student Portal Authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAuthRequestDTO {
    /**
     * Student ID or email address
     * Examples: "S12345" or "ljohnson@student.edu"
     */
    @NotBlank(message = "Student ID is required")
    @Size(min = 1, max = 100, message = "Student ID must be between 1 and 100 characters")
    private String studentId;

    /**
     * Password (defaults to student ID if not set)
     */
    @Size(max = 255, message = "Password must not exceed 255 characters")
    private String password;

    /**
     * QR code ID for QR-based authentication (optional)
     */
    private String qrCodeId;

    /**
     * Base64-encoded photo for facial recognition verification (optional)
     */
    private String photoBase64;

    /**
     * 4-digit PIN for enhanced security (optional)
     * Only required if student has pinRequired=true
     * Used as 3rd layer of security: QR + Facial + PIN
     *
     * @since December 13, 2025 - PIN Authentication System
     */
    @Size(min = 4, max = 4, message = "PIN must be exactly 4 digits")
    private String pin;
}
