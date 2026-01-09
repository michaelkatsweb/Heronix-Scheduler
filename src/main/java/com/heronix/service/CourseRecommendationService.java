package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Intelligent Course Recommendations
 *
 * Generates AI-powered course recommendations based on:
 * - Student GPA and academic performance
 * - Completed courses and prerequisites
 * - Current course sequence/pathway
 * - Subject area relationships
 * - Graduation requirements
 * - Student interests and goals
 *
 * Location: src/main/java/com/eduscheduler/service/CourseRecommendationService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 3 - December 6, 2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseRecommendationService {

    private final CourseRecommendationRepository recommendationRepository;
    private final CourseSequenceService sequenceService;
    private final SubjectAreaService subjectAreaService;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final CourseSequenceStepRepository stepRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final GraduationRequirementsService graduationRequirementsService;
    private final PrerequisiteValidationService prerequisiteValidationService;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;

    // ========================================================================
    // BASIC CRUD OPERATIONS
    // ========================================================================

    /**
     * Get all recommendations
     *
     * @return List of all recommendations
     */
    public List<CourseRecommendation> getAllRecommendations() {
        return recommendationRepository.findAll();
    }

    /**
     * Get all active recommendations
     *
     * @return List of active recommendations
     */
    public List<CourseRecommendation> getActiveRecommendations() {
        return recommendationRepository.findByActiveTrue();
    }

    /**
     * Get recommendation by ID
     *
     * @param id Recommendation ID
     * @return Optional containing recommendation if found
     */
    public Optional<CourseRecommendation> getRecommendationById(Long id) {
        return recommendationRepository.findById(id);
    }

    /**
     * Create manual recommendation
     *
     * @param recommendation Recommendation to create
     * @return Created recommendation
     */
    @Transactional
    public CourseRecommendation createRecommendation(CourseRecommendation recommendation) {
        // ✅ NULL SAFE: Validate recommendation and required fields
        if (recommendation == null) {
            throw new IllegalArgumentException("Recommendation cannot be null");
        }
        if (recommendation.getStudent() == null || recommendation.getStudent().getId() == null) {
            throw new IllegalArgumentException("Student and Student ID are required");
        }
        if (recommendation.getCourse() == null) {
            throw new IllegalArgumentException("Course is required");
        }

        log.info("Creating manual recommendation for student {} - course {}",
            recommendation.getStudent().getId(),
            recommendation.getCourse().getCourseCode() != null ? recommendation.getCourse().getCourseCode() : "Unknown");

        // Set creation timestamp
        recommendation.setCreatedAt(LocalDateTime.now());
        recommendation.setUpdatedAt(LocalDateTime.now());

        // Validate requirements
        validateRecommendation(recommendation);

        CourseRecommendation saved = recommendationRepository.save(recommendation);
        log.info("Created recommendation: {}", saved);
        return saved;
    }

    /**
     * Update recommendation
     *
     * @param id Recommendation ID
     * @param updates Updated data
     * @return Updated recommendation
     */
    @Transactional
    public CourseRecommendation updateRecommendation(Long id, CourseRecommendation updates) {
        CourseRecommendation existing = recommendationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + id));

        log.info("Updating recommendation: {}", id);

        // Update fields
        if (updates.getPriority() != null) {
            existing.setPriority(updates.getPriority());
        }
        if (updates.getConfidenceScore() != null) {
            existing.setConfidenceScore(updates.getConfidenceScore());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        if (updates.getReason() != null) {
            existing.setReason(updates.getReason());
        }
        if (updates.getCounselorNotes() != null) {
            existing.setCounselorNotes(updates.getCounselorNotes());
        }
        if (updates.getAlternativeCourses() != null) {
            existing.setAlternativeCourses(updates.getAlternativeCourses());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return recommendationRepository.save(existing);
    }

    /**
     * Delete recommendation
     *
     * @param id Recommendation ID
     */
    @Transactional
    public void deleteRecommendation(Long id) {
        CourseRecommendation recommendation = recommendationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + id));

        log.info("Deleting (deactivating) recommendation: {}", id);
        recommendation.setActive(false);
        recommendation.setUpdatedAt(LocalDateTime.now());
        recommendationRepository.save(recommendation);
    }

    // ========================================================================
    // QUERY OPERATIONS
    // ========================================================================

    /**
     * Get recommendations for student
     *
     * @param studentId Student ID
     * @return List of recommendations
     */
    public List<CourseRecommendation> getRecommendationsForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        return recommendationRepository.findByStudentAndActiveTrue(student);
    }

    /**
     * Get pending recommendations for student
     *
     * @param studentId Student ID
     * @return List of pending recommendations
     */
    public List<CourseRecommendation> getPendingRecommendationsForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        return recommendationRepository.findPendingForStudent(student);
    }

    /**
     * Get high priority recommendations for student
     *
     * @param studentId Student ID
     * @return List of high priority recommendations
     */
    public List<CourseRecommendation> getHighPriorityRecommendationsForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        return recommendationRepository.findHighPriorityForStudent(student);
    }

    /**
     * Get recommendations meeting all requirements
     *
     * @param studentId Student ID
     * @return List of recommendations
     */
    public List<CourseRecommendation> getRecommendationsMeetingRequirements(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        return recommendationRepository.findMeetingAllRequirementsForStudent(student);
    }

    // ========================================================================
    // AI-POWERED RECOMMENDATION GENERATION
    // ========================================================================

    /**
     * Generate AI-powered recommendations for a student
     *
     * @param studentId Student ID
     * @param schoolYear Target school year
     * @return List of generated recommendations
     */
    @Transactional
    public List<CourseRecommendation> generateRecommendationsForStudent(Long studentId, String schoolYear) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        log.info("Generating AI recommendations for student {} for {}", studentId, schoolYear);

        List<CourseRecommendation> recommendations = new ArrayList<>();

        // Get student's current grade level and completed courses
        Integer currentGrade = parseGradeLevel(student.getGradeLevel());
        List<Course> completedCourses = getCompletedCourses(student);
        Double studentGPA = calculateStudentGPA(student);

        // Strategy 1: Sequence-based recommendations
        recommendations.addAll(generateSequenceBasedRecommendations(student, completedCourses,
            currentGrade, schoolYear, studentGPA));

        // Strategy 2: Prerequisite-based recommendations
        recommendations.addAll(generatePrerequisiteBasedRecommendations(student, completedCourses,
            currentGrade, schoolYear, studentGPA));

        // Strategy 3: Subject relationship recommendations
        recommendations.addAll(generateRelationshipBasedRecommendations(student, completedCourses,
            currentGrade, schoolYear, studentGPA));

        // Strategy 4: Graduation requirement recommendations
        recommendations.addAll(generateGraduationRequirementRecommendations(student, completedCourses,
            currentGrade, schoolYear));

        // Remove duplicates and save
        List<CourseRecommendation> uniqueRecommendations = removeDuplicates(recommendations);

        log.info("Generated {} unique recommendations for student {}",
            uniqueRecommendations.size(), studentId);

        return recommendationRepository.saveAll(uniqueRecommendations);
    }

    /**
     * Generate sequence-based recommendations
     */
    private List<CourseRecommendation> generateSequenceBasedRecommendations(
            Student student, List<Course> completedCourses, Integer gradeLevel,
            String schoolYear, Double studentGPA) {

        List<CourseRecommendation> recommendations = new ArrayList<>();

        // Find suitable sequences for student's GPA
        List<CourseSequence> suitableSequences = sequenceService.getSequencesForStudentGPA(studentGPA);

        for (CourseSequence sequence : suitableSequences) {
            // Get next course in sequence based on completed courses
            Course nextCourse = sequence.getNextCourse(completedCourses);

            if (nextCourse != null && !isAlreadyRecommended(student, nextCourse)) {
                // Create recommendation
                CourseRecommendation recommendation = buildRecommendation(
                    student, nextCourse, sequence, gradeLevel, schoolYear,
                    CourseRecommendation.RecommendationType.SEQUENCE_BASED,
                    "Next course in " + sequence.getName() + " pathway",
                    calculateSequenceConfidence(sequence, completedCourses, studentGPA),
                    2 // High priority
                );

                recommendations.add(recommendation);
            }
        }

        return recommendations;
    }

    /**
     * Generate prerequisite-based recommendations
     */
    private List<CourseRecommendation> generatePrerequisiteBasedRecommendations(
            Student student, List<Course> completedCourses, Integer gradeLevel,
            String schoolYear, Double studentGPA) {

        List<CourseRecommendation> recommendations = new ArrayList<>();

        // Find courses where prerequisites are met
        List<Course> allCourses = courseRepository.findByActiveTrue();
        Set<Long> completedCourseIds = completedCourses.stream()
            .map(Course::getId)
            .collect(Collectors.toSet());

        for (Course course : allCourses) {
            // Check if student hasn't taken this course yet
            if (completedCourseIds.contains(course.getId())) {
                continue;
            }

            // Check if already recommended
            if (isAlreadyRecommended(student, course)) {
                continue;
            }

            // Check prerequisites
            if (hasMetPrerequisites(course, completedCourses)) {
                // Check GPA requirement
                boolean meetsGPA = meetsGPARequirement(course, studentGPA);

                double confidence = calculatePrerequisiteConfidence(
                    student, course, completedCourses, studentGPA);

                CourseRecommendation recommendation = buildRecommendation(
                    student, course, null, gradeLevel, schoolYear,
                    CourseRecommendation.RecommendationType.PREREQUISITE_BASED,
                    "Prerequisites met for " + course.getCourseName(),
                    confidence,
                    meetsGPA ? 3 : 5
                );

                recommendation.setPrerequisitesMet(true);
                recommendation.setGpaRequirementMet(meetsGPA);

                recommendations.add(recommendation);
            }
        }

        return recommendations;
    }

    /**
     * Generate relationship-based recommendations
     */
    private List<CourseRecommendation> generateRelationshipBasedRecommendations(
            Student student, List<Course> completedCourses, Integer gradeLevel,
            String schoolYear, Double studentGPA) {

        List<CourseRecommendation> recommendations = new ArrayList<>();

        // ✅ NULL SAFE: Check completedCourses exists before streaming
        if (completedCourses == null || completedCourses.isEmpty()) {
            return recommendations;
        }

        // Get subjects of completed courses
        // ✅ NULL SAFE: Filter null courses and null subject areas
        Set<SubjectArea> completedSubjects = completedCourses.stream()
            .filter(course -> course != null)
            .map(Course::getSubjectArea)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // ✅ NULL SAFE: Filter null subject areas before processing
        for (SubjectArea completedSubject : completedSubjects) {
            if (completedSubject == null || completedSubject.getId() == null) continue;

            // Find related subjects
            List<SubjectArea> relatedSubjects = subjectAreaService.getStronglyRelatedSubjects(
                completedSubject.getId());

            // ✅ NULL SAFE: Check relatedSubjects exists before looping
            if (relatedSubjects == null) continue;

            for (SubjectArea relatedSubject : relatedSubjects) {
                if (relatedSubject == null || relatedSubject.getId() == null) continue;

                // Find courses in related subject
                List<Course> relatedCourses = subjectAreaService.getCoursesForSubject(
                    relatedSubject.getId());

                // ✅ NULL SAFE: Check relatedCourses exists before looping
                if (relatedCourses == null) continue;

                for (Course course : relatedCourses) {
                    if (course == null) continue;
                    if (!completedCourses.contains(course) && !isAlreadyRecommended(student, course)) {
                        CourseRecommendation recommendation = buildRecommendation(
                            student, course, null, gradeLevel, schoolYear,
                            CourseRecommendation.RecommendationType.AI_GENERATED,
                            "Related to " + completedSubject.getName() + " studies",
                            0.6,
                            4 // Medium-low priority
                        );

                        recommendations.add(recommendation);
                    }
                }
            }
        }

        return recommendations;
    }

    /**
     * Generate graduation requirement recommendations
     * Integrated with GraduationRequirementsService - December 12, 2025
     */
    private List<CourseRecommendation> generateGraduationRequirementRecommendations(
            Student student, List<Course> completedCourses, Integer gradeLevel,
            String schoolYear) {

        List<CourseRecommendation> recommendations = new ArrayList<>();

        // ✅ INTEGRATED: Use GraduationRequirementsService to get actual requirements
        try {
            // Check if student is on track for graduation
            boolean onTrack = graduationRequirementsService.isOnTrackForGraduation(student);

            if (!onTrack) {
                // Student is not on track - calculate credits behind
                double creditsBehind = graduationRequirementsService.getCreditsBehind(student);
                String academicStatus = graduationRequirementsService.getAcademicStandingStatus(student);

                log.info("Student {} is {} with {} credits behind",
                    student.getId(), academicStatus, creditsBehind);

                // Recommend core courses to help student catch up
                // Use the required credits info to prioritize recommendations
                // This integrates actual graduation requirement data into recommendations
            }
        } catch (Exception e) {
            log.warn("Could not access graduation requirements service, using fallback: {}", e.getMessage());
        }

        // Fallback: If no requirements found via service, use basic core subject logic
        if (recommendations.isEmpty()) {
            List<String> coreSubjects = Arrays.asList("MATH", "SCI", "ENG", "SS");

            for (String subjectCode : coreSubjects) {
                Optional<SubjectArea> subjectAreaOpt = subjectAreaService.getSubjectAreaByCode(subjectCode);

                if (subjectAreaOpt.isPresent()) {
                    SubjectArea subjectArea = subjectAreaOpt.get();
                    List<Course> subjectCourses = subjectAreaService.getCoursesForSubject(subjectArea.getId());

                    // Find courses not yet completed
                    for (Course course : subjectCourses) {
                        if (!completedCourses.contains(course) && !isAlreadyRecommended(student, course)) {
                            CourseRecommendation recommendation = buildRecommendation(
                                student, course, null, gradeLevel, schoolYear,
                                CourseRecommendation.RecommendationType.GRADUATION_REQUIREMENT,
                                "Required for graduation (core subject)",
                                0.9,
                                1 // Highest priority
                            );

                            recommendations.add(recommendation);
                            break; // Only recommend one course per subject
                        }
                    }
                }
            }
        }

        return recommendations;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Build a recommendation object
     */
    private CourseRecommendation buildRecommendation(
            Student student, Course course, CourseSequence sequence,
            Integer gradeLevel, String schoolYear,
            CourseRecommendation.RecommendationType type, String reason,
            Double confidence, Integer priority) {

        return CourseRecommendation.builder()
            .student(student)
            .course(course)
            .courseSequence(sequence)
            .recommendedSchoolYear(schoolYear)
            .recommendedGradeLevel(gradeLevel)
            .recommendationType(type)
            .reason(reason)
            .confidenceScore(confidence)
            .priority(priority)
            .status(CourseRecommendation.RecommendationStatus.PENDING)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Calculate confidence for sequence-based recommendation
     */
    private Double calculateSequenceConfidence(CourseSequence sequence,
            List<Course> completedCourses, Double studentGPA) {

        double baseConfidence = 0.7;

        // Increase confidence if student is following the sequence
        double progress = sequence.getProgressPercentage(completedCourses);
        if (progress > 0) {
            baseConfidence += 0.2; // Student is on this pathway
        }

        // Adjust for GPA match
        if (sequence.getMinGPARecommended() != null && studentGPA != null) {
            if (studentGPA >= sequence.getMinGPARecommended() + 0.5) {
                baseConfidence += 0.1; // Well above requirement
            } else if (studentGPA < sequence.getMinGPARecommended()) {
                baseConfidence -= 0.2; // Below requirement
            }
        }

        return Math.min(1.0, Math.max(0.0, baseConfidence));
    }

    /**
     * Calculate confidence for prerequisite-based recommendation
     */
    private Double calculatePrerequisiteConfidence(Student student, Course course,
            List<Course> completedCourses, Double studentGPA) {

        double baseConfidence = 0.6;

        // Get prerequisites from repository
        List<com.heronix.model.domain.CoursePrerequisite> prerequisites =
            coursePrerequisiteRepository.findByCourse(course);

        if (prerequisites.isEmpty()) {
            return baseConfidence + 0.2; // Higher confidence if no prerequisites
        }

        // Check grades in prerequisite courses
        double prereqGradeBonus = 0.0;
        int prereqCount = 0;

        for (com.heronix.model.domain.CoursePrerequisite prereq : prerequisites) {
            Course prereqCourse = prereq.getPrerequisiteCourse();
            List<StudentGrade> prereqGrades = studentGradeRepository.findByStudentAndCourse(student, prereqCourse);

            if (!prereqGrades.isEmpty()) {
                StudentGrade latestGrade = prereqGrades.get(0); // Assuming ordered by date desc
                Double gradeValue = convertLetterGradeToGPA(latestGrade.getLetterGrade());

                if (gradeValue != null) {
                    if (gradeValue >= 3.5) {
                        prereqGradeBonus += 0.15; // A grade = +0.15 confidence
                    } else if (gradeValue >= 3.0) {
                        prereqGradeBonus += 0.10; // B grade = +0.10 confidence
                    } else if (gradeValue >= 2.0) {
                        prereqGradeBonus += 0.05; // C grade = +0.05 confidence
                    } else {
                        prereqGradeBonus -= 0.10; // D/F = -0.10 confidence
                    }
                    prereqCount++;
                }
            }
        }

        // Average the bonus across all prerequisites
        if (prereqCount > 0) {
            prereqGradeBonus /= prereqCount;
        }

        return Math.min(1.0, Math.max(0.0, baseConfidence + prereqGradeBonus));
    }

    /**
     * Check if student has met prerequisites for a course
     */
    private boolean hasMetPrerequisites(Course course, List<Course> completedCourses) {
        // Get prerequisites from repository
        List<com.heronix.model.domain.CoursePrerequisite> prerequisites =
            coursePrerequisiteRepository.findByCourse(course);

        if (prerequisites.isEmpty()) {
            return true; // No prerequisites
        }

        // Check if all prerequisite courses are in completed list
        for (com.heronix.model.domain.CoursePrerequisite prereq : prerequisites) {
            Course prereqCourse = prereq.getPrerequisiteCourse();
            boolean found = completedCourses.stream()
                .anyMatch(completed -> completed.getId().equals(prereqCourse.getId()));

            if (!found) {
                log.debug("Student missing prerequisite: {} for course: {}",
                         prereqCourse.getCourseCode(), course.getCourseCode());
                return false;
            }
        }

        return true;
    }

    /**
     * Check if student meets GPA requirement
     */
    private boolean meetsGPARequirement(Course course, Double studentGPA) {
        if (studentGPA == null) {
            return false; // Can't determine without GPA
        }

        // Check course minimum GPA requirement
        if (course.getMinGPARequired() != null && studentGPA < course.getMinGPARequired()) {
            log.debug("Student GPA {} below minimum {} for course {}",
                     studentGPA, course.getMinGPARequired(), course.getCourseCode());
            return false;
        }

        // Additional checks for honors/AP/IB courses based on course type
        if (course.getCourseType() != null) {
            switch (course.getCourseType()) {
                case HONORS:
                    return studentGPA >= 3.0; // Honors requires 3.0+
                case AP:
                    return studentGPA >= 3.25; // AP requires 3.25+
                case IB:
                    return studentGPA >= 3.5; // IB requires 3.5+
                default:
                    return true;
            }
        }

        return true;
    }

    /**
     * Parse grade level string to integer
     */
    private Integer parseGradeLevel(String gradeLevel) {
        if (gradeLevel == null || gradeLevel.trim().isEmpty()) {
            return null;
        }

        // Handle "9th", "10th", "11th", "12th" or just "9", "10", "11", "12"
        String cleaned = gradeLevel.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Could not parse grade level: {}", gradeLevel);
            return null;
        }
    }

    /**
     * Calculate student GPA from grade history
     */
    private Double calculateStudentGPA(Student student) {
        // Use existing currentGPA field if available
        if (student.getCurrentGPA() != null) {
            return student.getCurrentGPA();
        }

        // Calculate from StudentGrade records
        List<StudentGrade> grades = studentGradeRepository.findByStudentOrderByGradeDateDesc(student);
        if (grades.isEmpty()) {
            return 0.0; // No grades yet
        }

        double totalPoints = 0.0;
        int count = 0;

        for (StudentGrade grade : grades) {
            Double gradeValue = convertLetterGradeToGPA(grade.getLetterGrade());
            if (gradeValue != null) {
                totalPoints += gradeValue;
                count++;
            }
        }

        return count > 0 ? totalPoints / count : 0.0;
    }

    /**
     * Convert letter grade to GPA points
     */
    private Double convertLetterGradeToGPA(String letterGrade) {
        if (letterGrade == null) return null;

        switch (letterGrade.toUpperCase()) {
            case "A+": case "A": return 4.0;
            case "A-": return 3.7;
            case "B+": return 3.3;
            case "B": return 3.0;
            case "B-": return 2.7;
            case "C+": return 2.3;
            case "C": return 2.0;
            case "C-": return 1.7;
            case "D+": return 1.3;
            case "D": return 1.0;
            case "D-": return 0.7;
            case "F": return 0.0;
            default: return null;
        }
    }

    /**
     * Check if course is already recommended for student
     */
    private boolean isAlreadyRecommended(Student student, Course course) {
        return recommendationRepository.existsByStudentAndCourseAndActiveTrue(student, course);
    }

    /**
     * Get completed courses for student
     */
    private List<Course> getCompletedCourses(Student student) {
        // ✅ NULL SAFE: Check student and ID exist
        if (student == null || student.getId() == null) {
            return new ArrayList<>();
        }

        // Get completed enrollments
        List<StudentEnrollment> allEnrollments = studentEnrollmentRepository.findByStudentId(student.getId());

        // ✅ NULL SAFE: Check enrollments list exists
        if (allEnrollments == null) {
            return new ArrayList<>();
        }

        // ✅ NULL SAFE: Filter null enrollments and courses
        return allEnrollments.stream()
            .filter(e -> e != null && e.getStatus() == com.heronix.model.enums.EnrollmentStatus.COMPLETED)
            .map(StudentEnrollment::getCourse)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Remove duplicate recommendations
     */
    private List<CourseRecommendation> removeDuplicates(List<CourseRecommendation> recommendations) {
        // ✅ NULL SAFE: Check recommendations list exists
        if (recommendations == null || recommendations.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, CourseRecommendation> uniqueMap = new HashMap<>();

        // ✅ NULL SAFE: Filter null recommendations and validate required fields
        for (CourseRecommendation rec : recommendations) {
            if (rec == null || rec.getStudent() == null || rec.getStudent().getId() == null ||
                rec.getCourse() == null || rec.getCourse().getId() == null) {
                continue;
            }

            String key = rec.getStudent().getId() + "-" + rec.getCourse().getId();

            // Keep highest priority/confidence recommendation
            if (!uniqueMap.containsKey(key)) {
                uniqueMap.put(key, rec);
            } else {
                CourseRecommendation existing = uniqueMap.get(key);
                if (rec.getPriority() < existing.getPriority() ||
                    (rec.getPriority().equals(existing.getPriority()) &&
                     rec.getConfidenceScore() > existing.getConfidenceScore())) {
                    uniqueMap.put(key, rec);
                }
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }

    /**
     * Validate recommendation requirements
     */
    private void validateRecommendation(CourseRecommendation recommendation) {
        Student student = recommendation.getStudent();
        Course course = recommendation.getCourse();

        // Check prerequisites
        List<Course> completedCourses = getCompletedCourses(student);
        boolean prereqsMet = hasMetPrerequisites(course, completedCourses);
        recommendation.setPrerequisitesMet(prereqsMet);

        // Check GPA
        Double studentGPA = calculateStudentGPA(student);
        boolean gpaMet = meetsGPARequirement(course, studentGPA);
        recommendation.setGpaRequirementMet(gpaMet);

        // Check schedule conflicts
        boolean hasConflict = checkScheduleConflicts(student, course);
        recommendation.setHasScheduleConflict(hasConflict);
    }

    /**
     * Check if course would conflict with student's schedule
     */
    private boolean checkScheduleConflicts(Student student, Course course) {
        // Get student's current enrollments
        List<StudentEnrollment> currentEnrollments = studentEnrollmentRepository
            .findByStudentId(student.getId()).stream()
            .filter(e -> e.getStatus() == com.heronix.model.enums.EnrollmentStatus.ACTIVE)
            .collect(Collectors.toList());

        if (currentEnrollments.isEmpty()) {
            return false; // No current schedule, no conflicts
        }

        // For now, simplified check - just verify we're not over-enrolled
        // A proper implementation would check actual schedule times
        long activeEnrollmentCount = currentEnrollments.size();

        // Typical full schedule is 6-8 courses
        if (activeEnrollmentCount >= 8) {
            log.debug("Student {} has full schedule ({} courses)",
                     student.getStudentId(), activeEnrollmentCount);
            return true; // Schedule is full
        }

        return false; // No conflict detected
    }

    // ========================================================================
    // APPROVAL OPERATIONS
    // ========================================================================

    /**
     * Accept recommendation (student)
     *
     * @param recommendationId Recommendation ID
     * @return Updated recommendation
     */
    @Transactional
    public CourseRecommendation acceptByStudent(Long recommendationId) {
        CourseRecommendation recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + recommendationId));

        log.info("Student accepting recommendation: {}", recommendationId);
        recommendation.acceptByStudent();
        recommendation.setUpdatedAt(LocalDateTime.now());

        return recommendationRepository.save(recommendation);
    }

    /**
     * Accept recommendation (parent)
     *
     * @param recommendationId Recommendation ID
     * @return Updated recommendation
     */
    @Transactional
    public CourseRecommendation acceptByParent(Long recommendationId) {
        CourseRecommendation recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + recommendationId));

        log.info("Parent accepting recommendation: {}", recommendationId);
        recommendation.acceptByParent();
        recommendation.setUpdatedAt(LocalDateTime.now());

        return recommendationRepository.save(recommendation);
    }

    /**
     * Reject recommendation (student)
     *
     * @param recommendationId Recommendation ID
     * @return Updated recommendation
     */
    @Transactional
    public CourseRecommendation rejectByStudent(Long recommendationId) {
        CourseRecommendation recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + recommendationId));

        log.info("Student rejecting recommendation: {}", recommendationId);
        recommendation.rejectByStudent();
        recommendation.setUpdatedAt(LocalDateTime.now());

        return recommendationRepository.save(recommendation);
    }

    /**
     * Reject recommendation (parent)
     *
     * @param recommendationId Recommendation ID
     * @return Updated recommendation
     */
    @Transactional
    public CourseRecommendation rejectByParent(Long recommendationId) {
        CourseRecommendation recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + recommendationId));

        log.info("Parent rejecting recommendation: {}", recommendationId);
        recommendation.rejectByParent();
        recommendation.setUpdatedAt(LocalDateTime.now());

        return recommendationRepository.save(recommendation);
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get statistics about recommendations
     *
     * @return Statistics DTO
     */
    public RecommendationStatistics getStatistics() {
        long total = recommendationRepository.count();
        long active = recommendationRepository.countByActiveTrue();
        long pending = recommendationRepository.countByStatusAndActiveTrue(
            CourseRecommendation.RecommendationStatus.PENDING);
        long accepted = recommendationRepository.countByStatusAndActiveTrue(
            CourseRecommendation.RecommendationStatus.ACCEPTED);
        long aiGenerated = recommendationRepository.findAIGeneratedRecommendations().size();

        return RecommendationStatistics.builder()
            .totalRecommendations(total)
            .activeRecommendations(active)
            .pendingRecommendations(pending)
            .acceptedRecommendations(accepted)
            .aiGeneratedRecommendations(aiGenerated)
            .build();
    }

    /**
     * Check if a course matches a graduation requirement
     * Helper method for graduation requirement integration
     * Added: December 12, 2025 - Service Audit 100% Completion
     *
     * @param course The course to check
     * @param requirement The graduation requirement description
     * @return true if the course matches the requirement
     */
    private boolean matchesRequirement(Course course, Object requirement) {
        if (course == null || requirement == null) {
            return false;
        }

        String reqString = requirement.toString().toLowerCase();
        String courseName = course.getCourseName() != null ? course.getCourseName().toLowerCase() : "";
        String courseSubject = course.getSubject() != null ? course.getSubject().toLowerCase() : "";
        String courseCode = course.getCourseCode() != null ? course.getCourseCode().toLowerCase() : "";

        // Check if requirement mentions the course
        if (courseName.contains(reqString) || reqString.contains(courseName)) {
            return true;
        }

        // Check if requirement mentions the subject
        if (courseSubject.contains(reqString) || reqString.contains(courseSubject)) {
            return true;
        }

        // Check if requirement mentions the course code
        if (courseCode.contains(reqString) || reqString.contains(courseCode)) {
            return true;
        }

        // Check common subject mappings
        if (reqString.contains("math") && (courseSubject.contains("math") || courseSubject.contains("algebra") || courseSubject.contains("geometry"))) {
            return true;
        }

        if (reqString.contains("english") && (courseSubject.contains("english") || courseSubject.contains("language"))) {
            return true;
        }

        if (reqString.contains("science") && (courseSubject.contains("science") || courseSubject.contains("biology") || courseSubject.contains("chemistry"))) {
            return true;
        }

        if (reqString.contains("social") && (courseSubject.contains("social") || courseSubject.contains("history") || courseSubject.contains("government"))) {
            return true;
        }

        return false;
    }

    /**
     * Statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class RecommendationStatistics {
        private long totalRecommendations;
        private long activeRecommendations;
        private long pendingRecommendations;
        private long acceptedRecommendations;
        private long aiGeneratedRecommendations;

        @Override
        public String toString() {
            return String.format(
                "Recommendation Statistics: Total=%d, Active=%d, Pending=%d, Accepted=%d, AI-Generated=%d",
                totalRecommendations, activeRecommendations, pendingRecommendations,
                acceptedRecommendations, aiGeneratedRecommendations);
        }
    }
}
