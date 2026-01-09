package com.heronix.service.impl;

import com.heronix.model.domain.Course;
import com.heronix.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for CourseServiceImpl
 *
 * Service: 20th of 65 services
 * File: src/test/java/com/eduscheduler/service/impl/CourseServiceImplTest.java
 *
 * Tests cover:
 * - Get all active courses
 * - Get course by ID
 * - Null safety and edge cases
 *
 * @author Heronix Scheduling System Testing Team
 * @version 1.0.0
 * @since December 19, 2025
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock(lenient = true)
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseServiceImpl service;

    private Course testCourse1;
    private Course testCourse2;

    @BeforeEach
    void setUp() {
        // Create test course 1 (active)
        testCourse1 = new Course();
        testCourse1.setId(1L);
        testCourse1.setCourseCode("MATH101");
        testCourse1.setCourseName("Algebra I");
        testCourse1.setCredits(1.0);
        testCourse1.setActive(true);

        // Create test course 2 (inactive)
        testCourse2 = new Course();
        testCourse2.setId(2L);
        testCourse2.setCourseCode("ENG101");
        testCourse2.setCourseName("English I");
        testCourse2.setCredits(1.0);
        testCourse2.setActive(false);
    }

    // ========== GET ALL ACTIVE COURSES TESTS ==========

    @Test
    void testGetAllActiveCourses_WithActiveCourses_ShouldReturnList() {
        when(courseRepository.findByActiveTrue()).thenReturn(Arrays.asList(testCourse1));

        List<Course> result = service.getAllActiveCourses();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getActive());
    }

    @Test
    void testGetAllActiveCourses_WithNoActiveCourses_ShouldReturnEmptyList() {
        when(courseRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        List<Course> result = service.getAllActiveCourses();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllActiveCourses_WithMultipleActiveCourses_ShouldReturnAll() {
        Course course3 = new Course();
        course3.setId(3L);
        course3.setCourseCode("SCI101");
        course3.setCourseName("Biology I");
        course3.setActive(true);

        when(courseRepository.findByActiveTrue()).thenReturn(Arrays.asList(testCourse1, course3));

        List<Course> result = service.getAllActiveCourses();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ========== GET COURSE BY ID TESTS ==========

    @Test
    void testGetCourseById_WithExistingCourse_ShouldReturnCourse() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse1));

        Course result = service.getCourseById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("MATH101", result.getCourseCode());
        assertEquals("Algebra I", result.getCourseName());
    }

    @Test
    void testGetCourseById_WithNonExistentCourse_ShouldReturnNull() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        Course result = service.getCourseById(999L);

        assertNull(result);
    }

    @Test
    void testGetCourseById_WithNullCourseName_ShouldNotCrash() {
        testCourse1.setCourseName(null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse1));

        Course result = service.getCourseById(1L);

        assertNotNull(result);
        assertNull(result.getCourseName());
    }

    @Test
    void testGetCourseById_WithNullCourseCode_ShouldNotCrash() {
        testCourse1.setCourseCode(null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse1));

        Course result = service.getCourseById(1L);

        assertNotNull(result);
        assertNull(result.getCourseCode());
    }

    @Test
    void testGetCourseById_WithBothNullNameAndCode_ShouldNotCrash() {
        testCourse1.setCourseName(null);
        testCourse1.setCourseCode(null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse1));

        Course result = service.getCourseById(1L);

        assertNotNull(result);
        assertNull(result.getCourseName());
        assertNull(result.getCourseCode());
    }
}
