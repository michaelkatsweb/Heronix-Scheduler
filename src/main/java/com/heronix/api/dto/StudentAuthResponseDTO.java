package com.heronix.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Student Authentication Response DTO
 * Returned to EduPro-Student portal after successful login
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - Student Portal Authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAuthResponseDTO {
    /**
     * Authentication success status
     */
    private Boolean success;

    /**
     * Response message (error or success)
     */
    private String message;

    /**
     * Optional JWT token for session management
     */
    private String token;

    /**
     * Student database ID
     */
    private Long studentId;

    /**
     * Student ID (e.g., "S12345")
     */
    private String studentIdNumber;

    /**
     * Student first name
     */
    private String firstName;

    /**
     * Student last name
     */
    private String lastName;

    /**
     * Student full name (firstName + lastName)
     */
    private String studentName;

    /**
     * Student email
     */
    private String email;

    /**
     * Student grade level (e.g., "9", "10", "11", "12")
     */
    private String gradeLevel;

    /**
     * QR code ID for this student (used for QR code generation)
     */
    private String qrCodeId;

    /**
     * Photo path (relative or URL)
     */
    private String photoPath;

    /**
     * Has IEP (Individual Education Plan)
     */
    private Boolean hasIEP;

    /**
     * Has 504 Plan
     */
    private Boolean has504Plan;

    /**
     * Last sync timestamp
     */
    private LocalDateTime lastSync;

    /**
     * Facial recognition confidence score (if used)
     */
    private Double faceMatchConfidence;

    /**
     * Whether facial recognition verification was performed
     */
    private Boolean faceVerified;

    /**
     * Whether this student requires PIN entry
     * If true, frontend should prompt for 4-digit PIN
     *
     * @since December 13, 2025 - PIN Authentication System
     */
    private Boolean pinRequired;

    /**
     * Whether PIN verification was successful
     * Only set if PIN was provided and verified
     */
    private Boolean pinVerified;

    /**
     * Account locked status
     * If true, student account is locked due to failed PIN attempts
     */
    private Boolean accountLocked;

    /**
     * When account will auto-unlock (if locked)
     */
    private LocalDateTime lockedUntil;
}
