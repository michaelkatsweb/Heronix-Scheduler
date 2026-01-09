package com.heronix.service.impl;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.IEPStatus;
import com.heronix.model.enums.ServiceType;
import com.heronix.model.enums.DeliveryModel;
import com.heronix.model.enums.ServiceStatus;
import com.heronix.repository.IEPRepository;
import com.heronix.repository.IEPServiceRepository;
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
 * Comprehensive test suite for IEPServiceImpl
 *
 * Tests special education IEP management including:
 * - CRUD operations
 * - Status management
 * - Service management
 * - Queries and statistics
 * - Validation logic
 * - Null safety
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 18 - IEP Service Testing
 */
@ExtendWith(MockitoExtension.class)
class IEPServiceImplTest {

    @Mock(lenient = true)
    private IEPRepository iepRepository;

    @Mock(lenient = true)
    private IEPServiceRepository iepServiceRepository;

    @InjectMocks
    private IEPServiceImpl service;

    private IEP testIEP;
    private Student testStudent;
    private IEPService testIEPService;

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
        testIEP.setStartDate(LocalDate.now());
        testIEP.setEndDate(LocalDate.now().plusYears(1));
        testIEP.setStatus(IEPStatus.DRAFT);
        testIEP.setEligibilityCategory("Learning Disability");
        testIEP.setCaseManager("Jane Doe");

        // Create test IEP service
        testIEPService = new IEPService();
        testIEPService.setId(1L);
        testIEPService.setServiceType(ServiceType.SPEECH_THERAPY);
        testIEPService.setMinutesPerWeek(60);
        testIEPService.setSessionDurationMinutes(30);
        testIEPService.setDeliveryModel(DeliveryModel.INDIVIDUAL);
        testIEPService.setStatus(ServiceStatus.NOT_SCHEDULED);

        // Create teacher for assigned staff
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setFirstName("Dr.");
        teacher.setLastName("Smith");
        testIEPService.setAssignedStaff(teacher);
    }

    // ========== CREATE IEP TESTS ==========

    @Test
    void testCreateIEP_WithValidIEP_ShouldSucceed() {
        when(iepRepository.save(any(IEP.class))).thenReturn(testIEP);
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.empty());

        IEP result = service.createIEP(testIEP);

        assertNotNull(result);
        assertEquals(testIEP.getId(), result.getId());
        verify(iepRepository).save(testIEP);
    }

    @Test
    void testCreateIEP_WithNullIEP_ShouldThrowException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.createIEP(null);
        });
        assertTrue(exception.getMessage().contains("IEP cannot be null"));
    }

    @Test
    void testCreateIEP_WithNullStudent_ShouldThrowException() {
        testIEP.setStudent(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.createIEP(testIEP);
        });
        assertTrue(exception.getMessage().contains("validation failed"));
    }

    @Test
    void testCreateIEP_WithExistingActiveIEP_ShouldThrowException() {
        IEP activeIEP = new IEP();
        activeIEP.setId(2L);
        activeIEP.setStatus(IEPStatus.ACTIVE);
        activeIEP.setStartDate(LocalDate.now().minusMonths(6));
        activeIEP.setEndDate(LocalDate.now().plusMonths(6)); // Set endDate so isActive() returns true
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.of(activeIEP));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            service.createIEP(testIEP);
        });
        assertTrue(exception.getMessage().contains("already has an active IEP"));
    }

    @Test
    void testCreateIEP_WithNullStatus_ShouldSetDraft() {
        testIEP.setStatus(null);
        when(iepRepository.save(any(IEP.class))).thenReturn(testIEP);
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.empty());

        IEP result = service.createIEP(testIEP);

        assertEquals(IEPStatus.DRAFT, result.getStatus());
    }

    @Test
    void testCreateIEP_WithNullStudentId_ShouldNotThrowNPE() {
        testIEP.getStudent().setId(null);

        assertDoesNotThrow(() -> {
            try {
                service.createIEP(testIEP);
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Expected validation exceptions are OK
            }
        });
    }

    // ========== UPDATE IEP TESTS ==========

    @Test
    void testUpdateIEP_WithValidIEP_ShouldSucceed() {
        when(iepRepository.existsById(anyLong())).thenReturn(true);
        when(iepRepository.save(any(IEP.class))).thenReturn(testIEP);

        IEP result = service.updateIEP(testIEP);

        assertNotNull(result);
        assertEquals(testIEP.getId(), result.getId());
        verify(iepRepository).save(testIEP);
    }

    @Test
    void testUpdateIEP_WithNullIEP_ShouldThrowException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.updateIEP(null);
        });
        assertTrue(exception.getMessage().contains("IEP cannot be null"));
    }

    @Test
    void testUpdateIEP_WithNullId_ShouldThrowException() {
        testIEP.setId(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.updateIEP(testIEP);
        });
        assertTrue(exception.getMessage().contains("IEP ID cannot be null"));
    }

    @Test
    void testUpdateIEP_WithNonExistentId_ShouldThrowException() {
        when(iepRepository.existsById(anyLong())).thenReturn(false);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.updateIEP(testIEP);
        });
        assertTrue(exception.getMessage().contains("IEP not found"));
    }

    // ========== FIND TESTS ==========

    @Test
    void testFindById_WithValidId_ShouldReturnIEP() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));

        Optional<IEP> result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(testIEP.getId(), result.get().getId());
    }

    @Test
    void testFindById_WithNonExistentId_ShouldReturnEmpty() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.empty());

        Optional<IEP> result = service.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByIepNumber_WithValidNumber_ShouldReturnIEP() {
        when(iepRepository.findByIepNumber(anyString())).thenReturn(Optional.of(testIEP));

        Optional<IEP> result = service.findByIepNumber("IEP-2024-001");

        assertTrue(result.isPresent());
        assertEquals(testIEP.getIepNumber(), result.get().getIepNumber());
    }

    @Test
    void testFindActiveIEPForStudent_WithActiveIEP_ShouldReturnIEP() {
        testIEP.setStatus(IEPStatus.ACTIVE);
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.of(testIEP));

        Optional<IEP> result = service.findActiveIEPForStudent(1L);

        assertTrue(result.isPresent());
        assertEquals(IEPStatus.ACTIVE, result.get().getStatus());
    }

    @Test
    void testFindActiveIEPForStudent_WithNullIEP_ShouldReturnEmpty() {
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.ofNullable(null));

        Optional<IEP> result = service.findActiveIEPForStudent(1L);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindAllIEPsForStudent_WithIEPs_ShouldReturnList() {
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.of(testIEP));

        List<IEP> result = service.findAllIEPsForStudent(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindAllIEPsForStudent_WithNoIEPs_ShouldReturnEmptyList() {
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.empty());

        List<IEP> result = service.findAllIEPsForStudent(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== DELETE TESTS ==========

    @Test
    void testDeleteIEP_WithDraftStatus_ShouldSucceed() {
        testIEP.setStatus(IEPStatus.DRAFT);
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        doNothing().when(iepRepository).delete(any(IEP.class));

        assertDoesNotThrow(() -> service.deleteIEP(1L));
        verify(iepRepository).delete(testIEP);
    }

    @Test
    void testDeleteIEP_WithActiveStatus_ShouldThrowException() {
        testIEP.setStatus(IEPStatus.ACTIVE);
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            service.deleteIEP(1L);
        });
        assertTrue(exception.getMessage().contains("Cannot delete IEP"));
    }

    @Test
    void testDeleteIEP_WithNonExistentId_ShouldThrowException() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.deleteIEP(999L);
        });
        assertTrue(exception.getMessage().contains("IEP not found"));
    }

    @Test
    void testDeleteIEP_WithNullStatus_ShouldDefaultToDraft() {
        testIEP.setStatus(null);
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        doNothing().when(iepRepository).delete(any(IEP.class));

        assertDoesNotThrow(() -> service.deleteIEP(1L));
        verify(iepRepository).delete(testIEP);
    }

    // ========== STATUS MANAGEMENT TESTS ==========

    @Test
    void testActivateIEP_WithValidIEP_ShouldSucceed() {
        testIEP.setStatus(IEPStatus.DRAFT);
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepRepository.save(any(IEP.class))).thenReturn(testIEP);

        IEP result = service.activateIEP(1L);

        assertNotNull(result);
        assertEquals(IEPStatus.ACTIVE, result.getStatus());
    }

    @Test
    void testActivateIEP_WithNonExistentId_ShouldThrowException() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.activateIEP(999L);
        });
        assertTrue(exception.getMessage().contains("IEP not found"));
    }

    @Test
    void testExpireIEP_WithValidIEP_ShouldSucceed() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepRepository.save(any(IEP.class))).thenReturn(testIEP);

        IEP result = service.expireIEP(1L);

        assertNotNull(result);
        assertEquals(IEPStatus.EXPIRED, result.getStatus());
    }

    @Test
    void testMarkForReview_WithValidIEP_ShouldSucceed() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepRepository.save(any(IEP.class))).thenReturn(testIEP);

        IEP result = service.markForReview(1L);

        assertNotNull(result);
        assertEquals(IEPStatus.PENDING_REVIEW, result.getStatus());
    }

    @Test
    void testExpireOutdatedIEPs_WithExpiredIEPs_ShouldUpdateStatus() {
        List<IEP> expiredIEPs = Arrays.asList(testIEP);
        when(iepRepository.findExpiredIEPs(any(LocalDate.class))).thenReturn(expiredIEPs);
        when(iepRepository.save(any(IEP.class))).thenReturn(testIEP);

        int count = service.expireOutdatedIEPs();

        assertEquals(1, count);
        verify(iepRepository).save(testIEP);
    }

    @Test
    void testExpireOutdatedIEPs_WithNullIEPInList_ShouldSkip() {
        List<IEP> expiredIEPs = Arrays.asList(testIEP, null, testIEP);
        when(iepRepository.findExpiredIEPs(any(LocalDate.class))).thenReturn(expiredIEPs);
        when(iepRepository.save(any(IEP.class))).thenReturn(testIEP);

        int count = service.expireOutdatedIEPs();

        assertEquals(2, count); // Should skip null and only process 2
    }

    // ========== SERVICE MANAGEMENT TESTS ==========

    @Test
    void testAddService_WithValidService_ShouldSucceed() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepRepository.save(any(IEP.class))).thenReturn(testIEP);

        IEP result = service.addService(1L, testIEPService);

        assertNotNull(result);
        verify(iepRepository).save(testIEP);
    }

    @Test
    void testAddService_WithNullService_ShouldThrowException() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.addService(1L, null);
        });
        assertTrue(exception.getMessage().contains("Service cannot be null"));
    }

    @Test
    void testAddService_WithNonExistentIEP_ShouldThrowException() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.addService(999L, testIEPService);
        });
        assertTrue(exception.getMessage().contains("IEP not found"));
    }

    @Test
    void testRemoveService_WithValidIds_ShouldSucceed() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepServiceRepository.findById(anyLong())).thenReturn(Optional.of(testIEPService));
        when(iepRepository.save(any(IEP.class))).thenReturn(testIEP);

        IEP result = service.removeService(1L, 1L);

        assertNotNull(result);
        verify(iepRepository).save(testIEP);
    }

    @Test
    void testRemoveService_WithNonExistentService_ShouldThrowException() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepServiceRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.removeService(1L, 999L);
        });
        assertTrue(exception.getMessage().contains("Service not found"));
    }

    @Test
    void testUpdateService_WithValidService_ShouldSucceed() {
        testIEPService.setId(1L);
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));
        when(iepRepository.save(any(IEP.class))).thenReturn(testIEP);

        IEP result = service.updateService(1L, testIEPService);

        assertNotNull(result);
        verify(iepRepository).save(testIEP);
    }

    @Test
    void testUpdateService_WithNullService_ShouldThrowException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.updateService(1L, null);
        });
        assertTrue(exception.getMessage().contains("Service cannot be null"));
    }

    @Test
    void testUpdateService_WithNullServiceId_ShouldThrowException() {
        testIEPService.setId(null);
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.updateService(1L, testIEPService);
        });
        assertTrue(exception.getMessage().contains("Service ID cannot be null"));
    }

    // ========== QUERY TESTS ==========

    @Test
    void testFindAllActiveIEPs_ShouldReturnList() {
        List<IEP> activeIEPs = Arrays.asList(testIEP);
        when(iepRepository.findAllActiveIEPs(any(LocalDate.class))).thenReturn(activeIEPs);

        List<IEP> result = service.findAllActiveIEPs();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindByStatus_WithValidStatus_ShouldReturnList() {
        List<IEP> draftIEPs = Arrays.asList(testIEP);
        when(iepRepository.findByStatus(any(IEPStatus.class))).thenReturn(draftIEPs);

        List<IEP> result = service.findByStatus(IEPStatus.DRAFT);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindIEPsNeedingRenewal_WithThreshold_ShouldReturnList() {
        List<IEP> needsRenewal = Arrays.asList(testIEP);
        when(iepRepository.findIEPsNeedingRenewal(any(LocalDate.class), any(LocalDate.class))).thenReturn(needsRenewal);

        List<IEP> result = service.findIEPsNeedingRenewal(30);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindIEPsWithReviewDue_ShouldReturnList() {
        List<IEP> reviewDue = Arrays.asList(testIEP);
        when(iepRepository.findIEPsWithReviewDue(any(LocalDate.class))).thenReturn(reviewDue);

        List<IEP> result = service.findIEPsWithReviewDue();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindByCaseManager_WithValidName_ShouldReturnList() {
        List<IEP> ieps = Arrays.asList(testIEP);
        when(iepRepository.findByCaseManager(anyString())).thenReturn(ieps);

        List<IEP> result = service.findByCaseManager("Jane Doe");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindIEPsWithUnscheduledServices_ShouldReturnList() {
        List<IEP> unscheduled = Arrays.asList(testIEP);
        when(iepRepository.findIEPsWithUnscheduledServices()).thenReturn(unscheduled);

        List<IEP> result = service.findIEPsWithUnscheduledServices();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testSearchByStudentName_WithSearchTerm_ShouldReturnList() {
        List<IEP> searchResults = Arrays.asList(testIEP);
        when(iepRepository.searchByStudentName(anyString())).thenReturn(searchResults);

        List<IEP> result = service.searchByStudentName("Smith");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== STATISTICS TESTS ==========

    @Test
    void testCountActiveIEPs_ShouldReturnCount() {
        when(iepRepository.countActiveIEPs(any(LocalDate.class))).thenReturn(5L);

        long count = service.countActiveIEPs();

        assertEquals(5L, count);
    }

    @Test
    void testGetIEPCountByEligibilityCategory_ShouldReturnData() {
        List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{"Learning Disability", 10L});
        data.add(new Object[]{"Autism", 5L});
        when(iepRepository.countByEligibilityCategory()).thenReturn(data);

        List<Object[]> result = service.getIEPCountByEligibilityCategory();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetTotalServiceMinutesPerWeek_WithIEPs_ShouldCalculateSum() {
        IEP iep1 = new IEP();
        iep1.setId(1L);

        IEP iep2 = new IEP();
        iep2.setId(2L);

        List<IEP> activeIEPs = Arrays.asList(iep1, iep2);
        when(iepRepository.findAllActiveIEPs(any(LocalDate.class))).thenReturn(activeIEPs);

        int result = service.getTotalServiceMinutesPerWeek();

        assertTrue(result >= 0);
    }

    @Test
    void testGetTotalServiceMinutesPerWeek_WithNullIEPInList_ShouldSkip() {
        IEP iep1 = new IEP();
        iep1.setId(1L);

        List<IEP> activeIEPs = Arrays.asList(iep1, null);
        when(iepRepository.findAllActiveIEPs(any(LocalDate.class))).thenReturn(activeIEPs);

        assertDoesNotThrow(() -> {
            int result = service.getTotalServiceMinutesPerWeek();
            assertTrue(result >= 0);
        });
    }

    // ========== VALIDATION TESTS ==========

    @Test
    void testValidateIEP_WithValidIEP_ShouldReturnNoErrors() {
        List<String> errors = service.validateIEP(testIEP);

        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateIEP_WithNullStudent_ShouldReturnError() {
        testIEP.setStudent(null);

        List<String> errors = service.validateIEP(testIEP);

        assertNotNull(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Student is required")));
    }

    @Test
    void testValidateIEP_WithNullStartDate_ShouldReturnError() {
        testIEP.setStartDate(null);

        List<String> errors = service.validateIEP(testIEP);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Start date is required")));
    }

    @Test
    void testValidateIEP_WithNullEndDate_ShouldReturnError() {
        testIEP.setEndDate(null);

        List<String> errors = service.validateIEP(testIEP);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("End date is required")));
    }

    @Test
    void testValidateIEP_WithEndDateBeforeStartDate_ShouldReturnError() {
        testIEP.setStartDate(LocalDate.now().plusYears(1));
        testIEP.setEndDate(LocalDate.now());

        List<String> errors = service.validateIEP(testIEP);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("End date must be after start date")));
    }

    @Test
    void testValidateIEP_WithNullStatus_ShouldReturnError() {
        testIEP.setStatus(null);

        List<String> errors = service.validateIEP(testIEP);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Status is required")));
    }

    @Test
    void testCanActivate_WithValidDraftIEP_ShouldReturnTrue() {
        testIEP.setStatus(IEPStatus.DRAFT);
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));

        boolean result = service.canActivate(1L);

        assertTrue(result);
    }

    @Test
    void testCanActivate_WithPendingReviewStatus_ShouldReturnTrue() {
        testIEP.setStatus(IEPStatus.PENDING_REVIEW);
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));

        boolean result = service.canActivate(1L);

        assertTrue(result);
    }

    @Test
    void testCanActivate_WithActiveStatus_ShouldReturnFalse() {
        testIEP.setStatus(IEPStatus.ACTIVE);
        when(iepRepository.findById(anyLong())).thenReturn(Optional.of(testIEP));

        boolean result = service.canActivate(1L);

        assertFalse(result);
    }

    @Test
    void testCanActivate_WithNullIEP_ShouldReturnFalse() {
        when(iepRepository.findById(anyLong())).thenReturn(Optional.empty());

        boolean result = service.canActivate(999L);

        assertFalse(result);
    }

    @Test
    void testHasActiveIEP_WithActiveIEP_ShouldReturnTrue() {
        testIEP.setStatus(IEPStatus.ACTIVE);
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.of(testIEP));

        boolean result = service.hasActiveIEP(1L);

        assertTrue(result);
    }

    @Test
    void testHasActiveIEP_WithNoActiveIEP_ShouldReturnFalse() {
        when(iepRepository.findByStudentId(anyLong())).thenReturn(Optional.empty());

        boolean result = service.hasActiveIEP(999L);

        assertFalse(result);
    }
}
