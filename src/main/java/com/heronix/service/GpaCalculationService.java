package com.heronix.service;

import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentGrade;
import com.heronix.model.domain.Course;
import com.heronix.repository.StudentGradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * GPA Calculation Service
 *
 * Centralized service for calculating student GPAs in various ways.
 * Provides cumulative, weighted, and term-based GPA calculations.
 *
 * Location: src/main/java/com/eduscheduler/service/GpaCalculationService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 10, 2025 - HIGH Priority Fix #11
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GpaCalculationService {

    private final StudentGradeRepository studentGradeRepository;

    /**
     * Calculate cumulative GPA for student (unweighted)
     *
     * @param student The student
     * @return Cumulative unweighted GPA
     */
    public Double calculateCumulativeGPA(Student student) {
        List<StudentGrade> allGrades = studentGradeRepository.findByStudentOrderByGradeDateDesc(student);

        if (allGrades.isEmpty()) {
            return 0.0;
        }

        double totalPoints = 0.0;
        double totalCredits = 0.0;

        for (StudentGrade grade : allGrades) {
            Double gradePoints = convertLetterGradeToGPA(grade.getLetterGrade());
            Double credits = getCourseCredits(grade.getCourse());

            if (gradePoints != null && credits != null) {
                totalPoints += gradePoints * credits;
                totalCredits += credits;
            }
        }

        return totalCredits > 0 ? totalPoints / totalCredits : 0.0;
    }

    /**
     * Calculate weighted GPA (honors/AP courses get boost)
     *
     * @param student The student
     * @return Weighted GPA
     */
    public Double calculateWeightedGPA(Student student) {
        List<StudentGrade> allGrades = studentGradeRepository.findByStudentOrderByGradeDateDesc(student);

        if (allGrades.isEmpty()) {
            return 0.0;
        }

        double totalPoints = 0.0;
        double totalCredits = 0.0;

        for (StudentGrade grade : allGrades) {
            Double gradePoints = convertLetterGradeToGPA(grade.getLetterGrade());
            Double credits = getCourseCredits(grade.getCourse());
            Double weightBonus = getCourseWeightBonus(grade.getCourse());

            if (gradePoints != null && credits != null) {
                totalPoints += (gradePoints + weightBonus) * credits;
                totalCredits += credits;
            }
        }

        return totalCredits > 0 ? totalPoints / totalCredits : 0.0;
    }

    /**
     * Calculate GPA for specific term
     *
     * @param student Student
     * @param academicYear Academic year (e.g., "2024-2025")
     * @param term Term (e.g., "Fall 2024")
     * @return Term GPA
     */
    public Double calculateTermGPA(Student student, String academicYear, String term) {
        // Use repository method if available
        List<StudentGrade> termGrades = studentGradeRepository
            .findByStudentOrderByGradeDateDesc(student).stream()
            .filter(g -> term.equals(g.getTerm()))
            .toList();

        if (termGrades.isEmpty()) {
            return 0.0;
        }

        double totalPoints = 0.0;
        double totalCredits = 0.0;

        for (StudentGrade grade : termGrades) {
            Double gradePoints = convertLetterGradeToGPA(grade.getLetterGrade());
            Double credits = getCourseCredits(grade.getCourse());

            if (gradePoints != null && credits != null) {
                totalPoints += gradePoints * credits;
                totalCredits += credits;
            }
        }

        return totalCredits > 0 ? totalPoints / totalCredits : 0.0;
    }

    /**
     * Convert letter grade to GPA points
     *
     * @param letterGrade Letter grade (A+, A, A-, B+, etc.)
     * @return GPA points (4.0 scale) or null if invalid
     */
    public Double convertLetterGradeToGPA(String letterGrade) {
        if (letterGrade == null || letterGrade.trim().isEmpty()) {
            return null;
        }

        Map<String, Double> gradeScale = new HashMap<>();
        gradeScale.put("A+", 4.0);
        gradeScale.put("A", 4.0);
        gradeScale.put("A-", 3.7);
        gradeScale.put("B+", 3.3);
        gradeScale.put("B", 3.0);
        gradeScale.put("B-", 2.7);
        gradeScale.put("C+", 2.3);
        gradeScale.put("C", 2.0);
        gradeScale.put("C-", 1.7);
        gradeScale.put("D+", 1.3);
        gradeScale.put("D", 1.0);
        gradeScale.put("D-", 0.7);
        gradeScale.put("F", 0.0);

        return gradeScale.getOrDefault(letterGrade.toUpperCase().trim(), null);
    }

    /**
     * Get course credits from the Course entity
     * Uses the credits field added December 12, 2025
     *
     * @param course The course
     * @return Credits for the course (defaults to 1.0 if null)
     */
    private Double getCourseCredits(Course course) {
        if (course == null) {
            return 1.0;
        }
        // Use the credits field from Course entity
        return course.getCredits() != null ? course.getCredits() : 1.0;
    }

    /**
     * Get weight bonus for honors/AP courses
     *
     * @param course The course
     * @return Weight bonus (0.0 for regular, 0.5 for honors, 1.0 for AP/IB)
     */
    private Double getCourseWeightBonus(Course course) {
        if (course == null || course.getCourseType() == null) {
            return 0.0;
        }

        switch (course.getCourseType()) {
            case AP:
            case IB:
                return 1.0; // +1.0 for AP/IB
            case HONORS:
                return 0.5; // +0.5 for Honors
            default:
                return 0.0;
        }
    }

    /**
     * Update student's GPA fields
     *
     * @param student The student
     */
    @Transactional
    public void updateStudentGPA(Student student) {
        Double cumulativeGPA = calculateCumulativeGPA(student);
        Double weightedGPA = calculateWeightedGPA(student);

        student.setCurrentGPA(cumulativeGPA);
        student.setUnweightedGPA(cumulativeGPA);
        student.setWeightedGPA(weightedGPA);

        log.info("Updated GPA for student {}: unweighted={}, weighted={}",
                student.getStudentId(), cumulativeGPA, weightedGPA);
    }

    /**
     * Calculate simple average GPA (no credit weighting)
     *
     * @param student The student
     * @return Simple average GPA
     */
    public Double calculateSimpleAverageGPA(Student student) {
        List<StudentGrade> allGrades = studentGradeRepository.findByStudentOrderByGradeDateDesc(student);

        if (allGrades.isEmpty()) {
            return 0.0;
        }

        double totalPoints = 0.0;
        int count = 0;

        for (StudentGrade grade : allGrades) {
            Double gradePoints = convertLetterGradeToGPA(grade.getLetterGrade());
            if (gradePoints != null) {
                totalPoints += gradePoints;
                count++;
            }
        }

        return count > 0 ? totalPoints / count : 0.0;
    }
}
