package com.heronix.service.impl;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.Plan504;
import com.heronix.model.domain.Student;
import com.heronix.repository.*;
import com.heronix.service.SPEDComplianceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SPED Compliance Service Implementation
 *
 * Implements business logic for tracking special education compliance.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8B - November 21, 2025
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class SPEDComplianceServiceImpl implements SPEDComplianceService {

    @Autowired
    private IEPRepository iepRepository;

    @Autowired
    private Plan504Repository plan504Repository;

    @Autowired
    private IEPServiceRepository iepServiceRepository;

    @Autowired
    private StudentRepository studentRepository;

    // ========== IEP COMPLIANCE ==========

    @Override
    public List<IEP> getIEPsRequiringAction() {
        List<IEP> requiringAction = new ArrayList<>();

        // Add expired IEPs
        // ✅ NULL SAFE: Validate repository result
        List<IEP> expiredIEPs = iepRepository.findExpiredIEPs(LocalDate.now());
        if (expiredIEPs != null) {
            requiringAction.addAll(expiredIEPs);
        }

        // Add IEPs expiring within 30 days
        List<IEP> expiringIEPs = getExpiringIEPs(30);
        if (expiringIEPs != null) {
            requiringAction.addAll(expiringIEPs);
        }

        // Add IEPs needing review
        List<IEP> needingReview = getIEPsNeedingReview();
        if (needingReview != null) {
            requiringAction.addAll(needingReview);
        }

        // Remove duplicates
        // ✅ NULL SAFE: Filter null IEPs before distinct
        return requiringAction.stream()
            .filter(iep -> iep != null)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<IEP> getExpiringIEPs(int daysThreshold) {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(daysThreshold);
        return iepRepository.findIEPsNeedingRenewal(today, thresholdDate);
    }

    @Override
    public List<IEP> getIEPsNeedingReview() {
        return iepRepository.findIEPsWithReviewDue(LocalDate.now());
    }

    @Override
    public boolean isIEPCompliant(Long iepId) {
        // ✅ NULL SAFE: Validate iepId parameter
        if (iepId == null) {
            return false;
        }

        IEP iep = iepRepository.findById(iepId).orElse(null);
        if (iep == null || !iep.isActive()) {
            return false;
        }

        // Check if all services are scheduled
        List<IEPService> services = iepServiceRepository.findByIepId(iepId);
        // ✅ NULL SAFE: Validate services list
        if (services == null) {
            return false;
        }

        // ✅ NULL SAFE: Filter null services and check status exists
        boolean allScheduled = services.stream()
            .filter(s -> s != null && s.getStatus() != null)
            .allMatch(s -> s.getStatus().isActive());

        // ✅ NULL SAFE: Filter null services before checking minutes
        boolean allMeetMinutes = services.stream()
            .filter(s -> s != null)
            .allMatch(IEPService::meetsMinutesRequirement);

        return allScheduled && allMeetMinutes;
    }

    // ========== 504 PLAN COMPLIANCE ==========

    @Override
    public List<Plan504> get504PlansRequiringAction() {
        List<Plan504> requiringAction = new ArrayList<>();

        // Add expired plans
        // ✅ NULL SAFE: Validate repository result
        List<Plan504> expiredPlans = plan504Repository.findExpiredPlans(LocalDate.now());
        if (expiredPlans != null) {
            requiringAction.addAll(expiredPlans);
        }

        // Add plans expiring within 30 days
        List<Plan504> expiringPlans = getExpiring504Plans(30);
        if (expiringPlans != null) {
            requiringAction.addAll(expiringPlans);
        }

        // Add plans needing review
        List<Plan504> needingReview = get504PlansWithOverdueReview();
        if (needingReview != null) {
            requiringAction.addAll(needingReview);
        }

        // Remove duplicates
        // ✅ NULL SAFE: Filter null plans before distinct
        return requiringAction.stream()
            .filter(plan -> plan != null)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<Plan504> getExpiring504Plans(int daysThreshold) {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(daysThreshold);
        return plan504Repository.findPlansNeedingRenewal(today, thresholdDate);
    }

    @Override
    public List<Plan504> get504PlansWithOverdueReview() {
        return plan504Repository.findPlansWithOverdueReview(LocalDate.now());
    }

    // ========== SERVICE MINUTES COMPLIANCE ==========

    @Override
    public List<IEPService> getServicesNotMeetingMinutes() {
        return iepServiceRepository.findServicesNotMeetingMinutes();
    }

    @Override
    public List<IEPService> getServicesNeedingScheduling() {
        return iepServiceRepository.findServicesNeedingScheduling();
    }

    @Override
    public double getServiceCompliancePercentage(Long serviceId) {
        // ✅ NULL SAFE: Validate serviceId parameter
        if (serviceId == null) {
            return 0.0;
        }

        IEPService service = iepServiceRepository.findById(serviceId).orElse(null);
        if (service == null) {
            return 0.0;
        }
        return service.getCompliancePercentage();
    }

    @Override
    public boolean meetsMinutesRequirement(Long serviceId) {
        // ✅ NULL SAFE: Validate serviceId parameter
        if (serviceId == null) {
            return false;
        }

        IEPService service = iepServiceRepository.findById(serviceId).orElse(null);
        if (service == null) {
            return false;
        }
        return service.meetsMinutesRequirement();
    }

    // ========== STUDENT COMPLIANCE ==========

    @Override
    public ComplianceSummary getStudentComplianceSummary(Long studentId) {
        // ✅ NULL SAFE: Validate studentId parameter
        if (studentId == null) {
            return new ComplianceSummary();
        }

        Student student = studentRepository.findById(studentId).orElse(null);
        ComplianceSummary summary = new ComplianceSummary();

        if (student == null) {
            return summary;
        }

        summary.studentId = studentId;
        // ✅ NULL SAFE: Safe extraction of student name
        summary.studentName = (student.getFullName() != null) ? student.getFullName() : "Unknown";

        // Check for active IEP
        Optional<IEP> activeIEP = iepRepository.findByStudentId(studentId)
            .filter(IEP::isActive);
        summary.hasActiveIEP = activeIEP.isPresent();

        // Check for active 504 plan
        Optional<Plan504> activePlan504 = plan504Repository.findByStudentId(studentId)
            .filter(Plan504::isActive);
        summary.hasActive504 = activePlan504.isPresent();

        // Get services if IEP exists
        activeIEP.ifPresent(iep -> {
            // ✅ NULL SAFE: Check IEP ID exists
            if (iep.getId() == null) {
                return;
            }

            List<IEPService> services = iepServiceRepository.findByIepId(iep.getId());
            // ✅ NULL SAFE: Validate services list
            if (services == null) {
                return;
            }

            summary.totalServices = services.size();
            // ✅ NULL SAFE: Filter null services and check status
            summary.scheduledServices = (int) services.stream()
                .filter(s -> s != null && s.getStatus() != null)
                .filter(s -> s.getStatus().isActive())
                .count();
            // ✅ NULL SAFE: Filter null services
            summary.servicesNotMeetingMinutes = (int) services.stream()
                .filter(s -> s != null)
                .filter(s -> !s.meetsMinutesRequirement())
                .count();

            // Calculate overall compliance rate
            if (summary.totalServices > 0) {
                int compliantServices = (int) services.stream()
                    .filter(IEPService::meetsMinutesRequirement)
                    .count();
                summary.overallComplianceRate = (compliantServices * 100.0) / summary.totalServices;
            }
        });

        // Compile issues
        summary.issues = getStudentComplianceIssues(studentId);

        return summary;
    }

    @Override
    public boolean isStudentCompliant(Long studentId) {
        ComplianceSummary summary = getStudentComplianceSummary(studentId);
        return summary.isCompliant();
    }

    @Override
    public List<String> getStudentComplianceIssues(Long studentId) {
        List<String> issues = new ArrayList<>();

        // ✅ NULL SAFE: Validate studentId parameter
        if (studentId == null) {
            return issues;
        }

        Optional<IEP> activeIEP = iepRepository.findByStudentId(studentId).filter(IEP::isActive);

        if (activeIEP.isEmpty()) {
            return issues; // No IEP, no issues to report
        }

        IEP iep = activeIEP.get();

        // Check if IEP is expiring soon
        if (iep.isDueForReview()) {
            issues.add("IEP review is due");
        }

        // ✅ NULL SAFE: Check end date exists
        if (iep.getEndDate() != null && iep.getEndDate().isBefore(LocalDate.now().plusDays(30))) {
            issues.add("IEP expiring within 30 days");
        }

        // Check services
        // ✅ NULL SAFE: Validate IEP ID
        if (iep.getId() == null) {
            return issues;
        }

        List<IEPService> services = iepServiceRepository.findByIepId(iep.getId());
        // ✅ NULL SAFE: Validate services list
        if (services == null) {
            return issues;
        }

        // ✅ NULL SAFE: Filter null services and check status
        long unscheduled = services.stream()
            .filter(s -> s != null && s.getStatus() != null)
            .filter(s -> !s.getStatus().isActive())
            .count();
        if (unscheduled > 0) {
            issues.add(unscheduled + " service(s) not scheduled");
        }

        // ✅ NULL SAFE: Filter null services
        long notMeetingMinutes = services.stream()
            .filter(s -> s != null)
            .filter(s -> !s.meetsMinutesRequirement())
            .count();
        if (notMeetingMinutes > 0) {
            issues.add(notMeetingMinutes + " service(s) not meeting minutes requirement");
        }

        return issues;
    }

    // ========== DASHBOARD METRICS ==========

    @Override
    public DashboardMetrics getDashboardMetrics() {
        DashboardMetrics metrics = new DashboardMetrics();

        metrics.totalActiveIEPs = iepRepository.countActiveIEPs(LocalDate.now());
        metrics.totalActive504Plans = plan504Repository.countActivePlans(LocalDate.now());

        List<IEPService> allServices = iepServiceRepository.findScheduledServices();
        metrics.totalServices = allServices.size();

        metrics.servicesNeedingScheduling = iepServiceRepository.findServicesNeedingScheduling().size();
        metrics.servicesNotMeetingMinutes = iepServiceRepository.findServicesNotMeetingMinutes().size();

        metrics.iepsExpiringWithin30Days = getExpiringIEPs(30).size();
        metrics.iepsNeedingReview = getIEPsNeedingReview().size();

        metrics.overallComplianceRate = getIEPComplianceRate();

        return metrics;
    }

    @Override
    public double getIEPComplianceRate() {
        List<IEP> activeIEPs = iepRepository.findAllActiveIEPs(LocalDate.now());
        // ✅ NULL SAFE: Validate IEPs list
        if (activeIEPs == null || activeIEPs.isEmpty()) {
            return 100.0;
        }

        // ✅ NULL SAFE: Filter null IEPs and check ID before compliance check
        long compliant = activeIEPs.stream()
            .filter(iep -> iep != null && iep.getId() != null)
            .filter(iep -> isIEPCompliant(iep.getId()))
            .count();

        return (compliant * 100.0) / activeIEPs.size();
    }

    @Override
    public Map<String, Long> getServiceStatusCounts() {
        List<Object[]> results = iepServiceRepository.countByStatus();
        // ✅ NULL SAFE: Validate results list
        if (results == null) {
            return new HashMap<>();
        }

        // ✅ NULL SAFE: Filter null results and validate array elements
        return results.stream()
            .filter(r -> r != null && r.length >= 2 && r[0] != null && r[1] != null)
            .collect(Collectors.toMap(
                r -> r[0].toString(),
                r -> (Long) r[1]
            ));
    }

    @Override
    public Map<String, Long> getIEPCountByCategory() {
        List<Object[]> results = iepRepository.countByEligibilityCategory();
        // ✅ NULL SAFE: Validate results list
        if (results == null) {
            return new HashMap<>();
        }

        // ✅ NULL SAFE: Filter null results and validate array elements
        return results.stream()
            .filter(r -> r != null && r.length >= 2 && r[1] != null)
            .collect(Collectors.toMap(
                r -> r[0] != null ? r[0].toString() : "Unknown",
                r -> (Long) r[1]
            ));
    }

    // ========== REPORTS ==========

    @Override
    public List<ComplianceSummary> generateComplianceReport() {
        List<IEP> activeIEPs = iepRepository.findAllActiveIEPs(LocalDate.now());

        // ✅ NULL SAFE: Filter IEPs with null student references
        return activeIEPs.stream()
            .filter(iep -> iep.getStudent() != null)
            .map(iep -> getStudentComplianceSummary(iep.getStudent().getId()))
            .collect(Collectors.toList());
    }

    @Override
    public ExpirationReport generateExpirationReport(int daysThreshold) {
        ExpirationReport report = new ExpirationReport();

        report.expiringIEPs = getExpiringIEPs(daysThreshold);
        report.expiring504Plans = getExpiring504Plans(daysThreshold);
        report.totalExpiring = report.expiringIEPs.size() + report.expiring504Plans.size();

        return report;
    }

    @Override
    public MinutesComplianceReport generateMinutesReport() {
        MinutesComplianceReport report = new MinutesComplianceReport();

        List<IEPService> allServices = iepServiceRepository.findScheduledServices();
        report.totalServices = allServices.size();

        for (IEPService service : allServices) {
            // ✅ NULL SAFE: Skip services with null student or serviceType references
            if (service.getStudent() == null || service.getServiceType() == null) {
                continue;
            }

            ServiceMinutesInfo info = new ServiceMinutesInfo();
            info.serviceId = service.getId();
            info.studentId = service.getStudent().getId();
            info.studentName = service.getStudent().getFullName();
            info.serviceType = service.getServiceType().getDisplayName();
            info.requiredMinutes = service.getMinutesPerWeek();
            info.scheduledMinutes = service.getScheduledMinutesPerWeek();
            info.compliancePercentage = service.getCompliancePercentage();
            info.isCompliant = service.meetsMinutesRequirement();

            report.servicesInfo.add(info);

            if (info.isCompliant) {
                report.compliantServices++;
            } else {
                report.nonCompliantServices++;
            }
        }

        if (report.totalServices > 0) {
            report.complianceRate = (report.compliantServices * 100.0) / report.totalServices;
        }

        return report;
    }
}
