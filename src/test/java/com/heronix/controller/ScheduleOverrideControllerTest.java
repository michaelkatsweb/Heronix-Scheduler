package com.heronix.controller;

import com.heronix.model.domain.*;
import com.heronix.model.enums.DayType;
import com.heronix.model.enums.RoomType;
import com.heronix.model.enums.SchedulePeriod;
import com.heronix.model.enums.ScheduleType;
import com.heronix.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ScheduleOverrideController
 * Tests manual schedule override business logic
 *
 * Note: This is a JavaFX controller. We test the business logic
 * (override management and audit trail) rather than UI interactions.
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class ScheduleOverrideControllerTest {

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private ScheduleOverrideRepository scheduleOverrideRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private ScheduleSlot testSlot;
    private Teacher testTeacher1;
    private Teacher testTeacher2;
    private Room testRoom1;
    private Room testRoom2;
    private Course testCourse;
    private Schedule testSchedule;

    @BeforeEach
    public void setup() {
        // Create test schedule
        testSchedule = new Schedule();
        testSchedule.setScheduleName("Test Schedule - Overrides");
        testSchedule.setStartDate(LocalDate.now());
        testSchedule.setEndDate(LocalDate.now().plusMonths(6));
        testSchedule.setPeriod(SchedulePeriod.SEMESTER);
        testSchedule.setScheduleType(ScheduleType.TRADITIONAL);
        testSchedule.setActive(true);
        testSchedule = scheduleRepository.save(testSchedule);

        // Create test teachers
        testTeacher1 = new Teacher();
        testTeacher1.setName("John Smith");
        testTeacher1.setEmployeeId("OVERRIDE-T001");
        testTeacher1.setActive(true);
        testTeacher1 = teacherRepository.save(testTeacher1);

        testTeacher2 = new Teacher();
        testTeacher2.setName("Jane Doe");
        testTeacher2.setEmployeeId("OVERRIDE-T002");
        testTeacher2.setActive(true);
        testTeacher2 = teacherRepository.save(testTeacher2);

        // Create test rooms
        testRoom1 = new Room();
        testRoom1.setRoomNumber("101");
        testRoom1.setRoomType(RoomType.CLASSROOM);
        testRoom1.setCapacity(30);
        testRoom1.setActive(true);
        testRoom1 = roomRepository.save(testRoom1);

        testRoom2 = new Room();
        testRoom2.setRoomNumber("102");
        testRoom2.setRoomType(RoomType.CLASSROOM);
        testRoom2.setCapacity(25);
        testRoom2.setActive(true);
        testRoom2 = roomRepository.save(testRoom2);

        // Create test course
        testCourse = new Course();
        testCourse.setCourseCode("MATH101");
        testCourse.setCourseName("Algebra I");
        testCourse.setActive(true);
        testCourse = courseRepository.save(testCourse);

        // Create test schedule slot
        testSlot = new ScheduleSlot();
        testSlot.setSchedule(testSchedule);
        testSlot.setCourse(testCourse);
        testSlot.setTeacher(testTeacher1);
        testSlot.setRoom(testRoom1);
        testSlot.setPeriodNumber(3);
        testSlot.setDayType(DayType.A_DAY);
        testSlot = scheduleSlotRepository.save(testSlot);
    }

    // ========== SCHEDULE OVERRIDE CRUD ==========

    @Test
    public void testCreateOverride_ShouldSaveAuditRecord() {
        // Arrange
        ScheduleOverride override = new ScheduleOverride();
        override.setScheduleSlot(testSlot);
        override.setChangedBy("admin");
        override.setChangedAt(LocalDateTime.now());
        override.setOldTeacher(testTeacher1);
        override.setNewTeacher(testTeacher2);
        override.setOldRoom(testRoom1);
        override.setNewRoom(testRoom1); // Same room
        override.setOverrideType("TEACHER");
        override.setReason("Teacher requested change");

        // Act
        ScheduleOverride saved = scheduleOverrideRepository.save(override);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("admin", saved.getChangedBy());
        assertEquals("TEACHER", saved.getOverrideType());
        assertEquals(testTeacher1, saved.getOldTeacher());
        assertEquals(testTeacher2, saved.getNewTeacher());

        System.out.println("✓ Override audit record created");
        System.out.println("  - Type: " + saved.getOverrideType());
        System.out.println("  - Changed by: " + saved.getChangedBy());
    }

    @Test
    public void testFindOverridesBySlot_ShouldReturnHistory() {
        // Arrange - Create multiple overrides
        ScheduleOverride override1 = createOverride("TEACHER", "First change");
        ScheduleOverride override2 = createOverride("ROOM", "Second change");

        // Act
        List<ScheduleOverride> overrides = scheduleOverrideRepository
            .findByScheduleSlotOrderByChangedAtDesc(testSlot);

        // Assert
        assertNotNull(overrides);
        assertTrue(overrides.size() >= 2);
        // Most recent first
        assertTrue(overrides.get(0).getChangedAt().isAfter(overrides.get(1).getChangedAt()) ||
                   overrides.get(0).getChangedAt().equals(overrides.get(1).getChangedAt()));

        System.out.println("✓ Found " + overrides.size() + " override(s) for slot");
    }

    @Test
    public void testDeleteOverride_ShouldRemoveAuditRecord() {
        // Arrange
        ScheduleOverride override = createOverride("TEACHER", "Test delete");
        Long overrideId = override.getId();

        // Act
        scheduleOverrideRepository.delete(override);

        // Assert
        Optional<ScheduleOverride> deleted = scheduleOverrideRepository.findById(overrideId);
        assertFalse(deleted.isPresent());

        System.out.println("✓ Override deleted successfully");
    }

    // ========== OVERRIDE TYPES ==========

    @Test
    public void testTeacherOverride_ShouldAuditTeacherChange() {
        // Arrange
        ScheduleOverride override = new ScheduleOverride();
        override.setScheduleSlot(testSlot);
        override.setChangedBy("principal");
        override.setChangedAt(LocalDateTime.now());
        override.setOldTeacher(testTeacher1);
        override.setNewTeacher(testTeacher2);
        override.setOldRoom(testRoom1);
        override.setNewRoom(testRoom1);
        override.setOverrideType("TEACHER");
        override.setReason("Emergency substitution");

        // Act
        ScheduleOverride saved = scheduleOverrideRepository.save(override);

        // Assert
        assertEquals("TEACHER", saved.getOverrideType());
        assertNotEquals(saved.getOldTeacher(), saved.getNewTeacher());
        assertEquals(saved.getOldRoom(), saved.getNewRoom());

        System.out.println("✓ Teacher override recorded");
        System.out.println("  - Old: " + saved.getOldTeacher().getName());
        System.out.println("  - New: " + saved.getNewTeacher().getName());
    }

    @Test
    public void testRoomOverride_ShouldAuditRoomChange() {
        // Arrange
        ScheduleOverride override = new ScheduleOverride();
        override.setScheduleSlot(testSlot);
        override.setChangedBy("admin");
        override.setChangedAt(LocalDateTime.now());
        override.setOldTeacher(testTeacher1);
        override.setNewTeacher(testTeacher1);
        override.setOldRoom(testRoom1);
        override.setNewRoom(testRoom2);
        override.setOverrideType("ROOM");
        override.setReason("Room renovation");

        // Act
        ScheduleOverride saved = scheduleOverrideRepository.save(override);

        // Assert
        assertEquals("ROOM", saved.getOverrideType());
        assertEquals(saved.getOldTeacher(), saved.getNewTeacher());
        assertNotEquals(saved.getOldRoom(), saved.getNewRoom());

        System.out.println("✓ Room override recorded");
        System.out.println("  - Old: " + saved.getOldRoom().getRoomNumber());
        System.out.println("  - New: " + saved.getNewRoom().getRoomNumber());
    }

    @Test
    public void testTeacherRoomOverride_ShouldAuditBothChanges() {
        // Arrange
        ScheduleOverride override = new ScheduleOverride();
        override.setScheduleSlot(testSlot);
        override.setChangedBy("scheduler");
        override.setChangedAt(LocalDateTime.now());
        override.setOldTeacher(testTeacher1);
        override.setNewTeacher(testTeacher2);
        override.setOldRoom(testRoom1);
        override.setNewRoom(testRoom2);
        override.setOverrideType("TEACHER_ROOM");
        override.setReason("Schedule conflict resolution");

        // Act
        ScheduleOverride saved = scheduleOverrideRepository.save(override);

        // Assert
        assertEquals("TEACHER_ROOM", saved.getOverrideType());
        assertNotEquals(saved.getOldTeacher(), saved.getNewTeacher());
        assertNotEquals(saved.getOldRoom(), saved.getNewRoom());

        System.out.println("✓ Teacher+Room override recorded");
    }

    // ========== SCHEDULE SLOT UPDATES ==========

    @Test
    public void testUpdateSlotTeacher_ShouldPersistChange() {
        // Arrange
        Teacher originalTeacher = testSlot.getTeacher();

        // Act
        testSlot.setTeacher(testTeacher2);
        ScheduleSlot updated = scheduleSlotRepository.save(testSlot);

        // Assert
        assertEquals(testTeacher2, updated.getTeacher());
        assertNotEquals(originalTeacher, updated.getTeacher());

        System.out.println("✓ Slot teacher updated");
        System.out.println("  - Original: " + originalTeacher.getName());
        System.out.println("  - Updated: " + updated.getTeacher().getName());
    }

    @Test
    public void testUpdateSlotRoom_ShouldPersistChange() {
        // Arrange
        Room originalRoom = testSlot.getRoom();

        // Act
        testSlot.setRoom(testRoom2);
        ScheduleSlot updated = scheduleSlotRepository.save(testSlot);

        // Assert
        assertEquals(testRoom2, updated.getRoom());
        assertNotEquals(originalRoom, updated.getRoom());

        System.out.println("✓ Slot room updated");
        System.out.println("  - Original: " + originalRoom.getRoomNumber());
        System.out.println("  - Updated: " + updated.getRoom().getRoomNumber());
    }

    @Test
    public void testPinAssignment_ShouldTrackPinner() {
        // Arrange
        assertNull(testSlot.getPinnedBy());

        // Act
        testSlot.setPinnedBy("admin");
        testSlot.setPinnedAt(LocalDateTime.now());
        ScheduleSlot updated = scheduleSlotRepository.save(testSlot);

        // Assert
        assertEquals("admin", updated.getPinnedBy());
        assertNotNull(updated.getPinnedAt());

        System.out.println("✓ Assignment pinned");
        System.out.println("  - Pinned by: " + updated.getPinnedBy());
    }

    // ========== AUDIT TRAIL ==========

    @Test
    public void testAuditTrail_ShouldMaintainChronology() throws InterruptedException {
        // Arrange - Create series of changes
        ScheduleOverride override1 = createOverride("TEACHER", "Change 1");
        Thread.sleep(10); // Ensure different timestamps
        ScheduleOverride override2 = createOverride("ROOM", "Change 2");
        Thread.sleep(10);
        ScheduleOverride override3 = createOverride("TEACHER_ROOM", "Change 3");

        // Act
        List<ScheduleOverride> history = scheduleOverrideRepository
            .findByScheduleSlotOrderByChangedAtDesc(testSlot);

        // Assert
        assertTrue(history.size() >= 3);
        // Verify chronological order (newest first)
        for (int i = 0; i < history.size() - 1; i++) {
            assertTrue(history.get(i).getChangedAt().isAfter(history.get(i + 1).getChangedAt()) ||
                      history.get(i).getChangedAt().equals(history.get(i + 1).getChangedAt()));
        }

        System.out.println("✓ Audit trail maintains chronology");
        System.out.println("  - Total changes: " + history.size());
    }

    @Test
    public void testOverrideReason_ShouldBeTracked() {
        // Arrange
        String reason = "Teacher emergency - family illness";
        ScheduleOverride override = createOverride("TEACHER", reason);

        // Act
        ScheduleOverride retrieved = scheduleOverrideRepository.findById(override.getId()).orElse(null);

        // Assert
        assertNotNull(retrieved);
        assertEquals(reason, retrieved.getReason());

        System.out.println("✓ Override reason tracked");
        System.out.println("  - Reason: " + retrieved.getReason());
    }

    @Test
    public void testOverrideUser_ShouldBeTracked() {
        // Arrange
        ScheduleOverride override = new ScheduleOverride();
        override.setScheduleSlot(testSlot);
        override.setChangedBy("principal.johnson");
        override.setChangedAt(LocalDateTime.now());
        override.setOldTeacher(testTeacher1);
        override.setNewTeacher(testTeacher2);
        override.setOverrideType("TEACHER");

        // Act
        ScheduleOverride saved = scheduleOverrideRepository.save(override);

        // Assert
        assertEquals("principal.johnson", saved.getChangedBy());
        assertNotNull(saved.getChangedAt());

        System.out.println("✓ Override user tracked");
        System.out.println("  - User: " + saved.getChangedBy());
        System.out.println("  - Time: " + saved.getChangedAt());
    }

    // ========== HELPER METHODS ==========

    private ScheduleOverride createOverride(String type, String reason) {
        ScheduleOverride override = new ScheduleOverride();
        override.setScheduleSlot(testSlot);
        override.setChangedBy("test-user");
        override.setChangedAt(LocalDateTime.now());
        override.setOldTeacher(testTeacher1);
        override.setNewTeacher(testTeacher2);
        override.setOldRoom(testRoom1);
        override.setNewRoom(testRoom2);
        override.setOverrideType(type);
        override.setReason(reason);
        return scheduleOverrideRepository.save(override);
    }
}
