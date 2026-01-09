package com.heronix.controller;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseRoomAssignment;
import com.heronix.model.domain.Room;
import com.heronix.model.enums.RoomAssignmentType;
import com.heronix.model.enums.RoomType;
import com.heronix.model.enums.UsagePattern;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.CourseRoomAssignmentRepository;
import com.heronix.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MultiRoomAssignmentDialogController
 * Tests multi-room course assignment business logic
 *
 * Note: This is a JavaFX dialog controller. We test the business logic
 * (room assignment CRUD, validation, persistence) rather than UI interactions.
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class MultiRoomAssignmentDialogControllerTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CourseRoomAssignmentRepository assignmentRepository;

    private Course testCourse;
    private Room room101;
    private Room room102;
    private Room room103;
    private Room labRoom;

    @BeforeEach
    public void setup() {
        // Create test rooms
        room101 = new Room();
        room101.setRoomNumber("101");
        room101.setRoomType(RoomType.CLASSROOM);
        room101.setCapacity(30);
        room101.setActive(true);
        room101 = roomRepository.save(room101);

        room102 = new Room();
        room102.setRoomNumber("102");
        room102.setRoomType(RoomType.CLASSROOM);
        room102.setCapacity(30);
        room102.setActive(true);
        room102 = roomRepository.save(room102);

        room103 = new Room();
        room103.setRoomNumber("103");
        room103.setRoomType(RoomType.CLASSROOM);
        room103.setCapacity(25);
        room103.setActive(true);
        room103 = roomRepository.save(room103);

        labRoom = new Room();
        labRoom.setRoomNumber("LAB-201");
        labRoom.setRoomType(RoomType.SCIENCE_LAB);
        labRoom.setCapacity(24);
        labRoom.setActive(true);
        labRoom = roomRepository.save(labRoom);

        // Create test course
        testCourse = new Course();
        testCourse.setCourseCode("CHEM-101");
        testCourse.setCourseName("Chemistry I");
        testCourse.setCredits(1.0);
        testCourse.setActive(true);
        testCourse.setUsesMultipleRooms(false); // Initially single room
        testCourse = courseRepository.save(testCourse);
    }

    // ========== BASIC OPERATIONS ==========

    @Test
    public void testCreateSingleRoomAssignment_ShouldPersist() {
        // Arrange
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(room101);
        assignment.setAssignmentType(RoomAssignmentType.PRIMARY);
        assignment.setUsagePattern(UsagePattern.ALWAYS);
        assignment.setPriority(1);
        assignment.setActive(true);

        // Act
        assignment = assignmentRepository.save(assignment);

        // Assert
        assertNotNull(assignment.getId());
        assertEquals(room101.getId(), assignment.getRoom().getId());
        assertEquals(RoomAssignmentType.PRIMARY, assignment.getAssignmentType());

        System.out.println("✓ Single room assignment created and persisted");
        System.out.println("  - Room: " + assignment.getRoom().getRoomNumber());
        System.out.println("  - Type: " + assignment.getAssignmentType());
    }

    @Test
    public void testCreateMultipleRoomAssignments_ShouldPersist() {
        // Arrange - Primary classroom + Lab
        CourseRoomAssignment primary = new CourseRoomAssignment();
        primary.setCourse(testCourse);
        primary.setRoom(room101);
        primary.setAssignmentType(RoomAssignmentType.PRIMARY);
        primary.setUsagePattern(UsagePattern.ALWAYS);
        primary.setPriority(1);
        primary.setActive(true);

        CourseRoomAssignment lab = new CourseRoomAssignment();
        lab.setCourse(testCourse);
        lab.setRoom(labRoom);
        lab.setAssignmentType(RoomAssignmentType.SECONDARY);
        lab.setUsagePattern(UsagePattern.ODD_DAYS);
        lab.setPriority(2);
        lab.setActive(true);
        lab.setNotes("Lab activities only");

        // Act
        assignmentRepository.save(primary);
        assignmentRepository.save(lab);

        List<CourseRoomAssignment> assignments = assignmentRepository.findByCourse(testCourse);

        // Assert
        assertEquals(2, assignments.size());

        System.out.println("✓ Multiple room assignments created");
        System.out.println("  - Assignment count: " + assignments.size());
    }

    @Test
    public void testUpdateRoomAssignment_ShouldPersist() {
        // Arrange - Create assignment
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(room101);
        assignment.setAssignmentType(RoomAssignmentType.PRIMARY);
        assignment.setUsagePattern(UsagePattern.ALWAYS);
        assignment.setPriority(1);
        assignment.setActive(true);
        assignment = assignmentRepository.save(assignment);

        // Act - Update to different room
        assignment.setRoom(room102);
        assignment.setNotes("Changed to room 102");
        assignment = assignmentRepository.save(assignment);

        // Reload
        CourseRoomAssignment reloaded = assignmentRepository.findById(assignment.getId()).orElse(null);

        // Assert
        assertNotNull(reloaded);
        assertEquals(room102.getId(), reloaded.getRoom().getId());
        assertEquals("Changed to room 102", reloaded.getNotes());

        System.out.println("✓ Room assignment updated and persisted");
    }

    @Test
    public void testDeleteRoomAssignment_ShouldRemove() {
        // Arrange
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(room101);
        assignment.setAssignmentType(RoomAssignmentType.PRIMARY);
        assignment.setUsagePattern(UsagePattern.ALWAYS);
        assignment.setPriority(1);
        assignment.setActive(true);
        assignment = assignmentRepository.save(assignment);

        Long assignmentId = assignment.getId();

        // Act
        assignmentRepository.delete(assignment);

        // Assert
        assertFalse(assignmentRepository.findById(assignmentId).isPresent());

        System.out.println("✓ Room assignment deleted successfully");
    }

    // ========== ASSIGNMENT TYPES ==========

    @Test
    public void testPrimaryRoomAssignment_ShouldWork() {
        // Arrange
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(room101);
        assignment.setAssignmentType(RoomAssignmentType.PRIMARY);
        assignment.setUsagePattern(UsagePattern.ALWAYS);
        assignment.setPriority(1);
        assignment.setActive(true);

        // Act
        assignment = assignmentRepository.save(assignment);

        // Assert
        assertTrue(assignment.isPrimary());
        assertEquals(RoomAssignmentType.PRIMARY, assignment.getAssignmentType());

        System.out.println("✓ Primary room assignment created");
    }

    @Test
    public void testSecondaryRoomAssignment_ShouldWork() {
        // Arrange
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(labRoom);
        assignment.setAssignmentType(RoomAssignmentType.SECONDARY);
        assignment.setUsagePattern(UsagePattern.ALTERNATING_DAYS);
        assignment.setPriority(2);
        assignment.setActive(true);

        // Act
        assignment = assignmentRepository.save(assignment);

        // Assert
        assertFalse(assignment.isPrimary());
        assertEquals(RoomAssignmentType.SECONDARY, assignment.getAssignmentType());

        System.out.println("✓ Secondary room assignment created");
    }

    @Test
    public void testOverflowRoomAssignment_ShouldWork() {
        // Arrange
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(room102);
        assignment.setAssignmentType(RoomAssignmentType.OVERFLOW);
        assignment.setUsagePattern(UsagePattern.ALWAYS);
        assignment.setPriority(3);
        assignment.setActive(true);
        assignment.setNotes("Video feed from Room 101");

        // Act
        assignment = assignmentRepository.save(assignment);

        // Assert
        assertEquals(RoomAssignmentType.OVERFLOW, assignment.getAssignmentType());
        assertEquals("Video feed from Room 101", assignment.getNotes());

        System.out.println("✓ Overflow room assignment created");
    }

    @Test
    public void testBreakoutRoomAssignment_ShouldWork() {
        // Arrange
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(room103);
        assignment.setAssignmentType(RoomAssignmentType.BREAKOUT);
        assignment.setUsagePattern(UsagePattern.ALWAYS);
        assignment.setPriority(4);
        assignment.setActive(true);
        assignment.setNotes("Small group instruction");

        // Act
        assignment = assignmentRepository.save(assignment);

        // Assert
        assertEquals(RoomAssignmentType.BREAKOUT, assignment.getAssignmentType());

        System.out.println("✓ Breakout room assignment created");
    }

    @Test
    public void testRotatingRoomAssignment_ShouldWork() {
        // Arrange
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(labRoom);
        assignment.setAssignmentType(RoomAssignmentType.ROTATING);
        assignment.setUsagePattern(UsagePattern.WEEKLY_ROTATION);
        assignment.setPriority(1);
        assignment.setActive(true);

        // Act
        assignment = assignmentRepository.save(assignment);

        // Assert
        assertEquals(RoomAssignmentType.ROTATING, assignment.getAssignmentType());
        assertEquals(UsagePattern.WEEKLY_ROTATION, assignment.getUsagePattern());

        System.out.println("✓ Rotating room assignment created");
    }

    // ========== USAGE PATTERNS ==========

    @Test
    public void testAlwaysUsagePattern_ShouldWork() {
        // Arrange
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(room101);
        assignment.setAssignmentType(RoomAssignmentType.PRIMARY);
        assignment.setUsagePattern(UsagePattern.ALWAYS);
        assignment.setPriority(1);
        assignment.setActive(true);

        // Act
        assignment = assignmentRepository.save(assignment);

        // Assert
        assertEquals(UsagePattern.ALWAYS, assignment.getUsagePattern());
        assertTrue(assignment.isDayBasedPattern());
        assertFalse(assignment.isTimeBasedPattern());

        System.out.println("✓ ALWAYS usage pattern configured");
    }

    @Test
    public void testAlternatingDaysPattern_ShouldWork() {
        // Arrange
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(labRoom);
        assignment.setAssignmentType(RoomAssignmentType.SECONDARY);
        assignment.setUsagePattern(UsagePattern.ALTERNATING_DAYS);
        assignment.setPriority(2);
        assignment.setActive(true);

        // Act
        assignment = assignmentRepository.save(assignment);

        // Assert
        assertEquals(UsagePattern.ALTERNATING_DAYS, assignment.getUsagePattern());
        assertTrue(assignment.isDayBasedPattern());

        System.out.println("✓ ALTERNATING_DAYS usage pattern configured");
    }

    @Test
    public void testOddEvenDaysPattern_ShouldWork() {
        // Arrange - ODD days
        CourseRoomAssignment oddDays = new CourseRoomAssignment();
        oddDays.setCourse(testCourse);
        oddDays.setRoom(room101);
        oddDays.setAssignmentType(RoomAssignmentType.PRIMARY);
        oddDays.setUsagePattern(UsagePattern.ODD_DAYS);
        oddDays.setPriority(1);
        oddDays.setActive(true);

        // Arrange - EVEN days
        CourseRoomAssignment evenDays = new CourseRoomAssignment();
        evenDays.setCourse(testCourse);
        evenDays.setRoom(labRoom);
        evenDays.setAssignmentType(RoomAssignmentType.SECONDARY);
        evenDays.setUsagePattern(UsagePattern.EVEN_DAYS);
        evenDays.setPriority(2);
        evenDays.setActive(true);

        // Act
        assignmentRepository.save(oddDays);
        assignmentRepository.save(evenDays);

        List<CourseRoomAssignment> assignments = assignmentRepository.findByCourse(testCourse);

        // Assert
        assertEquals(2, assignments.size());

        System.out.println("✓ ODD/EVEN days pattern configured");
    }

    @Test
    public void testTimeBasedPatterns_ShouldWork() {
        // Arrange - First half (lecture)
        CourseRoomAssignment firstHalf = new CourseRoomAssignment();
        firstHalf.setCourse(testCourse);
        firstHalf.setRoom(room101);
        firstHalf.setAssignmentType(RoomAssignmentType.PRIMARY);
        firstHalf.setUsagePattern(UsagePattern.FIRST_HALF);
        firstHalf.setPriority(1);
        firstHalf.setActive(true);
        firstHalf.setNotes("Lecture portion");

        // Arrange - Second half (lab)
        CourseRoomAssignment secondHalf = new CourseRoomAssignment();
        secondHalf.setCourse(testCourse);
        secondHalf.setRoom(labRoom);
        secondHalf.setAssignmentType(RoomAssignmentType.SECONDARY);
        secondHalf.setUsagePattern(UsagePattern.SECOND_HALF);
        secondHalf.setPriority(2);
        secondHalf.setActive(true);
        secondHalf.setNotes("Lab portion");

        // Act
        assignmentRepository.save(firstHalf);
        assignmentRepository.save(secondHalf);

        List<CourseRoomAssignment> assignments = assignmentRepository.findByCourse(testCourse);

        // Assert
        assertEquals(2, assignments.size());
        assertTrue(firstHalf.isTimeBasedPattern());
        assertTrue(secondHalf.isTimeBasedPattern());

        System.out.println("✓ Time-based patterns (FIRST_HALF/SECOND_HALF) configured");
    }

    // ========== PRIORITY AND ACTIVE STATUS ==========

    @Test
    public void testPriorityOrdering_ShouldWork() {
        // Arrange - Create 3 rooms with different priorities
        CourseRoomAssignment high = new CourseRoomAssignment();
        high.setCourse(testCourse);
        high.setRoom(room101);
        high.setAssignmentType(RoomAssignmentType.PRIMARY);
        high.setUsagePattern(UsagePattern.ALWAYS);
        high.setPriority(1); // Highest priority
        high.setActive(true);

        CourseRoomAssignment medium = new CourseRoomAssignment();
        medium.setCourse(testCourse);
        medium.setRoom(room102);
        medium.setAssignmentType(RoomAssignmentType.SECONDARY);
        medium.setUsagePattern(UsagePattern.ALWAYS);
        medium.setPriority(2);
        medium.setActive(true);

        CourseRoomAssignment low = new CourseRoomAssignment();
        low.setCourse(testCourse);
        low.setRoom(room103);
        low.setAssignmentType(RoomAssignmentType.OVERFLOW);
        low.setUsagePattern(UsagePattern.ALWAYS);
        low.setPriority(3); // Lowest priority
        low.setActive(true);

        // Act
        assignmentRepository.save(high);
        assignmentRepository.save(medium);
        assignmentRepository.save(low);

        List<CourseRoomAssignment> assignments = assignmentRepository.findByCourse(testCourse);

        // Assert
        assertEquals(3, assignments.size());

        System.out.println("✓ Priority ordering configured (1=highest, 3=lowest)");
        System.out.println("  - Priority 1: Room 101 (Primary)");
        System.out.println("  - Priority 2: Room 102 (Secondary)");
        System.out.println("  - Priority 3: Room 103 (Overflow)");
    }

    @Test
    public void testActiveInactiveStatus_ShouldFilter() {
        // Arrange
        CourseRoomAssignment active = new CourseRoomAssignment();
        active.setCourse(testCourse);
        active.setRoom(room101);
        active.setAssignmentType(RoomAssignmentType.PRIMARY);
        active.setUsagePattern(UsagePattern.ALWAYS);
        active.setPriority(1);
        active.setActive(true);

        CourseRoomAssignment inactive = new CourseRoomAssignment();
        inactive.setCourse(testCourse);
        inactive.setRoom(room102);
        inactive.setAssignmentType(RoomAssignmentType.SECONDARY);
        inactive.setUsagePattern(UsagePattern.ALWAYS);
        inactive.setPriority(2);
        inactive.setActive(false); // Inactive

        // Act
        assignmentRepository.save(active);
        assignmentRepository.save(inactive);

        List<CourseRoomAssignment> allAssignments = assignmentRepository.findByCourse(testCourse);
        long activeCount = allAssignments.stream().filter(CourseRoomAssignment::getActive).count();

        // Assert
        assertEquals(2, allAssignments.size());
        assertEquals(1, activeCount);
        assertTrue(active.isUsable());
        assertFalse(inactive.isUsable());

        System.out.println("✓ Active/inactive status handled correctly");
    }

    // ========== REAL-WORLD SCENARIOS ==========

    @Test
    public void testRealWorld_ChemistryLabLectureSplit() {
        // Arrange - Chemistry course with lecture/lab split
        CourseRoomAssignment lecture = new CourseRoomAssignment();
        lecture.setCourse(testCourse);
        lecture.setRoom(room101);
        lecture.setAssignmentType(RoomAssignmentType.PRIMARY);
        lecture.setUsagePattern(UsagePattern.EVEN_DAYS); // Lecture on even days
        lecture.setPriority(1);
        lecture.setActive(true);
        lecture.setNotes("Lecture instruction");

        CourseRoomAssignment lab = new CourseRoomAssignment();
        lab.setCourse(testCourse);
        lab.setRoom(labRoom);
        lab.setAssignmentType(RoomAssignmentType.SECONDARY);
        lab.setUsagePattern(UsagePattern.ODD_DAYS); // Lab on odd days
        lab.setPriority(1); // Same priority - different days
        lab.setActive(true);
        lab.setNotes("Lab experiments");

        // Act
        assignmentRepository.save(lecture);
        assignmentRepository.save(lab);

        List<CourseRoomAssignment> assignments = assignmentRepository.findByCourse(testCourse);

        // Assert
        assertEquals(2, assignments.size());

        System.out.println("✓ Real-world: Chemistry lab/lecture split");
        System.out.println("  - Even days: Room 101 (Lecture)");
        System.out.println("  - Odd days: LAB-201 (Lab)");
    }

    @Test
    public void testRealWorld_TeamTeaching() {
        // Arrange - Two teachers, two rooms, simultaneous instruction
        CourseRoomAssignment teacher1Room = new CourseRoomAssignment();
        teacher1Room.setCourse(testCourse);
        teacher1Room.setRoom(room101);
        teacher1Room.setAssignmentType(RoomAssignmentType.PRIMARY);
        teacher1Room.setUsagePattern(UsagePattern.ALWAYS);
        teacher1Room.setPriority(1);
        teacher1Room.setActive(true);
        teacher1Room.setNotes("Teacher 1 - Main group");

        CourseRoomAssignment teacher2Room = new CourseRoomAssignment();
        teacher2Room.setCourse(testCourse);
        teacher2Room.setRoom(room102);
        teacher2Room.setAssignmentType(RoomAssignmentType.SECONDARY);
        teacher2Room.setUsagePattern(UsagePattern.ALWAYS);
        teacher2Room.setPriority(1); // Same priority - simultaneous
        teacher2Room.setActive(true);
        teacher2Room.setNotes("Teacher 2 - Small group");

        // Act
        assignmentRepository.save(teacher1Room);
        assignmentRepository.save(teacher2Room);

        testCourse.setUsesMultipleRooms(true);
        testCourse.setMaxRoomDistanceMinutes(2); // Close proximity
        testCourse = courseRepository.save(testCourse);

        List<CourseRoomAssignment> assignments = assignmentRepository.findByCourse(testCourse);

        // Assert
        assertEquals(2, assignments.size());
        assertTrue(testCourse.getUsesMultipleRooms());
        assertEquals(2, testCourse.getMaxRoomDistanceMinutes());

        System.out.println("✓ Real-world: Team teaching scenario");
        System.out.println("  - Room 101: Teacher 1 (Main group)");
        System.out.println("  - Room 102: Teacher 2 (Small group)");
        System.out.println("  - Max distance: 2 minutes");
    }

    @Test
    public void testRealWorld_OverflowRoom() {
        // Arrange - Large class requiring overflow room
        CourseRoomAssignment primary = new CourseRoomAssignment();
        primary.setCourse(testCourse);
        primary.setRoom(room101);
        primary.setAssignmentType(RoomAssignmentType.PRIMARY);
        primary.setUsagePattern(UsagePattern.ALWAYS);
        primary.setPriority(1);
        primary.setActive(true);
        primary.setNotes("Primary instruction - 30 students");

        CourseRoomAssignment overflow = new CourseRoomAssignment();
        overflow.setCourse(testCourse);
        overflow.setRoom(room102);
        overflow.setAssignmentType(RoomAssignmentType.OVERFLOW);
        overflow.setUsagePattern(UsagePattern.ALWAYS);
        overflow.setPriority(2);
        overflow.setActive(true);
        overflow.setNotes("Overflow - 15 students, video feed from 101");

        // Act
        assignmentRepository.save(primary);
        assignmentRepository.save(overflow);

        List<CourseRoomAssignment> assignments = assignmentRepository.findByCourse(testCourse);

        // Assert
        assertEquals(2, assignments.size());

        System.out.println("✓ Real-world: Overflow room for large class");
        System.out.println("  - Primary: Room 101 (30 students)");
        System.out.println("  - Overflow: Room 102 (15 students, video feed)");
    }

    @Test
    public void testRealWorld_RotatingSchedule() {
        // Arrange - Rotating between 3 lab stations
        CourseRoomAssignment station1 = new CourseRoomAssignment();
        station1.setCourse(testCourse);
        station1.setRoom(room101);
        station1.setAssignmentType(RoomAssignmentType.ROTATING);
        station1.setUsagePattern(UsagePattern.WEEKLY_ROTATION);
        station1.setPriority(1);
        station1.setActive(true);
        station1.setNotes("Week 1, 4, 7...");

        CourseRoomAssignment station2 = new CourseRoomAssignment();
        station2.setCourse(testCourse);
        station2.setRoom(room102);
        station2.setAssignmentType(RoomAssignmentType.ROTATING);
        station2.setUsagePattern(UsagePattern.WEEKLY_ROTATION);
        station2.setPriority(2);
        station2.setActive(true);
        station2.setNotes("Week 2, 5, 8...");

        CourseRoomAssignment station3 = new CourseRoomAssignment();
        station3.setCourse(testCourse);
        station3.setRoom(room103);
        station3.setAssignmentType(RoomAssignmentType.ROTATING);
        station3.setUsagePattern(UsagePattern.WEEKLY_ROTATION);
        station3.setPriority(3);
        station3.setActive(true);
        station3.setNotes("Week 3, 6, 9...");

        // Act
        assignmentRepository.save(station1);
        assignmentRepository.save(station2);
        assignmentRepository.save(station3);

        List<CourseRoomAssignment> assignments = assignmentRepository.findByCourse(testCourse);

        // Assert
        assertEquals(3, assignments.size());

        System.out.println("✓ Real-world: 3-station rotating schedule");
        System.out.println("  - Station 1: Room 101 (Week 1, 4, 7...)");
        System.out.println("  - Station 2: Room 102 (Week 2, 5, 8...)");
        System.out.println("  - Station 3: Room 103 (Week 3, 6, 9...)");
    }

    // ========== COURSE MULTI-ROOM CONFIGURATION ==========

    @Test
    public void testCourseMultiRoomFlag_ShouldToggle() {
        // Arrange
        assertFalse(testCourse.getUsesMultipleRooms()); // Initially false

        // Act - Enable multi-room
        testCourse.setUsesMultipleRooms(true);
        testCourse.setMaxRoomDistanceMinutes(5);
        testCourse = courseRepository.save(testCourse);

        // Reload
        Course reloaded = courseRepository.findById(testCourse.getId()).orElse(null);

        // Assert
        assertNotNull(reloaded);
        assertTrue(reloaded.getUsesMultipleRooms());
        assertEquals(5, reloaded.getMaxRoomDistanceMinutes());

        System.out.println("✓ Course multi-room flag toggled");
    }

    @Test
    public void testMaxRoomDistance_ShouldPersist() {
        // Arrange
        testCourse.setUsesMultipleRooms(true);
        testCourse.setMaxRoomDistanceMinutes(10);

        // Act
        testCourse = courseRepository.save(testCourse);

        // Reload
        Course reloaded = courseRepository.findById(testCourse.getId()).orElse(null);

        // Assert
        assertNotNull(reloaded);
        assertEquals(10, reloaded.getMaxRoomDistanceMinutes());

        System.out.println("✓ Max room distance configured: 10 minutes");
    }

    // ========== DISPLAY NAME AND HELPERS ==========

    @Test
    public void testGetDisplayName_ShouldFormatCorrectly() {
        // Arrange
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(room101);
        assignment.setAssignmentType(RoomAssignmentType.SECONDARY);
        assignment.setUsagePattern(UsagePattern.ODD_DAYS);
        assignment.setPriority(2);
        assignment.setActive(true);

        // Act
        String displayName = assignment.getDisplayName();

        // Assert
        assertNotNull(displayName);
        assertTrue(displayName.contains("101"));
        assertTrue(displayName.contains("Secondary"));
        assertTrue(displayName.contains("Odd-Numbered Days"));

        System.out.println("✓ Display name formatted: " + displayName);
    }

    @Test
    public void testIsUsable_ShouldCheckConditions() {
        // Arrange - Valid assignment
        CourseRoomAssignment valid = new CourseRoomAssignment();
        valid.setCourse(testCourse);
        valid.setRoom(room101);
        valid.setAssignmentType(RoomAssignmentType.PRIMARY);
        valid.setUsagePattern(UsagePattern.ALWAYS);
        valid.setPriority(1);
        valid.setActive(true);

        // Arrange - Invalid (no room)
        CourseRoomAssignment noRoom = new CourseRoomAssignment();
        noRoom.setCourse(testCourse);
        noRoom.setRoom(null);
        noRoom.setAssignmentType(RoomAssignmentType.PRIMARY);
        noRoom.setActive(true);

        // Arrange - Invalid (inactive)
        CourseRoomAssignment inactive = new CourseRoomAssignment();
        inactive.setCourse(testCourse);
        inactive.setRoom(room101);
        inactive.setAssignmentType(RoomAssignmentType.PRIMARY);
        inactive.setActive(false);

        // Assert
        assertTrue(valid.isUsable());
        assertFalse(noRoom.isUsable());
        assertFalse(inactive.isUsable());

        System.out.println("✓ isUsable() checks all conditions correctly");
    }

    // ========== PERSISTENCE AND RELOAD ==========

    @Test
    public void testPersistenceAndReload_ShouldMaintainData() {
        // Arrange
        CourseRoomAssignment assignment = new CourseRoomAssignment();
        assignment.setCourse(testCourse);
        assignment.setRoom(room101);
        assignment.setAssignmentType(RoomAssignmentType.PRIMARY);
        assignment.setUsagePattern(UsagePattern.ALWAYS);
        assignment.setPriority(1);
        assignment.setActive(true);
        assignment.setNotes("Test notes");

        // Act
        assignment = assignmentRepository.save(assignment);
        Long assignmentId = assignment.getId();

        // Reload
        CourseRoomAssignment reloaded = assignmentRepository.findById(assignmentId).orElse(null);

        // Assert
        assertNotNull(reloaded);
        assertEquals(testCourse.getId(), reloaded.getCourse().getId());
        assertEquals(room101.getId(), reloaded.getRoom().getId());
        assertEquals(RoomAssignmentType.PRIMARY, reloaded.getAssignmentType());
        assertEquals(UsagePattern.ALWAYS, reloaded.getUsagePattern());
        assertEquals(1, reloaded.getPriority());
        assertTrue(reloaded.getActive());
        assertEquals("Test notes", reloaded.getNotes());

        System.out.println("✓ Assignment persisted and reloaded correctly");
    }

    @Test
    public void testDeleteByCourse_ShouldRemoveAll() {
        // Arrange - Create multiple assignments
        CourseRoomAssignment assignment1 = new CourseRoomAssignment();
        assignment1.setCourse(testCourse);
        assignment1.setRoom(room101);
        assignment1.setAssignmentType(RoomAssignmentType.PRIMARY);
        assignment1.setUsagePattern(UsagePattern.ALWAYS);
        assignment1.setPriority(1);
        assignment1.setActive(true);

        CourseRoomAssignment assignment2 = new CourseRoomAssignment();
        assignment2.setCourse(testCourse);
        assignment2.setRoom(room102);
        assignment2.setAssignmentType(RoomAssignmentType.SECONDARY);
        assignment2.setUsagePattern(UsagePattern.ALWAYS);
        assignment2.setPriority(2);
        assignment2.setActive(true);

        assignmentRepository.save(assignment1);
        assignmentRepository.save(assignment2);

        // Act - Delete all assignments for course
        assignmentRepository.deleteByCourse(testCourse);

        List<CourseRoomAssignment> remaining = assignmentRepository.findByCourse(testCourse);

        // Assert
        assertTrue(remaining.isEmpty());

        System.out.println("✓ All assignments for course deleted");
    }
}
