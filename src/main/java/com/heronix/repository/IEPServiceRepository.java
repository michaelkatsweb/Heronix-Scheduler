package com.heronix.repository;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.ServiceType;
import com.heronix.model.enums.DeliveryModel;
import com.heronix.model.enums.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * IEP Service Repository
 *
 * Provides data access methods for IEP Service entities including:
 * - Finding services by IEP, type, and provider
 * - Querying active/unscheduled services
 * - Finding services by scheduling requirements
 * - Service utilization and compliance tracking
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8A - November 21, 2025
 */
@Repository
public interface IEPServiceRepository extends JpaRepository<IEPService, Long> {

    // ========== BASIC QUERIES ==========

    /**
     * Find all services for an IEP
     */
    List<IEPService> findByIep(IEP iep);

    /**
     * Find all services for an IEP ID
     */
    List<IEPService> findByIepId(Long iepId);

    /**
     * Find services by type
     */
    List<IEPService> findByServiceType(ServiceType serviceType);

    /**
     * Find services by delivery model
     */
    List<IEPService> findByDeliveryModel(DeliveryModel deliveryModel);

    /**
     * Find services by assigned staff
     */
    List<IEPService> findByAssignedStaff(Teacher staff);

    /**
     * Find services by assigned staff ID
     */
    List<IEPService> findByAssignedStaffId(Long staffId);

    /**
     * Find services by status
     */
    List<IEPService> findByStatus(ServiceStatus status);

    // ========== ACTIVE SERVICES QUERIES ==========

    /**
     * Find services needing scheduling
     */
    @Query("SELECT s FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE i.status = 'ACTIVE' " +
           "AND s.status = 'NOT_SCHEDULED'")
    List<IEPService> findServicesNeedingScheduling();

    /**
     * Find services needing scheduling for a specific student
     */
    @Query("SELECT s FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE i.student.id = :studentId " +
           "AND i.status = 'ACTIVE' " +
           "AND s.status = 'NOT_SCHEDULED'")
    List<IEPService> findStudentServicesNeedingScheduling(@Param("studentId") Long studentId);

    /**
     * Find fully scheduled services
     */
    @Query("SELECT s FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE i.status = 'ACTIVE' " +
           "AND s.status = 'SCHEDULED'")
    List<IEPService> findScheduledServices();

    /**
     * Find services by type for active IEPs
     */
    @Query("SELECT s FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE i.status = 'ACTIVE' " +
           "AND s.serviceType = :type")
    List<IEPService> findActiveServicesByType(@Param("type") ServiceType type);

    // ========== PROVIDER QUERIES ==========

    /**
     * Find services assigned to staff that need scheduling
     */
    @Query("SELECT s FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE s.assignedStaff.id = :staffId " +
           "AND i.status = 'ACTIVE' " +
           "AND s.status = 'NOT_SCHEDULED'")
    List<IEPService> findStaffServicesNeedingScheduling(@Param("staffId") Long staffId);

    /**
     * Find services pending staff assignment
     */
    @Query("SELECT s FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE s.assignedStaff IS NULL " +
           "AND i.status = 'ACTIVE' " +
           "AND s.status = 'NOT_SCHEDULED'")
    List<IEPService> findServicesPendingStaffAssignment();

    /**
     * Calculate staff workload (total minutes per week)
     */
    @Query("SELECT s.assignedStaff.id, SUM(s.minutesPerWeek) FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE s.assignedStaff IS NOT NULL " +
           "AND i.status = 'ACTIVE' " +
           "AND s.status = 'SCHEDULED' " +
           "GROUP BY s.assignedStaff.id")
    List<Object[]> calculateStaffWorkload();

    // ========== COMPLIANCE TRACKING ==========

    /**
     * Find services not meeting minutes requirement
     */
    @Query("SELECT s FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE i.status = 'ACTIVE' " +
           "AND s.status = 'SCHEDULED' " +
           "AND s.id IN (" +
           "  SELECT ps.iepService.id FROM PullOutSchedule ps " +
           "  WHERE ps.status = 'ACTIVE' " +
           "  GROUP BY ps.iepService.id " +
           "  HAVING SUM(ps.durationMinutes) < ps.iepService.minutesPerWeek" +
           ")")
    List<IEPService> findServicesNotMeetingMinutes();

    // ========== STATISTICS QUERIES ==========

    /**
     * Count services by type for active IEPs
     */
    @Query("SELECT s.serviceType, COUNT(s) FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE i.status = 'ACTIVE' " +
           "GROUP BY s.serviceType")
    List<Object[]> countServicesByType();

    /**
     * Count services by status
     */
    @Query("SELECT s.status, COUNT(s) FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE i.status = 'ACTIVE' " +
           "GROUP BY s.status")
    List<Object[]> countByStatus();

    /**
     * Count services by delivery model
     */
    @Query("SELECT s.deliveryModel, COUNT(s) FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE i.status = 'ACTIVE' " +
           "GROUP BY s.deliveryModel")
    List<Object[]> countByDeliveryModel();

    /**
     * Calculate total service minutes per week across all active services
     */
    @Query("SELECT SUM(s.minutesPerWeek) FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE i.status = 'ACTIVE'")
    Long getTotalServiceMinutesPerWeek();

    // ========== SEARCH QUERIES ==========

    /**
     * Search services by student name
     */
    @Query("SELECT s FROM IEPService s " +
           "JOIN s.iep i " +
           "WHERE LOWER(i.student.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.student.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<IEPService> searchByStudentName(@Param("searchTerm") String searchTerm);

    /**
     * Search services by staff name
     */
    @Query("SELECT s FROM IEPService s " +
           "WHERE s.assignedStaff IS NOT NULL AND (" +
           "LOWER(s.assignedStaff.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.assignedStaff.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<IEPService> searchByStaffName(@Param("searchTerm") String searchTerm);
}
