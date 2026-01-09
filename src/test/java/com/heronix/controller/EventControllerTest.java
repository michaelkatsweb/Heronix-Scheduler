package com.heronix.controller;

import com.heronix.model.domain.Event;
import com.heronix.model.domain.Room;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.EventType;
import com.heronix.repository.EventRepository;
import com.heronix.repository.RoomRepository;
import com.heronix.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for EventController
 * Tests CRUD operations, query endpoints, and event management functionality
 *
 * @author Heronix Scheduling System Team
 * @since December 19, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class EventControllerTest {

    @Autowired
    private EventController eventController;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Event testEvent;
    private Event testEvent2;
    private Teacher testTeacher;
    private Room testRoom;

    @BeforeEach
    public void setup() {
        // Create test teacher
        testTeacher = new Teacher();
        testTeacher.setEmployeeId("TCH_EVENT_001");
        testTeacher.setName("Event Teacher");
        testTeacher.setFirstName("Event");
        testTeacher.setLastName("Teacher");
        testTeacher.setEmail("event.teacher@school.edu");
        testTeacher = teacherRepository.save(testTeacher);

        // Create test room
        testRoom = new Room();
        testRoom.setRoomNumber("AUDIT101");
        testRoom.setCapacity(200);
        testRoom = roomRepository.save(testRoom);

        // Create first test event
        testEvent = new Event();
        testEvent.setName("Faculty Meeting");
        testEvent.setEventType(EventType.FACULTY_MEETING);
        testEvent.setStartDateTime(LocalDateTime.now().plusDays(1));
        testEvent.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(2));
        testEvent.setDescription("Monthly faculty meeting");
        testEvent.setBlocksScheduling(true);
        testEvent.setAllDay(false);
        testEvent.setRecurring(false);
        testEvent.setNotes("Attendance required");
        testEvent.setAffectedTeachers(new ArrayList<>());
        testEvent.getAffectedTeachers().add(testTeacher);
        testEvent.setAffectedRooms(new ArrayList<>());
        testEvent.getAffectedRooms().add(testRoom);
        testEvent = eventRepository.save(testEvent);

        // Create second test event for filtering tests
        testEvent2 = new Event();
        testEvent2.setName("IEP Meeting");
        testEvent2.setEventType(EventType.IEP_MEETING);
        testEvent2.setStartDateTime(LocalDateTime.now().minusDays(1));
        testEvent2.setEndDateTime(LocalDateTime.now().minusDays(1).plusHours(1));
        testEvent2.setDescription("Individual Education Plan meeting");
        testEvent2.setBlocksScheduling(false);
        testEvent2.setAllDay(false);
        testEvent2.setRecurring(true);
        testEvent2.setRecurrencePattern("WEEKLY");
        testEvent2.setNotes("Compliance meeting");
        testEvent2 = eventRepository.save(testEvent2);
    }

    // ========== BASIC CRUD OPERATIONS ==========

    @Test
    public void testGetAllEvents_ShouldReturnList() {
        // Act
        List<Event> events = eventController.getAllEvents();

        // Assert
        assertNotNull(events);
        assertTrue(events.size() >= 2, "Should return at least two events");

        System.out.println("✓ Events retrieved successfully");
        System.out.println("  - Total events: " + events.size());
    }

    @Test
    public void testGetEventById_WithValidId_ShouldReturnEvent() {
        // Act
        ResponseEntity<Event> response = eventController.getEventById(testEvent.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Faculty Meeting", response.getBody().getName());
        assertEquals(EventType.FACULTY_MEETING, response.getBody().getEventType());

        System.out.println("✓ Event retrieved by ID successfully");
        System.out.println("  - ID: " + response.getBody().getId());
        System.out.println("  - Name: " + response.getBody().getName());
        System.out.println("  - Type: " + response.getBody().getEventType());
    }

    @Test
    public void testGetEventById_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<Event> response = eventController.getEventById(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Non-existent event correctly returns 404");
    }

    @Test
    public void testCreateEvent_WithValidData_ShouldSucceed() {
        // Arrange
        Event newEvent = new Event();
        newEvent.setName("Parent-Teacher Conference");
        newEvent.setEventType(EventType.PARENT_TEACHER_CONFERENCE);
        newEvent.setStartDateTime(LocalDateTime.now().plusDays(3));
        newEvent.setEndDateTime(LocalDateTime.now().plusDays(3).plusHours(3));
        newEvent.setDescription("Fall conferences");

        // Act
        ResponseEntity<Event> response = eventController.createEvent(newEvent);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals("Parent-Teacher Conference", response.getBody().getName());

        System.out.println("✓ Event created successfully");
        System.out.println("  - ID: " + response.getBody().getId());
        System.out.println("  - Name: " + response.getBody().getName());
    }

    @Test
    public void testCreateEvent_WithoutDates_ShouldSetDefaults() {
        // Arrange
        Event newEvent = new Event();
        newEvent.setName("Quick Event");
        newEvent.setEventType(EventType.OTHER);
        // No dates set

        // Act
        ResponseEntity<Event> response = eventController.createEvent(newEvent);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getStartDateTime(), "Should have default start date");
        assertNotNull(response.getBody().getEndDateTime(), "Should have default end date");

        System.out.println("✓ Event created with default dates");
        System.out.println("  - Start: " + response.getBody().getStartDateTime());
        System.out.println("  - End: " + response.getBody().getEndDateTime());
    }

    @Test
    public void testUpdateEvent_WithValidData_ShouldSucceed() {
        // Arrange
        testEvent.setName("Updated Staff Meeting");
        testEvent.setBlocksScheduling(false);

        // Act
        ResponseEntity<Event> response = eventController.updateEvent(
                testEvent.getId(), testEvent);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Staff Meeting", response.getBody().getName());
        assertFalse(response.getBody().getBlocksScheduling());

        System.out.println("✓ Event updated successfully");
        System.out.println("  - Updated name: " + response.getBody().getName());
        System.out.println("  - Blocks scheduling: " + response.getBody().getBlocksScheduling());
    }

    @Test
    public void testUpdateEvent_WithNonExistentId_ShouldReturn404() {
        // Arrange
        Event eventUpdate = new Event();
        eventUpdate.setName("Test Event");

        // Act
        ResponseEntity<Event> response = eventController.updateEvent(99999L, eventUpdate);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        System.out.println("✓ Update non-existent event correctly returns 404");
    }

    @Test
    public void testDeleteEvent_WithValidId_ShouldSucceed() {
        // Act
        ResponseEntity<?> response = eventController.deleteEvent(testEvent.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify hard deletion (event should not exist)
        boolean exists = eventRepository.findById(testEvent.getId()).isPresent();
        assertFalse(exists, "Event should be deleted from database");

        System.out.println("✓ Event deleted successfully");
        System.out.println("  - Event removed from database: " + !exists);
    }

    @Test
    public void testDeleteEvent_WithNonExistentId_ShouldReturn404() {
        // Act
        ResponseEntity<?> response = eventController.deleteEvent(99999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        System.out.println("✓ Delete non-existent event correctly returns 404");
    }

    // ========== QUERY OPERATIONS ==========

    @Test
    public void testGetEventsByType_WithValidType_ShouldReturnEvents() {
        // Act
        List<Event> events = eventController.getEventsByType(EventType.FACULTY_MEETING);

        // Assert
        assertNotNull(events);
        assertTrue(events.size() >= 1, "Should find at least one faculty meeting");

        // Verify all events are faculty meetings
        boolean allFacultyMeetings = events.stream()
                .allMatch(e -> e.getEventType() == EventType.FACULTY_MEETING);
        assertTrue(allFacultyMeetings, "All returned events should be faculty meetings");

        System.out.println("✓ Events retrieved by type successfully");
        System.out.println("  - Type: FACULTY_MEETING");
        System.out.println("  - Events found: " + events.size());
    }

    @Test
    public void testGetUpcomingEvents_ShouldReturnFutureEvents() {
        // Act
        List<Event> events = eventController.getUpcomingEvents();

        // Assert
        assertNotNull(events);
        // Verify all events are in the future
        LocalDateTime now = LocalDateTime.now();
        boolean allUpcoming = events.stream()
                .allMatch(e -> e.getStartDateTime().isAfter(now));
        assertTrue(allUpcoming, "All returned events should be in the future");

        System.out.println("✓ Upcoming events retrieved successfully");
        System.out.println("  - Upcoming events: " + events.size());
    }

    @Test
    public void testGetBlockingEvents_ShouldReturnBlockingEvents() {
        // Act
        List<Event> events = eventController.getBlockingEvents();

        // Assert
        assertNotNull(events);
        // Verify all events block scheduling
        boolean allBlocking = events.stream()
                .allMatch(Event::getBlocksScheduling);
        assertTrue(allBlocking, "All returned events should block scheduling");

        System.out.println("✓ Blocking events retrieved successfully");
        System.out.println("  - Blocking events: " + events.size());
    }

    @Test
    public void testGetRecurringEvents_ShouldReturnRecurringEvents() {
        // Act
        List<Event> events = eventController.getRecurringEvents();

        // Assert
        assertNotNull(events);
        assertTrue(events.size() >= 1, "Should have at least one recurring event");

        // Verify all events are recurring
        boolean allRecurring = events.stream()
                .allMatch(Event::getRecurring);
        assertTrue(allRecurring, "All returned events should be recurring");

        System.out.println("✓ Recurring events retrieved successfully");
        System.out.println("  - Recurring events: " + events.size());
    }

    @Test
    public void testGetAllDayEvents_ShouldReturnAllDayEvents() {
        // Arrange - create an all-day event
        Event allDayEvent = new Event();
        allDayEvent.setName("Professional Development Day");
        allDayEvent.setEventType(EventType.PROFESSIONAL_DEVELOPMENT);
        allDayEvent.setStartDateTime(LocalDateTime.now().plusDays(5));
        allDayEvent.setEndDateTime(LocalDateTime.now().plusDays(5).plusHours(8));
        allDayEvent.setAllDay(true);
        eventRepository.save(allDayEvent);

        // Act
        List<Event> events = eventController.getAllDayEvents();

        // Assert
        assertNotNull(events);
        assertTrue(events.size() >= 1, "Should have at least one all-day event");

        // Verify all events are all-day
        boolean allAllDay = events.stream()
                .allMatch(Event::getAllDay);
        assertTrue(allAllDay, "All returned events should be all-day");

        System.out.println("✓ All-day events retrieved successfully");
        System.out.println("  - All-day events: " + events.size());
    }

    @Test
    public void testGetComplianceMeetings_ShouldReturnComplianceEvents() {
        // Act
        List<Event> events = eventController.getComplianceMeetings();

        // Assert
        assertNotNull(events);
        // Should include IEP meetings and other compliance events

        System.out.println("✓ Compliance meetings retrieved successfully");
        System.out.println("  - Compliance meetings: " + events.size());
    }

    @Test
    public void testGetEventsInDateRange_ShouldReturnEventsInRange() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        // Act
        List<Event> events = eventController.getEventsInDateRange(start, end);

        // Assert
        assertNotNull(events);
        // Verify events are within range
        boolean allInRange = events.stream()
                .allMatch(e -> !e.getStartDateTime().isBefore(start) &&
                              !e.getStartDateTime().isAfter(end));
        assertTrue(allInRange, "All events should be within date range");

        System.out.println("✓ Events in date range retrieved successfully");
        System.out.println("  - Events found: " + events.size());
    }

    @Test
    public void testGetEventsByTeacher_WithValidTeacherId_ShouldReturnEvents() {
        // Act
        List<Event> events = eventController.getEventsByTeacher(testTeacher.getId());

        // Assert
        assertNotNull(events);
        assertTrue(events.size() >= 1, "Should find at least one event for teacher");

        System.out.println("✓ Events retrieved by teacher successfully");
        System.out.println("  - Teacher ID: " + testTeacher.getId());
        System.out.println("  - Events found: " + events.size());
    }

    @Test
    public void testGetEventsByRoom_WithValidRoomId_ShouldReturnEvents() {
        // Act
        List<Event> events = eventController.getEventsByRoom(testRoom.getId());

        // Assert
        assertNotNull(events);
        assertTrue(events.size() >= 1, "Should find at least one event for room");

        System.out.println("✓ Events retrieved by room successfully");
        System.out.println("  - Room ID: " + testRoom.getId());
        System.out.println("  - Events found: " + events.size());
    }
}
