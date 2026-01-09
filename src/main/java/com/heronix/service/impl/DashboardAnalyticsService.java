package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard Analytics Service
 * Provides advanced analytics, trend data, and insights for the enhanced dashboard.
 *
 * Location: src/main/java/com/eduscheduler/service/impl/DashboardAnalyticsService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-07
 */
@Slf4j
@Service
public class DashboardAnalyticsService {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SubstituteAssignmentRepository substituteAssignmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    // ========================================================================
    // TREND ANALYTICS
    // ========================================================================

    /**
     * Get teacher utilization trend over the last N months
     * Returns map of month -> utilization percentage
     */
    public Map<String, Double> getTeacherUtilizationTrend(int months) {
        Map<String, Double> trend = new LinkedHashMap<>();
        LocalDate endDate = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = endDate.minusMonths(i);
            YearMonth yearMonth = YearMonth.from(monthDate);
            String monthKey = yearMonth.getMonth().toString().substring(0, 3) + " " + yearMonth.getYear();

            double utilization = calculateTeacherUtilizationForMonth(yearMonth);
            trend.put(monthKey, utilization);
        }

        log.info("Generated teacher utilization trend for {} months", months);
        return trend;
    }

    /**
     * Get substitute usage trend over the last N months
     * Returns map of month -> number of substitute assignments
     */
    public Map<String, Long> getSubstituteUsageTrend(int months) {
        Map<String, Long> trend = new LinkedHashMap<>();
        LocalDate endDate = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = endDate.minusMonths(i);
            YearMonth yearMonth = YearMonth.from(monthDate);
            String monthKey = yearMonth.getMonth().toString().substring(0, 3) + " " + yearMonth.getYear();

            LocalDate monthStart = yearMonth.atDay(1);
            LocalDate monthEnd = yearMonth.atEndOfMonth();

            List<SubstituteAssignment> assignments = substituteAssignmentRepository.findAll();
            // ✅ NULL SAFE: Validate repository result
            if (assignments == null) {
                assignments = Collections.emptyList();
            }

            long count = assignments.stream()
                // ✅ NULL SAFE: Filter null assignments
                .filter(a -> a != null && a.getAssignmentDate() != null)
                .filter(a -> !a.getAssignmentDate().isBefore(monthStart) &&
                           !a.getAssignmentDate().isAfter(monthEnd))
                .count();

            trend.put(monthKey, count);
        }

        log.info("Generated substitute usage trend for {} months", months);
        return trend;
    }

    // ========================================================================
    // CAPACITY UTILIZATION METRICS
    // ========================================================================

    /**
     * Get room capacity utilization heatmap data
     * Returns simplified utilization metrics
     */
    public Map<String, Object> getRoomUtilizationData() {
        Map<String, Object> data = new HashMap<>();

        List<Room> rooms = roomRepository.findAll();
        List<ScheduleSlot> allSlots = scheduleSlotRepository.findAll();

        // ✅ NULL SAFE: Validate repository results
        if (rooms == null) {
            log.warn("Room repository returned null");
            rooms = Collections.emptyList();
        }
        if (allSlots == null) {
            log.warn("ScheduleSlot repository returned null");
            allSlots = Collections.emptyList();
        }

        if (rooms.isEmpty()) {
            data.put("rooms", Collections.emptyList());
            data.put("avgUtilization", 0.0);
            return data;
        }

        List<Map<String, Object>> roomUtilization = new ArrayList<>();

        for (Room room : rooms) {
            // ✅ NULL SAFE: Skip null rooms and validate room ID
            if (room == null || room.getId() == null) {
                continue;
            }

            long assignedSlots = allSlots.stream()
                // ✅ NULL SAFE: Filter null slots before accessing properties
                .filter(slot -> slot != null && slot.getRoom() != null)
                .filter(slot -> slot.getRoom().getId() != null && slot.getRoom().getId().equals(room.getId()))
                .count();

            // Assuming 40 slots per week (8 periods x 5 days)
            double utilization = (assignedSlots / 40.0) * 100.0;

            Map<String, Object> roomData = new HashMap<>();
            roomData.put("roomNumber", room.getRoomNumber());
            roomData.put("capacity", room.getCapacity());
            roomData.put("utilization", Math.min(utilization, 100.0));

            roomUtilization.add(roomData);
        }

        double avgUtilization = roomUtilization.stream()
            .mapToDouble(r -> (Double) r.get("utilization"))
            .average()
            .orElse(0.0);

        data.put("rooms", roomUtilization);
        data.put("avgUtilization", avgUtilization);

        log.info("Generated room utilization data for {} rooms, avg={:.1f}%", rooms.size(), avgUtilization);
        return data;
    }

    /**
     * Get overall capacity utilization metrics
     */
    public CapacityMetrics getCapacityMetrics() {
        long totalRooms = roomRepository.count();
        long totalTimeSlots = 8; // Assuming 8 periods per day
        long totalCapacity = totalRooms * totalTimeSlots * 5; // 5 days per week

        List<ScheduleSlot> capacitySlots = scheduleSlotRepository.findAll();
        // ✅ NULL SAFE: Validate repository result
        if (capacitySlots == null) {
            log.warn("Repository returned null for capacity slots");
            capacitySlots = Collections.emptyList();
        }

        long usedSlots = capacitySlots.stream()
            // ✅ NULL SAFE: Filter null slots before checking room
            .filter(slot -> slot != null && slot.getRoom() != null)
            .count();

        double utilizationPercentage = totalCapacity > 0 ? (usedSlots * 100.0) / totalCapacity : 0.0;

        CapacityMetrics metrics = new CapacityMetrics();
        metrics.setTotalCapacity(totalCapacity);
        metrics.setUsedSlots(usedSlots);
        metrics.setAvailableSlots(totalCapacity - usedSlots);
        metrics.setUtilizationPercentage(utilizationPercentage);

        log.info("Capacity metrics: {:.1f}% utilization ({} / {})",
                utilizationPercentage, usedSlots, totalCapacity);

        return metrics;
    }

    // ========================================================================
    // PERFORMANCE METRICS
    // ========================================================================

    /**
     * Get schedule quality metrics
     */
    public QualityMetrics getScheduleQualityMetrics() {
        QualityMetrics metrics = new QualityMetrics();

        List<Schedule> schedules = scheduleRepository.findAll();
        // ✅ NULL SAFE: Validate repository result
        if (schedules == null) {
            log.warn("Schedule repository returned null");
            schedules = Collections.emptyList();
        }

        if (schedules.isEmpty()) {
            metrics.setHasData(false);
            return metrics;
        }

        Schedule latestSchedule = schedules.get(schedules.size() - 1);

        metrics.setHasData(true);
        metrics.setOptimizationScore(latestSchedule.getOptimizationScore() != null ?
            latestSchedule.getOptimizationScore() : 0.0);
        metrics.setConflictCount(latestSchedule.getTotalConflicts() != null ?
            latestSchedule.getTotalConflicts() : 0);
        metrics.setTeacherUtilization(latestSchedule.getTeacherUtilization() != null ?
            latestSchedule.getTeacherUtilization() : 0.0);

        // Calculate conflict-free percentage
        long totalSlots = scheduleSlotRepository.count();
        double conflictFreePercentage = totalSlots > 0 ?
            ((totalSlots - metrics.getConflictCount()) * 100.0) / totalSlots : 100.0;
        metrics.setConflictFreePercentage(conflictFreePercentage);

        log.info("Schedule quality: {}% optimization, {} conflicts, {:.1f}% conflict-free",
                metrics.getOptimizationScore(), metrics.getConflictCount(), conflictFreePercentage);

        return metrics;
    }

    /**
     * Get teacher workload distribution metrics
     */
    public WorkloadMetrics getWorkloadMetrics() {
        List<Teacher> teachers = teacherRepository.findAllActive();
        List<ScheduleSlot> allSlots = scheduleSlotRepository.findAll();

        // ✅ NULL SAFE: Validate repository results
        if (teachers == null) {
            log.warn("Teacher repository returned null");
            teachers = Collections.emptyList();
        }
        if (allSlots == null) {
            log.warn("ScheduleSlot repository returned null");
            allSlots = Collections.emptyList();
        }

        Map<Long, Long> teacherSlotCount = allSlots.stream()
            // ✅ NULL SAFE: Filter null slots and validate teacher before grouping
            .filter(slot -> slot != null && slot.getTeacher() != null)
            .filter(slot -> slot.getTeacher().getId() != null)
            .collect(Collectors.groupingBy(
                slot -> slot.getTeacher().getId(),
                Collectors.counting()
            ));

        List<Long> slotCounts = new ArrayList<>(teacherSlotCount.values());

        WorkloadMetrics metrics = new WorkloadMetrics();

        if (!slotCounts.isEmpty()) {
            metrics.setAverageSlots(slotCounts.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0));

            metrics.setMinSlots(Collections.min(slotCounts));
            metrics.setMaxSlots(Collections.max(slotCounts));

            // Calculate standard deviation for workload balance
            double mean = metrics.getAverageSlots();
            double variance = slotCounts.stream()
                .mapToDouble(count -> Math.pow(count - mean, 2))
                .average()
                .orElse(0.0);
            metrics.setStandardDeviation(Math.sqrt(variance));

            // Workload balance score (lower stddev = better balance)
            double balanceScore = 100.0 - Math.min(metrics.getStandardDeviation() * 10, 100.0);
            metrics.setBalanceScore(balanceScore);
        }

        log.info("Workload metrics: avg={:.1f}, min={}, max={}, balance={:.1f}%",
                metrics.getAverageSlots(), metrics.getMinSlots(),
                metrics.getMaxSlots(), metrics.getBalanceScore());

        return metrics;
    }

    /**
     * Get alerts and warnings for the dashboard
     */
    public List<Alert> getAlerts() {
        List<Alert> alerts = new ArrayList<>();

        LocalDate now = LocalDate.now();

        // Check for overutilized teachers (>= 90% of max hours)
        List<Teacher> overutilized = teacherRepository.findTeachersNearMaxHours();
        // ✅ NULL SAFE: Validate repository result
        if (overutilized == null) {
            log.warn("Repository returned null for overutilized teachers");
            overutilized = Collections.emptyList();
        }

        if (!overutilized.isEmpty()) {
            alerts.add(new Alert(
                "WARNING",
                "Overutilized Teachers",
                overutilized.size() + " teacher(s) near maximum hours"
            ));
        }

        // Check for underutilized teachers (< 50% utilization)
        List<Teacher> underutilized = teacherRepository.findUnderutilizedTeachers();
        // ✅ NULL SAFE: Validate repository result
        if (underutilized == null) {
            log.warn("Repository returned null for underutilized teachers");
            underutilized = Collections.emptyList();
        }

        if (!underutilized.isEmpty()) {
            alerts.add(new Alert(
                "INFO",
                "Underutilized Teachers",
                underutilized.size() + " teacher(s) have low utilization"
            ));
        }

        // Check for pending substitute assignments
        List<SubstituteAssignment> allAssignments = substituteAssignmentRepository.findAll();
        // ✅ NULL SAFE: Validate repository result
        if (allAssignments == null) {
            log.warn("Repository returned null for substitute assignments");
            allAssignments = Collections.emptyList();
        }

        long pendingAssignments = allAssignments.stream()
            // ✅ NULL SAFE: Filter null assignments before checking status
            .filter(a -> a != null && a.getStatus() != null)
            .filter(a -> a.getStatus().name().equals("PENDING"))
            .count();

        if (pendingAssignments > 0) {
            alerts.add(new Alert(
                "INFO",
                "Pending Assignments",
                pendingAssignments + " substitute assignment(s) need confirmation"
            ));
        }

        // Check for IEP/504 reviews due soon
        List<Student> allStudents = studentRepository.findAll();
        // ✅ NULL SAFE: Validate repository result
        if (allStudents == null) {
            log.warn("Repository returned null for students");
            allStudents = Collections.emptyList();
        }

        long upcomingReviews = allStudents.stream()
            // ✅ NULL SAFE: Filter null students before checking review date
            .filter(s -> s != null && s.getAccommodationReviewDate() != null)
            .filter(s -> s.getAccommodationReviewDate().isAfter(now) &&
                       s.getAccommodationReviewDate().isBefore(now.plusDays(7)))
            .count();

        if (upcomingReviews > 0) {
            alerts.add(new Alert(
                "WARNING",
                "Upcoming Reviews",
                upcomingReviews + " IEP/504 review(s) due within 7 days"
            ));
        }

        log.info("Generated {} alerts for dashboard", alerts.size());
        return alerts;
    }

    /**
     * Get week-over-week comparison metrics
     */
    public ComparisonMetrics getWeekOverWeekComparison() {
        LocalDate today = LocalDate.now();
        LocalDate thisWeekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);

        List<SubstituteAssignment> weekAssignments = substituteAssignmentRepository.findAll();
        // ✅ NULL SAFE: Validate repository result
        if (weekAssignments == null) {
            log.warn("Repository returned null for week assignments");
            weekAssignments = Collections.emptyList();
        }

        List<SubstituteAssignment> thisWeekAssignments = weekAssignments.stream()
            // ✅ NULL SAFE: Filter null assignments before checking date
            .filter(a -> a != null && a.getAssignmentDate() != null)
            .filter(a -> !a.getAssignmentDate().isBefore(thisWeekStart))
            .collect(Collectors.toList());

        List<SubstituteAssignment> lastWeekAssignments = weekAssignments.stream()
            // ✅ NULL SAFE: Filter null assignments before checking date
            .filter(a -> a != null && a.getAssignmentDate() != null)
            .filter(a -> !a.getAssignmentDate().isBefore(lastWeekStart) &&
                       !a.getAssignmentDate().isAfter(lastWeekEnd))
            .collect(Collectors.toList());

        ComparisonMetrics metrics = new ComparisonMetrics();
        metrics.setThisWeekSubstitutes(thisWeekAssignments.size());
        metrics.setLastWeekSubstitutes(lastWeekAssignments.size());

        if (lastWeekAssignments.size() > 0) {
            double change = ((thisWeekAssignments.size() - lastWeekAssignments.size()) * 100.0) /
                          lastWeekAssignments.size();
            metrics.setPercentageChange(change);
        } else {
            metrics.setPercentageChange(thisWeekAssignments.size() > 0 ? 100.0 : 0.0);
        }

        log.info("Week comparison: this week={}, last week={}, change={:.1f}%",
                metrics.getThisWeekSubstitutes(), metrics.getLastWeekSubstitutes(),
                metrics.getPercentageChange());

        return metrics;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private double calculateTeacherUtilizationForMonth(YearMonth yearMonth) {
        List<ScheduleSlot> allMonthSlots = scheduleSlotRepository.findAll();
        // ✅ NULL SAFE: Validate repository result
        if (allMonthSlots == null) {
            log.warn("Repository returned null for month slots");
            allMonthSlots = Collections.emptyList();
        }

        List<ScheduleSlot> monthSlots = allMonthSlots.stream()
            // ✅ NULL SAFE: Filter null slots before checking teacher
            .filter(slot -> slot != null && slot.getTeacher() != null)
            .collect(Collectors.toList());

        if (monthSlots.isEmpty()) {
            return 0.0;
        }

        // Simplified utilization: percentage of teachers with assignments
        long teachersWithAssignments = monthSlots.stream()
            // ✅ NULL SAFE: Validate teacher ID before mapping
            .filter(slot -> slot.getTeacher() != null && slot.getTeacher().getId() != null)
            .map(slot -> slot.getTeacher().getId())
            .distinct()
            .count();

        long totalTeachers = teacherRepository.countByActiveTrue();
        return totalTeachers > 0 ? (teachersWithAssignments * 100.0) / totalTeachers : 0.0;
    }

    // ========================================================================
    // DATA CLASSES
    // ========================================================================

    public static class CapacityMetrics {
        private long totalCapacity;
        private long usedSlots;
        private long availableSlots;
        private double utilizationPercentage;

        public long getTotalCapacity() { return totalCapacity; }
        public void setTotalCapacity(long totalCapacity) { this.totalCapacity = totalCapacity; }
        public long getUsedSlots() { return usedSlots; }
        public void setUsedSlots(long usedSlots) { this.usedSlots = usedSlots; }
        public long getAvailableSlots() { return availableSlots; }
        public void setAvailableSlots(long availableSlots) { this.availableSlots = availableSlots; }
        public double getUtilizationPercentage() { return utilizationPercentage; }
        public void setUtilizationPercentage(double utilizationPercentage) {
            this.utilizationPercentage = utilizationPercentage;
        }
    }

    public static class QualityMetrics {
        private boolean hasData;
        private double optimizationScore;
        private int conflictCount;
        private double teacherUtilization;
        private double conflictFreePercentage;

        public boolean isHasData() { return hasData; }
        public void setHasData(boolean hasData) { this.hasData = hasData; }
        public double getOptimizationScore() { return optimizationScore; }
        public void setOptimizationScore(double optimizationScore) {
            this.optimizationScore = optimizationScore;
        }
        public int getConflictCount() { return conflictCount; }
        public void setConflictCount(int conflictCount) { this.conflictCount = conflictCount; }
        public double getTeacherUtilization() { return teacherUtilization; }
        public void setTeacherUtilization(double teacherUtilization) {
            this.teacherUtilization = teacherUtilization;
        }
        public double getConflictFreePercentage() { return conflictFreePercentage; }
        public void setConflictFreePercentage(double conflictFreePercentage) {
            this.conflictFreePercentage = conflictFreePercentage;
        }
    }

    public static class WorkloadMetrics {
        private double averageSlots;
        private long minSlots;
        private long maxSlots;
        private double standardDeviation;
        private double balanceScore;

        public double getAverageSlots() { return averageSlots; }
        public void setAverageSlots(double averageSlots) { this.averageSlots = averageSlots; }
        public long getMinSlots() { return minSlots; }
        public void setMinSlots(long minSlots) { this.minSlots = minSlots; }
        public long getMaxSlots() { return maxSlots; }
        public void setMaxSlots(long maxSlots) { this.maxSlots = maxSlots; }
        public double getStandardDeviation() { return standardDeviation; }
        public void setStandardDeviation(double standardDeviation) {
            this.standardDeviation = standardDeviation;
        }
        public double getBalanceScore() { return balanceScore; }
        public void setBalanceScore(double balanceScore) { this.balanceScore = balanceScore; }
    }

    public static class Alert {
        private String severity; // INFO, WARNING, ERROR
        private String title;
        private String message;

        public Alert(String severity, String title, String message) {
            this.severity = severity;
            this.title = title;
            this.message = message;
        }

        public String getSeverity() { return severity; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
    }

    public static class ComparisonMetrics {
        private int thisWeekSubstitutes;
        private int lastWeekSubstitutes;
        private double percentageChange;

        public int getThisWeekSubstitutes() { return thisWeekSubstitutes; }
        public void setThisWeekSubstitutes(int thisWeekSubstitutes) {
            this.thisWeekSubstitutes = thisWeekSubstitutes;
        }
        public int getLastWeekSubstitutes() { return lastWeekSubstitutes; }
        public void setLastWeekSubstitutes(int lastWeekSubstitutes) {
            this.lastWeekSubstitutes = lastWeekSubstitutes;
        }
        public double getPercentageChange() { return percentageChange; }
        public void setPercentageChange(double percentageChange) {
            this.percentageChange = percentageChange;
        }
    }
}
