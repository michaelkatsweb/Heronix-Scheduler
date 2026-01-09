package com.heronix.controller;

import com.heronix.dto.TeacherSyncDTO;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Teacher Sync REST API Controller
 *
 * Provides REST endpoints for EduPro-Teacher application to sync teacher data
 * from PostgreSQL (Heronix Scheduler-Pro) to H2 (EduPro-Teacher)
 *
 * Endpoints:
 * - GET /api/teacher-sync/teachers - Get all active teachers
 * - GET /api/teacher-sync/teachers/{employeeId} - Get specific teacher
 * - GET /api/teacher-sync/health - Health check
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-29
 */
@RestController
@RequestMapping("/api/teacher-sync")
@Slf4j
@RequiredArgsConstructor
public class TeacherSyncController {

    private final TeacherRepository teacherRepository;

    /**
     * Health check endpoint
     *
     * @return Status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Teacher Sync API is running");
    }

    /**
     * Get all active teachers for synchronization
     *
     * @return List of active teachers with encrypted passwords
     */
    @GetMapping("/teachers")
    public ResponseEntity<List<TeacherSyncDTO>> getAllTeachers() {
        log.info("Received teacher sync request - fetching all active teachers");

        List<Teacher> teachers = teacherRepository.findByActiveTrue();

        List<TeacherSyncDTO> teacherDTOs = teachers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        log.info("Returning {} active teachers for sync", teacherDTOs.size());

        return ResponseEntity.ok(teacherDTOs);
    }

    /**
     * Get specific teacher by employee ID
     *
     * @param employeeId Employee ID
     * @return Teacher data or 404 if not found
     */
    @GetMapping("/teachers/{employeeId}")
    public ResponseEntity<TeacherSyncDTO> getTeacherByEmployeeId(@PathVariable String employeeId) {
        log.info("Received teacher sync request for employee ID: {}", employeeId);

        Optional<Teacher> teacherOpt = teacherRepository.findByEmployeeId(employeeId);

        if (teacherOpt.isEmpty()) {
            log.warn("Teacher not found: {}", employeeId);
            return ResponseEntity.notFound().build();
        }

        Teacher teacher = teacherOpt.get();

        if (!teacher.getActive()) {
            log.warn("Teacher account is inactive: {}", employeeId);
            return ResponseEntity.notFound().build();
        }

        TeacherSyncDTO dto = convertToDTO(teacher);

        log.info("Returning teacher data for: {}", employeeId);

        return ResponseEntity.ok(dto);
    }

    /**
     * Convert Teacher entity to TeacherSyncDTO
     */
    private TeacherSyncDTO convertToDTO(Teacher teacher) {
        return TeacherSyncDTO.builder()
                .id(teacher.getId())
                .employeeId(teacher.getEmployeeId())
                .password(teacher.getPassword())  // BCrypt encrypted password
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .name(teacher.getName())
                .email(teacher.getEmail())
                .department(teacher.getDepartment())
                .active(teacher.getActive())
                .build();
    }
}
