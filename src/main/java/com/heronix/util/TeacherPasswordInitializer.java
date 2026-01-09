package com.heronix.util;

import com.heronix.model.domain.Teacher;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.TeacherAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Teacher Password Initializer
 *
 * Utility to set initial passwords for teachers who don't have one.
 * Generates secure random passwords and marks them for forced change.
 *
 * Usage:
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--init-passwords"
 *
 * Features:
 * - Generates secure random passwords (12 characters)
 * - Skips teachers who already have passwords
 * - Sets password_expires_at to force immediate change
 * - Prints passwords to console (save these!)
 *
 * @author Heronix Scheduler Team
 * @version 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TeacherPasswordInitializer implements CommandLineRunner {

    private final TeacherRepository teacherRepository;
    private final TeacherAuthService authService;

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String CHAR_DIGITS = "0123456789";
    private static final String CHAR_SPECIAL = "!@#$%";
    private static final String PASSWORD_CHARS = CHAR_LOWER + CHAR_UPPER + CHAR_DIGITS + CHAR_SPECIAL;

    private final SecureRandom random = new SecureRandom();

    @Override
    public void run(String... args) throws Exception {
        // Only run if --init-passwords argument is provided
        if (args.length == 0 || !args[0].equals("--init-passwords")) {
            return;
        }

        log.info("=======================================================");
        log.info("TEACHER PASSWORD INITIALIZATION");
        log.info("=======================================================");

        List<Teacher> teachers = teacherRepository.findAllActive();
        int initialized = 0;
        int skipped = 0;

        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║           TEACHER PASSWORD INITIALIZATION REPORT              ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        for (Teacher teacher : teachers) {
            if (teacher.getPassword() == null || teacher.getPassword().isEmpty()) {
                String generatedPassword = generateSecurePassword(12);

                try {
                    // Set password using auth service (encrypts with BCrypt)
                    authService.updatePassword(teacher.getId(), generatedPassword);

                    // Force password change on first login by setting expiration to now
                    teacher.setPasswordExpiresAt(LocalDateTime.now());
                    teacher.setMustChangePassword(true);
                    teacherRepository.save(teacher);

                    initialized++;

                    // Print credentials
                    System.out.println("┌─────────────────────────────────────────────────────────────┐");
                    System.out.printf("│ Teacher: %-50s │%n", teacher.getName());
                    System.out.printf("│ Employee ID: %-47s │%n", teacher.getEmployeeId());
                    System.out.printf("│ Email: %-52s │%n", teacher.getEmail() != null ? teacher.getEmail() : "N/A");
                    System.out.printf("│ TEMPORARY PASSWORD: %-40s │%n", generatedPassword);
                    System.out.println("│ Status: MUST CHANGE PASSWORD ON FIRST LOGIN                 │");
                    System.out.println("└─────────────────────────────────────────────────────────────┘\n");

                    log.info("Initialized password for: {} ({})", teacher.getName(), teacher.getEmployeeId());

                } catch (Exception e) {
                    log.error("Failed to initialize password for {}: {}", teacher.getName(), e.getMessage());
                    System.out.printf("ERROR: Failed to set password for %s: %s%n%n", teacher.getName(), e.getMessage());
                }

            } else {
                skipped++;
                log.debug("Skipped {} - already has password", teacher.getName());
            }
        }

        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        SUMMARY                                 ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Teachers: %-47d║%n", teachers.size());
        System.out.printf("║ Passwords Initialized: %-39d║%n", initialized);
        System.out.printf("║ Skipped (already have password): %-28d║%n", skipped);
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ IMPORTANT: Save these passwords securely!                     ║");
        System.out.println("║ Teachers MUST change password on first login.                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        log.info("Password initialization complete. {} passwords set, {} skipped.", initialized, skipped);
    }

    /**
     * Generate a secure random password
     *
     * @param length Password length (minimum 8, recommended 12+)
     * @return Secure random password
     */
    private String generateSecurePassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8 characters");
        }

        StringBuilder password = new StringBuilder(length);

        // Ensure at least one of each character type
        password.append(CHAR_LOWER.charAt(random.nextInt(CHAR_LOWER.length())));
        password.append(CHAR_UPPER.charAt(random.nextInt(CHAR_UPPER.length())));
        password.append(CHAR_DIGITS.charAt(random.nextInt(CHAR_DIGITS.length())));
        password.append(CHAR_SPECIAL.charAt(random.nextInt(CHAR_SPECIAL.length())));

        // Fill the rest randomly
        for (int i = 4; i < length; i++) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }

        // Shuffle to randomize position of required characters
        return shuffleString(password.toString());
    }

    /**
     * Shuffle string characters for better randomness
     */
    private String shuffleString(String input) {
        char[] characters = input.toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }
        return new String(characters);
    }
}
