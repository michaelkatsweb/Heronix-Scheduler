package com.heronix.service;

import com.heronix.dto.SISExportResult;

import java.util.List;

/**
 * Service for exporting schedules to various Student Information Systems (SIS)
 *
 * Location: src/main/java/com/eduscheduler/service/SISExportService.java
 */
public interface SISExportService {

    /**
     * Supported SIS export formats
     */
    enum ExportFormat {
        POWERSCHOOL("PowerSchool", "csv"),
        SKYWARD("Skyward", "txt"),
        INFINITE_CAMPUS("Infinite Campus", "csv"),
        GENERIC_CSV("Generic CSV", "csv"),
        EXCEL("Microsoft Excel", "xlsx");

        private final String displayName;
        private final String fileExtension;

        ExportFormat(String displayName, String fileExtension) {
            this.displayName = displayName;
            this.fileExtension = fileExtension;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getFileExtension() {
            return fileExtension;
        }
    }

    /**
     * Export types
     */
    enum ExportType {
        MASTER_SCHEDULE,      // Full master schedule
        STUDENT_SCHEDULES,    // Individual student schedules
        TEACHER_SCHEDULES,    // Teacher assignments
        ROOM_SCHEDULES,       // Room usage
        COURSE_SECTIONS,      // Section details
        ENROLLMENTS          // Student enrollments
    }

    // ========== Export Operations ==========

    /**
     * Export master schedule to specified format
     */
    SISExportResult exportMasterSchedule(ExportFormat format, String outputPath);

    /**
     * Export student schedules to specified format
     */
    SISExportResult exportStudentSchedules(ExportFormat format, String outputPath, List<Long> studentIds);

    /**
     * Export all student schedules
     */
    SISExportResult exportAllStudentSchedules(ExportFormat format, String outputPath);

    /**
     * Export teacher schedules
     */
    SISExportResult exportTeacherSchedules(ExportFormat format, String outputPath, List<Long> teacherIds);

    /**
     * Export course sections
     */
    SISExportResult exportCourseSections(ExportFormat format, String outputPath);

    /**
     * Export student enrollments
     */
    SISExportResult exportEnrollments(ExportFormat format, String outputPath);

    // ========== PowerSchool-Specific Exports ==========

    /**
     * Export to PowerSchool format (CSV with specific column order)
     */
    SISExportResult exportToPowerSchool(ExportType exportType, String outputPath);

    /**
     * Export PowerSchool student schedule import file
     */
    SISExportResult exportPowerSchoolStudentSchedules(String outputPath);

    /**
     * Export PowerSchool section setup file
     */
    SISExportResult exportPowerSchoolSections(String outputPath);

    // ========== Skyward-Specific Exports ==========

    /**
     * Export to Skyward format (Tab-delimited text)
     */
    SISExportResult exportToSkyward(ExportType exportType, String outputPath);

    /**
     * Export Skyward student schedule file
     */
    SISExportResult exportSkywardStudentSchedules(String outputPath);

    /**
     * Export Skyward course section file
     */
    SISExportResult exportSkywardSections(String outputPath);

    // ========== Infinite Campus-Specific Exports ==========

    /**
     * Export to Infinite Campus format
     */
    SISExportResult exportToInfiniteCampus(ExportType exportType, String outputPath);

    /**
     * Export Infinite Campus student roster
     */
    SISExportResult exportInfiniteCampusRoster(String outputPath);

    /**
     * Export Infinite Campus section details
     */
    SISExportResult exportInfiniteCampusSections(String outputPath);

    // ========== Validation ==========

    /**
     * Validate data before export
     */
    List<String> validateExportData(ExportType exportType);

    /**
     * Check if export format is supported
     */
    boolean isFormatSupported(ExportFormat format);

    /**
     * Get list of supported formats
     */
    List<ExportFormat> getSupportedFormats();

    // ========== Utility Methods ==========

    /**
     * Get default export path for format
     */
    String getDefaultExportPath(ExportFormat format);

    /**
     * Generate export filename with timestamp
     */
    String generateExportFilename(ExportFormat format, ExportType exportType);

    /**
     * Preview export data (first 10 rows)
     */
    String previewExportData(ExportFormat format, ExportType exportType);
}
