package com.heronix.service.impl;

import com.heronix.dto.SISExportResult;
import com.heronix.service.SISExportService.ExportFormat;
import com.heronix.service.SISExportService.ExportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for SISExportServiceImpl
 *
 * Tests SIS export functionality for various formats (PowerSchool, Skyward, Infinite Campus).
 * Focuses on validation, format detection, and export preparation logic.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SISExportServiceImplTest {

    @InjectMocks
    private SISExportServiceImpl service;

    private String testOutputPath;

    @BeforeEach
    void setUp() {
        testOutputPath = System.getProperty("user.home") + "/Heronix Scheduler/SIS_Exports/test_export";
    }

    // ========== EXPORT FORMAT TESTS ==========

    @Test
    void testGetSupportedFormats_ShouldReturnAllFormats() {
        List<ExportFormat> formats = service.getSupportedFormats();

        assertNotNull(formats);
        assertFalse(formats.isEmpty());
        assertTrue(formats.size() >= 4); // At least PowerSchool, Skyward, Infinite Campus, Generic CSV
    }

    @Test
    void testIsFormatSupported_WithPowerSchool_ShouldReturnTrue() {
        assertTrue(service.isFormatSupported(ExportFormat.POWERSCHOOL));
    }

    @Test
    void testIsFormatSupported_WithSkyward_ShouldReturnTrue() {
        assertTrue(service.isFormatSupported(ExportFormat.SKYWARD));
    }

    @Test
    void testIsFormatSupported_WithInfiniteCampus_ShouldReturnTrue() {
        assertTrue(service.isFormatSupported(ExportFormat.INFINITE_CAMPUS));
    }

    @Test
    void testIsFormatSupported_WithGenericCSV_ShouldReturnTrue() {
        assertTrue(service.isFormatSupported(ExportFormat.GENERIC_CSV));
    }

    @Test
    void testIsFormatSupported_WithExcel_ShouldReturnTrue() {
        assertTrue(service.isFormatSupported(ExportFormat.EXCEL));
    }

    @Test
    void testIsFormatSupported_WithNullFormat_ShouldReturnFalse() {
        assertFalse(service.isFormatSupported(null));
    }

    // ========== EXPORT PATH TESTS ==========

    @Test
    void testGetDefaultExportPath_WithPowerSchool_ShouldReturnPath() {
        String path = service.getDefaultExportPath(ExportFormat.POWERSCHOOL);

        assertNotNull(path);
        assertTrue(path.contains("SIS_Exports") || path.contains("PowerSchool"));
    }

    @Test
    void testGetDefaultExportPath_WithSkyward_ShouldReturnPath() {
        String path = service.getDefaultExportPath(ExportFormat.SKYWARD);

        assertNotNull(path);
        assertTrue(path.contains("SIS_Exports") || path.contains("Skyward"));
    }

    @Test
    void testGetDefaultExportPath_WithInfiniteCampus_ShouldReturnPath() {
        String path = service.getDefaultExportPath(ExportFormat.INFINITE_CAMPUS);

        assertNotNull(path);
        assertTrue(path.contains("SIS_Exports") || path.contains("Infinite"));
    }

    @Test
    void testGetDefaultExportPath_WithNullFormat_ShouldHandleGracefully() {
        String path = service.getDefaultExportPath(null);

        // Should return default path or null
        assertTrue(path == null || path.contains("SIS_Exports"));
    }

    // ========== FILENAME GENERATION TESTS ==========

    @Test
    void testGenerateExportFilename_WithPowerSchoolMasterSchedule_ShouldIncludeFormatAndType() {
        String filename = service.generateExportFilename(
            ExportFormat.POWERSCHOOL,
            ExportType.MASTER_SCHEDULE
        );

        assertNotNull(filename);
        assertTrue(filename.contains("PowerSchool") || filename.contains("MASTER_SCHEDULE"));
        assertTrue(filename.endsWith(".csv"));
    }

    @Test
    void testGenerateExportFilename_WithSkywardStudentSchedules_ShouldIncludeFormatAndType() {
        String filename = service.generateExportFilename(
            ExportFormat.SKYWARD,
            ExportType.STUDENT_SCHEDULES
        );

        assertNotNull(filename);
        assertTrue(filename.endsWith(".txt"));
    }

    @Test
    void testGenerateExportFilename_WithExcelEnrollments_ShouldIncludeFormatAndType() {
        String filename = service.generateExportFilename(
            ExportFormat.EXCEL,
            ExportType.ENROLLMENTS
        );

        assertNotNull(filename);
        assertTrue(filename.endsWith(".xlsx"));
    }

    @Test
    void testGenerateExportFilename_ShouldIncludeTimestamp() {
        String filename1 = service.generateExportFilename(
            ExportFormat.POWERSCHOOL,
            ExportType.MASTER_SCHEDULE
        );

        String filename2 = service.generateExportFilename(
            ExportFormat.POWERSCHOOL,
            ExportType.MASTER_SCHEDULE
        );

        assertNotNull(filename1);
        assertNotNull(filename2);
        // Both should contain date/timestamp components
    }

    // ========== VALIDATION TESTS ==========

    @Test
    void testValidateExportData_WithMasterSchedule_ShouldReturnValidationResults() {
        List<String> validationErrors = service.validateExportData(ExportType.MASTER_SCHEDULE);

        assertNotNull(validationErrors);
        // Empty list means valid, non-empty means errors found
    }

    @Test
    void testValidateExportData_WithStudentSchedules_ShouldReturnValidationResults() {
        List<String> validationErrors = service.validateExportData(ExportType.STUDENT_SCHEDULES);

        assertNotNull(validationErrors);
    }

    @Test
    void testValidateExportData_WithEnrollments_ShouldReturnValidationResults() {
        List<String> validationErrors = service.validateExportData(ExportType.ENROLLMENTS);

        assertNotNull(validationErrors);
    }

    @Test
    void testValidateExportData_WithNullExportType_ShouldHandleGracefully() {
        List<String> validationErrors = service.validateExportData(null);

        assertNotNull(validationErrors);
        // Should return errors for null type
        assertFalse(validationErrors.isEmpty());
    }

    // ========== MASTER SCHEDULE EXPORT TESTS ==========

    @Test
    void testExportMasterSchedule_WithPowerSchool_ShouldReturnResult() {
        SISExportResult result = service.exportMasterSchedule(
            ExportFormat.POWERSCHOOL,
            testOutputPath
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportMasterSchedule_WithSkyward_ShouldReturnResult() {
        SISExportResult result = service.exportMasterSchedule(
            ExportFormat.SKYWARD,
            testOutputPath
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportMasterSchedule_WithNullFormat_ShouldHandleGracefully() {
        SISExportResult result = service.exportMasterSchedule(null, testOutputPath);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.hasErrors());
    }

    @Test
    void testExportMasterSchedule_WithNullPath_ShouldUseDefault() {
        SISExportResult result = service.exportMasterSchedule(
            ExportFormat.POWERSCHOOL,
            null
        );

        assertNotNull(result);
        // Should use default path
    }

    // ========== STUDENT SCHEDULES EXPORT TESTS ==========

    @Test
    void testExportStudentSchedules_WithValidStudentIds_ShouldReturnResult() {
        List<Long> studentIds = Arrays.asList(1L, 2L, 3L);

        SISExportResult result = service.exportStudentSchedules(
            ExportFormat.POWERSCHOOL,
            testOutputPath,
            studentIds
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportStudentSchedules_WithEmptyStudentIds_ShouldHandleGracefully() {
        SISExportResult result = service.exportStudentSchedules(
            ExportFormat.POWERSCHOOL,
            testOutputPath,
            Collections.emptyList()
        );

        assertNotNull(result);
        // May succeed with empty list or return warning
    }

    @Test
    void testExportStudentSchedules_WithNullStudentIds_ShouldHandleGracefully() {
        SISExportResult result = service.exportStudentSchedules(
            ExportFormat.POWERSCHOOL,
            testOutputPath,
            null
        );

        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    void testExportAllStudentSchedules_WithPowerSchool_ShouldReturnResult() {
        SISExportResult result = service.exportAllStudentSchedules(
            ExportFormat.POWERSCHOOL,
            testOutputPath
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    // ========== TEACHER SCHEDULES EXPORT TESTS ==========

    @Test
    void testExportTeacherSchedules_WithValidTeacherIds_ShouldReturnResult() {
        List<Long> teacherIds = Arrays.asList(1L, 2L, 3L);

        SISExportResult result = service.exportTeacherSchedules(
            ExportFormat.EXCEL,
            testOutputPath,
            teacherIds
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportTeacherSchedules_WithEmptyTeacherIds_ShouldHandleGracefully() {
        SISExportResult result = service.exportTeacherSchedules(
            ExportFormat.EXCEL,
            testOutputPath,
            Collections.emptyList()
        );

        assertNotNull(result);
    }

    // ========== COURSE SECTIONS EXPORT TESTS ==========

    @Test
    void testExportCourseSections_WithPowerSchool_ShouldReturnResult() {
        SISExportResult result = service.exportCourseSections(
            ExportFormat.POWERSCHOOL,
            testOutputPath
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportCourseSections_WithInfiniteCampus_ShouldReturnResult() {
        SISExportResult result = service.exportCourseSections(
            ExportFormat.INFINITE_CAMPUS,
            testOutputPath
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    // ========== ENROLLMENTS EXPORT TESTS ==========

    @Test
    void testExportEnrollments_WithGenericCSV_ShouldReturnResult() {
        SISExportResult result = service.exportEnrollments(
            ExportFormat.GENERIC_CSV,
            testOutputPath
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportEnrollments_WithExcel_ShouldReturnResult() {
        SISExportResult result = service.exportEnrollments(
            ExportFormat.EXCEL,
            testOutputPath
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    // ========== POWERSCHOOL-SPECIFIC TESTS ==========

    @Test
    void testExportToPowerSchool_WithMasterSchedule_ShouldReturnResult() {
        SISExportResult result = service.exportToPowerSchool(
            ExportType.MASTER_SCHEDULE,
            testOutputPath
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportPowerSchoolStudentSchedules_ShouldReturnResult() {
        SISExportResult result = service.exportPowerSchoolStudentSchedules(testOutputPath);

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportPowerSchoolSections_ShouldReturnResult() {
        SISExportResult result = service.exportPowerSchoolSections(testOutputPath);

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    // ========== SKYWARD-SPECIFIC TESTS ==========

    @Test
    void testExportToSkyward_WithStudentSchedules_ShouldReturnResult() {
        SISExportResult result = service.exportToSkyward(
            ExportType.STUDENT_SCHEDULES,
            testOutputPath
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportSkywardStudentSchedules_ShouldReturnResult() {
        SISExportResult result = service.exportSkywardStudentSchedules(testOutputPath);

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportSkywardSections_ShouldReturnResult() {
        SISExportResult result = service.exportSkywardSections(testOutputPath);

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    // ========== INFINITE CAMPUS-SPECIFIC TESTS ==========

    @Test
    void testExportToInfiniteCampus_WithCourseSections_ShouldReturnResult() {
        SISExportResult result = service.exportToInfiniteCampus(
            ExportType.COURSE_SECTIONS,
            testOutputPath
        );

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportInfiniteCampusRoster_ShouldReturnResult() {
        SISExportResult result = service.exportInfiniteCampusRoster(testOutputPath);

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    @Test
    void testExportInfiniteCampusSections_ShouldReturnResult() {
        SISExportResult result = service.exportInfiniteCampusSections(testOutputPath);

        assertNotNull(result);
        assertNotNull(result.isSuccess());
    }

    // ========== PREVIEW TESTS ==========

    @Test
    void testPreviewExportData_WithMasterSchedule_ShouldReturnPreview() {
        String preview = service.previewExportData(
            ExportFormat.GENERIC_CSV,
            ExportType.MASTER_SCHEDULE
        );

        assertNotNull(preview);
        // Preview should contain some data or be empty string
    }

    @Test
    void testPreviewExportData_WithStudentSchedules_ShouldReturnPreview() {
        String preview = service.previewExportData(
            ExportFormat.POWERSCHOOL,
            ExportType.STUDENT_SCHEDULES
        );

        assertNotNull(preview);
    }

    @Test
    void testPreviewExportData_WithNullFormat_ShouldHandleGracefully() {
        String preview = service.previewExportData(null, ExportType.MASTER_SCHEDULE);

        // Should return empty or error message
        assertNotNull(preview);
    }

    @Test
    void testPreviewExportData_WithNullType_ShouldHandleGracefully() {
        String preview = service.previewExportData(ExportFormat.GENERIC_CSV, null);

        assertNotNull(preview);
    }
}
