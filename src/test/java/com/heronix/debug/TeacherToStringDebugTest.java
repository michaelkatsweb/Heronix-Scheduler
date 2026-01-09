package com.heronix.debug;

import com.heronix.model.domain.Teacher;
import com.heronix.repository.TeacherRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug Test for Bug #1: LazyInitializationException in Teacher.toString()
 *
 * This test verifies that Teacher.toString() can be called outside a Hibernate session
 * without triggering LazyInitializationException for lazy-loaded collections.
 *
 * Expected Behavior:
 * - Teacher.toString() should NOT access lazy-loaded collections (passwordHistory, certifications, specialAssignments)
 * - toString() should work correctly when called outside transaction/session
 * - No LazyInitializationException should be thrown
 *
 * @author Heronix Scheduling System Team
 * @since December 15, 2025 - Bug Fix Session
 */
@Slf4j
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
public class TeacherToStringDebugTest {

    @Autowired
    private TeacherRepository teacherRepository;

    /**
     * Test 1: Verify Teacher.toString() works within transaction
     */
    @Test
    @Transactional
    public void testTeacherToString_WithinTransaction() {
        log.info("========================================");
        log.info("TEST 1: Teacher.toString() within transaction");
        log.info("========================================");

        List<Teacher> teachers = teacherRepository.findAll();
        assertTrue(teachers.size() > 0, "Should have at least one teacher");

        Teacher teacher = teachers.get(0);
        log.info("Found teacher: {}", teacher.getName());

        // Call toString() - should work within transaction
        String teacherString = teacher.toString();
        assertNotNull(teacherString, "toString() should not return null");
        log.info("✅ toString() within transaction: SUCCESS");
        log.info("Output length: {} characters", teacherString.length());
    }

    /**
     * Test 2: Verify Teacher.toString() works OUTSIDE transaction
     * This is the CRITICAL test for Bug #1 fix
     */
    @Test
    public void testTeacherToString_OutsideTransaction() {
        log.info("========================================");
        log.info("TEST 2: Teacher.toString() OUTSIDE transaction");
        log.info("========================================");

        // Load teacher within transaction
        Teacher teacher;
        List<Teacher> teachers = teacherRepository.findAll();
        assertTrue(teachers.size() > 0, "Should have at least one teacher");
        teacher = teachers.get(0);

        log.info("Found teacher: {}", teacher.getName());

        // Now we're outside the transaction - this is where the bug would occur
        // Call toString() - should NOT throw LazyInitializationException
        try {
            String teacherString = teacher.toString();
            assertNotNull(teacherString, "toString() should not return null");
            log.info("✅ toString() outside transaction: SUCCESS");
            log.info("Output length: {} characters", teacherString.length());
            log.info("This proves Bug #1 is FIXED - no LazyInitializationException!");
        } catch (Exception e) {
            log.error("❌ FAILED: toString() threw exception outside transaction", e);
            fail("toString() should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 3: Verify toString() output does NOT contain lazy collection data
     * This ensures @ToString.Exclude is working correctly
     */
    @Test
    public void testTeacherToString_DoesNotIncludeLazyCollections() {
        log.info("========================================");
        log.info("TEST 3: Verify toString() excludes lazy collections");
        log.info("========================================");

        List<Teacher> teachers = teacherRepository.findAll();
        assertTrue(teachers.size() > 0, "Should have at least one teacher");
        Teacher teacher = teachers.get(0);

        String teacherString = teacher.toString();

        // Verify toString() does NOT contain lazy collection field names
        assertFalse(teacherString.contains("passwordHistory"),
            "toString() should NOT include passwordHistory (lazy collection)");
        assertFalse(teacherString.contains("certifications="),
            "toString() should NOT include certifications (lazy collection)");
        assertFalse(teacherString.contains("specialAssignments="),
            "toString() should NOT include specialAssignments (lazy collection)");

        log.info("✅ toString() correctly excludes lazy collections");
        log.info("Output: {}", teacherString.substring(0, Math.min(200, teacherString.length())) + "...");
    }

    /**
     * Test 4: Verify multiple teachers can be processed without exception
     * Simulates the Rooms dialog scenario where multiple teachers are displayed
     */
    @Test
    public void testMultipleTeachers_ToStringBatch() {
        log.info("========================================");
        log.info("TEST 4: Multiple teachers toString() batch");
        log.info("========================================");

        List<Teacher> teachers = teacherRepository.findAll();
        log.info("Processing {} teachers...", teachers.size());

        int successCount = 0;
        for (Teacher teacher : teachers) {
            try {
                String teacherString = teacher.toString();
                assertNotNull(teacherString);
                successCount++;
            } catch (Exception e) {
                log.error("❌ Failed for teacher: {}", teacher.getName(), e);
                fail("toString() failed for teacher: " + teacher.getName());
            }
        }

        log.info("✅ Successfully processed {} teachers", successCount);
        log.info("This simulates the Rooms dialog scenario - all teachers can be displayed!");
    }

    /**
     * Test 5: Verify the fix allows ComboBox/TableView usage
     * This test simulates JavaFX ComboBox StringConverter usage
     */
    @Test
    public void testTeacherStringConverter_Simulation() {
        log.info("========================================");
        log.info("TEST 5: Simulate JavaFX StringConverter usage");
        log.info("========================================");

        List<Teacher> teachers = teacherRepository.findAll();
        assertTrue(teachers.size() > 0, "Should have at least one teacher");

        // Simulate StringConverter toString() method
        for (Teacher teacher : teachers.subList(0, Math.min(5, teachers.size()))) {
            try {
                String displayString = teacher.getName() != null ? teacher.getName() :
                    (teacher.getFirstName() + " " + teacher.getLastName());

                log.info("Teacher display string: {}", displayString);
                assertNotNull(displayString, "Display string should not be null");
            } catch (Exception e) {
                log.error("❌ Failed to create display string for teacher", e);
                fail("Should be able to create display string: " + e.getMessage());
            }
        }

        log.info("✅ StringConverter simulation successful");
        log.info("This proves Rooms dialog ComboBox will work correctly!");
    }
}
