package com.heronix.model.domain;

import com.heronix.model.enums.CourseCategory;
import com.heronix.model.enums.CourseType;
import com.heronix.model.enums.EducationLevel;
import com.heronix.model.enums.PriorityLevel;
import com.heronix.model.enums.RoomType;
import com.heronix.model.enums.ScheduleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Course code is required")
    @Size(min = 2, max = 20, message = "Course code must be between 2 and 20 characters")
    @Column(nullable = false, unique = true)
    private String courseCode;

    @NotNull(message = "Course name is required")
    @Size(min = 1, max = 200, message = "Course name must be between 1 and 200 characters")
    @Column(nullable = false)
    private String courseName;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 100, message = "Subject cannot exceed 100 characters")
    private String subject;

    /**
     * Subject area this course belongs to (structured hierarchy)
     * Phase 1: Subject Area Enhancement - December 6, 2025
     *
     * This replaces the String 'subject' field with a structured relationship.
     * The String 'subject' field is kept for backward compatibility.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_area_id")
    private SubjectArea subjectArea;

    @Enumerated(EnumType.STRING)
    private EducationLevel level;

    /**
     * Course type/difficulty level (AP, Honors, Regular, etc.)
     * Separate from EducationLevel which represents grade level
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "course_type")
    private CourseType courseType = CourseType.REGULAR;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 300, message = "Duration cannot exceed 300 minutes")
    private Integer durationMinutes = 50;

    // UPDATED December 15, 2025: Changed from 5 to 6 to match real-world scheduling
    // Standard school day: 7 periods (6 teaching + 1 planning)
    // Most courses taught 6 periods per week (one section per period)
    // CRITICAL: @Min(1) prevents division by zero in room assignment and student distribution!
    @NotNull(message = "Sessions per week is required")
    @Min(value = 1, message = "Sessions per week must be at least 1 (prevents division by zero)")
    @Max(value = 10, message = "Sessions per week cannot exceed 10")
    private Integer sessionsPerWeek = 6;

    // ========================================================================
    // COURSE CAPACITY MANAGEMENT
    // ========================================================================

    /**
     * Minimum students required for course to run
     * Default: 25 students (83% of max capacity)
     */
    @Min(value = 0, message = "Minimum students cannot be negative")
    @Max(value = 999, message = "Minimum students cannot exceed 999")
    @Column(name = "min_students")
    private Integer minStudents = 25;

    /**
     * Optimal/target class size for this course
     * Default: 28 students (93% of max capacity)
     */
    @Min(value = 0, message = "Optimal students cannot be negative")
    @Max(value = 999, message = "Optimal students cannot exceed 999")
    @Column(name = "optimal_students")
    private Integer optimalStudents = 28;

    /**
     * Maximum students allowed in this course
     * Default: 30 students
     */
    @Min(value = 0, message = "Maximum students cannot be negative")
    @Max(value = 999, message = "Maximum students cannot exceed 999")
    private Integer maxStudents = 30;

    /**
     * Current enrollment count
     */
    private Integer currentEnrollment = 0;

    /**
     * Whether this is a core required course (true) or elective (false)
     * Core courses: Math, English, Science, Social Studies
     * Electives: Art, Music, PE, Foreign Language, etc.
     */
    @Column(name = "is_core_required")
    private Boolean isCoreRequired = false;

    /**
     * Credit hours/units for this course (for GPA calculation)
     * Default: 1.0 credit (standard semester course)
     * Common values:
     *   - 0.5 = Half credit (semester elective)
     *   - 1.0 = Full credit (standard year-long course)
     *   - 1.5 = Extended credit (advanced/extended courses)
     *   - 2.0 = Double credit (intensive programs)
     *
     * Added: December 12, 2025 - Service Audit 100% Completion
     */
    @Column(name = "credits", nullable = true)
    private Double credits = 1.0;

    /**
     * Priority mode for student placement
     * Options: GPA_BASED, FIRST_COME_FIRST_SERVE, LOTTERY, SENIORITY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority_mode")
    private com.heronix.model.enums.CoursePriorityMode priorityMode =
        com.heronix.model.enums.CoursePriorityMode.GPA_BASED;

    /**
     * Allow waitlist for this course when full?
     */
    @Column(name = "allow_waitlist")
    private Boolean allowWaitlist = true;

    /**
     * Maximum waitlist size
     */
    @Column(name = "max_waitlist")
    private Integer maxWaitlist = 10;

    /**
     * Minimum GPA required for this course (optional)
     * Example: AP courses might require 3.0 GPA
     */
    @Column(name = "min_gpa_required")
    private Double minGPARequired;

    // ========================================================================
    // GRADE LEVEL ELIGIBILITY (Phase 4B - Core vs. Elective System)
    // ========================================================================

    /**
     * Course category: CORE (required) or ELECTIVE (optional)
     * CORE: Required courses like English, Math, Science - all students must take
     * ELECTIVE: Optional courses like HVAC, Art, Music - student-selected
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "course_category")
    private CourseCategory courseCategory = CourseCategory.CORE;

    /**
     * Minimum grade level for this course (9-12 for high school)
     * Example: 9 = 9th graders and above can take this course
     * null = no minimum restriction
     */
    @Column(name = "min_grade_level")
    private Integer minGradeLevel;

    /**
     * Maximum grade level for this course (9-12 for high school)
     * Example: 10 = up to 10th graders can take this course
     * null = no maximum restriction
     */
    @Column(name = "max_grade_level")
    private Integer maxGradeLevel;

    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType;

    // ========================================================================
    // TIME SLOT AND CONFLICT DETECTION (Phase 7D)
    // ========================================================================

    /**
     * Period identifier for this course (e.g., "1", "2", "A-Block", "B-Block")
     * Used for simple period-based scheduling
     */
    @Column(name = "period")
    private String period;

    /**
     * Days of week this course meets
     * Format: "MWF" (Mon/Wed/Fri), "TTH" (Tue/Thu), "MTWTHF" (All weekdays)
     * Or numeric: "135" (Mon/Wed/Fri), "24" (Tue/Thu)
     */
    @Column(name = "days_of_week")
    private String daysOfWeek;

    /**
     * Start time for this course (optional, for precise scheduling)
     * Example: 09:00:00 for 9:00 AM
     */
    @Column(name = "start_time")
    private java.time.LocalTime startTime;

    /**
     * End time for this course (optional, for precise scheduling)
     * Example: 10:00:00 for 10:00 AM
     */
    @Column(name = "end_time")
    private java.time.LocalTime endTime;

    /**
     * Specific activity type for PE and specialized courses
     * Used for fine-grained room matching in scheduling
     *
     * Examples:
     *   PE Courses: "Basketball", "Volleyball", "Weights", "Dance", "Karate"
     *   Other: Leave null for non-PE courses
     *
     * This field works in conjunction with Room.activityTags to ensure
     * courses are scheduled in appropriate rooms (e.g., Basketball → Gymnasium with basketball court)
     *
     * @since Phase 5F - December 2, 2025
     */
    @Column(name = "activity_type")
    private String activityType;

    // ========================================================================
    // EQUIPMENT REQUIREMENTS (Phase 6D - December 3, 2025)
    // ========================================================================

    /**
     * Whether this course requires a projector
     * Phase 6D: Room Equipment Matching
     */
    @Column(name = "requires_projector")
    private Boolean requiresProjector = false;

    /**
     * Whether this course requires a smartboard/interactive whiteboard
     * Phase 6D: Room Equipment Matching
     */
    @Column(name = "requires_smartboard")
    private Boolean requiresSmartboard = false;

    /**
     * Whether this course requires computers for students
     * Phase 6D: Room Equipment Matching
     */
    @Column(name = "requires_computers")
    private Boolean requiresComputers = false;

    /**
     * Specific room type required for this course (overrides general equipment matching)
     * Examples: SCIENCE_LAB, COMPUTER_LAB, GYMNASIUM, ART_STUDIO
     * If set, course will strongly prefer rooms of this type
     * Phase 6D: Room Equipment Matching
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "required_room_type")
    private RoomType requiredRoomType;

    /**
     * Additional specialized equipment requirements (comma-separated)
     * Examples: "3D Printer", "Kiln", "Stage Lighting", "Basketball Court", "Microscopes"
     * Used for matching courses to specialized rooms beyond standard equipment
     * Phase 6D: Room Equipment Matching
     */
    @Column(name = "additional_equipment", length = 500)
    private String additionalEquipment;

    // ========================================================================
    // MULTI-ROOM SUPPORT (Phase 6E - December 3, 2025)
    // ========================================================================

    /**
     * Whether this course uses multiple rooms simultaneously
     * Phase 6E: Multi-Room Courses
     */
    @Column(name = "uses_multiple_rooms")
    private Boolean usesMultipleRooms = false;

    /**
     * Maximum walking time between rooms (in minutes)
     * Only applicable if usesMultipleRooms = true
     * Phase 6E: Multi-Room Courses
     */
    @Column(name = "max_room_distance_minutes")
    private Integer maxRoomDistanceMinutes;

    /**
     * Phase 6E: Multiple room assignments for this course
     * Supports team teaching, lab/lecture splits, overflow rooms, etc.
     */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CourseRoomAssignment> roomAssignments = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    private boolean requiresLab = false;
    private Integer optimalStartHour;
    private Integer complexityScore = 5;

    @Enumerated(EnumType.STRING)
    private PriorityLevel priorityLevel = PriorityLevel.NORMAL;

    @Column(name = "is_singleton")
    private Boolean isSingleton = false;

    @Column(name = "num_sections_needed")
    private Integer numSectionsNeeded = 1;

    @Column(name = "is_zero_period_eligible")
    private Boolean isZeroPeriodEligible = false;

    @Column(columnDefinition = "TEXT")
    private String requiredResources;

    @Column(columnDefinition = "TEXT")
    private String prerequisites;

    /**
     * Required certifications to teach this course
     * Example: ["Mathematics 6-12", "AP Calculus", "Texas Teaching Certificate"]
     */
    @ElementCollection
    @CollectionTable(name = "course_required_certifications", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "certification")
    private List<String> requiredCertifications = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ========================================================================
    // DEPRECATED: teacher and room fields - Use CourseSections instead
    // ========================================================================
    // Course is a CATALOG ENTRY (metadata about the course)
    // CourseSection is a SCHEDULED CLASS (teacher, room, period, enrollment)
    //
    // These fields are kept for backward compatibility but marked @Deprecated
    // New code should use sections list instead
    // One course can have multiple sections with different teachers/rooms
    // ========================================================================

    /**
     * @deprecated Use sections list instead. This field is kept for backward compatibility.
     * For multi-section courses, this represents the "primary" teacher (usually section 1)
     */
    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    @ToString.Exclude
    private Teacher teacher;

    /**
     * @deprecated Use sections list instead. This field is kept for backward compatibility.
     * For multi-section courses, this represents the "primary" room (usually section 1)
     */
    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    @ToString.Exclude
    private Room room;

    /**
     * Sections of this course
     * One course (e.g., "English 1") can have multiple sections
     * Each section has its own teacher, room, period, and enrollment
     */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<CourseSection> sections = new ArrayList<>();

    /**
     * Schedule slots for this course - Using LAZY fetch by default
     * Use repository methods with JOIN FETCH when accessing slots outside transactions
     * to prevent LazyInitializationException
     */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ScheduleSlot> scheduleSlots = new ArrayList<>();

    @ManyToMany(mappedBy = "enrolledCourses")
    @ToString.Exclude
    private List<Student> students = new ArrayList<>();

    /**
     * Co-teachers assigned to this course
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "course_co_teachers",
        joinColumns = @JoinColumn(name = "course_id"),
        inverseJoinColumns = @JoinColumn(name = "co_teacher_id")
    )
    @ToString.Exclude
    private List<CoTeacher> coTeachers = new ArrayList<>();

    @Transient
    private String combinedCodeName;

    public void parseCombinedCodeName() {
        if (combinedCodeName != null && combinedCodeName.contains(" - ")) {
            String[] parts = combinedCodeName.split(" - ", 2);
            this.courseCode = parts[0].trim();
            this.courseName = parts[1].trim();
        }
    }

    public boolean getActive() {
        return active;
    }

    public boolean isActive() {
        return active;
    }

    public boolean getRequiresLab() {
        return requiresLab;
    }

    public boolean isRequiresLab() {
        return requiresLab;
    }

    // ========================================================================
    // CAPACITY MANAGEMENT UTILITY METHODS
    // ========================================================================

    /**
     * Check if course is at minimum capacity
     */
    public boolean isAtMinimumCapacity() {
        return currentEnrollment != null && minStudents != null
            && currentEnrollment >= minStudents;
    }

    /**
     * Check if course is at optimal capacity
     */
    public boolean isAtOptimalCapacity() {
        return currentEnrollment != null && optimalStudents != null
            && currentEnrollment >= optimalStudents;
    }

    /**
     * Check if course is full (at max capacity)
     */
    public boolean isFull() {
        return currentEnrollment != null && maxStudents != null && currentEnrollment >= maxStudents;
    }

    /**
     * Get number of available seats
     */
    public int getAvailableSeats() {
        int max = maxStudents != null ? maxStudents : 30;
        int current = currentEnrollment != null ? currentEnrollment : 0;
        return Math.max(0, max - current);
    }

    /**
     * Get number of seats needed to reach minimum
     */
    public int getSeatsNeededForMinimum() {
        if (currentEnrollment == null || minStudents == null) return 0;
        return Math.max(0, minStudents - currentEnrollment);
    }

    /**
     * Get capacity status indicator
     * Returns: "Under", "Good", "Optimal", "Full", "Over"
     */
    public String getCapacityStatus() {
        if (currentEnrollment == null || minStudents == null || maxStudents == null) {
            return "Unknown";
        }

        if (currentEnrollment > maxStudents) return "Over";
        if (currentEnrollment >= maxStudents) return "Full";
        if (currentEnrollment >= optimalStudents) return "Optimal";
        if (currentEnrollment >= minStudents) return "Good";
        return "Under";
    }

    /**
     * Get fill percentage
     */
    public double getEnrollmentPercentage() {
        if (maxStudents == null || maxStudents == 0)
            return 0.0;
        return (currentEnrollment != null ? currentEnrollment : 0) * 100.0 / maxStudents;
    }

    /**
     * Get fill percentage (alias for compatibility)
     */
    public double getFillPercentage() {
        return getEnrollmentPercentage();
    }

    /**
     * Check if course is under-enrolled (below minimum)
     */
    public boolean isUnderEnrolled() {
        if (currentEnrollment == null || minStudents == null) return false;
        return currentEnrollment < minStudents;
    }

    /**
     * Check if course is over-enrolled (above maximum)
     */
    public boolean isOverEnrolled() {
        if (currentEnrollment == null || maxStudents == null) return false;
        return currentEnrollment > maxStudents;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Get the activity type for this course
     * @return Activity type (e.g., "Basketball", "Weights") or null for non-PE courses
     */
    public String getActivityType() {
        return activityType;
    }

    /**
     * Set the activity type for this course
     * @param activityType Activity type (e.g., "Basketball", "Weights")
     */
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    // ✅ Convenience methods for backward compatibility
    public Integer getDuration() {
        return this.durationMinutes != null ? this.durationMinutes : 50;
    }

    public void setDuration(Integer duration) {
        this.durationMinutes = duration;
    }

    // ═══════════════════════════════════════════════════════════════
    // VISUAL MANAGEMENT - Assignment Status Indicators
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get the assignment status for visual management
     *
     * @return CourseAssignmentStatus based on teacher and room assignment
     */
    @Transient
    public com.heronix.model.enums.CourseAssignmentStatus getAssignmentStatus() {
        return com.heronix.model.enums.CourseAssignmentStatus.from(
            this.teacher != null,
            this.room != null
        );
    }

    /**
     * Get the status indicator emoji for UI display
     *
     * @return Unicode emoji (🟢, 🟡, or 🔴)
     */
    @Transient
    public String getStatusIndicator() {
        return getAssignmentStatus().getIndicator();
    }

    /**
     * Get the status color code for CSS styling
     *
     * @return Hex color code (e.g., "#4CAF50")
     */
    @Transient
    public String getStatusColor() {
        return getAssignmentStatus().getColorCode();
    }

    /**
     * Get human-readable status description
     *
     * @return Status description string
     */
    @Transient
    public String getStatusDescription() {
        return getAssignmentStatus().getDescription();
    }

    /**
     * Check if course is fully assigned (for filtering)
     *
     * @return true if both teacher and room are assigned
     */
    @Transient
    public boolean isFullyAssigned() {
        return this.teacher != null && this.room != null;
    }

    /**
     * Check if course is unassigned (for filtering)
     *
     * @return true if neither teacher nor room are assigned
     */
    @Transient
    public boolean isUnassigned() {
        return this.teacher == null && this.room == null;
    }

    /**
     * Check if course is partially assigned (for filtering)
     *
     * @return true if only teacher or only room is assigned
     */
    @Transient
    public boolean isPartiallyAssigned() {
        return (this.teacher != null && this.room == null) ||
               (this.teacher == null && this.room != null);
    }

    // ========================================================================
    // ENROLLMENT MANAGEMENT (For Phase 4 AI Assignment)
    // ========================================================================

    /**
     * Check if course has available seats
     * @return true if current enrollment < max capacity
     */
    public boolean hasAvailableSeats() {
        if (currentEnrollment == null || maxStudents == null) return false;
        return currentEnrollment < maxStudents;
    }

    /**
     * Check if course should accept waitlist
     * @return true if waitlist is enabled and not at max waitlist capacity
     */
    public boolean shouldAcceptWaitlist() {
        return Boolean.TRUE.equals(allowWaitlist) &&
               maxWaitlist != null &&
               maxWaitlist > 0;
    }

    /**
     * Increment enrollment count by 1
     * Used when assigning a student to the course
     */
    public void incrementEnrollment() {
        if (currentEnrollment == null) {
            currentEnrollment = 0;
        }
        currentEnrollment++;
    }

    /**
     * Decrement enrollment count by 1
     * Used when removing a student from the course
     */
    public void decrementEnrollment() {
        if (currentEnrollment != null && currentEnrollment > 0) {
            currentEnrollment--;
        }
    }

    /**
     * Set enrollment count directly
     * @param enrollment New enrollment count
     */
    public void setEnrollmentCount(int enrollment) {
        this.currentEnrollment = Math.max(0, enrollment);
    }

    // ========================================================================
    // GRADE LEVEL ELIGIBILITY METHODS (Phase 4B)
    // ========================================================================

    /**
     * Check if a student at the given grade level is eligible for this course
     *
     * @param studentGradeLevel The student's grade level (9-12)
     * @return true if student meets grade level requirements
     */
    @Transient
    public boolean isEligibleForGradeLevel(Integer studentGradeLevel) {
        if (studentGradeLevel == null) {
            return true; // No grade level info = allow by default
        }

        // Check minimum grade level
        if (minGradeLevel != null && studentGradeLevel < minGradeLevel) {
            return false; // Student is below minimum grade
        }

        // Check maximum grade level
        if (maxGradeLevel != null && studentGradeLevel > maxGradeLevel) {
            return false; // Student is above maximum grade
        }

        return true; // Student meets grade level requirements
    }

    /**
     * Check if a student meets GPA requirements for this course
     *
     * @param studentGPA The student's current GPA
     * @return true if student meets GPA requirements (or no requirement set)
     */
    @Transient
    public boolean meetsGPARequirement(Double studentGPA) {
        if (minGPARequired == null) {
            return true; // No GPA requirement
        }
        if (studentGPA == null) {
            return false; // GPA required but student has no GPA
        }
        return studentGPA >= minGPARequired;
    }

    /**
     * Check if student is eligible for this course (grade level + GPA)
     *
     * @param studentGradeLevel The student's grade level (9-12)
     * @param studentGPA The student's current GPA
     * @return true if student meets all requirements
     */
    @Transient
    public boolean isStudentEligible(Integer studentGradeLevel, Double studentGPA) {
        return isEligibleForGradeLevel(studentGradeLevel) && meetsGPARequirement(studentGPA);
    }

    /**
     * Check if this is a core required course
     *
     * @return true if courseCategory is CORE
     */
    @Transient
    public boolean isCoreRequiredCourse() {
        return courseCategory != null && courseCategory == CourseCategory.CORE;
    }

    /**
     * Check if this is an elective course
     *
     * @return true if courseCategory is ELECTIVE
     */
    @Transient
    public boolean isElectiveCourse() {
        return courseCategory != null && courseCategory == CourseCategory.ELECTIVE;
    }

    /**
     * Get a human-readable description of grade level requirements
     *
     * @return Description like "9th-12th grade" or "10th grade only"
     */
    @Transient
    public String getGradeLevelDescription() {
        if (minGradeLevel == null && maxGradeLevel == null) {
            return "All grades";
        }
        if (minGradeLevel != null && maxGradeLevel != null) {
            if (minGradeLevel.equals(maxGradeLevel)) {
                return minGradeLevel + "th grade only";
            }
            return minGradeLevel + "th-" + maxGradeLevel + "th grade";
        }
        if (minGradeLevel != null) {
            return minGradeLevel + "th grade and above";
        }
        return "Up to " + maxGradeLevel + "th grade";
    }

    // ========================================================================
    // COURSE SECTION MANAGEMENT METHODS
    // ========================================================================

    /**
     * Get number of sections for this course
     * @return Number of sections
     */
    @Transient
    public int getSectionCount() {
        return sections != null ? sections.size() : 0;
    }

    /**
     * Get total enrollment across all sections
     * @return Total students enrolled in all sections
     */
    @Transient
    public int getTotalEnrollment() {
        if (sections == null || sections.isEmpty()) {
            return 0;
        }
        return sections.stream()
                .mapToInt(section -> section.getCurrentEnrollment() != null ? section.getCurrentEnrollment() : 0)
                .sum();
    }

    /**
     * Get total capacity across all sections
     * @return Total seats available in all sections
     */
    @Transient
    public int getTotalCapacity() {
        if (sections == null || sections.isEmpty()) {
            return 0;
        }
        return sections.stream()
                .mapToInt(section -> section.getMaxEnrollment() != null ? section.getMaxEnrollment() : 0)
                .sum();
    }

    /**
     * Add a new section to this course
     * @param section Section to add
     */
    public void addSection(CourseSection section) {
        if (sections == null) {
            sections = new ArrayList<>();
        }
        sections.add(section);
        section.setCourse(this);
    }

    /**
     * Remove a section from this course
     * @param section Section to remove
     */
    public void removeSection(CourseSection section) {
        if (sections != null) {
            sections.remove(section);
            section.setCourse(null);
        }
    }

    /**
     * Get display string showing section count
     * @return String like "3 sections" or "1 section"
     */
    @Transient
    public String getSectionCountDisplay() {
        int count = getSectionCount();
        if (count == 0) {
            return "No sections";
        }
        return count + (count == 1 ? " section" : " sections");
    }

    /**
     * Get display string showing enrollment status
     * @return String like "75/90 enrolled (3 sections)"
     */
    @Transient
    public String getEnrollmentDisplay() {
        int total = getTotalEnrollment();
        int capacity = getTotalCapacity();
        int sectionCount = getSectionCount();

        if (sectionCount == 0) {
            return "No sections";
        }

        return String.format("%d/%d enrolled (%d %s)",
            total, capacity, sectionCount, sectionCount == 1 ? "section" : "sections");
    }
}