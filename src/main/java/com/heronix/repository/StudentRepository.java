package com.heronix.repository;

import com.heronix.model.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Find student by student ID
    Optional<Student> findByStudentId(String studentId);

    // Find student by email
    Optional<Student> findByEmail(String email);

    // Find student by QR code ID (for QR attendance system)
    Optional<Student> findByQrCodeId(String qrCodeId);

    // Check if email exists (for duplicate detection)
    boolean existsByEmail(String email);

    // Check if student ID exists (for duplicate detection)
    boolean existsByStudentId(String studentId);

    // Find all active students (excluding soft-deleted)
    @Query("SELECT s FROM Student s WHERE s.active = true AND (s.deleted = false OR s.deleted IS NULL)")
    List<Student> findByActiveTrue();

    @Query("SELECT s FROM Student s WHERE s.active = :active AND (s.deleted = false OR s.deleted IS NULL)")
    List<Student> findByActive(@org.springframework.data.repository.query.Param("active") boolean active);

    // Find students by grade level
    List<Student> findByGradeLevel(String gradeLevel);

    // Search students by name
    @Query("SELECT s FROM Student s WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Student> searchByName(String name);

    // Find students in a specific schedule slot
    @Query("SELECT s FROM Student s JOIN s.scheduleSlots slot WHERE slot.id = :slotId")
    List<Student> findByScheduleSlotId(Long slotId);
    // Find active students by grade level
    List<Student> findByGradeLevelAndActiveTrue(String gradeLevel);

    // Count active students by grade level
    int countByGradeLevelAndActiveTrue(String gradeLevel);

    // Find graduated students
    List<Student> findByGraduatedTrue();

    // Find students by graduation year
    List<Student> findByGraduationYear(Integer graduationYear);

    // Find students enrolled in a specific course (with JOIN FETCH to avoid N+1)
    @Query("SELECT DISTINCT s FROM Student s " +
           "LEFT JOIN FETCH s.enrolledCourses ec " +
           "WHERE :course MEMBER OF s.enrolledCourses")
    List<Student> findStudentsEnrolledInCourse(@org.springframework.data.repository.query.Param("course") com.heronix.model.domain.Course course);

    // Find all students with enrolled courses loaded (prevents LazyInitializationException)
    @Query("SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.enrolledCourses")
    List<Student> findAllWithEnrolledCourses();

    // Find student by ID with enrolled courses eagerly loaded
    @Query("SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.enrolledCourses WHERE s.id = :id")
    Optional<Student> findByIdWithEnrolledCourses(@org.springframework.data.repository.query.Param("id") Long id);

    // ========================================================================
    // SOFT DELETE SUPPORT
    // ========================================================================

    /**
     * Find all non-deleted students (active and inactive)
     * Use this instead of findAll() to exclude soft-deleted records
     */
    @Query("SELECT s FROM Student s WHERE s.deleted = false OR s.deleted IS NULL")
    List<Student> findAllNonDeleted();

    /**
     * Find all active students excluding soft-deleted
     */
    @Query("SELECT s FROM Student s WHERE s.active = true AND (s.deleted = false OR s.deleted IS NULL)")
    List<Student> findAllActive();

    /**
     * Find soft-deleted students (for audit/recovery purposes)
     */
    @Query("SELECT s FROM Student s WHERE s.deleted = true")
    List<Student> findDeleted();

    /**
     * Find students deleted by a specific user
     */
    @Query("SELECT s FROM Student s WHERE s.deleted = true AND s.deletedBy = :username")
    List<Student> findDeletedByUser(@org.springframework.data.repository.query.Param("username") String username);
}