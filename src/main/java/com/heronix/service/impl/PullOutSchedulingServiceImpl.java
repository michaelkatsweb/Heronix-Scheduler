package com.heronix.service.impl;

import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.PullOutSchedule;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;
import com.heronix.model.enums.PullOutStatus;
import com.heronix.model.enums.ServiceStatus;
import com.heronix.repository.IEPServiceRepository;
import com.heronix.repository.PullOutScheduleRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.PullOutSchedulingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pull-Out Scheduling Service Implementation
 *
 * Implements business logic for scheduling pull-out service sessions.
 * This is a simplified implementation focusing on core functionality.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8B - November 21, 2025
 */
@Service
@Slf4j
@Transactional
public class PullOutSchedulingServiceImpl implements PullOutSchedulingService {

    @Autowired
    private PullOutScheduleRepository pullOutScheduleRepository;

    @Autowired
    private IEPServiceRepository iepServiceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    private static final LocalTime SCHOOL_START = LocalTime.of(8, 0);
    private static final LocalTime SCHOOL_END = LocalTime.of(15, 30);
    private static final String[] DAYS_OF_WEEK = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};

    // ========== CRUD OPERATIONS ==========

    @Override
    public PullOutSchedule createSchedule(PullOutSchedule schedule) {
        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction of student ID for logging
        String studentInfo = (schedule.getStudent() != null && schedule.getStudent().getId() != null)
            ? schedule.getStudent().getId().toString() : "Unknown";
        log.info("Creating new pull-out schedule for student ID: {}", studentInfo);

        // Validate before creating
        List<String> errors = validateSchedule(schedule);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Schedule validation failed: " + String.join(", ", errors));
        }

        // Check for conflicts
        if (!canSchedule(schedule)) {
            throw new IllegalStateException("Schedule conflicts detected. Please choose a different time.");
        }

        // Set default values
        if (schedule.getStatus() == null) {
            schedule.setStatus(PullOutStatus.ACTIVE);
        }
        if (schedule.getRecurring() == null) {
            schedule.setRecurring(true);
        }
        if (schedule.getStartDate() == null) {
            schedule.setStartDate(LocalDate.now());
        }

        PullOutSchedule saved = pullOutScheduleRepository.save(schedule);

        // Update service status
        updateServiceStatus(schedule.getIepService());

        log.info("Created pull-out schedule with ID: {}", saved.getId());
        return saved;
    }

    @Override
    public PullOutSchedule updateSchedule(PullOutSchedule schedule) {
        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction of schedule ID for logging
        String scheduleIdStr = (schedule.getId() != null) ? schedule.getId().toString() : "null";
        log.info("Updating pull-out schedule ID: {}", scheduleIdStr);

        if (schedule.getId() == null) {
            throw new IllegalArgumentException("Schedule ID cannot be null for update");
        }

        if (!pullOutScheduleRepository.existsById(schedule.getId())) {
            throw new IllegalArgumentException("Schedule not found with ID: " + schedule.getId());
        }

        // Validate before updating
        List<String> errors = validateSchedule(schedule);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Schedule validation failed: " + String.join(", ", errors));
        }

        PullOutSchedule updated = pullOutScheduleRepository.save(schedule);
        log.info("Updated pull-out schedule ID: {}", updated.getId());
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PullOutSchedule> findById(Long id) {
        return pullOutScheduleRepository.findById(id);
    }

    @Override
    public void deleteSchedule(Long id) {
        log.info("Deleting pull-out schedule ID: {}", id);

        PullOutSchedule schedule = pullOutScheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found with ID: " + id));

        pullOutScheduleRepository.delete(schedule);

        // Update service status
        updateServiceStatus(schedule.getIepService());

        log.info("Deleted pull-out schedule ID: {}", id);
    }

    // ========== SCHEDULING OPERATIONS ==========

    @Override
    public PullOutSchedule scheduleService(Long serviceId) {
        // ✅ NULL SAFE: Validate serviceId parameter
        if (serviceId == null) {
            throw new IllegalArgumentException("Service ID cannot be null");
        }

        log.info("Scheduling service ID: {}", serviceId);

        IEPService service = iepServiceRepository.findById(serviceId)
            .orElseThrow(() -> new IllegalArgumentException("Service not found with ID: " + serviceId));

        // Validate service has required data for scheduling
        if (service.getStudent() == null) {
            throw new IllegalStateException("Service ID " + serviceId + " has no associated student");
        }
        if (service.getAssignedStaff() == null) {
            throw new IllegalStateException("Service ID " + serviceId + " has no assigned staff. Please assign a staff member before scheduling.");
        }

        // Find best time slot
        Optional<TimeSlot> bestSlot = findBestTimeSlot(serviceId);
        if (bestSlot.isEmpty()) {
            // ✅ NULL SAFE: Safe extraction of student/staff names
            String studentName = (service.getStudent().getFullName() != null) ?
                service.getStudent().getFullName() : "Unknown Student";
            String staffName = (service.getAssignedStaff().getName() != null) ?
                service.getAssignedStaff().getName() : "Unknown Staff";
            throw new IllegalStateException("No available time slots found for service ID: " + serviceId +
                ". All time slots may have conflicts for student '" + studentName +
                "' or staff '" + staffName + "'.");
        }

        TimeSlot slot = bestSlot.get();

        // Create schedule
        PullOutSchedule schedule = new PullOutSchedule();
        schedule.setIepService(service);
        schedule.setStudent(service.getStudent());
        schedule.setStaff(service.getAssignedStaff());
        schedule.setDayOfWeek(slot.dayOfWeek);
        schedule.setStartTime(slot.startTime);
        schedule.setEndTime(slot.endTime);
        schedule.setDurationMinutes(slot.durationMinutes);
        schedule.setRecurring(true);
        schedule.setStartDate(LocalDate.now());
        schedule.setStatus(PullOutStatus.ACTIVE);

        return createSchedule(schedule);
    }

    @Override
    public List<PullOutSchedule> scheduleAllServicesForStudent(Long studentId) {
        // ✅ NULL SAFE: Validate studentId parameter
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }

        log.info("Scheduling all services for student ID: {}", studentId);

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));

        // Use the correct student-based query
        List<IEPService> unscheduledServices = iepServiceRepository.findStudentServicesNeedingScheduling(studentId);
        List<PullOutSchedule> schedules = new ArrayList<>();

        for (IEPService service : unscheduledServices) {
            // ✅ NULL SAFE: Skip null services
            if (service == null) continue;

            try {
                // Skip services without assigned staff - they can't be auto-scheduled
                if (service.getAssignedStaff() == null) {
                    // ✅ NULL SAFE: Safe extraction of service ID
                    String serviceIdStr = (service.getId() != null) ? service.getId().toString() : "Unknown";
                    log.warn("Service ID {} has no assigned staff, skipping auto-schedule", serviceIdStr);
                    continue;
                }
                // ✅ NULL SAFE: Check service ID exists before scheduling
                if (service.getId() != null) {
                    PullOutSchedule schedule = scheduleService(service.getId());
                    schedules.add(schedule);
                }
            } catch (Exception e) {
                // ✅ NULL SAFE: Safe extraction of service ID
                String serviceIdStr = (service.getId() != null) ? service.getId().toString() : "Unknown";
                log.warn("Could not schedule service ID {}: {}", serviceIdStr, e.getMessage());
            }
        }

        log.info("Scheduled {} services for student ID: {}", schedules.size(), studentId);
        return schedules;
    }

    @Override
    public PullOutSchedule reschedule(Long scheduleId, String newDayOfWeek, LocalTime newStartTime, LocalTime newEndTime) {
        // ✅ NULL SAFE: Validate parameters
        if (scheduleId == null) {
            throw new IllegalArgumentException("Schedule ID cannot be null");
        }

        log.info("Rescheduling schedule ID {} to {} {}",  scheduleId, newDayOfWeek, newStartTime);

        PullOutSchedule schedule = pullOutScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found with ID: " + scheduleId));

        // ✅ NULL SAFE: Check student and staff exist with IDs
        if (schedule.getStudent() == null || schedule.getStudent().getId() == null) {
            throw new IllegalStateException("Schedule has no student or student ID");
        }
        if (schedule.getStaff() == null || schedule.getStaff().getId() == null) {
            throw new IllegalStateException("Schedule has no staff or staff ID");
        }

        // Check for conflicts at new time
        List<PullOutSchedule> studentConflicts = checkStudentConflicts(
            schedule.getStudent().getId(), newDayOfWeek, newStartTime, newEndTime);
        List<PullOutSchedule> staffConflicts = checkStaffConflicts(
            schedule.getStaff().getId(), newDayOfWeek, newStartTime, newEndTime);

        // Remove self from conflicts
        // ✅ NULL SAFE: Filter null schedules before checking ID
        studentConflicts.removeIf(s -> s != null && s.getId() != null && s.getId().equals(scheduleId));
        staffConflicts.removeIf(s -> s != null && s.getId() != null && s.getId().equals(scheduleId));

        if (!studentConflicts.isEmpty() || !staffConflicts.isEmpty()) {
            throw new IllegalStateException("Cannot reschedule: conflicts detected at new time");
        }

        schedule.setDayOfWeek(newDayOfWeek);
        schedule.setStartTime(newStartTime);
        schedule.setEndTime(newEndTime);
        schedule.setStatus(PullOutStatus.RESCHEDULED);

        return updateSchedule(schedule);
    }

    @Override
    public PullOutSchedule cancelSchedule(Long scheduleId) {
        log.info("Cancelling schedule ID: {}", scheduleId);

        PullOutSchedule schedule = pullOutScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found with ID: " + scheduleId));

        schedule.setStatus(PullOutStatus.CANCELLED);
        PullOutSchedule updated = pullOutScheduleRepository.save(schedule);

        // Update service status
        updateServiceStatus(schedule.getIepService());

        return updated;
    }

    // ========== CONFLICT DETECTION ==========

    @Override
    @Transactional(readOnly = true)
    public List<PullOutSchedule> checkStudentConflicts(Long studentId, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
        // ✅ NULL SAFE: Validate studentId parameter
        if (studentId == null) {
            return new ArrayList<>();
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));

        return pullOutScheduleRepository.findConflictingSchedulesForStudent(
            student, dayOfWeek, startTime, endTime, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PullOutSchedule> checkStaffConflicts(Long staffId, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
        // ✅ NULL SAFE: Validate staffId parameter
        if (staffId == null) {
            return new ArrayList<>();
        }

        Teacher staff = teacherRepository.findById(staffId)
            .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));

        return pullOutScheduleRepository.findConflictingSchedulesForStaff(
            staff, dayOfWeek, startTime, endTime, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTimeSlotAvailable(Long studentId, Long staffId, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
        // ✅ NULL SAFE: Validate parameters
        if (studentId == null || staffId == null) {
            return false;
        }

        List<PullOutSchedule> studentConflicts = checkStudentConflicts(studentId, dayOfWeek, startTime, endTime);
        List<PullOutSchedule> staffConflicts = checkStaffConflicts(staffId, dayOfWeek, startTime, endTime);

        return studentConflicts.isEmpty() && staffConflicts.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, PullOutSchedule>> findAllStudentConflicts(Long studentId) {
        // ✅ NULL SAFE: Validate studentId parameter
        if (studentId == null) {
            return new ArrayList<>();
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));

        List<PullOutSchedule> schedules = pullOutScheduleRepository.findActiveByStudent(student, LocalDate.now());
        List<Map<String, PullOutSchedule>> conflicts = new ArrayList<>();

        // Check each pair of schedules for conflicts
        for (int i = 0; i < schedules.size(); i++) {
            for (int j = i + 1; j < schedules.size(); j++) {
                PullOutSchedule s1 = schedules.get(i);
                PullOutSchedule s2 = schedules.get(j);

                // ✅ NULL SAFE: Skip if either schedule is null
                if (s1 == null || s2 == null) continue;

                if (schedulesConflict(s1, s2)) {
                    Map<String, PullOutSchedule> conflict = new HashMap<>();
                    conflict.put("schedule1", s1);
                    conflict.put("schedule2", s2);
                    conflicts.add(conflict);
                }
            }
        }

        return conflicts;
    }

    // ========== QUERIES ==========

    @Override
    @Transactional(readOnly = true)
    public List<PullOutSchedule> getStudentSchedule(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
        return pullOutScheduleRepository.findActiveByStudent(student, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PullOutSchedule> getStaffSchedule(Long staffId) {
        Teacher staff = teacherRepository.findById(staffId)
            .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));
        return pullOutScheduleRepository.findActiveByStaff(staff, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PullOutSchedule> getStudentScheduleForDay(Long studentId, String dayOfWeek) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
        return pullOutScheduleRepository.findByStudentAndDayOfWeek(student, dayOfWeek, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PullOutSchedule> getStaffScheduleForDay(Long staffId, String dayOfWeek) {
        Teacher staff = teacherRepository.findById(staffId)
            .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));
        return pullOutScheduleRepository.findByStaffAndDayOfWeek(staff, dayOfWeek, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PullOutSchedule> getSchedulesForService(Long serviceId) {
        IEPService service = iepServiceRepository.findById(serviceId)
            .orElseThrow(() -> new IllegalArgumentException("Service not found with ID: " + serviceId));
        return pullOutScheduleRepository.findByIepService(service);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PullOutSchedule> getAllGroupSessions() {
        return pullOutScheduleRepository.findGroupSessions(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PullOutSchedule> findAllActiveSchedules() {
        return pullOutScheduleRepository.findByStatus(PullOutStatus.ACTIVE);
    }

    // ========== AVAILABILITY FINDING ==========

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<TimeSlot>> findAvailableTimeSlots(Long studentId, Long staffId, int durationMinutes) {
        Map<String, List<TimeSlot>> availableSlots = new HashMap<>();

        // ✅ NULL SAFE: Validate parameters
        if (studentId == null || staffId == null) {
            return availableSlots;
        }

        for (String day : DAYS_OF_WEEK) {
            List<TimeSlot> daySlots = findAvailableTimeSlotsForDay(studentId, staffId, day, durationMinutes);
            if (!daySlots.isEmpty()) {
                availableSlots.put(day, daySlots);
            }
        }

        return availableSlots;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TimeSlot> findBestTimeSlot(Long serviceId) {
        // ✅ NULL SAFE: Validate serviceId parameter
        if (serviceId == null) {
            return Optional.empty();
        }

        IEPService service = iepServiceRepository.findById(serviceId)
            .orElseThrow(() -> new IllegalArgumentException("Service not found with ID: " + serviceId));

        // ✅ NULL SAFE: Check student/staff and their IDs exist
        if (service.getStudent() == null || service.getStudent().getId() == null ||
            service.getAssignedStaff() == null || service.getAssignedStaff().getId() == null) {
            return Optional.empty();
        }

        Map<String, List<TimeSlot>> availableSlots = findAvailableTimeSlots(
            service.getStudent().getId(),
            service.getAssignedStaff().getId(),
            service.getSessionDurationMinutes()
        );

        // Find slot with highest score across all days
        return availableSlots.values().stream()
                .flatMap(List::stream)
                .max(Comparator.comparingDouble(slot -> slot.score));
    }

    // ========== STATISTICS ==========

    @Override
    @Transactional(readOnly = true)
    public int getTotalMinutesPerWeek(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
        Long total = pullOutScheduleRepository.sumWeeklyMinutesForStudent(student, LocalDate.now());
        return total != null ? total.intValue() : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public int getStaffWorkload(Long staffId) {
        Teacher staff = teacherRepository.findById(staffId)
            .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));
        Long total = pullOutScheduleRepository.sumWeeklyMinutesForStaff(staff, LocalDate.now());
        return total != null ? total.intValue() : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countStudentSchedules(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
        return pullOutScheduleRepository.countActiveByStudent(student, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countStaffSchedules(Long staffId) {
        Teacher staff = teacherRepository.findById(staffId)
            .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));
        return pullOutScheduleRepository.countActiveByStaff(staff, LocalDate.now());
    }

    // ========== VALIDATION ==========

    @Override
    public List<String> validateSchedule(PullOutSchedule schedule) {
        List<String> errors = new ArrayList<>();

        if (schedule.getIepService() == null) {
            errors.add("IEP service is required");
        }

        if (schedule.getStudent() == null) {
            errors.add("Student is required");
        }

        if (schedule.getStaff() == null) {
            errors.add("Staff is required");
        }

        if (schedule.getDayOfWeek() == null || schedule.getDayOfWeek().trim().isEmpty()) {
            errors.add("Day of week is required");
        }

        if (schedule.getStartTime() == null) {
            errors.add("Start time is required");
        }

        if (schedule.getEndTime() == null) {
            errors.add("End time is required");
        }

        if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
            if (schedule.getEndTime().isBefore(schedule.getStartTime())) {
                errors.add("End time must be after start time");
            }
        }

        if (schedule.getStartDate() == null) {
            errors.add("Start date is required");
        }

        return errors;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canSchedule(PullOutSchedule schedule) {
        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null) {
            return false;
        }

        // Validate first
        if (!validateSchedule(schedule).isEmpty()) {
            return false;
        }

        // ✅ NULL SAFE: Check required objects and IDs exist
        if (schedule.getStudent() == null || schedule.getStudent().getId() == null ||
            schedule.getStaff() == null || schedule.getStaff().getId() == null) {
            return false;
        }

        // Check for conflicts
        return isTimeSlotAvailable(
            schedule.getStudent().getId(),
            schedule.getStaff().getId(),
            schedule.getDayOfWeek(),
            schedule.getStartTime(),
            schedule.getEndTime()
        );
    }

    // ========== PRIVATE HELPER METHODS ==========

    private boolean schedulesConflict(PullOutSchedule s1, PullOutSchedule s2) {
        // ✅ NULL SAFE: Validate both schedules and their properties
        if (s1 == null || s2 == null ||
            s1.getDayOfWeek() == null || s2.getDayOfWeek() == null ||
            s1.getStartTime() == null || s1.getEndTime() == null ||
            s2.getStartTime() == null || s2.getEndTime() == null) {
            return false;
        }

        // Different days = no conflict
        if (!s1.getDayOfWeek().equals(s2.getDayOfWeek())) {
            return false;
        }

        // Check time overlap
        return !(s1.getEndTime().isBefore(s2.getStartTime()) || s1.getStartTime().isAfter(s2.getEndTime()));
    }

    private List<TimeSlot> findAvailableTimeSlotsForDay(Long studentId, Long staffId, String day, int durationMinutes) {
        List<TimeSlot> availableSlots = new ArrayList<>();
        LocalTime currentTime = SCHOOL_START;

        while (currentTime.plusMinutes(durationMinutes).isBefore(SCHOOL_END)) {
            LocalTime endTime = currentTime.plusMinutes(durationMinutes);

            if (isTimeSlotAvailable(studentId, staffId, day, currentTime, endTime)) {
                double score = calculateTimeSlotScore(currentTime, durationMinutes);
                availableSlots.add(new TimeSlot(day, currentTime, endTime, durationMinutes, score));
            }

            currentTime = currentTime.plusMinutes(15); // 15-minute intervals
        }

        return availableSlots;
    }

    private double calculateTimeSlotScore(LocalTime time, int duration) {
        // Prefer morning times (higher score for earlier times)
        double timeScore = 1.0 - (time.getHour() - 8) / 7.0;

        // Prefer standard session lengths (30, 45, 60 minutes)
        double durationScore = (duration % 15 == 0) ? 1.0 : 0.5;

        return (timeScore + durationScore) / 2.0;
    }

    private void updateServiceStatus(IEPService service) {
        // ✅ NULL SAFE: Validate service parameter and ID
        if (service == null || service.getId() == null) {
            return;
        }

        // ✅ NULL SAFE: Filter null schedules before checking status
        List<PullOutSchedule> activeSchedules = getSchedulesForService(service.getId()).stream()
            .filter(s -> s != null && s.getStatus() == PullOutStatus.ACTIVE)
            .collect(Collectors.toList());

        if (activeSchedules.isEmpty()) {
            service.setStatus(ServiceStatus.NOT_SCHEDULED);
        } else if (service.meetsMinutesRequirement()) {
            service.setStatus(ServiceStatus.SCHEDULED);
        } else {
            service.setStatus(ServiceStatus.NOT_SCHEDULED);
        }

        iepServiceRepository.save(service);
    }
}
