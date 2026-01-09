package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Transcript Record Entity
 * Stores academic records for transcript generation and GPA calculation.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 16 - Transcript System
 */
@Entity
@Table(name = "transcript_records", indexes = {
    @Index(name = "idx_transcript_student", columnList = "student_id"),
    @Index(name = "idx_transcript_academic_year", columnList = "academic_year, semester")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "academic_year", nullable = false)
    private String academicYear; // e.g., "2024-2025"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Semester semester = Semester.FALL;

    @Column(name = "grade_level")
    private Integer gradeLevel; // 9, 10, 11, 12

    @Column(name = "letter_grade", length = 2)
    private String letterGrade; // A, B+, C, etc.

    @Column(name = "numeric_grade", precision = 5, scale = 2)
    private BigDecimal numericGrade; // 0-100

    @Column(name = "grade_points", precision = 3, scale = 2)
    private BigDecimal gradePoints; // 0.0-4.0 (or 5.0 for weighted)

    @Column(name = "credits_attempted", precision = 4, scale = 2)
    private BigDecimal creditsAttempted;

    @Column(name = "credits_earned", precision = 4, scale = 2)
    private BigDecimal creditsEarned;

    @Column(name = "weighted")
    @Builder.Default
    private Boolean weighted = false; // Honors/AP weighting

    @Column(name = "weight_factor", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal weightFactor = BigDecimal.ONE; // 1.0 regular, 1.1 honors, 1.2 AP

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type")
    @Builder.Default
    private CourseType courseType = CourseType.REGULAR;

    @Column(name = "teacher_name")
    private String teacherName;

    @Column(name = "final_exam_score", precision = 5, scale = 2)
    private BigDecimal finalExamScore;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "include_in_gpa")
    @Builder.Default
    private Boolean includeInGpa = true;

    @Column(name = "transfer_credit")
    @Builder.Default
    private Boolean transferCredit = false;

    @Column(name = "retake")
    @Builder.Default
    private Boolean retake = false;

    @Column(name = "original_record_id")
    private Long originalRecordId; // For retakes

    public enum Semester {
        FALL,
        SPRING,
        SUMMER,
        FULL_YEAR
    }

    public enum CourseType {
        REGULAR,
        HONORS,
        AP,
        IB,
        DUAL_CREDIT,
        REMEDIAL,
        ELECTIVE
    }

    // GPA calculation helpers
    @Transient
    public BigDecimal getWeightedGradePoints() {
        if (gradePoints == null || weightFactor == null) return BigDecimal.ZERO;
        return gradePoints.multiply(weightFactor).setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public BigDecimal getQualityPoints() {
        if (gradePoints == null || creditsAttempted == null) return BigDecimal.ZERO;
        return gradePoints.multiply(creditsAttempted).setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public BigDecimal getWeightedQualityPoints() {
        return getWeightedGradePoints().multiply(
            creditsAttempted != null ? creditsAttempted : BigDecimal.ZERO
        ).setScale(2, RoundingMode.HALF_UP);
    }

    @Transient
    public boolean isPassing() {
        if (numericGrade != null) {
            return numericGrade.compareTo(new BigDecimal("60")) >= 0;
        }
        if (letterGrade != null) {
            return !letterGrade.equalsIgnoreCase("F") &&
                   !letterGrade.equalsIgnoreCase("I") &&
                   !letterGrade.equalsIgnoreCase("W");
        }
        return false;
    }

    // Convert letter grade to grade points
    public static BigDecimal letterToGradePoints(String letter) {
        if (letter == null) return BigDecimal.ZERO;
        return switch (letter.toUpperCase()) {
            case "A+", "A" -> new BigDecimal("4.0");
            case "A-" -> new BigDecimal("3.7");
            case "B+" -> new BigDecimal("3.3");
            case "B" -> new BigDecimal("3.0");
            case "B-" -> new BigDecimal("2.7");
            case "C+" -> new BigDecimal("2.3");
            case "C" -> new BigDecimal("2.0");
            case "C-" -> new BigDecimal("1.7");
            case "D+" -> new BigDecimal("1.3");
            case "D" -> new BigDecimal("1.0");
            case "D-" -> new BigDecimal("0.7");
            default -> BigDecimal.ZERO;
        };
    }
}
