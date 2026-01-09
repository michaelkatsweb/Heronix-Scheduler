package com.heronix.model.domain;

import com.heronix.model.enums.CertificationType;
import com.heronix.model.enums.PriorityLevel;
import com.heronix.model.enums.TeacherRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {

    private static final Logger log = LoggerFactory.getLogger(Teacher.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull(message = "Teacher name cannot be null")
    @Size(min = 2, max = 100, message = "Teacher name must be between 2 and 100 characters")
    private String name;

    private String firstName; // For CSV/Excel imports
    private String lastName; // For CSV/Excel imports

    @Column(unique = true)
    private String employeeId;

    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    /**
     * BCrypt encrypted password for teacher authentication
     * Used by EduPro-Teacher application for login
     * Encrypted with BCryptPasswordEncoder (60 characters)
     */
    @Column(name = "password")
    private String password;

    /**
     * Timestamp when password expires
     * Teachers must change password when this date is reached
     * Configurable via application.properties (default: 60 days)
     */
    @Column(name = "password_expires_at")
    private java.time.LocalDateTime passwordExpiresAt;

    /**
     * Flag to force password change on next login
     * Set to true for new accounts or after password reset
     */
    @Column(name = "must_change_password")
    private Boolean mustChangePassword = false;

    /**
     * Timestamp when password was last changed
     * Used to track password history and enforce expiration
     */
    @Column(name = "password_changed_at")
    private java.time.LocalDateTime passwordChangedAt;

    /**
     * Password history - stores BCrypt hashes of previous passwords
     * Used to prevent password reuse
     * Configurable: Can enforce "last N passwords cannot be reused"
     */
    @ElementCollection
    @CollectionTable(name = "teacher_password_history", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "password_hash", length = 60)
    @ToString.Exclude
    private List<String> passwordHistory = new ArrayList<>();

    // ========================================================================
    // QR CODE AUTHENTICATION
    // ========================================================================

    /**
     * Unique QR code identifier for this teacher
     * Format: QR-{EMPLOYEE_ID}-{RANDOM_SUFFIX}
     * Example: QR-T12345-ABC123
     * Used for:
     * - Quick login to EduPro-Teacher mobile app
     * - Clock in/out time tracking
     * - Room check-in verification
     * - Event/meeting attendance
     */
    @Column(name = "qr_code_id", unique = true, length = 100)
    private String qrCodeId;

    /**
     * Path to teacher's photo file
     * Used for:
     * - QR code badge printing with photo
     * - Facial recognition verification (optional)
     * - Staff directory display
     * Format: ./data/teacher-photos/{EMPLOYEE_ID}.jpg
     */
    @Column(name = "photo_path", length = 500)
    private String photoPath;

    /**
     * Timestamp of last QR code scan
     * Used for tracking QR code usage and security auditing
     */
    @Column(name = "last_qr_scan")
    private java.time.LocalDateTime lastQrScan;

    /**
     * Timestamp when QR code was generated
     * QR codes can be regenerated if lost, stolen, or for security rotation
     */
    @Column(name = "qr_generated_date")
    private java.time.LocalDateTime qrGeneratedDate;

    private String phoneNumber;
    private String department;

    @ElementCollection
    @CollectionTable(name = "teacher_certifications", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "certification")
    @ToString.Exclude
    private List<String> certifications = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    // ========================================================================
    // TEACHER TYPES & ROLES
    // ========================================================================

    /**
     * Primary certification type: CERTIFIED, PROFESSIONAL, TEMPORARY, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "certification_type")
    private CertificationType certificationType = CertificationType.CERTIFIED;

    /**
     * Teacher role: LEAD_TEACHER, CO_TEACHER, or SPECIALIST
     * Co-teachers follow assigned IEP students from class to class
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private TeacherRole role = TeacherRole.LEAD_TEACHER;

    /**
     * Students assigned to this co-teacher (for IEP support)
     * Only populated if role = CO_TEACHER
     * Co-teachers follow these students throughout the day from class to class
     */
    @ManyToMany
    @JoinTable(
        name = "coteacher_students",
        joinColumns = @JoinColumn(name = "teacher_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @ToString.Exclude
    private List<Student> assignedStudents = new ArrayList<>();

    /**
     * Teacher role: TEACHER, CO_TEACHER, PARAPROFESSIONAL, SUBSTITUTE
     * @deprecated Use 'role' field instead
     */
    @Deprecated
    @Column(name = "teacher_role")
    private String teacherRole = "TEACHER"; // TEACHER, CO_TEACHER, PARAPROFESSIONAL, SUBSTITUTE

    /**
     * Is this teacher a substitute?
     */
    @Column(name = "is_substitute")
    private Boolean isSubstitute = false;

    /**
     * Substitute assignment date (for tracking substitutes per week/month)
     */
    @Column(name = "substitute_date")
    private java.time.LocalDate substituteDate;

    /**
     * Minimum number of different courses this teacher should teach per day
     * Default: 3 courses
     * Adjustable by administrators for workload balancing
     */
    @Column(name = "min_courses_per_day")
    @Min(value = 0, message = "Minimum courses per day cannot be negative")
    @Max(value = 10, message = "Minimum courses per day cannot exceed 10")
    private Integer minCoursesPerDay = 3;

    /**
     * Maximum number of different courses this teacher can teach per day
     * Default: 4 courses
     * Adjustable by administrators for workload balancing
     */
    @Column(name = "max_courses_per_day")
    @Min(value = 0, message = "Maximum courses per day cannot be negative")
    @Max(value = 10, message = "Maximum courses per day cannot exceed 10")
    private Integer maxCoursesPerDay = 4;

    @Min(value = 0, message = "Maximum hours per week cannot be negative")
    @Max(value = 168, message = "Maximum hours per week cannot exceed 168")
    private Integer maxHoursPerWeek = 40;

    /**
     * Primary campus this teacher is assigned to (for multi-campus support)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_campus_id")
    private Campus primaryCampus;
    private Integer maxConsecutiveHours = 4;
    private Integer preferredBreakMinutes = 30;
    @Min(value = 0, message = "Current week hours cannot be negative")
    private Integer currentWeekHours = 0;

    /**
     * Designated home room for this teacher
     * This is the primary classroom where the teacher conducts most classes
     * Helps maintain consistency and reduces teacher movement between rooms
     */
    @ManyToOne
    @JoinColumn(name = "home_room_id")
    private Room homeRoom;

    /**
     * Planning period number for this teacher (1-7)
     * ADDED: December 15, 2025 - Real-world scheduling support
     *
     * Every teacher gets one planning period per day for:
     * - Lesson planning and preparation
     * - Grading assignments
     * - Parent communication
     * - Professional meetings
     *
     * Example schedules:
     * - Teacher Maria: Period 4 is planning (teaches periods 1,2,3,5,6,7)
     * - Geometry teacher: Period 5 is planning (teaches periods 1,2,3,4,6,7)
     *
     * null = no planning period assigned yet (system will assign one)
     */
    @Column(name = "planning_period")
    @Min(value = 1, message = "Planning period must be between 1 and 7")
    @Max(value = 7, message = "Planning period must be between 1 and 7")
    private Integer planningPeriod;

    // ========================================================================
    // SOFT DELETE SUPPORT - Preserves historical data
    // ========================================================================

    /**
     * Soft delete flag - when true, teacher is deleted but data preserved
     * Prevents foreign key constraint violations
     * Allows historical schedule/grade/attendance data to remain intact
     */
    @Column(name = "deleted")
    private Boolean deleted = false;

    /**
     * Timestamp when this teacher was soft deleted
     */
    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    /**
     * Username of administrator who performed the soft delete
     */
    @Column(name = "deleted_by")
    private String deletedBy;

    /**
     * Maximum number of class periods this teacher can teach per day
     * Standard: 6-7 periods, Block schedule: 3-4 periods
     * Default: 7 periods
     */
    @Column(name = "max_periods_per_day")
    @Min(value = 0, message = "Maximum periods per day cannot be negative")
    @Max(value = 10, message = "Maximum periods per day cannot exceed 10")
    private Integer maxPeriodsPerDay = 7;

    /**
     * Special assignments/duties for this teacher
     * Examples: Lunch Duty, Hall Monitor, Bus Duty, After-School Supervision,
     * Department Chair, Club Sponsor, Athletic Coach, etc.
     */
    @ElementCollection
    @CollectionTable(name = "teacher_special_assignments", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "assignment", columnDefinition = "TEXT")
    @ToString.Exclude
    private List<String> specialAssignments = new ArrayList<>();

    /**
     * Version field for optimistic locking to prevent race conditions
     * on concurrent updates to currentWeekHours and other fields
     */
    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    private PriorityLevel priorityLevel = PriorityLevel.NORMAL;

    private Double utilizationRate = 0.0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "special_assignment", columnDefinition = "TEXT")
    private String specialAssignment;

    // ========================================================================
    // PHASE 6A: TEACHER AVAILABILITY CONSTRAINTS
    // ========================================================================

    /**
     * Unavailable time blocks stored as JSON
     * Format: [
     *   {"dayOfWeek": "MONDAY", "startTime": "09:00:00", "endTime": "10:00:00", "reason": "Department Meeting"},
     *   {"dayOfWeek": "WEDNESDAY", "startTime": "14:00:00", "endTime": "15:00:00", "reason": "IEP Meetings"}
     * ]
     *
     * Phase 6A: Teacher Availability Constraints
     * Allows administrators to define when teachers are unavailable for scheduling
     * Scheduler will enforce these as HARD constraints
     *
     * @since Phase 6A - December 2, 2025
     */
    @Column(name = "unavailable_times", columnDefinition = "TEXT")
    private String unavailableTimes;

    /**
     * Room preferences stored as JSON
     * Contains teacher's preferred rooms and whether they are restrictions or preferences
     * Format: {
     *   "preferredRoomIds": [101, 205, 310],
     *   "restrictedToRooms": false,
     *   "preferenceStrength": "MEDIUM"
     * }
     *
     * Phase 6B: Room Preferences
     * Allows administrators to specify which rooms a teacher prefers or is restricted to
     * Scheduler will enforce restrictions as HARD constraints and preferences as SOFT constraints
     *
     * @since Phase 6B - December 3, 2025
     */
    @Column(name = "room_preferences", columnDefinition = "TEXT")
    private String roomPreferencesJson;

    @OneToMany(mappedBy = "teacher", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @ToString.Exclude
    private List<ScheduleSlot> scheduleSlots = new ArrayList<>();

    @OneToMany(mappedBy = "teacher", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @ToString.Exclude
    private List<Course> courses = new ArrayList<>();

    /**
     * Subject certifications (FLDOE certifications by subject)
     */
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<SubjectCertification> subjectCertifications = new ArrayList<>();

    /**
     * Special duty assignments for this teacher
     * Examples: Lunch Duty, Hall Monitor, Bus Duty, After-School Supervision, etc.
     */
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<SpecialDutyAssignment> specialDutyAssignments = new ArrayList<>();

    @Transient
    private String combinedName; // For import (FirstName LastName)

    public void parseCombinedName() {
        if (combinedName != null && combinedName.contains(" ")) {
            String[] parts = combinedName.split(" ", 2);
            this.firstName = parts[0].trim();
            this.lastName = parts[1].trim();
            this.name = combinedName.trim();
        } else if (combinedName != null) {
            this.name = combinedName.trim();
        }
    }

    public boolean getActive() {
        return active;
    }

    public boolean isAvailable() {
        return active && (currentWeekHours == null || currentWeekHours < maxHoursPerWeek);
    }

    public int getRemainingHours() {
        int max = maxHoursPerWeek != null ? maxHoursPerWeek : 40;
        int current = currentWeekHours != null ? currentWeekHours : 0;
        return Math.max(0, max - current);
    }

    public double getUtilizationRate() {
        if (utilizationRate != null)
            return utilizationRate;
        if (maxHoursPerWeek == null || maxHoursPerWeek == 0)
            return 0.0;
        return (currentWeekHours != null ? currentWeekHours : 0) * 100.0 / maxHoursPerWeek;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // ========================================================================
    // CERTIFICATION HELPER METHODS
    // ========================================================================

    /**
     * Check if teacher has a valid certification for a specific subject
     */
    public boolean hasCertificationForSubject(String subject) {
        if (subjectCertifications == null || subject == null) {
            return false;
        }
        return subjectCertifications.stream()
                .anyMatch(cert -> cert.isValid() &&
                                  cert.getSubject() != null &&
                                  cert.getSubject().equalsIgnoreCase(subject.trim()));
    }

    /**
     * Check if teacher has a valid certification for subject and grade level
     */
    public boolean hasCertificationForSubjectAndGrade(String subject, String gradeLevel) {
        if (subjectCertifications == null || subject == null) {
            return false;
        }
        return subjectCertifications.stream()
                .anyMatch(cert -> cert.isValid() &&
                                  cert.getSubject() != null &&
                                  cert.getSubject().equalsIgnoreCase(subject.trim()) &&
                                  cert.coversGradeLevel(gradeLevel));
    }

    /**
     * Get all valid subject certifications
     */
    public List<SubjectCertification> getValidCertifications() {
        if (subjectCertifications == null) {
            return new ArrayList<>();
        }
        return subjectCertifications.stream()
                .filter(SubjectCertification::isValid)
                .collect(Collectors.toList());
    }

    /**
     * Get all subjects this teacher is certified to teach
     */
    public List<String> getCertifiedSubjects() {
        if (subjectCertifications == null) {
            return new ArrayList<>();
        }
        return subjectCertifications.stream()
                .filter(SubjectCertification::isValid)
                .map(SubjectCertification::getSubject)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get count of valid certifications
     */
    public int getValidCertificationCount() {
        if (subjectCertifications == null) {
            return 0;
        }
        return (int) subjectCertifications.stream()
                .filter(SubjectCertification::isValid)
                .count();
    }

    /**
     * Check if teacher has any expiring certifications (within 90 days)
     */
    public boolean hasExpiringCertifications() {
        if (subjectCertifications == null) {
            return false;
        }
        return subjectCertifications.stream()
                .anyMatch(SubjectCertification::isExpiringSoon);
    }

    /**
     * Get list of expiring certifications
     */
    public List<SubjectCertification> getExpiringCertifications() {
        if (subjectCertifications == null) {
            return new ArrayList<>();
        }
        return subjectCertifications.stream()
                .filter(SubjectCertification::isExpiringSoon)
                .collect(Collectors.toList());
    }

    /**
     * Add a subject certification
     */
    public void addSubjectCertification(SubjectCertification certification) {
        if (subjectCertifications == null) {
            subjectCertifications = new ArrayList<>();
        }
        certification.setTeacher(this);
        subjectCertifications.add(certification);
    }

    /**
     * Remove a subject certification
     */
    public void removeSubjectCertification(SubjectCertification certification) {
        if (subjectCertifications != null) {
            subjectCertifications.remove(certification);
            certification.setTeacher(null);
        }
    }

    // ========================================================================
    // CO-TEACHER HELPER METHODS
    // ========================================================================

    /**
     * Check if this teacher is a co-teacher
     */
    public boolean isCoTeacher() {
        return role == TeacherRole.CO_TEACHER;
    }

    /**
     * Check if this teacher is a lead teacher
     */
    public boolean isLeadTeacher() {
        return role == TeacherRole.LEAD_TEACHER;
    }

    /**
     * Check if this teacher is a specialist
     */
    public boolean isSpecialist() {
        return role == TeacherRole.SPECIALIST;
    }

    /**
     * Assign a student to this co-teacher (for IEP support)
     */
    public void assignStudent(Student student) {
        if (assignedStudents == null) {
            assignedStudents = new ArrayList<>();
        }
        if (!assignedStudents.contains(student)) {
            assignedStudents.add(student);
        }
    }

    /**
     * Remove a student from this co-teacher's assigned list
     */
    public void removeAssignedStudent(Student student) {
        if (assignedStudents != null) {
            assignedStudents.remove(student);
        }
    }

    /**
     * Get count of assigned students
     */
    public int getAssignedStudentCount() {
        return assignedStudents != null ? assignedStudents.size() : 0;
    }

    /**
     * Check if a specific student is assigned to this co-teacher
     */
    public boolean hasAssignedStudent(Student student) {
        return assignedStudents != null && assignedStudents.contains(student);
    }

    // ========================================================================
    // VISUAL MANAGEMENT - Workload Indicators
    // ========================================================================

    /**
     * Get the number of courses assigned to this teacher
     */
    @Transient
    public int getCourseCount() {
        return courses != null ? courses.size() : 0;
    }

    /**
     * Get workload status indicator (emoji)
     * 🔴 None (0 courses)
     * 🟢 Light (1-3 courses)
     * 🟡 Normal (4-5 courses)
     * 🔴 Heavy (6+ courses)
     */
    @Transient
    public String getWorkloadIndicator() {
        int count = getCourseCount();
        if (count == 0) return "🔴";
        if (count <= 3) return "🟢";
        if (count <= 5) return "🟡";
        return "🔴";
    }

    /**
     * Get workload status text
     */
    @Transient
    public String getWorkloadStatus() {
        int count = getCourseCount();
        if (count == 0) return "None";
        if (count <= 3) return "Light";
        if (count <= 5) return "Normal";
        return "Heavy";
    }

    /**
     * Get workload description with count
     */
    @Transient
    public String getWorkloadDescription() {
        return String.format("%s (%d)", getWorkloadStatus(), getCourseCount());
    }

    /**
     * Check if teacher is overloaded (6+ courses)
     */
    @Transient
    public boolean isOverloaded() {
        return getCourseCount() >= 6;
    }

    /**
     * Check if teacher is underutilized (0 courses)
     */
    @Transient
    public boolean isUnderutilized() {
        return getCourseCount() == 0;
    }

    /**
     * Check if teacher has normal workload (1-5 courses)
     */
    @Transient
    public boolean hasNormalWorkload() {
        int count = getCourseCount();
        return count >= 1 && count <= 5;
    }

    /**
     * Get formatted certifications string for display
     * Returns comma-separated list of certified subjects
     */
    @Transient
    public String getCertificationsDisplay() {
        List<String> subjects = getCertifiedSubjects();
        if (subjects == null || subjects.isEmpty()) {
            return "None";
        }
        return String.join(", ", subjects);
    }

    /**
     * Get formatted courses string for display
     * Returns comma-separated list of course codes
     */
    @Transient
    public String getCoursesDisplay() {
        if (courses == null || courses.isEmpty()) {
            return "None";
        }
        return courses.stream()
                .map(Course::getCourseCode)
                .collect(Collectors.joining(", "));
    }

    // ========================================================================
    // PHASE 6A: TEACHER AVAILABILITY HELPER METHODS
    // ========================================================================

    /**
     * Get unavailable time blocks from JSON storage
     *
     * @return List of unavailable time blocks, empty list if none defined
     * @since Phase 6A - December 2, 2025
     */
    public List<com.heronix.model.dto.UnavailableTimeBlock> getUnavailableTimeBlocks() {
        if (unavailableTimes == null || unavailableTimes.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

            return mapper.readValue(
                unavailableTimes,
                mapper.getTypeFactory().constructCollectionType(
                    List.class,
                    com.heronix.model.dto.UnavailableTimeBlock.class
                )
            );
        } catch (Exception e) {
            // Log error and return empty list on parsing failure
            log.error("Error parsing unavailable times for teacher {}: {}", name, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Set unavailable time blocks and serialize to JSON
     *
     * @param blocks List of unavailable time blocks to set
     * @since Phase 6A - December 2, 2025
     */
    public void setUnavailableTimeBlocks(List<com.heronix.model.dto.UnavailableTimeBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            this.unavailableTimes = null;
            return;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            this.unavailableTimes = mapper.writeValueAsString(blocks);
        } catch (Exception e) {
            // Log error and set to null on serialization failure
            log.error("Error serializing unavailable times for teacher {}: {}", name, e.getMessage());
            this.unavailableTimes = null;
        }
    }

    /**
     * Check if teacher is available at a specific day and time
     *
     * This method checks against the unavailable time blocks defined for this teacher.
     * Returns true if teacher IS available (not in any blocked time)
     * Returns false if teacher IS NOT available (in a blocked time)
     *
     * Used by OptaPlanner scheduler to enforce availability constraints.
     *
     * @param dayOfWeek Day of week to check (MONDAY, TUESDAY, etc.)
     * @param time Time to check (e.g., 09:00, 14:30)
     * @return true if teacher is available, false if unavailable
     * @since Phase 6A - December 2, 2025
     */
    public boolean isAvailableAt(java.time.DayOfWeek dayOfWeek, java.time.LocalTime time) {
        if (dayOfWeek == null || time == null) {
            return true; // Null check - assume available if invalid input
        }

        List<com.heronix.model.dto.UnavailableTimeBlock> blocks = getUnavailableTimeBlocks();

        for (com.heronix.model.dto.UnavailableTimeBlock block : blocks) {
            if (block.contains(dayOfWeek, time)) {
                return false; // Teacher is unavailable during this block
            }
        }

        return true; // Teacher is available (not in any unavailable block)
    }

    /**
     * Check if teacher has any unavailable time blocks defined
     *
     * @return true if teacher has unavailable times configured
     * @since Phase 6A - December 2, 2025
     */
    public boolean hasUnavailableTimes() {
        return unavailableTimes != null && !unavailableTimes.trim().isEmpty();
    }

    /**
     * Add an unavailable time block
     *
     * @param block Time block to add
     * @throws IllegalArgumentException if block overlaps with existing block
     * @since Phase 6A - December 2, 2025
     */
    public void addUnavailableTimeBlock(com.heronix.model.dto.UnavailableTimeBlock block) {
        block.validate(); // Validate the block first

        List<com.heronix.model.dto.UnavailableTimeBlock> blocks = getUnavailableTimeBlocks();

        // Check for overlaps
        for (com.heronix.model.dto.UnavailableTimeBlock existing : blocks) {
            if (existing.overlaps(block)) {
                throw new IllegalArgumentException(
                    String.format("Unavailable time block overlaps with existing block: %s overlaps %s",
                        block.getDisplayString(), existing.getDisplayString())
                );
            }
        }

        blocks.add(block);
        setUnavailableTimeBlocks(blocks);
    }

    /**
     * Remove an unavailable time block at the specified index
     *
     * @param index Index of block to remove
     * @since Phase 6A - December 2, 2025
     */
    public void removeUnavailableTimeBlock(int index) {
        List<com.heronix.model.dto.UnavailableTimeBlock> blocks = getUnavailableTimeBlocks();

        if (index >= 0 && index < blocks.size()) {
            blocks.remove(index);
            setUnavailableTimeBlocks(blocks);
        }
    }

    /**
     * Clear all unavailable time blocks
     *
     * @since Phase 6A - December 2, 2025
     */
    public void clearUnavailableTimeBlocks() {
        this.unavailableTimes = null;
    }

    // ========== Phase 6B: Room Preferences Methods ==========

    /**
     * Get room preferences from JSON storage
     *
     * @return RoomPreferences object, or default preferences if not set
     * @since Phase 6B - December 3, 2025
     */
    public com.heronix.model.dto.RoomPreferences getRoomPreferences() {
        if (roomPreferencesJson == null || roomPreferencesJson.trim().isEmpty()) {
            return new com.heronix.model.dto.RoomPreferences();
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.readValue(
                roomPreferencesJson,
                com.heronix.model.dto.RoomPreferences.class
            );
        } catch (Exception e) {
            log.error("Error deserializing room preferences for teacher {}: {}", name, e.getMessage());
            return new com.heronix.model.dto.RoomPreferences();
        }
    }

    /**
     * Set room preferences and serialize to JSON
     *
     * @param preferences Room preferences to set
     * @since Phase 6B - December 3, 2025
     */
    public void setRoomPreferences(com.heronix.model.dto.RoomPreferences preferences) {
        if (preferences == null || !preferences.hasRooms()) {
            this.roomPreferencesJson = null;
            return;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            this.roomPreferencesJson = mapper.writeValueAsString(preferences);
        } catch (Exception e) {
            log.error("Error serializing room preferences for teacher {}: {}", name, e.getMessage());
            this.roomPreferencesJson = null;
        }
    }

    /**
     * Check if teacher prefers a specific room
     *
     * @param room Room to check
     * @return true if teacher prefers this room
     * @since Phase 6B - December 3, 2025
     */
    public boolean prefersRoom(com.heronix.model.domain.Room room) {
        if (room == null) {
            return false;
        }
        com.heronix.model.dto.RoomPreferences prefs = getRoomPreferences();
        return prefs.prefersRoom(room.getId());
    }

    /**
     * Check if teacher can use a specific room (based on restrictions)
     *
     * @param room Room to check
     * @return true if teacher can use this room
     * @since Phase 6B - December 3, 2025
     */
    public boolean canUseRoom(com.heronix.model.domain.Room room) {
        if (room == null) {
            return false;
        }
        com.heronix.model.dto.RoomPreferences prefs = getRoomPreferences();
        return prefs.canUseRoom(room.getId());
    }

    /**
     * Check if teacher has room preferences or restrictions defined
     *
     * @return true if teacher has room preferences configured
     * @since Phase 6B - December 3, 2025
     */
    public boolean hasRoomPreferences() {
        return roomPreferencesJson != null && !roomPreferencesJson.trim().isEmpty();
    }

    /**
     * Check if teacher is restricted to specific rooms (HARD constraint)
     *
     * @return true if restricted to specific rooms
     * @since Phase 6B - December 3, 2025
     */
    public boolean isRestrictedToRooms() {
        if (!hasRoomPreferences()) {
            return false;
        }
        return getRoomPreferences().isRestrictedToRooms();
    }

    /**
     * Clear all room preferences
     *
     * @since Phase 6B - December 3, 2025
     */
    public void clearRoomPreferences() {
        this.roomPreferencesJson = null;
    }

    // ========================================================================
    // PHASE: TEACHER PERSONNEL COMPLIANCE FIELDS
    // Federal & State Requirements (I-9, Background Checks, Certification, etc.)
    // ========================================================================

    // ========== I-9 Employment Eligibility Verification (Federal - REQUIRED) ==========

    /**
     * Date when Form I-9 was completed
     * Required by USCIS for all U.S. employees
     */
    @Column(name = "i9_completion_date")
    private java.time.LocalDate i9CompletionDate;

    /**
     * Type of document provided for I-9 verification
     * Examples: Passport, Driver's License + Social Security Card, etc.
     */
    @Column(name = "i9_document_type", length = 100)
    private String i9DocumentType;

    /**
     * Document number from I-9 verification
     */
    @Column(name = "i9_document_number", length = 100)
    private String i9DocumentNumber;

    /**
     * Expiration date for work authorization documents (if applicable)
     */
    @Column(name = "i9_expiration_date")
    private java.time.LocalDate i9ExpirationDate;

    /**
     * Name of staff member who verified I-9 documentation
     */
    @Column(name = "i9_verified_by")
    private String i9VerifiedBy;

    /**
     * Current status of I-9 form
     * Values: Completed, Pending, Expired
     */
    @Column(name = "i9_status", length = 50)
    private String i9Status;

    /**
     * File path to scanned I-9 form
     */
    @Column(name = "i9_form_path", length = 500)
    private String i9FormPath;

    // ========== Background Checks & Fingerprinting (State - REQUIRED) ==========

    /**
     * Date when background check was completed
     * Required in all 50 states for teachers
     */
    @Column(name = "background_check_date")
    private java.time.LocalDate backgroundCheckDate;

    /**
     * Current status of background check
     * Values: Passed, Failed, Pending, Expired
     */
    @Column(name = "background_check_status", length = 50)
    private String backgroundCheckStatus;

    /**
     * Expiration date for background check
     * Most states require renewal every 1-5 years
     */
    @Column(name = "background_check_expiration")
    private java.time.LocalDate backgroundCheckExpiration;

    /**
     * Type of background check performed
     * Values: FBI, State, Local, All
     */
    @Column(name = "background_check_type", length = 50)
    private String backgroundCheckType;

    /**
     * Date fingerprints were taken
     */
    @Column(name = "fingerprint_date")
    private java.time.LocalDate fingerprintDate;

    /**
     * Agency that processed fingerprints
     * Examples: State Police, FBI
     */
    @Column(name = "fingerprint_agency")
    private String fingerprintAgency;

    /**
     * Result of criminal history check
     * Values: Clear, Flagged, Under Review
     */
    @Column(name = "criminal_history_result", length = 50)
    private String criminalHistoryResult;

    /**
     * NASDTEC Clearinghouse check completed
     * National educator discipline database
     */
    @Column(name = "nasdtec_check")
    private Boolean nasdtecCheck = false;

    /**
     * File path to background check results document
     */
    @Column(name = "background_check_document_path", length = 500)
    private String backgroundCheckDocumentPath;

    // ========== Teaching Certification & Credentials (State - REQUIRED) ==========

    /**
     * State certification number (license number)
     * REQUIRED for teacher employment
     */
    @Column(name = "state_certification_number", length = 100)
    private String stateCertificationNumber;

    /**
     * State that issued the certification
     * Examples: FL, CA, TX, NY, etc.
     */
    @Column(name = "certification_state", length = 50)
    private String certificationState;

    /**
     * Date certification was issued
     */
    @Column(name = "certification_issue_date")
    private java.time.LocalDate certificationIssueDate;

    /**
     * Date certification expires
     * CRITICAL for compliance tracking
     */
    @Column(name = "certification_expiration_date")
    private java.time.LocalDate certificationExpirationDate;

    /**
     * Current status of certification
     * Values: Active, Expired, Suspended, Revoked, Pending
     */
    @Column(name = "certification_status", length = 50)
    private String certificationStatus;

    /**
     * Subject areas covered by certification
     * Examples: Mathematics, Science, Special Education
     */
    @Column(name = "certification_subject_areas", columnDefinition = "TEXT")
    private String certificationSubjectAreas;

    /**
     * Special endorsements on certification
     * Examples: ESL, Gifted, Reading Specialist
     */
    @Column(name = "certification_endorsements", columnDefinition = "TEXT")
    private String certificationEndorsements;

    /**
     * File path to certification document
     */
    @Column(name = "certification_document_path", length = 500)
    private String certificationDocumentPath;

    /**
     * States where certification is valid (reciprocity)
     */
    @Column(name = "reciprocity_states", columnDefinition = "TEXT")
    private String reciprocityStates;

    // ========== Education & Degrees (State - REQUIRED for certification) ==========

    /**
     * Highest degree earned
     * Values: Bachelor's, Master's, Doctorate, Other
     */
    @Column(name = "highest_degree", length = 100)
    private String highestDegree;

    /**
     * Field/major of highest degree
     * Examples: Mathematics, Education, Biology
     */
    @Column(name = "degree_field")
    private String degreeField;

    /**
     * University/college where degree was earned
     */
    @Column(name = "university")
    private String university;

    /**
     * Graduation date
     */
    @Column(name = "graduation_date")
    private java.time.LocalDate graduationDate;

    /**
     * Whether official transcript has been verified
     */
    @Column(name = "transcript_verified")
    private Boolean transcriptVerified = false;

    /**
     * Date transcript was verified
     */
    @Column(name = "transcript_verification_date")
    private java.time.LocalDate transcriptVerificationDate;

    // ========== Emergency Contact Information (Best Practice - REQUIRED by most districts) ==========

    /**
     * Emergency contact name
     */
    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    /**
     * Relationship to teacher
     */
    @Column(name = "emergency_contact_relationship", length = 100)
    private String emergencyContactRelationship;

    /**
     * Emergency contact primary phone
     */
    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    /**
     * Emergency contact alternate phone
     */
    @Column(name = "emergency_contact_alternate_phone", length = 20)
    private String emergencyContactAlternatePhone;

    /**
     * Medical conditions, allergies, medications
     */
    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions;

    /**
     * Health insurance provider
     */
    @Column(name = "health_insurance_provider")
    private String healthInsuranceProvider;

    /**
     * Health insurance policy number
     */
    @Column(name = "health_insurance_policy_number", length = 100)
    private String healthInsurancePolicyNumber;

    // ========== Employment History (Best Practice - Recommended) ==========

    /**
     * Date teacher was hired
     */
    @Column(name = "hire_date")
    private java.time.LocalDate hireDate;

    /**
     * First day of work
     */
    @Column(name = "start_date")
    private java.time.LocalDate startDate;

    /**
     * Contract type
     * Values: Full-time, Part-time, Substitute, Contract
     */
    @Column(name = "contract_type", length = 50)
    private String contractType;

    /**
     * Employment status
     * Values: Active, On Leave, Resigned, Terminated
     */
    @Column(name = "employment_status", length = 50)
    private String employmentStatus;

    /**
     * Total years of teaching experience
     */
    @Column(name = "years_of_experience")
    private Integer yearsOfExperience = 0;

    /**
     * Previous school districts where teacher worked
     */
    @Column(name = "previous_school_districts", columnDefinition = "TEXT")
    private String previousSchoolDistricts;

    /**
     * Termination date (if applicable)
     */
    @Column(name = "termination_date")
    private java.time.LocalDate terminationDate;

    /**
     * Reason for termination (if applicable)
     */
    @Column(name = "termination_reason", columnDefinition = "TEXT")
    private String terminationReason;

    // ========== TB Test / Health Screening (State - REQUIRED in many states) ==========

    /**
     * Date of tuberculosis test
     * Many states require TB testing before employment
     */
    @Column(name = "tb_test_date")
    private java.time.LocalDate tbTestDate;

    /**
     * Result of TB test
     * Values: Negative, Positive, Pending
     */
    @Column(name = "tb_test_result", length = 50)
    private String tbTestResult;

    /**
     * TB test expiration date
     * Usually requires annual renewal
     */
    @Column(name = "tb_test_expiration")
    private java.time.LocalDate tbTestExpiration;

    /**
     * Date of physical exam (if required)
     */
    @Column(name = "physical_exam_date")
    private java.time.LocalDate physicalExamDate;

    /**
     * Physical exam status
     * Values: Passed, Failed, Pending
     */
    @Column(name = "physical_exam_status", length = 50)
    private String physicalExamStatus;

    /**
     * File path to health clearance document
     */
    @Column(name = "health_clearance_document_path", length = 500)
    private String healthClearanceDocumentPath;

    // ========== Professional Development (State - Varies) ==========

    /**
     * Total professional development hours completed
     */
    @Column(name = "professional_development_hours")
    private Integer professionalDevelopmentHours = 0;

    /**
     * Required PD hours for license renewal
     */
    @Column(name = "required_pd_hours")
    private Integer requiredPdHours = 0;

    /**
     * Deadline for completing required PD hours
     */
    @Column(name = "pd_deadline")
    private java.time.LocalDate pdDeadline;

    /**
     * Date of last completed professional development
     */
    @Column(name = "last_pd_completion_date")
    private java.time.LocalDate lastPdCompletionDate;

    // ========== Salary & Compensation (HR/Payroll) ==========

    /**
     * Annual salary
     */
    @Column(name = "salary")
    private java.math.BigDecimal salary;

    /**
     * Pay grade (e.g., "Step 5, Column B")
     */
    @Column(name = "pay_grade", length = 50)
    private String payGrade;

    /**
     * Pay type
     * Values: Salary, Hourly
     */
    @Column(name = "pay_type", length = 50)
    private String payType;

    /**
     * Contracted work days per year
     * Examples: 180, 190, 220
     */
    @Column(name = "contracted_days")
    private Integer contractedDays;

    // ========== ADA Accommodations (ADA Compliance) ==========

    /**
     * ADA accommodations (if applicable)
     */
    @Column(name = "ada_accommodations", columnDefinition = "TEXT")
    private String adaAccommodations;

    /**
     * Date accommodation was requested
     */
    @Column(name = "accommodation_request_date")
    private java.time.LocalDate accommodationRequestDate;

    /**
     * Accommodation approval status
     * Values: Approved, Denied, Pending
     */
    @Column(name = "accommodation_approval_status", length = 50)
    private String accommodationApprovalStatus;

    // ========================================================================
    // COMPLIANCE HELPER METHODS
    // ========================================================================

    /**
     * Check if teacher's certification is expiring within the specified number of days
     *
     * @param days Number of days to check ahead
     * @return true if certification expires within the specified days
     */
    @Transient
    public boolean isCertificationExpiringSoon(int days) {
        if (certificationExpirationDate == null) {
            return false;
        }
        java.time.LocalDate threshold = java.time.LocalDate.now().plusDays(days);
        return certificationExpirationDate.isBefore(threshold) || certificationExpirationDate.isEqual(threshold);
    }

    /**
     * Check if teacher's background check is expiring within the specified number of days
     *
     * @param days Number of days to check ahead
     * @return true if background check expires within the specified days
     */
    @Transient
    public boolean isBackgroundCheckExpiringSoon(int days) {
        if (backgroundCheckExpiration == null) {
            return false;
        }
        java.time.LocalDate threshold = java.time.LocalDate.now().plusDays(days);
        return backgroundCheckExpiration.isBefore(threshold) || backgroundCheckExpiration.isEqual(threshold);
    }

    /**
     * Check if teacher's TB test is expiring within the specified number of days
     *
     * @param days Number of days to check ahead
     * @return true if TB test expires within the specified days
     */
    @Transient
    public boolean isTbTestExpiringSoon(int days) {
        if (tbTestExpiration == null) {
            return false;
        }
        java.time.LocalDate threshold = java.time.LocalDate.now().plusDays(days);
        return tbTestExpiration.isBefore(threshold) || tbTestExpiration.isEqual(threshold);
    }

    /**
     * Check if teacher's I-9 work authorization is expiring within the specified number of days
     *
     * @param days Number of days to check ahead
     * @return true if I-9 expires within the specified days
     */
    @Transient
    public boolean isI9ExpiringSoon(int days) {
        if (i9ExpirationDate == null) {
            return false; // No expiration date means no expiration (permanent resident/citizen)
        }
        java.time.LocalDate threshold = java.time.LocalDate.now().plusDays(days);
        return i9ExpirationDate.isBefore(threshold) || i9ExpirationDate.isEqual(threshold);
    }

    /**
     * Check if teacher has any expiring compliance items (60-day threshold for cert, 30 days for others)
     *
     * @return true if any compliance item is expiring soon
     */
    @Transient
    public boolean hasExpiringCompliance() {
        return isCertificationExpiringSoon(60) ||
               isBackgroundCheckExpiringSoon(30) ||
               isTbTestExpiringSoon(30) ||
               isI9ExpiringSoon(30);
    }

    /**
     * Check if teacher is fully compliant (all required fields completed and valid)
     *
     * @return true if teacher meets all compliance requirements
     */
    @Transient
    public boolean isFullyCompliant() {
        // I-9 required
        boolean i9Complete = i9CompletionDate != null && "Completed".equals(i9Status);

        // Background check required and not expired
        boolean backgroundCheckValid = backgroundCheckDate != null &&
                                       "Passed".equals(backgroundCheckStatus) &&
                                       (backgroundCheckExpiration == null ||
                                        backgroundCheckExpiration.isAfter(java.time.LocalDate.now()));

        // Certification required and active
        boolean certificationValid = stateCertificationNumber != null &&
                                    "Active".equals(certificationStatus) &&
                                    (certificationExpirationDate == null ||
                                     certificationExpirationDate.isAfter(java.time.LocalDate.now()));

        return i9Complete && backgroundCheckValid && certificationValid;
    }

    /**
     * Get compliance status summary for display
     *
     * @return String describing compliance status
     */
    @Transient
    public String getComplianceStatus() {
        if (isFullyCompliant()) {
            if (hasExpiringCompliance()) {
                return "⚠️ Expiring Soon";
            }
            return "✅ Compliant";
        }
        return "❌ Non-Compliant";
    }
}
