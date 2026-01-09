package com.heronix.service;

import com.heronix.model.dto.DashboardMetrics;
import com.heronix.model.dto.Recommendation;
import java.util.List;
import java.util.Map;

/**
 * Analytics Service Interface
 * Location: src/main/java/com/eduscheduler/service/AnalyticsService.java
 */
public interface AnalyticsService {

    DashboardMetrics getDashboardMetrics();

    double calculateTeacherUtilization(Long teacherId);

    double calculateRoomUtilization(Long roomId);

    double calculateScheduleQuality(Long scheduleId);

    List<Recommendation> getRecommendations(Long scheduleId);

    Map<String, Object> generateEfficiencyReport(Long scheduleId);

    Map<Long, Integer> getTeacherWorkloadDistribution();

    Map<Long, Double> getRoomUsageStatistics();

    Map<String, Double> calculateLeanMetrics(Long scheduleId);
}