package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.dto.DashboardMetrics;
import com.heronix.model.dto.Recommendation;
import com.heronix.repository.*;
import com.heronix.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service Implementation
 * Location:
 * src/main/java/com/eduscheduler/service/impl/AnalyticsServiceImpl.java
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardMetrics getDashboardMetrics() {
        DashboardMetrics metrics = new DashboardMetrics();

        try {
            // Total counts
            metrics.setTotalTeachers(teacherRepository.countByActiveTrue());
            metrics.setTotalCourses(courseRepository.count());
            metrics.setTotalRooms(roomRepository.count());
            metrics.setTotalSchedules(scheduleRepository.count());

            // Active entities
            metrics.setActiveTeachers(teacherRepository.findByActiveTrue().size());
            metrics.setActiveCourses(courseRepository.findByActiveTrue().size());
            metrics.setActiveRooms(roomRepository.findByActiveTrue().size());

            // Schedule status counts
            List<Schedule> allSchedules = scheduleRepository.findAll();
            // ✅ NULL SAFE: Filter null schedules before checking status
            metrics.setDraftSchedules(allSchedules.stream()
                    .filter(s -> s != null && s.getStatus() != null && s.getStatus().name().contains("DRAFT"))
                    .count());
            metrics.setPublishedSchedules(allSchedules.stream()
                    .filter(s -> s != null && s.getStatus() != null && s.getStatus().name().contains("PUBLISHED"))
                    .count());
            metrics.setArchivedSchedules(allSchedules.stream()
                    .filter(s -> s != null && s.getStatus() != null && s.getStatus().name().contains("ARCHIVED"))
                    .count());

            // Average optimization score
            // ✅ NULL SAFE: Filter null schedules before accessing score
            double avgScore = allSchedules.stream()
                    .filter(s -> s != null && s.getOptimizationScore() != null)
                    .mapToDouble(Schedule::getOptimizationScore)
                    .average()
                    .orElse(0.0);
            metrics.setAverageOptimizationScore(avgScore);

            // Teacher utilization
            // ✅ NULL SAFE: Filter null teachers and check ID before calculation
            double teacherUtil = teacherRepository.findByActiveTrue().stream()
                    .filter(t -> t != null && t.getId() != null)
                    .mapToDouble(t -> calculateTeacherUtilization(t.getId()))
                    .average()
                    .orElse(0.0);
            metrics.setTeacherUtilizationRate(teacherUtil);

            // Room utilization
            // ✅ NULL SAFE: Filter null rooms and check ID before calculation
            double roomUtil = roomRepository.findByActiveTrue().stream()
                    .filter(r -> r != null && r.getId() != null)
                    .mapToDouble(r -> calculateRoomUtilization(r.getId()))
                    .average()
                    .orElse(0.0);
            metrics.setRoomUtilizationRate(roomUtil);

        } catch (Exception e) {
            log.error("Error calculating dashboard metrics", e);
        }

        return metrics;
    }

    @Override
    public double calculateTeacherUtilization(Long teacherId) {
        // ✅ NULL SAFE: Validate teacherId parameter
        if (teacherId == null) {
            return 0.0;
        }

        try {
            Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
            if (teacher == null || teacher.getMaxHoursPerWeek() == null || teacher.getMaxHoursPerWeek() == 0) {
                return 0.0;
            }

            // ✅ NULL SAFE: Filter null slots and check teacher/ID before comparison
            long assignedSlots = scheduleSlotRepository.findAll().stream()
                    .filter(slot -> slot != null && slot.getTeacher() != null &&
                            slot.getTeacher().getId() != null &&
                            slot.getTeacher().getId().equals(teacherId))
                    .count();

            return (assignedSlots * 100.0) / teacher.getMaxHoursPerWeek();
        } catch (Exception e) {
            log.error("Error calculating teacher utilization for ID: {}", teacherId, e);
            return 0.0;
        }
    }

    @Override
    public double calculateRoomUtilization(Long roomId) {
        // ✅ NULL SAFE: Validate roomId parameter
        if (roomId == null) {
            return 0.0;
        }

        try {
            Room room = roomRepository.findById(roomId).orElse(null);
            if (room == null) {
                return 0.0;
            }

            // ✅ NULL SAFE: Filter null slots and check room/ID before comparison
            long usedSlots = scheduleSlotRepository.findAll().stream()
                    .filter(slot -> slot != null && slot.getRoom() != null &&
                            slot.getRoom().getId() != null &&
                            slot.getRoom().getId().equals(roomId))
                    .count();

            // Assume 35 time slots per week (7 periods × 5 days)
            long totalAvailableSlots = 35L;

            return (usedSlots * 100.0) / totalAvailableSlots;
        } catch (Exception e) {
            log.error("Error calculating room utilization for ID: {}", roomId, e);
            return 0.0;
        }
    }

    @Override
    public double calculateScheduleQuality(Long scheduleId) {
        // ✅ NULL SAFE: Validate scheduleId parameter
        if (scheduleId == null) {
            return 0.0;
        }

        try {
            Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
            if (schedule == null) {
                return 0.0;
            }

            // Use optimization score if available
            if (schedule.getOptimizationScore() != null) {
                return schedule.getOptimizationScore();
            }

            // ✅ NULL SAFE: Safe extraction of metrics with defaults
            double teacherUtil = schedule.getTeacherUtilization() != null ? schedule.getTeacherUtilization() : 0.0;
            double roomUtil = schedule.getRoomUtilization() != null ? schedule.getRoomUtilization() : 0.0;
            double efficiency = schedule.getEfficiencyRate() != null ? schedule.getEfficiencyRate() : 0.0;

            return (teacherUtil + roomUtil + efficiency) / 3.0;
        } catch (Exception e) {
            log.error("Error calculating schedule quality for ID: {}", scheduleId, e);
            return 0.0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendations(Long scheduleId) {
        List<Recommendation> recommendations = new ArrayList<>();

        // ✅ NULL SAFE: Validate scheduleId parameter
        if (scheduleId == null) {
            return recommendations;
        }

        try {
            Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
            if (schedule == null) {
                return recommendations;
            }

            // ✅ NULL SAFE: Check recommendation lists before adding
            List<Recommendation> workloadRecs = getWorkloadRecommendations(schedule);
            if (workloadRecs != null) {
                recommendations.addAll(workloadRecs);
            }

            List<Recommendation> roomRecs = getRoomOptimizationRecommendations(schedule);
            if (roomRecs != null) {
                recommendations.addAll(roomRecs);
            }

            List<Recommendation> qualityRecs = getQualityRecommendations(schedule);
            if (qualityRecs != null) {
                recommendations.addAll(qualityRecs);
            }

        } catch (Exception e) {
            log.error("Error generating recommendations for schedule ID: {}", scheduleId, e);
        }

        return recommendations;
    }

    private List<Recommendation> getWorkloadRecommendations(Schedule schedule) {
        List<Recommendation> recommendations = new ArrayList<>();

        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null || schedule.getSlots() == null) {
            return recommendations;
        }

        // ✅ NULL SAFE: Filter null slots and check teacher/ID before grouping
        Map<Long, Long> teacherLoads = schedule.getSlots().stream()
                .filter(slot -> slot != null && slot.getTeacher() != null &&
                               slot.getTeacher().getId() != null)
                .collect(Collectors.groupingBy(
                        slot -> slot.getTeacher().getId(),
                        Collectors.counting()));

        teacherLoads.forEach((teacherId, count) -> {
            if (count > 35) { // More than 35 slots per week
                Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
                if (teacher != null) {
                    // ✅ NULL SAFE: Safe extraction of teacher name with default
                    String teacherName = (teacher.getName() != null) ? teacher.getName() : "Unknown Teacher";

                    Recommendation rec = new Recommendation();
                    rec.setType("WORKLOAD_BALANCE");
                    rec.setSeverity("WARNING");
                    rec.setTitle("Teacher Overload Detected");
                    rec.setDescription(teacherName + " has " + count + " slots assigned");
                    rec.setAffectedEntityId(teacherId);
                    rec.setAffectedEntityType("TEACHER");
                    rec.setAffectedEntityName(teacherName);
                    rec.setImpactScore(75.0);
                    rec.setActionRequired("Consider redistributing classes to balance workload");
                    rec.setCanAutoFix(false);
                    rec.setCategory("LEAN");
                    recommendations.add(rec);
                }
            }
        });

        return recommendations;
    }

    private List<Recommendation> getRoomOptimizationRecommendations(Schedule schedule) {
        List<Recommendation> recommendations = new ArrayList<>();

        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null || schedule.getSlots() == null) {
            return recommendations;
        }

        // ✅ NULL SAFE: Filter null slots and check room/ID before grouping
        Map<Long, Long> roomUsage = schedule.getSlots().stream()
                .filter(slot -> slot != null && slot.getRoom() != null &&
                               slot.getRoom().getId() != null)
                .collect(Collectors.groupingBy(
                        slot -> slot.getRoom().getId(),
                        Collectors.counting()));

        roomUsage.forEach((roomId, count) -> {
            if (count < 10) { // Underutilized (less than 10 slots)
                Room room = roomRepository.findById(roomId).orElse(null);
                if (room != null) {
                    // ✅ NULL SAFE: Safe extraction of room number with default
                    String roomNumber = (room.getRoomNumber() != null) ? room.getRoomNumber() : "Unknown Room";

                    Recommendation rec = new Recommendation();
                    rec.setType("ROOM_OPTIMIZATION");
                    rec.setSeverity("INFO");
                    rec.setTitle("Room Underutilized");
                    rec.setDescription("Room " + roomNumber + " is only used " + count + " times");
                    rec.setAffectedEntityId(roomId);
                    rec.setAffectedEntityType("ROOM");
                    rec.setAffectedEntityName(roomNumber);
                    rec.setImpactScore(40.0);
                    rec.setActionRequired("Consider scheduling more classes in this room");
                    rec.setCanAutoFix(true);
                    rec.setCategory("LEAN");
                    recommendations.add(rec);
                }
            }
        });

        return recommendations;
    }

    private List<Recommendation> getQualityRecommendations(Schedule schedule) {
        List<Recommendation> recommendations = new ArrayList<>();

        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null) {
            return recommendations;
        }

        if (schedule.getOptimizationScore() != null && schedule.getOptimizationScore() < 70) {
            // ✅ NULL SAFE: Safe extraction of schedule name with default
            String scheduleName = (schedule.getScheduleName() != null) ?
                schedule.getScheduleName() : "Unknown Schedule";

            Recommendation rec = new Recommendation();
            rec.setType("SCHEDULE_QUALITY");
            rec.setSeverity("WARNING");
            rec.setTitle("Low Optimization Score");
            rec.setDescription("Schedule optimization score is " +
                    String.format("%.1f%%", schedule.getOptimizationScore()));
            rec.setAffectedEntityId(schedule.getId());
            rec.setAffectedEntityType("SCHEDULE");
            rec.setAffectedEntityName(scheduleName);
            rec.setImpactScore(80.0);
            rec.setActionRequired("Re-run optimization with adjusted constraints");
            rec.setCanAutoFix(true);
            rec.setCategory("EISENHOWER");
            recommendations.add(rec);
        }

        return recommendations;
    }

    @Override
    public Map<String, Object> generateEfficiencyReport(Long scheduleId) {
        Map<String, Object> report = new HashMap<>();

        // ✅ NULL SAFE: Validate scheduleId parameter
        if (scheduleId == null) {
            return report;
        }

        try {
            Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
            if (schedule == null) {
                return report;
            }

            report.put("scheduleId", scheduleId);
            // ✅ NULL SAFE: Safe extraction of schedule properties with defaults
            report.put("scheduleName", schedule.getScheduleName() != null ? schedule.getScheduleName() : "Unknown");
            report.put("optimizationScore", schedule.getOptimizationScore() != null ? schedule.getOptimizationScore() : 0.0);
            report.put("teacherUtilization", schedule.getTeacherUtilization() != null ? schedule.getTeacherUtilization() : 0.0);
            report.put("roomUtilization", schedule.getRoomUtilization() != null ? schedule.getRoomUtilization() : 0.0);
            report.put("efficiencyRate", schedule.getEfficiencyRate() != null ? schedule.getEfficiencyRate() : 0.0);
            report.put("totalSlots", schedule.getSlots() != null ? schedule.getSlots().size() : 0);
            report.put("conflicts", schedule.getTotalConflicts() != null ? schedule.getTotalConflicts() : 0);
            report.put("resolvedConflicts", schedule.getResolvedConflicts() != null ? schedule.getResolvedConflicts() : 0);

        } catch (Exception e) {
            log.error("Error generating efficiency report for schedule ID: {}", scheduleId, e);
        }

        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getTeacherWorkloadDistribution() {
        Map<Long, Integer> distribution = new HashMap<>();

        try {
            List<ScheduleSlot> allSlots = scheduleSlotRepository.findAll();

            // ✅ NULL SAFE: Filter null slots and check teacher/ID before grouping
            Map<Long, Long> rawCounts = allSlots.stream()
                    .filter(slot -> slot != null && slot.getTeacher() != null &&
                                   slot.getTeacher().getId() != null)
                    .collect(Collectors.groupingBy(
                            slot -> slot.getTeacher().getId(),
                            Collectors.counting()));

            // Convert Long to Integer
            rawCounts.forEach((teacherId, count) -> distribution.put(teacherId, count.intValue()));

        } catch (Exception e) {
            log.error("Error calculating teacher workload distribution", e);
        }

        return distribution;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Double> getRoomUsageStatistics() {
        Map<Long, Double> statistics = new HashMap<>();

        try {
            List<Room> rooms = roomRepository.findAll();
            List<ScheduleSlot> allSlots = scheduleSlotRepository.findAll();

            for (Room room : rooms) {
                // ✅ NULL SAFE: Skip null rooms and check ID
                if (room == null || room.getId() == null) continue;

                // ✅ NULL SAFE: Filter null slots and check room/ID before comparison
                long usedSlots = allSlots.stream()
                        .filter(slot -> slot != null && slot.getRoom() != null &&
                                       slot.getRoom().getId() != null &&
                                       slot.getRoom().getId().equals(room.getId()))
                        .count();

                double utilizationRate = (usedSlots * 100.0) / 35.0; // 35 slots per week
                statistics.put(room.getId(), utilizationRate);
            }

        } catch (Exception e) {
            log.error("Error calculating room usage statistics", e);
        }

        return statistics;
    }

    @Override
    public Map<String, Double> calculateLeanMetrics(Long scheduleId) {
        Map<String, Double> metrics = new HashMap<>();

        // ✅ NULL SAFE: Validate scheduleId parameter
        if (scheduleId == null) {
            return metrics;
        }

        try {
            Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
            if (schedule == null) {
                return metrics;
            }

            // ✅ NULL SAFE: Safe extraction of metrics with defaults
            metrics.put("teacherUtilization", schedule.getTeacherUtilization() != null ? schedule.getTeacherUtilization() : 0.0);
            metrics.put("roomUtilization", schedule.getRoomUtilization() != null ? schedule.getRoomUtilization() : 0.0);
            metrics.put("efficiencyRate", schedule.getEfficiencyRate() != null ? schedule.getEfficiencyRate() : 0.0);
            metrics.put("optimizationScore", schedule.getOptimizationScore() != null ? schedule.getOptimizationScore() : 0.0);

            // Calculate waste metrics
            // ✅ NULL SAFE: Check slots collection exists before filtering
            if (schedule.getSlots() != null) {
                long emptySlots = schedule.getSlots().stream()
                        .filter(slot -> slot != null && (slot.getTeacher() == null || slot.getRoom() == null))
                        .count();

                double wasteRate = (emptySlots * 100.0) / Math.max(schedule.getSlots().size(), 1);
                metrics.put("wasteRate", wasteRate);
            } else {
                metrics.put("wasteRate", 0.0);
            }

        } catch (Exception e) {
            log.error("Error calculating Lean metrics for schedule ID: {}", scheduleId, e);
        }

        return metrics;
    }
}