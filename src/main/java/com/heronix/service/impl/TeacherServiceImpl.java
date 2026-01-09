// Location: src/main/java/com/eduscheduler/service/impl/TeacherServiceImpl.java
package com.heronix.service.impl;

import com.heronix.model.domain.Teacher;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Teacher Service Implementation
 * Location: src/main/java/com/eduscheduler/service/impl/TeacherServiceImpl.java
 * 
 * @author Heronix Scheduling System Team
 * @version 1.1.0
 * @since 2025-10-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;

    /**
     * Get all active teachers
     * 
     * @return List of active teachers
     */
    @Override
    @Transactional(readOnly = true)
    public List<Teacher> getAllActiveTeachers() {
        log.debug("📚 Fetching all active teachers");
        List<Teacher> teachers = teacherRepository.findByActiveTrue();
        log.info("✅ Found {} active teachers", teachers.size());
        return teachers;
    }

    /**
     * Get teacher by ID
     * 
     * @param id Teacher ID
     * @return Teacher object or null if not found
     */
    @Override
    @Transactional(readOnly = true)
    public Teacher getTeacherById(Long id) {
        log.debug("🔍 Fetching teacher with ID: {}", id);
        Teacher teacher = teacherRepository.findById(id).orElse(null);
        if (teacher != null) {
            // ✅ NULL SAFE: Safe extraction of teacher name
            String teacherName = teacher.getName() != null ? teacher.getName() : "Unknown";
            log.debug("✅ Found teacher: {}", teacherName);
        } else {
            log.warn("⚠️ Teacher not found with ID: {}", id);
        }
        return teacher;
    }

    /**
     * Get all teachers (including inactive)
     *
     * @return List of all teachers
     */
    @Override
    @Transactional(readOnly = true)
    public List<Teacher> findAll() {
        log.debug("Fetching all teachers (including inactive)");
        List<Teacher> teachers = teacherRepository.findAllActive();
        log.info("Found {} total teachers", teachers.size());
        return teachers;
    }

    /**
     * Create a new teacher
     *
     * @param teacher Teacher to create
     * @return Created teacher with ID
     */
    @Override
    @Transactional
    public Teacher createTeacher(Teacher teacher) {
        // ✅ NULL SAFE: Validate teacher object first
        if (teacher == null) {
            throw new IllegalArgumentException("Teacher cannot be null");
        }

        log.info("Creating new teacher: {}", teacher.getName() != null ? teacher.getName() : "Unknown");

        // Validation
        if (teacher.getName() == null || teacher.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Teacher name cannot be empty");
        }

        // Set defaults
        if (teacher.getMaxHoursPerWeek() == null) {
            teacher.setMaxHoursPerWeek(40);
        }
        if (teacher.getMaxConsecutiveHours() == null) {
            teacher.setMaxConsecutiveHours(4);
        }
        if (teacher.getPreferredBreakMinutes() == null) {
            teacher.setPreferredBreakMinutes(30);
        }
        if (teacher.getCurrentWeekHours() == null) {
            teacher.setCurrentWeekHours(0);
        }

        Teacher savedTeacher = teacherRepository.save(teacher);
        log.info("Teacher created successfully with ID: {}", savedTeacher.getId());
        return savedTeacher;
    }

    /**
     * Update an existing teacher
     *
     * @param teacher Teacher with updated information
     * @return Updated teacher
     */
    @Override
    @Transactional
    public Teacher updateTeacher(Teacher teacher) {
        // ✅ NULL SAFE: Validate teacher object first
        if (teacher == null) {
            throw new IllegalArgumentException("Teacher cannot be null");
        }

        log.info("Updating teacher ID: {}", teacher.getId());

        // Check if teacher exists
        if (teacher.getId() == null || !teacherRepository.existsById(teacher.getId())) {
            throw new IllegalArgumentException("Teacher not found with ID: " + teacher.getId());
        }

        // Validation
        if (teacher.getName() == null || teacher.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Teacher name cannot be empty");
        }

        Teacher updatedTeacher = teacherRepository.save(teacher);
        // ✅ NULL SAFE: Safe extraction of updated teacher name
        String teacherName = updatedTeacher.getName() != null ? updatedTeacher.getName() : "Unknown";
        log.info("Teacher updated successfully: {}", teacherName);
        return updatedTeacher;
    }

    /**
     * Delete a teacher by ID (hard delete)
     *
     * @param id Teacher ID to delete
     */
    @Override
    @Transactional
    public void deleteTeacher(Long id) {
        log.info("Deleting teacher ID: {}", id);

        if (!teacherRepository.existsById(id)) {
            throw new IllegalArgumentException("Teacher not found with ID: " + id);
        }

        teacherRepository.deleteById(id);
        log.info("Teacher deleted successfully");
    }

    /**
     * Soft delete - deactivate a teacher
     *
     * @param id Teacher ID to deactivate
     */
    @Override
    @Transactional
    public void deactivateTeacher(Long id) {
        log.info("Deactivating teacher ID: {}", id);

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with ID: " + id));

        teacher.setActive(false);
        teacherRepository.save(teacher);
        // ✅ NULL SAFE: Safe extraction of teacher name
        String teacherName = teacher.getName() != null ? teacher.getName() : "Unknown";
        log.info("Teacher deactivated: {}", teacherName);
    }

    /**
     * Activate a teacher
     *
     * @param id Teacher ID to activate
     */
    @Override
    @Transactional
    public void activateTeacher(Long id) {
        log.info("Activating teacher ID: {}", id);

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with ID: " + id));

        teacher.setActive(true);
        teacherRepository.save(teacher);
        // ✅ NULL SAFE: Safe extraction of teacher name
        String teacherName = teacher.getName() != null ? teacher.getName() : "Unknown";
        log.info("Teacher activated: {}", teacherName);
    }

    /**
     * Search teachers by name
     *
     * @param searchTerm Search term to match against teacher name
     * @return List of matching teachers
     */
    @Override
    @Transactional(readOnly = true)
    public List<Teacher> searchByName(String searchTerm) {
        log.debug("Searching teachers by name: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAll();
        }

        List<Teacher> teachers = teacherRepository.findByNameContainingIgnoreCase(searchTerm);
        log.info("Found {} teachers matching '{}'", teachers.size(), searchTerm);
        return teachers;
    }

    /**
     * Get teachers by department
     *
     * @param department Department name
     * @return List of teachers in the department
     */
    @Override
    @Transactional(readOnly = true)
    public List<Teacher> findByDepartment(String department) {
        log.debug("Finding teachers in department: {}", department);

        if (department == null || department.trim().isEmpty()) {
            return findAll();
        }

        List<Teacher> teachers = teacherRepository.findByDepartment(department);
        log.info("Found {} teachers in department '{}'", teachers.size(), department);
        return teachers;
    }

    /**
     * Load teacher with all collections eagerly fetched for UI display
     * Uses four-step fetch to avoid Hibernate's MultipleBagFetchException
     *
     * This method is transactional to ensure all four queries run in the same
     * Hibernate session, allowing collections to attach to the same Teacher entity.
     *
     * @param teacherId Teacher ID
     * @return Teacher with all collections loaded
     */
    @Override
    @Transactional(readOnly = true)
    public Teacher loadTeacherWithCollections(Long teacherId) {
        log.debug("Loading teacher {} with all collections", teacherId);

        // Four-step fetch within single transaction to avoid MultipleBagFetchException
        // All queries run in same Hibernate session, so collections attach to same entity

        // Step 1: Load teacher with certifications (@ElementCollection)
        Teacher teacher = teacherRepository.findByIdWithCollections(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with ID: " + teacherId));

        // Step 2: Load subject certifications (attaches to same entity in session cache)
        teacherRepository.findByIdWithSubjectCertifications(teacherId);

        // Step 3: Load special assignments (attaches to same entity in session cache)
        teacherRepository.findByIdWithSpecialAssignments(teacherId);

        // Step 4: Load assigned courses (attaches to same entity in session cache)
        teacherRepository.findByIdWithCourses(teacherId);

        log.debug("Teacher {} loaded with all collections", teacherId);
        return teacher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Teacher> findAllWithCollectionsForUI() {
        log.debug("Loading all teachers with collections for UI");

        // Three-step fetch within single transaction to avoid MultipleBagFetchException
        // Step 1: Load all teachers (IDs only, no collections)
        List<Teacher> teachers = teacherRepository.findAllForUI();

        log.debug("Found {} teachers, now loading collections...", teachers.size());

        // Step 2: Load certifications for all teachers (in batches to avoid N+1)
        // Hibernate will batch these queries
        teachers.forEach(teacher -> {
            try {
                if (teacher.getSubjectCertifications() != null) {
                    int certCount = teacher.getSubjectCertifications().size();
                    log.trace("Teacher {}: {} certifications", teacher.getName(), certCount);
                }
            } catch (Exception e) {
                log.warn("Could not load certifications for teacher {}: {}",
                        teacher.getId(), e.getMessage());
            }
        });

        // Step 3: Load courses for all teachers (in batches)
        teachers.forEach(teacher -> {
            try {
                if (teacher.getCourses() != null) {
                    int courseCount = teacher.getCourses().size();
                    log.trace("Teacher {}: {} courses", teacher.getName(), courseCount);
                }
            } catch (Exception e) {
                log.warn("Could not load courses for teacher {}: {}",
                        teacher.getId(), e.getMessage());
            }
        });

        log.debug("Loaded {} teachers with all collections", teachers.size());
        return teachers;
    }

    // ========================================================================
    // SOFT DELETE OPERATIONS
    // ========================================================================

    /**
     * Soft delete a teacher (mark as deleted without removing from database)
     * Preserves historical data for schedules, grades, attendance records
     * Prevents foreign key constraint violations
     *
     * @param teacherId Teacher ID to soft delete
     * @param deletedBy Username of administrator performing the deletion
     */
    @Override
    @Transactional
    public void softDelete(Long teacherId, String deletedBy) {
        log.info("🗑️ Soft deleting teacher ID: {} by: {}", teacherId, deletedBy);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with ID: " + teacherId));

        // Mark as deleted
        teacher.setDeleted(true);
        teacher.setDeletedAt(java.time.LocalDateTime.now());
        teacher.setDeletedBy(deletedBy);
        teacher.setActive(false); // Also deactivate

        teacherRepository.save(teacher);
        // ✅ NULL SAFE: Safe extraction of teacher name
        String teacherName = teacher.getName() != null ? teacher.getName() : "Unknown";
        log.info("✅ Teacher soft deleted: {} by {}", teacherName, deletedBy);
    }

    /**
     * Restore a soft-deleted teacher
     *
     * @param teacherId Teacher ID to restore
     */
    @Override
    @Transactional
    public void restoreDeleted(Long teacherId) {
        log.info("♻️ Restoring soft-deleted teacher ID: {}", teacherId);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with ID: " + teacherId));

        // ✅ NULL SAFE: Safe Boolean wrapper check
        if (teacher.getDeleted() == null || !teacher.getDeleted()) {
            log.warn("⚠️ Teacher {} is not deleted, nothing to restore", teacherId);
            return;
        }

        // Restore teacher
        teacher.setDeleted(false);
        teacher.setDeletedAt(null);
        teacher.setDeletedBy(null);
        teacher.setActive(true); // Reactivate

        teacherRepository.save(teacher);
        // ✅ NULL SAFE: Safe extraction of teacher name
        String teacherName = teacher.getName() != null ? teacher.getName() : "Unknown";
        log.info("✅ Teacher restored: {}", teacherName);
    }

    /**
     * Get all soft-deleted teachers (for audit/recovery)
     *
     * @return List of deleted teachers
     */
    @Override
    @Transactional(readOnly = true)
    public List<Teacher> getDeleted() {
        log.debug("📋 Fetching soft-deleted teachers");
        List<Teacher> deletedTeachers = teacherRepository.findDeleted();
        log.info("✅ Found {} soft-deleted teachers", deletedTeachers.size());
        return deletedTeachers;
    }
}