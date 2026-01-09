package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Four-Year Academic Planning
 *
 * Integrates all phases to provide comprehensive academic planning:
 * - Phase 1: Subject Area relationships
 * - Phase 2: Course Sequence pathways
 * - Phase 3: AI-powered recommendations
 * - Phase 4: Four-year planning and tracking
 *
 * Location: src/main/java/com/eduscheduler/service/AcademicPlanningService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 4 - December 6, 2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AcademicPlanningService {

    private final AcademicPlanRepository planRepository;
    private final PlannedCourseRepository plannedCourseRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final CourseSequenceService sequenceService;
    private final CourseRecommendationService recommendationService;
    private final SubjectAreaService subjectAreaService;
    private final GraduationRequirementsService graduationRequirementsService;

    // ========================================================================
    // BASIC CRUD OPERATIONS - ACADEMIC PLANS
    // ========================================================================

    /**
     * Get all plans
     *
     * @return List of all plans
     */
    public List<AcademicPlan> getAllPlans() {
        return planRepository.findAll();
    }

    /**
     * Get plan by ID
     *
     * @param id Plan ID
     * @return Optional containing plan if found
     */
    public Optional<AcademicPlan> getPlanById(Long id) {
        return planRepository.findById(id);
    }

    /**
     * Get plans for student
     *
     * @param studentId Student ID
     * @return List of plans
     */
    public List<AcademicPlan> getPlansForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        return planRepository.findByStudentAndActiveTrue(student);
    }

    /**
     * Get primary plan for student
     *
     * @param studentId Student ID
     * @return Optional containing primary plan if found
     */
    public Optional<AcademicPlan> getPrimaryPlanForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        return planRepository.findByStudentAndIsPrimaryTrueAndActiveTrue(student);
    }

    /**
     * Create new academic plan
     *
     * @param plan Plan to create
     * @return Created plan
     */
    @Transactional
    public AcademicPlan createPlan(AcademicPlan plan) {
        log.info("Creating academic plan: {} for student {}",
            plan.getPlanName(), plan.getStudent().getId());

        // If this is marked as primary, unmark other primary plans
        if (Boolean.TRUE.equals(plan.getIsPrimary())) {
            unprimaryOtherPlans(plan.getStudent());
        }

        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());

        AcademicPlan saved = planRepository.save(plan);
        log.info("Created plan: {}", saved);
        return saved;
    }

    /**
     * Update academic plan
     *
     * @param id Plan ID
     * @param updates Updated data
     * @return Updated plan
     */
    @Transactional
    public AcademicPlan updatePlan(Long id, AcademicPlan updates) {
        AcademicPlan existing = planRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        log.info("Updating plan: {}", id);

        // Update fields
        if (updates.getPlanName() != null) {
            existing.setPlanName(updates.getPlanName());
        }
        if (updates.getPlanType() != null) {
            existing.setPlanType(updates.getPlanType());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        if (updates.getIsPrimary() != null && Boolean.TRUE.equals(updates.getIsPrimary())) {
            unprimaryOtherPlans(existing.getStudent());
            existing.setIsPrimary(true);
        }
        if (updates.getNotes() != null) {
            existing.setNotes(updates.getNotes());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return planRepository.save(existing);
    }

    /**
     * Delete academic plan
     *
     * @param id Plan ID
     */
    @Transactional
    public void deletePlan(Long id) {
        AcademicPlan plan = planRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));

        log.info("Deleting (deactivating) plan: {}", id);
        plan.setActive(false);
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
    }

    // ========================================================================
    // PLAN GENERATION
    // ========================================================================

    /**
     * Generate four-year plan for student based on course sequence
     *
     * @param studentId Student ID
     * @param sequenceId Course sequence ID
     * @param startYear Start school year
     * @param planName Plan name
     * @return Generated plan
     */
    @Transactional
    public AcademicPlan generatePlanFromSequence(Long studentId, Long sequenceId,
            String startYear, String planName) {

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        CourseSequence sequence = sequenceService.getSequenceById(sequenceId)
            .orElseThrow(() -> new IllegalArgumentException("Sequence not found: " + sequenceId));

        log.info("Generating plan from sequence {} for student {}", sequenceId, studentId);

        // Create plan
        AcademicPlan plan = AcademicPlan.builder()
            .student(student)
            .planName(planName != null ? planName : sequence.getName())
            .planType(determPlanType(sequence.getSequenceType()))
            .startYear(startYear)
            .status(AcademicPlan.PlanStatus.DRAFT)
            .isPrimary(false)
            .active(true)
            .build();

        // Get sequence steps
        List<CourseSequenceStep> steps = sequenceService.getStepsForSequence(sequenceId);

        // Add courses from sequence to plan
        for (CourseSequenceStep step : steps) {
            PlannedCourse plannedCourse = PlannedCourse.builder()
                .course(step.getCourse())
                .schoolYear(calculateSchoolYear(startYear, step.getRecommendedGradeLevel(), student))
                .gradeLevel(step.getRecommendedGradeLevel())
                .semester(0) // Full year by default
                .credits(step.getCredits())
                .isRequired(step.getIsRequired())
                .status(PlannedCourse.CourseStatus.PLANNED)
                .sequenceStep(step)
                .alternatives(step.getAlternativeCourses())
                .build();

            plan.addPlannedCourse(plannedCourse);
        }

        // Calculate totals
        recalculatePlanTotals(plan);

        return planRepository.save(plan);
    }

    /**
     * Generate plan from recommendations
     *
     * @param studentId Student ID
     * @param startYear Start year
     * @param planName Plan name
     * @return Generated plan
     */
    @Transactional
    public AcademicPlan generatePlanFromRecommendations(Long studentId,
            String startYear, String planName) {

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        log.info("Generating plan from recommendations for student {}", studentId);

        // Create plan
        AcademicPlan plan = AcademicPlan.builder()
            .student(student)
            .planName(planName != null ? planName : "AI-Generated Plan")
            .planType(AcademicPlan.PlanType.STANDARD)
            .startYear(startYear)
            .status(AcademicPlan.PlanStatus.DRAFT)
            .isPrimary(false)
            .active(true)
            .build();

        // Get accepted recommendations
        List<CourseRecommendation> recommendations =
            recommendationService.getRecommendationsForStudent(studentId)
                .stream()
                .filter(r -> r.isAccepted() || r.isPending())
                .collect(Collectors.toList());

        // Add recommended courses to plan
        for (CourseRecommendation rec : recommendations) {
            PlannedCourse plannedCourse = PlannedCourse.builder()
                .course(rec.getCourse())
                .schoolYear(rec.getRecommendedSchoolYear())
                .gradeLevel(rec.getRecommendedGradeLevel())
                .semester(0)
                .credits(1.0) // Default
                .isRequired(rec.getRecommendationType() ==
                    CourseRecommendation.RecommendationType.GRADUATION_REQUIREMENT)
                .status(PlannedCourse.CourseStatus.PLANNED)
                .recommendation(rec)
                .alternatives(rec.getAlternativeCourses())
                .build();

            plan.addPlannedCourse(plannedCourse);
        }

        // Calculate totals
        recalculatePlanTotals(plan);

        return planRepository.save(plan);
    }

    // ========================================================================
    // PLANNED COURSE OPERATIONS
    // ========================================================================

    /**
     * Add course to plan
     *
     * @param planId Plan ID
     * @param plannedCourse Planned course
     * @return Updated plan
     */
    @Transactional
    public AcademicPlan addCourseToPlan(Long planId, PlannedCourse plannedCourse) {
        AcademicPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        log.info("Adding course {} to plan {}", plannedCourse.getCourse().getCourseCode(), planId);

        plan.addPlannedCourse(plannedCourse);
        recalculatePlanTotals(plan);

        return planRepository.save(plan);
    }

    /**
     * Remove course from plan
     *
     * @param planId Plan ID
     * @param plannedCourseId Planned course ID
     * @return Updated plan
     */
    @Transactional
    public AcademicPlan removeCourseFromPlan(Long planId, Long plannedCourseId) {
        AcademicPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        PlannedCourse plannedCourse = plannedCourseRepository.findById(plannedCourseId)
            .orElseThrow(() -> new IllegalArgumentException("Planned course not found: " + plannedCourseId));

        log.info("Removing course {} from plan {}", plannedCourse.getCourse().getCourseCode(), planId);

        plan.removePlannedCourse(plannedCourse);
        plannedCourseRepository.delete(plannedCourse);
        recalculatePlanTotals(plan);

        return planRepository.save(plan);
    }

    /**
     * Mark course as completed
     *
     * @param plannedCourseId Planned course ID
     * @param grade Grade earned
     * @return Updated planned course
     */
    @Transactional
    public PlannedCourse markCourseCompleted(Long plannedCourseId, String grade) {
        PlannedCourse plannedCourse = plannedCourseRepository.findById(plannedCourseId)
            .orElseThrow(() -> new IllegalArgumentException("Planned course not found: " + plannedCourseId));

        log.info("Marking course {} as completed with grade {}",
            plannedCourse.getCourse().getCourseCode(), grade);

        plannedCourse.markCompleted(grade);
        PlannedCourse saved = plannedCourseRepository.save(plannedCourse);

        // Recalculate plan totals
        recalculatePlanTotals(plannedCourse.getAcademicPlan());

        return saved;
    }

    // ========================================================================
    // APPROVAL OPERATIONS
    // ========================================================================

    /**
     * Approve plan by counselor
     *
     * @param planId Plan ID
     * @param counselorId Counselor user ID
     * @return Updated plan
     */
    @Transactional
    public AcademicPlan approveByCounselor(Long planId, Long counselorId) {
        AcademicPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        User counselor = userRepository.findById(counselorId)
            .orElseThrow(() -> new IllegalArgumentException("Counselor not found: " + counselorId));

        log.info("Counselor {} ({}) approving plan {}", counselor.getUsername(), counselorId, planId);
        plan.approveByCounselor(counselor);
        plan.setUpdatedAt(LocalDateTime.now());

        return planRepository.save(plan);
    }

    /**
     * Accept plan by student
     *
     * @param planId Plan ID
     * @return Updated plan
     */
    @Transactional
    public AcademicPlan acceptByStudent(Long planId) {
        AcademicPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        log.info("Student accepting plan {}", planId);
        plan.acceptByStudent();
        plan.setUpdatedAt(LocalDateTime.now());

        return planRepository.save(plan);
    }

    /**
     * Accept plan by parent
     *
     * @param planId Plan ID
     * @return Updated plan
     */
    @Transactional
    public AcademicPlan acceptByParent(Long planId) {
        AcademicPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        log.info("Parent accepting plan {}", planId);
        plan.acceptByParent();
        plan.setUpdatedAt(LocalDateTime.now());

        return planRepository.save(plan);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Recalculate plan totals (credits, completion, etc.)
     */
    @Transactional
    public void recalculatePlanTotals(AcademicPlan plan) {
        Double totalPlanned = plannedCourseRepository.getTotalCreditsForPlan(plan);
        Double totalCompleted = plannedCourseRepository.getTotalCompletedCreditsForPlan(plan);

        plan.setTotalCreditsPlanned(totalPlanned != null ? totalPlanned : 0.0);
        plan.setTotalCreditsCompleted(totalCompleted != null ? totalCompleted : 0.0);

        // Check graduation requirements using GraduationRequirementsService
        boolean meetsRequirements = checkGraduationRequirements(plan);
        plan.setMeetsGraduationRequirements(meetsRequirements);

        planRepository.save(plan);
    }

    /**
     * Unmark other plans as primary
     */
    private void unprimaryOtherPlans(Student student) {
        List<AcademicPlan> otherPlans = planRepository.findByStudentAndActiveTrue(student);
        for (AcademicPlan plan : otherPlans) {
            if (Boolean.TRUE.equals(plan.getIsPrimary())) {
                plan.setIsPrimary(false);
                planRepository.save(plan);
            }
        }
    }

    /**
     * Determine plan type from sequence type
     */
    private AcademicPlan.PlanType determPlanType(CourseSequence.SequenceType sequenceType) {
        return switch (sequenceType) {
            case HONORS, AP, IB -> AcademicPlan.PlanType.ACCELERATED;
            case DUAL_ENROLLMENT -> AcademicPlan.PlanType.DUAL_ENROLLMENT;
            default -> AcademicPlan.PlanType.STANDARD;
        };
    }

    /**
     * Calculate school year from start year and grade level
     *
     * @param startYear Starting school year (e.g., "2024-2025")
     * @param targetGrade Target grade level (9, 10, 11, 12)
     * @param student Student entity
     * @return Calculated school year for the target grade
     */
    private String calculateSchoolYear(String startYear, Integer targetGrade, Student student) {
        // Parse the starting year from the school year string (e.g., "2024-2025" -> 2024)
        int baseYear = Integer.parseInt(startYear.split("-")[0]);

        // Calculate how many years from the starting grade to the target grade
        String gradeLevelStr = student.getGradeLevel();
        int currentGrade = 9; // Default to 9th grade if not specified
        if (gradeLevelStr != null) {
            try {
                currentGrade = Integer.parseInt(gradeLevelStr.trim());
            } catch (NumberFormatException e) {
                // Keep default value of 9
            }
        }

        // Calculate year offset
        int yearOffset = targetGrade - currentGrade;

        // Calculate the target year
        int targetStartYear = baseYear + yearOffset;
        int targetEndYear = targetStartYear + 1;

        return targetStartYear + "-" + targetEndYear;
    }

    /**
     * Get current school year based on today's date
     * School year runs from August to July
     *
     * @return Current school year (e.g., "2025-2026")
     */
    private String getCurrentSchoolYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();

        // School year starts in August (month 8)
        // If we're before August, we're in the previous school year
        if (now.getMonthValue() < 8) {
            return (year - 1) + "-" + year;
        } else {
            return year + "-" + (year + 1);
        }
    }

    /**
     * Get statistics about plans
     *
     * @return Statistics DTO
     */
    public PlanningStatistics getStatistics() {
        long totalPlans = planRepository.count();
        long activePlans = planRepository.countByActiveTrue();
        long draftPlans = planRepository.countByStatusAndActiveTrue(AcademicPlan.PlanStatus.DRAFT);
        long approvedPlans = planRepository.countByStatusAndActiveTrue(AcademicPlan.PlanStatus.APPROVED);
        long completedPlans = planRepository.countByStatusAndActiveTrue(AcademicPlan.PlanStatus.COMPLETED);

        return PlanningStatistics.builder()
            .totalPlans(totalPlans)
            .activePlans(activePlans)
            .draftPlans(draftPlans)
            .approvedPlans(approvedPlans)
            .completedPlans(completedPlans)
            .build();
    }

    /**
     * Check if academic plan meets graduation requirements
     *
     * Validates planned courses against graduation requirements including:
     * - Total credit requirements
     * - Subject area distribution (English, Math, Science, Social Studies)
     * - Elective requirements
     * - Special requirements (PE, Arts, Foreign Language)
     *
     * @param plan The academic plan to validate
     * @return true if all graduation requirements are met
     */
    private boolean checkGraduationRequirements(AcademicPlan plan) {
        if (plan == null || plan.getStudent() == null) {
            return false;
        }

        try {
            // Get planned courses for this plan
            List<PlannedCourse> plannedCourses = plannedCourseRepository
                .findByAcademicPlanOrderBySchoolYearAscSemesterAsc(plan);

            // Check minimum credit requirement (typically 24 credits for high school)
            Double totalCredits = plan.getTotalCreditsPlanned();
            if (totalCredits == null || totalCredits < 24.0) {
                log.debug("Plan {} does not meet minimum credit requirement: {} < 24",
                         plan.getId(), totalCredits);
                return false;
            }

            // Use graduation requirements service to assess if student is on track
            // This checks overall graduation progress including subject area distribution
            Student student = plan.getStudent();
            boolean onTrack = graduationRequirementsService.isOnTrackForGraduation(student);

            if (!onTrack) {
                log.debug("Plan {} - Student {} not on track for graduation",
                         plan.getId(), student.getStudentId());
                return false;
            }

            // Additional check: Ensure plan has core subject coverage
            // Count how many different subject areas are represented
            long subjectAreas = plannedCourses.stream()
                .map(pc -> pc.getCourse().getSubject())
                .filter(Objects::nonNull)
                .distinct()
                .count();

            // Expect at least 4 core subjects (English, Math, Science, Social Studies)
            if (subjectAreas < 4) {
                log.debug("Plan {} has insufficient subject diversity: {} subjects",
                         plan.getId(), subjectAreas);
                return false;
            }

            // All checks passed
            return true;

        } catch (Exception e) {
            log.error("Error checking graduation requirements for plan {}: {}",
                     plan.getId(), e.getMessage());
            // If graduation requirements service has an error, fall back to simple credit check
            Double totalCredits = plan.getTotalCreditsPlanned();
            return totalCredits != null && totalCredits >= 24.0;
        }
    }

    /**
     * Statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class PlanningStatistics {
        private long totalPlans;
        private long activePlans;
        private long draftPlans;
        private long approvedPlans;
        private long completedPlans;

        @Override
        public String toString() {
            return String.format(
                "Planning Statistics: Total=%d, Active=%d, Draft=%d, Approved=%d, Completed=%d",
                totalPlans, activePlans, draftPlans, approvedPlans, completedPlans);
        }
    }
}
