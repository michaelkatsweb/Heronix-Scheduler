package com.heronix.config;

import com.heronix.service.TeacherAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Teacher Data Initializer
 *
 * Creates test teacher accounts in PostgreSQL database
 * These accounts are used by EduPro-Teacher application for authentication
 *
 * Test Accounts:
 * - T001: John Smith / password123
 * - T002: Sarah Johnson / password123
 * - T003: Michael Davis / password123
 * - T004: Emily Wilson / password123
 * - T005: David Martinez / password123
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-29
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class TeacherDataInitializer {

    private final TeacherAuthService teacherAuthService;

    /**
     * Initialize test teacher accounts
     * Only creates accounts if they don't already exist
     */
    @Bean
    public CommandLineRunner initializeTestTeachers() {
        return args -> {
            log.info("Checking if test teacher accounts need to be created...");

            try {
                // Create 5 test teachers
                createTestTeacher("T001", "John", "Smith", "john.smith@school.edu",
                                "password123", "Mathematics");

                createTestTeacher("T002", "Sarah", "Johnson", "sarah.johnson@school.edu",
                                "password123", "English");

                createTestTeacher("T003", "Michael", "Davis", "michael.davis@school.edu",
                                "password123", "Science");

                createTestTeacher("T004", "Emily", "Wilson", "emily.wilson@school.edu",
                                "password123", "History");

                createTestTeacher("T005", "David", "Martinez", "david.martinez@school.edu",
                                "password123", "Physical Education");

                log.info("✓ Test teacher data initialization completed successfully");

            } catch (Exception e) {
                log.error("Error during test teacher data initialization", e);
            }
        };
    }

    /**
     * Create a test teacher account (only if it doesn't exist)
     */
    private void createTestTeacher(String employeeId, String firstName, String lastName,
                                  String email, String password, String department) {
        try {
            teacherAuthService.createTeacher(
                employeeId, firstName, lastName, email, password, department
            );
            log.info("✓ Created test teacher: {} {} ({}) - Password: {}",
                     firstName, lastName, employeeId, password);

        } catch (IllegalArgumentException e) {
            // Teacher already exists, skip
            log.debug("  Teacher {} already exists, skipping", employeeId);
        } catch (Exception e) {
            log.error("  Failed to create teacher {}: {}", employeeId, e.getMessage());
        }
    }
}
