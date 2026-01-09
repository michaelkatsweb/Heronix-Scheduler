package com.heronix.repository;

import com.heronix.model.domain.BehaviorIncident;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.BehaviorIncident.BehaviorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for BehaviorIncident entities
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - Multi-Level Monitoring and Reporting
 */
@Repository
public interface BehaviorIncidentRepository extends JpaRepository<BehaviorIncident, Long> {

    // Find all behavior incidents for a student
    List<BehaviorIncident> findByStudent(Student student);

    // Find behavior incidents within date range
    List<BehaviorIncident> findByStudentAndIncidentDateBetween(
            Student student,
            LocalDate startDate,
            LocalDate endDate
    );

    // Find behavior incidents by type (positive/negative)
    List<BehaviorIncident> findByStudentAndBehaviorType(Student student, BehaviorType behaviorType);

    // Find behavior incidents by type within date range
    List<BehaviorIncident> findByStudentAndBehaviorTypeAndIncidentDateBetween(
            Student student,
            BehaviorType behaviorType,
            LocalDate startDate,
            LocalDate endDate
    );

    // Count behavior incidents by type within date range
    @Query("SELECT COUNT(b) FROM BehaviorIncident b WHERE b.student = :student " +
           "AND b.behaviorType = :behaviorType AND b.incidentDate BETWEEN :startDate AND :endDate")
    Long countByStudentAndBehaviorTypeAndDateBetween(
            @Param("student") Student student,
            @Param("behaviorType") BehaviorType behaviorType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find incidents requiring admin referral
    List<BehaviorIncident> findByStudentAndAdminReferralRequired(Student student, Boolean required);

    // Find recent negative incidents requiring immediate attention
    @Query("SELECT b FROM BehaviorIncident b WHERE b.student = :student " +
           "AND b.behaviorType = 'NEGATIVE' AND b.incidentDate >= :sinceDate " +
           "AND (b.severityLevel = 'MAJOR' OR b.adminReferralRequired = true) " +
           "ORDER BY b.incidentDate DESC, b.incidentTime DESC")
    List<BehaviorIncident> findCriticalIncidentsSince(
            @Param("student") Student student,
            @Param("sinceDate") LocalDate sinceDate
    );

    // Find all incidents within date range (for school/district reporting)
    List<BehaviorIncident> findByIncidentDateBetween(LocalDate startDate, LocalDate endDate);

    // Find incidents by behavior category
    @Query("SELECT b FROM BehaviorIncident b WHERE b.student = :student " +
           "AND b.behaviorCategory = :category")
    List<BehaviorIncident> findByStudentAndCategory(
            @Param("student") Student student,
            @Param("category") BehaviorIncident.BehaviorCategory category
    );

    // Find incidents requiring parent contact that haven't been contacted
    @Query("SELECT b FROM BehaviorIncident b WHERE b.student = :student " +
           "AND b.behaviorType = 'NEGATIVE' AND b.parentContacted = false " +
           "ORDER BY b.incidentDate DESC")
    List<BehaviorIncident> findUncontactedParentIncidents(@Param("student") Student student);
}
