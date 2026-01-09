package com.heronix.model.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Phase 5F PE Activity Tagging System
 * Tests Room.activityTags and Course.activityType functionality
 */
@DisplayName("Phase 5F: PE Activity Tagging Tests")
class Phase5FActivityTaggingTest {

    @Test
    @DisplayName("Room: Set and get activity tags")
    void testRoomActivityTagsBasic() {
        Room room = new Room();
        room.setActivityTags("Basketball,Volleyball,Soccer");

        assertEquals("Basketball,Volleyball,Soccer", room.getActivityTags());
    }

    @Test
    @DisplayName("Room: supportsActivity() - case insensitive")
    void testRoomSupportsActivity() {
        Room room = new Room();
        room.setActivityTags("Basketball,Volleyball,Indoor Soccer");

        // Case insensitive matching
        assertTrue(room.supportsActivity("Basketball"));
        assertTrue(room.supportsActivity("basketball"));
        assertTrue(room.supportsActivity("BASKETBALL"));
        assertTrue(room.supportsActivity("Volleyball"));
        assertTrue(room.supportsActivity("Indoor Soccer"));

        // Not supported
        assertFalse(room.supportsActivity("Weights"));
        assertFalse(room.supportsActivity("Dance"));
    }

    @Test
    @DisplayName("Room: supportsActivity() - handles null/empty")
    void testRoomSupportsActivityNullHandling() {
        Room room = new Room();
        room.setActivityTags(null);

        assertFalse(room.supportsActivity("Basketball"));

        room.setActivityTags("");
        assertFalse(room.supportsActivity("Basketball"));
    }

    @Test
    @DisplayName("Room: getActivityList() - returns correct list")
    void testRoomGetActivityList() {
        Room room = new Room();
        room.setActivityTags("Basketball,Volleyball,Soccer");

        List<String> activities = room.getActivityList();

        assertEquals(3, activities.size());
        assertTrue(activities.contains("Basketball"));
        assertTrue(activities.contains("Volleyball"));
        assertTrue(activities.contains("Soccer"));
    }

    @Test
    @DisplayName("Room: getActivityList() - handles empty/null")
    void testRoomGetActivityListEmpty() {
        Room room = new Room();

        // Null tags
        room.setActivityTags(null);
        assertTrue(room.getActivityList().isEmpty());

        // Empty tags
        room.setActivityTags("");
        assertTrue(room.getActivityList().isEmpty());

        // Whitespace only
        room.setActivityTags("   ");
        assertTrue(room.getActivityList().isEmpty());
    }

    @Test
    @DisplayName("Room: hasActivityTags() - returns correct boolean")
    void testRoomHasActivityTags() {
        Room room = new Room();

        // No tags
        room.setActivityTags(null);
        assertFalse(room.hasActivityTags());

        room.setActivityTags("");
        assertFalse(room.hasActivityTags());

        // Has tags
        room.setActivityTags("Basketball");
        assertTrue(room.hasActivityTags());

        room.setActivityTags("Basketball,Volleyball");
        assertTrue(room.hasActivityTags());
    }

    @Test
    @DisplayName("Room: addActivityTag() - adds new tag")
    void testRoomAddActivityTag() {
        Room room = new Room();

        // Add to empty
        room.setActivityTags(null);
        room.addActivityTag("Basketball");
        assertEquals("Basketball", room.getActivityTags());

        // Add to existing
        room.addActivityTag("Volleyball");
        assertEquals("Basketball,Volleyball", room.getActivityTags());

        // Don't duplicate
        room.addActivityTag("Basketball");
        assertEquals("Basketball,Volleyball", room.getActivityTags());
    }

    @Test
    @DisplayName("Room: removeActivityTag() - removes tag")
    void testRoomRemoveActivityTag() {
        Room room = new Room();
        room.setActivityTags("Basketball,Volleyball,Soccer");

        // Remove middle
        room.removeActivityTag("Volleyball");
        assertEquals("Basketball,Soccer", room.getActivityTags());

        // Remove first
        room.removeActivityTag("Basketball");
        assertEquals("Soccer", room.getActivityTags());

        // Remove last
        room.removeActivityTag("Soccer");
        assertEquals("", room.getActivityTags());
    }

    @Test
    @DisplayName("Room: removeActivityTag() - case insensitive")
    void testRoomRemoveActivityTagCaseInsensitive() {
        Room room = new Room();
        room.setActivityTags("Basketball,Volleyball");

        room.removeActivityTag("basketball");
        assertEquals("Volleyball", room.getActivityTags());
    }

    @Test
    @DisplayName("Course: Set and get activity type")
    void testCourseActivityTypeBasic() {
        Course course = new Course();
        course.setActivityType("Basketball");

        assertEquals("Basketball", course.getActivityType());
    }

    @Test
    @DisplayName("Course: Activity type handles null")
    void testCourseActivityTypeNull() {
        Course course = new Course();
        course.setActivityType(null);

        assertNull(course.getActivityType());
    }

    @Test
    @DisplayName("Integration: Match course to appropriate room")
    void testCourseRoomMatching() {
        // Create gymnasium with basketball support
        Room gym = new Room();
        gym.setRoomNumber("Gym 1");
        gym.setActivityTags("Basketball,Volleyball,General PE");

        // Create weight room
        Room weightRoom = new Room();
        weightRoom.setRoomNumber("Weight Room");
        weightRoom.setActivityTags("Weights,Strength Training,Conditioning");

        // Create basketball course
        Course basketballCourse = new Course();
        basketballCourse.setCourseCode("PE-BB-101");
        basketballCourse.setCourseName("PE - Basketball");
        basketballCourse.setActivityType("Basketball");

        // Create weights course
        Course weightsCourse = new Course();
        weightsCourse.setCourseCode("PE-WT-101");
        weightsCourse.setCourseName("PE - Weights");
        weightsCourse.setActivityType("Weights");

        // Verify matching
        assertTrue(gym.supportsActivity(basketballCourse.getActivityType()));
        assertFalse(weightRoom.supportsActivity(basketballCourse.getActivityType()));

        assertFalse(gym.supportsActivity(weightsCourse.getActivityType()));
        assertTrue(weightRoom.supportsActivity(weightsCourse.getActivityType()));
    }

    @Test
    @DisplayName("Real-world scenario: Multiple gymnasiums with different capabilities")
    void testMultipleGymnasiums() {
        // Main gymnasium - all activities
        Room mainGym = new Room();
        mainGym.setRoomNumber("Main Gymnasium");
        mainGym.setActivityTags("Basketball,Volleyball,Indoor Soccer,Badminton,General PE");

        // Auxiliary gymnasium - smaller activities
        Room auxGym = new Room();
        auxGym.setRoomNumber("Auxiliary Gymnasium");
        auxGym.setActivityTags("Badminton,Table Tennis,Adaptive PE,General PE");

        // Competition gymnasium - competitive sports only
        Room compGym = new Room();
        compGym.setRoomNumber("Competition Gymnasium");
        compGym.setActivityTags("Basketball,Volleyball,Wrestling,Assemblies");

        // Courses
        Course basketball = new Course();
        basketball.setActivityType("Basketball");

        Course badminton = new Course();
        badminton.setActivityType("Badminton");

        Course tableTennis = new Course();
        tableTennis.setActivityType("Table Tennis");

        // Verify appropriate assignments
        assertTrue(mainGym.supportsActivity(basketball.getActivityType()));
        assertFalse(auxGym.supportsActivity(basketball.getActivityType()));
        assertTrue(compGym.supportsActivity(basketball.getActivityType()));

        assertTrue(mainGym.supportsActivity(badminton.getActivityType()));
        assertTrue(auxGym.supportsActivity(badminton.getActivityType()));
        assertFalse(compGym.supportsActivity(badminton.getActivityType()));

        assertFalse(mainGym.supportsActivity(tableTennis.getActivityType()));
        assertTrue(auxGym.supportsActivity(tableTennis.getActivityType()));
        assertFalse(compGym.supportsActivity(tableTennis.getActivityType()));
    }

    @Test
    @DisplayName("Edge case: Comma with spaces handling")
    void testCommaWithSpacesHandling() {
        Room room = new Room();
        room.setActivityTags("Basketball, Volleyball, Soccer");

        // Should still work with spaces
        assertTrue(room.supportsActivity("Basketball"));
        assertTrue(room.supportsActivity("Volleyball"));
        assertTrue(room.supportsActivity("Soccer"));

        List<String> activities = room.getActivityList();
        assertEquals(3, activities.size());
    }

    @Test
    @DisplayName("Edge case: Empty activity type for non-PE courses")
    void testEmptyActivityTypeForNonPE() {
        Course mathCourse = new Course();
        mathCourse.setCourseCode("MATH-101");
        mathCourse.setCourseName("Algebra I");
        mathCourse.setSubject("Mathematics");
        mathCourse.setActivityType(null); // Non-PE courses don't need activity type

        assertNull(mathCourse.getActivityType());
    }
}
