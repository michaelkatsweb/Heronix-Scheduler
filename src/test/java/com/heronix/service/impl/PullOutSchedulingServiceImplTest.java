package com.heronix.service.impl;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.PullOutSchedule;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.PullOutStatus;
import com.heronix.model.enums.ServiceStatus;
import com.heronix.model.enums.ServiceType;
import com.heronix.repository.IEPServiceRepository;
import com.heronix.repository.PullOutScheduleRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.PullOutSchedulingService.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for PullOutSchedulingServiceImpl
 *
 * Tests special education pull-out scheduling including conflict detection,
 * time slot finding, and workload tracking.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PullOutSchedulingServiceImplTest {

    @Mock
    private PullOutScheduleRepository pullOutScheduleRepository;

    @Mock
    private IEPServiceRepository iepServiceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private PullOutSchedulingServiceImpl service;

    private PullOutSchedule testSchedule;
    private IEPService testIEPService;
    private IEP testIEP;
    private Student testStudent;
    private Teacher testStaff;

    @BeforeEach
    void setUp() {
        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("John");
        testStudent.setLastName("Doe");
        testStudent.setStudentId("STU-001");
        testStudent.setGradeLevel("9");

        // Create test staff
        testStaff = new Teacher();
        testStaff.setId(1L);
        testStaff.setName("Jane Smith");
        testStaff.setEmployeeId("EMP-001");

        // Create test IEP
        testIEP = new IEP();
        testIEP.setId(1L);
        testIEP.setStudent(testStudent);

        // Create test IEP service
        testIEPService = new IEPService();
        testIEPService.setId(1L);
        testIEPService.setIep(testIEP);
        testIEPService.setAssignedStaff(testStaff);
        testIEPService.setServiceType(ServiceType.SPEECH_THERAPY);
        testIEPService.setSessionDurationMinutes(30);
        testIEPService.setMinutesPerWeek(60);
        testIEPService.setFrequency("2x per week");
        testIEPService.setStatus(ServiceStatus.NOT_SCHEDULED);

        // Create test pull-out schedule
        testSchedule = new PullOutSchedule();
        testSchedule.setId(1L);
        testSchedule.setIepService(testIEPService);
        testSchedule.setStudent(testStudent);
        testSchedule.setStaff(testStaff);
        testSchedule.setDayOfWeek("MONDAY");
        testSchedule.setStartTime(LocalTime.of(9, 0));
        testSchedule.setEndTime(LocalTime.of(9, 30));
        testSchedule.setDurationMinutes(30);
        testSchedule.setRecurring(true);
        testSchedule.setStartDate(LocalDate.now());
        testSchedule.setStatus(PullOutStatus.ACTIVE);

        // Mock repositories
        when(pullOutScheduleRepository.save(any(PullOutSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(iepServiceRepository.save(any(IEPService.class))).thenAnswer(i -> i.getArgument(0));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testStaff));
        when(iepServiceRepository.findById(1L)).thenReturn(Optional.of(testIEPService));
        when(pullOutScheduleRepository.existsById(1L)).thenReturn(true);
        when(pullOutScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
    }

    // ========== CREATE SCHEDULE TESTS ==========

    @Test
    void testCreateSchedule_WithValidSchedule_ShouldSaveSuccessfully() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());

        PullOutSchedule result = service.createSchedule(testSchedule);

        assertNotNull(result);
        assertEquals(PullOutStatus.ACTIVE, result.getStatus());
        verify(pullOutScheduleRepository, times(1)).save(testSchedule);
    }

    @Test
    void testCreateSchedule_WithNullSchedule_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(null));
    }

    @Test
    void testCreateSchedule_WithMissingStudent_ShouldThrowException() {
        testSchedule.setStudent(null);

        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(testSchedule));
    }

    @Test
    void testCreateSchedule_WithMissingStaff_ShouldThrowException() {
        testSchedule.setStaff(null);

        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(testSchedule));
    }

    @Test
    void testCreateSchedule_WithMissingIEPService_ShouldThrowException() {
        testSchedule.setIepService(null);

        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(testSchedule));
    }

    @Test
    void testCreateSchedule_WithConflict_ShouldThrowException() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(Arrays.asList(testSchedule));

        assertThrows(IllegalStateException.class, () -> service.createSchedule(testSchedule));
    }

    @Test
    void testCreateSchedule_WithEndBeforeStart_ShouldThrowException() {
        testSchedule.setStartTime(LocalTime.of(10, 0));
        testSchedule.setEndTime(LocalTime.of(9, 0));

        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(testSchedule));
    }

    @Test
    void testCreateSchedule_ShouldSetDefaultValues() {
        testSchedule.setStatus(null);
        testSchedule.setRecurring(null);
        // Note: Cannot set startDate to null as it's required by validation
        // This test validates that status and recurring get defaults

        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());

        PullOutSchedule result = service.createSchedule(testSchedule);

        assertEquals(PullOutStatus.ACTIVE, result.getStatus());
        assertEquals(true, result.getRecurring());
        assertNotNull(result.getStartDate());
    }

    // ========== UPDATE SCHEDULE TESTS ==========

    @Test
    void testUpdateSchedule_WithValidSchedule_ShouldUpdateSuccessfully() {
        PullOutSchedule result = service.updateSchedule(testSchedule);

        assertNotNull(result);
        verify(pullOutScheduleRepository, times(1)).save(testSchedule);
    }

    @Test
    void testUpdateSchedule_WithNullSchedule_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> service.updateSchedule(null));
    }

    @Test
    void testUpdateSchedule_WithNullId_ShouldThrowException() {
        testSchedule.setId(null);

        assertThrows(IllegalArgumentException.class, () -> service.updateSchedule(testSchedule));
    }

    @Test
    void testUpdateSchedule_WithNonExistentId_ShouldThrowException() {
        when(pullOutScheduleRepository.existsById(999L)).thenReturn(false);
        testSchedule.setId(999L);

        assertThrows(IllegalArgumentException.class, () -> service.updateSchedule(testSchedule));
    }

    // ========== DELETE SCHEDULE TESTS ==========

    @Test
    void testDeleteSchedule_WithValidId_ShouldDeleteSuccessfully() {
        when(pullOutScheduleRepository.findByIepService(testIEPService)).thenReturn(new ArrayList<>());

        service.deleteSchedule(1L);

        verify(pullOutScheduleRepository, times(1)).delete(testSchedule);
    }

    @Test
    void testDeleteSchedule_WithNonExistentId_ShouldThrowException() {
        when(pullOutScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteSchedule(999L));
    }

    // ========== SCHEDULE SERVICE TESTS ==========

    @Test
    void testScheduleService_WithValidService_ShouldCreateSchedule() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());

        PullOutSchedule result = service.scheduleService(1L);

        assertNotNull(result);
        assertEquals(testStudent, result.getStudent());
        assertEquals(testStaff, result.getStaff());
        verify(pullOutScheduleRepository, times(1)).save(any(PullOutSchedule.class));
    }

    @Test
    void testScheduleService_WithNullServiceId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> service.scheduleService(null));
    }

    @Test
    void testScheduleService_WithNonExistentService_ShouldThrowException() {
        when(iepServiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.scheduleService(999L));
    }

    @Test
    void testScheduleService_WithNoAssignedStaff_ShouldThrowException() {
        testIEPService.setAssignedStaff(null);

        assertThrows(IllegalStateException.class, () -> service.scheduleService(1L));
    }

    @Test
    void testScheduleService_WithNoStudent_ShouldThrowException() {
        testIEPService.setIep(null);

        assertThrows(IllegalStateException.class, () -> service.scheduleService(1L));
    }

    @Test
    void testScheduleService_WithNoAvailableTimeSlots_ShouldThrowException() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(Arrays.asList(testSchedule));
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(Arrays.asList(testSchedule));

        assertThrows(IllegalStateException.class, () -> service.scheduleService(1L));
    }

    // ========== SCHEDULE ALL SERVICES TESTS ==========

    @Test
    void testScheduleAllServicesForStudent_WithValidStudent_ShouldScheduleAll() {
        when(iepServiceRepository.findStudentServicesNeedingScheduling(1L))
            .thenReturn(Arrays.asList(testIEPService));
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());

        List<PullOutSchedule> result = service.scheduleAllServicesForStudent(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testScheduleAllServicesForStudent_WithNullStudentId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> service.scheduleAllServicesForStudent(null));
    }

    @Test
    void testScheduleAllServicesForStudent_WithNonExistentStudent_ShouldThrowException() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.scheduleAllServicesForStudent(999L));
    }

    @Test
    void testScheduleAllServicesForStudent_WithNoServices_ShouldReturnEmptyList() {
        when(iepServiceRepository.findStudentServicesNeedingScheduling(1L)).thenReturn(new ArrayList<>());

        List<PullOutSchedule> result = service.scheduleAllServicesForStudent(1L);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testScheduleAllServicesForStudent_WithServicesMissingStaff_ShouldSkipThem() {
        testIEPService.setAssignedStaff(null);
        when(iepServiceRepository.findStudentServicesNeedingScheduling(1L))
            .thenReturn(Arrays.asList(testIEPService));

        List<PullOutSchedule> result = service.scheduleAllServicesForStudent(1L);

        assertNotNull(result);
        assertEquals(0, result.size()); // Should skip services without staff
    }

    // ========== RESCHEDULE TESTS ==========

    @Test
    void testReschedule_WithValidParameters_ShouldRescheduleSuccessfully() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());

        PullOutSchedule result = service.reschedule(1L, "TUESDAY", LocalTime.of(10, 0), LocalTime.of(10, 30));

        assertNotNull(result);
        assertEquals("TUESDAY", result.getDayOfWeek());
        assertEquals(LocalTime.of(10, 0), result.getStartTime());
        assertEquals(PullOutStatus.RESCHEDULED, result.getStatus());
    }

    @Test
    void testReschedule_WithNullScheduleId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.reschedule(null, "TUESDAY", LocalTime.of(10, 0), LocalTime.of(10, 30)));
    }

    @Test
    void testReschedule_WithConflict_ShouldThrowException() {
        PullOutSchedule conflictSchedule = new PullOutSchedule();
        conflictSchedule.setId(2L);

        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(Arrays.asList(conflictSchedule));

        assertThrows(IllegalStateException.class, () ->
            service.reschedule(1L, "TUESDAY", LocalTime.of(10, 0), LocalTime.of(10, 30)));
    }

    @Test
    void testReschedule_ShouldIgnoreSelfConflict() {
        // Use mutable list for removeIf operation
        List<PullOutSchedule> studentConflicts = new ArrayList<>(Arrays.asList(testSchedule));
        List<PullOutSchedule> staffConflicts = new ArrayList<>();

        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(studentConflicts);
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(staffConflicts);

        PullOutSchedule result = service.reschedule(1L, "TUESDAY", LocalTime.of(10, 0), LocalTime.of(10, 30));

        assertNotNull(result);
        assertEquals("TUESDAY", result.getDayOfWeek());
    }

    // ========== CANCEL SCHEDULE TESTS ==========

    @Test
    void testCancelSchedule_WithValidId_ShouldCancelSuccessfully() {
        when(pullOutScheduleRepository.findByIepService(testIEPService)).thenReturn(new ArrayList<>());

        PullOutSchedule result = service.cancelSchedule(1L);

        assertNotNull(result);
        assertEquals(PullOutStatus.CANCELLED, result.getStatus());
    }

    @Test
    void testCancelSchedule_WithNonExistentId_ShouldThrowException() {
        when(pullOutScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.cancelSchedule(999L));
    }

    // ========== CONFLICT DETECTION TESTS ==========

    @Test
    void testCheckStudentConflicts_WithConflicts_ShouldReturnList() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(Arrays.asList(testSchedule));

        List<PullOutSchedule> result = service.checkStudentConflicts(
            1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(9, 30));

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testCheckStudentConflicts_WithNullStudentId_ShouldReturnEmptyList() {
        List<PullOutSchedule> result = service.checkStudentConflicts(
            null, "MONDAY", LocalTime.of(9, 0), LocalTime.of(9, 30));

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testCheckStudentConflicts_WithNoConflicts_ShouldReturnEmptyList() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());

        List<PullOutSchedule> result = service.checkStudentConflicts(
            1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(9, 30));

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testCheckStaffConflicts_WithConflicts_ShouldReturnList() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(Arrays.asList(testSchedule));

        List<PullOutSchedule> result = service.checkStaffConflicts(
            1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(9, 30));

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testCheckStaffConflicts_WithNullStaffId_ShouldReturnEmptyList() {
        List<PullOutSchedule> result = service.checkStaffConflicts(
            null, "MONDAY", LocalTime.of(9, 0), LocalTime.of(9, 30));

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testIsTimeSlotAvailable_WithNoConflicts_ShouldReturnTrue() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());

        boolean result = service.isTimeSlotAvailable(
            1L, 1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(9, 30));

        assertTrue(result);
    }

    @Test
    void testIsTimeSlotAvailable_WithNullStudentId_ShouldReturnFalse() {
        boolean result = service.isTimeSlotAvailable(
            null, 1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(9, 30));

        assertFalse(result);
    }

    @Test
    void testIsTimeSlotAvailable_WithNullStaffId_ShouldReturnFalse() {
        boolean result = service.isTimeSlotAvailable(
            1L, null, "MONDAY", LocalTime.of(9, 0), LocalTime.of(9, 30));

        assertFalse(result);
    }

    @Test
    void testIsTimeSlotAvailable_WithStudentConflict_ShouldReturnFalse() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(Arrays.asList(testSchedule));

        boolean result = service.isTimeSlotAvailable(
            1L, 1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(9, 30));

        assertFalse(result);
    }

    @Test
    void testIsTimeSlotAvailable_WithStaffConflict_ShouldReturnFalse() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(Arrays.asList(testSchedule));

        boolean result = service.isTimeSlotAvailable(
            1L, 1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(9, 30));

        assertFalse(result);
    }

    @Test
    void testFindAllStudentConflicts_WithConflicts_ShouldReturnList() {
        PullOutSchedule schedule2 = new PullOutSchedule();
        schedule2.setId(2L);
        schedule2.setDayOfWeek("MONDAY");
        schedule2.setStartTime(LocalTime.of(9, 15));
        schedule2.setEndTime(LocalTime.of(9, 45));

        when(pullOutScheduleRepository.findActiveByStudent(testStudent, LocalDate.now()))
            .thenReturn(Arrays.asList(testSchedule, schedule2));

        List<Map<String, PullOutSchedule>> result = service.findAllStudentConflicts(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindAllStudentConflicts_WithNullStudentId_ShouldReturnEmptyList() {
        List<Map<String, PullOutSchedule>> result = service.findAllStudentConflicts(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ========== QUERY TESTS ==========

    @Test
    void testGetStudentSchedule_WithValidStudent_ShouldReturnSchedules() {
        when(pullOutScheduleRepository.findActiveByStudent(testStudent, LocalDate.now()))
            .thenReturn(Arrays.asList(testSchedule));

        List<PullOutSchedule> result = service.getStudentSchedule(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetStaffSchedule_WithValidStaff_ShouldReturnSchedules() {
        when(pullOutScheduleRepository.findActiveByStaff(testStaff, LocalDate.now()))
            .thenReturn(Arrays.asList(testSchedule));

        List<PullOutSchedule> result = service.getStaffSchedule(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetStudentScheduleForDay_WithValidParameters_ShouldReturnSchedules() {
        when(pullOutScheduleRepository.findByStudentAndDayOfWeek(testStudent, "MONDAY", LocalDate.now()))
            .thenReturn(Arrays.asList(testSchedule));

        List<PullOutSchedule> result = service.getStudentScheduleForDay(1L, "MONDAY");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetStaffScheduleForDay_WithValidParameters_ShouldReturnSchedules() {
        when(pullOutScheduleRepository.findByStaffAndDayOfWeek(testStaff, "MONDAY", LocalDate.now()))
            .thenReturn(Arrays.asList(testSchedule));

        List<PullOutSchedule> result = service.getStaffScheduleForDay(1L, "MONDAY");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetSchedulesForService_WithValidService_ShouldReturnSchedules() {
        when(pullOutScheduleRepository.findByIepService(testIEPService))
            .thenReturn(Arrays.asList(testSchedule));

        List<PullOutSchedule> result = service.getSchedulesForService(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllGroupSessions_ShouldReturnGroupSchedules() {
        when(pullOutScheduleRepository.findGroupSessions(LocalDate.now()))
            .thenReturn(Arrays.asList(testSchedule));

        List<PullOutSchedule> result = service.getAllGroupSessions();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindAllActiveSchedules_ShouldReturnActiveSchedules() {
        when(pullOutScheduleRepository.findByStatus(PullOutStatus.ACTIVE))
            .thenReturn(Arrays.asList(testSchedule));

        List<PullOutSchedule> result = service.findAllActiveSchedules();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== AVAILABILITY FINDING TESTS ==========

    @Test
    void testFindAvailableTimeSlots_WithAvailableSlots_ShouldReturnMap() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());

        Map<String, List<TimeSlot>> result = service.findAvailableTimeSlots(1L, 1L, 30);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void testFindAvailableTimeSlots_WithNullStudentId_ShouldReturnEmptyMap() {
        Map<String, List<TimeSlot>> result = service.findAvailableTimeSlots(null, 1L, 30);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testFindAvailableTimeSlots_WithNullStaffId_ShouldReturnEmptyMap() {
        Map<String, List<TimeSlot>> result = service.findAvailableTimeSlots(1L, null, 30);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testFindBestTimeSlot_WithAvailableSlots_ShouldReturnBestSlot() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());

        Optional<TimeSlot> result = service.findBestTimeSlot(1L);

        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    void testFindBestTimeSlot_WithNullServiceId_ShouldReturnEmpty() {
        Optional<TimeSlot> result = service.findBestTimeSlot(null);

        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    void testFindBestTimeSlot_WithServiceMissingStudent_ShouldReturnEmpty() {
        testIEPService.setIep(null);

        Optional<TimeSlot> result = service.findBestTimeSlot(1L);

        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    void testFindBestTimeSlot_WithServiceMissingStaff_ShouldReturnEmpty() {
        testIEPService.setAssignedStaff(null);

        Optional<TimeSlot> result = service.findBestTimeSlot(1L);

        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    // ========== STATISTICS TESTS ==========

    @Test
    void testGetTotalMinutesPerWeek_WithSchedules_ShouldReturnTotal() {
        when(pullOutScheduleRepository.sumWeeklyMinutesForStudent(testStudent, LocalDate.now()))
            .thenReturn(120L);

        int result = service.getTotalMinutesPerWeek(1L);

        assertEquals(120, result);
    }

    @Test
    void testGetTotalMinutesPerWeek_WithNoSchedules_ShouldReturnZero() {
        when(pullOutScheduleRepository.sumWeeklyMinutesForStudent(testStudent, LocalDate.now()))
            .thenReturn(null);

        int result = service.getTotalMinutesPerWeek(1L);

        assertEquals(0, result);
    }

    @Test
    void testGetStaffWorkload_WithSchedules_ShouldReturnTotal() {
        when(pullOutScheduleRepository.sumWeeklyMinutesForStaff(testStaff, LocalDate.now()))
            .thenReturn(180L);

        int result = service.getStaffWorkload(1L);

        assertEquals(180, result);
    }

    @Test
    void testGetStaffWorkload_WithNoSchedules_ShouldReturnZero() {
        when(pullOutScheduleRepository.sumWeeklyMinutesForStaff(testStaff, LocalDate.now()))
            .thenReturn(null);

        int result = service.getStaffWorkload(1L);

        assertEquals(0, result);
    }

    @Test
    void testCountStudentSchedules_WithSchedules_ShouldReturnCount() {
        when(pullOutScheduleRepository.countActiveByStudent(testStudent, LocalDate.now()))
            .thenReturn(3L);

        long result = service.countStudentSchedules(1L);

        assertEquals(3L, result);
    }

    @Test
    void testCountStaffSchedules_WithSchedules_ShouldReturnCount() {
        when(pullOutScheduleRepository.countActiveByStaff(testStaff, LocalDate.now()))
            .thenReturn(5L);

        long result = service.countStaffSchedules(1L);

        assertEquals(5L, result);
    }

    // ========== VALIDATION TESTS ==========

    @Test
    void testValidateSchedule_WithValidSchedule_ShouldReturnEmptyList() {
        List<String> result = service.validateSchedule(testSchedule);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testValidateSchedule_WithMissingFields_ShouldReturnErrors() {
        testSchedule.setIepService(null);
        testSchedule.setStudent(null);
        testSchedule.setStaff(null);

        List<String> result = service.validateSchedule(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() >= 3);
    }

    @Test
    void testValidateSchedule_WithEndBeforeStart_ShouldReturnError() {
        testSchedule.setStartTime(LocalTime.of(10, 0));
        testSchedule.setEndTime(LocalTime.of(9, 0));

        List<String> result = service.validateSchedule(testSchedule);

        assertNotNull(result);
        assertTrue(result.size() > 0);
        assertTrue(result.stream().anyMatch(e -> e.contains("End time must be after start time")));
    }

    @Test
    void testCanSchedule_WithValidSchedule_ShouldReturnTrue() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(pullOutScheduleRepository.findConflictingSchedulesForStaff(
            any(), anyString(), any(), any(), any())).thenReturn(new ArrayList<>());

        boolean result = service.canSchedule(testSchedule);

        assertTrue(result);
    }

    @Test
    void testCanSchedule_WithNullSchedule_ShouldReturnFalse() {
        boolean result = service.canSchedule(null);

        assertFalse(result);
    }

    @Test
    void testCanSchedule_WithInvalidSchedule_ShouldReturnFalse() {
        testSchedule.setStudent(null);

        boolean result = service.canSchedule(testSchedule);

        assertFalse(result);
    }

    @Test
    void testCanSchedule_WithConflict_ShouldReturnFalse() {
        when(pullOutScheduleRepository.findConflictingSchedulesForStudent(
            any(), anyString(), any(), any(), any())).thenReturn(Arrays.asList(testSchedule));

        boolean result = service.canSchedule(testSchedule);

        assertFalse(result);
    }

    // ========== FIND BY ID TEST ==========

    @Test
    void testFindById_WithValidId_ShouldReturnSchedule() {
        Optional<PullOutSchedule> result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(testSchedule, result.get());
    }

    @Test
    void testFindById_WithNonExistentId_ShouldReturnEmpty() {
        when(pullOutScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<PullOutSchedule> result = service.findById(999L);

        assertFalse(result.isPresent());
    }
}
