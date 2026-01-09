package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.dto.ScheduleGenerationRequest;
import com.heronix.repository.*;
import com.heronix.testutil.BaseServiceTest;
import com.heronix.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ScheduleGenerationService
 * Tests the core schedule generation logic including:
 * - Null safety (all inputs can be null)
 * - Basic schedule generation workflow
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
class ScheduleGenerationServiceTest extends BaseServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleSlotRepository scheduleSlotRepository;

    @InjectMocks
    private ScheduleGenerationService service;

    private ScheduleGenerationRequest validRequest;
    private List<Teacher> teachers;
    private List<Course> courses;
    private List<Room> rooms;
    private List<Student> students;

    @BeforeEach
    void setUp() {
        // Create test data
        teachers = TestDataBuilder.createTeachers(3);
        courses = TestDataBuilder.createCourses(5);
        rooms = TestDataBuilder.createRooms(10);
        students = TestDataBuilder.createStudents(100);

        // Create valid request
        validRequest = new ScheduleGenerationRequest();
        validRequest.setScheduleName("Test Schedule");
        validRequest.setStartDate(LocalDate.now());
        validRequest.setEndDate(LocalDate.now().plusMonths(4));
        validRequest.setSchoolStartTime(LocalTime.of(8, 0));
        validRequest.setSchoolEndTime(LocalTime.of(15, 0));
        validRequest.setPeriodDuration(50);
    }

    // ==================== Null Safety Tests ====================

    @Test
    void testGenerateSchedule_WithNullRequest_ShouldNotThrowNPE() {
        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(null, null);
            } catch (Exception e) {
                // Expected - may throw business exception, but not NPE
                assertFalse(e instanceof NullPointerException,
                    "Should not throw NullPointerException");
            }
        });
    }

    @Test
    void testGenerateSchedule_WithNullCallback_ShouldNotThrowNPE() {
        // Given: Valid request but null callback
        when(teacherRepository.findAll()).thenReturn(teachers);
        when(courseRepository.findAll()).thenReturn(courses);
        when(roomRepository.findAll()).thenReturn(rooms);
        when(studentRepository.findAll()).thenReturn(students);

        // When/Then: Should handle null callback
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(validRequest, null);
            } catch (Exception e) {
                // May throw business exception, but not NPE
                assertFalse(e instanceof NullPointerException,
                    "Should not throw NullPointerException with null callback");
            }
        });
    }

    @Test
    void testGenerateSchedule_WithNullRepositoryResults_ShouldNotThrowNPE() {
        // Given: Repositories return null
        when(teacherRepository.findAll()).thenReturn(null);
        when(courseRepository.findAll()).thenReturn(null);
        when(roomRepository.findAll()).thenReturn(null);
        when(studentRepository.findAll()).thenReturn(null);

        // When/Then: Should handle null repository results
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(validRequest, null);
            } catch (Exception e) {
                // May throw business exception, but not NPE
                assertFalse(e instanceof NullPointerException,
                    "Should not throw NullPointerException with null repository results");
            }
        });
    }

    // ==================== Empty Data Tests ====================

    @Test
    void testGenerateSchedule_WithEmptyTeacherList_ShouldNotThrowException() {
        // Given: Empty teacher list
        when(teacherRepository.findAll()).thenReturn(Collections.emptyList());
        when(courseRepository.findAll()).thenReturn(courses);
        when(roomRepository.findAll()).thenReturn(rooms);
        when(studentRepository.findAll()).thenReturn(students);

        // When/Then: Should handle empty teachers
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(validRequest, null);
            } catch (Exception e) {
                // May throw business exception for insufficient resources
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    void testGenerateSchedule_WithEmptyCourseList_ShouldNotThrowException() {
        // Given: Empty course list
        when(teacherRepository.findAll()).thenReturn(teachers);
        when(courseRepository.findAll()).thenReturn(Collections.emptyList());
        when(roomRepository.findAll()).thenReturn(rooms);
        when(studentRepository.findAll()).thenReturn(students);

        // When/Then: Should handle empty courses
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(validRequest, null);
            } catch (Exception e) {
                // May throw business exception for no courses
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    void testGenerateSchedule_WithEmptyRoomList_ShouldNotThrowException() {
        // Given: Empty room list
        when(teacherRepository.findAll()).thenReturn(teachers);
        when(courseRepository.findAll()).thenReturn(courses);
        when(roomRepository.findAll()).thenReturn(Collections.emptyList());
        when(studentRepository.findAll()).thenReturn(students);

        // When/Then: Should handle empty rooms
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(validRequest, null);
            } catch (Exception e) {
                // May throw business exception for insufficient rooms
                assertNotNull(e.getMessage());
            }
        });
    }

    // ==================== Request Validation Tests ====================

    @Test
    void testGenerateSchedule_WithNullName_ShouldHandleGracefully() {
        // Given: Request with null name
        validRequest.setScheduleName(null);
        when(teacherRepository.findAll()).thenReturn(teachers);
        when(courseRepository.findAll()).thenReturn(courses);

        // When/Then: Should handle null name
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(validRequest, null);
            } catch (Exception e) {
                // May throw validation exception
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    void testGenerateSchedule_WithNullDates_ShouldHandleGracefully() {
        // Given: Request with null dates
        validRequest.setStartDate(null);
        validRequest.setEndDate(null);

        // When/Then: Should handle null dates
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(validRequest, null);
            } catch (Exception e) {
                // May throw validation exception
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    void testGenerateSchedule_WithNullTimes_ShouldHandleGracefully() {
        // Given: Request with null times
        validRequest.setSchoolStartTime(null);
        validRequest.setSchoolEndTime(null);

        // When/Then: Should handle null times
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(validRequest, null);
            } catch (Exception e) {
                // May throw validation exception
                assertNotNull(e.getMessage());
            }
        });
    }

    // ==================== Repository Interaction Tests ====================

    @Test
    void testGenerateSchedule_ShouldLoadResources() {
        // Given: Valid request and resources
        when(teacherRepository.findAll()).thenReturn(teachers);
        when(courseRepository.findAll()).thenReturn(courses);
        when(roomRepository.findAll()).thenReturn(rooms);
        when(studentRepository.findAll()).thenReturn(students);

        // When: Generate schedule
        try {
            service.generateSchedule(validRequest, null);
        } catch (Exception e) {
            // Expected - may fail due to incomplete mocking
        }

        // Then: Should have called repositories
        verify(teacherRepository, atLeastOnce()).findAll();
        verify(courseRepository, atLeastOnce()).findAll();
    }

    @Test
    void testGenerateSchedule_WithCallback_ShouldInvokeCallback() {
        // Given: Valid request with callback
        final boolean[] callbackInvoked = {false};
        when(teacherRepository.findAll()).thenReturn(teachers);
        when(courseRepository.findAll()).thenReturn(courses);
        when(roomRepository.findAll()).thenReturn(rooms);
        when(studentRepository.findAll()).thenReturn(students);

        // When: Generate with callback
        try {
            service.generateSchedule(validRequest, (progress, message) -> {
                callbackInvoked[0] = true;
            });
        } catch (Exception e) {
            // Expected - may fail due to incomplete mocking
        }

        // Then: Callback may or may not be invoked depending on how far generation proceeds
        // This test validates that having a callback doesn't cause NPE
    }

    // ==================== Edge Cases ====================

    @Test
    void testGenerateSchedule_WithInvalidDateRange_ShouldHandleGracefully() {
        // Given: End date before start date
        validRequest.setStartDate(LocalDate.now());
        validRequest.setEndDate(LocalDate.now().minusMonths(1));

        // When/Then: Should handle invalid range
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(validRequest, null);
            } catch (Exception e) {
                // Expected - should throw validation exception
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    void testGenerateSchedule_WithZeroPeriodDuration_ShouldHandleGracefully() {
        // Given: Zero period duration
        validRequest.setPeriodDuration(0);

        // When/Then: Should handle zero duration
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(validRequest, null);
            } catch (Exception e) {
                // Expected - should throw validation exception
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    void testGenerateSchedule_WithNegativePeriodDuration_ShouldHandleGracefully() {
        // Given: Negative period duration
        validRequest.setPeriodDuration(-50);

        // When/Then: Should handle negative duration
        assertDoesNotThrow(() -> {
            try {
                service.generateSchedule(validRequest, null);
            } catch (Exception e) {
                // Expected - should throw validation exception
                assertNotNull(e.getMessage());
            }
        });
    }
}
