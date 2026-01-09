package com.heronix.service;

import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentGrade;
import com.heronix.model.domain.Course;
import com.heronix.model.enums.CourseType;
import com.heronix.repository.StudentGradeRepository;
import com.heronix.repository.CourseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Student Placement Service - AI-Powered Course Assignment
 * Location: src/main/java/com/eduscheduler/service/StudentPlacementService.java
 *
 * Uses GPA, grade history, and AI analysis to recommend optimal course placements.
 * Considers academic performance, prerequisites, interests, and special needs.
 *
 * Features:
 * - Smart elective recommendations based on GPA and subject performance
 * - Honors/AP eligibility determination
 * - Special education course placement
 * - Gifted program recommendations
 * - Academic intervention course suggestions
 * - AI-powered analysis (when Ollama enabled)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-20
 */
@Slf4j
@Service
public class StudentPlacementService {

    @Autowired
    private StudentGradeRepository gradeRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private GradeService gradeService;

    @Autowired(required = false)
    private OllamaAIService aiService;

    // GPA Thresholds for placement
    private static final double HONORS_THRESHOLD = 3.5;
    private static final double AP_THRESHOLD = 3.2;
    private static final double INTERVENTION_THRESHOLD = 2.0;
    private static final double GIFTED_THRESHOLD = 3.8;

    // ========================================================================
    // COURSE RECOMMENDATIONS
    // ========================================================================

    /**
     * Get recommended electives for a student based on their academic performance
     */
    public List<Course> recommendElectives(Student student) {
        log.info("Generating elective recommendations for student: {}", student.getFullName());

        List<Course> recommendations = new ArrayList<>();
        double studentGPA = student.getCurrentGPA() != null ? student.getCurrentGPA() : 0.0;

        // Get student's strongest subjects (based on grade history)
        Map<String, Double> subjectAverages = getSubjectAverages(student);

        // Get available elective courses
        List<Course> allElectives = courseRepository.findAll().stream()
            .filter(c -> isElective(c))
            .filter(c -> gradeService.isEligibleForCourse(student, c))
            .collect(Collectors.toList());

        // Score and rank electives
        List<CourseRecommendation> scored = allElectives.stream()
            .map(course -> scoreElective(student, course, subjectAverages))
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .collect(Collectors.toList());

        // Return top recommendations
        recommendations = scored.stream()
            .limit(10)
            .map(cr -> cr.course)
            .collect(Collectors.toList());

        log.info("Generated {} elective recommendations for {}", recommendations.size(), student.getFullName());

        return recommendations;
    }

    /**
     * Recommend courses for special education students
     */
    public List<Course> recommendSpecialEdCourses(Student student) {
        log.info("Generating special education course recommendations for: {}", student.getFullName());

        List<Course> recommendations = new ArrayList<>();

        if (!student.getHasIEP() && !student.getHas504Plan()) {
            log.debug("Student {} does not have IEP or 504 plan", student.getFullName());
            return recommendations;
        }

        // Find courses suitable for special education (smaller class sizes, appropriate difficulty)
        recommendations = courseRepository.findAll().stream()
            .filter(c -> c.getMaxStudents() != null && c.getMaxStudents() <= 15) // Smaller classes
            .filter(c -> c.getCourseType() == CourseType.REGULAR) // Regular level courses
            .filter(c -> gradeService.isEligibleForCourse(student, c))
            .collect(Collectors.toList());

        log.info("Found {} SPED-appropriate courses for {}", recommendations.size(), student.getFullName());

        return recommendations;
    }

    /**
     * Recommend gifted program courses
     */
    public List<Course> recommendGiftedCourses(Student student) {
        log.info("Generating gifted program recommendations for: {}", student.getFullName());

        List<Course> recommendations = new ArrayList<>();
        double studentGPA = student.getCurrentGPA() != null ? student.getCurrentGPA() : 0.0;

        // Check if student qualifies for gifted program
        if (!student.getIsGifted() && studentGPA < GIFTED_THRESHOLD) {
            log.debug("Student {} does not qualify for gifted program (GPA: {})",
                student.getFullName(), studentGPA);
            return recommendations;
        }

        // Find honors and AP courses
        recommendations = courseRepository.findAll().stream()
            .filter(c -> isHonorsOrAP(c))
            .filter(c -> gradeService.isEligibleForCourse(student, c))
            .collect(Collectors.toList());

        log.info("Found {} gifted program courses for {}", recommendations.size(), student.getFullName());

        return recommendations;
    }

    /**
     * Recommend intervention courses for struggling students
     */
    public List<Course> recommendInterventionCourses(Student student) {
        log.info("Generating intervention course recommendations for: {}", student.getFullName());

        List<Course> recommendations = new ArrayList<>();
        double studentGPA = student.getCurrentGPA() != null ? student.getCurrentGPA() : 0.0;

        // Only recommend if student needs intervention
        if (studentGPA >= INTERVENTION_THRESHOLD) {
            log.debug("Student {} does not need intervention (GPA: {})",
                student.getFullName(), studentGPA);
            return recommendations;
        }

        // Find subjects where student is struggling
        Map<String, Double> subjectAverages = getSubjectAverages(student);
        List<String> weakSubjects = subjectAverages.entrySet().stream()
            .filter(e -> e.getValue() < 2.0) // C average or below
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Find intervention/remedial courses for weak subjects
        for (String subject : weakSubjects) {
            List<Course> interventionCourses = courseRepository.findAll().stream()
                .filter(c -> c.getSubject() != null && c.getSubject().equalsIgnoreCase(subject))
                .filter(c -> isRemediaCourse(c))
                .collect(Collectors.toList());

            recommendations.addAll(interventionCourses);
        }

        log.info("Found {} intervention courses for {}", recommendations.size(), student.getFullName());

        return recommendations;
    }

    /**
     * Get AI-powered course recommendations (requires Ollama)
     */
    public String getAIRecommendations(Student student) {
        if (aiService == null || !aiService.isOllamaAvailable()) {
            log.warn("AI service not available for recommendations");
            return "AI recommendations not available. Please enable Ollama AI service.";
        }

        log.info("Generating AI-powered recommendations for: {}", student.getFullName());

        // Build student profile for AI
        StringBuilder profile = new StringBuilder();
        profile.append("STUDENT PROFILE:\n");
        profile.append(String.format("Name: %s\n", student.getFullName()));
        profile.append(String.format("Grade Level: %s\n", student.getGradeLevel()));
        profile.append(String.format("Current GPA: %.2f\n", student.getCurrentGPA() != null ? student.getCurrentGPA() : 0.0));
        profile.append(String.format("Credits Earned: %.1f / %.1f\n",
            student.getCreditsEarned() != null ? student.getCreditsEarned() : 0.0,
            student.getCreditsRequired() != null ? student.getCreditsRequired() : 24.0));
        profile.append(String.format("Academic Standing: %s\n", student.getAcademicStanding()));

        // Add special considerations
        if (student.getHasIEP()) profile.append("- Has IEP\n");
        if (student.getHas504Plan()) profile.append("- Has 504 Plan\n");
        if (student.getIsGifted()) profile.append("- Identified as Gifted\n");
        if (student.getHonorsEligible()) profile.append("- Honors Eligible\n");
        if (student.getApEligible()) profile.append("- AP Eligible\n");

        // Add grade history
        try {
            Map<String, Double> subjectAverages = getSubjectAverages(student);
            if (!subjectAverages.isEmpty()) {
                profile.append("\nSUBJECT PERFORMANCE:\n");
                subjectAverages.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .forEach(e -> profile.append(String.format("- %s: %.2f GPA\n", e.getKey(), e.getValue())));
            }
        } catch (Exception e) {
            log.warn("Could not retrieve subject averages for student", e);
        }

        // Generate AI prompt
        String prompt = String.format(
            "%s\n\nBased on this student's academic profile, please provide:\n" +
            "1. Recommended elective courses that match their strengths\n" +
            "2. Suggested advanced courses if they qualify\n" +
            "3. Any intervention courses if needed\n" +
            "4. Career pathways that align with their performance\n" +
            "5. Specific course types to consider for academic growth\n\n" +
            "Format your response as clear, actionable recommendations.",
            profile.toString()
        );

        try {
            Optional<String> aiResponse = aiService.generate(prompt);
            if (aiResponse.isPresent()) {
                log.info("AI recommendations generated for {}", student.getFullName());
                return aiResponse.get();
            } else {
                log.warn("AI service returned empty response for {}", student.getFullName());
                return "AI service did not return recommendations. Please check Ollama server status.";
            }
        } catch (Exception e) {
            log.error("Error generating AI recommendations", e);
            return "Error generating AI recommendations: " + e.getMessage();
        }
    }

    // ========================================================================
    // BATCH OPERATIONS
    // ========================================================================

    /**
     * Assign students to electives based on GPA and performance
     */
    public Map<Student, List<Course>> batchAssignElectives(List<Student> students, int electivesPerStudent) {
        log.info("Batch assigning electives for {} students", students.size());

        Map<Student, List<Course>> assignments = new HashMap<>();

        for (Student student : students) {
            List<Course> recommended = recommendElectives(student);
            List<Course> assigned = recommended.stream()
                .limit(electivesPerStudent)
                .collect(Collectors.toList());
            assignments.put(student, assigned);
        }

        log.info("Batch elective assignment complete: {} students processed", students.size());

        return assignments;
    }

    /**
     * Identify students for honors/AP placement
     */
    public Map<String, List<Student>> identifyAdvancedPlacements() {
        log.info("Identifying students for advanced placements");

        Map<String, List<Student>> placements = new HashMap<>();

        // AP Eligible
        List<Student> apEligible = gradeRepository.findHonorsEligibleStudents(AP_THRESHOLD);
        placements.put("AP", apEligible);

        // Honors Eligible
        List<Student> honorsEligible = gradeRepository.findHonorsEligibleStudents(HONORS_THRESHOLD);
        placements.put("Honors", honorsEligible);

        // Gifted Program
        List<Student> giftedEligible = gradeRepository.findHonorsEligibleStudents(GIFTED_THRESHOLD);
        placements.put("Gifted", giftedEligible);

        log.info("Advanced placement identification complete: AP={}, Honors={}, Gifted={}",
            apEligible.size(), honorsEligible.size(), giftedEligible.size());

        return placements;
    }

    /**
     * Identify students needing academic support
     */
    public List<Student> identifyStudentsNeedingSupport() {
        return gradeService.getStudentsNeedingIntervention();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Calculate average GPA for each subject a student has taken
     */
    private Map<String, Double> getSubjectAverages(Student student) {
        List<StudentGrade> grades = gradeRepository.findFinalGradesByStudentId(student.getId());

        Map<String, List<Double>> subjectGrades = new HashMap<>();

        for (StudentGrade grade : grades) {
            if (grade.getCourse() != null) {
                String subject = grade.getCourse().getSubject();
                if (subject != null && !subject.trim().isEmpty()) {
                    subjectGrades.computeIfAbsent(subject, k -> new ArrayList<>())
                        .add(grade.getGpaPoints());
                }
            }
        }

        Map<String, Double> averages = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : subjectGrades.entrySet()) {
            double avg = entry.getValue().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            averages.put(entry.getKey(), avg);
        }

        return averages;
    }

    /**
     * Score an elective course for a student
     */
    private CourseRecommendation scoreElective(Student student, Course course, Map<String, Double> subjectAverages) {
        double score = 0.0;

        // Match with strongest subject (+3 points)
        String courseSubject = course.getSubject();
        if (courseSubject != null && subjectAverages.containsKey(courseSubject)) {
            double subjectGPA = subjectAverages.get(courseSubject);
            if (subjectGPA >= 3.5) {
                score += 3.0;
            } else if (subjectGPA >= 3.0) {
                score += 2.0;
            } else if (subjectGPA >= 2.5) {
                score += 1.0;
            }
        }

        // Gifted student bonus for challenging courses (+2 points)
        if (student.getIsGifted() && isHonorsOrAP(course)) {
            score += 2.0;
        }

        // Interest alignment (check if related to strong subjects) (+1 point)
        for (String strongSubject : getStrongSubjects(subjectAverages)) {
            if (courseSubject != null && courseSubject.contains(strongSubject)) {
                score += 1.0;
                break;
            }
        }

        // Special education support (+1 point if needed - smaller class sizes)
        if (student.getHasIEP() && course.getMaxStudents() != null && course.getMaxStudents() <= 15) {
            score += 1.0;
        }

        return new CourseRecommendation(course, score);
    }

    /**
     * Check if course is an elective
     */
    private boolean isElective(Course course) {
        String courseName = course.getCourseName() != null ? course.getCourseName().toLowerCase() : "";
        String subject = course.getSubject() != null ? course.getSubject().toLowerCase() : "";

        // Common elective indicators
        return courseName.contains("elective") ||
               courseName.contains("art") ||
               courseName.contains("music") ||
               courseName.contains("drama") ||
               courseName.contains("computer science") ||
               courseName.contains("programming") ||
               courseName.contains("photography") ||
               courseName.contains("journalism") ||
               courseName.contains("yearbook") ||
               subject.contains("elective") ||
               subject.contains("arts") ||
               subject.contains("technology");
    }

    /**
     * Check if course is honors or AP
     */
    private boolean isHonorsOrAP(Course course) {
        String courseName = course.getCourseName() != null ? course.getCourseName().toUpperCase() : "";
        return courseName.contains("HONORS") || courseName.contains("AP ") || courseName.contains("ADVANCED PLACEMENT");
    }

    /**
     * Check if course is remedial/intervention
     */
    private boolean isRemediaCourse(Course course) {
        String courseName = course.getCourseName() != null ? course.getCourseName().toLowerCase() : "";
        return courseName.contains("remedial") ||
               courseName.contains("intervention") ||
               courseName.contains("support") ||
               courseName.contains("basics") ||
               courseName.contains("fundamentals");
    }

    /**
     * Get list of subjects where student excels
     */
    private List<String> getStrongSubjects(Map<String, Double> subjectAverages) {
        return subjectAverages.entrySet().stream()
            .filter(e -> e.getValue() >= 3.5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Inner class for course recommendations with scores
     */
    private static class CourseRecommendation {
        Course course;
        double score;

        CourseRecommendation(Course course, double score) {
            this.course = course;
            this.score = score;
        }
    }
}
