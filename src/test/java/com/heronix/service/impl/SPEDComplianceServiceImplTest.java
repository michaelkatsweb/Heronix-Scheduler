package com.heronix.service.impl;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.Plan504;
import com.heronix.model.domain.Student;
import com.heronix.model.enums.IEPStatus;
import com.heronix.model.enums.ServiceStatus;
import com.heronix.model.enums.ServiceType;
import com.heronix.repository.*;
import com.heronix.service.SPEDComplianceService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for SPEDComplianceServiceImpl
 *
 * Tests special education compliance tracking including:
 * - IEP compliance checking
 * - 504 Plan compliance
 * - Service minutes tracking
 * - Student compliance summaries
 * - Dashboard metrics
 * - Reports generation
 * - Null safety
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 18 - SPED Compliance Testing
 */
@ExtendWith(MockitoExtension.class)
class SPEDComplianceServiceImplTest {

    @Mock(lenient = true)
    private IEPRepository iepRepository;

    @Mock(lenient = true)
    private Plan504Repository plan504Repository;

    @Mock(lenient = true)
    private IEPServiceRepository iepServiceRepository;

    @Mock(lenient = true)
    private StudentRepository studentRepository;

    @InjectMocks
    private SPEDComplianceServiceImpl service;

    private IEP testIEP;
    private Plan504 test504;
    private IEPService testService;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Smith");
        testStudent.setStudentId("STU001");

        // Create test IEP
        testIEP = new IEP();
        testIEP.setId(1L);
        testIEP.setStudent(testStudent);
        testIEP.setIepNumber("IEP-2024-001");
        testIEP.setStartDate(LocalDate.now().minusMonths(6));
        testIEP.setEndDate(LocalDate.now().plusMonths(6));
        testIEP.setStatus(IEPStatus.ACTIVE);

        // Create test 504 plan
        test504 = new Plan504();
        test504.setId(1L);
        test504.setStudent(testStudent);
        test504.setPlanNumber("504-2024-001");
        test504.setStartDate(LocalDate.now().minusMonths(6));
        test504.setEndDate(LocalDate.now().plusMonths(6));

        // Create test IEP service
        testService = new IEPService();
        testService.setId(1L);
        testService.setIep(testIEP);
        testService.setServiceType(ServiceType.SPEECH_THERAPY);
        testService.setMinutesPerWeek(60);
        testService.setStatus(ServiceStatus.SCHEDULED);
    }

    // ========== IEP COMPLIANCE TESTS ==========

    @Test
    void testGetIEPsRequiringAction_ShouldCombineAllSources() {
        List<IEP> expiredIEPs = Arrays.asList(testIEP);
        List<IEP> expiringIEPs = Arrays.asList(testIEP);
        List<IEP> needingReview = Arrays.asList(testIEP);

        when(iepRepository.findExpiredIEPs(any(LocalDate.class))).thenReturn(expiredIEPs);
        when(iepRepository.findIEPsNeedingRenewal(any(LocalDate.class), any(LocalDate.class))).thenReturn(expiringIEPs);
        when(iepRepository.findIEPsWithReviewDue(any(LocalDate.class))).thenReturn(needingReview);

        List<IEP> result = service.getIEPsRequiringAction();

        assertNotNull(result);
        assertEquals(1, result.size()); // Should be distinct
    }

    @Test
    void testGetIEPsRequiringAction_WithNullRepositoryResults_ShouldHandleGracefully() {
        when(iepRepository.findExpiredIEPs(any(LocalDate.class))).thenReturn(null);
        when(iepRepository.findIEPsNeedingRenewal(any(LocalDate.class), any(LocalDate.class))).thenReturn(null);
        when(iepRepository.findIEPsWithReviewDue(any(LocalDate.class))).thenReturn(null);

        List<IEP> result = service.getIEPsRequiringAction();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetIEPsRequiringAction_WithNullIEPsInList_ShouldFilter() {
        List<IEP> mixedList = Arrays.asList(testIEP, null, testIEP, null);
        when(iepRepository.findExpiredIEPs(any(LocalDate.class))).thenReturn(mixedList);

        List<IEP> result = service.getIEPsRequiringAction();

        assertNotNull(result);
        assertFalse(result.contains(null));
    }

    @Test
    void testGetExpiringIEPs_WithValidThreshold_ShouldReturnList() {
        List<IEP> expiringIEPs = Arrays.asList(testIEP);
        when(iepRepository.findIEPsNeedingRenewal(any(LocalDate.class), any(LocalDate.class))).thenReturn(expiringIEPs);

        List<IEP> result = service.getExpiringIEPs(30);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testIsIEPCompliant_WithNullIepId_ShouldReturnFalse() {
        boolean result = service.isIEPCompliant(null);

        assertFalse(result);
        verify(iepRepository, never()).findById(anyLong());
    }

    @Test
    void testIsIEPCompliant_WithNonExistentIEP_ShouldReturnFalse() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.empty());

        boolean result = service.isIEPCompliant(999L);

        assertFalse(result);
    }

    @Test
    void testIsIEPCompliant_WithCompliantIEP_ShouldReturnTrue() {
        // Create a mock service that meets requirements
        IEPService compliantService = mock(IEPService.class);
        when(compliantService.getStatus()).thenReturn(ServiceStatus.SCHEDULED);
        when(compliantService.meetsMinutesRequirement()).thenReturn(true);

        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepServiceRepository.findByIepId(anyLong())).thenReturn(Arrays.asList(compliantService));

        boolean result = service.isIEPCompliant(1L);

        assertTrue(result);
    }

    @Test
    void testIsIEPCompliant_WithNullServicesList_ShouldReturnFalse() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepServiceRepository.findByIepId(anyLong())).thenReturn(null);

        boolean result = service.isIEPCompliant(1L);

        assertFalse(result);
    }

    // ========== 504 PLAN COMPLIANCE TESTS ==========

    @Test
    void testGet504PlansRequiringAction_ShouldCombineAllSources() {
        List<Plan504> expiredPlans = Arrays.asList(test504);
        List<Plan504> expiringPlans = Arrays.asList(test504);
        List<Plan504> needingReview = Arrays.asList(test504);

        when(plan504Repository.findExpiredPlans(any(LocalDate.class))).thenReturn(expiredPlans);
        when(plan504Repository.findPlansNeedingRenewal(any(LocalDate.class), any(LocalDate.class))).thenReturn(expiringPlans);
        when(plan504Repository.findPlansWithOverdueReview(any(LocalDate.class))).thenReturn(needingReview);

        List<Plan504> result = service.get504PlansRequiringAction();

        assertNotNull(result);
        assertEquals(1, result.size()); // Should be distinct
    }

    @Test
    void testGet504PlansRequiringAction_WithNullResults_ShouldHandleGracefully() {
        when(plan504Repository.findExpiredPlans(any(LocalDate.class))).thenReturn(null);
        when(plan504Repository.findPlansNeedingRenewal(any(LocalDate.class), any(LocalDate.class))).thenReturn(null);
        when(plan504Repository.findPlansWithOverdueReview(any(LocalDate.class))).thenReturn(null);

        List<Plan504> result = service.get504PlansRequiringAction();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetExpiring504Plans_WithValidThreshold_ShouldReturnList() {
        List<Plan504> expiringPlans = Arrays.asList(test504);
        when(plan504Repository.findPlansNeedingRenewal(any(LocalDate.class), any(LocalDate.class))).thenReturn(expiringPlans);

        List<Plan504> result = service.getExpiring504Plans(30);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== SERVICE MINUTES COMPLIANCE TESTS ==========

    @Test
    void testGetServiceCompliancePercentage_WithNullServiceId_ShouldReturnZero() {
        double result = service.getServiceCompliancePercentage(null);

        assertEquals(0.0, result);
        verify(iepServiceRepository, never()).findById(anyLong());
    }

    @Test
    void testGetServiceCompliancePercentage_WithNonExistentService_ShouldReturnZero() {
        when(iepServiceRepository.findById(anyLong())).thenReturn(Optional.empty());

        double result = service.getServiceCompliancePercentage(999L);

        assertEquals(0.0, result);
    }

    @Test
    void testMeetsMinutesRequirement_WithNullServiceId_ShouldReturnFalse() {
        boolean result = service.meetsMinutesRequirement(null);

        assertFalse(result);
        verify(iepServiceRepository, never()).findById(anyLong());
    }

    @Test
    void testMeetsMinutesRequirement_WithNonExistentService_ShouldReturnFalse() {
        when(iepServiceRepository.findById(anyLong())).thenReturn(Optional.empty());

        boolean result = service.meetsMinutesRequirement(999L);

        assertFalse(result);
    }

    // ========== STUDENT COMPLIANCE TESTS ==========

    @Test
    void testGetStudentComplianceSummary_WithNullStudentId_ShouldReturnEmptySummary() {
        ComplianceSummary result = service.getStudentComplianceSummary(null);

        assertNotNull(result);
        assertNull(result.studentId);
    }

    @Test
    void testGetStudentComplianceSummary_WithNonExistentStudent_ShouldReturnEmptySummary() {
        when(studentRepository.findById(anyLong())).thenReturn(Optional.empty());

        ComplianceSummary result = service.getStudentComplianceSummary(999L);

        assertNotNull(result);
        assertNotNull(result.issues);
    }

    @Test
    void testGetStudentComplianceSummary_WithValidStudent_ShouldReturnSummary() {
        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.of(testIEP));
        when(plan504Repository.findByStudentId(anyLong())).thenReturn(Optional.empty());
        when(iepServiceRepository.findByIepId(anyLong())).thenReturn(Arrays.asList(testService));

        ComplianceSummary result = service.getStudentComplianceSummary(1L);

        assertNotNull(result);
        assertEquals(1L, result.studentId);
        assertTrue(result.hasActiveIEP);
        assertFalse(result.hasActive504);
    }

    @Test
    void testGetStudentComplianceSummary_WithNullIEPId_ShouldHandleGracefully() {
        IEP iepWithNullId = new IEP();
        iepWithNullId.setId(null);
        iepWithNullId.setStatus(IEPStatus.ACTIVE);
        iepWithNullId.setStartDate(LocalDate.now().minusMonths(6));
        iepWithNullId.setEndDate(LocalDate.now().plusMonths(6));

        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.of(iepWithNullId));

        ComplianceSummary result = service.getStudentComplianceSummary(1L);

        assertNotNull(result);
        assertEquals(0, result.totalServices); // Should skip service loading
    }

    @Test
    void testGetStudentComplianceIssues_WithNullStudentId_ShouldReturnEmptyList() {
        List<String> result = service.getStudentComplianceIssues(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetStudentComplianceIssues_WithNoActiveIEP_ShouldReturnEmptyList() {
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.empty());

        List<String> result = service.getStudentComplianceIssues(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetStudentComplianceIssues_WithExpiringIEP_ShouldReturnIssues() {
        IEP expiringIEP = new IEP();
        expiringIEP.setId(1L);
        expiringIEP.setStatus(IEPStatus.ACTIVE);
        expiringIEP.setStartDate(LocalDate.now().minusMonths(11));
        expiringIEP.setEndDate(LocalDate.now().plusDays(15)); // Expires in 15 days
        expiringIEP.setNextReviewDate(null);

        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.of(expiringIEP));
        when(iepServiceRepository.findByIepId(anyLong())).thenReturn(new ArrayList<>());

        List<String> result = service.getStudentComplianceIssues(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(issue -> issue.contains("expiring within 30 days")));
    }

    // ========== DASHBOARD METRICS TESTS ==========

    @Test
    void testGetDashboardMetrics_ShouldReturnMetrics() {
        when(iepRepository.countActiveIEPs(any(LocalDate.class))).thenReturn(10L);
        when(plan504Repository.countActivePlans(any(LocalDate.class))).thenReturn(5L);
        when(iepServiceRepository.findScheduledServices()).thenReturn(Arrays.asList(testService));
        when(iepServiceRepository.findServicesNeedingScheduling()).thenReturn(new ArrayList<>());
        when(iepServiceRepository.findServicesNotMeetingMinutes()).thenReturn(new ArrayList<>());
        when(iepRepository.findIEPsNeedingRenewal(any(LocalDate.class), any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(iepRepository.findIEPsWithReviewDue(any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(iepRepository.findAllActiveIEPs(any(LocalDate.class))).thenReturn(new ArrayList<>());

        DashboardMetrics result = service.getDashboardMetrics();

        assertNotNull(result);
        assertEquals(10L, result.totalActiveIEPs);
        assertEquals(5L, result.totalActive504Plans);
    }

    @Test
    void testGetIEPComplianceRate_WithNoActiveIEPs_ShouldReturn100Percent() {
        when(iepRepository.findAllActiveIEPs(any(LocalDate.class))).thenReturn(new ArrayList<>());

        double result = service.getIEPComplianceRate();

        assertEquals(100.0, result);
    }

    @Test
    void testGetIEPComplianceRate_WithNullIEPsList_ShouldReturn100Percent() {
        when(iepRepository.findAllActiveIEPs(any(LocalDate.class))).thenReturn(null);

        double result = service.getIEPComplianceRate();

        assertEquals(100.0, result);
    }

    @Test
    void testGetIEPComplianceRate_WithNullIEPsInList_ShouldFilter() {
        List<IEP> mixedList = Arrays.asList(testIEP, null, testIEP);
        when(iepRepository.findAllActiveIEPs(any(LocalDate.class))).thenReturn(mixedList);
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepServiceRepository.findByIepId(anyLong())).thenReturn(Arrays.asList(testService));

        double result = service.getIEPComplianceRate();

        assertTrue(result >= 0.0 && result <= 100.0);
    }

    @Test
    void testGetServiceStatusCounts_WithNullResults_ShouldReturnEmptyMap() {
        when(iepServiceRepository.countByStatus()).thenReturn(null);

        Map<String, Long> result = service.getServiceStatusCounts();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetServiceStatusCounts_WithValidResults_ShouldReturnMap() {
        List<Object[]> results = Arrays.asList(
            new Object[]{"SCHEDULED", 10L},
            new Object[]{"NOT_SCHEDULED", 5L}
        );
        when(iepServiceRepository.countByStatus()).thenReturn(results);

        Map<String, Long> result = service.getServiceStatusCounts();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetServiceStatusCounts_WithNullElementsInResults_ShouldFilter() {
        List<Object[]> results = Arrays.asList(
            new Object[]{"SCHEDULED", 10L},
            null,
            new Object[]{null, 5L},
            new Object[]{"NOT_SCHEDULED", null}
        );
        when(iepServiceRepository.countByStatus()).thenReturn(results);

        assertDoesNotThrow(() -> service.getServiceStatusCounts());
    }

    @Test
    void testGetIEPCountByCategory_WithNullResults_ShouldReturnEmptyMap() {
        when(iepRepository.countByEligibilityCategory()).thenReturn(null);

        Map<String, Long> result = service.getIEPCountByCategory();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetIEPCountByCategory_WithNullCategoryName_ShouldUseUnknown() {
        List<Object[]> results = Arrays.asList(
            new Object[]{null, 5L},
            new Object[]{"Learning Disability", 10L}
        );
        when(iepRepository.countByEligibilityCategory()).thenReturn(results);

        Map<String, Long> result = service.getIEPCountByCategory();

        assertNotNull(result);
        assertTrue(result.containsKey("Unknown"));
    }

    // ========== REPORTS TESTS ==========

    @Test
    void testGenerateComplianceReport_WithActiveIEPs_ShouldReturnReport() {
        when(iepRepository.findAllActiveIEPs(any(LocalDate.class))).thenReturn(Arrays.asList(testIEP));
        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepServiceRepository.findByIepId(anyLong())).thenReturn(new ArrayList<>());

        List<ComplianceSummary> result = service.generateComplianceReport();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGenerateComplianceReport_WithNullStudents_ShouldFilter() {
        IEP iepWithNullStudent = new IEP();
        iepWithNullStudent.setId(2L);
        iepWithNullStudent.setStudent(null);

        when(iepRepository.findAllActiveIEPs(any(LocalDate.class))).thenReturn(Arrays.asList(testIEP, iepWithNullStudent));
        when(studentRepository.findById(anyLong())).thenReturn(Optional.of(testStudent));
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepServiceRepository.findByIepId(anyLong())).thenReturn(new ArrayList<>());

        List<ComplianceSummary> result = service.generateComplianceReport();

        assertNotNull(result);
        assertEquals(1, result.size()); // Only IEP with valid student
    }

    @Test
    void testGenerateExpirationReport_WithThreshold_ShouldReturnReport() {
        when(iepRepository.findIEPsNeedingRenewal(any(LocalDate.class), any(LocalDate.class))).thenReturn(Arrays.asList(testIEP));
        when(plan504Repository.findPlansNeedingRenewal(any(LocalDate.class), any(LocalDate.class))).thenReturn(Arrays.asList(test504));

        ExpirationReport result = service.generateExpirationReport(30);

        assertNotNull(result);
        assertEquals(1, result.expiringIEPs.size());
        assertEquals(1, result.expiring504Plans.size());
        assertEquals(2, result.totalExpiring);
    }

    @Test
    void testGenerateMinutesReport_WithServices_ShouldReturnReport() {
        when(iepServiceRepository.findScheduledServices()).thenReturn(Arrays.asList(testService));

        MinutesComplianceReport result = service.generateMinutesReport();

        assertNotNull(result);
        assertEquals(1, result.totalServices);
    }

    @Test
    void testGenerateMinutesReport_WithServicesWithNullStudent_ShouldSkip() {
        IEPService serviceWithNullStudent = new IEPService();
        serviceWithNullStudent.setId(2L);
        serviceWithNullStudent.setServiceType(ServiceType.OCCUPATIONAL_THERAPY);

        when(iepServiceRepository.findScheduledServices()).thenReturn(Arrays.asList(testService, serviceWithNullStudent));

        MinutesComplianceReport result = service.generateMinutesReport();

        assertNotNull(result);
        assertEquals(2, result.totalServices);
        assertEquals(1, result.servicesInfo.size()); // Only service with valid student
    }
}
