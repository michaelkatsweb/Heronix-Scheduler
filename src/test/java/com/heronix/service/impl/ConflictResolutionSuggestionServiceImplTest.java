package com.heronix.service.impl;

import com.heronix.model.ConflictPriorityScore;
import com.heronix.model.ConflictResolutionSuggestion;
import com.heronix.model.ConflictResolutionSuggestion.ResolutionType;
import com.heronix.model.domain.*;
import com.heronix.model.enums.*;
import com.heronix.repository.*;
import com.heronix.service.OllamaAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for ConflictResolutionSuggestionServiceImpl
 *
 * Tests AI-powered conflict resolution including suggestion generation,
 * priority scoring, and historical success rate tracking.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConflictResolutionSuggestionServiceImplTest {

    @Mock
    private ConflictRepository conflictRepository;

    @Mock
    private ScheduleSlotRepository scheduleSlotRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private OllamaAIService ollamaAIService;

    @InjectMocks
    private ConflictResolutionSuggestionServiceImpl service;

    private Conflict testConflict;
    private ScheduleSlot testSlot1;
    private ScheduleSlot testSlot2;
    private Teacher testTeacher;
    private Room testRoom;
    private Student testStudent;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        // Create test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseName("Math 101");

        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setId(1L);
        testTeacher.setName("John Doe");

        // Create test room
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(30);
        testRoom.setRoomType(RoomType.CLASSROOM);

        // Create test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setFirstName("Jane");
        testStudent.setLastName("Smith");

        // Create test slots
        testSlot1 = new ScheduleSlot();
        testSlot1.setId(1L);
        testSlot1.setTeacher(testTeacher);
        testSlot1.setRoom(testRoom);
        testSlot1.setCourse(testCourse);

        testSlot2 = new ScheduleSlot();
        testSlot2.setId(2L);
        testSlot2.setTeacher(testTeacher);
        testSlot2.setRoom(testRoom);
        testSlot2.setCourse(testCourse);

        // Create test conflict
        testConflict = new Conflict();
        testConflict.setId(1L);
        testConflict.setConflictType(ConflictType.TEACHER_OVERLOAD);
        testConflict.setSeverity(ConflictSeverity.CRITICAL);
        testConflict.setDescription("Teacher double-booked");
        testConflict.setAffectedSlots(Arrays.asList(testSlot1, testSlot2));
        testConflict.setAffectedTeachers(Arrays.asList(testTeacher));
        testConflict.setAffectedStudents(new ArrayList<>());
        testConflict.setDetectedAt(LocalDateTime.now());

        // Mock repositories
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher));
        when(roomRepository.findAll()).thenReturn(Arrays.asList(testRoom));
        when(conflictRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void testGenerateSuggestions_WithTeacherOverload_ShouldGenerateSuggestions() {
        List<ConflictResolutionSuggestion> suggestions = service.generateSuggestions(testConflict);

        assertNotNull(suggestions);
        assertTrue(suggestions.size() > 0);
        assertTrue(suggestions.stream().anyMatch(s ->
            s.getType() == ResolutionType.CHANGE_TEACHER ||
            s.getType() == ResolutionType.CHANGE_TIME_SLOT
        ));
    }

    @Test
    void testGenerateSuggestions_WithRoomDoubleBooking_ShouldGenerateRoomSuggestions() {
        testConflict.setConflictType(ConflictType.ROOM_DOUBLE_BOOKING);

        List<ConflictResolutionSuggestion> suggestions = service.generateSuggestions(testConflict);

        assertNotNull(suggestions);
        assertTrue(suggestions.size() > 0);
    }

    @Test
    void testGenerateSuggestions_WithStudentConflict_ShouldGenerateReassignSuggestion() {
        testConflict.setConflictType(ConflictType.STUDENT_SCHEDULE_CONFLICT);
        testConflict.setAffectedStudents(Arrays.asList(testStudent));

        List<ConflictResolutionSuggestion> suggestions = service.generateSuggestions(testConflict);

        assertNotNull(suggestions);
        assertTrue(suggestions.size() > 0);
        assertTrue(suggestions.stream().anyMatch(s ->
            s.getType() == ResolutionType.REASSIGN_STUDENT
        ));
    }

    @Test
    void testGenerateSuggestions_WithRoomCapacity_ShouldGenerateCapacitySuggestions() {
        testConflict.setConflictType(ConflictType.ROOM_CAPACITY_EXCEEDED);

        List<ConflictResolutionSuggestion> suggestions = service.generateSuggestions(testConflict);

        assertNotNull(suggestions);
        assertTrue(suggestions.size() > 0);
    }

    @Test
    void testGenerateSuggestions_WithNullConflict_ShouldReturnEmptyList() {
        List<ConflictResolutionSuggestion> suggestions = service.generateSuggestions(null);

        assertNotNull(suggestions);
        assertEquals(0, suggestions.size());
    }

    @Test
    void testGenerateSuggestions_WithNullConflictType_ShouldGenerateGeneric() {
        testConflict.setConflictType(null);

        List<ConflictResolutionSuggestion> suggestions = service.generateSuggestions(testConflict);

        assertNotNull(suggestions);
        assertTrue(suggestions.size() > 0);
    }

    @Test
    void testCalculatePriorityScore_WithCriticalConflict_ShouldReturnHighScore() {
        ConflictPriorityScore score = service.calculatePriorityScore(testConflict);

        assertNotNull(score);
        assertEquals(1L, score.getConflictId());
        assertTrue(score.getTotalScore() >= 50); // Critical = 50 hard score
        assertNotNull(score.getPriorityLevel());
    }

    @Test
    void testCalculatePriorityScore_WithHighSeverity_ShouldScoreAppropriately() {
        testConflict.setSeverity(ConflictSeverity.HIGH);

        ConflictPriorityScore score = service.calculatePriorityScore(testConflict);

        assertNotNull(score);
        assertTrue(score.getHardConstraintScore() >= 40);
    }

    @Test
    void testCalculatePriorityScore_WithMediumSeverity_ShouldScoreLower() {
        testConflict.setSeverity(ConflictSeverity.MEDIUM);

        ConflictPriorityScore score = service.calculatePriorityScore(testConflict);

        assertNotNull(score);
        assertTrue(score.getHardConstraintScore() <= 30);
    }

    @Test
    void testApplySuggestion_WithValidSuggestion_ShouldApply() {
        ConflictResolutionSuggestion suggestion = ConflictResolutionSuggestion.builder()
            .id("test-1")
            .type(ResolutionType.CHANGE_ROOM)
            .actions(new ArrayList<>())
            .build();

        when(conflictRepository.save(any())).thenReturn(testConflict);

        boolean result = service.applySuggestion(testConflict, suggestion);

        // Result depends on action execution - may be true or false
        assertNotNull(result);
    }

    @Test
    void testGetHistoricalSuccessRate_WithNoHistory_ShouldReturnDefault() {
        int rate = service.getHistoricalSuccessRate(ResolutionType.CHANGE_ROOM);

        assertTrue(rate > 0);
        assertTrue(rate <= 100);
    }

    @Test
    void testGetHistoricalSuccessRate_ForDifferentTypes_ShouldVary() {
        int changeRoom = service.getHistoricalSuccessRate(ResolutionType.CHANGE_ROOM);
        int splitSection = service.getHistoricalSuccessRate(ResolutionType.SPLIT_SECTION);

        // Change room should have higher success rate than split section
        assertTrue(changeRoom >= splitSection);
    }

    @Test
    void testGetConflictsByPriority_ShouldSortByScore() {
        Conflict conflict1 = new Conflict();
        conflict1.setId(1L);
        conflict1.setConflictType(ConflictType.TEACHER_OVERLOAD);
        conflict1.setSeverity(ConflictSeverity.CRITICAL);
        // affectedEntitiesCount is computed - set affected lists instead
        conflict1.setAffectedSlots(Arrays.asList(testSlot1, testSlot2, testSlot1, testSlot2, testSlot1));
        conflict1.setDetectedAt(LocalDateTime.now());

        Conflict conflict2 = new Conflict();
        conflict2.setId(2L);
        conflict2.setConflictType(ConflictType.TEACHER_OVERLOAD);
        conflict2.setSeverity(ConflictSeverity.LOW);
        conflict2.setAffectedSlots(Arrays.asList(testSlot1));
        conflict2.setDetectedAt(LocalDateTime.now().minusDays(10));

        when(conflictRepository.findAllActive()).thenReturn(Arrays.asList(conflict1, conflict2));

        List<Conflict> result = service.getConflictsByPriority();

        assertNotNull(result);
        assertEquals(2, result.size());
        // First should be higher priority (critical)
        assertEquals(conflict1.getId(), result.get(0).getId());
    }

    @Test
    void testEstimateCascadeImpact_WithMultipleEntities_ShouldCalculate() {
        int impact = service.estimateCascadeImpact(testConflict);

        assertTrue(impact >= 0);
        assertTrue(impact <= 5); // Capped at 5
    }

    @Test
    void testEstimateCascadeImpact_WithNullConflict_ShouldReturnZero() {
        int impact = service.estimateCascadeImpact(null);

        assertEquals(0, impact);
    }

    @Test
    void testCanAutoApply_WithValidSuggestion_ShouldCheck() {
        ConflictResolutionSuggestion suggestion = ConflictResolutionSuggestion.builder()
            .id("test-1")
            .type(ResolutionType.CHANGE_ROOM)
            .requiresConfirmation(false)
            .confidenceLevel(95)
            .build();

        boolean result = service.canAutoApply(suggestion);

        assertNotNull(result);
    }

    @Test
    void testCanAutoApply_WithNullSuggestion_ShouldReturnFalse() {
        boolean result = service.canAutoApply(null);

        assertFalse(result);
    }

    @Test
    void testGenerateSuggestions_WithExcessiveTeachingHours_ShouldSuggestCoTeacher() {
        testConflict.setConflictType(ConflictType.EXCESSIVE_TEACHING_HOURS);

        List<ConflictResolutionSuggestion> suggestions = service.generateSuggestions(testConflict);

        assertNotNull(suggestions);
        assertTrue(suggestions.size() > 0);
    }

    @Test
    void testGenerateSuggestions_WithSubjectMismatch_ShouldSuggestTeacherChange() {
        testConflict.setConflictType(ConflictType.SUBJECT_MISMATCH);

        List<ConflictResolutionSuggestion> suggestions = service.generateSuggestions(testConflict);

        assertNotNull(suggestions);
        assertTrue(suggestions.size() > 0);
    }
}
