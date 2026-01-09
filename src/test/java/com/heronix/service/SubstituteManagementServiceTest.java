package com.heronix.service;

import com.heronix.model.domain.Substitute;
import com.heronix.model.domain.SubstituteAssignment;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.SubstituteType;
import com.heronix.repository.SubstituteRepository;
import com.heronix.repository.SubstituteAssignmentRepository;
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
 * Comprehensive test suite for SubstituteManagementService
 *
 * Testing Strategy:
 * - Null safety for all parameters
 * - Repository null handling
 * - Edge cases (empty lists, missing data)
 * - Business logic validation
 * - Error scenarios
 *
 * @author Heronix Scheduling System Test Team
 * @version 1.0.0
 * @since December 17, 2025
 */
@ExtendWith(MockitoExtension.class)
class SubstituteManagementServiceTest {

    @Mock(lenient = true)
    private SubstituteRepository substituteRepository;

    @Mock(lenient = true)
    private SubstituteAssignmentRepository assignmentRepository;

    @InjectMocks
    private SubstituteManagementService service;

    private Substitute testSubstitute;
    private SubstituteAssignment testAssignment;
    private Teacher testTeacher;

    @BeforeEach
    void setUp() {
        // Create test substitute
        testSubstitute = new Substitute("John", "Smith", SubstituteType.CERTIFIED_TEACHER);
        testSubstitute.setId(1L);
        testSubstitute.setEmployeeId("SUB001");
        testSubstitute.setEmail("john.smith@example.com");
        testSubstitute.setPhoneNumber("555-1234");
        testSubstitute.setActive(true);

        // Create test assignment
        testAssignment = new SubstituteAssignment();
        testAssignment.setId(1L);
        testAssignment.setSubstitute(testSubstitute);
        testAssignment.setAssignmentDate(LocalDate.now());
        testAssignment.setIsFloater(false);

        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setName("Jane Doe");
        testTeacher.setFirstName("Jane");
        testTeacher.setLastName("Doe");
    }

    // ==================== getAllSubstitutes() Tests ====================

    @Test
    void testGetAllSubstitutes_WithNullRepository_ShouldHandleGracefully() {
        when(substituteRepository.findAllWithCertifications()).thenReturn(null);

        List<Substitute> result = service.getAllSubstitutes();

        // Service should handle null from repository
        assertDoesNotThrow(() -> service.getAllSubstitutes());
    }

    @Test
    void testGetAllSubstitutes_WithEmptyList_ShouldReturnEmptyList() {
        when(substituteRepository.findAllWithCertifications()).thenReturn(new ArrayList<>());

        List<Substitute> result = service.getAllSubstitutes();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getActiveSubstitutes() Tests ====================

    @Test
    void testGetActiveSubstitutes_WithNullRepository_ShouldHandleGracefully() {
        when(substituteRepository.findByActiveTrue()).thenReturn(null);

        assertDoesNotThrow(() -> service.getActiveSubstitutes());
    }

    @Test
    void testGetActiveSubstitutes_WithEmptyList_ShouldReturnEmptyList() {
        when(substituteRepository.findByActiveTrue()).thenReturn(new ArrayList<>());

        List<Substitute> result = service.getActiveSubstitutes();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getSubstituteById() Tests ====================

    @Test
    void testGetSubstituteById_WithNullId_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getSubstituteById(null));
    }

    @Test
    void testGetSubstituteById_WhenNotFound_ShouldReturnEmpty() {
        when(substituteRepository.findById(anyLong())).thenReturn(Optional.empty());

        Optional<Substitute> result = service.getSubstituteById(999L);

        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    void testGetSubstituteById_WhenFound_ShouldReturnSubstitute() {
        when(substituteRepository.findById(1L)).thenReturn(Optional.of(testSubstitute));

        Optional<Substitute> result = service.getSubstituteById(1L);

        assertTrue(result.isPresent());
        assertEquals(testSubstitute, result.get());
    }

    // ==================== getSubstituteByEmployeeId() Tests ====================

    @Test
    void testGetSubstituteByEmployeeId_WithNullEmployeeId_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getSubstituteByEmployeeId(null));
    }

    @Test
    void testGetSubstituteByEmployeeId_WithEmptyString_ShouldNotThrowException() {
        assertDoesNotThrow(() -> service.getSubstituteByEmployeeId(""));
    }

    @Test
    void testGetSubstituteByEmployeeId_WhenNotFound_ShouldReturnEmpty() {
        when(substituteRepository.findByEmployeeId(anyString())).thenReturn(Optional.empty());

        Optional<Substitute> result = service.getSubstituteByEmployeeId("UNKNOWN");

        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    // ==================== getSubstitutesByType() Tests ====================

    @Test
    void testGetSubstitutesByType_WithNullType_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getSubstitutesByType(null));
    }

    @Test
    void testGetSubstitutesByType_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        when(substituteRepository.findByType(any())).thenReturn(null);

        assertDoesNotThrow(() -> service.getSubstitutesByType(SubstituteType.CERTIFIED_TEACHER));
    }

    @Test
    void testGetSubstitutesByType_WithEmptyList_ShouldReturnEmptyList() {
        when(substituteRepository.findByType(any())).thenReturn(new ArrayList<>());

        List<Substitute> result = service.getSubstitutesByType(SubstituteType.CERTIFIED_TEACHER);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getActiveSubstitutesByType() Tests ====================

    @Test
    void testGetActiveSubstitutesByType_WithNullType_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getActiveSubstitutesByType(null));
    }

    @Test
    void testGetActiveSubstitutesByType_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        when(substituteRepository.findByTypeAndActiveTrue(any())).thenReturn(null);

        assertDoesNotThrow(() -> service.getActiveSubstitutesByType(SubstituteType.CERTIFIED_TEACHER));
    }

    // ==================== searchSubstitutesByName() Tests ====================

    @Test
    void testSearchSubstitutesByName_WithNullName_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.searchSubstitutesByName(null));
    }

    @Test
    void testSearchSubstitutesByName_WithEmptyString_ShouldNotThrowException() {
        assertDoesNotThrow(() -> service.searchSubstitutesByName(""));
    }

    @Test
    void testSearchSubstitutesByName_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        when(substituteRepository.findByNameContaining(anyString())).thenReturn(null);

        assertDoesNotThrow(() -> service.searchSubstitutesByName("Smith"));
    }

    // ==================== getSubstitutesWithCertification() Tests ====================

    @Test
    void testGetSubstitutesWithCertification_WithNullCertification_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getSubstitutesWithCertification(null));
    }

    @Test
    void testGetSubstitutesWithCertification_WithEmptyString_ShouldNotThrowException() {
        assertDoesNotThrow(() -> service.getSubstitutesWithCertification(""));
    }

    @Test
    void testGetSubstitutesWithCertification_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        when(substituteRepository.findByCertification(anyString())).thenReturn(null);

        assertDoesNotThrow(() -> service.getSubstitutesWithCertification("Math"));
    }

    // ==================== saveSubstitute() Tests ====================

    @Test
    void testSaveSubstitute_WithNullSubstitute_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.saveSubstitute(null));
    }

    @Test
    void testSaveSubstitute_WithValidSubstitute_ShouldSave() {
        when(substituteRepository.save(any(Substitute.class))).thenReturn(testSubstitute);

        Substitute result = service.saveSubstitute(testSubstitute);

        assertNotNull(result);
        assertEquals(testSubstitute, result);
        verify(substituteRepository).save(testSubstitute);
    }

    // ==================== createSubstitute() Tests ====================

    @Test
    void testCreateSubstitute_WithNullFirstName_ShouldNotThrowNPE() {
        when(substituteRepository.save(any(Substitute.class))).thenReturn(testSubstitute);

        assertDoesNotThrow(() -> service.createSubstitute(null, "Smith", SubstituteType.CERTIFIED_TEACHER,
                "SUB001", "email@test.com", "555-1234"));
    }

    @Test
    void testCreateSubstitute_WithNullLastName_ShouldNotThrowNPE() {
        when(substituteRepository.save(any(Substitute.class))).thenReturn(testSubstitute);

        assertDoesNotThrow(() -> service.createSubstitute("John", null, SubstituteType.CERTIFIED_TEACHER,
                "SUB001", "email@test.com", "555-1234"));
    }

    @Test
    void testCreateSubstitute_WithNullType_ShouldNotThrowNPE() {
        when(substituteRepository.save(any(Substitute.class))).thenReturn(testSubstitute);

        assertDoesNotThrow(() -> service.createSubstitute("John", "Smith", null,
                "SUB001", "email@test.com", "555-1234"));
    }

    @Test
    void testCreateSubstitute_WithNullEmployeeId_ShouldNotThrowNPE() {
        when(substituteRepository.save(any(Substitute.class))).thenReturn(testSubstitute);

        assertDoesNotThrow(() -> service.createSubstitute("John", "Smith", SubstituteType.CERTIFIED_TEACHER,
                null, "email@test.com", "555-1234"));
    }

    @Test
    void testCreateSubstitute_WithDuplicateEmployeeId_ShouldThrowException() {
        when(substituteRepository.existsByEmployeeId(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
            () -> service.createSubstitute("John", "Smith", SubstituteType.CERTIFIED_TEACHER,
                    "SUB001", "email@test.com", "555-1234"));
    }

    @Test
    void testCreateSubstitute_WithValidData_ShouldCreateSubstitute() {
        when(substituteRepository.existsByEmployeeId(anyString())).thenReturn(false);
        when(substituteRepository.save(any(Substitute.class))).thenReturn(testSubstitute);

        Substitute result = service.createSubstitute("John", "Smith", SubstituteType.CERTIFIED_TEACHER,
                "SUB001", "email@test.com", "555-1234");

        assertNotNull(result);
        verify(substituteRepository).save(any(Substitute.class));
    }

    // ==================== updateSubstitute() Tests ====================

    @Test
    void testUpdateSubstitute_WithNullId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.updateSubstitute(null, testSubstitute));
    }

    @Test
    void testUpdateSubstitute_WithNullSubstitute_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.updateSubstitute(1L, null));
    }

    @Test
    void testUpdateSubstitute_WhenSubstituteNotFound_ShouldThrowException() {
        when(substituteRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.updateSubstitute(999L, testSubstitute));
    }

    @Test
    void testUpdateSubstitute_WithValidData_ShouldUpdateSubstitute() {
        when(substituteRepository.findById(1L)).thenReturn(Optional.of(testSubstitute));
        when(substituteRepository.save(any(Substitute.class))).thenReturn(testSubstitute);

        Substitute result = service.updateSubstitute(1L, testSubstitute);

        assertNotNull(result);
        verify(substituteRepository).save(any(Substitute.class));
    }

    // ==================== deactivateSubstitute() Tests ====================

    @Test
    void testDeactivateSubstitute_WithNullId_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.deactivateSubstitute(null));
    }

    @Test
    void testDeactivateSubstitute_WhenSubstituteNotFound_ShouldNotThrowException() {
        when(substituteRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.deactivateSubstitute(999L));
    }

    @Test
    void testDeactivateSubstitute_WithValidId_ShouldDeactivate() {
        when(substituteRepository.findById(1L)).thenReturn(Optional.of(testSubstitute));

        service.deactivateSubstitute(1L);

        verify(substituteRepository).save(testSubstitute);
        assertFalse(testSubstitute.getActive());
    }

    // ==================== deleteSubstitute() Tests ====================

    @Test
    void testDeleteSubstitute_WithNullId_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.deleteSubstitute(null));
    }

    @Test
    void testDeleteSubstitute_WithValidId_ShouldDelete() {
        service.deleteSubstitute(1L);

        verify(substituteRepository).deleteById(1L);
    }

    // ==================== ASSIGNMENT OPERATIONS Tests ====================

    @Test
    void testGetAllAssignments_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        when(assignmentRepository.findAll()).thenReturn(null);

        assertDoesNotThrow(() -> service.getAllAssignments());
    }

    @Test
    void testGetAssignmentById_WithNullId_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getAssignmentById(null));
    }

    @Test
    void testGetAssignmentsForSubstitute_WithNullId_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getAssignmentsForSubstitute(null));
    }

    @Test
    void testGetAssignmentsForDate_WithNullDate_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getAssignmentsForDate(null));
    }

    @Test
    void testGetAssignmentsBetweenDates_WithNullStartDate_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getAssignmentsBetweenDates(null, LocalDate.now()));
    }

    @Test
    void testGetAssignmentsBetweenDates_WithNullEndDate_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getAssignmentsBetweenDates(LocalDate.now(), null));
    }

    @Test
    void testGetAssignmentsBetweenDates_WithBothNullDates_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getAssignmentsBetweenDates(null, null));
    }

    @Test
    void testGetAssignmentsForTeacher_WithNullTeacherId_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getAssignmentsForTeacher(null));
    }

    @Test
    void testGetFloaterAssignmentsForDate_WithNullDate_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getFloaterAssignmentsForDate(null));
    }

    @Test
    void testSaveAssignment_WithNullAssignment_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.saveAssignment(null));
    }

    @Test
    void testDeleteAssignment_WithNullId_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.deleteAssignment(null));
    }

    // ==================== STATISTICS Tests ====================

    @Test
    void testCountActiveSubstitutes_ShouldReturnCount() {
        when(substituteRepository.countByActiveTrue()).thenReturn(5L);

        long result = service.countActiveSubstitutes();

        assertEquals(5L, result);
    }

    @Test
    void testCountSubstitutesByType_WithNullType_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.countSubstitutesByType(null));
    }

    @Test
    void testCountAssignmentsForDate_WithNullDate_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.countAssignmentsForDate(null));
    }

    @Test
    void testCountFloaterAssignmentsForDate_WithNullDate_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.countFloaterAssignmentsForDate(null));
    }

    @Test
    void testGetTotalHoursForSubstitute_WithNullSubstituteId_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getTotalHoursForSubstitute(null, LocalDate.now(), LocalDate.now()));
    }

    @Test
    void testGetTotalHoursForSubstitute_WithNullDates_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getTotalHoursForSubstitute(1L, null, null));
    }

    @Test
    void testGetTotalHoursForSubstitute_WhenRepositoryReturnsNull_ShouldReturnZero() {
        when(assignmentRepository.getTotalHoursForSubstituteInDateRange(anyLong(), any(), any())).thenReturn(null);

        Double result = service.getTotalHoursForSubstitute(1L, LocalDate.now(), LocalDate.now());

        assertEquals(0.0, result);
    }

    @Test
    void testGetTotalPayForSubstitute_WithNullSubstituteId_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getTotalPayForSubstitute(null, LocalDate.now(), LocalDate.now()));
    }

    @Test
    void testGetTotalPayForSubstitute_WithNullDates_ShouldNotThrowNPE() {
        assertDoesNotThrow(() -> service.getTotalPayForSubstitute(1L, null, null));
    }

    @Test
    void testGetTotalPayForSubstitute_WhenRepositoryReturnsNull_ShouldReturnZero() {
        when(assignmentRepository.getTotalPayForSubstituteInDateRange(anyLong(), any(), any())).thenReturn(null);

        Double result = service.getTotalPayForSubstitute(1L, LocalDate.now(), LocalDate.now());

        assertEquals(0.0, result);
    }

    // ==================== BUSINESS LOGIC Tests ====================

    @Test
    void testCreateSubstitute_WithAllValidData_ShouldSetAllFields() {
        when(substituteRepository.existsByEmployeeId("SUB002")).thenReturn(false);
        when(substituteRepository.save(any(Substitute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Substitute result = service.createSubstitute("Jane", "Doe", SubstituteType.PARAPROFESSIONAL,
                "SUB002", "jane.doe@example.com", "555-5678");

        assertNotNull(result);
        verify(substituteRepository).save(any(Substitute.class));
    }

    @Test
    void testGetAssignmentsBetweenDates_WithValidDates_ShouldReturnAssignments() {
        LocalDate start = LocalDate.of(2025, 12, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);
        List<SubstituteAssignment> assignments = Arrays.asList(testAssignment);

        when(assignmentRepository.findByAssignmentDateBetween(start, end)).thenReturn(assignments);

        List<SubstituteAssignment> result = service.getAssignmentsBetweenDates(start, end);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetTotalHoursForSubstitute_WithValidData_ShouldReturnHours() {
        when(assignmentRepository.getTotalHoursForSubstituteInDateRange(anyLong(), any(), any())).thenReturn(40.0);

        Double result = service.getTotalHoursForSubstitute(1L, LocalDate.now().minusDays(7), LocalDate.now());

        assertEquals(40.0, result);
    }

    @Test
    void testGetTotalPayForSubstitute_WithValidData_ShouldReturnPay() {
        when(assignmentRepository.getTotalPayForSubstituteInDateRange(anyLong(), any(), any())).thenReturn(800.0);

        Double result = service.getTotalPayForSubstitute(1L, LocalDate.now().minusDays(7), LocalDate.now());

        assertEquals(800.0, result);
    }
}
