// Location: src/main/java/com/eduscheduler/controller/StudentController.java
package com.heronix.controller;

import com.heronix.dto.*;
import com.heronix.model.domain.*;
import com.heronix.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Student REST API Controller - COMPLETE IMPLEMENTATION
 * Location: src/main/java/com/eduscheduler/controller/StudentController.java
 *
 * Endpoints for Student Portal App:
 * - GET /api/students/{id} - Get student by ID (with DTO to avoid lazy loading)
 * - GET /api/students/{id}/dashboard - Get dashboard summary
 * - GET /api/students/{id}/grades - Get student's grades
 * - GET /api/students/{id}/attendance - Get attendance records
 * - GET /api/students/{id}/attendance/summary - Get attendance summary
 * - GET /api/students/{id}/assignments/upcoming - Get upcoming assignments
 * - GET /api/students/{id}/schedule - Get student's schedule
 *
 * Plus original CRUD endpoints for admin use
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0 - Complete Student Portal API
 * @since December 13, 2025
 */
@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentGradeRepository studentGradeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentGradeRepository assignmentGradeRepository;

    // ========================================================================
    // STUDENT PORTAL ENDPOINTS (Primary for Student App)
    // ========================================================================

    /**
     * GET /api/students/{id}
     * Get student by ID - Returns DTO to avoid lazy loading issues
     *
     * FIXED: Previously returned Student entity directly causing 500 errors
     * NOW: Returns StudentDTO with no lazy-loaded relationships
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/students/{id}/dashboard
     * Get student dashboard summary
     *
     * Returns comprehensive dashboard data including:
     * - Academic summary (GPA, credits, standing)
     * - Quick stats (courses, assignments, attendance)
     * - Alerts and notifications
     */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<StudentDashboardDTO> getStudentDashboard(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    StudentDashboardDTO dashboard = StudentDashboardDTO.builder()
                            .studentId(student.getId())
                            .studentName(student.getFullName())
                            .gradeLevel(student.getGradeLevel())
                            .photoPath(student.getPhotoPath())
                            .currentGPA(student.getCurrentGPA())
                            .unweightedGPA(student.getUnweightedGPA())
                            .weightedGPA(student.getWeightedGPA())
                            .academicStanding(student.getAcademicStanding())
                            .honorRollStatus(student.getHonorRollStatus())
                            .creditsEarned(student.getCreditsEarned())
                            .creditsRequired(student.getCreditsRequired())
                            .graduationStatus(student.getGraduationReadinessStatus())
                            .hasIEP(student.getHasIEP())
                            .has504Plan(student.getHas504Plan())
                            .build();

                    // Calculate course count
                    try {
                        dashboard.setTotalCourses(student.getScheduleSlots() != null ?
                                student.getScheduleSlots().size() : 0);
                    } catch (Exception e) {
                        dashboard.setTotalCourses(0);
                    }

                    // Calculate upcoming assignments count
                    LocalDate today = LocalDate.now();
                    LocalDate nextWeek = today.plusDays(7);

                    int upcomingCount = 0;
                    int missingCount = 0;

                    try {
                        // Count assignments across all enrolled courses
                        if (student.getEnrolledCourses() != null) {
                            for (Course course : student.getEnrolledCourses()) {
                                List<Assignment> upcoming = assignmentRepository.findUpcomingAssignments(
                                        course.getId(), today, nextWeek);
                                upcomingCount += upcoming.size();

                                // Count missing assignments
                                List<AssignmentGrade> grades = assignmentGradeRepository
                                        .findMissingByStudentAndCourse(student.getId(), course.getId());
                                missingCount += grades.size();
                            }
                        }
                    } catch (Exception e) {
                        // If lazy loading fails, leave at 0
                    }

                    dashboard.setUpcomingAssignmentsCount(upcomingCount);
                    dashboard.setMissingAssignmentsCount(missingCount);

                    // Calculate attendance summary
                    LocalDate startOfYear = LocalDate.of(today.getYear(), 8, 1);
                    if (today.getMonthValue() < 8) {
                        startOfYear = startOfYear.minusYears(1);
                    }

                    List<AttendanceRecord> records = attendanceRepository
                            .findByStudentIdAndAttendanceDateBetween(student.getId(), startOfYear, today);

                    long present = records.stream()
                            .filter(r -> "PRESENT".equals(r.getStatus().toString()))
                            .count();
                    long absent = records.stream()
                            .filter(r -> r.getStatus().toString().contains("ABSENT"))
                            .count();
                    long tardy = records.stream()
                            .filter(r -> "TARDY".equals(r.getStatus().toString()))
                            .count();

                    dashboard.setTotalDaysPresent((int) present);
                    dashboard.setTotalDaysAbsent((int) absent);
                    dashboard.setTotalDaysTardy((int) tardy);

                    int totalDays = (int) (present + absent);
                    if (totalDays > 0) {
                        dashboard.setAttendanceRate((present * 100.0) / totalDays);
                    } else {
                        dashboard.setAttendanceRate(100.0);
                    }

                    // Calculate average grade
                    List<StudentGrade> currentGrades = studentGradeRepository
                            .findByStudentIdAndTerm(student.getId(), getCurrentTerm());

                    if (!currentGrades.isEmpty()) {
                        double avgNumeric = currentGrades.stream()
                                .filter(g -> g.getNumericalGrade() != null)
                                .mapToDouble(StudentGrade::getNumericalGrade)
                                .average()
                                .orElse(0.0);

                        dashboard.setAverageNumericGrade(avgNumeric);
                        dashboard.setAverageLetterGrade(StudentGrade.numericalGradeToLetterGrade(avgNumeric));
                    }

                    // Set active alerts count
                    dashboard.setActiveAlertsCount(missingCount > 0 || absent > 5 ? 1 : 0);

                    return ResponseEntity.ok(dashboard);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/students/{id}/grades
     * Get all grades for a student
     *
     * Returns list of grades with course information
     * Sorted by most recent first
     */
    @GetMapping("/{id}/grades")
    public ResponseEntity<List<StudentGradeDTO>> getStudentGrades(@PathVariable Long id) {
        List<StudentGrade> grades = studentGradeRepository.findByStudentId(id);

        List<StudentGradeDTO> gradeDTOs = grades.stream()
                .map(this::convertGradeToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(gradeDTOs);
    }

    /**
     * GET /api/students/{id}/attendance
     * Get attendance records for a student
     *
     * Optional query parameters:
     * - startDate: Filter by start date (format: yyyy-MM-dd)
     * - endDate: Filter by end date (format: yyyy-MM-dd)
     *
     * If no dates provided, returns last 30 days
     */
    @GetMapping("/{id}/attendance")
    public ResponseEntity<List<StudentAttendanceDTO>> getStudentAttendance(
            @PathVariable Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        List<AttendanceRecord> records = attendanceRepository
                .findByStudentIdAndAttendanceDateBetween(id, start, end);

        List<StudentAttendanceDTO> attendanceDTOs = records.stream()
                .map(this::convertAttendanceToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(attendanceDTOs);
    }

    /**
     * GET /api/students/{id}/attendance/summary
     * Get attendance summary statistics
     *
     * Returns aggregated attendance data for the current academic year
     */
    @GetMapping("/{id}/attendance/summary")
    public ResponseEntity<AttendanceSummaryDTO> getAttendanceSummary(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    // Get current academic year dates
                    LocalDate today = LocalDate.now();
                    LocalDate startOfYear = LocalDate.of(today.getYear(), 8, 1);
                    if (today.getMonthValue() < 8) {
                        startOfYear = startOfYear.minusYears(1);
                    }

                    List<AttendanceRecord> records = attendanceRepository
                            .findByStudentIdAndAttendanceDateBetween(student.getId(), startOfYear, today);

                    // Calculate statistics
                    long totalDays = records.stream()
                            .map(AttendanceRecord::getAttendanceDate)
                            .distinct()
                            .count();

                    long present = records.stream()
                            .filter(r -> "PRESENT".equals(r.getStatus().toString()))
                            .count();

                    long absent = records.stream()
                            .filter(r -> r.getStatus().toString().contains("ABSENT"))
                            .count();

                    long excused = records.stream()
                            .filter(r -> "EXCUSED_ABSENT".equals(r.getStatus().toString()))
                            .count();

                    long unexcused = records.stream()
                            .filter(r -> "UNEXCUSED_ABSENT".equals(r.getStatus().toString()))
                            .count();

                    long tardy = records.stream()
                            .filter(r -> "TARDY".equals(r.getStatus().toString()))
                            .count();

                    // Calculate percentages
                    double attendanceRate = totalDays > 0 ? (present * 100.0) / totalDays : 100.0;
                    double absenceRate = totalDays > 0 ? (absent * 100.0) / totalDays : 0.0;
                    double tardyRate = totalDays > 0 ? (tardy * 100.0) / totalDays : 0.0;

                    // Determine alert level
                    String alertLevel = "NONE";
                    if (absenceRate >= 15) {
                        alertLevel = "CRITICAL";
                    } else if (absenceRate >= 10 || tardy >= 10) {
                        alertLevel = "WARNING";
                    }

                    AttendanceSummaryDTO summary = AttendanceSummaryDTO.builder()
                            .studentId(student.getId())
                            .studentName(student.getFullName())
                            .totalDays((int) totalDays)
                            .daysPresent((int) present)
                            .daysAbsent((int) absent)
                            .daysExcused((int) excused)
                            .daysUnexcused((int) unexcused)
                            .daysTardy((int) tardy)
                            .attendanceRate(attendanceRate)
                            .absenceRate(absenceRate)
                            .tardyRate(tardyRate)
                            .totalPeriods(records.size())
                            .periodsPresent((int) present)
                            .periodsAbsent((int) absent)
                            .hasChronicAbsence(absenceRate >= 10)
                            .hasExcessiveTardies(tardy >= 5)
                            .alertLevel(alertLevel)
                            .build();

                    return ResponseEntity.ok(summary);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/students/{id}/assignments/upcoming
     * Get upcoming assignments for a student
     *
     * Returns assignments due in the next 14 days across all enrolled courses
     * Sorted by due date (soonest first)
     */
    @GetMapping("/{id}/assignments/upcoming")
    public ResponseEntity<List<StudentAssignmentDTO>> getUpcomingAssignments(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    List<StudentAssignmentDTO> upcomingAssignments = new ArrayList<>();

                    LocalDate today = LocalDate.now();
                    LocalDate twoWeeksOut = today.plusDays(14);

                    try {
                        // Get upcoming assignments from all enrolled courses
                        if (student.getEnrolledCourses() != null) {
                            for (Course course : student.getEnrolledCourses()) {
                                List<Assignment> assignments = assignmentRepository
                                        .findUpcomingAssignments(course.getId(), today, twoWeeksOut);

                                for (Assignment assignment : assignments) {
                                    StudentAssignmentDTO dto = convertAssignmentToDTO(assignment, student, course);
                                    upcomingAssignments.add(dto);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // If lazy loading fails, return empty list
                    }

                    // Sort by due date (soonest first)
                    upcomingAssignments.sort((a, b) -> a.getDueDate().compareTo(b.getDueDate()));

                    return ResponseEntity.ok(upcomingAssignments);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/students/{id}/schedule
     * Get student's schedule
     *
     * FIXED: Previously caused 500 error due to lazy loading
     * NOW: Returns DTO with all necessary information pre-loaded
     */
    @GetMapping("/{id}/schedule")
    public ResponseEntity<StudentScheduleDTO> getStudentSchedule(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    List<StudentScheduleDTO.ScheduleSlotDTO> slots = new ArrayList<>();

                    try {
                        if (student.getScheduleSlots() != null) {
                            for (ScheduleSlot slot : student.getScheduleSlots()) {
                                StudentScheduleDTO.ScheduleSlotDTO slotDTO = StudentScheduleDTO.ScheduleSlotDTO.builder()
                                        .slotId(slot.getId())
                                        .dayOfWeek(slot.getDayOfWeek())
                                        .startTime(slot.getStartTime())
                                        .endTime(slot.getEndTime())
                                        .periodNumber(slot.getPeriodNumber())
                                        .isLunchPeriod(slot.getIsLunchPeriod())
                                        .dayType(slot.getDayType() != null ? slot.getDayType().toString() : "DAILY")
                                        .build();

                                // Safely get course info
                                try {
                                    if (slot.getCourse() != null) {
                                        slotDTO.setCourseName(slot.getCourse().getCourseName());
                                        slotDTO.setCourseCode(slot.getCourse().getCourseCode());
                                    }
                                } catch (Exception e) {
                                    // Skip if lazy loading fails
                                }

                                // Safely get teacher info
                                try {
                                    if (slot.getTeacher() != null) {
                                        String teacherName = slot.getTeacher().getName();
                                        if (teacherName == null || teacherName.trim().isEmpty()) {
                                            // Try firstName + lastName
                                            String firstName = slot.getTeacher().getFirstName();
                                            String lastName = slot.getTeacher().getLastName();
                                            if (firstName != null && lastName != null) {
                                                teacherName = firstName + " " + lastName;
                                            }
                                        }
                                        slotDTO.setTeacherName(teacherName);
                                    }
                                } catch (Exception e) {
                                    // Skip if lazy loading fails
                                }

                                // Safely get room info
                                try {
                                    if (slot.getRoom() != null) {
                                        slotDTO.setRoomNumber(slot.getRoom().getRoomNumber());
                                        // Room entity doesn't have roomName field, use roomNumber
                                        slotDTO.setRoomName(slot.getRoom().getRoomNumber());
                                    }
                                } catch (Exception e) {
                                    // Skip if lazy loading fails
                                }

                                slots.add(slotDTO);
                            }
                        }
                    } catch (Exception e) {
                        // If entire schedule loading fails, return empty schedule
                    }

                    // Sort by day and time
                    slots.sort((a, b) -> {
                        if (a.getDayOfWeek() != b.getDayOfWeek()) {
                            return a.getDayOfWeek().compareTo(b.getDayOfWeek());
                        }
                        if (a.getStartTime() != null && b.getStartTime() != null) {
                            return a.getStartTime().compareTo(b.getStartTime());
                        }
                        return 0;
                    });

                    StudentScheduleDTO schedule = StudentScheduleDTO.builder()
                            .studentId(student.getId())
                            .studentName(student.getFullName())
                            .gradeLevel(student.getGradeLevel())
                            .scheduleSlots(slots)
                            .build();

                    return ResponseEntity.ok(schedule);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========================================================================
    // ORIGINAL CRUD ENDPOINTS (For Admin/Teacher use)
    // ========================================================================

    /**
     * GET /api/students
     * Get all students
     */
    @GetMapping
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /**
     * POST /api/students
     * Create new student
     */
    @PostMapping
    public ResponseEntity<Student> createStudent(@RequestBody Student student) {
        // Ensure new student is active
        student.setActive(true);

        Student saved = studentRepository.save(student);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * PUT /api/students/{id}
     * Update existing student
     */
    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(
            @PathVariable Long id,
            @RequestBody Student studentDetails) {

        return studentRepository.findById(id)
                .map(student -> {
                    student.setFirstName(studentDetails.getFirstName());
                    student.setLastName(studentDetails.getLastName());
                    student.setStudentId(studentDetails.getStudentId());
                    student.setEmail(studentDetails.getEmail());
                    student.setGradeLevel(studentDetails.getGradeLevel());
                    student.setActive(studentDetails.getActive());

                    Student updated = studentRepository.save(student);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/students/{id}
     * Soft delete student (sets active = false)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    student.setActive(false);
                    studentRepository.save(student);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/students/active
     * Get all active students
     */
    @GetMapping("/active")
    public List<Student> getActiveStudents() {
        return studentRepository.findByActiveTrue();
    }

    /**
     * GET /api/students/student-id/{studentId}
     * Get student by student ID
     */
    @GetMapping("/student-id/{studentId}")
    public ResponseEntity<Student> getStudentByStudentId(@PathVariable String studentId) {
        return studentRepository.findByStudentId(studentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/students/grade/{gradeLevel}
     * Get students by grade level
     */
    @GetMapping("/grade/{gradeLevel}")
    public List<Student> getStudentsByGradeLevel(@PathVariable String gradeLevel) {
        return studentRepository.findByGradeLevel(gradeLevel);
    }

    /**
     * PATCH /api/students/{id}/activate
     * Activate student
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Student> activateStudent(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    student.setActive(true);
                    return ResponseEntity.ok(studentRepository.save(student));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PATCH /api/students/{id}/deactivate
     * Deactivate student
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Student> deactivateStudent(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    student.setActive(false);
                    return ResponseEntity.ok(studentRepository.save(student));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========================================================================
    // HELPER METHODS - DTO Conversion
    // ========================================================================

    /**
     * Convert Student entity to DTO
     * Avoids lazy loading issues by only accessing eagerly loaded fields
     *
     * Updated: December 14, 2025 - Using builder pattern for new fields
     */
    private StudentDTO convertToDTO(Student student) {
        return StudentDTO.builder()
                // Basic Information
                .id(student.getId())
                .studentId(student.getStudentId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .gradeLevel(student.getGradeLevel())
                .active(student.isActive())
                .photoPath(student.getPhotoPath())

                // Academic
                .currentGPA(student.getCurrentGPA())
                .unweightedGPA(student.getUnweightedGPA())
                .weightedGPA(student.getWeightedGPA())
                .creditsEarned(student.getCreditsEarned())
                .creditsRequired(student.getCreditsRequired())
                .academicStanding(student.getAcademicStanding())
                .honorRollStatus(student.getHonorRollStatus())
                .graduationYear(student.getGraduationYear())
                .graduated(student.getGraduated())

                // Special Education
                .hasIEP(student.getHasIEP())
                .has504Plan(student.getHas504Plan())
                .isGifted(student.getIsGifted())
                .behaviorScore(student.getBehaviorScore())
                .medicalConditions(student.getMedicalConditions())

                // Emergency Contact (deprecated - keeping for backward compatibility)
                .emergencyContact(student.getEmergencyContact())
                .emergencyPhone(student.getEmergencyPhone())

                // Demographics (NEW - Phase 2)
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .ethnicity(student.getEthnicity())
                .race(student.getRace())
                .preferredLanguage(student.getPreferredLanguage())
                .isEnglishLearner(student.getIsEnglishLearner())
                .birthCity(student.getBirthCity())
                .birthState(student.getBirthState())
                .birthCountry(student.getBirthCountry())

                // Home Address (NEW - Phase 2)
                .homeStreetAddress(student.getHomeStreetAddress())
                .homeCity(student.getHomeCity())
                .homeState(student.getHomeState())
                .homeZipCode(student.getHomeZipCode())
                .county(student.getHomeCounty())
                .mailingAddress(student.getMailingStreetAddress())
                .mailingCity(student.getMailingCity())
                .mailingState(student.getMailingState())
                .mailingZipCode(student.getMailingZipCode())

                // Special Circumstances (NEW - Phase 2)
                .isFosterCare(student.getIsFosterCare())
                .fosterCareAgency(student.getFosterCareAgency())
                .fosterCaseworker(student.getFosterCaseWorkerName())
                .isHomeless(student.getIsHomeless())
                .homelessShelter(student.getHomelessSituationType())
                .isOrphan(student.getIsOrphan())
                .isWardOfCourt(student.getIsWardOfCourt())
                .isRefugeeAsylee(student.getIsRefugeeAsylee())
                .isUnaccompaniedYouth(student.getIsUnaccompaniedYouth())
                .isMilitaryFamily(student.getIsMilitaryFamily())
                .isMigrantFamily(student.getIsMigrantFamily())
                .custodyArrangement(student.getCustodyArrangement())
                // TODO Phase 2: .livesWith() - field not added yet in Student entity

                // Previous School (NEW - Phase 2)
                .previousSchoolName(student.getPreviousSchoolName())
                // TODO Phase 2: Use actual field names from Student entity (previousSchoolAddress, etc.)

                // Immunization & Health Insurance (NEW - Phase 2)
                .immunizationComplete(student.getImmunizationComplete())
                .immunizationExemptionType(student.getImmunizationExemptionType())
                .physicalExamDate(student.getPhysicalExamDate())
                .physicianName(student.getPhysicianName())
                .physicianPhone(student.getPhysicianPhone())
                .dentistName(student.getDentistName())
                .dentistPhone(student.getDentistPhone())
                .healthInsuranceProvider(student.getHealthInsuranceProvider())
                .insurancePolicyNumber(student.getHealthInsurancePolicyNumber())
                .insuranceGroupNumber(student.getHealthInsuranceGroupNumber())
                // TODO Phase 2: .insuranceSubscriberName() - field not added yet

                // Enrollment Documentation (NEW - Phase 2)
                .birthCertificateVerified(student.getBirthCertificateVerified())
                .birthCertificateDate(student.getBirthCertificateVerifiedDate())
                .residencyVerified(student.getResidencyVerified())
                .residencyVerificationDate(student.getResidencyVerifiedDate())
                // TODO Phase 2: Additional enrollment fields not added yet in Student entity
                .previousSchoolRecordsReceived(student.getPreviousSchoolRecordsReceived())
                .recordsReceivedDate(student.getPreviousSchoolRecordsDate())

                // Audit Fields (NEW - Phase 2)
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                // TODO Phase 2: .createdBy() and .modifiedBy() - fields not added yet

                // Note: parents and emergencyContacts lists NOT included here
                // to avoid lazy loading issues. Use dedicated endpoints to fetch these.
                .build();
    }

    /**
     * Convert StudentGrade entity to DTO
     */
    private StudentGradeDTO convertGradeToDTO(StudentGrade grade) {
        String teacherName = null;
        try {
            if (grade.getTeacher() != null) {
                teacherName = grade.getTeacher().getName();
                if (teacherName == null || teacherName.trim().isEmpty()) {
                    String firstName = grade.getTeacher().getFirstName();
                    String lastName = grade.getTeacher().getLastName();
                    if (firstName != null && lastName != null) {
                        teacherName = firstName + " " + lastName;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore lazy loading errors
        }

        return StudentGradeDTO.builder()
                .gradeId(grade.getId())
                .courseId(grade.getCourse() != null ? grade.getCourse().getId() : null)
                .courseName(grade.getCourseName())
                .courseCode(grade.getCourse() != null ? grade.getCourse().getCourseCode() : null)
                .term(grade.getTerm())
                .academicYear(grade.getAcademicYear())
                .letterGrade(grade.getLetterGrade())
                .numericalGrade(grade.getNumericalGrade())
                .gpaPoints(grade.getGpaPoints())
                .credits(grade.getCredits())
                .isWeighted(grade.getIsWeighted())
                .gradeType(grade.getGradeType())
                .gradeDate(grade.getGradeDate())
                .teacherName(teacherName)
                .comments(grade.getComments())
                .effortGrade(grade.getEffortGrade())
                .conductGrade(grade.getConductGrade())
                .absences(grade.getAbsences())
                .tardies(grade.getTardies())
                .isFinal(grade.getIsFinal())
                .build();
    }

    /**
     * Convert AttendanceRecord entity to DTO
     */
    private StudentAttendanceDTO convertAttendanceToDTO(AttendanceRecord record) {
        return StudentAttendanceDTO.builder()
                .recordId(record.getId())
                .attendanceDate(record.getAttendanceDate())
                .checkInTime(record.getArrivalTime()) // Use arrivalTime instead of checkInTime
                .status(record.getStatus() != null ? record.getStatus().toString() : "UNKNOWN")
                .courseName(record.getCourse() != null ? record.getCourse().getCourseName() : null)
                .periodNumber(record.getPeriodNumber() != null ? record.getPeriodNumber().toString() : null)
                .notes(record.getNotes())
                .verified(record.getVerified())
                .recordedBy(record.getRecordedBy())
                .build();
    }

    /**
     * Convert Assignment to StudentAssignmentDTO
     */
    private StudentAssignmentDTO convertAssignmentToDTO(Assignment assignment, Student student, Course course) {
        StudentAssignmentDTO dto = StudentAssignmentDTO.builder()
                .assignmentId(assignment.getId())
                .assignmentName(assignment.getTitle()) // Use 'title' not 'assignmentName'
                .description(assignment.getDescription())
                .courseName(course.getCourseName())
                .courseCode(course.getCourseCode())
                .dueDate(assignment.getDueDate())
                .assignedDate(assignment.getAssignedDate())
                .category(assignment.getCategory() != null ? assignment.getCategory().getName() : null) // Use 'name' not 'categoryName'
                .maxPoints(assignment.getMaxPoints() != null ? assignment.getMaxPoints().intValue() : 100)
                .isPublished(assignment.getPublished())
                .build();

        // Check student's grade for this assignment
        assignmentGradeRepository.findByStudentIdAndAssignmentId(student.getId(), assignment.getId())
                .ifPresent(grade -> {
                    dto.setStudentScore(grade.getScore());
                    dto.setStatus(grade.getStatus() != null ? grade.getStatus().toString() : "NOT_STARTED");
                    dto.setIsGraded(grade.getScore() != null);
                    dto.setIsMissing("MISSING".equals(grade.getStatus() != null ? grade.getStatus().toString() : ""));
                    dto.setIsExcused(grade.getExcused());
                    dto.setTeacherComments(grade.getComments());
                });

        // Calculate time indicators
        if (assignment.getDueDate() != null) {
            LocalDate today = LocalDate.now();
            long daysUntil = ChronoUnit.DAYS.between(today, assignment.getDueDate());
            dto.setDaysUntilDue((int) daysUntil);
            dto.setOverdue(daysUntil < 0); // Use 'setOverdue' not 'setIsOverdue'
            dto.setDueSoon(daysUntil >= 0 && daysUntil <= 3); // Use 'setDueSoon' not 'setIsDueSoon'
        }

        return dto;
    }

    /**
     * Get current academic term
     * TODO: Replace with actual term lookup from database
     */
    private String getCurrentTerm() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        if (month >= 8 && month <= 12) {
            return "Fall " + year;
        } else if (month >= 1 && month <= 5) {
            return "Spring " + year;
        } else {
            return "Summer " + year;
        }
    }
}
