package com.heronix.service.impl;

import com.heronix.model.DistrictSettings;
import com.heronix.repository.DistrictSettingsRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.DistrictSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of DistrictSettingsService
 *
 * Handles:
 * - Email generation with placeholder replacement
 * - ID generation with auto-increment
 * - Room phone generation (supports both extension and full number)
 * - Duplicate detection and alternative generation
 * - Format validation
 *
 * Location: src/main/java/com/eduscheduler/service/impl/DistrictSettingsServiceImpl.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - District Configuration System
 */
@Service
@Transactional
public class DistrictSettingsServiceImpl implements DistrictSettingsService {

    @Autowired
    private DistrictSettingsRepository districtSettingsRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    // Track student ID sequences per graduation year
    private Map<Integer, Integer> studentIdSequences = new HashMap<>();

    // ========================================================================
    // DISTRICT SETTINGS CRUD
    // ========================================================================

    @Override
    public DistrictSettings getOrCreateDistrictSettings() {
        return districtSettingsRepository.findFirstBy()
            .orElseGet(() -> {
                // Create default settings
                DistrictSettings defaultSettings = DistrictSettings.builder()
                    .districtName("Heronix Scheduler District")
                    .teacherEmailDomain("@school.edu")
                    .studentEmailDomain("@students.school.edu")
                    .teacherEmailFormat("{lastname}_{firstname_initial}")
                    .studentEmailFormat("{firstname}.{lastname}")
                    .teacherIdPrefix("T")
                    .teacherIdStartNumber(1)
                    .teacherIdPadding(4)
                    .teacherIdCurrentNumber(1)
                    .studentIdFormat("{grad_year}-{sequence}")
                    .studentIdPadding(4)
                    .roomPhonePrefix("(555) 123-")
                    .roomPhoneExtensionStart(4000)
                    .autoGenerateTeacherEmail(true)
                    .autoGenerateStudentEmail(true)
                    .autoGenerateTeacherId(true)
                    .autoGenerateStudentId(true)
                    .autoGenerateRoomPhone(true)
                    .defaultPeriodDuration(50)
                    .defaultPassingPeriod(5)
                    .defaultLunchDuration(30)
                    .includeDistrictInfoOnPrint(true)
                    .build();

                return districtSettingsRepository.save(defaultSettings);
            });
    }

    @Override
    public DistrictSettings updateDistrictSettings(DistrictSettings settings) {
        if (settings.getId() == null) {
            // New settings, make sure there's only one
            districtSettingsRepository.deleteAll();
        }
        return districtSettingsRepository.save(settings);
    }

    @Override
    public boolean isDistrictConfigured() {
        DistrictSettings settings = getOrCreateDistrictSettings();
        // ✅ NULL SAFE: Check settings exists before accessing method
        return settings != null && settings.isConfigured();
    }

    // ========================================================================
    // EMAIL GENERATION
    // ========================================================================

    @Override
    public String generateTeacherEmail(String firstName, String lastName) {
        return generateTeacherEmail(firstName, null, lastName);
    }

    @Override
    public String generateTeacherEmail(String firstName, String middleName, String lastName) {
        DistrictSettings settings = getOrCreateDistrictSettings();

        // ✅ NULL SAFE: Check settings and Boolean wrappers with null-safe comparison
        if (settings == null ||
            !Boolean.TRUE.equals(settings.getAutoGenerateTeacherEmail()) ||
            settings.getTeacherEmailFormat() == null ||
            settings.getTeacherEmailDomain() == null) {
            return "";
        }

        String email = generateEmailFromFormat(
            settings.getTeacherEmailFormat(),
            firstName,
            middleName,
            lastName,
            null,
            0
        );

        // ✅ NULL SAFE: Email should never be null but use safe concatenation
        email = (email != null ? email.toLowerCase() : "") + settings.getTeacherEmailDomain();

        // Check for duplicates and generate alternative if needed
        if (isTeacherEmailTaken(email)) {
            email = generateAlternativeEmail(email, true);
        }

        return email;
    }

    @Override
    public String generateStudentEmail(String firstName, String lastName, int gradYear) {
        return generateStudentEmail(firstName, lastName, gradYear, null);
    }

    @Override
    public String generateStudentEmail(String firstName, String lastName, int gradYear, String studentId) {
        DistrictSettings settings = getOrCreateDistrictSettings();

        // ✅ NULL SAFE: Check settings and Boolean wrappers with null-safe comparison
        if (settings == null ||
            !Boolean.TRUE.equals(settings.getAutoGenerateStudentEmail()) ||
            settings.getStudentEmailFormat() == null ||
            settings.getStudentEmailDomain() == null) {
            return "";
        }

        String email = generateEmailFromFormat(
            settings.getStudentEmailFormat(),
            firstName,
            null,
            lastName,
            studentId,
            gradYear
        );

        // ✅ NULL SAFE: Email should never be null but use safe concatenation
        email = (email != null ? email.toLowerCase() : "") + settings.getStudentEmailDomain();

        // Check for duplicates and generate alternative if needed
        if (isStudentEmailTaken(email)) {
            email = generateAlternativeEmail(email, false);
        }

        return email;
    }

    /**
     * Generate email from format template by replacing placeholders
     */
    private String generateEmailFromFormat(String format, String firstName, String middleName,
                                          String lastName, String studentId, int gradYear) {
        if (format == null || format.trim().isEmpty()) {
            return "";
        }

        String result = format;

        // Replace name placeholders
        if (firstName != null && !firstName.isEmpty()) {
            result = result.replace("{firstname}", firstName);
            result = result.replace("{firstname_initial}", firstName.substring(0, 1));
        }

        if (lastName != null && !lastName.isEmpty()) {
            result = result.replace("{lastname}", lastName);
            result = result.replace("{lastname_initial}", lastName.substring(0, 1));
        }

        if (middleName != null && !middleName.isEmpty()) {
            result = result.replace("{middlename_initial}", middleName.substring(0, 1));
        }

        // Replace student-specific placeholders
        if (studentId != null) {
            result = result.replace("{student_id}", studentId);
        }

        if (gradYear > 0) {
            result = result.replace("{grad_year}", String.valueOf(gradYear));
            result = result.replace("{grad_year_short}", String.valueOf(gradYear % 100));
        }

        return result;
    }

    @Override
    public boolean isTeacherEmailTaken(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return teacherRepository.existsByEmail(email.toLowerCase());
    }

    @Override
    public boolean isStudentEmailTaken(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return studentRepository.existsByEmail(email.toLowerCase());
    }

    @Override
    public String generateAlternativeEmail(String baseEmail, boolean isTeacher) {
        if (baseEmail == null || !baseEmail.contains("@")) {
            return baseEmail;
        }

        String[] parts = baseEmail.split("@");
        String localPart = parts[0];
        String domain = "@" + parts[1];

        // Remove existing number suffix if present
        Pattern pattern = Pattern.compile("^(.+?)(\\d+)$");
        Matcher matcher = pattern.matcher(localPart);
        if (matcher.matches()) {
            localPart = matcher.group(1);
        }

        // Try appending numbers until we find an available email
        for (int i = 2; i <= 99; i++) {
            String alternative = localPart + i + domain;

            boolean taken = isTeacher ?
                isTeacherEmailTaken(alternative) :
                isStudentEmailTaken(alternative);

            if (!taken) {
                return alternative;
            }
        }

        // If we get here, return with timestamp
        return localPart + System.currentTimeMillis() + domain;
    }

    // ========================================================================
    // ID GENERATION
    // ========================================================================

    @Override
    public String generateNextTeacherId() {
        DistrictSettings settings = getOrCreateDistrictSettings();

        // ✅ NULL SAFE: Check settings and Boolean wrappers with null-safe comparison
        if (settings == null ||
            !Boolean.TRUE.equals(settings.getAutoGenerateTeacherId()) ||
            settings.getTeacherIdPrefix() == null) {
            return "";
        }

        // ✅ NULL SAFE: Safe extraction of current number with fallback
        Integer currentNumber = settings.getTeacherIdCurrentNumber();
        if (currentNumber == null) {
            currentNumber = settings.getTeacherIdStartNumber();
            if (currentNumber == null) {
                currentNumber = 1; // Ultimate fallback
            }
        }

        // ✅ NULL SAFE: Safe padding value extraction
        Integer padding = settings.getTeacherIdPadding();
        if (padding == null) {
            padding = 4; // Default padding
        }

        String paddedNumber = padNumber(currentNumber, padding);
        String teacherId = settings.getTeacherIdPrefix() + paddedNumber;

        // Increment the current number for next time
        settings.setTeacherIdCurrentNumber(currentNumber + 1);
        districtSettingsRepository.save(settings);

        return teacherId;
    }

    @Override
    public String generateNextStudentId(int gradYear) {
        DistrictSettings settings = getOrCreateDistrictSettings();

        // ✅ NULL SAFE: Check settings and Boolean wrappers with null-safe comparison
        if (settings == null ||
            !Boolean.TRUE.equals(settings.getAutoGenerateStudentId()) ||
            settings.getStudentIdFormat() == null) {
            return "";
        }

        // Get or initialize sequence for this graduation year
        Integer sequence = studentIdSequences.getOrDefault(gradYear, 1);

        // ✅ NULL SAFE: Safe padding value extraction
        Integer padding = settings.getStudentIdPadding();
        if (padding == null) {
            padding = 4; // Default padding
        }

        String studentId = generateStudentIdFromFormat(
            settings.getStudentIdFormat(),
            settings.getStudentIdPrefix(),
            gradYear,
            sequence,
            padding
        );

        // Increment sequence for this graduation year
        studentIdSequences.put(gradYear, sequence + 1);

        return studentId;
    }

    /**
     * Generate student ID from format template
     */
    private String generateStudentIdFromFormat(String format, String prefix,
                                               int gradYear, int sequence, int padding) {
        // ✅ NULL SAFE: Validate format parameter
        if (format == null || format.trim().isEmpty()) {
            return "";
        }

        String result = format;

        if (prefix != null) {
            result = result.replace("{prefix}", prefix);
        }

        result = result.replace("{grad_year}", String.valueOf(gradYear));
        result = result.replace("{grad_year_short}", String.valueOf(gradYear % 100));
        result = result.replace("{sequence}", padNumber(sequence, padding));

        return result;
    }

    /**
     * Pad a number with leading zeros
     */
    private String padNumber(int number, int padding) {
        return String.format("%0" + padding + "d", number);
    }

    @Override
    public boolean isTeacherIdAvailable(String teacherId) {
        if (teacherId == null || teacherId.trim().isEmpty()) {
            return false;
        }
        return !teacherRepository.existsByEmployeeId(teacherId);
    }

    @Override
    public boolean isStudentIdAvailable(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return false;
        }
        return !studentRepository.existsByStudentId(studentId);
    }

    // ========================================================================
    // ROOM PHONE GENERATION
    // ========================================================================

    @Override
    public String generateRoomPhoneNumber(String roomNumber) {
        DistrictSettings settings = getOrCreateDistrictSettings();

        // ✅ NULL SAFE: Check settings and Boolean wrappers with null-safe comparison
        if (settings == null ||
            !Boolean.TRUE.equals(settings.getAutoGenerateRoomPhone()) ||
            settings.getRoomPhonePrefix() == null ||
            roomNumber == null || roomNumber.trim().isEmpty()) {
            return "";
        }

        Integer roomNumericPart = extractRoomNumberValue(roomNumber);
        if (roomNumericPart == null) {
            return "";
        }

        // ✅ NULL SAFE: Safe extraction of extension start with fallback
        Integer extensionStart = settings.getRoomPhoneExtensionStart();
        if (extensionStart == null) {
            extensionStart = 4000; // Default extension start
        }

        Integer extension = extensionStart + roomNumericPart;
        return settings.getRoomPhonePrefix() + extension;
    }

    @Override
    public Integer extractRoomNumberValue(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            return null;
        }

        // Extract numeric part from room number
        // Examples: "CLS-101" -> 101, "SCI-105" -> 105, "GYM-1" -> 1
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(roomNumber);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    @Override
    public boolean isValidEmailFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return false;
        }

        // Check if format contains at least one valid placeholder
        String[] placeholders = getEmailFormatPlaceholders();
        for (String placeholder : placeholders) {
            if (format.contains(placeholder)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isValidIdFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return false;
        }

        // Check if format contains at least one valid placeholder
        String[] placeholders = getIdFormatPlaceholders();
        for (String placeholder : placeholders) {
            if (format.contains(placeholder)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String[] getEmailFormatPlaceholders() {
        return new String[] {
            "{firstname}",
            "{lastname}",
            "{firstname_initial}",
            "{lastname_initial}",
            "{middlename_initial}",
            "{student_id}",
            "{grad_year}",
            "{grad_year_short}"
        };
    }

    @Override
    public String[] getIdFormatPlaceholders() {
        return new String[] {
            "{prefix}",
            "{grad_year}",
            "{grad_year_short}",
            "{sequence}"
        };
    }
}
