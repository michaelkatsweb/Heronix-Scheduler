package com.heronix.repository;

import com.heronix.model.domain.EmergencyContact;
import com.heronix.model.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * EmergencyContact Repository - K-12 Enrollment Enhancement
 * Location: src/main/java/com/eduscheduler/repository/EmergencyContactRepository.java
 *
 * Data access layer for emergency contact operations.
 * Provides CRUD operations and custom queries for emergency contact management.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 14, 2025
 */
@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    /**
     * Find all emergency contacts for a specific student
     * Ordered by priority (1 = first to call, 2 = second, etc.)
     *
     * @param student The student entity
     * @return List of emergency contacts ordered by priority
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student = :student " +
           "AND ec.isActive = true " +
           "ORDER BY ec.priorityOrder ASC")
    List<EmergencyContact> findByStudentOrderByPriority(@Param("student") Student student);

    /**
     * Find all emergency contacts for a specific student ID
     *
     * @param studentId The student ID
     * @return List of emergency contacts ordered by priority
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND ec.isActive = true " +
           "ORDER BY ec.priorityOrder ASC")
    List<EmergencyContact> findByStudentId(@Param("studentId") Long studentId);

    /**
     * Find all active emergency contacts for a student
     *
     * @param studentId The student ID
     * @return List of active emergency contacts
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND ec.isActive = true " +
           "ORDER BY ec.priorityOrder ASC")
    List<EmergencyContact> findActiveByStudentId(@Param("studentId") Long studentId);

    /**
     * Find primary emergency contact (priority 1) for a student
     *
     * @param studentId The student ID
     * @return Primary emergency contact (if exists)
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND ec.priorityOrder = 1 " +
           "AND ec.isActive = true")
    Optional<EmergencyContact> findPrimaryContact(@Param("studentId") Long studentId);

    /**
     * Find high priority contacts (priority 1 or 2) for a student
     *
     * @param studentId The student ID
     * @return List of high priority contacts
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND ec.priorityOrder <= 2 " +
           "AND ec.isActive = true " +
           "ORDER BY ec.priorityOrder ASC")
    List<EmergencyContact> findHighPriorityContacts(@Param("studentId") Long studentId);

    /**
     * Find emergency contacts authorized to pick up student
     *
     * @param studentId The student ID
     * @return List of contacts authorized for pickup
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND ec.authorizedToPickUp = true " +
           "AND ec.isActive = true " +
           "ORDER BY ec.priorityOrder ASC")
    List<EmergencyContact> findAuthorizedForPickup(@Param("studentId") Long studentId);

    /**
     * Find emergency contacts living with student
     *
     * @param studentId The student ID
     * @return List of contacts living with student
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND ec.livesWithStudent = true " +
           "AND ec.isActive = true")
    List<EmergencyContact> findLivingWithStudent(@Param("studentId") Long studentId);

    /**
     * Find emergency contacts by phone number
     *
     * @param phoneNumber Phone number (primary or secondary)
     * @return List of matching contacts
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE " +
           "ec.primaryPhone = :phoneNumber OR ec.secondaryPhone = :phoneNumber")
    List<EmergencyContact> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * Find emergency contacts by email
     *
     * @param email Email address
     * @return List of matching contacts
     */
    List<EmergencyContact> findByEmail(String email);

    /**
     * Search emergency contacts by name (first or last)
     *
     * @param searchTerm Search term
     * @return List of matching contacts
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE " +
           "(LOWER(ec.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ec.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND ec.isActive = true")
    List<EmergencyContact> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Find emergency contacts by relationship type
     *
     * @param studentId The student ID
     * @param relationship Relationship type (e.g., "Grandmother", "Aunt")
     * @return List of contacts with this relationship
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND LOWER(ec.relationship) = LOWER(:relationship) " +
           "AND ec.isActive = true " +
           "ORDER BY ec.priorityOrder ASC")
    List<EmergencyContact> findByRelationship(@Param("studentId") Long studentId,
                                               @Param("relationship") String relationship);

    /**
     * Count active emergency contacts for a student
     *
     * @param studentId The student ID
     * @return Number of active emergency contacts
     */
    @Query("SELECT COUNT(ec) FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND ec.isActive = true")
    Long countActiveByStudentId(@Param("studentId") Long studentId);

    /**
     * Check if student has minimum required emergency contacts (at least 2)
     *
     * @param studentId The student ID
     * @return true if student has at least 2 active emergency contacts
     */
    @Query("SELECT CASE WHEN COUNT(ec) >= 2 THEN true ELSE false END " +
           "FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND ec.isActive = true")
    boolean hasMinimumContacts(@Param("studentId") Long studentId);

    /**
     * Find next available priority order for a student
     * (Used when adding a new contact)
     *
     * @param studentId The student ID
     * @return Next priority order number
     */
    @Query("SELECT COALESCE(MAX(ec.priorityOrder), 0) + 1 " +
           "FROM EmergencyContact ec WHERE ec.student.id = :studentId")
    Integer getNextPriorityOrder(@Param("studentId") Long studentId);

    /**
     * Find emergency contacts with specific priority order
     *
     * @param studentId The student ID
     * @param priorityOrder Priority order
     * @return List of contacts with this priority
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND ec.priorityOrder = :priorityOrder " +
           "AND ec.isActive = true")
    List<EmergencyContact> findByPriorityOrder(@Param("studentId") Long studentId,
                                                @Param("priorityOrder") Integer priorityOrder);

    /**
     * Deactivate (soft delete) an emergency contact
     *
     * @param contactId Emergency contact ID
     */
    @Query("UPDATE EmergencyContact ec SET ec.isActive = false " +
           "WHERE ec.id = :contactId")
    void deactivateContact(@Param("contactId") Long contactId);

    /**
     * Find all emergency contacts with valid phone numbers
     *
     * @param studentId The student ID
     * @return List of contacts with at least one valid phone
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND (ec.primaryPhone IS NOT NULL OR ec.secondaryPhone IS NOT NULL OR ec.workPhone IS NOT NULL) " +
           "AND ec.isActive = true " +
           "ORDER BY ec.priorityOrder ASC")
    List<EmergencyContact> findWithValidPhone(@Param("studentId") Long studentId);

    /**
     * Find emergency contacts with email addresses
     *
     * @param studentId The student ID
     * @return List of contacts with email
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND ec.email IS NOT NULL " +
           "AND ec.isActive = true " +
           "ORDER BY ec.priorityOrder ASC")
    List<EmergencyContact> findWithEmail(@Param("studentId") Long studentId);

    /**
     * Delete all emergency contacts for a student
     * (Used when student is deleted - cascade should handle this automatically)
     *
     * @param studentId The student ID
     */
    @Query("DELETE FROM EmergencyContact ec WHERE ec.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    /**
     * Reorder emergency contacts after deletion
     * (Update priority orders to fill gaps: 1, 2, 3, 4, 5...)
     *
     * @param studentId The student ID
     */
    @Query("SELECT ec FROM EmergencyContact ec WHERE ec.student.id = :studentId " +
           "AND ec.isActive = true " +
           "ORDER BY ec.priorityOrder ASC")
    List<EmergencyContact> findAllForReordering(@Param("studentId") Long studentId);
}
