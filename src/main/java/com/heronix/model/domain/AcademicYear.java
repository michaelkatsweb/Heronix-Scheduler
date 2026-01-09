package com.heronix.model.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Represents an academic year in the school system.
 *
 * Tracks school years for automatic grade progression and historical records.
 *
 * @author Heronix Scheduler Team
 */
@Entity
@Table(name = "academic_years")
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String yearName;  // e.g., "2024-2025"

    @Column(nullable = false)
    private LocalDate startDate;  // First day of school year

    @Column(nullable = false)
    private LocalDate endDate;  // Last day of school year

    @Column
    private LocalDate graduationDate;  // Senior graduation date

    @Column(nullable = false)
    private boolean active = false;  // Is this the current year?

    @Column(nullable = false)
    private boolean graduated = false;  // Have seniors graduated?

    @Column
    private LocalDate progressionDate;  // When progression to next year occurred

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Constructors

    public AcademicYear() {
    }

    public AcademicYear(String yearName, LocalDate startDate, LocalDate endDate) {
        this.yearName = yearName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = false;
        this.graduated = false;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getYearName() {
        return yearName;
    }

    public void setYearName(String yearName) {
        this.yearName = yearName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getGraduationDate() {
        return graduationDate;
    }

    public void setGraduationDate(LocalDate graduationDate) {
        this.graduationDate = graduationDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isGraduated() {
        return graduated;
    }

    public void setGraduated(boolean graduated) {
        this.graduated = graduated;
    }

    public LocalDate getProgressionDate() {
        return progressionDate;
    }

    public void setProgressionDate(LocalDate progressionDate) {
        this.progressionDate = progressionDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Utility methods

    /**
     * Checks if this academic year is currently in session.
     *
     * @return true if today is between start and end dates
     */
    public boolean isInSession() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    /**
     * Checks if graduation date has passed.
     *
     * @return true if graduation date has occurred
     */
    public boolean hasGraduationPassed() {
        if (graduationDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(graduationDate);
    }

    /**
     * Gets the duration of the academic year in days.
     *
     * @return number of days from start to end
     */
    public long getDurationInDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }

    @Override
    public String toString() {
        return yearName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcademicYear that = (AcademicYear) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
