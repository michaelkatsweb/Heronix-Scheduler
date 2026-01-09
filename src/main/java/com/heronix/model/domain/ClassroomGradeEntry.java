package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Classroom Grade Entry Entity
 * Tracks individual assignment grades entered by teachers for daily/weekly monitoring.
 * This is distinct from final grades - it captures all formative and summative assessments.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - Multi-Level Monitoring and Reporting
 */
@Entity
@Table(name = "classroom_grade_entries", indexes = {
    @Index(name = "idx_grade_entry_student_date", columnList = "student_id, assignment_date"),
    @Index(name = "idx_grade_entry_course_date", columnList = "course_id, assignment_date"),
    @Index(name = "idx_grade_entry_missing_work", columnList = "is_missing_work, assignment_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomGradeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "assignment_name", nullable = false, length = 255)
    private String assignmentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false)
    @Builder.Default
    private AssignmentType assignmentType = AssignmentType.HOMEWORK;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    @Column(name = "points_earned")
    private Double pointsEarned;

    @Column(name = "points_possible", nullable = false)
    private Double pointsPossible;

    @Column(name = "percentage_grade")
    private Double percentageGrade;

    @Column(name = "letter_grade", length = 2)
    private String letterGrade;

    @Column(name = "is_benchmark_assessment", nullable = false)
    @Builder.Default
    private Boolean isBenchmarkAssessment = false;

    @Column(name = "is_missing_work", nullable = false)
    @Builder.Default
    private Boolean isMissingWork = false;

    @Column(name = "teacher_notes", columnDefinition = "TEXT")
    private String teacherNotes;

    @Column(name = "entry_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime entryTimestamp = LocalDateTime.now();

    @Column(name = "last_modified_timestamp")
    private LocalDateTime lastModifiedTimestamp;

    @Column(name = "entered_by_staff_id", nullable = false)
    private Long enteredByStaffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    // ========================================================================
    // ENUMS
    // ========================================================================

    public enum AssignmentType {
        QUIZ,
        TEST,
        PROJECT,
        HOMEWORK,
        PARTICIPATION,
        BENCHMARK,
        CLASSWORK,
        LAB,
        PRESENTATION,
        ESSAY
    }

    // ========================================================================
    // CALCULATED FIELDS
    // ========================================================================

    @PrePersist
    @PreUpdate
    public void calculateGrade() {
        if (pointsEarned != null && pointsPossible != null && pointsPossible > 0) {
            percentageGrade = (pointsEarned / pointsPossible) * 100.0;
            letterGrade = calculateLetterGrade(percentageGrade);
        }

        if (lastModifiedTimestamp == null) {
            lastModifiedTimestamp = LocalDateTime.now();
        }
    }

    private String calculateLetterGrade(double percentage) {
        if (percentage >= 90) return "A";
        if (percentage >= 80) return "B";
        if (percentage >= 70) return "C";
        if (percentage >= 60) return "D";
        return "F";
    }

    @Transient
    public boolean isPassing() {
        return percentageGrade != null && percentageGrade >= 60.0;
    }

    @Transient
    public boolean isFailing() {
        return percentageGrade != null && percentageGrade < 60.0;
    }
}
