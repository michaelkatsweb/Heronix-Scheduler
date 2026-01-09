package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.domain.AttendanceRecord.AttendanceStatus;
import com.heronix.repository.AttendanceRepository;
import com.heronix.repository.StudentRepository;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Attendance Service
 * Manages student attendance tracking and reporting.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 16 - Attendance System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;

    /**
     * Record attendance for a student in a class
     */
    @Transactional
    public AttendanceRecord recordAttendance(Long studentId, Long courseId,
            LocalDate date, AttendanceStatus status, String recordedBy) {

        // ✅ NULL SAFE: Validate parameters
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("Attendance date cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Attendance status cannot be null");
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        AttendanceRecord record = AttendanceRecord.builder()
            .student(student)
            .attendanceDate(date)
            .status(status)
            .recordedBy(recordedBy)
            .arrivalTime(status == AttendanceStatus.PRESENT ? LocalTime.now() : null)
            .build();

        return attendanceRepository.save(record);
    }

    /**
     * Bulk record attendance for a class
     */
    @Transactional
    public List<AttendanceRecord> recordClassAttendance(Long courseId, LocalDate date,
            Integer periodNumber, Map<Long, AttendanceStatus> studentStatuses, String recordedBy) {

        List<AttendanceRecord> records = new ArrayList<>();

        // ✅ NULL SAFE: Validate parameters
        if (date == null || studentStatuses == null) {
            log.warn("Cannot record class attendance with null date or studentStatuses");
            return records;
        }

        for (Map.Entry<Long, AttendanceStatus> entry : studentStatuses.entrySet()) {
            // ✅ NULL SAFE: Skip null entries or null keys/values
            if (entry == null || entry.getKey() == null || entry.getValue() == null) continue;

            Student student = studentRepository.findById(entry.getKey()).orElse(null);
            if (student != null) {
                AttendanceRecord record = AttendanceRecord.builder()
                    .student(student)
                    .attendanceDate(date)
                    .periodNumber(periodNumber)
                    .status(entry.getValue())
                    .recordedBy(recordedBy)
                    .build();
                records.add(attendanceRepository.save(record));
            }
        }

        // ✅ NULL SAFE: Safe extraction of courseId
        String courseIdStr = (courseId != null) ? courseId.toString() : "Unknown";
        log.info("Recorded attendance for {} students in course {} on {}",
            records.size(), courseIdStr, date);

        return records;
    }

    /**
     * Get student attendance summary
     */
    public AttendanceSummary getStudentAttendanceSummary(Long studentId,
            LocalDate startDate, LocalDate endDate) {

        // ✅ NULL SAFE: Validate parameters
        if (studentId == null || startDate == null || endDate == null) {
            return AttendanceSummary.builder()
                .studentId(studentId)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        }

        List<AttendanceRecord> records = attendanceRepository
            .findByStudentIdAndAttendanceDateBetween(studentId, startDate, endDate);

        // ✅ NULL SAFE: Validate records list
        if (records == null) {
            records = new ArrayList<>();
        }

        long totalRecords = records.size();
        // ✅ NULL SAFE: Filter null records and check status
        long presentCount = records.stream()
            .filter(r -> r != null && r.getStatus() == AttendanceStatus.PRESENT)
            .count();
        long absentCount = records.stream()
            .filter(r -> r != null)
            .filter(AttendanceRecord::isAbsent)
            .count();
        long tardyCount = records.stream()
            .filter(r -> r != null && r.getStatus() == AttendanceStatus.TARDY)
            .count();

        double attendanceRate = totalRecords > 0 ?
            (double) presentCount / totalRecords * 100 : 0;

        return AttendanceSummary.builder()
            .studentId(studentId)
            .startDate(startDate)
            .endDate(endDate)
            .totalDays((int) totalRecords)
            .daysPresent((int) presentCount)
            .daysAbsent((int) absentCount)
            .daysTardy((int) tardyCount)
            .attendanceRate(attendanceRate)
            .isChronicAbsent(attendanceRate < 90)
            .build();
    }

    /**
     * Get students with chronic absences
     */
    public List<ChronicAbsenceAlert> getChronicAbsenceAlerts(LocalDate startDate,
            LocalDate endDate, int threshold) {

        List<Object[]> results = attendanceRepository.findStudentsWithChronicAbsences(
            startDate, endDate, threshold);

        List<ChronicAbsenceAlert> alerts = new ArrayList<>();

        // ✅ NULL SAFE: Validate results list
        if (results == null) {
            return alerts;
        }

        for (Object[] row : results) {
            // ✅ NULL SAFE: Skip null rows or validate array elements
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;

            Long studentId = (Long) row[0];
            Long absences = (Long) row[1];

            Student student = studentRepository.findById(studentId).orElse(null);
            if (student != null) {
                // ✅ NULL SAFE: Safe extraction of student names
                String firstName = (student.getFirstName() != null) ? student.getFirstName() : "";
                String lastName = (student.getLastName() != null) ? student.getLastName() : "";
                String studentName = (firstName + " " + lastName).trim();
                if (studentName.isEmpty()) studentName = "Unknown";

                alerts.add(ChronicAbsenceAlert.builder()
                    .studentId(studentId)
                    .studentName(studentName)
                    .gradeLevel(student.getGradeLevel())
                    .totalAbsences(absences.intValue())
                    .alertLevel(absences > threshold * 2 ? "CRITICAL" :
                               absences > threshold * 1.5 ? "HIGH" : "MODERATE")
                    .build());
            }
        }

        return alerts;
    }

    /**
     * Get daily attendance report for a campus
     */
    public DailyAttendanceReport getDailyReport(Long campusId, LocalDate date) {
        // ✅ NULL SAFE: Validate parameters
        if (date == null) {
            return DailyAttendanceReport.builder()
                .date(null)
                .campusId(campusId)
                .build();
        }

        List<AttendanceRecord> records = attendanceRepository
            .findByAttendanceDateAndCampusId(date, campusId);

        // ✅ NULL SAFE: Validate records list
        if (records == null) {
            records = new ArrayList<>();
        }

        // ✅ NULL SAFE: Filter null records before grouping
        Map<AttendanceStatus, Long> statusCounts = records.stream()
            .filter(r -> r != null && r.getStatus() != null)
            .collect(Collectors.groupingBy(AttendanceRecord::getStatus, Collectors.counting()));

        long total = records.size();
        long present = statusCounts.getOrDefault(AttendanceStatus.PRESENT, 0L);
        long absent = statusCounts.getOrDefault(AttendanceStatus.ABSENT, 0L) +
                      statusCounts.getOrDefault(AttendanceStatus.UNEXCUSED_ABSENT, 0L);

        return DailyAttendanceReport.builder()
            .date(date)
            .campusId(campusId)
            .totalStudents((int) total)
            .studentsPresent((int) present)
            .studentsAbsent((int) absent)
            .attendanceRate(total > 0 ? (double) present / total * 100 : 0)
            .statusBreakdown(statusCounts)
            .build();
    }

    // DTOs

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceSummary {
        private Long studentId;
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalDays;
        private int daysPresent;
        private int daysAbsent;
        private int daysTardy;
        private double attendanceRate;
        private boolean isChronicAbsent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChronicAbsenceAlert {
        private Long studentId;
        private String studentName;
        private String gradeLevel;
        private int totalAbsences;
        private String alertLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAttendanceReport {
        private LocalDate date;
        private Long campusId;
        private int totalStudents;
        private int studentsPresent;
        private int studentsAbsent;
        private double attendanceRate;
        private Map<AttendanceStatus, Long> statusBreakdown;
    }
}
