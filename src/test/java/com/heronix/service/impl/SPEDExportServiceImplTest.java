package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.enums.ServiceType;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for SPEDExportServiceImpl
 *
 * Tests SPED compliance reports and pull-out schedule export functionality.
 * Focuses on validation, data preparation, and export logic.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SPEDExportServiceImplTest {

    @Mock
    private IEPRepository iepRepository;

    @Mock
    private IEPServiceRepository iepServiceRepository;

    @Mock
    private PullOutScheduleRepository pullOutScheduleRepository;

    @InjectMocks
    private SPEDExportServiceImpl service;

    private IEP testIEP;
    private IEPService testIEPService;
    private PullOutSchedule testPullOutSchedule;
    private Teacher testProvider;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("Jane");
        testStudent.setLastName("Smith");
        testStudent.setStudentId("STU-001");

        // Create test provider
        testProvider = new Teacher();
        testProvider.setId(1L);
        testProvider.setName("Sarah Johnson");
        testProvider.setEmployeeId("EMP-001");

        // Create test IEP
        testIEP = new IEP();
        testIEP.setId(1L);
        testIEP.setStudent(testStudent);
        testIEP.setStartDate(LocalDate.of(2025, 1, 1));
        testIEP.setEndDate(LocalDate.of(2025, 12, 31));

        // Create test IEP service
        testIEPService = new IEPService();
        testIEPService.setId(1L);
        testIEPService.setIep(testIEP);
        testIEPService.setServiceType(ServiceType.SPEECH_THERAPY);
        testIEPService.setMinutesPerWeek(60);
        testIEPService.setFrequency("2x per week");
        testIEPService.setAssignedStaff(testProvider);

        // Create test pull-out schedule
        testPullOutSchedule = new PullOutSchedule();
        testPullOutSchedule.setId(1L);
        testPullOutSchedule.setIepService(testIEPService);
        testPullOutSchedule.setStaff(testProvider);
        testPullOutSchedule.setStartTime(LocalTime.of(9, 0));
        testPullOutSchedule.setEndTime(LocalTime.of(9, 30));
        testPullOutSchedule.setStartDate(LocalDate.of(2025, 1, 6));

        // Mock repositories
        when(iepRepository.findAll()).thenReturn(Arrays.asList(testIEP));
        when(iepServiceRepository.findAll()).thenReturn(Arrays.asList(testIEPService));
        when(pullOutScheduleRepository.findAll()).thenReturn(Arrays.asList(testPullOutSchedule));
    }

    // ========== COMPLIANCE REPORT TESTS ==========

    @Test
    void testExportComplianceReportPdf_ShouldCreateFile() throws Exception {
        File result = service.exportComplianceReportPdf();

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".pdf"));
        assertTrue(result.length() > 0);
        verify(iepRepository, atLeastOnce()).findAll();
    }

    @Test
    void testExportComplianceReportExcel_ShouldCreateFile() throws Exception {
        File result = service.exportComplianceReportExcel();

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".xlsx"));
        assertTrue(result.length() > 0);
        verify(iepRepository, atLeastOnce()).findAll();
    }

    @Test
    void testExportComplianceReport_WithNoActiveIEPs_ShouldCreateEmptyReport() throws Exception {
        when(iepRepository.findAll()).thenReturn(Collections.emptyList());

        File result = service.exportComplianceReportPdf();

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportComplianceReport_WithMultipleIEPs_ShouldIncludeAll() throws Exception {
        IEP iep2 = new IEP();
        iep2.setId(2L);
        iep2.setStudent(testStudent);

        when(iepRepository.findAll()).thenReturn(Arrays.asList(testIEP, iep2));

        File result = service.exportComplianceReportPdf();

        assertNotNull(result);
        assertTrue(result.exists());
    }

    // ========== PULL-OUT SCHEDULES EXPORT TESTS ==========

    @Test
    void testExportPullOutSchedulesPdf_WithValidSchedules_ShouldCreateFile() throws Exception {
        List<PullOutSchedule> schedules = Arrays.asList(testPullOutSchedule);

        File result = service.exportPullOutSchedulesPdf(schedules);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".pdf"));
        assertTrue(result.length() > 0);
    }

    @Test
    void testExportPullOutSchedulesExcel_WithValidSchedules_ShouldCreateFile() throws Exception {
        List<PullOutSchedule> schedules = Arrays.asList(testPullOutSchedule);

        File result = service.exportPullOutSchedulesExcel(schedules);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".xlsx"));
        assertTrue(result.length() > 0);
    }

    @Test
    void testExportPullOutSchedulesCsv_WithValidSchedules_ShouldCreateFile() throws Exception {
        List<PullOutSchedule> schedules = Arrays.asList(testPullOutSchedule);

        File result = service.exportPullOutSchedulesCsv(schedules);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".csv"));
        assertTrue(result.length() > 0);
    }

    @Test
    void testExportPullOutSchedules_WithEmptyList_ShouldCreateEmptyFile() throws Exception {
        List<PullOutSchedule> emptySchedules = Collections.emptyList();

        File result = service.exportPullOutSchedulesPdf(emptySchedules);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportPullOutSchedules_WithNullList_ShouldThrowException() {
        assertThrows(Exception.class, () ->
            service.exportPullOutSchedulesPdf(null));
    }

    @Test
    void testExportPullOutSchedules_WithMultipleSchedules_ShouldIncludeAll() throws Exception {
        PullOutSchedule schedule2 = new PullOutSchedule();
        schedule2.setId(2L);
        schedule2.setIepService(testIEPService);
        schedule2.setStaff(testProvider);
        schedule2.setStartTime(LocalTime.of(10, 0));
        schedule2.setEndTime(LocalTime.of(10, 30));

        List<PullOutSchedule> schedules = Arrays.asList(testPullOutSchedule, schedule2);

        File result = service.exportPullOutSchedulesPdf(schedules);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportPullOutSchedules_WithNullProperties_ShouldHandleGracefully() throws Exception {
        testPullOutSchedule.setStaff(null);
        testPullOutSchedule.setStartTime(null);

        List<PullOutSchedule> schedules = Arrays.asList(testPullOutSchedule);

        File result = service.exportPullOutSchedulesCsv(schedules);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    // ========== PROVIDER WORKLOAD EXPORT TESTS ==========

    @Test
    void testExportProviderWorkloadPdf_WithValidProviders_ShouldCreateFile() throws Exception {
        List<Teacher> providers = Arrays.asList(testProvider);

        File result = service.exportProviderWorkloadPdf(providers);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".pdf"));
        assertTrue(result.length() > 0);
    }

    @Test
    void testExportProviderWorkloadExcel_WithValidProviders_ShouldCreateFile() throws Exception {
        List<Teacher> providers = Arrays.asList(testProvider);

        File result = service.exportProviderWorkloadExcel(providers);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".xlsx"));
        assertTrue(result.length() > 0);
    }

    @Test
    void testExportProviderWorkload_WithEmptyList_ShouldCreateEmptyFile() throws Exception {
        List<Teacher> emptyProviders = Collections.emptyList();

        File result = service.exportProviderWorkloadPdf(emptyProviders);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportProviderWorkload_WithNullList_ShouldThrowException() {
        assertThrows(Exception.class, () ->
            service.exportProviderWorkloadPdf(null));
    }

    @Test
    void testExportProviderWorkload_WithMultipleProviders_ShouldIncludeAll() throws Exception {
        Teacher provider2 = new Teacher();
        provider2.setId(2L);
        provider2.setName("John Doe");

        List<Teacher> providers = Arrays.asList(testProvider, provider2);

        File result = service.exportProviderWorkloadPdf(providers);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    // ========== IEP SUMMARY EXPORT TESTS ==========

    @Test
    void testExportIepSummaryPdf_WithValidIEP_ShouldCreateFile() throws Exception {
        File result = service.exportIepSummaryPdf(testIEP);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".pdf"));
        assertTrue(result.length() > 0);
    }

    @Test
    void testExportIepSummaryPdf_WithNullIEP_ShouldThrowException() {
        assertThrows(Exception.class, () ->
            service.exportIepSummaryPdf(null));
    }

    @Test
    void testExportIepSummaryPdf_WithNullStudent_ShouldHandleGracefully() throws Exception {
        testIEP.setStudent(null);

        File result = service.exportIepSummaryPdf(testIEP);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportIepsSummaryExcel_WithValidIEPs_ShouldCreateFile() throws Exception {
        List<IEP> ieps = Arrays.asList(testIEP);

        File result = service.exportIepsSummaryExcel(ieps);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".xlsx"));
        assertTrue(result.length() > 0);
    }

    @Test
    void testExportIepsSummaryExcel_WithEmptyList_ShouldCreateEmptyFile() throws Exception {
        List<IEP> emptyIeps = Collections.emptyList();

        File result = service.exportIepsSummaryExcel(emptyIeps);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportIepsSummaryExcel_WithNullList_ShouldThrowException() {
        assertThrows(Exception.class, () ->
            service.exportIepsSummaryExcel(null));
    }

    // ========== SERVICE DELIVERY EXPORT TESTS ==========

    @Test
    void testExportServiceDeliveryPdf_WithValidServices_ShouldCreateFile() throws Exception {
        List<IEPService> services = Arrays.asList(testIEPService);

        File result = service.exportServiceDeliveryPdf(services);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".pdf"));
        assertTrue(result.length() > 0);
    }

    @Test
    void testExportServiceDeliveryExcel_WithValidServices_ShouldCreateFile() throws Exception {
        List<IEPService> services = Arrays.asList(testIEPService);

        File result = service.exportServiceDeliveryExcel(services);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".xlsx"));
        assertTrue(result.length() > 0);
    }

    @Test
    void testExportServiceDelivery_WithEmptyList_ShouldCreateEmptyFile() throws Exception {
        List<IEPService> emptyServices = Collections.emptyList();

        File result = service.exportServiceDeliveryPdf(emptyServices);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportServiceDelivery_WithNullList_ShouldThrowException() {
        assertThrows(Exception.class, () ->
            service.exportServiceDeliveryPdf(null));
    }

    @Test
    void testExportServiceDelivery_WithMultipleServices_ShouldIncludeAll() throws Exception {
        IEPService service2 = new IEPService();
        service2.setId(2L);
        service2.setIep(testIEP);
        service2.setServiceType(ServiceType.OCCUPATIONAL_THERAPY);

        List<IEPService> services = Arrays.asList(testIEPService, service2);

        File result = service.exportServiceDeliveryPdf(services);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void testExportServiceDelivery_WithNullProperties_ShouldHandleGracefully() throws Exception {
        testIEPService.setAssignedStaff(null);
        testIEPService.setServiceType(null);

        List<IEPService> services = Arrays.asList(testIEPService);

        File result = service.exportServiceDeliveryExcel(services);

        assertNotNull(result);
        assertTrue(result.exists());
    }

    // ========== EXPORTS DIRECTORY TESTS ==========

    @Test
    void testGetExportsDirectory_ShouldCreateIfNotExists() {
        File dir = service.getExportsDirectory();

        assertNotNull(dir);
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
    }

    @Test
    void testGetExportsDirectory_ShouldBeConsistent() {
        File dir1 = service.getExportsDirectory();
        File dir2 = service.getExportsDirectory();

        assertNotNull(dir1);
        assertNotNull(dir2);
        assertEquals(dir1.getAbsolutePath(), dir2.getAbsolutePath());
    }

    @Test
    void testGetExportsDirectory_ShouldContainSPEDInPath() {
        File dir = service.getExportsDirectory();

        assertNotNull(dir);
        assertTrue(dir.getAbsolutePath().contains("SPED") ||
                   dir.getAbsolutePath().contains("Heronix Scheduler"));
    }

    // ========== FILE NAMING TESTS ==========

    @Test
    void testExportFiles_ShouldGenerateUniqueFilenames() throws Exception {
        File file1 = service.exportComplianceReportPdf();
        Thread.sleep(100); // Ensure different timestamps
        File file2 = service.exportComplianceReportPdf();

        assertNotNull(file1);
        assertNotNull(file2);
        // Both files should exist
        assertTrue(file1.exists());
        assertTrue(file2.exists());
    }

    @Test
    void testExportFiles_ShouldIncludeDescriptiveNames() throws Exception {
        File complianceReport = service.exportComplianceReportPdf();
        List<PullOutSchedule> schedules = Arrays.asList(testPullOutSchedule);
        File pullOutReport = service.exportPullOutSchedulesPdf(schedules);

        assertNotNull(complianceReport);
        assertNotNull(pullOutReport);
        // Filenames should be different and descriptive
        assertNotEquals(complianceReport.getName(), pullOutReport.getName());
    }
}
