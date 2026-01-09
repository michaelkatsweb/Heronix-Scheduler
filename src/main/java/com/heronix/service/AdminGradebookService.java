package com.heronix.service;

import com.heronix.model.domain.GradeAuditLog;
import com.heronix.model.domain.StudentGrade;
import com.heronix.model.domain.User;
import com.heronix.repository.GradeAuditLogRepository;
import com.heronix.repository.StudentGradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin Gradebook Service
 *
 * Provides business logic for administrator grade management:
 * - Grade editing with audit trail
 * - Transfer student grade entry
 * - Bulk operations
 * - Audit log management
 * - Grade validation and integrity
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-28
 */
@Service
@Transactional
public class AdminGradebookService {

    private static final Logger logger = LoggerFactory.getLogger(AdminGradebookService.class);

    @Autowired
    private StudentGradeRepository studentGradeRepository;

    @Autowired
    private GradeAuditLogRepository gradeAuditLogRepository;

    // ========================================================================
    // GRADE EDITING WITH AUDIT TRAIL
    // ========================================================================

    /**
     * Update a grade with full audit trail
     *
     * @param grade The grade to update
     * @param fieldName Field being changed
     * @param oldValue Previous value
     * @param newValue New value
     * @param admin Administrator making the change
     * @param reason Reason for the change
     * @return Updated grade
     */
    public StudentGrade updateGradeWithAudit(StudentGrade grade, String fieldName,
                                            String oldValue, String newValue,
                                            User admin, String reason) {
        // Create audit log entry
        GradeAuditLog auditLog = GradeAuditLog.builder()
            .studentGrade(grade)
            .editedByAdmin(admin)
            .fieldName(fieldName)
            .oldValue(oldValue)
            .newValue(newValue)
            .reason(reason)
            .editType(GradeAuditLog.EditType.MANUAL)
            .timestamp(LocalDateTime.now())
            .build();

        gradeAuditLogRepository.save(auditLog);

        // Update the grade
        StudentGrade updatedGrade = studentGradeRepository.save(grade);

        logger.info("Grade updated by {}: {} for student {} in course {}",
            admin.getUsername(), fieldName, grade.getStudentName(), grade.getCourseName());

        return updatedGrade;
    }

    /**
     * Update multiple fields of a grade with audit
     */
    public StudentGrade updateGradeMultipleFields(StudentGrade grade,
                                                 Map<String, Object> changes,
                                                 User admin, String reason) {
        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String fieldName = entry.getKey();
            Object newValue = entry.getValue();
            String oldValue = getFieldValue(grade, fieldName);

            // Create audit log for this field
            GradeAuditLog auditLog = GradeAuditLog.builder()
                .studentGrade(grade)
                .editedByAdmin(admin)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue != null ? newValue.toString() : null)
                .reason(reason)
                .editType(GradeAuditLog.EditType.MANUAL)
                .build();

            gradeAuditLogRepository.save(auditLog);

            // Update the field
            setFieldValue(grade, fieldName, newValue);
        }

        return studentGradeRepository.save(grade);
    }

    /**
     * Add a transfer student grade with audit
     */
    public StudentGrade addTransferGrade(StudentGrade grade, User admin, String transferNotes) {
        // Save the grade
        StudentGrade savedGrade = studentGradeRepository.save(grade);

        // Create audit log
        GradeAuditLog auditLog = GradeAuditLog.builder()
            .studentGrade(savedGrade)
            .editedByAdmin(admin)
            .fieldName("transfer_entry")
            .oldValue(null)
            .newValue(grade.getLetterGrade())
            .reason("Transfer student grade entry: " + transferNotes)
            .editType(GradeAuditLog.EditType.TRANSFER)
            .build();

        gradeAuditLogRepository.save(auditLog);

        logger.info("Transfer grade added by {} for student {} in course {}",
            admin.getUsername(), grade.getStudentName(), grade.getCourseName());

        return savedGrade;
    }

    // ========================================================================
    // AUDIT LOG QUERIES
    // ========================================================================

    /**
     * Get audit log for a specific grade
     */
    public List<GradeAuditLog> getGradeHistory(StudentGrade grade) {
        return gradeAuditLogRepository.findByStudentGradeOrderByTimestampDesc(grade);
    }

    /**
     * Get recent audit logs
     */
    public List<GradeAuditLog> getRecentAuditLogs(int limit) {
        List<GradeAuditLog> all = gradeAuditLogRepository.findTop100ByOrderByTimestampDesc();
        return all.stream().limit(limit).toList();
    }

    /**
     * Get audit logs for a student (across all courses)
     */
    public List<GradeAuditLog> getStudentAuditHistory(Long studentId) {
        return gradeAuditLogRepository.findByStudentId(studentId);
    }

    /**
     * Get audit logs for a course
     */
    public List<GradeAuditLog> getCourseAuditHistory(Long courseId) {
        return gradeAuditLogRepository.findByCourseId(courseId);
    }

    /**
     * Get pending approval audit logs
     */
    public List<GradeAuditLog> getPendingApprovals() {
        return gradeAuditLogRepository.findPendingApprovals();
    }

    /**
     * Count recent edits
     */
    public long countRecentEdits(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return gradeAuditLogRepository.countRecentEdits(since);
    }

    // ========================================================================
    // GRADE VALIDATION
    // ========================================================================

    /**
     * Validate a grade before saving
     */
    public boolean validateGrade(StudentGrade grade, StringBuilder errors) {
        boolean isValid = true;

        if (grade.getStudent() == null) {
            errors.append("Student is required. ");
            isValid = false;
        }

        if (grade.getCourse() == null) {
            errors.append("Course is required. ");
            isValid = false;
        }

        if (grade.getLetterGrade() == null || grade.getLetterGrade().trim().isEmpty()) {
            errors.append("Letter grade is required. ");
            isValid = false;
        }

        if (grade.getGpaPoints() == null || grade.getGpaPoints() < 0 || grade.getGpaPoints() > 5.0) {
            errors.append("GPA points must be between 0.0 and 5.0. ");
            isValid = false;
        }

        if (grade.getCredits() == null || grade.getCredits() <= 0) {
            errors.append("Credits must be greater than 0. ");
            isValid = false;
        }

        if (grade.getNumericalGrade() != null &&
            (grade.getNumericalGrade() < 0 || grade.getNumericalGrade() > 100)) {
            errors.append("Numerical grade must be between 0 and 100. ");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Check for duplicate grades
     */
    public boolean isDuplicateGrade(StudentGrade grade) {
        List<StudentGrade> existing = studentGradeRepository.findByStudentAndCourse(
            grade.getStudent(), grade.getCourse());

        return existing.stream()
            .anyMatch(g -> !g.getId().equals(grade.getId()) &&
                          g.getTerm().equals(grade.getTerm()) &&
                          g.getAcademicYear().equals(grade.getAcademicYear()));
    }

    // ========================================================================
    // STATISTICS AND REPORTING
    // ========================================================================

    /**
     * Calculate average GPA for all students
     */
    public double calculateAverageGPA() {
        List<StudentGrade> allGrades = studentGradeRepository.findAll();

        return allGrades.stream()
            .filter(g -> g.getGpaPoints() != null && Boolean.TRUE.equals(g.getIncludeInGPA()))
            .mapToDouble(StudentGrade::getGpaPoints)
            .average()
            .orElse(0.0);
    }

    /**
     * Count failing grades
     */
    public long countFailingGrades() {
        List<StudentGrade> allGrades = studentGradeRepository.findAll();
        return allGrades.stream()
            .filter(g -> !g.isPassing())
            .count();
    }

    /**
     * Get grade distribution
     */
    public Map<String, Long> getGradeDistribution() {
        List<StudentGrade> allGrades = studentGradeRepository.findAll();

        return allGrades.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                StudentGrade::getLetterGrade,
                java.util.stream.Collectors.counting()
            ));
    }

    /**
     * Get audit log statistics
     */
    public Map<String, Object> getAuditStatistics() {
        List<Object[]> editTypeStats = gradeAuditLogRepository.getEditTypeStatistics();
        List<Object[]> activeAdmins = gradeAuditLogRepository.getMostActiveAdmins();

        return Map.of(
            "editTypeStatistics", editTypeStats,
            "mostActiveAdmins", activeAdmins,
            "totalAudits", gradeAuditLogRepository.count(),
            "pendingApprovals", getPendingApprovals().size()
        );
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get field value as string
     */
    private String getFieldValue(StudentGrade grade, String fieldName) {
        try {
            return switch (fieldName) {
                case "letterGrade" -> grade.getLetterGrade();
                case "numericalGrade" -> grade.getNumericalGrade() != null ?
                    grade.getNumericalGrade().toString() : null;
                case "gpaPoints" -> grade.getGpaPoints() != null ?
                    grade.getGpaPoints().toString() : null;
                case "credits" -> grade.getCredits() != null ?
                    grade.getCredits().toString() : null;
                case "comments" -> grade.getComments();
                default -> null;
            };
        } catch (Exception e) {
            logger.error("Error getting field value for {}", fieldName, e);
            return null;
        }
    }

    /**
     * Set field value
     */
    private void setFieldValue(StudentGrade grade, String fieldName, Object value) {
        try {
            switch (fieldName) {
                case "letterGrade" -> grade.setLetterGrade((String) value);
                case "numericalGrade" -> grade.setNumericalGrade((Double) value);
                case "gpaPoints" -> grade.setGpaPoints((Double) value);
                case "credits" -> grade.setCredits((Double) value);
                case "comments" -> grade.setComments((String) value);
            }
        } catch (Exception e) {
            logger.error("Error setting field value for {}", fieldName, e);
        }
    }
}
