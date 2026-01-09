package com.heronix.model.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import java.util.ArrayList;
import java.util.List;

/**
 * Student Entity - COMPREHENSIVE ENROLLMENT ENHANCEMENT
 * Location: src/main/java/com/eduscheduler/model/domain/Student.java
 *
 * ✅ FIXED: Added lunchPeriod relationship to match LunchPeriod.students mapping
 * ✅ VERIFIED: getEnrolledCourseCount() method exists
 * ✅ ENHANCED: With null-safety and utility methods
 * ✅ ENHANCED: December 14, 2025 - Added comprehensive enrollment fields
 *    - Demographics (DOB, gender, ethnicity, race, language)
 *    - Home address & proof of residency
 *    - Special circumstances (foster care, homeless, orphan)
 *    - Immunization & health insurance tracking
 *    - Enrollment documentation tracking
 *
 * @author Heronix Scheduling System Team
 * @version 4.0.0 - Comprehensive K-12 Enrollment Enhancement
 * @since December 14, 2025
 */
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, name = "student_id")
    @NotNull(message = "Student ID cannot be null")
    @Size(min = 1, max = 20, message = "Student ID must be between 1 and 20 characters")
    private String studentId;

    @Column(nullable = false, name = "first_name")
    @NotNull(message = "First name cannot be null")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @Column(nullable = false, name = "last_name")
    @NotNull(message = "Last name cannot be null")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    @Column(nullable = false, name = "grade_level")
    @NotNull(message = "Grade level cannot be null")
    @Size(min = 1, max = 2, message = "Grade level must be between 1 and 2 characters")
    private String gradeLevel;

    @Column(name = "is_senior")
    private Boolean isSenior = false;

    @Column(name = "priority_weight")
    private Integer priorityWeight = 0;

    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Campus this student is enrolled at (for multi-campus support)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    @ToString.Exclude
    private Campus campus;

    // ========================================================================
    // DEMOGRAPHICS - CRITICAL ENROLLMENT FIELDS
    // Added: December 14, 2025 - K-12 Enrollment Enhancement
    // ========================================================================

    /**
     * Date of Birth (REQUIRED for enrollment)
     * Used for: Age verification, grade placement eligibility
     */
    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    /**
     * Gender
     * Values: "Male", "Female", "Non-binary", "Prefer not to say"
     */
    @Column(name = "gender", length = 50)
    private String gender;

    /**
     * Ethnicity (Federal reporting requirement)
     * Values: "Hispanic or Latino", "Not Hispanic or Latino"
     */
    @Column(name = "ethnicity", length = 100)
    private String ethnicity;

    /**
     * Race (Federal NCES reporting - can be multiple)
     * Values: "American Indian or Alaska Native", "Asian", "Black or African American",
     *         "Native Hawaiian or Other Pacific Islander", "White", "Two or More Races"
     */
    @Column(name = "race", length = 200)
    private String race;

    /**
     * Preferred/Primary language spoken at home
     * Used for ELL program eligibility
     */
    @Column(name = "preferred_language", length = 50)
    private String preferredLanguage;

    /**
     * Is student an English Language Learner?
     * Qualifies for ESL/ELL services
     */
    @Column(name = "is_english_learner")
    private Boolean isEnglishLearner = false;

    /**
     * Birth city (for enrollment verification)
     */
    @Column(name = "birth_city", length = 100)
    private String birthCity;

    /**
     * Birth state (for enrollment verification)
     */
    @Column(name = "birth_state", length = 2)
    private String birthState;

    /**
     * Birth country (for international students)
     */
    @Column(name = "birth_country", length = 100)
    private String birthCountry;

    // ========================================================================
    // HOME ADDRESS - REQUIRED FOR PROOF OF RESIDENCY
    // Added: December 14, 2025 - K-12 Enrollment Enhancement
    // ========================================================================

    /**
     * Home street address (REQUIRED - proof of residency)
     */
    @Column(name = "home_street_address", length = 255)
    private String homeStreetAddress;

    /**
     * Home city (REQUIRED)
     */
    @Column(name = "home_city", length = 100)
    private String homeCity;

    /**
     * Home state (REQUIRED)
     */
    @Column(name = "home_state", length = 2)
    private String homeState;

    /**
     * Home ZIP code (REQUIRED - determines district boundaries)
     */
    @Column(name = "home_zip_code", length = 10)
    private String homeZipCode;

    /**
     * County (for district assignment)
     */
    @Column(name = "home_county", length = 100)
    private String homeCounty;

    /**
     * Mailing address (if different from home address)
     */
    @Column(name = "mailing_street_address", length = 255)
    private String mailingStreetAddress;

    /**
     * Mailing city
     */
    @Column(name = "mailing_city", length = 100)
    private String mailingCity;

    /**
     * Mailing state
     */
    @Column(name = "mailing_state", length = 2)
    private String mailingState;

    /**
     * Mailing ZIP code
     */
    @Column(name = "mailing_zip_code", length = 10)
    private String mailingZipCode;

    // ========================================================================
    // SPECIAL CIRCUMSTANCES & CUSTODY STATUS
    // Added: December 14, 2025 - Federal Compliance (FAFSA, McKinney-Vento)
    // ========================================================================

    /**
     * Special living situation status
     * Values: "Traditional Family", "Foster Care", "Orphan", "Ward of Court",
     *         "Homeless/Unaccompanied", "Refugee/Asylee", "Kinship Care",
     *         "Group Home", "Independent Living"
     */
    @Column(name = "living_situation", length = 100)
    private String livingSituation = "Traditional Family";

    /**
     * Foster care status (Federal FAFSA requirement)
     */
    @Column(name = "is_foster_care")
    private Boolean isFosterCare = false;

    /**
     * Foster care agency name
     */
    @Column(name = "foster_care_agency", length = 200)
    private String fosterCareAgency;

    /**
     * Foster care case worker name
     */
    @Column(name = "foster_case_worker_name", length = 100)
    private String fosterCaseWorkerName;

    /**
     * Foster care case worker phone
     */
    @Column(name = "foster_case_worker_phone", length = 20)
    private String fosterCaseWorkerPhone;

    /**
     * Is student an orphan? (Both parents deceased)
     */
    @Column(name = "is_orphan")
    private Boolean isOrphan = false;

    /**
     * Is student a ward of the court?
     */
    @Column(name = "is_ward_of_court")
    private Boolean isWardOfCourt = false;

    /**
     * Court case number (if ward of court)
     */
    @Column(name = "court_case_number", length = 50)
    private String courtCaseNumber;

    /**
     * Is student experiencing homelessness?
     * McKinney-Vento Homeless Assistance Act
     */
    @Column(name = "is_homeless")
    private Boolean isHomeless = false;

    /**
     * Homeless situation type
     * Values: "Shelter", "Doubled Up", "Hotel/Motel", "Unsheltered", "Transitional Housing"
     */
    @Column(name = "homeless_situation_type", length = 100)
    private String homelessSituationType;

    /**
     * Is student unaccompanied youth? (Under 21, not with parent/guardian)
     */
    @Column(name = "is_unaccompanied_youth")
    private Boolean isUnaccompaniedYouth = false;

    /**
     * Refugee or Asylee status
     */
    @Column(name = "is_refugee_asylee")
    private Boolean isRefugeeAsylee = false;

    /**
     * Immigrant/Visa status (for international students)
     */
    @Column(name = "visa_status", length = 50)
    private String visaStatus;

    /**
     * Custody arrangement
     * Values: "Both Parents", "Mother Only", "Father Only", "Joint Custody",
     *         "Foster Parents", "Grandparents", "Other Relative", "Legal Guardian"
     */
    @Column(name = "custody_arrangement", length = 100)
    private String custodyArrangement;

    /**
     * Court custody documentation file path
     */
    @Column(name = "custody_documentation_path", length = 500)
    private String custodyDocumentationPath;

    /**
     * Special circumstances notes (confidential)
     */
    @Column(name = "special_circumstances_notes", columnDefinition = "TEXT")
    private String specialCircumstancesNotes;

    /**
     * Is student in kinship care? (Living with relatives, not formal foster)
     */
    @Column(name = "is_kinship_care")
    private Boolean isKinshipCare = false;

    /**
     * Is student from a military family?
     */
    @Column(name = "is_military_family")
    private Boolean isMilitaryFamily = false;

    /**
     * Is student from a migrant worker family?
     */
    @Column(name = "is_migrant_family")
    private Boolean isMigrantFamily = false;

    /**
     * Previous school name (for transfer students)
     */
    @Column(name = "previous_school_name", length = 200)
    private String previousSchoolName;

    /**
     * Previous school address
     */
    @Column(name = "previous_school_address", length = 300)
    private String previousSchoolAddress;

    /**
     * Previous school phone
     */
    @Column(name = "previous_school_phone", length = 20)
    private String previousSchoolPhone;

    /**
     * Last date attended previous school
     */
    @Column(name = "previous_school_last_date")
    private java.time.LocalDate previousSchoolLastDate;

    /**
     * Reason for transfer
     */
    @Column(name = "transfer_reason", length = 200)
    private String transferReason;

    // ========================================================================
    // IMMUNIZATION & HEALTH INSURANCE
    // Added: December 14, 2025 - K-12 Enrollment Enhancement
    // ========================================================================

    /**
     * Immunization records complete? (Required for enrollment)
     */
    @Column(name = "immunization_complete")
    private Boolean immunizationComplete = false;

    /**
     * Immunization exemption status
     * Values: "None", "Medical Exemption", "Religious Exemption", "Philosophical Exemption"
     */
    @Column(name = "immunization_exemption_type", length = 100)
    private String immunizationExemptionType;

    /**
     * Immunization exemption documentation path
     */
    @Column(name = "immunization_exemption_doc_path", length = 500)
    private String immunizationExemptionDocPath;

    /**
     * Physical examination date (required within last 12 months)
     */
    @Column(name = "physical_exam_date")
    private java.time.LocalDate physicalExamDate;

    /**
     * Physician name
     */
    @Column(name = "physician_name", length = 100)
    private String physicianName;

    /**
     * Physician phone
     */
    @Column(name = "physician_phone", length = 20)
    private String physicianPhone;

    /**
     * Dentist name
     */
    @Column(name = "dentist_name", length = 100)
    private String dentistName;

    /**
     * Dentist phone
     */
    @Column(name = "dentist_phone", length = 20)
    private String dentistPhone;

    /**
     * Health insurance provider
     */
    @Column(name = "health_insurance_provider", length = 200)
    private String healthInsuranceProvider;

    /**
     * Health insurance policy number
     */
    @Column(name = "health_insurance_policy_number", length = 100)
    private String healthInsurancePolicyNumber;

    /**
     * Health insurance group number
     */
    @Column(name = "health_insurance_group_number", length = 100)
    private String healthInsuranceGroupNumber;

    // ========================================================================
    // ENROLLMENT DOCUMENTATION TRACKING
    // Added: December 14, 2025 - K-12 Enrollment Enhancement
    // ========================================================================

    /**
     * Birth certificate verified?
     */
    @Column(name = "birth_certificate_verified")
    private Boolean birthCertificateVerified = false;

    /**
     * Birth certificate verification date
     */
    @Column(name = "birth_certificate_verified_date")
    private java.time.LocalDate birthCertificateVerifiedDate;

    /**
     * Proof of residency verified?
     */
    @Column(name = "residency_verified")
    private Boolean residencyVerified = false;

    /**
     * Residency verification document type
     * Values: "Utility Bill", "Lease Agreement", "Mortgage Statement", "Affidavit of Residency"
     */
    @Column(name = "residency_document_type", length = 100)
    private String residencyDocumentType;

    /**
     * Residency verification date
     */
    @Column(name = "residency_verified_date")
    private java.time.LocalDate residencyVerifiedDate;

    /**
     * Previous school records received?
     */
    @Column(name = "previous_school_records_received")
    private Boolean previousSchoolRecordsReceived = false;

    /**
     * Previous school records received date
     */
    @Column(name = "previous_school_records_date")
    private java.time.LocalDate previousSchoolRecordsDate;

    /**
     * Enrollment packet complete?
     */
    @Column(name = "enrollment_packet_complete")
    private Boolean enrollmentPacketComplete = false;

    /**
     * Enrollment completion date
     */
    @Column(name = "enrollment_completion_date")
    private java.time.LocalDate enrollmentCompletionDate;

    /**
     * Enrollment verified by (staff name)
     */
    @Column(name = "enrollment_verified_by", length = 100)
    private String enrollmentVerifiedBy;

    // ========================================================================
    // SOFT DELETE SUPPORT - Preserves historical data
    // ========================================================================

    /**
     * Soft delete flag - when true, student is deleted but data preserved
     * Prevents foreign key constraint violations
     * Allows historical schedule/grade/attendance data to remain intact
     */
    @Column(name = "deleted")
    private Boolean deleted = false;

    /**
     * Timestamp when this student was soft deleted
     */
    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    /**
     * Username of administrator who performed the soft delete
     */
    @Column(name = "deleted_by")
    private String deletedBy;

    /**
     * Timestamp when this record was last updated
     * Used for incremental sync with Teacher Portal and conflict detection
     */
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    /**
     * Timestamp when this record was created
     */
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    // ========================================================================
    // SPECIAL EDUCATION & ACCOMMODATIONS
    // ========================================================================

    /**
     * Individual Education Plan (IEP) status
     */
    @Column(name = "has_iep")
    private Boolean hasIEP = false;

    /**
     * 504 Plan status (accommodations for students with disabilities)
     */
    @Column(name = "has_504_plan")
    private Boolean has504Plan = false;

    /**
     * Gifted and Talented Program status
     */
    @Column(name = "is_gifted")
    private Boolean isGifted = false;

    /**
     * IEP/504 next review date (for dashboard tracking)
     */
    @Column(name = "accommodation_review_date")
    private java.time.LocalDate accommodationReviewDate;

    /**
     * Notes about special accommodations
     */
    @Column(name = "accommodation_notes", columnDefinition = "TEXT")
    private String accommodationNotes;

    /**
     * IEP (Individualized Education Program) - One-to-Many relationship
     * A student can have multiple IEPs over time, but typically only one is active
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<IEP> ieps = new ArrayList<>();

    /**
     * 504 Plan - One-to-Many relationship
     * A student can have multiple 504 plans over time, but typically only one is active
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Plan504> plan504s = new ArrayList<>();

    // ========================================================================
    // MEDICAL & HEALTH INFORMATION
    // ========================================================================

    /**
     * Medical conditions or health needs
     * Examples: Asthma, Diabetes, Allergies, Seizure Disorder, etc.
     */
    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions;

    /**
     * Medication information (if applicable)
     */
    @Column(name = "medication_info", columnDefinition = "TEXT")
    private String medicationInfo;

    /**
     * Emergency contact information
     */
    @Column(name = "emergency_contact")
    private String emergencyContact;

    /**
     * Emergency contact phone
     */
    @Column(name = "emergency_phone")
    private String emergencyPhone;

    // ========================================================================
    // QR CODE & FACIAL RECOGNITION ATTENDANCE SYSTEM
    // ========================================================================

    /**
     * Unique QR code identifier for this student
     * Format: QR-{studentId}-{UUID}
     * Used for two-factor attendance verification
     *
     * @since December 12, 2025 - QR Attendance System Phase 1
     */
    @Column(name = "qr_code_id", unique = true)
    private String qrCodeId;

    /**
     * Path to student photo (for facial recognition)
     * Example: /photos/students/2025/12345.jpg
     */
    @Column(name = "photo_path", length = 500)
    private String photoPath;

    /**
     * Student photo data (BLOB storage)
     * Captured during registration or enrollment
     * Used for visual verification and facial recognition
     */
    @Lob
    @Column(name = "photo_data", columnDefinition = "BLOB")
    private byte[] photoData;

    /**
     * Facial recognition signature/encoding
     * Extracted from photo using facial recognition algorithm
     * Used for fast face matching during QR code scans
     */
    @Lob
    @Column(name = "face_signature", columnDefinition = "BLOB")
    private byte[] faceSignature;

    /**
     * Enable/disable QR code attendance for this student
     * Default: true (enabled)
     * Can be disabled for students without QR cards
     */
    @Column(name = "qr_attendance_enabled")
    private Boolean qrAttendanceEnabled = true;

    /**
     * Enable/disable facial recognition verification for this student
     * Default: true (enabled)
     * Can be disabled if photo not available or opt-out request
     */
    @Column(name = "facial_recognition_enabled")
    private Boolean facialRecognitionEnabled = true;

    /**
     * Timestamp of last QR code scan
     * Used for duplicate scan detection and audit trail
     */
    @Column(name = "last_qr_scan")
    private java.time.LocalDateTime lastQrScan;

    // ========================================================================
    // PIN AUTHENTICATION SYSTEM (3-Layer Security: QR + Face + PIN)
    // ========================================================================

    /**
     * Student PIN (BCrypt encrypted)
     * 4-digit PIN for enhanced security when logging in via QR code
     * Format: BCrypt hash of 4-digit PIN
     *
     * Security Flow:
     * - Layer 1: QR Code ID verification
     * - Layer 2: Facial recognition (75% match)
     * - Layer 3: PIN verification (optional, configurable per student)
     *
     * @since December 13, 2025 - PIN Authentication System
     */
    @Column(name = "pin_hash", length = 60)
    private String pinHash;

    /**
     * Whether PIN is required for this student
     * If true, student must enter PIN after QR scan + facial verification
     * If false, QR + facial verification is sufficient
     *
     * Default: false (PIN optional)
     * Can be set to true for high-security students (e.g., access to restricted areas)
     */
    @Column(name = "pin_required")
    private Boolean pinRequired = false;

    /**
     * Count of consecutive failed PIN attempts
     * Incremented on each wrong PIN entry
     * Reset to 0 on successful PIN entry
     * Account locked when reaches 3 failed attempts
     */
    @Column(name = "failed_pin_attempts")
    private Integer failedPinAttempts = 0;

    /**
     * Timestamp when account will be auto-unlocked
     * Set to 30 minutes after 3rd failed PIN attempt
     * Null if account is not locked
     *
     * Security Feature:
     * - Prevents brute-force PIN guessing
     * - Auto-unlocks after cooldown period
     * - Admin can manually unlock via Student Management
     */
    @Column(name = "pin_locked_until")
    private java.time.LocalDateTime pinLockedUntil;

    /**
     * Timestamp of last successful PIN verification
     * Used for audit trail and security monitoring
     */
    @Column(name = "last_pin_verification")
    private java.time.LocalDateTime lastPinVerification;

    // ========================================================================
    // ACADEMIC YEAR TRACKING (Grade Progression)
    // ========================================================================

    /**
     * Expected graduation year (e.g., 2027 for current freshmen)
     */
    @Column(name = "graduation_year")
    private Integer graduationYear;

    /**
     * Has this student graduated?
     */
    @Column(name = "graduated")
    private Boolean graduated = false;

    /**
     * Graduation date (when they graduated)
     */
    @Column(name = "graduated_date")
    private java.time.LocalDate graduatedDate;

    /**
     * Current or last academic year ID
     */
    @Column(name = "academic_year_id")
    private Long academicYearId;

    // ========================================================================
    // ACADEMIC PERFORMANCE TRACKING
    // ========================================================================

    /**
     * Current cumulative GPA (Grade Point Average)
     * Scale: 0.0 - 4.0 (unweighted) or 0.0 - 5.0 (weighted)
     */
    @Column(name = "current_gpa")
    private Double currentGPA;

    /**
     * Cumulative unweighted GPA (4.0 scale)
     */
    @Column(name = "unweighted_gpa")
    private Double unweightedGPA;

    /**
     * Cumulative weighted GPA (5.0 scale for honors/AP)
     */
    @Column(name = "weighted_gpa")
    private Double weightedGPA;

    /**
     * Class rank (position in graduating class)
     */
    @Column(name = "class_rank")
    private Integer classRank;

    /**
     * Total class size for rank calculation
     */
    @Column(name = "class_size")
    private Integer classSize;

    /**
     * Total credits earned
     */
    @Column(name = "credits_earned")
    private Double creditsEarned = 0.0;

    /**
     * Credits required for graduation
     */
    @Column(name = "credits_required")
    private Double creditsRequired = 24.0;

    /**
     * Academic standing (Good Standing, Academic Probation, Academic Warning)
     */
    @Column(name = "academic_standing")
    private String academicStanding = "Good Standing";

    /**
     * Honor roll status (Honor Roll, High Honor Roll, Principal's List)
     */
    @Column(name = "honor_roll_status")
    private String honorRollStatus;

    /**
     * Previous term GPA (for trend analysis)
     */
    @Column(name = "previous_term_gpa")
    private Double previousTermGPA;

    /**
     * Grade history - relationship to individual course grades
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<StudentGrade> gradeHistory = new ArrayList<>();

    // ========================================================================
    // COURSE PLACEMENT & ELIGIBILITY
    // ========================================================================

    /**
     * Eligible for honors courses based on GPA threshold
     */
    @Column(name = "honors_eligible")
    private Boolean honorsEligible = false;

    /**
     * Eligible for AP courses
     */
    @Column(name = "ap_eligible")
    private Boolean apEligible = false;

    // ========================================================================
    // STUDENT PRIORITY & COURSE ASSIGNMENT
    // ========================================================================

    /**
     * Behavior score (1-5 scale)
     * 1 = Poor behavior (frequent disciplinary issues)
     * 2 = Below average behavior
     * 3 = Average behavior (meets expectations) - DEFAULT
     * 4 = Good behavior (exceeds expectations)
     * 5 = Excellent behavior (model student)
     */
    @Column(name = "behavior_score")
    private Integer behaviorScore = 3;

    /**
     * First choice elective course (highest priority)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elective_preference_1_id")
    @ToString.Exclude
    private Course electivePreference1;

    /**
     * Second choice elective course
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elective_preference_2_id")
    @ToString.Exclude
    private Course electivePreference2;

    /**
     * Third choice elective course
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elective_preference_3_id")
    @ToString.Exclude
    private Course electivePreference3;

    /**
     * Fourth choice elective course (alternate)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elective_preference_4_id")
    @ToString.Exclude
    private Course electivePreference4;

    /**
     * Special scheduling needs or notes
     */
    @Column(name = "scheduling_notes", columnDefinition = "TEXT")
    private String schedulingNotes;

    /**
     * Parent/Guardian consent for advanced placement
     */
    @Column(name = "ap_consent_given")
    private Boolean apConsentGiven = false;

    /**
     * Preferred learning style (for course matching)
     * Values: "Visual", "Auditory", "Kinesthetic", "Reading/Writing"
     */
    @Column(name = "learning_style")
    private String learningStyle;

    /**
     * Courses recommended by counselor or system
     */
    @Column(name = "recommended_courses", columnDefinition = "TEXT")
    private String recommendedCourses;

    /**
     * Academic advisor notes
     */
    @Column(name = "advisor_notes", columnDefinition = "TEXT")
    private String advisorNotes;

    // ========================================================================
    // RELATIONSHIPS
    // ========================================================================

    /**
     * Many-to-Many: Student enrolled courses
     */
    @ManyToMany
    @JoinTable(name = "student_courses", joinColumns = @JoinColumn(name = "student_id"), inverseJoinColumns = @JoinColumn(name = "course_id"))
    @ToString.Exclude
    private List<Course> enrolledCourses = new ArrayList<>();

    /**
     * Many-to-Many: Student's schedule slots (inverse side)
     */
    @ManyToMany(mappedBy = "students")
    @ToString.Exclude
    private List<ScheduleSlot> scheduleSlots = new ArrayList<>();

    /**
     * ✅ NEW: Many-to-Many relationship with LunchPeriod
     * This fixes the JPA mapping error where LunchPeriod.students
     * was mapped by a non-existent "lunchPeriod" field
     */
    @ManyToMany
    @JoinTable(name = "student_lunch_periods", joinColumns = @JoinColumn(name = "student_id"), inverseJoinColumns = @JoinColumn(name = "lunch_period_id"))
    @ToString.Exclude
    private List<LunchPeriod> lunchPeriod = new ArrayList<>();

    /**
     * One-to-One relationship with MedicalRecord
     * Each student may have one medical record
     */
    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private MedicalRecord medicalRecord;

    /**
     * ✅ NEW: One-to-Many relationship with ParentGuardian
     * Added: December 14, 2025 - K-12 Enrollment Enhancement
     * One student can have multiple parents/guardians (divorced, foster, etc.)
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ParentGuardian> parents = new ArrayList<>();

    /**
     * ✅ NEW: One-to-Many relationship with EmergencyContact
     * Added: December 14, 2025 - K-12 Enrollment Enhancement
     * One student can have 2-5 emergency contacts (ordered by priority)
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<EmergencyContact> emergencyContacts = new ArrayList<>();

    // ========================================================================
    // TRANSIENT FIELDS (Import Support)
    // ========================================================================

    @Transient
    private String combinedName; // For import parsing

    // ========================================================================
    // ✅ UTILITY METHODS
    // ========================================================================

    /**
     * ✅ VERIFIED METHOD: Get count of enrolled courses
     * Used by StudentsController line 75
     * Handles LazyInitializationException gracefully
     * 
     * @return Number of enrolled courses, 0 if null or lazy load fails
     */
    public int getEnrolledCourseCount() {
        try {
            return enrolledCourses != null ? enrolledCourses.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get full name (FirstName LastName)
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get display name (LastName, FirstName)
     */
    public String getDisplayName() {
        return lastName + ", " + firstName;
    }

    /**
     * Check if student is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Get active status (for JavaFX property binding compatibility)
     * Delegates to isActive() for consistency with Lombok conventions
     */
    public boolean getActive() {
        return active;
    }

    /**
     * Parse combined name for import
     * Format: "FirstName LastName" or "LastName, FirstName"
     */
    public void parseCombinedName() {
        if (combinedName == null || combinedName.trim().isEmpty()) {
            return;
        }

        if (combinedName.contains(",")) {
            // Format: "LastName, FirstName"
            String[] parts = combinedName.split(",", 2);
            this.lastName = parts[0].trim();
            this.firstName = parts.length > 1 ? parts[1].trim() : "";
        } else if (combinedName.contains(" ")) {
            // Format: "FirstName LastName"
            String[] parts = combinedName.trim().split("\\s+", 2);
            this.firstName = parts[0].trim();
            this.lastName = parts.length > 1 ? parts[1].trim() : "";
        } else {
            // Single name - use as last name
            this.lastName = combinedName.trim();
            this.firstName = "";
        }
    }

    /**
     * Get assigned lunch period (first one if multiple)
     * 
     * @return LunchPeriod or null if not assigned
     */
    public LunchPeriod getAssignedLunchPeriod() {
        if (lunchPeriod != null && !lunchPeriod.isEmpty()) {
            return lunchPeriod.get(0);
        }
        return null;
    }

    /**
     * Check if student has a lunch period assigned
     */
    public boolean hasLunchPeriod() {
        return lunchPeriod != null && !lunchPeriod.isEmpty();
    }

    /**
     * Add a lunch period to this student
     */
    public void addLunchPeriod(LunchPeriod period) {
        if (this.lunchPeriod == null) {
            this.lunchPeriod = new ArrayList<>();
        }
        if (!this.lunchPeriod.contains(period)) {
            this.lunchPeriod.add(period);
        }
    }

    /**
     * Remove a lunch period from this student
     */
    public void removeLunchPeriod(LunchPeriod period) {
        if (this.lunchPeriod != null) {
            this.lunchPeriod.remove(period);
        }
    }

    /**
     * Check if student has medical conditions requiring attention
     */
    public boolean hasMedicalConditions() {
        return medicalRecord != null && medicalRecord.hasMedicalConditions();
    }

    /**
     * Get medical quick summary for tooltip
     */
    public String getMedicalQuickSummary() {
        return medicalRecord != null ? medicalRecord.getQuickSummary() : "No medical information on file";
    }

    /**
     * Check if this is a critical medical case
     */
    public boolean isCriticalMedicalCase() {
        return medicalRecord != null && medicalRecord.isCriticalCase();
    }

    // ========================================================================
    // COURSE ASSIGNMENT PRIORITY METHODS
    // ========================================================================

    /**
     * Calculate overall priority score for course placement
     * Higher score = higher priority for course assignment
     *
     * Score Breakdown:
     * - GPA component (40% weight): 0-500 points (GPA × 100, capped at 5.0)
     * - Seniority bonus (20% weight): 200 points for seniors
     * - Behavior component (20% weight): 0-100 points (behaviorScore × 20)
     * - Special needs (10% weight): 100 points for IEP/504
     * - Gifted bonus (10% weight): 100 points for gifted students
     * - Manual adjustment: priorityWeight × 10
     *
     * @return Priority score (typically 200-900 range)
     */
    public int calculatePriorityScore() {
        int score = 0;

        // GPA component (40% weight) - scale 0-500 points
        // 4.0 GPA = 400 points, 5.0 GPA (weighted) = 500 points
        if (currentGPA != null) {
            score += (int)(Math.min(currentGPA, 5.0) * 100);
        }

        // Seniority bonus (20% weight) - 200 points for seniors
        if (Boolean.TRUE.equals(isSenior)) {
            score += 200;
        }

        // Behavior component (20% weight) - 0-100 points
        // Behavior score 5/5 = 100 points, 3/5 = 60 points, 1/5 = 20 points
        if (behaviorScore != null) {
            score += (behaviorScore * 20);
        }

        // Special needs consideration (10% weight) - 100 points
        // IEP/504 students get priority to ensure appropriate placement
        if (Boolean.TRUE.equals(hasIEP) || Boolean.TRUE.equals(has504Plan)) {
            score += 100;
        }

        // Gifted bonus (10% weight) - 100 points
        if (Boolean.TRUE.equals(isGifted)) {
            score += 100;
        }

        // Manual priority weight override (administrative adjustment)
        if (priorityWeight != null && priorityWeight > 0) {
            score += (priorityWeight * 10);
        }

        return score;
    }

    /**
     * Get academic tier for GPA-based priority
     * Used for grouping students into priority tiers
     *
     * @return Tier description string
     */
    public String getAcademicTier() {
        if (currentGPA == null) return "Tier 6 (Unranked)";
        if (currentGPA >= 3.8) return "Tier 1 (GPA 3.8+)";
        if (currentGPA >= 3.5) return "Tier 2 (GPA 3.5-3.8)";
        if (currentGPA >= 3.0) return "Tier 3 (GPA 3.0-3.5)";
        if (currentGPA >= 2.0) return "Tier 4 (GPA 2.0-3.0)";
        return "Tier 5 (GPA < 2.0)";
    }

    /**
     * Check if student meets GPA requirement for a course
     *
     * @param course The course to check requirements for
     * @return true if student meets GPA requirement (or no requirement exists)
     */
    public boolean meetsGPARequirement(Course course) {
        if (course == null || course.getMinGPARequired() == null) return true;
        if (currentGPA == null) return false;
        return currentGPA >= course.getMinGPARequired();
    }

    /**
     * Get human-readable behavior description
     *
     * @return Behavior description string
     */
    public String getBehaviorDescription() {
        if (behaviorScore == null) return "Not Rated";
        return switch (behaviorScore) {
            case 5 -> "Excellent (5/5)";
            case 4 -> "Good (4/5)";
            case 3 -> "Average (3/5)";
            case 2 -> "Below Average (2/5)";
            case 1 -> "Poor (1/5)";
            default -> "Not Rated";
        };
    }

    /**
     * Check if student has submitted elective preferences
     *
     * @return true if at least one elective preference is set
     */
    public boolean hasElectivePreferences() {
        return electivePreference1 != null
            || electivePreference2 != null
            || electivePreference3 != null
            || electivePreference4 != null;
    }

    /**
     * Get count of elective preferences submitted
     *
     * @return Number of preferences (0-4)
     */
    public int getElectivePreferenceCount() {
        int count = 0;
        if (electivePreference1 != null) count++;
        if (electivePreference2 != null) count++;
        if (electivePreference3 != null) count++;
        if (electivePreference4 != null) count++;
        return count;
    }

    /**
     * Get priority score breakdown for debugging/display
     *
     * @return Formatted string showing score components
     */
    public String getPriorityScoreBreakdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("Priority Score Breakdown:\n");

        if (currentGPA != null) {
            int gpaPoints = (int)(Math.min(currentGPA, 5.0) * 100);
            sb.append(String.format("  GPA (%.2f): %d points\n", currentGPA, gpaPoints));
        } else {
            sb.append("  GPA: 0 points (no GPA)\n");
        }

        if (Boolean.TRUE.equals(isSenior)) {
            sb.append("  Seniority (Senior): 200 points\n");
        } else {
            sb.append("  Seniority: 0 points (not senior)\n");
        }

        if (behaviorScore != null) {
            int behaviorPoints = behaviorScore * 20;
            sb.append(String.format("  Behavior (%d/5): %d points\n", behaviorScore, behaviorPoints));
        } else {
            sb.append("  Behavior: 0 points (not rated)\n");
        }

        if (Boolean.TRUE.equals(hasIEP) || Boolean.TRUE.equals(has504Plan)) {
            sb.append("  IEP/504: 100 points\n");
        }

        if (Boolean.TRUE.equals(isGifted)) {
            sb.append("  Gifted: 100 points\n");
        }

        if (priorityWeight != null && priorityWeight > 0) {
            int manualPoints = priorityWeight * 10;
            sb.append(String.format("  Manual Adjustment: %d points\n", manualPoints));
        }

        sb.append("  --------------------------------\n");
        sb.append(String.format("  TOTAL: %d points", calculatePriorityScore()));

        return sb.toString();
    }

    // ========================================================================
    // GRADUATION REQUIREMENTS HELPER METHODS
    // ========================================================================

    /**
     * Check if student has enough credits earned for their grade level
     * Florida standard: Grade 9=6, 10=12, 11=18, 12=24 credits
     *
     * @return true if student has met credit requirements for current grade
     */
    public boolean hasMinimumCreditsForGrade() {
        if (creditsEarned == null) return false;
        double required = getRequiredCreditsForGrade();
        return creditsEarned >= required;
    }

    /**
     * Get required credits for current grade level
     *
     * @return Required credits based on grade level
     */
    public double getRequiredCreditsForGrade() {
        if (gradeLevel == null) return 0.0;

        return switch (gradeLevel.trim()) {
            case "9" -> 6.0;
            case "10" -> 12.0;
            case "11" -> 18.0;
            case "12" -> 24.0;
            default -> 0.0;
        };
    }

    /**
     * Calculate how many credits behind schedule
     *
     * @return Number of credits behind (0 if on track)
     */
    public double getCreditDeficit() {
        if (creditsEarned == null) return getRequiredCreditsForGrade();
        double deficit = getRequiredCreditsForGrade() - creditsEarned;
        return Math.max(0.0, deficit);
    }

    /**
     * Check if student meets minimum GPA for graduation (2.0)
     *
     * @return true if GPA >= 2.0
     */
    public boolean meetsGraduationGPA() {
        if (currentGPA == null) return false;
        return currentGPA >= 2.0;
    }

    /**
     * Check if student is on track for graduation
     * Requires: credits >= required AND GPA >= 2.0
     *
     * @return true if on track
     */
    public boolean isOnTrackForGraduation() {
        return hasMinimumCreditsForGrade() && meetsGraduationGPA();
    }

    /**
     * Get credit progress percentage
     *
     * @return Percentage of credits earned vs required (0-100+)
     */
    public double getCreditProgressPercentage() {
        double required = getRequiredCreditsForGrade();
        if (required == 0) return 100.0;
        if (creditsEarned == null) return 0.0;
        return (creditsEarned / required) * 100.0;
    }

    /**
     * Get graduation readiness status
     *
     * @return "On Track", "At Risk", or "Retention Risk"
     */
    public String getGraduationReadinessStatus() {
        double deficit = getCreditDeficit();
        double gpa = currentGPA != null ? currentGPA : 0.0;

        // Retention Risk: 3+ credits behind OR GPA < 1.5
        if (deficit >= 3.0 || gpa < 1.5) {
            return "Retention Risk";
        }

        // At Risk: 1-2 credits behind OR GPA 1.5-1.99
        if (deficit >= 1.0 || (gpa >= 1.5 && gpa < 2.0)) {
            return "At Risk";
        }

        // On Track
        return "On Track";
    }

    /**
     * Check if this is a senior at risk of not graduating
     *
     * @return true if senior and not meeting requirements
     */
    public boolean isSeniorAtRisk() {
        return "12".equals(gradeLevel) && !isOnTrackForGraduation();
    }

    /**
     * Get estimated credits needed per remaining term to graduate on time
     * Assumes 2 terms per year
     *
     * @return Credits per term needed
     */
    public double getCreditsNeededPerTerm() {
        if (gradeLevel == null || creditsEarned == null) return 0.0;

        int currentGrade = getGradeNumber();
        if (currentGrade < 9 || currentGrade > 12) return 0.0;

        // Calculate remaining terms (2 per year)
        int termsRemaining = (13 - currentGrade) * 2;
        if (termsRemaining <= 0) return 0.0;

        double creditsNeeded = Math.max(0, creditsRequired - creditsEarned);
        return creditsNeeded / termsRemaining;
    }

    /**
     * Get numeric grade level (9-12)
     *
     * @return Grade number or -1 if invalid
     */
    private int getGradeNumber() {
        if (gradeLevel == null) return -1;
        try {
            return Integer.parseInt(gradeLevel.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ========================================================================
    // JPA LIFECYCLE CALLBACKS
    // ========================================================================

    /**
     * Called before entity is persisted to database
     * Sets creation and update timestamps
     */
    @jakarta.persistence.PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    /**
     * Called before entity is updated in database
     * Updates the update timestamp
     */
    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}