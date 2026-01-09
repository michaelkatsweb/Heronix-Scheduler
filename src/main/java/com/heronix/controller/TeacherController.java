// Location: src/main/java/com/eduscheduler/controller/TeacherController.java
package com.heronix.controller;

import com.heronix.model.domain.Teacher;
import com.heronix.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Teacher REST API Controller
 * Location: src/main/java/com/eduscheduler/controller/TeacherController.java
 * 
 * Endpoints:
 * - GET /api/teachers - Get all teachers
 * - GET /api/teachers/{id} - Get teacher by ID
 * - POST /api/teachers - Create new teacher
 * - PUT /api/teachers/{id} - Update teacher
 * - DELETE /api/teachers/{id} - Soft delete teacher
 * - GET /api/teachers/active - Get active teachers
 * - GET /api/teachers/department/{dept} - Get by department
 * - GET /api/teachers/employee/{empId} - Get by employee ID
 */
@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = "*")
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepository;

    /**
     * GET /api/teachers
     * Get all teachers
     */
    @GetMapping
    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAllActive();
    }

    /**
     * GET /api/teachers/{id}
     * Get teacher by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Teacher> getTeacherById(@PathVariable Long id) {
        return teacherRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/teachers
     * Create new teacher
     */
    @PostMapping
    public ResponseEntity<Teacher> createTeacher(@RequestBody Teacher teacher) {
        // Ensure new teacher is active
        teacher.setActive(true);
        
        Teacher saved = teacherRepository.save(teacher);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * PUT /api/teachers/{id}
     * Update existing teacher
     */
    @PutMapping("/{id}")
    public ResponseEntity<Teacher> updateTeacher(
            @PathVariable Long id,
            @RequestBody Teacher teacherDetails) {

        return teacherRepository.findById(id)
                .map(teacher -> {
                    teacher.setName(teacherDetails.getName());
                    teacher.setEmployeeId(teacherDetails.getEmployeeId());
                    teacher.setDepartment(teacherDetails.getDepartment());
                    teacher.setEmail(teacherDetails.getEmail());
                    teacher.setPhoneNumber(teacherDetails.getPhoneNumber());
                    teacher.setCertifications(teacherDetails.getCertifications());
                    teacher.setMaxHoursPerWeek(teacherDetails.getMaxHoursPerWeek());
                    teacher.setMaxConsecutiveHours(teacherDetails.getMaxConsecutiveHours());
                    teacher.setPreferredBreakMinutes(teacherDetails.getPreferredBreakMinutes());
                    teacher.setPriorityLevel(teacherDetails.getPriorityLevel());
                    teacher.setNotes(teacherDetails.getNotes());
                    teacher.setActive(teacherDetails.getActive());

                    Teacher updated = teacherRepository.save(teacher);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/teachers/{id}
     * Soft delete teacher (sets active = false)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTeacher(@PathVariable Long id) {
        return teacherRepository.findById(id)
                .map(teacher -> {
                    teacher.setActive(false);
                    teacherRepository.save(teacher);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/teachers/active
     * Get all active teachers
     */
    @GetMapping("/active")
    public List<Teacher> getActiveTeachers() {
        return teacherRepository.findByActiveTrue();
    }

    /**
     * GET /api/teachers/department/{department}
     * Get teachers by department
     */
    @GetMapping("/department/{department}")
    public List<Teacher> getTeachersByDepartment(@PathVariable String department) {
        return teacherRepository.findByDepartment(department);
    }

    /**
     * GET /api/teachers/employee/{employeeId}
     * Get teacher by employee ID
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Teacher> getTeacherByEmployeeId(@PathVariable String employeeId) {
        return teacherRepository.findByEmployeeId(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/teachers/{id}/utilization
     * Get teacher's utilization rate
     */
    @GetMapping("/{id}/utilization")
    public ResponseEntity<Map<String, Object>> getTeacherUtilization(@PathVariable Long id) {
        return teacherRepository.findById(id)
                .map(teacher -> {
                    Map<String, Object> response = Map.of(
                        "teacherId", teacher.getId(),
                        "teacherName", teacher.getName(),
                        "utilizationRate", teacher.getUtilizationRate(),
                        "currentWeekHours", teacher.getCurrentWeekHours(),
                        "maxWeekHours", teacher.getMaxHoursPerWeek()
                    );
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PATCH /api/teachers/{id}/activate
     * Activate teacher
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Teacher> activateTeacher(@PathVariable Long id) {
        return teacherRepository.findById(id)
                .map(teacher -> {
                    teacher.setActive(true);
                    return ResponseEntity.ok(teacherRepository.save(teacher));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PATCH /api/teachers/{id}/deactivate
     * Deactivate teacher
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Teacher> deactivateTeacher(@PathVariable Long id) {
        return teacherRepository.findById(id)
                .map(teacher -> {
                    teacher.setActive(false);
                    return ResponseEntity.ok(teacherRepository.save(teacher));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}