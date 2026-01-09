package com.heronix.service;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.Plan504;
import com.heronix.model.domain.Student;

import java.util.List;
import java.util.Map;

/**
 * SPED Compliance Service Interface
 *
 * Provides business logic for tracking special education compliance.
 * Monitors IEP/504 expiration, service minutes, and review requirements.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8B - November 21, 2025
 */
public interface SPEDComplianceService {

    // ========== IEP COMPLIANCE ==========

    /**
     * Get IEPs requiring immediate attention (expiring/expired)
     *
     * @return List of IEPs needing action
     */
    List<IEP> getIEPsRequiringAction();

    /**
     * Get IEPs expiring within a threshold
     *
     * @param daysThreshold Number of days
     * @return List of expiring IEPs
     */
    List<IEP> getExpiringIEPs(int daysThreshold);

    /**
     * Get IEPs with reviews due
     *
     * @return List of IEPs needing review
     */
    List<IEP> getIEPsNeedingReview();

    /**
     * Check if an IEP is compliant (active, not expired, services scheduled)
     *
     * @param iepId IEP ID
     * @return true if compliant
     */
    boolean isIEPCompliant(Long iepId);

    // ========== 504 PLAN COMPLIANCE ==========

    /**
     * Get 504 plans requiring immediate attention
     *
     * @return List of plans needing action
     */
    List<Plan504> get504PlansRequiringAction();

    /**
     * Get 504 plans expiring within a threshold
     *
     * @param daysThreshold Number of days
     * @return List of expiring plans
     */
    List<Plan504> getExpiring504Plans(int daysThreshold);

    /**
     * Get 504 plans with reviews overdue
     *
     * @return List of plans with overdue reviews
     */
    List<Plan504> get504PlansWithOverdueReview();

    // ========== SERVICE MINUTES COMPLIANCE ==========

    /**
     * Get services not meeting minutes requirements
     *
     * @return List of services below required minutes
     */
    List<IEPService> getServicesNotMeetingMinutes();

    /**
     * Get services needing scheduling
     *
     * @return List of unscheduled services
     */
    List<IEPService> getServicesNeedingScheduling();

    /**
     * Calculate compliance percentage for a service
     *
     * @param serviceId Service ID
     * @return Compliance percentage (0-100)
     */
    double getServiceCompliancePercentage(Long serviceId);

    /**
     * Check if a service meets IEP minutes requirement
     *
     * @param serviceId Service ID
     * @return true if meets requirements
     */
    boolean meetsMinutesRequirement(Long serviceId);

    // ========== STUDENT COMPLIANCE ==========

    /**
     * Get compliance summary for a student
     *
     * @param studentId Student ID
     * @return Compliance summary
     */
    ComplianceSummary getStudentComplianceSummary(Long studentId);

    /**
     * Check if student is SPED compliant (all services scheduled and meeting minutes)
     *
     * @param studentId Student ID
     * @return true if compliant
     */
    boolean isStudentCompliant(Long studentId);

    /**
     * Get all compliance issues for a student
     *
     * @param studentId Student ID
     * @return List of compliance issues
     */
    List<String> getStudentComplianceIssues(Long studentId);

    // ========== DASHBOARD METRICS ==========

    /**
     * Get overall SPED compliance metrics
     *
     * @return Dashboard metrics
     */
    DashboardMetrics getDashboardMetrics();

    /**
     * Get compliance rate for active IEPs (percentage fully compliant)
     *
     * @return Compliance rate (0-100)
     */
    double getIEPComplianceRate();

    /**
     * Get count of services by status
     *
     * @return Map of status to count
     */
    Map<String, Long> getServiceStatusCounts();

    /**
     * Get count of IEPs by eligibility category
     *
     * @return Map of category to count
     */
    Map<String, Long> getIEPCountByCategory();

    // ========== REPORTS ==========

    /**
     * Generate compliance report for all active IEPs
     *
     * @return List of compliance summaries
     */
    List<ComplianceSummary> generateComplianceReport();

    /**
     * Generate expiration report (IEPs/504s expiring soon)
     *
     * @param daysThreshold Days threshold
     * @return Expiration report
     */
    ExpirationReport generateExpirationReport(int daysThreshold);

    /**
     * Generate minutes compliance report (services not meeting requirements)
     *
     * @return Minutes compliance report
     */
    MinutesComplianceReport generateMinutesReport();

    // ========== HELPER CLASSES ==========

    /**
     * Compliance summary for a student
     */
    class ComplianceSummary {
        public Long studentId;
        public String studentName;
        public boolean hasActiveIEP;
        public boolean hasActive504;
        public int totalServices;
        public int scheduledServices;
        public int servicesNotMeetingMinutes;
        public double overallComplianceRate;
        public List<String> issues;

        public ComplianceSummary() {
            this.issues = new java.util.ArrayList<>();
        }

        public boolean isCompliant() {
            return issues.isEmpty() && overallComplianceRate >= 100.0;
        }
    }

    /**
     * Dashboard metrics
     */
    class DashboardMetrics {
        public long totalActiveIEPs;
        public long totalActive504Plans;
        public long totalServices;
        public long servicesNeedingScheduling;
        public long servicesNotMeetingMinutes;
        public long iepsExpiringWithin30Days;
        public long iepsNeedingReview;
        public double overallComplianceRate;

        public DashboardMetrics() {}
    }

    /**
     * Expiration report
     */
    class ExpirationReport {
        public List<IEP> expiringIEPs;
        public List<Plan504> expiring504Plans;
        public int totalExpiring;

        public ExpirationReport() {
            this.expiringIEPs = new java.util.ArrayList<>();
            this.expiring504Plans = new java.util.ArrayList<>();
        }
    }

    /**
     * Minutes compliance report
     */
    class MinutesComplianceReport {
        public List<ServiceMinutesInfo> servicesInfo;
        public int totalServices;
        public int compliantServices;
        public int nonCompliantServices;
        public double complianceRate;

        public MinutesComplianceReport() {
            this.servicesInfo = new java.util.ArrayList<>();
        }
    }

    /**
     * Service minutes information
     */
    class ServiceMinutesInfo {
        public Long serviceId;
        public Long studentId;
        public String studentName;
        public String serviceType;
        public int requiredMinutes;
        public int scheduledMinutes;
        public double compliancePercentage;
        public boolean isCompliant;

        public ServiceMinutesInfo() {}
    }
}
