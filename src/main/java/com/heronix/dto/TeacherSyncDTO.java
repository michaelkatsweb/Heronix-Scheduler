package com.heronix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Teacher Sync DTO
 *
 * Data Transfer Object for synchronizing teacher data from PostgreSQL (Heronix Scheduler-Pro)
 * to H2 (EduPro-Teacher application)
 *
 * Contains essential teacher information including encrypted password for authentication
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSyncDTO {

    /**
     * Teacher ID
     */
    private Long id;

    /**
     * Employee ID (unique identifier for login)
     */
    private String employeeId;

    /**
     * BCrypt encrypted password
     */
    private String password;

    /**
     * First name
     */
    private String firstName;

    /**
     * Last name
     */
    private String lastName;

    /**
     * Full name (for display)
     */
    private String name;

    /**
     * Email address
     */
    private String email;

    /**
     * Department
     */
    private String department;

    /**
     * Account active status
     */
    private boolean active;
}
