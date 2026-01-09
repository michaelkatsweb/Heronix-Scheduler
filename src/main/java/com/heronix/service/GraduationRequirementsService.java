package com.heronix.service;

import com.heronix.model.domain.Student;
import java.util.List;
import java.util.Map;

/**
 * Graduation Requirements Service Interface
 *
 * Provides methods to:
 * - Assess student graduation readiness
 * - Calculate required vs earned credits
 * - Identify at-risk students
 * - Track academic standing
 *
 * Location: src/main/java/com/eduscheduler/service/GraduationRequirementsService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since November 2025
 */
public interface GraduationRequirementsService {

    /**
     * Assess if a student is on track for graduation
     * @param student The student to assess
     * @return true if on track, false otherwise
     */
    boolean isOnTrackForGraduation(Student student);

    /**
     * Get the number of credits required for a specific grade level
     * @param gradeLevel Grade level (9, 10, 11, 12)
     * @return Required credits
     */
    double getRequiredCreditsByGrade(String gradeLevel);

    /**
     * Get academic standing status for a student
     * @param student The student
     * @return "On Track", "At Risk", or "Retention Risk"
     */
    String getAcademicStandingStatus(Student student);

    /**
     * Get color code for academic standing (for UI)
     * @param student The student
     * @return Hex color code (#4CAF50, #FF9800, #f44336)
     */
    String getStandingColorCode(Student student);

    /**
     * Get background color for table row (light version)
     * @param student The student
     * @return Hex color code for background
     */
    String getStandingBackgroundColor(Student student);

    /**
     * Get icon/emoji for academic standing
     * @param student The student
     * @return "✅", "⚠️", or "❌"
     */
    String getStandingIcon(Student student);

    /**
     * Calculate credits behind schedule
     * @param student The student
     * @return Number of credits behind (0 if on track or ahead)
     */
    double getCreditsBehind(Student student);

    /**
     * Get detailed tooltip text for a student's academic standing
     * @param student The student
     * @return Multi-line tooltip text with details
     */
    String getStandingTooltip(Student student);

    /**
     * Get all students who are at risk (not on track)
     * @return List of at-risk students
     */
    List<Student> getAtRiskStudents();

    /**
     * Get students at retention risk (seriously behind)
     * @return List of students at retention risk
     */
    List<Student> getRetentionRiskStudents();

    /**
     * Get students who may not graduate (seniors not meeting requirements)
     * @return List of seniors at risk of not graduating
     */
    List<Student> getSeniorsNotMeetingRequirements();

    /**
     * Get summary statistics for all students
     * @return Map with counts: "onTrack", "atRisk", "retentionRisk", "total"
     */
    Map<String, Integer> getAcademicStandingSummary();

    /**
     * Check if student meets minimum GPA requirement
     * @param student The student
     * @return true if GPA >= 2.0, false otherwise
     */
    boolean meetsGPARequirement(Student student);

    /**
     * Get the numeric grade level (9-12) from string
     * @param gradeLevel Grade level string ("9", "10", "11", "12")
     * @return Numeric grade (9-12) or -1 if invalid
     */
    int getGradeNumber(String gradeLevel);

    /**
     * Update academic standing for a student
     * This will recalculate and update the academicStanding field
     * @param student The student to update
     * @return Updated student
     */
    Student updateAcademicStanding(Student student);

    /**
     * Update academic standing for all students
     * Batch operation to recalculate standing for entire student body
     * @return Number of students updated
     */
    int updateAllAcademicStandings();
}
