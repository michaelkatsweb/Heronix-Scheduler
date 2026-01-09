package com.heronix.model.dto;

/**
 * Result of a grade progression operation.
 *
 * Contains statistics about what was changed during progression.
 *
 * @author Heronix Scheduler Team
 */
public class ProgressionResult {

    private boolean successful;
    private String message;
    private int seniorsGraduated;
    private int studentsPromoted;
    private int enrollmentsArchived;
    private Long newAcademicYearId;
    private String newYearName;

    public ProgressionResult() {
    }

    public ProgressionResult(boolean successful, String message) {
        this.successful = successful;
        this.message = message;
    }

    // Getters and Setters

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getSeniorsGraduated() {
        return seniorsGraduated;
    }

    public void setSeniorsGraduated(int seniorsGraduated) {
        this.seniorsGraduated = seniorsGraduated;
    }

    public int getStudentsPromoted() {
        return studentsPromoted;
    }

    public void setStudentsPromoted(int studentsPromoted) {
        this.studentsPromoted = studentsPromoted;
    }

    public int getEnrollmentsArchived() {
        return enrollmentsArchived;
    }

    public void setEnrollmentsArchived(int enrollmentsArchived) {
        this.enrollmentsArchived = enrollmentsArchived;
    }

    public Long getNewAcademicYearId() {
        return newAcademicYearId;
    }

    public void setNewAcademicYearId(Long newAcademicYearId) {
        this.newAcademicYearId = newAcademicYearId;
    }

    public String getNewYearName() {
        return newYearName;
    }

    public void setNewYearName(String newYearName) {
        this.newYearName = newYearName;
    }

    @Override
    public String toString() {
        return "ProgressionResult{" +
                "successful=" + successful +
                ", graduated=" + seniorsGraduated +
                ", promoted=" + studentsPromoted +
                ", archived=" + enrollmentsArchived +
                ", newYear='" + newYearName + '\'' +
                '}';
    }
}
