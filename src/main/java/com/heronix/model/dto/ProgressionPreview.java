package com.heronix.model.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Preview of what will happen during grade progression.
 *
 * Shows counts before executing actual progression.
 *
 * @author Heronix Scheduler Team
 */
public class ProgressionPreview {

    private int grade9Count;
    private int grade10Count;
    private int grade11Count;
    private int grade12Count;
    private int totalEnrollments;
    private Map<String, Integer> gradeDistribution = new HashMap<>();

    public ProgressionPreview() {
    }

    // Getters and Setters

    public int getGrade9Count() {
        return grade9Count;
    }

    public void setGrade9Count(int grade9Count) {
        this.grade9Count = grade9Count;
    }

    public int getGrade10Count() {
        return grade10Count;
    }

    public void setGrade10Count(int grade10Count) {
        this.grade10Count = grade10Count;
    }

    public int getGrade11Count() {
        return grade11Count;
    }

    public void setGrade11Count(int grade11Count) {
        this.grade11Count = grade11Count;
    }

    public int getGrade12Count() {
        return grade12Count;
    }

    public void setGrade12Count(int grade12Count) {
        this.grade12Count = grade12Count;
    }

    public int getTotalEnrollments() {
        return totalEnrollments;
    }

    public void setTotalEnrollments(int totalEnrollments) {
        this.totalEnrollments = totalEnrollments;
    }

    public Map<String, Integer> getGradeDistribution() {
        return gradeDistribution;
    }

    public void setGradeDistribution(Map<String, Integer> gradeDistribution) {
        this.gradeDistribution = gradeDistribution;
    }

    /**
     * Gets total students that will be promoted.
     *
     * @return count of students in grades 9-11
     */
    public int getStudentsToPromote() {
        return grade9Count + grade10Count + grade11Count;
    }

    /**
     * Gets total seniors that will graduate.
     *
     * @return count of grade 12 students
     */
    public int getSeniorsToGraduate() {
        return grade12Count;
    }

    @Override
    public String toString() {
        return "ProgressionPreview{" +
                "grade9=" + grade9Count +
                ", grade10=" + grade10Count +
                ", grade11=" + grade11Count +
                ", grade12=" + grade12Count +
                ", enrollments=" + totalEnrollments +
                '}';
    }
}
