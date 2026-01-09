package com.heronix.service.impl;

import com.heronix.model.domain.Teacher;
import com.heronix.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for TeacherServiceImpl
 *
 * Service: 21st of 65 services
 * File: src/test/java/com/eduscheduler/service/impl/TeacherServiceImplTest.java
 *
 * Tests cover:
 * - Get all active teachers
 * - Get teacher by ID
 * - Find all teachers (including inactive)
 * - Create teacher with validation and defaults
 * - Update teacher with validation
 * - Delete teacher (hard delete)
 * - Deactivate/activate teacher (soft delete)
 * - Search teachers by name
 * - Find teachers by department
 * - Load teacher with collections (eager fetch)
 * - Soft delete operations (mark as deleted, restore, get deleted)
 * - Null safety and edge cases
 *
 * @author Heronix Scheduling System Testing Team
 * @version 1.0.0
 * @since December 19, 2025
 */
@ExtendWith(MockitoExtension.class)
class TeacherServiceImplTest {

    @Mock(lenient = true)
    private TeacherRepository teacherRepository;

    @InjectMocks
    private TeacherServiceImpl service;

    private Teacher testTeacher1;
    private Teacher testTeacher2;

    @BeforeEach
    void setUp() {
        // Create test teacher 1 (active)
        testTeacher1 = new Teacher();
        testTeacher1.setId(1L);
        testTeacher1.setName("John Smith");
        testTeacher1.setDepartment("Mathematics");
        testTeacher1.setMaxHoursPerWeek(40);
        testTeacher1.setMaxConsecutiveHours(4);
        testTeacher1.setPreferredBreakMinutes(30);
        testTeacher1.setCurrentWeekHours(0);
        testTeacher1.setActive(true);
        testTeacher1.setDeleted(false);

        // Create test teacher 2 (inactive)
        testTeacher2 = new Teacher();
        testTeacher2.setId(2L);
        testTeacher2.setName("Jane Doe");
        testTeacher2.setDepartment("Science");
        testTeacher2.setActive(false);
        testTeacher2.setDeleted(false);
    }

    // ========== GET ALL ACTIVE TEACHERS TESTS ==========

    @Test
    void testGetAllActiveTeachers_WithActiveTeachers_ShouldReturnList() {
        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(testTeacher1));

        List<Teacher> result = service.getAllActiveTeachers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getActive());
    }

    @Test
    void testGetAllActiveTeachers_WithNoActiveTeachers_ShouldReturnEmptyList() {
        when(teacherRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        List<Teacher> result = service.getAllActiveTeachers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllActiveTeachers_WithMultipleActiveTeachers_ShouldReturnAll() {
        Teacher teacher3 = new Teacher();
        teacher3.setId(3L);
        teacher3.setName("Bob Johnson");
        teacher3.setActive(true);

        when(teacherRepository.findByActiveTrue()).thenReturn(Arrays.asList(testTeacher1, teacher3));

        List<Teacher> result = service.getAllActiveTeachers();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ========== GET TEACHER BY ID TESTS ==========

    @Test
    void testGetTeacherById_WithExistingTeacher_ShouldReturnTeacher() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher1));

        Teacher result = service.getTeacherById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Smith", result.getName());
    }

    @Test
    void testGetTeacherById_WithNonExistentTeacher_ShouldReturnNull() {
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        Teacher result = service.getTeacherById(999L);

        assertNull(result);
    }

    @Test
    void testGetTeacherById_WithNullName_ShouldNotCrash() {
        testTeacher1.setName(null);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher1));

        Teacher result = service.getTeacherById(1L);

        assertNotNull(result);
        assertNull(result.getName());
    }

    // ========== FIND ALL TESTS ==========

    @Test
    void testFindAll_WithTeachers_ShouldReturnAll() {
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1, testTeacher2));

        List<Teacher> result = service.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testFindAll_WithNoTeachers_ShouldReturnEmptyList() {
        when(teacherRepository.findAllActive()).thenReturn(Collections.emptyList());

        List<Teacher> result = service.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAll_ShouldIncludeInactiveTeachers() {
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1, testTeacher2));

        List<Teacher> result = service.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        // Verify both active and inactive are included
        assertTrue(result.stream().anyMatch(t -> t.getActive()));
        assertTrue(result.stream().anyMatch(t -> !t.getActive()));
    }

    // ========== CREATE TEACHER TESTS ==========

    @Test
    void testCreateTeacher_WithValidTeacher_ShouldCreateAndSetDefaults() {
        Teacher newTeacher = new Teacher();
        newTeacher.setName("New Teacher");

        when(teacherRepository.save(any(Teacher.class))).thenAnswer(i -> {
            Teacher saved = i.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        Teacher result = service.createTeacher(newTeacher);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("New Teacher", result.getName());
        // Verify defaults set
        assertEquals(40, result.getMaxHoursPerWeek());
        assertEquals(4, result.getMaxConsecutiveHours());
        assertEquals(30, result.getPreferredBreakMinutes());
        assertEquals(0, result.getCurrentWeekHours());
    }

    @Test
    void testCreateTeacher_WithNullTeacher_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.createTeacher(null);
        });
    }

    @Test
    void testCreateTeacher_WithNullName_ShouldThrowException() {
        Teacher newTeacher = new Teacher();
        newTeacher.setName(null);

        assertThrows(IllegalArgumentException.class, () -> {
            service.createTeacher(newTeacher);
        });
    }

    @Test
    void testCreateTeacher_WithEmptyName_ShouldThrowException() {
        Teacher newTeacher = new Teacher();
        newTeacher.setName("   ");

        assertThrows(IllegalArgumentException.class, () -> {
            service.createTeacher(newTeacher);
        });
    }

    @Test
    void testCreateTeacher_WithExistingDefaults_ShouldNotOverride() {
        Teacher newTeacher = new Teacher();
        newTeacher.setName("New Teacher");
        newTeacher.setMaxHoursPerWeek(35);
        newTeacher.setMaxConsecutiveHours(3);
        newTeacher.setPreferredBreakMinutes(45);
        newTeacher.setCurrentWeekHours(5);

        when(teacherRepository.save(any(Teacher.class))).thenAnswer(i -> i.getArgument(0));

        Teacher result = service.createTeacher(newTeacher);

        // Should preserve custom values
        assertEquals(35, result.getMaxHoursPerWeek());
        assertEquals(3, result.getMaxConsecutiveHours());
        assertEquals(45, result.getPreferredBreakMinutes());
        assertEquals(5, result.getCurrentWeekHours());
    }

    @Test
    void testCreateTeacher_WithNullMaxHoursPerWeek_ShouldSetDefault() {
        Teacher newTeacher = new Teacher();
        newTeacher.setName("New Teacher");
        newTeacher.setMaxHoursPerWeek(null);

        when(teacherRepository.save(any(Teacher.class))).thenAnswer(i -> i.getArgument(0));

        Teacher result = service.createTeacher(newTeacher);

        assertEquals(40, result.getMaxHoursPerWeek());
    }

    // ========== UPDATE TEACHER TESTS ==========

    @Test
    void testUpdateTeacher_WithValidTeacher_ShouldUpdate() {
        testTeacher1.setName("Updated Name");
        when(teacherRepository.existsById(1L)).thenReturn(true);
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher1);

        Teacher result = service.updateTeacher(testTeacher1);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
    }

    @Test
    void testUpdateTeacher_WithNullTeacher_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.updateTeacher(null);
        });
    }

    @Test
    void testUpdateTeacher_WithNullId_ShouldThrowException() {
        Teacher teacher = new Teacher();
        teacher.setId(null);
        teacher.setName("Test");

        assertThrows(IllegalArgumentException.class, () -> {
            service.updateTeacher(teacher);
        });
    }

    @Test
    void testUpdateTeacher_WithNonExistentId_ShouldThrowException() {
        Teacher teacher = new Teacher();
        teacher.setId(999L);
        teacher.setName("Test");

        when(teacherRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            service.updateTeacher(teacher);
        });
    }

    @Test
    void testUpdateTeacher_WithNullName_ShouldThrowException() {
        testTeacher1.setName(null);
        when(teacherRepository.existsById(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            service.updateTeacher(testTeacher1);
        });
    }

    @Test
    void testUpdateTeacher_WithEmptyName_ShouldThrowException() {
        testTeacher1.setName("  ");
        when(teacherRepository.existsById(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            service.updateTeacher(testTeacher1);
        });
    }

    // ========== DELETE TEACHER TESTS ==========

    @Test
    void testDeleteTeacher_WithExistingTeacher_ShouldDelete() {
        when(teacherRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> {
            service.deleteTeacher(1L);
        });

        verify(teacherRepository).deleteById(1L);
    }

    @Test
    void testDeleteTeacher_WithNonExistentTeacher_ShouldThrowException() {
        when(teacherRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            service.deleteTeacher(999L);
        });
    }

    // ========== DEACTIVATE TEACHER TESTS ==========

    @Test
    void testDeactivateTeacher_WithExistingTeacher_ShouldDeactivate() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher1));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher1);

        service.deactivateTeacher(1L);

        assertFalse(testTeacher1.getActive());
        verify(teacherRepository).save(testTeacher1);
    }

    @Test
    void testDeactivateTeacher_WithNonExistentTeacher_ShouldThrowException() {
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.deactivateTeacher(999L);
        });
    }

    @Test
    void testDeactivateTeacher_WithNullName_ShouldNotCrash() {
        testTeacher1.setName(null);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher1));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher1);

        assertDoesNotThrow(() -> {
            service.deactivateTeacher(1L);
        });

        assertFalse(testTeacher1.getActive());
    }

    // ========== ACTIVATE TEACHER TESTS ==========

    @Test
    void testActivateTeacher_WithExistingTeacher_ShouldActivate() {
        testTeacher2.setActive(false);
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(testTeacher2));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher2);

        service.activateTeacher(2L);

        assertTrue(testTeacher2.getActive());
        verify(teacherRepository).save(testTeacher2);
    }

    @Test
    void testActivateTeacher_WithNonExistentTeacher_ShouldThrowException() {
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.activateTeacher(999L);
        });
    }

    @Test
    void testActivateTeacher_WithNullName_ShouldNotCrash() {
        testTeacher2.setName(null);
        testTeacher2.setActive(false);
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(testTeacher2));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher2);

        assertDoesNotThrow(() -> {
            service.activateTeacher(2L);
        });

        assertTrue(testTeacher2.getActive());
    }

    // ========== SEARCH BY NAME TESTS ==========

    @Test
    void testSearchByName_WithMatchingTeachers_ShouldReturnMatches() {
        when(teacherRepository.findByNameContainingIgnoreCase("Smith"))
                .thenReturn(Arrays.asList(testTeacher1));

        List<Teacher> result = service.searchByName("Smith");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Smith", result.get(0).getName());
    }

    @Test
    void testSearchByName_WithNoMatches_ShouldReturnEmptyList() {
        when(teacherRepository.findByNameContainingIgnoreCase("NotFound"))
                .thenReturn(Collections.emptyList());

        List<Teacher> result = service.searchByName("NotFound");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchByName_WithNullSearchTerm_ShouldReturnAll() {
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1, testTeacher2));

        List<Teacher> result = service.searchByName(null);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testSearchByName_WithEmptySearchTerm_ShouldReturnAll() {
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1, testTeacher2));

        List<Teacher> result = service.searchByName("   ");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ========== FIND BY DEPARTMENT TESTS ==========

    @Test
    void testFindByDepartment_WithMatchingTeachers_ShouldReturnMatches() {
        when(teacherRepository.findByDepartment("Mathematics"))
                .thenReturn(Arrays.asList(testTeacher1));

        List<Teacher> result = service.findByDepartment("Mathematics");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Mathematics", result.get(0).getDepartment());
    }

    @Test
    void testFindByDepartment_WithNoMatches_ShouldReturnEmptyList() {
        when(teacherRepository.findByDepartment("Art"))
                .thenReturn(Collections.emptyList());

        List<Teacher> result = service.findByDepartment("Art");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByDepartment_WithNullDepartment_ShouldReturnAll() {
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1, testTeacher2));

        List<Teacher> result = service.findByDepartment(null);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testFindByDepartment_WithEmptyDepartment_ShouldReturnAll() {
        when(teacherRepository.findAllActive()).thenReturn(Arrays.asList(testTeacher1, testTeacher2));

        List<Teacher> result = service.findByDepartment("  ");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ========== LOAD TEACHER WITH COLLECTIONS TESTS ==========

    @Test
    void testLoadTeacherWithCollections_WithExistingTeacher_ShouldLoadAllCollections() {
        when(teacherRepository.findByIdWithCollections(1L)).thenReturn(Optional.of(testTeacher1));
        when(teacherRepository.findByIdWithSubjectCertifications(1L)).thenReturn(Optional.of(testTeacher1));
        when(teacherRepository.findByIdWithSpecialAssignments(1L)).thenReturn(Optional.of(testTeacher1));
        when(teacherRepository.findByIdWithCourses(1L)).thenReturn(Optional.of(testTeacher1));

        Teacher result = service.loadTeacherWithCollections(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(teacherRepository).findByIdWithCollections(1L);
        verify(teacherRepository).findByIdWithSubjectCertifications(1L);
        verify(teacherRepository).findByIdWithSpecialAssignments(1L);
        verify(teacherRepository).findByIdWithCourses(1L);
    }

    @Test
    void testLoadTeacherWithCollections_WithNonExistentTeacher_ShouldThrowException() {
        when(teacherRepository.findByIdWithCollections(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            service.loadTeacherWithCollections(999L);
        });
    }

    // ========== SOFT DELETE TESTS ==========

    @Test
    void testSoftDelete_WithExistingTeacher_ShouldMarkAsDeleted() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher1));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher1);

        service.softDelete(1L, "admin");

        assertTrue(testTeacher1.getDeleted());
        assertFalse(testTeacher1.getActive());
        assertNotNull(testTeacher1.getDeletedAt());
        assertEquals("admin", testTeacher1.getDeletedBy());
        verify(teacherRepository).save(testTeacher1);
    }

    @Test
    void testSoftDelete_WithNonExistentTeacher_ShouldThrowException() {
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.softDelete(999L, "admin");
        });
    }

    @Test
    void testSoftDelete_WithNullName_ShouldNotCrash() {
        testTeacher1.setName(null);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher1));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher1);

        assertDoesNotThrow(() -> {
            service.softDelete(1L, "admin");
        });

        assertTrue(testTeacher1.getDeleted());
    }

    // ========== RESTORE DELETED TESTS ==========

    @Test
    void testRestoreDeleted_WithDeletedTeacher_ShouldRestore() {
        testTeacher1.setDeleted(true);
        testTeacher1.setDeletedAt(LocalDateTime.now());
        testTeacher1.setDeletedBy("admin");
        testTeacher1.setActive(false);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher1));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher1);

        service.restoreDeleted(1L);

        assertFalse(testTeacher1.getDeleted());
        assertTrue(testTeacher1.getActive());
        assertNull(testTeacher1.getDeletedAt());
        assertNull(testTeacher1.getDeletedBy());
        verify(teacherRepository).save(testTeacher1);
    }

    @Test
    void testRestoreDeleted_WithNonDeletedTeacher_ShouldDoNothing() {
        testTeacher1.setDeleted(false);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher1));

        service.restoreDeleted(1L);

        // Should not save if not deleted
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void testRestoreDeleted_WithNullDeletedFlag_ShouldDoNothing() {
        testTeacher1.setDeleted(null);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher1));

        service.restoreDeleted(1L);

        // Should not save if deleted flag is null
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void testRestoreDeleted_WithNonExistentTeacher_ShouldThrowException() {
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.restoreDeleted(999L);
        });
    }

    @Test
    void testRestoreDeleted_WithNullName_ShouldNotCrash() {
        testTeacher1.setName(null);
        testTeacher1.setDeleted(true);
        testTeacher1.setDeletedAt(LocalDateTime.now());
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(testTeacher1));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(testTeacher1);

        assertDoesNotThrow(() -> {
            service.restoreDeleted(1L);
        });

        assertFalse(testTeacher1.getDeleted());
    }

    // ========== GET DELETED TESTS ==========

    @Test
    void testGetDeleted_WithDeletedTeachers_ShouldReturnList() {
        Teacher deletedTeacher = new Teacher();
        deletedTeacher.setId(10L);
        deletedTeacher.setName("Deleted Teacher");
        deletedTeacher.setDeleted(true);

        when(teacherRepository.findDeleted()).thenReturn(Arrays.asList(deletedTeacher));

        List<Teacher> result = service.getDeleted();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getDeleted());
    }

    @Test
    void testGetDeleted_WithNoDeletedTeachers_ShouldReturnEmptyList() {
        when(teacherRepository.findDeleted()).thenReturn(Collections.emptyList());

        List<Teacher> result = service.getDeleted();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
