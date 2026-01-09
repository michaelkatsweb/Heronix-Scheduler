package com.heronix.model.dto;

import java.util.List;

/**
 * Criteria for searching and filtering students.
 *
 * Used by BulkEnrollmentService to find specific groups of students.
 *
 * @author Heronix Scheduler Team
 */
public class StudentSearchCriteria {

    // Basic filters
    private Integer gradeLevel;              // Filter by grade (9, 10, 11, 12)
    private Boolean hasIEP;                  // Only IEP students
    private Boolean has504;                  // Only 504 plan students
    private String namePattern;              // Search by name
    private String studentIdPattern;         // Search by ID
    private Boolean active;                  // Active students only

    // Advanced filters
    private Integer minCoursesEnrolled;      // Students with >= X courses
    private Integer maxCoursesEnrolled;      // Students with <= X courses
    private List<String> mustHaveCourses;    // Students enrolled in these courses
    private List<String> mustNotHaveCourses; // Students NOT in these courses
    private Boolean missingRequiredCourses;  // Students missing core courses
    private String prerequisiteFor;          // Students who completed prerequisite

    // Graduation filters
    private Boolean graduated;               // Only graduated students
    private Integer graduationYear;          // Expected graduation year

    public StudentSearchCriteria() {
    }

    // Getters and Setters

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public Boolean getHasIEP() {
        return hasIEP;
    }

    public void setHasIEP(Boolean hasIEP) {
        this.hasIEP = hasIEP;
    }

    public Boolean getHas504() {
        return has504;
    }

    public void setHas504(Boolean has504) {
        this.has504 = has504;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }

    public String getStudentIdPattern() {
        return studentIdPattern;
    }

    public void setStudentIdPattern(String studentIdPattern) {
        this.studentIdPattern = studentIdPattern;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getMinCoursesEnrolled() {
        return minCoursesEnrolled;
    }

    public void setMinCoursesEnrolled(Integer minCoursesEnrolled) {
        this.minCoursesEnrolled = minCoursesEnrolled;
    }

    public Integer getMaxCoursesEnrolled() {
        return maxCoursesEnrolled;
    }

    public void setMaxCoursesEnrolled(Integer maxCoursesEnrolled) {
        this.maxCoursesEnrolled = maxCoursesEnrolled;
    }

    public List<String> getMustHaveCourses() {
        return mustHaveCourses;
    }

    public void setMustHaveCourses(List<String> mustHaveCourses) {
        this.mustHaveCourses = mustHaveCourses;
    }

    public List<String> getMustNotHaveCourses() {
        return mustNotHaveCourses;
    }

    public void setMustNotHaveCourses(List<String> mustNotHaveCourses) {
        this.mustNotHaveCourses = mustNotHaveCourses;
    }

    public Boolean getMissingRequiredCourses() {
        return missingRequiredCourses;
    }

    public void setMissingRequiredCourses(Boolean missingRequiredCourses) {
        this.missingRequiredCourses = missingRequiredCourses;
    }

    public String getPrerequisiteFor() {
        return prerequisiteFor;
    }

    public void setPrerequisiteFor(String prerequisiteFor) {
        this.prerequisiteFor = prerequisiteFor;
    }

    public Boolean getGraduated() {
        return graduated;
    }

    public void setGraduated(Boolean graduated) {
        this.graduated = graduated;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    @Override
    public String toString() {
        return "StudentSearchCriteria{" +
                "gradeLevel=" + gradeLevel +
                ", hasIEP=" + hasIEP +
                ", has504=" + has504 +
                ", namePattern='" + namePattern + '\'' +
                ", minCourses=" + minCoursesEnrolled +
                ", maxCourses=" + maxCoursesEnrolled +
                '}';
    }
}
