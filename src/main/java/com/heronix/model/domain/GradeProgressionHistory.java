package com.heronix.model.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Audit log of grade progression events.
 *
 * Records when grade progressions occur and their statistics for auditing.
 *
 * @author Heronix Scheduler Team
 */
@Entity
@Table(name = "grade_progression_history")
public class GradeProgressionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(nullable = false)
    private LocalDate progressionDate;

    @Column
    private Integer seniorsGraduated;

    @Column
    private Integer studentsPromoted;

    @Column
    private Integer enrollmentsArchived;

    @Column
    private Long performedByUserId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Constructors

    public GradeProgressionHistory() {
    }

    public GradeProgressionHistory(AcademicYear academicYear, LocalDate progressionDate) {
        this.academicYear = academicYear;
        this.progressionDate = progressionDate;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public LocalDate getProgressionDate() {
        return progressionDate;
    }

    public void setProgressionDate(LocalDate progressionDate) {
        this.progressionDate = progressionDate;
    }

    public Integer getSeniorsGraduated() {
        return seniorsGraduated;
    }

    public void setSeniorsGraduated(Integer seniorsGraduated) {
        this.seniorsGraduated = seniorsGraduated;
    }

    public Integer getStudentsPromoted() {
        return studentsPromoted;
    }

    public void setStudentsPromoted(Integer studentsPromoted) {
        this.studentsPromoted = studentsPromoted;
    }

    public Integer getEnrollmentsArchived() {
        return enrollmentsArchived;
    }

    public void setEnrollmentsArchived(Integer enrollmentsArchived) {
        this.enrollmentsArchived = enrollmentsArchived;
    }

    public Long getPerformedByUserId() {
        return performedByUserId;
    }

    public void setPerformedByUserId(Long performedByUserId) {
        this.performedByUserId = performedByUserId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "ProgressionHistory{" +
                "year=" + (academicYear != null ? academicYear.getYearName() : "null") +
                ", date=" + progressionDate +
                ", graduated=" + seniorsGraduated +
                ", promoted=" + studentsPromoted +
                '}';
    }
}
