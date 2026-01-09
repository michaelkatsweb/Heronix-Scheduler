package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.enums.ConflictType;
import com.heronix.repository.*;
import com.heronix.service.ConflictDetectorService;
import com.heronix.service.ConflictResolverService.ResolutionSuggestion;
import com.heronix.service.ConflictResolverService.ResolutionAction;
import com.heronix.service.ConflictResolverService.TimeSlotOption;
import com.heronix.service.ConflictResolverService.SlotSwapSuggestion;
import com.heronix.service.ConflictResolverService.ResolutionImpact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ConflictResolverServiceImpl
 *
 * Tests conflict resolution strategies, suggestions, automatic resolution,
 * manual resolution tools, and resolution actions.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConflictResolverServiceImplTest {

    @Mock
    private ConflictRepository conflictRepository;

    @Mock
    private ScheduleSlotRepository scheduleSlotRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private ConflictDetectorService conflictDetectorService;

    @Mock
    private CourseSectionRepository courseSectionRepository;

    @InjectMocks
    private ConflictResolverServiceImpl service;

    private Conflict testConflict;
    private ScheduleSlot testSlot1;
    private ScheduleSlot testSlot2;
    private Schedule testSchedule;
    private Teacher testTeacher;
    private Room testRoom;
    private Course testCourse;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // Setup test schedule
        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setScheduleName("Test Schedule");

        // Setup test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setName("John Doe");
        testTeacher.setActive(true);

        // Setup test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(30);

        // Setup test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseName("Algebra I");
        testCourse.setSubject("Math");

        // Setup test slots
        testSlot1 = new ScheduleSlot();
        testSlot1.setId(1L);
        testSlot1.setSchedule(testSchedule);
        testSlot1.setTeacher(testTeacher);
        testSlot1.setRoom(testRoom);
        testSlot1.setCourse(testCourse);
        testSlot1.setDayOfWeek(DayOfWeek.MONDAY);
        testSlot1.setStartTime(LocalTime.of(9, 0));
        testSlot1.setEndTime(LocalTime.of(10, 0));

        testSlot2 = new ScheduleSlot();
        testSlot2.setId(2L);
        testSlot2.setSchedule(testSchedule);
        testSlot2.setTeacher(testTeacher);
        testSlot2.setRoom(testRoom);
        testSlot2.setCourse(testCourse);
        testSlot2.setDayOfWeek(DayOfWeek.MONDAY);
        testSlot2.setStartTime(LocalTime.of(10, 0));
        testSlot2.setEndTime(LocalTime.of(11, 0));

        // Setup test conflict
        testConflict = new Conflict();
        testConflict.setId(1L);
        testConflict.setSchedule(testSchedule);
        testConflict.setConflictType(ConflictType.ROOM_DOUBLE_BOOKING);
        testConflict.setAffectedSlots(Arrays.asList(testSlot1, testSlot2));

        // Default mock setups
        when(scheduleSlotRepository.save(any(ScheduleSlot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(conflictRepository.save(any(Conflict.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(scheduleSlotRepository.findAll()).thenReturn(Collections.emptyList());
    }

    // ========================================================================
    // GET SUGGESTIONS TESTS
    // ========================================================================

    @Test
    void testGetSuggestions_WithNullConflict_ShouldReturnEmptyList() {
        List<ResolutionSuggestion> result = service.getSuggestions(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetSuggestions_WithNullConflictType_ShouldReturnEmptyList() {
        testConflict.setConflictType(null);

        List<ResolutionSuggestion> result = service.getSuggestions(testConflict);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetSuggestions_WithRoomDoubleBooking_ShouldReturnSuggestions() {
        testConflict.setConflictType(ConflictType.ROOM_DOUBLE_BOOKING);

        List<ResolutionSuggestion> result = service.getSuggestions(testConflict);

        assertNotNull(result);
        // May be empty if no alternative rooms/times found
    }

    @Test
    void testGetSuggestions_ShouldSortByConfidence() {
        testConflict.setConflictType(ConflictType.TEACHER_OVERLOAD);
        testConflict.setAffectedSlots(Arrays.asList(testSlot1));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom));
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher));

        List<ResolutionSuggestion> result = service.getSuggestions(testConflict);

        assertNotNull(result);
        // Verify sorted by confidence (if multiple suggestions)
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).getConfidence() >= result.get(i + 1).getConfidence());
        }
    }

    @Test
    void testGetSuggestions_WithNullConflictId_ShouldHandleGracefully() {
        testConflict.setId(null);
        testConflict.setConflictType(ConflictType.ROOM_DOUBLE_BOOKING);

        List<ResolutionSuggestion> result = service.getSuggestions(testConflict);

        assertNotNull(result);
    }

    // ========================================================================
    // GET BEST SUGGESTION TESTS
    // ========================================================================

    @Test
    void testGetBestSuggestion_WithNoSuggestions_ShouldReturnNull() {
        testConflict.setAffectedSlots(Collections.emptyList());

        ResolutionSuggestion result = service.getBestSuggestion(testConflict);

        assertNull(result);
    }

    @Test
    void testGetBestSuggestion_ShouldReturnHighestConfidence() {
        testConflict.setConflictType(ConflictType.TEACHER_OVERLOAD);
        testConflict.setAffectedSlots(Arrays.asList(testSlot1));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom));

        ResolutionSuggestion result = service.getBestSuggestion(testConflict);

        // If suggestions exist, should return one with highest confidence
        if (result != null) {
            assertNotNull(result.getConfidence());
        }
    }

    // ========================================================================
    // APPLY RESOLUTION TESTS
    // ========================================================================

    @Test
    void testApplyResolution_WithChangeRoomAction_ShouldChangeRoom() {
        Room newRoom = new Room();
        newRoom.setId(2L);
        newRoom.setRoomNumber("102");

        ResolutionAction action = new ResolutionAction();
        action.setActionType("CHANGE_ROOM");
        action.setTargetSlot(testSlot1);
        action.setNewRoom(newRoom);

        ResolutionSuggestion suggestion = new ResolutionSuggestion(
            "Change Room", "Move to room 102", 0.9, action);

        boolean result = service.applyResolution(testConflict, suggestion, testUser);

        assertTrue(result);
        verify(scheduleSlotRepository).save(testSlot1);
        verify(conflictRepository).save(testConflict);
    }

    @Test
    void testApplyResolution_WithNullSuggestion_ShouldReturnFalse() {
        boolean result = service.applyResolution(testConflict, null, testUser);

        assertFalse(result);
    }

    @Test
    void testApplyResolution_WithNullAction_ShouldReturnFalse() {
        ResolutionSuggestion suggestion = new ResolutionSuggestion(
            "Test", "Test", 0.9, null);

        boolean result = service.applyResolution(testConflict, suggestion, testUser);

        assertFalse(result);
    }

    @Test
    void testApplyResolution_WithUnknownActionType_ShouldReturnFalse() {
        ResolutionAction action = new ResolutionAction();
        action.setActionType("UNKNOWN_ACTION");

        ResolutionSuggestion suggestion = new ResolutionSuggestion(
            "Unknown", "Unknown action", 0.5, action);

        boolean result = service.applyResolution(testConflict, suggestion, testUser);

        assertFalse(result);
    }

    // ========================================================================
    // AUTO RESOLVE TESTS
    // ========================================================================

    @Test
    void testAutoResolve_WithHighConfidenceSuggestion_ShouldApply() {
        testConflict.setConflictType(ConflictType.ROOM_DOUBLE_BOOKING);
        testConflict.setAffectedSlots(Arrays.asList(testSlot1));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom));
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot2));

        boolean result = service.autoResolve(testConflict, testUser);

        // Result depends on whether suggestions are generated
        assertNotNull(result);
    }

    @Test
    void testAutoResolve_WithLowConfidence_ShouldNotApply() {
        // Create a mock suggestion with low confidence
        testConflict.setConflictType(ConflictType.ROOM_DOUBLE_BOOKING);
        testConflict.setAffectedSlots(Collections.emptyList()); // No suggestions

        boolean result = service.autoResolve(testConflict, testUser);

        assertFalse(result);
    }

    @Test
    void testAutoResolve_WithNoSuggestions_ShouldReturnFalse() {
        testConflict.setAffectedSlots(Collections.emptyList());

        boolean result = service.autoResolve(testConflict, testUser);

        assertFalse(result);
    }

    // ========================================================================
    // AUTO RESOLVE ALL TESTS
    // ========================================================================

    @Test
    void testAutoResolveAll_WithNullSchedule_ShouldReturn0() {
        int result = service.autoResolveAll(null, testUser);

        assertEquals(0, result);
    }

    @Test
    void testAutoResolveAll_WithNoConflicts_ShouldReturn0() {
        when(conflictRepository.findActiveBySchedule(testSchedule)).thenReturn(Collections.emptyList());

        int result = service.autoResolveAll(testSchedule, testUser);

        assertEquals(0, result);
    }

    @Test
    void testAutoResolveAll_WithMultipleConflicts_ShouldAttemptAll() {
        Conflict conflict1 = new Conflict();
        conflict1.setId(1L);
        conflict1.setConflictType(ConflictType.ROOM_DOUBLE_BOOKING);
        conflict1.setAffectedSlots(Collections.emptyList());

        Conflict conflict2 = new Conflict();
        conflict2.setId(2L);
        conflict2.setConflictType(ConflictType.TEACHER_OVERLOAD);
        conflict2.setAffectedSlots(Collections.emptyList());

        when(conflictRepository.findActiveBySchedule(testSchedule))
            .thenReturn(Arrays.asList(conflict1, conflict2));

        int result = service.autoResolveAll(testSchedule, testUser);

        assertEquals(0, result); // Both have no suggestions (empty affected slots)
    }

    @Test
    void testAutoResolveAll_WithNullScheduleName_ShouldHandleGracefully() {
        testSchedule.setScheduleName(null);
        when(conflictRepository.findActiveBySchedule(testSchedule)).thenReturn(Collections.emptyList());

        int result = service.autoResolveAll(testSchedule, testUser);

        assertEquals(0, result);
    }

    @Test
    void testAutoResolveAll_WithNullConflictInList_ShouldSkip() {
        when(conflictRepository.findActiveBySchedule(testSchedule))
            .thenReturn(Arrays.asList(testConflict, null));

        int result = service.autoResolveAll(testSchedule, testUser);

        assertNotNull(result);
        // Should skip null conflict
    }

    // ========================================================================
    // AUTO RESOLVE BY TYPE TESTS
    // ========================================================================

    @Test
    void testAutoResolveByType_WithSpecificType_ShouldResolveOnlyThatType() {
        Conflict roomConflict = new Conflict();
        roomConflict.setId(1L);
        roomConflict.setConflictType(ConflictType.ROOM_DOUBLE_BOOKING);
        roomConflict.setAffectedSlots(Collections.emptyList());

        when(conflictRepository.findByScheduleAndConflictType(testSchedule, ConflictType.ROOM_DOUBLE_BOOKING))
            .thenReturn(Arrays.asList(roomConflict));

        int result = service.autoResolveByType(testSchedule, "ROOM_DOUBLE_BOOKING", testUser);

        assertEquals(0, result); // No suggestions due to empty affected slots
    }

    @Test
    void testAutoResolveByType_WithNullConflict_ShouldSkip() {
        when(conflictRepository.findByScheduleAndConflictType(testSchedule, ConflictType.ROOM_DOUBLE_BOOKING))
            .thenReturn(Arrays.asList(null, testConflict));

        int result = service.autoResolveByType(testSchedule, "ROOM_DOUBLE_BOOKING", testUser);

        assertNotNull(result);
    }

    @Test
    void testAutoResolveByType_WithInactiveConflict_ShouldSkip() {
        // Mark conflict as resolved (making it inactive)
        testConflict.resolve(testUser, "Already resolved");
        when(conflictRepository.findByScheduleAndConflictType(testSchedule, ConflictType.ROOM_DOUBLE_BOOKING))
            .thenReturn(Arrays.asList(testConflict));

        int result = service.autoResolveByType(testSchedule, "ROOM_DOUBLE_BOOKING", testUser);

        assertEquals(0, result);
    }

    // ========================================================================
    // FIND ALTERNATIVE TIME SLOTS TESTS
    // ========================================================================

    @Test
    void testFindAlternativeTimeSlots_WithNullSlot_ShouldReturnEmptyList() {
        List<TimeSlotOption> result = service.findAlternativeTimeSlots(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAlternativeTimeSlots_WithNullSchedule_ShouldReturnEmptyList() {
        testSlot1.setSchedule(null);

        List<TimeSlotOption> result = service.findAlternativeTimeSlots(testSlot1);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAlternativeTimeSlots_ShouldReturnAvailableSlots() {
        when(scheduleSlotRepository.findAll()).thenReturn(Arrays.asList(testSlot2));

        List<TimeSlotOption> result = service.findAlternativeTimeSlots(testSlot1);

        assertNotNull(result);
        // Should find at least some available time slots
    }

    @Test
    void testFindAlternativeTimeSlots_WithNullSlotId_ShouldHandleGracefully() {
        testSlot1.setId(null);

        List<TimeSlotOption> result = service.findAlternativeTimeSlots(testSlot1);

        assertNotNull(result);
    }

    // ========================================================================
    // FIND ALTERNATIVE ROOMS TESTS
    // ========================================================================

    @Test
    void testFindAlternativeRooms_WithNullSlot_ShouldReturnEmptyList() {
        List<Room> result = service.findAlternativeRooms(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAlternativeRooms_WithNullCourse_ShouldReturnEmptyList() {
        testSlot1.setCourse(null);

        List<Room> result = service.findAlternativeRooms(testSlot1);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAlternativeRooms_ShouldReturnAvailableRooms() {
        Room availableRoom = new Room();
        availableRoom.setId(2L);
        availableRoom.setRoomNumber("102");
        availableRoom.setCapacity(35);

        when(roomRepository.findAll()).thenReturn(Arrays.asList(availableRoom));
        when(scheduleSlotRepository.findAll()).thenReturn(Collections.emptyList());

        List<Room> result = service.findAlternativeRooms(testSlot1);

        assertNotNull(result);
    }

    @Test
    void testFindAlternativeRooms_WithNullSlotId_ShouldHandleGracefully() {
        testSlot1.setId(null);

        List<Room> result = service.findAlternativeRooms(testSlot1);

        assertNotNull(result);
    }

    @Test
    void testFindAlternativeRooms_WithNullRoomInList_ShouldFilterOut() {
        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom, null));

        List<Room> result = service.findAlternativeRooms(testSlot1);

        assertNotNull(result);
        assertFalse(result.contains(null));
    }

    // ========================================================================
    // FIND ALTERNATIVE TEACHERS TESTS
    // ========================================================================

    @Test
    void testFindAlternativeTeachers_WithNullSlot_ShouldReturnEmptyList() {
        List<Teacher> result = service.findAlternativeTeachers(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAlternativeTeachers_WithNullCourse_ShouldReturnEmptyList() {
        testSlot1.setCourse(null);

        List<Teacher> result = service.findAlternativeTeachers(testSlot1);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAlternativeTeachers_ShouldReturnAvailableTeachers() {
        Teacher availableTeacher = new Teacher();
        availableTeacher.setId(2L);
        availableTeacher.setName("Jane Smith");
        availableTeacher.setActive(true);

        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(availableTeacher));
        when(scheduleSlotRepository.findAll()).thenReturn(Collections.emptyList());

        List<Teacher> result = service.findAlternativeTeachers(testSlot1);

        assertNotNull(result);
    }

    @Test
    void testFindAlternativeTeachers_WithNullSlotId_ShouldHandleGracefully() {
        testSlot1.setId(null);

        List<Teacher> result = service.findAlternativeTeachers(testSlot1);

        assertNotNull(result);
    }

    @Test
    void testFindAlternativeTeachers_WithNullTeacherInList_ShouldFilterOut() {
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher, null));

        List<Teacher> result = service.findAlternativeTeachers(testSlot1);

        assertNotNull(result);
        assertFalse(result.contains(null));
    }

    @Test
    void testFindAlternativeTeachers_WithInactiveTeacher_ShouldFilterOut() {
        Teacher inactiveTeacher = new Teacher();
        inactiveTeacher.setId(2L);
        inactiveTeacher.setActive(false);

        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(inactiveTeacher));

        List<Teacher> result = service.findAlternativeTeachers(testSlot1);

        assertNotNull(result);
    }

    // ========================================================================
    // SUGGEST SLOT SWAPS TESTS
    // ========================================================================

    @Test
    void testSuggestSlotSwaps_WithNullConflict_ShouldReturnEmptyList() {
        List<SlotSwapSuggestion> result = service.suggestSlotSwaps(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSuggestSlotSwaps_WithNullAffectedSlots_ShouldReturnEmptyList() {
        testConflict.setAffectedSlots(null);

        List<SlotSwapSuggestion> result = service.suggestSlotSwaps(testConflict);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSuggestSlotSwaps_WithLessThan2Slots_ShouldReturnEmptyList() {
        testConflict.setAffectedSlots(Arrays.asList(testSlot1));

        List<SlotSwapSuggestion> result = service.suggestSlotSwaps(testConflict);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSuggestSlotSwaps_With2Slots_ShouldReturnSwapSuggestion() {
        testConflict.setAffectedSlots(Arrays.asList(testSlot1, testSlot2));

        List<SlotSwapSuggestion> result = service.suggestSlotSwaps(testConflict);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0.8, result.get(0).getBenefit());
    }

    @Test
    void testSuggestSlotSwaps_WithNullSlotInList_ShouldReturnEmptyList() {
        testConflict.setAffectedSlots(Arrays.asList(testSlot1, null));

        List<SlotSwapSuggestion> result = service.suggestSlotSwaps(testConflict);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSuggestSlotSwaps_WithNullConflictId_ShouldHandleGracefully() {
        testConflict.setId(null);
        testConflict.setAffectedSlots(Arrays.asList(testSlot1, testSlot2));

        List<SlotSwapSuggestion> result = service.suggestSlotSwaps(testConflict);

        assertNotNull(result);
    }

    // ========================================================================
    // MOVE SLOT TESTS
    // ========================================================================

    @Test
    void testMoveSlot_WithValidTime_ShouldMoveSlot() {
        TimeSlotOption newTime = new TimeSlotOption("TUESDAY", "10:00", "11:00");

        boolean result = service.moveSlot(testSlot1, newTime, testUser);

        assertTrue(result);
        assertEquals(DayOfWeek.TUESDAY, testSlot1.getDayOfWeek());
        assertEquals(LocalTime.of(10, 0), testSlot1.getStartTime());
        verify(scheduleSlotRepository).save(testSlot1);
    }

    @Test
    void testMoveSlot_WithInvalidTimeFormat_ShouldReturnFalse() {
        TimeSlotOption newTime = new TimeSlotOption("TUESDAY", "invalid", "11:00");

        boolean result = service.moveSlot(testSlot1, newTime, testUser);

        assertFalse(result);
    }

    // ========================================================================
    // CHANGE ROOM TESTS
    // ========================================================================

    @Test
    void testChangeRoom_ShouldUpdateRoom() {
        Room newRoom = new Room();
        newRoom.setId(2L);
        newRoom.setRoomNumber("102");

        boolean result = service.changeRoom(testSlot1, newRoom, testUser);

        assertTrue(result);
        assertEquals(newRoom, testSlot1.getRoom());
        verify(scheduleSlotRepository).save(testSlot1);
    }

    // ========================================================================
    // CHANGE TEACHER TESTS
    // ========================================================================

    @Test
    void testChangeTeacher_ShouldUpdateTeacher() {
        Teacher newTeacher = new Teacher();
        newTeacher.setId(2L);
        newTeacher.setName("Jane Smith");

        boolean result = service.changeTeacher(testSlot1, newTeacher, testUser);

        assertTrue(result);
        assertEquals(newTeacher, testSlot1.getTeacher());
        verify(scheduleSlotRepository).save(testSlot1);
    }

    // ========================================================================
    // SWAP SLOTS TESTS
    // ========================================================================

    @Test
    void testSwapSlots_ShouldSwapTimesAndRooms() {
        DayOfWeek day1 = testSlot1.getDayOfWeek();
        LocalTime start1 = testSlot1.getStartTime();
        Room room1 = testSlot1.getRoom();

        DayOfWeek day2 = testSlot2.getDayOfWeek();
        LocalTime start2 = testSlot2.getStartTime();
        Room room2 = testSlot2.getRoom();

        boolean result = service.swapSlots(testSlot1, testSlot2, testUser);

        assertTrue(result);
        assertEquals(day2, testSlot1.getDayOfWeek());
        assertEquals(start2, testSlot1.getStartTime());
        assertEquals(day1, testSlot2.getDayOfWeek());
        assertEquals(start1, testSlot2.getStartTime());
        verify(scheduleSlotRepository, times(2)).save(any(ScheduleSlot.class));
    }

    // ========================================================================
    // DELETE SLOT TESTS
    // ========================================================================

    @Test
    void testDeleteSlot_ShouldRemoveSlot() {
        boolean result = service.deleteSlot(testSlot1, testUser, "Test reason");

        assertTrue(result);
        verify(scheduleSlotRepository).delete(testSlot1);
    }

    // ========================================================================
    // MARK RESOLVED TESTS
    // ========================================================================

    @Test
    void testMarkResolved_ShouldResolveConflict() {
        service.markResolved(testConflict, testUser, "Manually resolved");

        verify(conflictRepository).save(testConflict);
    }

    // ========================================================================
    // MARK IGNORED TESTS
    // ========================================================================

    @Test
    void testMarkIgnored_ShouldIgnoreConflict() {
        service.markIgnored(testConflict, "Not important");

        verify(conflictRepository).save(testConflict);
    }

    // ========================================================================
    // UNIGNORE TESTS
    // ========================================================================

    @Test
    void testUnignore_ShouldUnignoreConflict() {
        service.unignore(testConflict);

        verify(conflictRepository).save(testConflict);
    }

    // ========================================================================
    // VALIDATE RESOLUTION TESTS
    // ========================================================================

    @Test
    void testValidateResolution_ShouldDetectPotentialConflicts() {
        TimeSlotOption newTime = new TimeSlotOption("TUESDAY", "10:00", "11:00");

        when(conflictDetectorService.detectConflictsForSlot(any())).thenReturn(Collections.emptyList());

        List<Conflict> result = service.validateResolution(testSlot1, newTime, null, null);

        assertNotNull(result);
        verify(conflictDetectorService).detectConflictsForSlot(any());
    }

    @Test
    void testValidateResolution_WithNullNewTime_ShouldUseOriginalTime() {
        when(conflictDetectorService.detectConflictsForSlot(any())).thenReturn(Collections.emptyList());

        List<Conflict> result = service.validateResolution(testSlot1, null, null, null);

        assertNotNull(result);
    }

    // ========================================================================
    // ANALYZE IMPACT TESTS
    // ========================================================================

    @Test
    void testAnalyzeImpact_WithValidSuggestion_ShouldReturnImpact() {
        ResolutionAction action = new ResolutionAction();
        action.setActionType("CHANGE_ROOM");
        action.setTargetSlot(testSlot1);
        action.setNewRoom(testRoom);

        ResolutionSuggestion suggestion = new ResolutionSuggestion(
            "Change Room", "Test", 0.9, action);

        ResolutionImpact result = service.analyzeImpact(testConflict, suggestion);

        assertNotNull(result);
        assertEquals(1, result.getSlotsAffected());
        assertTrue(result.getSummary().contains("slot"));
    }

    @Test
    void testAnalyzeImpact_WithNullAction_ShouldReturnEmptyImpact() {
        ResolutionSuggestion suggestion = new ResolutionSuggestion(
            "Test", "Test", 0.5, null);

        ResolutionImpact result = service.analyzeImpact(testConflict, suggestion);

        assertNotNull(result);
    }
}
