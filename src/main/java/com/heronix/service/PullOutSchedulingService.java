package com.heronix.service;

import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.PullOutSchedule;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pull-Out Scheduling Service Interface
 *
 * Provides business logic for scheduling pull-out service sessions.
 * Handles conflict detection, schedule optimization, and compliance tracking.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8B - November 21, 2025
 */
public interface PullOutSchedulingService {

    // ========== CRUD OPERATIONS ==========

    /**
     * Create a new pull-out schedule
     *
     * @param schedule Schedule to create
     * @return Created schedule with ID
     */
    PullOutSchedule createSchedule(PullOutSchedule schedule);

    /**
     * Update an existing pull-out schedule
     *
     * @param schedule Schedule to update
     * @return Updated schedule
     */
    PullOutSchedule updateSchedule(PullOutSchedule schedule);

    /**
     * Find schedule by ID
     *
     * @param id Schedule ID
     * @return Optional containing schedule if found
     */
    Optional<PullOutSchedule> findById(Long id);

    /**
     * Delete a pull-out schedule
     *
     * @param id Schedule ID
     */
    void deleteSchedule(Long id);

    // ========== SCHEDULING OPERATIONS ==========

    /**
     * Schedule a service for a student (find optimal time slot)
     *
     * @param serviceId IEP service ID
     * @return Created schedule if successful
     * @throws IllegalStateException if no suitable time slot found
     */
    PullOutSchedule scheduleService(Long serviceId);

    /**
     * Schedule multiple services for a student at once
     *
     * @param studentId Student ID
     * @return List of created schedules
     */
    List<PullOutSchedule> scheduleAllServicesForStudent(Long studentId);

    /**
     * Reschedule an existing session to a new time
     *
     * @param scheduleId Schedule ID
     * @param newDayOfWeek New day
     * @param newStartTime New start time
     * @param newEndTime New end time
     * @return Updated schedule
     */
    PullOutSchedule reschedule(Long scheduleId, String newDayOfWeek, LocalTime newStartTime, LocalTime newEndTime);

    /**
     * Cancel a scheduled session
     *
     * @param scheduleId Schedule ID
     * @return Updated schedule with CANCELLED status
     */
    PullOutSchedule cancelSchedule(Long scheduleId);

    // ========== CONFLICT DETECTION ==========

    /**
     * Check if a time slot has conflicts for a student
     *
     * @param studentId Student ID
     * @param dayOfWeek Day of week
     * @param startTime Start time
     * @param endTime End time
     * @return List of conflicting schedules (empty if no conflicts)
     */
    List<PullOutSchedule> checkStudentConflicts(Long studentId, String dayOfWeek, LocalTime startTime, LocalTime endTime);

    /**
     * Check if a time slot has conflicts for a staff member
     *
     * @param staffId Staff ID
     * @param dayOfWeek Day of week
     * @param startTime Start time
     * @param endTime End time
     * @return List of conflicting schedules (empty if no conflicts)
     */
    List<PullOutSchedule> checkStaffConflicts(Long staffId, String dayOfWeek, LocalTime startTime, LocalTime endTime);

    /**
     * Check if a time slot is available (no conflicts for student or staff)
     *
     * @param studentId Student ID
     * @param staffId Staff ID
     * @param dayOfWeek Day of week
     * @param startTime Start time
     * @param endTime End time
     * @return true if time slot is available
     */
    boolean isTimeSlotAvailable(Long studentId, Long staffId, String dayOfWeek, LocalTime startTime, LocalTime endTime);

    /**
     * Find all conflicts for a student's current schedule
     *
     * @param studentId Student ID
     * @return List of conflicting schedule pairs
     */
    List<Map<String, PullOutSchedule>> findAllStudentConflicts(Long studentId);

    // ========== QUERIES ==========

    /**
     * Get all active schedules for a student
     *
     * @param studentId Student ID
     * @return List of active schedules
     */
    List<PullOutSchedule> getStudentSchedule(Long studentId);

    /**
     * Get all active schedules for a staff member
     *
     * @param staffId Staff ID
     * @return List of active schedules
     */
    List<PullOutSchedule> getStaffSchedule(Long staffId);

    /**
     * Get student's schedule for a specific day
     *
     * @param studentId Student ID
     * @param dayOfWeek Day of week
     * @return List of schedules for that day
     */
    List<PullOutSchedule> getStudentScheduleForDay(Long studentId, String dayOfWeek);

    /**
     * Get staff's schedule for a specific day
     *
     * @param staffId Staff ID
     * @param dayOfWeek Day of week
     * @return List of schedules for that day
     */
    List<PullOutSchedule> getStaffScheduleForDay(Long staffId, String dayOfWeek);

    /**
     * Get all active schedules for a specific IEP service
     *
     * @param serviceId Service ID
     * @return List of schedules
     */
    List<PullOutSchedule> getSchedulesForService(Long serviceId);

    /**
     * Get all group sessions
     *
     * @return List of group sessions
     */
    List<PullOutSchedule> getAllGroupSessions();

    /**
     * Get all active schedules
     *
     * @return List of all active schedules
     */
    List<PullOutSchedule> findAllActiveSchedules();

    // ========== AVAILABILITY FINDING ==========

    /**
     * Find available time slots for a student and staff member
     *
     * @param studentId Student ID
     * @param staffId Staff ID
     * @param durationMinutes Session duration
     * @return Map of day -> list of available time slots
     */
    Map<String, List<TimeSlot>> findAvailableTimeSlots(Long studentId, Long staffId, int durationMinutes);

    /**
     * Find best time slot for a service (considers least conflicts, optimal times)
     *
     * @param serviceId Service ID
     * @return Optimal time slot or empty if none found
     */
    Optional<TimeSlot> findBestTimeSlot(Long serviceId);

    // ========== STATISTICS ==========

    /**
     * Calculate total pull-out minutes per week for a student
     *
     * @param studentId Student ID
     * @return Total minutes
     */
    int getTotalMinutesPerWeek(Long studentId);

    /**
     * Calculate staff workload (total minutes per week)
     *
     * @param staffId Staff ID
     * @return Total minutes
     */
    int getStaffWorkload(Long staffId);

    /**
     * Count active schedules for a student
     *
     * @param studentId Student ID
     * @return Number of active schedules
     */
    long countStudentSchedules(Long studentId);

    /**
     * Count active schedules for a staff member
     *
     * @param staffId Staff ID
     * @return Number of active schedules
     */
    long countStaffSchedules(Long staffId);

    // ========== VALIDATION ==========

    /**
     * Validate schedule data before saving
     *
     * @param schedule Schedule to validate
     * @return List of validation errors (empty if valid)
     */
    List<String> validateSchedule(PullOutSchedule schedule);

    /**
     * Check if a schedule can be created without conflicts
     *
     * @param schedule Schedule to check
     * @return true if schedule can be created
     */
    boolean canSchedule(PullOutSchedule schedule);

    // ========== HELPER CLASSES ==========

    /**
     * Represents a time slot option
     */
    class TimeSlot {
        public String dayOfWeek;
        public LocalTime startTime;
        public LocalTime endTime;
        public int durationMinutes;
        public double score; // Quality score (higher is better)

        public TimeSlot(String dayOfWeek, LocalTime startTime, LocalTime endTime, int durationMinutes) {
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationMinutes = durationMinutes;
            this.score = 0.0;
        }

        public TimeSlot(String dayOfWeek, LocalTime startTime, LocalTime endTime, int durationMinutes, double score) {
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationMinutes = durationMinutes;
            this.score = score;
        }
    }
}
