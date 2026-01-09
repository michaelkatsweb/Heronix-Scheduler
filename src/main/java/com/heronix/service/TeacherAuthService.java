package com.heronix.service;

import com.heronix.model.domain.Teacher;
import com.heronix.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Teacher Authentication Service
 *
 * Handles teacher account creation and management with BCrypt password encryption
 * This service is used by:
 * 1. Heronix Scheduler-Pro (Admin) to create teacher accounts
 * 2. REST API for EduPro-Teacher synchronization
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-29
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TeacherAuthService {

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Password expiration period in days
     * Default: 60 days
     * Configurable via application.properties: teacher.password.expiration.days
     */
    @Value("${teacher.password.expiration.days:60}")
    private int passwordExpirationDays;

    /**
     * Number of previous passwords to check against for reuse prevention
     * Default: 3 (cannot reuse last 3 passwords)
     * Configurable via application.properties: teacher.password.history.size
     */
    @Value("${teacher.password.history.size:3}")
    private int passwordHistorySize;

    /**
     * Create a new teacher account with encrypted password
     *
     * @param employeeId Unique employee ID for login
     * @param firstName Teacher's first name
     * @param lastName Teacher's last name
     * @param email Teacher's email address
     * @param plainPassword Plaintext password (will be encrypted)
     * @param department Teacher's department
     * @return Created teacher
     * @throws IllegalArgumentException if employee ID or email already exists
     */
    @Transactional
    public Teacher createTeacher(String employeeId, String firstName, String lastName,
                                  String email, String plainPassword, String department) {

        log.debug("Creating teacher account: {} {} ({})", firstName, lastName, employeeId);

        // Validate employee ID is unique
        if (teacherRepository.existsByEmployeeId(employeeId)) {
            throw new IllegalArgumentException("Employee ID already exists: " + employeeId);
        }

        // Validate email is unique
        if (teacherRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        // Create teacher entity
        Teacher teacher = new Teacher();
        teacher.setEmployeeId(employeeId);
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setName(firstName + " " + lastName);
        teacher.setEmail(email);
        teacher.setDepartment(department);
        teacher.setActive(true);

        // Encrypt password with BCrypt
        String encryptedPassword = passwordEncoder.encode(plainPassword);
        teacher.setPassword(encryptedPassword);

        // Save to database
        Teacher savedTeacher = teacherRepository.save(teacher);

        log.info("Created teacher account: {} {} ({}) - Password encrypted with BCrypt",
                 firstName, lastName, employeeId);

        return savedTeacher;
    }

    /**
     * Update teacher password with expiration and history tracking
     *
     * @param teacherId Teacher ID
     * @param plainPassword New plaintext password (will be encrypted)
     * @throws IllegalArgumentException if teacher not found or password was used recently
     */
    @Transactional
    public void updatePassword(Long teacherId, String plainPassword) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));

        // Check if new password matches any recent passwords
        if (teacher.getPasswordHistory() == null) {
            teacher.setPasswordHistory(new ArrayList<>());
        }

        // Check password history to prevent reuse
        for (String oldPasswordHash : teacher.getPasswordHistory()) {
            if (passwordEncoder.matches(plainPassword, oldPasswordHash)) {
                throw new IllegalArgumentException(
                    "Cannot reuse a recent password. Please choose a different password."
                );
            }
        }

        // Encrypt new password
        String encryptedPassword = passwordEncoder.encode(plainPassword);

        // Add current password to history before changing
        if (teacher.getPassword() != null && !teacher.getPassword().isEmpty()) {
            teacher.getPasswordHistory().add(0, teacher.getPassword());

            // Limit history size
            if (teacher.getPasswordHistory().size() > passwordHistorySize) {
                teacher.setPasswordHistory(
                    teacher.getPasswordHistory().subList(0, passwordHistorySize)
                );
            }
        }

        // Set new password
        teacher.setPassword(encryptedPassword);
        teacher.setPasswordChangedAt(LocalDateTime.now());

        // Set password expiration date
        teacher.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordExpirationDays));

        // Clear must change password flag
        teacher.setMustChangePassword(false);

        teacherRepository.save(teacher);

        log.info("Updated password for teacher: {} ({}) - Expires: {}",
                 teacher.getName(), teacher.getEmployeeId(), teacher.getPasswordExpiresAt());
    }

    /**
     * Verify teacher credentials and check password expiration
     *
     * @param employeeId Employee ID
     * @param plainPassword Plaintext password
     * @return Teacher if credentials are valid, empty otherwise
     */
    public Optional<Teacher> authenticate(String employeeId, String plainPassword) {
        Optional<Teacher> teacherOpt = teacherRepository.findByEmployeeId(employeeId);

        if (teacherOpt.isEmpty()) {
            log.warn("Authentication failed: Employee ID not found: {}", employeeId);
            return Optional.empty();
        }

        Teacher teacher = teacherOpt.get();

        // Check if account is active
        if (!teacher.getActive()) {
            log.warn("Authentication failed: Account inactive: {}", employeeId);
            return Optional.empty();
        }

        // Verify password
        if (teacher.getPassword() == null) {
            log.warn("Authentication failed: No password set for: {}", employeeId);
            return Optional.empty();
        }

        boolean passwordMatches = passwordEncoder.matches(plainPassword, teacher.getPassword());

        if (!passwordMatches) {
            log.warn("Authentication failed: Invalid password for: {}", employeeId);
            return Optional.empty();
        }

        // Check if password is expired
        if (isPasswordExpired(teacher)) {
            log.warn("Authentication warning: Password expired for: {} - Must change password", employeeId);
            teacher.setMustChangePassword(true);
            teacherRepository.save(teacher);
        }

        log.info("Authentication successful: {} ({}) - Must change password: {}",
                 teacher.getName(), employeeId, teacher.getMustChangePassword());
        return Optional.of(teacher);
    }

    /**
     * Check if teacher's password is expired
     *
     * @param teacher Teacher to check
     * @return true if password is expired
     */
    public boolean isPasswordExpired(Teacher teacher) {
        if (teacher.getPasswordExpiresAt() == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(teacher.getPasswordExpiresAt());
    }

    /**
     * Check if teacher must change password
     *
     * @param teacher Teacher to check
     * @return true if password change is required
     */
    public boolean mustChangePassword(Teacher teacher) {
        return teacher.getMustChangePassword() != null && teacher.getMustChangePassword();
    }

    /**
     * Get days until password expires
     *
     * @param teacher Teacher to check
     * @return Days until expiration, or -1 if already expired, or null if no expiration set
     */
    public Long getDaysUntilPasswordExpires(Teacher teacher) {
        if (teacher.getPasswordExpiresAt() == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = teacher.getPasswordExpiresAt();

        if (now.isAfter(expiresAt)) {
            return -1L; // Already expired
        }

        return java.time.Duration.between(now, expiresAt).toDays();
    }

    /**
     * Check if employee ID exists
     */
    public boolean employeeIdExists(String employeeId) {
        return teacherRepository.existsByEmployeeId(employeeId);
    }

    /**
     * Check if email exists
     */
    public boolean emailExists(String email) {
        return teacherRepository.existsByEmail(email);
    }
}
