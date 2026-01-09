package com.heronix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Student Data Transfer Object
 * Lightweight DTO to avoid lazy loading issues in API responses
 *
 * Enhanced with K-12 enrollment fields (Phase 2 - December 14, 2025)
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since December 13, 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class StudentDTO {
    // ========================================================================
    // BASIC INFORMATION (Original fields)
    // ========================================================================
    private Long id;
    private String studentId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String gradeLevel;
    private boolean active;
    private String photoPath;
    private Double currentGPA;
    private Double unweightedGPA;
    private Double weightedGPA;
    private Double creditsEarned;
    private Double creditsRequired;
    private String academicStanding;
    private String honorRollStatus;
    private Integer graduationYear;
    private Boolean graduated;

    // Medical/Special needs flags
    private Boolean hasIEP;
    private Boolean has504Plan;
    private Boolean isGifted;

    // Attendance/behavior
    private Integer behaviorScore;

    // Emergency contact information (DEPRECATED - use emergencyContacts list)
    @Deprecated
    private String emergencyContact;
    @Deprecated
    private String emergencyPhone;

    // Medical information (summary only, not full medical record)
    private String medicalConditions;

    // ========================================================================
    // DEMOGRAPHICS - K-12 ENROLLMENT ENHANCEMENT (Phase 2)
    // Added: December 14, 2025
    // ========================================================================

    /**
     * Date of Birth (REQUIRED for enrollment)
     */
    private LocalDate dateOfBirth;

    /**
     * Gender (Male, Female, Non-binary, Prefer not to say)
     */
    private String gender;

    /**
     * Ethnicity (Federal reporting)
     */
    private String ethnicity;

    /**
     * Race (Federal NCES reporting)
     */
    private String race;

    /**
     * Preferred/Primary language
     */
    private String preferredLanguage;

    /**
     * English Language Learner status
     */
    private Boolean isEnglishLearner;

    /**
     * Birth city
     */
    private String birthCity;

    /**
     * Birth state
     */
    private String birthState;

    /**
     * Birth country
     */
    private String birthCountry;

    // ========================================================================
    // HOME ADDRESS - PROOF OF RESIDENCY
    // Added: December 14, 2025
    // ========================================================================

    /**
     * Home street address (REQUIRED)
     */
    private String homeStreetAddress;

    /**
     * Home city (REQUIRED)
     */
    private String homeCity;

    /**
     * Home state (REQUIRED)
     */
    private String homeState;

    /**
     * Home ZIP code (REQUIRED)
     */
    private String homeZipCode;

    /**
     * County (for district boundaries)
     */
    private String county;

    /**
     * Mailing address (if different from home)
     */
    private String mailingAddress;

    /**
     * Mailing city
     */
    private String mailingCity;

    /**
     * Mailing state
     */
    private String mailingState;

    /**
     * Mailing ZIP code
     */
    private String mailingZipCode;

    // ========================================================================
    // SPECIAL CIRCUMSTANCES - FEDERAL COMPLIANCE
    // Added: December 14, 2025
    // ========================================================================

    /**
     * Foster care status (FAFSA requirement)
     */
    private Boolean isFosterCare;

    /**
     * Foster care agency
     */
    private String fosterCareAgency;

    /**
     * Foster care case worker
     */
    private String fosterCaseworker;

    /**
     * Homeless status (McKinney-Vento Act)
     */
    private Boolean isHomeless;

    /**
     * Homeless shelter name
     */
    private String homelessShelter;

    /**
     * Orphan status
     */
    private Boolean isOrphan;

    /**
     * Ward of court
     */
    private Boolean isWardOfCourt;

    /**
     * Refugee/Asylee status
     */
    private Boolean isRefugeeAsylee;

    /**
     * Unaccompanied youth
     */
    private Boolean isUnaccompaniedYouth;

    /**
     * Military family status
     */
    private Boolean isMilitaryFamily;

    /**
     * Migrant worker family
     */
    private Boolean isMigrantFamily;

    /**
     * Custody arrangement
     */
    private String custodyArrangement;

    /**
     * Lives with (description)
     */
    private String livesWith;

    // ========================================================================
    // PREVIOUS SCHOOL INFORMATION
    // Added: December 14, 2025
    // ========================================================================

    /**
     * Previous school name
     */
    private String previousSchoolName;

    /**
     * Previous school city
     */
    private String previousSchoolCity;

    /**
     * Previous school state
     */
    private String previousSchoolState;

    /**
     * Previous school district
     */
    private String previousSchoolDistrict;

    /**
     * Reason for transfer
     */
    private String transferReason;

    // ========================================================================
    // IMMUNIZATION & HEALTH INSURANCE
    // Added: December 14, 2025
    // ========================================================================

    /**
     * Immunizations complete?
     */
    private Boolean immunizationComplete;

    /**
     * Immunization exemption type
     */
    private String immunizationExemptionType;

    /**
     * Physical exam date
     */
    private LocalDate physicalExamDate;

    /**
     * Physician name
     */
    private String physicianName;

    /**
     * Physician phone
     */
    private String physicianPhone;

    /**
     * Dentist name
     */
    private String dentistName;

    /**
     * Dentist phone
     */
    private String dentistPhone;

    /**
     * Health insurance provider
     */
    private String healthInsuranceProvider;

    /**
     * Health insurance policy number
     */
    private String insurancePolicyNumber;

    /**
     * Health insurance group number
     */
    private String insuranceGroupNumber;

    /**
     * Insurance subscriber name
     */
    private String insuranceSubscriberName;

    // ========================================================================
    // ENROLLMENT DOCUMENTATION
    // Added: December 14, 2025
    // ========================================================================

    /**
     * Birth certificate verified?
     */
    private Boolean birthCertificateVerified;

    /**
     * Birth certificate verified date
     */
    private LocalDate birthCertificateDate;

    /**
     * Proof of residency verified?
     */
    private Boolean residencyVerified;

    /**
     * Residency verification date
     */
    private LocalDate residencyVerificationDate;

    /**
     * Proof of residency type
     */
    private String residencyProofType;

    /**
     * Previous school records received?
     */
    private Boolean previousSchoolRecordsReceived;

    /**
     * Records received date
     */
    private LocalDate recordsReceivedDate;

    /**
     * Enrollment packet complete?
     */
    private Boolean enrollmentPacketComplete;

    /**
     * Enrollment date
     */
    private LocalDate enrollmentDate;

    /**
     * Enrolled by (staff name)
     */
    private String enrolledBy;

    /**
     * Withdrawal date (if student withdrew)
     */
    private LocalDate withdrawalDate;

    /**
     * Withdrawal reason
     */
    private String withdrawalReason;

    // ========================================================================
    // RELATIONSHIPS (NEW - Phase 2)
    // Added: December 14, 2025
    // ========================================================================

    /**
     * Parent/Guardian information (multiple parents/guardians)
     * Replaces single parent contact info
     */
    private List<ParentGuardianDTO> parents;

    /**
     * Emergency contacts (2-5 contacts ordered by priority)
     * Replaces single emergency contact fields
     */
    private List<EmergencyContactDTO> emergencyContacts;

    // ========================================================================
    // AUDIT FIELDS
    // Added: December 14, 2025
    // ========================================================================

    /**
     * Record created date
     */
    private LocalDateTime createdAt;

    /**
     * Record last updated date
     */
    private LocalDateTime updatedAt;

    /**
     * Created by (staff username)
     */
    private String createdBy;

    /**
     * Last modified by (staff username)
     */
    private String modifiedBy;
}
