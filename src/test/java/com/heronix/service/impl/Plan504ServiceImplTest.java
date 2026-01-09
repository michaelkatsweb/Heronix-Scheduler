package com.heronix.service.impl;

import com.heronix.model.domain.Plan504;
import com.heronix.model.domain.Student;
import com.heronix.model.enums.Plan504Status;
import com.heronix.repository.Plan504Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for Plan504ServiceImpl
 *
 * Tests 504 plan CRUD operations, status management, queries, validation, and null safety.
 */
@ExtendWith(MockitoExtension.class)
class Plan504ServiceImplTest {

    @Mock(lenient = true)
    private Plan504Repository plan504Repository;

    @InjectMocks
    private Plan504ServiceImpl service;

    private Plan504 testPlan;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setStudentId("STU001");

        testPlan = new Plan504();
        testPlan.setId(1L);
        testPlan.setStudent(testStudent);
        testPlan.setPlanNumber("504-2024-001");
        testPlan.setStartDate(LocalDate.now());
        testPlan.setEndDate(LocalDate.now().plusYears(1));
        testPlan.setNextReviewDate(LocalDate.now().plusMonths(6));
        testPlan.setDisability("ADHD");
        testPlan.setCoordinator("Jane Smith");
        testPlan.setStatus(Plan504Status.DRAFT);
        testPlan.setAccommodations("Extended time on tests, preferential seating");
        testPlan.setNotes("Student responds well to structured environment");

        // Default repository stubbing
        when(plan504Repository.save(any(Plan504.class)))
            .thenAnswer(invocation -> {
                Plan504 plan = invocation.getArgument(0);
                if (plan.getId() == null) {
                    plan.setId(1L);
                }
                return plan;
            });

        when(plan504Repository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(plan504Repository.existsById(1L)).thenReturn(true);
    }

    // ========== CREATE OPERATIONS ==========

    @Test
    void testCreatePlan_WithValidPlan_ShouldCreateSuccessfully() {
        testPlan.setId(null); // New plan doesn't have ID
        when(plan504Repository.findByStudentId(1L)).thenReturn(Optional.empty());

        Plan504 result = service.createPlan(testPlan);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(plan504Repository).save(testPlan);
    }

    @Test
    void testCreatePlan_WithNullPlan_ShouldThrowException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.createPlan(null);
        });

        assertTrue(exception.getMessage().contains("Plan cannot be null"));
        verify(plan504Repository, never()).save(any());
    }

    @Test
    void testCreatePlan_WithNullStudent_ShouldThrowException() {
        testPlan.setStudent(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.createPlan(testPlan);
        });

        assertTrue(exception.getMessage().contains("validation failed"));
        assertTrue(exception.getMessage().contains("Student is required"));
    }

    @Test
    void testCreatePlan_WithNullStartDate_ShouldThrowException() {
        testPlan.setStartDate(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.createPlan(testPlan);
        });

        assertTrue(exception.getMessage().contains("Start date is required"));
    }

    @Test
    void testCreatePlan_WithNullEndDate_ShouldThrowException() {
        testPlan.setEndDate(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.createPlan(testPlan);
        });

        assertTrue(exception.getMessage().contains("End date is required"));
    }

    @Test
    void testCreatePlan_WithEndBeforeStart_ShouldThrowException() {
        testPlan.setStartDate(LocalDate.now());
        testPlan.setEndDate(LocalDate.now().minusDays(1));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.createPlan(testPlan);
        });

        assertTrue(exception.getMessage().contains("End date must be after start date"));
    }

    @Test
    void testCreatePlan_WithNullAccommodations_ShouldThrowException() {
        testPlan.setAccommodations(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.createPlan(testPlan);
        });

        assertTrue(exception.getMessage().contains("Accommodations are required"));
    }

    @Test
    void testCreatePlan_WithEmptyAccommodations_ShouldThrowException() {
        testPlan.setAccommodations("   ");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.createPlan(testPlan);
        });

        assertTrue(exception.getMessage().contains("Accommodations are required"));
    }

    @Test
    void testCreatePlan_WithExistingActivePlan_ShouldThrowException() {
        Plan504 activePlan = new Plan504();
        activePlan.setId(2L);
        activePlan.setStatus(Plan504Status.ACTIVE);
        activePlan.setEndDate(LocalDate.now().plusYears(1));

        when(plan504Repository.findByStudentId(1L)).thenReturn(Optional.of(activePlan));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            service.createPlan(testPlan);
        });

        assertTrue(exception.getMessage().contains("already has an active 504 Plan"));
    }

    @Test
    void testCreatePlan_WithNullStatus_ShouldSetToDraft() {
        testPlan.setId(null);
        testPlan.setStatus(null);
        when(plan504Repository.findByStudentId(1L)).thenReturn(Optional.empty());

        Plan504 result = service.createPlan(testPlan);

        assertEquals(Plan504Status.DRAFT, result.getStatus());
    }

    @Test
    void testCreatePlan_WithNullStudentId_ShouldNotCheckActivePlan() {
        testPlan.setId(null);
        testPlan.getStudent().setId(null);

        // Should not crash, just skip active plan check
        Plan504 result = service.createPlan(testPlan);

        assertNotNull(result);
        verify(plan504Repository, never()).findByStudentId(any());
    }

    // ========== UPDATE OPERATIONS ==========

    @Test
    void testUpdatePlan_WithValidPlan_ShouldUpdateSuccessfully() {
        testPlan.setAccommodations("Updated accommodations");

        Plan504 result = service.updatePlan(testPlan);

        assertNotNull(result);
        verify(plan504Repository).save(testPlan);
    }

    @Test
    void testUpdatePlan_WithNullPlan_ShouldThrowException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.updatePlan(null);
        });

        assertTrue(exception.getMessage().contains("Plan cannot be null"));
    }

    @Test
    void testUpdatePlan_WithNullId_ShouldThrowException() {
        testPlan.setId(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.updatePlan(testPlan);
        });

        assertTrue(exception.getMessage().contains("Plan ID cannot be null"));
    }

    @Test
    void testUpdatePlan_WithNonExistentId_ShouldThrowException() {
        testPlan.setId(999L);
        when(plan504Repository.existsById(999L)).thenReturn(false);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.updatePlan(testPlan);
        });

        assertTrue(exception.getMessage().contains("504 Plan not found"));
    }

    @Test
    void testUpdatePlan_WithInvalidData_ShouldThrowException() {
        testPlan.setAccommodations(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.updatePlan(testPlan);
        });

        assertTrue(exception.getMessage().contains("validation failed"));
    }

    // ========== FIND OPERATIONS ==========

    @Test
    void testFindById_WithValidId_ShouldReturnPlan() {
        Optional<Plan504> result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(plan504Repository).findById(1L);
    }

    @Test
    void testFindById_WithNullId_ShouldQueryRepository() {
        service.findById(null);

        verify(plan504Repository).findById(null);
    }

    @Test
    void testFindByPlanNumber_WithValidNumber_ShouldReturnPlan() {
        when(plan504Repository.findByPlanNumber("504-2024-001"))
            .thenReturn(Optional.of(testPlan));

        Optional<Plan504> result = service.findByPlanNumber("504-2024-001");

        assertTrue(result.isPresent());
        verify(plan504Repository).findByPlanNumber("504-2024-001");
    }

    @Test
    void testFindActivePlanForStudent_WithActivePlan_ShouldReturnPlan() {
        testPlan.setStatus(Plan504Status.ACTIVE);
        testPlan.setEndDate(LocalDate.now().plusYears(1));
        when(plan504Repository.findByStudentId(1L)).thenReturn(Optional.of(testPlan));

        Optional<Plan504> result = service.findActivePlanForStudent(1L);

        assertTrue(result.isPresent());
        verify(plan504Repository).findByStudentId(1L);
    }

    @Test
    void testFindActivePlanForStudent_WithExpiredPlan_ShouldReturnEmpty() {
        testPlan.setStatus(Plan504Status.ACTIVE);
        testPlan.setEndDate(LocalDate.now().minusDays(1)); // Expired
        when(plan504Repository.findByStudentId(1L)).thenReturn(Optional.of(testPlan));

        Optional<Plan504> result = service.findActivePlanForStudent(1L);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindActivePlanForStudent_WithNullPlan_ShouldReturnEmpty() {
        when(plan504Repository.findByStudentId(1L)).thenReturn(Optional.empty());

        Optional<Plan504> result = service.findActivePlanForStudent(1L);

        assertFalse(result.isPresent());
    }

    // ========== DELETE OPERATIONS ==========

    @Test
    void testDeletePlan_WithDraftStatus_ShouldDeleteSuccessfully() {
        testPlan.setStatus(Plan504Status.DRAFT);

        service.deletePlan(1L);

        verify(plan504Repository).delete(testPlan);
    }

    @Test
    void testDeletePlan_WithActiveStatus_ShouldThrowException() {
        testPlan.setStatus(Plan504Status.ACTIVE);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            service.deletePlan(1L);
        });

        assertTrue(exception.getMessage().contains("Cannot delete 504 Plan with status: ACTIVE"));
        verify(plan504Repository, never()).delete(any());
    }

    @Test
    void testDeletePlan_WithNonExistentId_ShouldThrowException() {
        when(plan504Repository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.deletePlan(999L);
        });

        assertTrue(exception.getMessage().contains("504 Plan not found"));
    }

    @Test
    void testDeletePlan_WithNullStatus_ShouldDefaultToDraft() {
        testPlan.setStatus(null);

        // Should treat as DRAFT and allow deletion
        service.deletePlan(1L);

        verify(plan504Repository).delete(testPlan);
    }

    // ========== STATUS MANAGEMENT ==========

    @Test
    void testActivatePlan_WithValidPlan_ShouldActivateSuccessfully() {
        testPlan.setStatus(Plan504Status.DRAFT);

        Plan504 result = service.activatePlan(1L);

        assertEquals(Plan504Status.ACTIVE, result.getStatus());
        verify(plan504Repository).save(testPlan);
    }

    @Test
    void testActivatePlan_WithInvalidPlan_ShouldThrowException() {
        testPlan.setAccommodations(null); // Invalid

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            service.activatePlan(1L);
        });

        assertTrue(exception.getMessage().contains("cannot be activated"));
    }

    @Test
    void testActivatePlan_WithNonExistentId_ShouldThrowException() {
        when(plan504Repository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.activatePlan(999L);
        });

        assertTrue(exception.getMessage().contains("504 Plan not found"));
    }

    @Test
    void testExpirePlan_ShouldSetStatusToExpired() {
        Plan504 result = service.expirePlan(1L);

        assertEquals(Plan504Status.EXPIRED, result.getStatus());
        verify(plan504Repository).save(testPlan);
    }

    @Test
    void testMarkForReview_ShouldSetStatusToPendingReview() {
        Plan504 result = service.markForReview(1L);

        assertEquals(Plan504Status.PENDING_REVIEW, result.getStatus());
        verify(plan504Repository).save(testPlan);
    }

    @Test
    void testExpireOutdatedPlans_ShouldExpireMultiplePlans() {
        Plan504 plan1 = new Plan504();
        plan1.setId(1L);
        plan1.setStatus(Plan504Status.ACTIVE);

        Plan504 plan2 = new Plan504();
        plan2.setId(2L);
        plan2.setStatus(Plan504Status.ACTIVE);

        List<Plan504> expiredPlans = Arrays.asList(plan1, plan2);
        when(plan504Repository.findExpiredPlans(any(LocalDate.class))).thenReturn(expiredPlans);

        int count = service.expireOutdatedPlans();

        assertEquals(2, count);
        assertEquals(Plan504Status.EXPIRED, plan1.getStatus());
        assertEquals(Plan504Status.EXPIRED, plan2.getStatus());
        verify(plan504Repository, times(2)).save(any(Plan504.class));
    }

    @Test
    void testExpireOutdatedPlans_WithNullInList_ShouldSkipNull() {
        Plan504 plan1 = new Plan504();
        plan1.setId(1L);
        plan1.setStatus(Plan504Status.ACTIVE);

        List<Plan504> expiredPlans = Arrays.asList(plan1, null);
        when(plan504Repository.findExpiredPlans(any(LocalDate.class))).thenReturn(expiredPlans);

        int count = service.expireOutdatedPlans();

        assertEquals(1, count); // Only counted non-null plan
        verify(plan504Repository, times(1)).save(plan1);
    }

    // ========== QUERY OPERATIONS ==========

    @Test
    void testFindAllActivePlans_ShouldReturnActivePlans() {
        List<Plan504> activePlans = Arrays.asList(testPlan);
        when(plan504Repository.findAllActivePlans(any(LocalDate.class))).thenReturn(activePlans);

        List<Plan504> result = service.findAllActivePlans();

        assertEquals(1, result.size());
        verify(plan504Repository).findAllActivePlans(any(LocalDate.class));
    }

    @Test
    void testFindByStatus_ShouldReturnPlansByStatus() {
        List<Plan504> draftPlans = Arrays.asList(testPlan);
        when(plan504Repository.findByStatus(Plan504Status.DRAFT)).thenReturn(draftPlans);

        List<Plan504> result = service.findByStatus(Plan504Status.DRAFT);

        assertEquals(1, result.size());
        verify(plan504Repository).findByStatus(Plan504Status.DRAFT);
    }

    @Test
    void testFindPlansNeedingRenewal_ShouldReturnPlans() {
        List<Plan504> plansNeedingRenewal = Arrays.asList(testPlan);
        when(plan504Repository.findPlansNeedingRenewal(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(plansNeedingRenewal);

        List<Plan504> result = service.findPlansNeedingRenewal(30);

        assertEquals(1, result.size());
        verify(plan504Repository).findPlansNeedingRenewal(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testFindPlansWithOverdueReview_ShouldReturnPlans() {
        List<Plan504> overduePlans = Arrays.asList(testPlan);
        when(plan504Repository.findPlansWithOverdueReview(any(LocalDate.class)))
            .thenReturn(overduePlans);

        List<Plan504> result = service.findPlansWithOverdueReview();

        assertEquals(1, result.size());
        verify(plan504Repository).findPlansWithOverdueReview(any(LocalDate.class));
    }

    @Test
    void testFindByCoordinator_ShouldReturnPlans() {
        List<Plan504> coordinatorPlans = Arrays.asList(testPlan);
        when(plan504Repository.findByCoordinator("Jane Smith")).thenReturn(coordinatorPlans);

        List<Plan504> result = service.findByCoordinator("Jane Smith");

        assertEquals(1, result.size());
        verify(plan504Repository).findByCoordinator("Jane Smith");
    }

    @Test
    void testFindByDisability_ShouldReturnPlans() {
        List<Plan504> disabilityPlans = Arrays.asList(testPlan);
        when(plan504Repository.findByDisability("ADHD")).thenReturn(disabilityPlans);

        List<Plan504> result = service.findByDisability("ADHD");

        assertEquals(1, result.size());
        verify(plan504Repository).findByDisability("ADHD");
    }

    @Test
    void testSearchByStudentName_ShouldReturnPlans() {
        List<Plan504> searchResults = Arrays.asList(testPlan);
        when(plan504Repository.searchByStudentName("John")).thenReturn(searchResults);

        List<Plan504> result = service.searchByStudentName("John");

        assertEquals(1, result.size());
        verify(plan504Repository).searchByStudentName("John");
    }

    // ========== STATISTICS ==========

    @Test
    void testCountActivePlans_ShouldReturnCount() {
        when(plan504Repository.countActivePlans(any(LocalDate.class))).thenReturn(5L);

        long count = service.countActivePlans();

        assertEquals(5L, count);
        verify(plan504Repository).countActivePlans(any(LocalDate.class));
    }

    @Test
    void testGetPlanCountByDisability_ShouldReturnCounts() {
        List<Object[]> counts = Arrays.asList(
            new Object[]{"ADHD", 5L},
            new Object[]{"Diabetes", 3L}
        );
        when(plan504Repository.countByDisability()).thenReturn(counts);

        List<Object[]> result = service.getPlanCountByDisability();

        assertEquals(2, result.size());
        verify(plan504Repository).countByDisability();
    }

    // ========== VALIDATION ==========

    @Test
    void testValidatePlan_WithValidPlan_ShouldReturnNoErrors() {
        List<String> errors = service.validatePlan(testPlan);

        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidatePlan_WithMultipleErrors_ShouldReturnAllErrors() {
        testPlan.setStudent(null);
        testPlan.setStartDate(null);
        testPlan.setEndDate(null);
        testPlan.setAccommodations(null);
        testPlan.setStatus(null);

        List<String> errors = service.validatePlan(testPlan);

        assertEquals(5, errors.size());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Student is required")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Start date is required")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("End date is required")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Accommodations are required")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Status is required")));
    }

    @Test
    void testCanActivate_WithValidDraftPlan_ShouldReturnTrue() {
        testPlan.setStatus(Plan504Status.DRAFT);

        boolean result = service.canActivate(1L);

        assertTrue(result);
    }

    @Test
    void testCanActivate_WithValidPendingReviewPlan_ShouldReturnTrue() {
        testPlan.setStatus(Plan504Status.PENDING_REVIEW);

        boolean result = service.canActivate(1L);

        assertTrue(result);
    }

    @Test
    void testCanActivate_WithActivePlan_ShouldReturnFalse() {
        testPlan.setStatus(Plan504Status.ACTIVE);

        boolean result = service.canActivate(1L);

        assertFalse(result);
    }

    @Test
    void testCanActivate_WithExpiredPlan_ShouldReturnFalse() {
        testPlan.setStatus(Plan504Status.EXPIRED);

        boolean result = service.canActivate(1L);

        assertFalse(result);
    }

    @Test
    void testCanActivate_WithInvalidPlan_ShouldReturnFalse() {
        testPlan.setAccommodations(null); // Invalid

        boolean result = service.canActivate(1L);

        assertFalse(result);
    }

    @Test
    void testCanActivate_WithNonExistentId_ShouldReturnFalse() {
        when(plan504Repository.findById(999L)).thenReturn(Optional.empty());

        boolean result = service.canActivate(999L);

        assertFalse(result);
    }

    @Test
    void testHasActivePlan_WithActivePlan_ShouldReturnTrue() {
        testPlan.setStatus(Plan504Status.ACTIVE);
        testPlan.setEndDate(LocalDate.now().plusYears(1));
        when(plan504Repository.findByStudentId(1L)).thenReturn(Optional.of(testPlan));

        boolean result = service.hasActivePlan(1L);

        assertTrue(result);
    }

    @Test
    void testHasActivePlan_WithoutActivePlan_ShouldReturnFalse() {
        when(plan504Repository.findByStudentId(1L)).thenReturn(Optional.empty());

        boolean result = service.hasActivePlan(1L);

        assertFalse(result);
    }

    // ========== EDGE CASES & NULL SAFETY ==========

    @Test
    void testCreatePlan_WithNullStudentAndNullStudentId_ShouldLogSafely() {
        testPlan.setId(null);
        Student studentWithNullId = new Student();
        studentWithNullId.setId(null);
        testPlan.setStudent(studentWithNullId);

        // Should not crash when logging student info
        Plan504 result = service.createPlan(testPlan);

        assertNotNull(result);
    }

    @Test
    void testUpdatePlan_WithNullIdInPlan_ShouldLogSafely() {
        testPlan.setId(null);

        // Should not crash when logging plan ID as "null"
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.updatePlan(testPlan);
        });

        assertTrue(exception.getMessage().contains("Plan ID cannot be null"));
    }
}
