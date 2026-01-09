package com.heronix.service.impl;

import com.heronix.model.domain.Student;
import com.heronix.repository.StudentRepository;
import com.heronix.service.GraduationRequirementsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Graduation Requirements Service Implementation
 *
 * Provides methods to:
 * - Assess student graduation readiness
 * - Calculate required vs earned credits
 * - Identify at-risk students
 * - Track academic standing
 *
 * Location: src/main/java/com/eduscheduler/service/impl/GraduationRequirementsServiceImpl.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since November 2025
 */
@Slf4j
@Service
@Transactional
public class GraduationRequirementsServiceImpl implements GraduationRequirementsService {

    @Autowired
    private StudentRepository studentRepository;

    // Florida graduation requirements
    private static final double TOTAL_CREDITS_REQUIRED = 24.0;
    private static final double MIN_GPA_REQUIRED = 2.0;
    private static final double AT_RISK_GPA_THRESHOLD = 1.5;

    /**
     * Assess if a student is on track for graduation
     * @param student The student to assess
     * @return true if on track, false otherwise
     */
    @Override
    public boolean isOnTrackForGraduation(Student student) {
        if (student == null) return false;

        double creditsEarned = student.getCreditsEarned() != null ? student.getCreditsEarned() : 0.0;
        double creditsRequired = getRequiredCreditsByGrade(student.getGradeLevel());
        Double gpa = student.getCurrentGPA();
        double currentGPA = gpa != null ? gpa : 0.0;

        return creditsEarned >= creditsRequired && currentGPA >= MIN_GPA_REQUIRED;
    }

    /**
     * Get the number of credits required for a specific grade level
     * Florida standard progression:
     * - Grade 9: 6+ credits
     * - Grade 10: 12+ credits
     * - Grade 11: 18+ credits
     * - Grade 12: 24+ credits (graduation)
     *
     * @param gradeLevel Grade level (9, 10, 11, 12)
     * @return Required credits
     */
    @Override
    public double getRequiredCreditsByGrade(String gradeLevel) {
        if (gradeLevel == null) return 0.0;

        return switch (gradeLevel.trim()) {
            case "9" -> 6.0;
            case "10" -> 12.0;
            case "11" -> 18.0;
            case "12" -> 24.0;
            default -> 0.0; // K-8 or invalid
        };
    }

    /**
     * Get academic standing status for a student
     *
     * Categories:
     * - "On Track": Credits >= required AND GPA >= 2.0
     * - "At Risk": 1-2 credits behind OR GPA 1.5-1.99
     * - "Retention Risk": 3+ credits behind OR GPA < 1.5
     *
     * @param student The student
     * @return "On Track", "At Risk", or "Retention Risk"
     */
    @Override
    public String getAcademicStandingStatus(Student student) {
        if (student == null) return "Unknown";

        double creditsBehind = getCreditsBehind(student);
        Double gpa = student.getCurrentGPA();
        double currentGPA = gpa != null ? gpa : 0.0;

        // Retention Risk: 3+ credits behind OR GPA < 1.5
        if (creditsBehind >= 3.0 || currentGPA < AT_RISK_GPA_THRESHOLD) {
            return "Retention Risk";
        }

        // At Risk: 1-2 credits behind OR GPA 1.5-1.99
        if (creditsBehind >= 1.0 || (currentGPA >= AT_RISK_GPA_THRESHOLD && currentGPA < MIN_GPA_REQUIRED)) {
            return "At Risk";
        }

        // On Track: Meeting requirements
        return "On Track";
    }

    /**
     * Get color code for academic standing (for UI)
     * @param student The student
     * @return Hex color code (#4CAF50, #FF9800, #f44336)
     */
    @Override
    public String getStandingColorCode(Student student) {
        String status = getAcademicStandingStatus(student);
        return switch (status) {
            case "On Track" -> "#4CAF50";        // Green
            case "At Risk" -> "#FF9800";         // Orange
            case "Retention Risk" -> "#f44336";  // Red
            default -> "#9E9E9E";                // Gray (unknown)
        };
    }

    /**
     * Get background color for table row (light version)
     * @param student The student
     * @return Hex color code for background
     */
    @Override
    public String getStandingBackgroundColor(Student student) {
        String status = getAcademicStandingStatus(student);
        return switch (status) {
            case "On Track" -> "#E8F5E9";        // Light green
            case "At Risk" -> "#FFF3E0";         // Light orange
            case "Retention Risk" -> "#FFEBEE";  // Light red
            default -> "#F5F5F5";                // Light gray
        };
    }

    /**
     * Get icon/emoji for academic standing
     * @param student The student
     * @return "✅", "⚠️", or "❌"
     */
    @Override
    public String getStandingIcon(Student student) {
        String status = getAcademicStandingStatus(student);
        return switch (status) {
            case "On Track" -> "✅";
            case "At Risk" -> "⚠️";
            case "Retention Risk" -> "❌";
            default -> "❓";
        };
    }

    /**
     * Calculate credits behind schedule
     * @param student The student
     * @return Number of credits behind (0 if on track or ahead)
     */
    @Override
    public double getCreditsBehind(Student student) {
        if (student == null) return 0.0;

        double creditsEarned = student.getCreditsEarned() != null ? student.getCreditsEarned() : 0.0;
        double creditsRequired = getRequiredCreditsByGrade(student.getGradeLevel());

        double behind = creditsRequired - creditsEarned;
        return Math.max(0.0, behind); // Never negative
    }

    /**
     * Get detailed tooltip text for a student's academic standing
     * @param student The student
     * @return Multi-line tooltip text with details
     */
    @Override
    public String getStandingTooltip(Student student) {
        if (student == null) return "No data available";

        double creditsEarned = student.getCreditsEarned() != null ? student.getCreditsEarned() : 0.0;
        double creditsRequired = getRequiredCreditsByGrade(student.getGradeLevel());
        double creditsBehind = getCreditsBehind(student);
        Double gpa = student.getCurrentGPA();
        double currentGPA = gpa != null ? gpa : 0.0;
        String status = getAcademicStandingStatus(student);

        StringBuilder tooltip = new StringBuilder();
        tooltip.append(String.format("Academic Standing: %s\n", status));
        tooltip.append(String.format("\nCredits Earned: %.1f / %.1f required\n", creditsEarned, creditsRequired));

        if (creditsBehind > 0) {
            tooltip.append(String.format("Credits Behind: %.1f\n", creditsBehind));
        }

        tooltip.append(String.format("Current GPA: %.2f (min 2.0 required)\n", currentGPA));

        // Add recommendations based on status
        if (status.equals("Retention Risk")) {
            tooltip.append("\n⚠️ CRITICAL: Immediate intervention required!");
            tooltip.append("\n- Schedule counselor meeting");
            tooltip.append("\n- Consider credit recovery program");
            tooltip.append("\n- Notify parents/guardians");
        } else if (status.equals("At Risk")) {
            tooltip.append("\n⚠️ WARNING: Student needs support");
            tooltip.append("\n- Monitor progress closely");
            tooltip.append("\n- Provide tutoring resources");
            tooltip.append("\n- Check attendance patterns");
        } else {
            tooltip.append("\n✅ Student is on track for graduation");
        }

        return tooltip.toString();
    }

    /**
     * Get all students who are at risk (not on track)
     * @return List of at-risk students
     */
    @Override
    public List<Student> getAtRiskStudents() {
        List<Student> allStudents = studentRepository.findAll();
        // ✅ NULL SAFE: Filter null students before checking status
        return allStudents.stream()
                .filter(student -> student != null)
                .filter(student -> {
                    String status = getAcademicStandingStatus(student);
                    return "At Risk".equals(status);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get students at retention risk (seriously behind)
     * @return List of students at retention risk
     */
    @Override
    public List<Student> getRetentionRiskStudents() {
        List<Student> allStudents = studentRepository.findAll();
        // ✅ NULL SAFE: Filter null students before checking status
        return allStudents.stream()
                .filter(student -> student != null)
                .filter(student -> {
                    String status = getAcademicStandingStatus(student);
                    return "Retention Risk".equals(status);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get students who may not graduate (seniors not meeting requirements)
     * @return List of seniors at risk of not graduating
     */
    @Override
    public List<Student> getSeniorsNotMeetingRequirements() {
        List<Student> allStudents = studentRepository.findAll();
        // ✅ NULL SAFE: Filter null students before checking grade level
        return allStudents.stream()
                .filter(student -> student != null && student.getGradeLevel() != null)
                .filter(student -> {
                    String gradeLevel = student.getGradeLevel();
                    return "12".equals(gradeLevel) && !isOnTrackForGraduation(student);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get summary statistics for all students
     * @return Map with counts: "onTrack", "atRisk", "retentionRisk", "total"
     */
    @Override
    public Map<String, Integer> getAcademicStandingSummary() {
        List<Student> allStudents = studentRepository.findAll();

        Map<String, Integer> summary = new HashMap<>();
        int onTrack = 0;
        int atRisk = 0;
        int retentionRisk = 0;

        for (Student student : allStudents) {
            // ✅ NULL SAFE: Skip null students
            if (student == null) continue;

            String status = getAcademicStandingStatus(student);
            switch (status) {
                case "On Track" -> onTrack++;
                case "At Risk" -> atRisk++;
                case "Retention Risk" -> retentionRisk++;
            }
        }

        summary.put("onTrack", onTrack);
        summary.put("atRisk", atRisk);
        summary.put("retentionRisk", retentionRisk);
        summary.put("total", allStudents.size());

        return summary;
    }

    /**
     * Check if student meets minimum GPA requirement
     * @param student The student
     * @return true if GPA >= 2.0, false otherwise
     */
    @Override
    public boolean meetsGPARequirement(Student student) {
        if (student == null) return false;
        Double gpa = student.getCurrentGPA();
        double currentGPA = gpa != null ? gpa : 0.0;
        return currentGPA >= MIN_GPA_REQUIRED;
    }

    /**
     * Get the numeric grade level (9-12) from string
     * @param gradeLevel Grade level string ("9", "10", "11", "12")
     * @return Numeric grade (9-12) or -1 if invalid
     */
    @Override
    public int getGradeNumber(String gradeLevel) {
        if (gradeLevel == null) return -1;

        try {
            int grade = Integer.parseInt(gradeLevel.trim());
            return (grade >= 9 && grade <= 12) ? grade : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Update academic standing for a student
     * This will recalculate and update the academicStanding field
     * @param student The student to update
     * @return Updated student
     */
    @Override
    @Transactional
    public Student updateAcademicStanding(Student student) {
        if (student == null) return null;

        String newStanding = getAcademicStandingStatus(student);
        student.setAcademicStanding(newStanding);

        return studentRepository.save(student);
    }

    /**
     * Update academic standing for all students
     * Batch operation to recalculate standing for entire student body
     * @return Number of students updated
     */
    @Override
    @Transactional
    public int updateAllAcademicStandings() {
        List<Student> allStudents = studentRepository.findAll();
        int updated = 0;

        for (Student student : allStudents) {
            // ✅ NULL SAFE: Skip null students
            if (student == null) continue;

            String newStanding = getAcademicStandingStatus(student);
            // ✅ NULL SAFE: Safe comparison with existing standing
            String currentStanding = student.getAcademicStanding();
            if (!newStanding.equals(currentStanding)) {
                student.setAcademicStanding(newStanding);
                studentRepository.save(student);
                updated++;
            }
        }

        log.info("Updated academic standing for {} students", updated);
        return updated;
    }
}
