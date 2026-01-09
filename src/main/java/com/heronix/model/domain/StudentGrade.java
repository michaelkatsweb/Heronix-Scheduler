package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Student Grade Entity
 * Location: src/main/java/com/eduscheduler/model/domain/StudentGrade.java
 *
 * Tracks individual course grades for students over time.
 * Used for GPA calculation, transcript generation, and academic history.
 *
 * Grade Scale:
 * - Letter Grades: A+, A, A-, B+, B, B-, C+, C, C-, D+, D, D-, F
 * - Percentage: 0-100
 * - GPA Points: 0.0-4.0 (unweighted), 0.0-5.0 (weighted for honors/AP)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-20
 */
@Entity
@Table(name = "student_grades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student who received this grade
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Course for this grade
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Academic term/semester (e.g., "Fall 2024", "Spring 2025", "Q1 2024-25")
     */
    @Column(name = "term", nullable = false)
    private String term;

    /**
     * Academic year (e.g., 2024)
     */
    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    /**
     * Letter grade (A+, A, A-, B+, B, B-, C+, C, C-, D+, D, D-, F, I, W, P)
     * I = Incomplete, W = Withdrawn, P = Pass (pass/fail courses)
     */
    @Column(name = "letter_grade", nullable = false, length = 3)
    private String letterGrade;

    /**
     * Numerical grade (percentage: 0-100)
     */
    @Column(name = "numerical_grade")
    private Double numericalGrade;

    /**
     * GPA points for this grade (0.0-4.0 unweighted, 0.0-5.0 weighted)
     */
    @Column(name = "gpa_points", nullable = false)
    private Double gpaPoints;

    /**
     * Course credits (typically 0.5 or 1.0)
     */
    @Column(name = "credits", nullable = false)
    private Double credits = 1.0;

    /**
     * Is this a weighted course (honors/AP)?
     */
    @Column(name = "is_weighted")
    private Boolean isWeighted = false;

    /**
     * Grade type (Final, Midterm, Quarter, Progress Report)
     */
    @Column(name = "grade_type")
    private String gradeType = "Final";

    /**
     * Date grade was assigned
     */
    @Column(name = "grade_date", nullable = false)
    private LocalDate gradeDate;

    /**
     * Date grade was entered into system
     */
    @Column(name = "entered_date")
    private LocalDate enteredDate = LocalDate.now();

    /**
     * Teacher who assigned the grade
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    /**
     * Is this grade final or can it be changed?
     */
    @Column(name = "is_final")
    private Boolean isFinal = false;

    /**
     * Comments from teacher
     */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    /**
     * Effort grade (1-5 or A-F, optional)
     */
    @Column(name = "effort_grade")
    private String effortGrade;

    /**
     * Conduct grade (1-5 or A-F, optional)
     */
    @Column(name = "conduct_grade")
    private String conductGrade;

    /**
     * Number of absences in this course
     */
    @Column(name = "absences")
    private Integer absences = 0;

    /**
     * Number of tardies in this course
     */
    @Column(name = "tardies")
    private Integer tardies = 0;

    /**
     * Include in GPA calculation?
     * Some courses (P/F, audit, etc.) may not count toward GPA
     */
    @Column(name = "include_in_gpa")
    private Boolean includeInGPA = true;

    /**
     * Include in transcript?
     */
    @Column(name = "include_in_transcript")
    private Boolean includeInTranscript = true;

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Check if student passed this course
     */
    public boolean isPassing() {
        if (letterGrade == null) return false;
        String grade = letterGrade.toUpperCase();
        return !grade.equals("F") && !grade.equals("I") && !grade.equals("W");
    }

    /**
     * Check if this is an honors-level grade
     */
    public boolean isHonorsGrade() {
        return isWeighted != null && isWeighted;
    }

    /**
     * Get display string for grade
     */
    public String getGradeDisplay() {
        if (numericalGrade != null) {
            return String.format("%s (%.1f%%)", letterGrade, numericalGrade);
        }
        return letterGrade;
    }

    /**
     * Get course name
     */
    public String getCourseName() {
        return course != null ? course.getCourseName() : "Unknown Course";
    }

    /**
     * Get student name
     */
    public String getStudentName() {
        return student != null ? student.getFullName() : "Unknown Student";
    }

    /**
     * Get weighted GPA points (for weighted GPA calculation)
     */
    public Double getWeightedGpaPoints() {
        if (isWeighted != null && isWeighted) {
            // Add 1.0 point for weighted courses
            return Math.min(gpaPoints + 1.0, 5.0);
        }
        return gpaPoints;
    }

    /**
     * Convert letter grade to GPA points (4.0 scale)
     */
    public static Double letterGradeToGpaPoints(String letterGrade) {
        if (letterGrade == null) return 0.0;

        switch (letterGrade.toUpperCase().trim()) {
            case "A+":
            case "A":
                return 4.0;
            case "A-":
                return 3.7;
            case "B+":
                return 3.3;
            case "B":
                return 3.0;
            case "B-":
                return 2.7;
            case "C+":
                return 2.3;
            case "C":
                return 2.0;
            case "C-":
                return 1.7;
            case "D+":
                return 1.3;
            case "D":
                return 1.0;
            case "D-":
                return 0.7;
            case "F":
            case "I":
            case "W":
                return 0.0;
            case "P": // Pass (not counted in GPA)
                return null;
            default:
                return 0.0;
        }
    }

    /**
     * Convert numerical grade to letter grade
     */
    public static String numericalGradeToLetterGrade(double numerical) {
        if (numerical >= 97) return "A+";
        if (numerical >= 93) return "A";
        if (numerical >= 90) return "A-";
        if (numerical >= 87) return "B+";
        if (numerical >= 83) return "B";
        if (numerical >= 80) return "B-";
        if (numerical >= 77) return "C+";
        if (numerical >= 73) return "C";
        if (numerical >= 70) return "C-";
        if (numerical >= 67) return "D+";
        if (numerical >= 63) return "D";
        if (numerical >= 60) return "D-";
        return "F";
    }

    /**
     * Validate grade data
     */
    public boolean isValid() {
        if (student == null || course == null) return false;
        if (letterGrade == null || letterGrade.trim().isEmpty()) return false;
        if (gpaPoints == null || gpaPoints < 0 || gpaPoints > 5.0) return false;
        if (credits == null || credits <= 0) return false;
        if (numericalGrade != null && (numericalGrade < 0 || numericalGrade > 100)) return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("Grade: %s in %s - %s (%s)",
            getStudentName(), getCourseName(), letterGrade, term);
    }
}
