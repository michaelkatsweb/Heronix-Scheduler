package com.heronix.service.impl;

import com.heronix.model.dto.*;
import com.heronix.repository.*;
import com.heronix.service.DataManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Data Management Service Implementation
 * Location: src/main/java/com/eduscheduler/service/impl/DataManagementServiceImpl.java
 * 
 * ⚠️ WARNING: This service PERMANENTLY deletes data!
 * 
 * Handles deletion of students, teachers, courses, rooms, events
 * (DatabaseCleanupService handles schedules)
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataManagementServiceImpl implements DataManagementService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final EventRepository eventRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;

    // ========================================================================
    // DELETE ALL METHODS
    // ========================================================================

    @Override
    @Transactional
    public CleanupResult deleteAllStudents() {
        log.warn("⚠️ DELETING ALL STUDENTS");
        
        CleanupResult result = new CleanupResult("Delete All Students");
        
        try {
            long count = studentRepository.count();
            studentRepository.deleteAll();
            
            result.setSuccess(true);
            result.setDeletedCount((int) count);
            result.addDeletedEntityType("Students", (int) count);
            
            log.info("✅ Deleted {} students", count);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete students: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional
    public CleanupResult deleteAllTeachers() {
        log.warn("⚠️ DELETING ALL TEACHERS");
        
        CleanupResult result = new CleanupResult("Delete All Teachers");
        
        try {
            long count = teacherRepository.countByActiveTrue();
            teacherRepository.deleteAll();
            
            result.setSuccess(true);
            result.setDeletedCount((int) count);
            result.addDeletedEntityType("Teachers", (int) count);
            
            log.info("✅ Deleted {} teachers", count);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete teachers: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional
    public CleanupResult deleteAllCourses() {
        log.warn("⚠️ DELETING ALL COURSES");
        
        CleanupResult result = new CleanupResult("Delete All Courses");
        
        try {
            long count = courseRepository.count();
            courseRepository.deleteAll();
            
            result.setSuccess(true);
            result.setDeletedCount((int) count);
            result.addDeletedEntityType("Courses", (int) count);
            
            log.info("✅ Deleted {} courses", count);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete courses: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional
    public CleanupResult deleteAllRooms() {
        log.warn("⚠️ DELETING ALL ROOMS");
        
        CleanupResult result = new CleanupResult("Delete All Rooms");
        
        try {
            long count = roomRepository.count();
            roomRepository.deleteAll();
            
            result.setSuccess(true);
            result.setDeletedCount((int) count);
            result.addDeletedEntityType("Rooms", (int) count);
            
            log.info("✅ Deleted {} rooms", count);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete rooms: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional
    public CleanupResult deleteAllEvents() {
        log.warn("⚠️ DELETING ALL EVENTS");
        
        CleanupResult result = new CleanupResult("Delete All Events");
        
        try {
            long count = eventRepository.count();
            eventRepository.deleteAll();
            
            result.setSuccess(true);
            result.setDeletedCount((int) count);
            result.addDeletedEntityType("Events", (int) count);
            
            log.info("✅ Deleted {} events", count);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete events: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    // ========================================================================
    // FILTERED DELETE METHODS
    // ========================================================================

    @Override
    @Transactional
    public CleanupResult deleteStudentsByGradeLevel(String gradeLevel) {
        // ✅ NULL SAFE: Validate gradeLevel parameter
        if (gradeLevel == null || gradeLevel.trim().isEmpty()) {
            CleanupResult result = new CleanupResult("Delete Students - Invalid Grade Level");
            result.setSuccess(false);
            result.setErrorMessage("Grade level cannot be null or empty");
            return result;
        }

        log.warn("⚠️ DELETING STUDENTS BY GRADE LEVEL: {}", gradeLevel);

        CleanupResult result = new CleanupResult("Delete Students - Grade " + gradeLevel);

        try {
            var students = studentRepository.findByGradeLevel(gradeLevel);
            int count = students.size();
            
            studentRepository.deleteAll(students);
            
            result.setSuccess(true);
            result.setDeletedCount(count);
            result.addDeletedEntityType("Students (Grade " + gradeLevel + ")", count);
            
            log.info("✅ Deleted {} students from grade {}", count, gradeLevel);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete students: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional
    public CleanupResult deleteTeachersByDepartment(String department) {
        // ✅ NULL SAFE: Validate department parameter
        if (department == null || department.trim().isEmpty()) {
            CleanupResult result = new CleanupResult("Delete Teachers - Invalid Department");
            result.setSuccess(false);
            result.setErrorMessage("Department cannot be null or empty");
            return result;
        }

        log.warn("⚠️ DELETING TEACHERS BY DEPARTMENT: {}", department);

        CleanupResult result = new CleanupResult("Delete Teachers - " + department);

        try {
            var teachers = teacherRepository.findByDepartment(department);
            int count = teachers.size();
            
            teacherRepository.deleteAll(teachers);
            
            result.setSuccess(true);
            result.setDeletedCount(count);
            result.addDeletedEntityType("Teachers (" + department + ")", count);
            
            log.info("✅ Deleted {} teachers from {}", count, department);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete teachers: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional
    public CleanupResult deleteInactiveStudents() {
        log.warn("⚠️ DELETING INACTIVE STUDENTS");
        
        CleanupResult result = new CleanupResult("Delete Inactive Students");
        
        try {
            // Get all students, filter inactive
            var allStudents = studentRepository.findAll();
            // ✅ NULL SAFE: Filter null students before checking active status
            var inactiveStudents = allStudents.stream()
                .filter(s -> s != null && !s.isActive())
                .toList();
            
            int count = inactiveStudents.size();
            studentRepository.deleteAll(inactiveStudents);
            
            result.setSuccess(true);
            result.setDeletedCount(count);
            result.addDeletedEntityType("Inactive Students", count);
            
            log.info("✅ Deleted {} inactive students", count);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete inactive students: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional
    public CleanupResult deleteInactiveTeachers() {
        log.warn("⚠️ DELETING INACTIVE TEACHERS");
        
        CleanupResult result = new CleanupResult("Delete Inactive Teachers");
        
        try {
            // Get all teachers, filter inactive
            var allTeachers = teacherRepository.findAllActive();
            var inactiveTeachers = allTeachers.stream()
                .filter(t -> Boolean.FALSE.equals(t.getActive()))  // ✅ FIXED: Null-safe check
                .toList();
            
            int count = inactiveTeachers.size();
            teacherRepository.deleteAll(inactiveTeachers);
            
            result.setSuccess(true);
            result.setDeletedCount(count);
            result.addDeletedEntityType("Inactive Teachers", count);
            
            log.info("✅ Deleted {} inactive teachers", count);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete inactive teachers: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    @Override
    public DatabaseStats getDatabaseStats() {
        DatabaseStats stats = new DatabaseStats();
        
        stats.setStudentCount(studentRepository.count());
        stats.setTeacherCount(teacherRepository.countByActiveTrue());
        stats.setCourseCount(courseRepository.count());
        stats.setRoomCount(roomRepository.count());
        stats.setEventCount(eventRepository.count());
        stats.setScheduleCount(scheduleRepository.count());
        stats.setScheduleSlotCount(scheduleSlotRepository.count());
        stats.setLastUpdated(LocalDateTime.now());
        
        return stats;
    }
}