package com.heronix.service;

import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentGrade;
import com.heronix.model.domain.Course;
import com.heronix.repository.StudentGradeRepository;
import com.heronix.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Grade Service
 * Location: src/main/java/com/eduscheduler/service/GradeService.java
 *
 * Business logic for student grades, GPA calculation, and academic performance tracking.
 * Handles grade entry, GPA calculations, honor roll determination, and academic standing updates.
 *
 * Features:
 * - GPA calculation (weighted and unweighted)
 * - Grade entry and validation
 * - Honor roll determination
 * - Academic standing updates
 * - Transcript generation support
 * - Course eligibility checks
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-20
 */
@Slf4j
@Service
@Transactional
public class GradeService {

    @Autowired
    private StudentGradeRepository gradeRepository;

    @Autowired
    private StudentRepository studentRepository;

    // GPA Thresholds
    private static final double HONORS_GPA_THRESHOLD = 3.5;
    private static final double HIGH_HONORS_GPA_THRESHOLD = 3.75;
    private static final double AP_ELIGIBLE_GPA_THRESHOLD = 3.2;
    private static final double ACADEMIC_WARNING_GPA = 2.0;
    private static final double ACADEMIC_PROBATION_GPA = 1.5;

    // ========================================================================
    // GRADE ENTRY AND MANAGEMENT
    // ========================================================================

    /**
     * Add or update a grade for a student
     */
    public StudentGrade saveGrade(StudentGrade grade) {
        log.debug("Saving grade for student: {}, course: {}",
            grade.getStudentName(), grade.getCourseName());

        // Validate grade
        if (!grade.isValid()) {
            throw new IllegalArgumentException("Invalid grade data");
        }

        // Auto-calculate GPA points if not set
        if (grade.getGpaPoints() == null && grade.getLetterGrade() != null) {
            Double gpaPoints = StudentGrade.letterGradeToGpaPoints(grade.getLetterGrade());
            if (gpaPoints != null) {
                grade.setGpaPoints(gpaPoints);
            }
        }

        // Auto-calculate letter grade from numerical if needed
        if (grade.getLetterGrade() == null && grade.getNumericalGrade() != null) {
            grade.setLetterGrade(StudentGrade.numericalGradeToLetterGrade(grade.getNumericalGrade()));
        }

        // Set entered date
        if (grade.getEnteredDate() == null) {
            grade.setEnteredDate(LocalDate.now());
        }

        StudentGrade saved = gradeRepository.save(grade);

        // Recalculate student's GPA if this is a final grade
        if (grade.getIsFinal() && grade.getIncludeInGPA()) {
            recalculateStudentGPA(grade.getStudent());
        }

        log.info("Grade saved: {} - {} in {}",
            grade.getStudentName(), grade.getLetterGrade(), grade.getCourseName());

        return saved;
    }

    /**
     * Save multiple grades at once (bulk entry)
     */
    public List<StudentGrade> saveGrades(List<StudentGrade> grades) {
        log.info("Bulk saving {} grades", grades.size());

        List<StudentGrade> savedGrades = new ArrayList<>();
        Set<Student> affectedStudents = new HashSet<>();

        for (StudentGrade grade : grades) {
            StudentGrade saved = saveGrade(grade);
            savedGrades.add(saved);
            if (grade.getIsFinal() && grade.getIncludeInGPA()) {
                affectedStudents.add(grade.getStudent());
            }
        }

        // Recalculate GPA for all affected students
        for (Student student : affectedStudents) {
            recalculateStudentGPA(student);
        }

        log.info("Bulk save complete: {} grades saved, {} students updated",
            savedGrades.size(), affectedStudents.size());

        return savedGrades;
    }

    /**
     * Delete a grade
     */
    public void deleteGrade(Long gradeId) {
        Optional<StudentGrade> gradeOpt = gradeRepository.findById(gradeId);
        if (gradeOpt.isPresent()) {
            StudentGrade grade = gradeOpt.get();
            Student student = grade.getStudent();

            gradeRepository.deleteById(gradeId);
            log.info("Grade deleted: {}", gradeId);

            // Recalculate GPA
            if (grade.getIsFinal() && grade.getIncludeInGPA()) {
                recalculateStudentGPA(student);
            }
        }
    }

    // ========================================================================
    // GPA CALCULATION
    // ========================================================================

    /**
     * Calculate and update student's GPA (both weighted and unweighted)
     */
    public void recalculateStudentGPA(Student student) {
        log.debug("Recalculating GPA for student: {}", student.getFullName());

        // Get all final grades that count toward GPA
        List<StudentGrade> finalGrades = gradeRepository.findFinalGradesByStudentId(student.getId());

        if (finalGrades.isEmpty()) {
            log.debug("No final grades found for student: {}", student.getFullName());
            return;
        }

        // Calculate unweighted GPA (4.0 scale)
        double totalPoints = 0.0;
        double totalCredits = 0.0;

        for (StudentGrade grade : finalGrades) {
            totalPoints += grade.getGpaPoints() * grade.getCredits();
            totalCredits += grade.getCredits();
        }

        double unweightedGPA = totalCredits > 0 ? totalPoints / totalCredits : 0.0;

        // Calculate weighted GPA (5.0 scale for honors/AP)
        double weightedPoints = 0.0;
        for (StudentGrade grade : finalGrades) {
            weightedPoints += grade.getWeightedGpaPoints() * grade.getCredits();
        }

        double weightedGPA = totalCredits > 0 ? weightedPoints / totalCredits : 0.0;

        // Calculate total credits earned
        double creditsEarned = finalGrades.stream()
            .filter(g -> g.isPassing())
            .mapToDouble(StudentGrade::getCredits)
            .sum();

        // Update student record
        student.setUnweightedGPA(Math.round(unweightedGPA * 100.0) / 100.0);
        student.setWeightedGPA(Math.round(weightedGPA * 100.0) / 100.0);
        student.setCurrentGPA(Math.round(unweightedGPA * 100.0) / 100.0); // Default to unweighted
        student.setCreditsEarned(creditsEarned);

        // Update eligibility flags
        updateStudentEligibility(student);

        // Update academic standing
        updateAcademicStanding(student);

        // Update honor roll status
        updateHonorRollStatus(student);

        studentRepository.save(student);

        log.info("GPA updated for {}: Unweighted={}, Weighted={}, Credits={}",
            student.getFullName(), unweightedGPA, weightedGPA, creditsEarned);
    }

    /**
     * Calculate GPA for specific term
     */
    public double calculateTermGPA(Student student, String term) {
        List<StudentGrade> termGrades = gradeRepository.findByStudentIdAndTerm(student.getId(), term);

        if (termGrades.isEmpty()) return 0.0;

        double totalPoints = termGrades.stream()
            .filter(g -> g.getIsFinal() && g.getIncludeInGPA())
            .mapToDouble(g -> g.getGpaPoints() * g.getCredits())
            .sum();

        double totalCredits = termGrades.stream()
            .filter(g -> g.getIsFinal() && g.getIncludeInGPA())
            .mapToDouble(StudentGrade::getCredits)
            .sum();

        return totalCredits > 0 ? totalPoints / totalCredits : 0.0;
    }

    // ========================================================================
    // ELIGIBILITY AND STANDING
    // ========================================================================

    /**
     * Update student eligibility for honors and AP courses
     */
    private void updateStudentEligibility(Student student) {
        double gpa = student.getUnweightedGPA() != null ? student.getUnweightedGPA() : 0.0;

        student.setHonorsEligible(gpa >= HONORS_GPA_THRESHOLD);
        student.setApEligible(gpa >= AP_ELIGIBLE_GPA_THRESHOLD);

        log.debug("Eligibility updated for {}: Honors={}, AP={}",
            student.getFullName(), student.getHonorsEligible(), student.getApEligible());
    }

    /**
     * Update academic standing based on GPA
     */
    private void updateAcademicStanding(Student student) {
        double gpa = student.getUnweightedGPA() != null ? student.getUnweightedGPA() : 0.0;

        String standing;
        if (gpa < ACADEMIC_PROBATION_GPA) {
            standing = "Academic Probation";
        } else if (gpa < ACADEMIC_WARNING_GPA) {
            standing = "Academic Warning";
        } else {
            standing = "Good Standing";
        }

        student.setAcademicStanding(standing);

        log.debug("Academic standing updated for {}: {}", student.getFullName(), standing);
    }

    /**
     * Update honor roll status
     */
    private void updateHonorRollStatus(Student student) {
        double gpa = student.getUnweightedGPA() != null ? student.getUnweightedGPA() : 0.0;

        String honorRoll = null;
        if (gpa >= HIGH_HONORS_GPA_THRESHOLD) {
            honorRoll = "High Honor Roll";
        } else if (gpa >= HONORS_GPA_THRESHOLD) {
            honorRoll = "Honor Roll";
        }

        student.setHonorRollStatus(honorRoll);

        if (honorRoll != null) {
            log.debug("Honor roll status updated for {}: {}", student.getFullName(), honorRoll);
        }
    }

    // ========================================================================
    // QUERIES AND REPORTS
    // ========================================================================

    /**
     * Get all grades for a student
     */
    public List<StudentGrade> getStudentGrades(Long studentId) {
        return gradeRepository.findByStudentId(studentId);
    }

    /**
     * Get student transcript (final grades only)
     */
    public List<StudentGrade> getStudentTranscript(Long studentId) {
        return gradeRepository.findFinalGradesByStudentId(studentId);
    }

    /**
     * Get grades for a specific term
     */
    public List<StudentGrade> getTermGrades(Long studentId, String term) {
        return gradeRepository.findByStudentIdAndTerm(studentId, term);
    }

    /**
     * Get honor roll students for a term
     */
    public List<Student> getHonorRollStudents(String term, boolean highHonors) {
        double threshold = highHonors ? HIGH_HONORS_GPA_THRESHOLD : HONORS_GPA_THRESHOLD;
        return gradeRepository.findHonorRollStudents(term, threshold);
    }

    /**
     * Get students who need academic intervention (low GPA)
     */
    public List<Student> getStudentsNeedingIntervention() {
        return studentRepository.findAll().stream()
            .filter(s -> s.getActive() && s.getCurrentGPA() != null)
            .filter(s -> s.getCurrentGPA() < ACADEMIC_WARNING_GPA)
            .collect(Collectors.toList());
    }

    /**
     * Get students with improved GPA
     */
    public List<Student> getStudentsWithImprovedGPA() {
        return gradeRepository.findStudentsWithImprovedGPA();
    }

    /**
     * Get students with declining GPA
     */
    public List<Student> getStudentsWithDecliningGPA() {
        return gradeRepository.findStudentsWithDecliningGPA();
    }

    /**
     * Check if student is eligible for a specific course based on prerequisites and GPA
     */
    public boolean isEligibleForCourse(Student student, Course course) {
        double studentGPA = student.getCurrentGPA() != null ? student.getCurrentGPA() : 0.0;

        // Check course name for honors/AP indicators
        String courseName = course.getCourseName() != null ? course.getCourseName().toUpperCase() : "";
        String subject = course.getSubject() != null ? course.getSubject().toUpperCase() : "";

        // Check if course is honors/AP and student meets GPA requirement
        if (courseName.contains("HONORS") || subject.contains("HONORS")) {
            return studentGPA >= HONORS_GPA_THRESHOLD;
        }

        if (courseName.contains("AP ") || courseName.contains("ADVANCED PLACEMENT") ||
            subject.contains("AP ")) {
            return studentGPA >= AP_ELIGIBLE_GPA_THRESHOLD;
        }

        // All students eligible for regular courses
        return true;
    }

    /**
     * Get grade distribution for a course
     */
    public Map<String, Long> getGradeDistribution(Long courseId) {
        List<Object[]> distribution = gradeRepository.getGradeDistributionForCourse(courseId);

        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : distribution) {
            result.put((String) row[0], (Long) row[1]);
        }

        return result;
    }

    /**
     * Get class rank for a student
     */
    public int calculateClassRank(Student student) {
        if (student.getCurrentGPA() == null) return 0;

        // Get all students in same grade level
        List<Student> classmates = studentRepository.findAll().stream()
            .filter(s -> s.getActive() && s.getGradeLevel().equals(student.getGradeLevel()))
            .filter(s -> s.getCurrentGPA() != null)
            .sorted((s1, s2) -> Double.compare(s2.getCurrentGPA(), s1.getCurrentGPA())) // Descending
            .collect(Collectors.toList());

        int rank = 1;
        for (Student s : classmates) {
            if (s.getId().equals(student.getId())) {
                return rank;
            }
            rank++;
        }

        return 0;
    }

    /**
     * Update class ranks for all students in a grade level
     */
    public void updateClassRanks(String gradeLevel) {
        List<Student> students = studentRepository.findAll().stream()
            .filter(s -> s.getActive() && s.getGradeLevel().equals(gradeLevel))
            .filter(s -> s.getCurrentGPA() != null)
            .sorted((s1, s2) -> Double.compare(s2.getCurrentGPA(), s1.getCurrentGPA()))
            .collect(Collectors.toList());

        int classSize = students.size();
        int rank = 1;

        for (Student student : students) {
            student.setClassRank(rank);
            student.setClassSize(classSize);
            studentRepository.save(student);
            rank++;
        }

        log.info("Class ranks updated for grade {}: {} students", gradeLevel, classSize);
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get average GPA across all students
     */
    public double getAverageGPA() {
        Double avg = gradeRepository.getAverageGPAAcrossAllStudents();
        return avg != null ? avg : 0.0;
    }

    /**
     * Get GPA statistics
     */
    public Map<String, Object> getGPAStatistics() {
        List<Student> activeStudents = studentRepository.findAll().stream()
            .filter(Student::isActive)
            .filter(s -> s.getCurrentGPA() != null)
            .collect(Collectors.toList());

        if (activeStudents.isEmpty()) {
            return Collections.emptyMap();
        }

        DoubleSummaryStatistics stats = activeStudents.stream()
            .mapToDouble(Student::getCurrentGPA)
            .summaryStatistics();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("average", Math.round(stats.getAverage() * 100.0) / 100.0);
        result.put("min", Math.round(stats.getMin() * 100.0) / 100.0);
        result.put("max", Math.round(stats.getMax() * 100.0) / 100.0);
        result.put("count", stats.getCount());

        // Count students in each category
        long honorRoll = activeStudents.stream()
            .filter(s -> s.getCurrentGPA() >= HONORS_GPA_THRESHOLD)
            .count();
        long highHonorRoll = activeStudents.stream()
            .filter(s -> s.getCurrentGPA() >= HIGH_HONORS_GPA_THRESHOLD)
            .count();
        long academicWarning = activeStudents.stream()
            .filter(s -> s.getCurrentGPA() < ACADEMIC_WARNING_GPA)
            .count();

        result.put("honorRollCount", honorRoll);
        result.put("highHonorRollCount", highHonorRoll);
        result.put("academicWarningCount", academicWarning);

        return result;
    }
}
