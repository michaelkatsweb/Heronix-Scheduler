package com.heronix.service;

import com.heronix.model.domain.LunchPeriod;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing lunch periods
 */
public interface LunchPeriodService {
    
    /**
     * Create a new lunch period
     */
    LunchPeriod createLunchPeriod(LunchPeriod lunchPeriod);
    
    /**
     * Get lunch period by ID
     */
    Optional<LunchPeriod> getLunchPeriodById(Long id);
    
    /**
     * Get all lunch periods
     */
    List<LunchPeriod> getAllLunchPeriods();
    
    /**
     * Get all active lunch periods
     */
    List<LunchPeriod> getActiveLunchPeriods();
    
    /**
     * Get lunch periods by lunch group
     */
    List<LunchPeriod> getLunchPeriodsByGroup(String lunchGroup);
    
    /**
     * Get lunch periods for specific day
     */
    List<LunchPeriod> getLunchPeriodsForDay(Integer dayOfWeek);
    
    /**
     * Get lunch periods for grade level
     */
    List<LunchPeriod> getLunchPeriodsForGradeLevel(String gradeLevel);
    
    /**
     * Update lunch period
     */
    LunchPeriod updateLunchPeriod(Long id, LunchPeriod lunchPeriod);
    
    /**
     * Delete lunch period (soft delete - set inactive)
     */
    void deleteLunchPeriod(Long id);
    
    /**
     * Activate lunch period
     */
    void activateLunchPeriod(Long id);
    
    /**
     * Check if lunch period exists
     */
    boolean existsById(Long id);
    
    /**
     * Check for time conflicts with existing lunch periods
     */
    boolean hasTimeConflict(LocalTime startTime, LocalTime endTime, String location);

    /**
     * Assign lunch periods to schedule based on configuration
     */
    void assignLunchPeriods(com.heronix.model.domain.Schedule schedule,
            com.heronix.model.domain.LunchConfiguration config);

    /**
     * Get lunch slots for a specific student
     */
    List<com.heronix.model.domain.ScheduleSlot> getLunchSlotsForStudent(
            com.heronix.model.domain.Student student);

    /**
     * Get lunch slots for a specific teacher
     */
    List<com.heronix.model.domain.ScheduleSlot> getLunchSlotsForTeacher(
            com.heronix.model.domain.Teacher teacher);

    /**
     * Get lunch distribution across waves
     */
    java.util.Map<Integer, List<com.heronix.model.domain.Student>> getLunchDistribution(
            com.heronix.model.domain.Schedule schedule);

    /**
     * Validate lunch capacity doesn't exceed limits
     */
    boolean validateLunchCapacity(com.heronix.model.domain.Schedule schedule,
            com.heronix.model.domain.LunchConfiguration config);

    /**
     * Stagger lunch periods by grade level
     */
    void staggerLunchByGrade(com.heronix.model.domain.Schedule schedule,
            com.heronix.model.domain.LunchConfiguration config);

    /**
     * Determine lunch wave for a student
     */
    Integer determineLunchWave(com.heronix.model.domain.Student student,
            com.heronix.model.domain.LunchConfiguration config);

    /**
     * Check if slot conflicts with lunch period
     */
    boolean hasLunchConflict(com.heronix.model.domain.ScheduleSlot slot);
}