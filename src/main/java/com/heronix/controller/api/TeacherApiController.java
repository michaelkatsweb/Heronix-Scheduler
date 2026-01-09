package com.heronix.controller.api;

import com.heronix.api.dto.*;
import com.heronix.model.domain.*;
import com.heronix.repository.*;
import com.heronix.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Teacher Portal API Controller
 *
 * REST endpoints for EduPro-Teacher offline-first desktop application.
 * Handles bidirectional sync for grades, attendance, assignments, and students.
 *
 * Features:
 * - Teacher authentication
 * - Student data pull
 * - Assignment synchronization
 * - Grade batch submission
 * - Attendance batch submission
 * - Conflict detection
 * - Sync status tracking
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 8, 2025 - Teacher Portal Integration
 */
@Slf4j
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher Portal", description = "Teacher Portal API endpoints for EduPro-Teacher app")
public class TeacherApiController {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentGradeRepository assignmentGradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final ClassWalletRepository classWalletRepository;
    private final ClubRepository clubRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    // ========================================================================
    // AUTHENTICATION ENDPOINTS
    // ========================================================================

    @PostMapping("/auth/login")
    @Operation(summary = "Teacher authentication",
               description = "Authenticate teacher using employee ID and password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "403", description = "Account inactive or locked")
    })
    public ResponseEntity<AuthResponseDTO> authenticateTeacher(
            @Valid @RequestBody AuthRequestDTO request) {

        log.info("Teacher login attempt for employee ID: {}", request.getEmployeeId());

        try {
            // Find teacher by employee ID
            Optional<Teacher> teacherOpt = teacherRepository.findByEmployeeId(request.getEmployeeId());

            if (teacherOpt.isEmpty()) {
                log.warn("Teacher not found: {}", request.getEmployeeId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponseDTO.builder()
                        .success(false)
                        .message("Invalid employee ID or password")
                        .build());
            }

            Teacher teacher = teacherOpt.get();

            // Check if teacher is active
            if (!teacher.getActive()) {
                log.warn("Inactive teacher login attempt: {}", request.getEmployeeId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AuthResponseDTO.builder()
                        .success(false)
                        .message("Account is inactive. Please contact administrator.")
                        .build());
            }

            // Find associated user account
            Optional<User> userOpt = userRepository.findByUsername(teacher.getEmail());
            if (userOpt.isEmpty()) {
                log.warn("User account not found for teacher: {}", request.getEmployeeId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponseDTO.builder()
                        .success(false)
                        .message("Invalid employee ID or password")
                        .build());
            }

            User user = userOpt.get();

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("Invalid password for teacher: {}", request.getEmployeeId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponseDTO.builder()
                        .success(false)
                        .message("Invalid employee ID or password")
                        .build());
            }

            // Check if user is active (User uses 'enabled' not 'active')
            if (!user.getEnabled()) {
                log.warn("Inactive user login attempt: {}", request.getEmployeeId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AuthResponseDTO.builder()
                        .success(false)
                        .message("User account is inactive. Please contact administrator.")
                        .build());
            }

            // Successful authentication
            String fullName = teacher.getFirstName() + " " + teacher.getLastName();
            log.info("Successful teacher login: {} ({})", fullName, request.getEmployeeId());

            return ResponseEntity.ok(AuthResponseDTO.builder()
                .success(true)
                .message("Authentication successful")
                .teacherId(teacher.getId())
                .employeeId(teacher.getEmployeeId())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .email(teacher.getEmail())
                .department(teacher.getDepartment())
                .lastSync(LocalDateTime.now())
                .build());

        } catch (Exception e) {
            log.error("Error during teacher authentication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponseDTO.builder()
                    .success(false)
                    .message("Authentication service temporarily unavailable")
                    .build());
        }
    }

    @GetMapping("/auth/validate")
    @Operation(summary = "Validate teacher session",
               description = "Validate that teacher session is still active")
    public ResponseEntity<Map<String, Object>> validateSession(
            @RequestParam String employeeId) {

        Optional<Teacher> teacherOpt = teacherRepository.findByEmployeeId(employeeId);

        // Check if teacher exists and is active
        boolean isValidSession = teacherOpt
            .map(Teacher::getActive)
            .orElse(false);

        if (!isValidSession) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("valid", false, "message", "Session expired or invalid"));
        }

        return teacherOpt
            .map(teacher -> {
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("valid", true);
                responseMap.put("teacherId", teacher.getId());
                responseMap.put("employeeId", employeeId);
                return ResponseEntity.ok(responseMap);
            })
            .orElseGet(() -> {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("valid", false);
                errorMap.put("message", "Session expired or invalid");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMap);
            });
    }

    // ========================================================================
    // STUDENT DATA ENDPOINTS
    // ========================================================================

    @GetMapping("/students")
    @Operation(summary = "Get all students",
               description = "Retrieve all active students for teacher portal sync")
    public ResponseEntity<List<StudentDTO>> getAllStudents(
            @RequestParam(required = false) @Parameter(description = "Filter by teacher ID") Long teacherId,
            @RequestParam(required = false) @Parameter(description = "Last sync timestamp") LocalDateTime since) {

        log.info("Fetching students - teacherId: {}, since: {}", teacherId, since);

        try {
            List<Student> students;

            // TODO: Incremental sync - Student entity needs updatedAt field
            // For now, return all active students
            students = studentRepository.findByActiveTrue();
            log.info("Found {} active students", students.size());

            List<StudentDTO> studentDTOs = students.stream()
                .map(this::convertToStudentDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(studentDTOs);

        } catch (Exception e) {
            log.error("Error fetching students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/students/{studentId}")
    @Operation(summary = "Get student by ID",
               description = "Retrieve specific student details")
    public ResponseEntity<StudentDTO> getStudentById(
            @PathVariable Long studentId) {

        log.info("Fetching student ID: {}", studentId);

        return studentRepository.findById(studentId)
            .map(this::convertToStudentDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ========================================================================
    // ASSIGNMENT DATA ENDPOINTS
    // ========================================================================

    @GetMapping("/assignments")
    @Operation(summary = "Get all assignments",
               description = "Retrieve all assignments for teacher portal sync")
    public ResponseEntity<List<AssignmentDTO>> getAllAssignments(
            @RequestParam(required = false) @Parameter(description = "Filter by teacher ID") Long teacherId,
            @RequestParam(required = false) @Parameter(description = "Last sync timestamp") LocalDateTime since) {

        log.info("Fetching assignments - teacherId: {}, since: {}", teacherId, since);

        try {
            List<Assignment> assignments;

            if (teacherId != null) {
                // Filter by teacher ID (would need teacher relationship in Assignment entity)
                // For now, return all assignments
                assignments = assignmentRepository.findAll();
            } else if (since != null) {
                // Return only assignments modified since last sync
                assignments = assignmentRepository.findAll().stream()
                    .filter(a -> a.getUpdatedAt() != null && a.getUpdatedAt().isAfter(since))
                    .collect(Collectors.toList());

                log.info("Found {} assignments modified since {}", assignments.size(), since);
            } else {
                // Return all assignments
                assignments = assignmentRepository.findAll();
                log.info("Found {} assignments", assignments.size());
            }

            List<AssignmentDTO> assignmentDTOs = assignments.stream()
                .map(this::convertToAssignmentDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(assignmentDTOs);

        } catch (Exception e) {
            log.error("Error fetching assignments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========================================================================
    // GRADE SYNC ENDPOINTS (BATCH)
    // ========================================================================

    @PostMapping("/grades/batch")
    @Transactional
    @Operation(summary = "Batch submit grades",
               description = "Submit multiple grades from teacher portal in a single transaction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Grades submitted successfully"),
        @ApiResponse(responseCode = "409", description = "Conflict detected - grades modified on server"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<Map<String, Object>> submitGradesBatch(
            @RequestBody List<GradeDTO> grades) {

        log.info("Batch submitting {} grades", grades.size());

        try {
            List<ConflictDTO> conflicts = new ArrayList<>();
            int successCount = 0;
            int conflictCount = 0;
            int errorCount = 0;

            for (GradeDTO gradeDTO : grades) {
                try {
                    // Check for existing grade
                    Optional<AssignmentGrade> existingGradeOpt = assignmentGradeRepository.findById(gradeDTO.getId());

                    if (existingGradeOpt.isPresent()) {
                        AssignmentGrade existingGrade = existingGradeOpt.get();

                        // Check for conflicts (server version newer than client version)
                        if (existingGrade.getUpdatedAt() != null &&
                            gradeDTO.getLastModified() != null &&
                            existingGrade.getUpdatedAt().isAfter(gradeDTO.getLastModified())) {

                            // Conflict detected
                            log.warn("Conflict detected for grade ID: {}", gradeDTO.getId());
                            conflicts.add(ConflictDTO.builder()
                                .entityType("Grade")
                                .entityId(gradeDTO.getId())
                                .field("score")
                                .localValue(gradeDTO.getScore() != null ? gradeDTO.getScore().toString() : "null")
                                .serverValue(existingGrade.getScore() != null ? existingGrade.getScore().toString() : "null")
                                .serverTimestamp(existingGrade.getUpdatedAt())
                                .clientTimestamp(gradeDTO.getLastModified())
                                .build());
                            conflictCount++;
                            continue;
                        }

                        // No conflict, update grade
                        updateGradeFromDTO(existingGrade, gradeDTO);
                        assignmentGradeRepository.save(existingGrade);
                        successCount++;

                    } else {
                        // Create new grade
                        AssignmentGrade newGrade = convertToGradeEntity(gradeDTO);
                        assignmentGradeRepository.save(newGrade);
                        successCount++;
                    }

                } catch (Exception e) {
                    log.error("Error processing grade ID: {}", gradeDTO.getId(), e);
                    errorCount++;
                }
            }

            log.info("Batch grade submission complete - Success: {}, Conflicts: {}, Errors: {}",
                     successCount, conflictCount, errorCount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", conflictCount == 0 && errorCount == 0);
            response.put("totalSubmitted", grades.size());
            response.put("successCount", successCount);
            response.put("conflictCount", conflictCount);
            response.put("errorCount", errorCount);
            response.put("conflicts", conflicts);
            response.put("timestamp", LocalDateTime.now());

            if (conflictCount > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during batch grade submission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Internal server error during grade submission",
                    "message", e.getMessage()
                ));
        }
    }

    // ========================================================================
    // ATTENDANCE SYNC ENDPOINTS (BATCH)
    // ========================================================================

    @PostMapping("/attendance/batch")
    @Transactional
    @Operation(summary = "Batch submit attendance",
               description = "Submit multiple attendance records from teacher portal in a single transaction")
    public ResponseEntity<Map<String, Object>> submitAttendanceBatch(
            @RequestBody List<AttendanceDTO> attendanceRecords) {

        log.info("Batch submitting {} attendance records", attendanceRecords.size());

        try {
            List<ConflictDTO> conflicts = new ArrayList<>();
            int successCount = 0;
            int conflictCount = 0;
            int errorCount = 0;

            for (AttendanceDTO attendanceDTO : attendanceRecords) {
                try {
                    // Check for existing attendance record
                    if (attendanceDTO.getId() != null) {
                        Optional<AttendanceRecord> existingOpt = attendanceRepository.findById(attendanceDTO.getId());

                        if (existingOpt.isPresent()) {
                            AttendanceRecord existing = existingOpt.get();

                            // Conflict detection using timestamp (AttendanceRecord now has updatedAt field)
                            if (attendanceDTO.getLastModified() != null && existing.getUpdatedAt() != null) {
                                if (existing.getUpdatedAt().isAfter(attendanceDTO.getLastModified())) {
                                    log.warn("Attendance conflict detected for record {}: server version is newer",
                                        existing.getId());
                                    ConflictDTO conflict = ConflictDTO.builder()
                                        .entityType("attendance")
                                        .entityId(existing.getId())
                                        .description("Server record is newer than client version")
                                        .serverTimestamp(existing.getUpdatedAt())
                                        .localTimestamp(attendanceDTO.getLastModified())
                                        .build();
                                    conflicts.add(conflict);
                                    conflictCount++;
                                    continue;
                                }
                            }
                            updateAttendanceFromDTO(existing, attendanceDTO);
                            attendanceRepository.save(existing);
                            successCount++;
                        }
                    } else {
                        // Create new attendance record
                        AttendanceRecord newRecord = convertToAttendanceEntity(attendanceDTO);
                        attendanceRepository.save(newRecord);
                        successCount++;
                    }

                } catch (Exception e) {
                    log.error("Error processing attendance ID: {}", attendanceDTO.getId(), e);
                    errorCount++;
                }
            }

            log.info("Batch attendance submission complete - Success: {}, Conflicts: {}, Errors: {}",
                     successCount, conflictCount, errorCount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", conflictCount == 0 && errorCount == 0);
            response.put("totalSubmitted", attendanceRecords.size());
            response.put("successCount", successCount);
            response.put("conflictCount", conflictCount);
            response.put("errorCount", errorCount);
            response.put("conflicts", conflicts);
            response.put("timestamp", LocalDateTime.now());

            if (conflictCount > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during batch attendance submission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Internal server error during attendance submission",
                    "message", e.getMessage()
                ));
        }
    }

    // ========================================================================
    // SYNC STATUS ENDPOINTS
    // ========================================================================

    @GetMapping("/sync/status")
    @Operation(summary = "Get sync status",
               description = "Check server health and sync availability")
    public ResponseEntity<SyncStatusDTO> getSyncStatus(
            @RequestParam(required = false) String employeeId) {

        log.info("Sync status check - employeeId: {}", employeeId);

        return ResponseEntity.ok(SyncStatusDTO.builder()
            .serverOnline(true)
            .syncAvailable(true)
            .serverTime(LocalDateTime.now())
            .apiVersion("1.0")
            .message("Server is operational and ready for sync")
            .build());
    }

    @PostMapping("/sync/complete")
    @Operation(summary = "Mark sync complete",
               description = "Teacher portal notifies server that sync is complete")
    public ResponseEntity<Map<String, Object>> markSyncComplete(
            @RequestBody SyncCompleteRequestDTO request) {

        // Log sync completion (request only contains IDs)
        log.info("Sync completed - Grades synced: {}, Attendance synced: {}",
                 request.getGradeIds() != null ? request.getGradeIds().size() : 0,
                 request.getAttendanceIds() != null ? request.getAttendanceIds().size() : 0);

        return ResponseEntity.ok(Map.of(
            "acknowledged", true,
            "timestamp", LocalDateTime.now()
        ));
    }

    // ========================================================================
    // HEALTH CHECK
    // ========================================================================

    @GetMapping("/health")
    @Operation(summary = "Health check",
               description = "Simple health check endpoint")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Teacher Portal API",
            "timestamp", LocalDateTime.now()
        ));
    }

    // ========================================================================
    // HELPER METHODS - DTO CONVERSIONS
    // ========================================================================

    private StudentDTO convertToStudentDTO(Student student) {
        return StudentDTO.builder()
            .id(student.getId())
            .studentId(student.getStudentId())
            .firstName(student.getFirstName())
            .lastName(student.getLastName())
            .gradeLevel(student.getGradeLevel())
            .email(student.getEmail())
            .dateOfBirth(null)  // Student entity doesn't have dateOfBirth field
            .currentGpa(student.getCurrentGPA())  // Field is currentGPA (capital letters)
            .active(student.getActive())
            .hasIep(!student.getIeps().isEmpty())  // Has IEP if list is not empty
            .has504(student.getHas504Plan())  // Student uses has504Plan field
            .notes(null)  // Student doesn't have simple notes field
            // Use actual timestamp fields from Student entity
            .lastModified(student.getUpdatedAt())
            .createdDate(student.getCreatedAt())
            .build();
    }

    private AssignmentDTO convertToAssignmentDTO(Assignment assignment) {
        return AssignmentDTO.builder()
            .id(assignment.getId())
            .name(assignment.getTitle())  // AssignmentDTO uses 'name' field
            .category(assignment.getCategory() != null ? assignment.getCategory().toString() : null)  // Assignment uses 'category' enum
            .dueDate(assignment.getDueDate())
            .maxPoints(assignment.getMaxPoints())
            .description(assignment.getDescription())
            .courseId(assignment.getCourse() != null ? assignment.getCourse().getId() : null)
            .modifiedDate(assignment.getUpdatedAt())  // AssignmentDTO uses modifiedDate
            .createdDate(assignment.getCreatedAt())
            .build();
    }

    private void updateGradeFromDTO(AssignmentGrade grade, GradeDTO dto) {
        grade.setScore(dto.getScore());
        // Letter grade is calculated automatically from score
        grade.setComments(dto.getComments());
        grade.setGradedDate(dto.getGradedDate());
        grade.setUpdatedAt(LocalDateTime.now());
    }

    private AssignmentGrade convertToGradeEntity(GradeDTO dto) {
        AssignmentGrade grade = new AssignmentGrade();
        grade.setScore(dto.getScore());
        // Letter grade is calculated automatically from score
        grade.setComments(dto.getComments());
        grade.setGradedDate(dto.getGradedDate());

        // Set relationships (would need to fetch from repositories)
        if (dto.getStudentId() != null) {
            studentRepository.findById(dto.getStudentId()).ifPresent(grade::setStudent);
        }
        if (dto.getAssignmentId() != null) {
            assignmentRepository.findById(dto.getAssignmentId()).ifPresent(grade::setAssignment);
        }

        return grade;
    }

    private void updateAttendanceFromDTO(AttendanceRecord record, AttendanceDTO dto) {
        // Convert String status to enum
        if (dto.getStatus() != null) {
            record.setStatus(AttendanceRecord.AttendanceStatus.valueOf(dto.getStatus().toUpperCase().replace(" ", "_")));
        }
        record.setArrivalTime(dto.getArrivalTime());
        record.setDepartureTime(dto.getDepartureTime());
        record.setNotes(dto.getNotes());
        // Note: AttendanceRecord doesn't have setUpdatedAt() - it may use @PreUpdate
    }

    private AttendanceRecord convertToAttendanceEntity(AttendanceDTO dto) {
        AttendanceRecord record = new AttendanceRecord();
        // Convert String status to enum
        if (dto.getStatus() != null) {
            record.setStatus(AttendanceRecord.AttendanceStatus.valueOf(dto.getStatus().toUpperCase().replace(" ", "_")));
        }
        record.setAttendanceDate(dto.getDate());  // Use attendanceDate, not setDate
        record.setArrivalTime(dto.getArrivalTime());
        record.setDepartureTime(dto.getDepartureTime());
        record.setNotes(dto.getNotes());

        // Set relationships
        if (dto.getStudentId() != null) {
            studentRepository.findById(dto.getStudentId()).ifPresent(record::setStudent);
        }

        return record;
    }

    // ========================================================================
    // CLASS WALLET ENDPOINTS
    // ========================================================================

    @GetMapping("/wallet/transactions")
    @Operation(summary = "Get wallet transactions",
               description = "Get class wallet transactions for students, optionally filtered by student ID or date")
    public ResponseEntity<Map<String, Object>> getWalletTransactions(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) LocalDateTime since) {

        List<ClassWallet> transactions;

        if (studentId != null && since != null) {
            transactions = classWalletRepository.findByStudentIdAndUpdatedAtAfter(studentId, since);
        } else if (studentId != null) {
            transactions = classWalletRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        } else if (since != null) {
            transactions = classWalletRepository.findByUpdatedAtAfter(since);
        } else {
            transactions = classWalletRepository.findAll();
        }

        List<ClassWalletDTO> dtos = transactions.stream()
            .map(this::convertToClassWalletDTO)
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "transactions", dtos,
            "count", dtos.size(),
            "timestamp", LocalDateTime.now()
        ));
    }

    @PostMapping("/wallet/batch")
    @Transactional
    @Operation(summary = "Batch sync wallet transactions",
               description = "Submit class wallet transactions from Teacher Portal")
    public ResponseEntity<Map<String, Object>> syncWalletTransactions(
            @RequestBody Map<String, List<ClassWalletDTO>> request) {

        List<ClassWalletDTO> transactions = request.get("transactions");
        if (transactions == null || transactions.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", 0,
                "failed", 0,
                "message", "No transactions provided"
            ));
        }

        int successCount = 0;
        int failedCount = 0;

        for (ClassWalletDTO dto : transactions) {
            try {
                ClassWallet transaction = convertToClassWalletEntity(dto);
                classWalletRepository.save(transaction);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to save wallet transaction: {}", e.getMessage());
                failedCount++;
            }
        }

        return ResponseEntity.ok(Map.of(
            "success", successCount,
            "failed", failedCount,
            "conflicts", 0
        ));
    }

    @GetMapping("/wallet/balance/{studentId}")
    @Operation(summary = "Get student wallet balance",
               description = "Get current balance and statistics for a student")
    public ResponseEntity<Map<String, Object>> getStudentBalance(@PathVariable Long studentId) {

        java.math.BigDecimal balance = classWalletRepository.getCurrentBalance(studentId);
        java.math.BigDecimal totalEarned = classWalletRepository.getTotalEarnedByStudent(studentId);
        java.math.BigDecimal totalSpent = classWalletRepository.getTotalSpentByStudent(studentId);
        Long transactionCount = classWalletRepository.countByStudentId(studentId);
        ClassWallet lastTransaction = classWalletRepository.findTopByStudentIdOrderByCreatedAtDesc(studentId);

        return ResponseEntity.ok(Map.of(
            "studentId", studentId,
            "balance", balance != null ? balance : java.math.BigDecimal.ZERO,
            "totalEarned", totalEarned != null ? totalEarned : java.math.BigDecimal.ZERO,
            "totalSpent", totalSpent != null ? totalSpent : java.math.BigDecimal.ZERO,
            "transactionCount", transactionCount != null ? transactionCount : 0,
            "lastTransaction", lastTransaction != null ? lastTransaction.getCreatedAt() : null
        ));
    }

    // ========================================================================
    // CLUB MANAGEMENT ENDPOINTS
    // ========================================================================

    @GetMapping("/clubs")
    @Operation(summary = "Get clubs",
               description = "Get all clubs, optionally filtered by category, active status, or date")
    public ResponseEntity<Map<String, Object>> getClubs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) LocalDateTime since) {

        List<Club> clubs;

        if (since != null) {
            clubs = clubRepository.findByUpdatedAtAfter(since);
        } else if (category != null && Boolean.TRUE.equals(active)) {
            clubs = clubRepository.findByCategoryAndActiveTrueOrderByNameAsc(category);
        } else if (Boolean.TRUE.equals(active)) {
            clubs = clubRepository.findByActiveTrueOrderByNameAsc();
        } else {
            clubs = clubRepository.findAll();
        }

        List<ClubDTO> dtos = clubs.stream()
            .map(this::convertToClubDTO)
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "clubs", dtos,
            "count", dtos.size(),
            "timestamp", LocalDateTime.now()
        ));
    }

    @PostMapping("/clubs/batch")
    @Transactional
    @Operation(summary = "Batch sync clubs",
               description = "Submit club data from Teacher Portal")
    public ResponseEntity<Map<String, Object>> syncClubs(
            @RequestBody Map<String, List<ClubDTO>> request) {

        List<ClubDTO> clubs = request.get("clubs");
        if (clubs == null || clubs.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", 0,
                "failed", 0,
                "message", "No clubs provided"
            ));
        }

        int successCount = 0;
        int failedCount = 0;

        for (ClubDTO dto : clubs) {
            try {
                Club club = convertToClubEntity(dto);
                clubRepository.save(club);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to save club: {}", e.getMessage());
                failedCount++;
            }
        }

        return ResponseEntity.ok(Map.of(
            "success", successCount,
            "failed", failedCount,
            "conflicts", 0
        ));
    }

    @GetMapping("/clubs/{clubId}/members")
    @Operation(summary = "Get club members",
               description = "Get all members of a specific club")
    public ResponseEntity<Map<String, Object>> getClubMembers(@PathVariable Long clubId) {

        Optional<Club> clubOpt = clubRepository.findById(clubId);
        if (clubOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Club club = clubOpt.get();
        List<Map<String, Object>> members = club.getMembers().stream()
            .map(student -> Map.of(
                "studentId", (Object) student.getId(),
                "firstName", student.getFirstName(),
                "lastName", student.getLastName(),
                "gradeLevel", student.getGradeLevel()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "clubId", clubId,
            "clubName", club.getName(),
            "members", members,
            "memberCount", members.size()
        ));
    }

    @PostMapping("/clubs/{clubId}/members")
    @Transactional
    @Operation(summary = "Update club memberships",
               description = "Add or remove students from a club")
    public ResponseEntity<Map<String, Object>> updateClubMemberships(
            @PathVariable Long clubId,
            @RequestBody Map<String, List<Long>> request) {

        Optional<Club> clubOpt = clubRepository.findById(clubId);
        if (clubOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Club club = clubOpt.get();
        List<Long> addMembers = request.get("addMembers");
        List<Long> removeMembers = request.get("removeMembers");

        int added = 0;
        int removed = 0;

        // Add members
        if (addMembers != null) {
            for (Long studentId : addMembers) {
                studentRepository.findById(studentId).ifPresent(student -> {
                    club.addMember(student);
                });
                added++;
            }
        }

        // Remove members
        if (removeMembers != null) {
            for (Long studentId : removeMembers) {
                studentRepository.findById(studentId).ifPresent(student -> {
                    club.removeMember(student);
                });
                removed++;
            }
        }

        clubRepository.save(club);

        return ResponseEntity.ok(Map.of(
            "added", added,
            "removed", removed,
            "currentEnrollment", club.getCurrentEnrollment()
        ));
    }

    // ========================================================================
    // CONVERSION HELPERS - CLASS WALLET & CLUBS
    // ========================================================================

    private ClassWalletDTO convertToClassWalletDTO(ClassWallet wallet) {
        return ClassWalletDTO.builder()
            .id(wallet.getId())
            .studentId(wallet.getStudent() != null ? wallet.getStudent().getId() : null)
            .transactionType(wallet.getTransactionType() != null ? wallet.getTransactionType().name() : null)
            .amount(wallet.getAmount())
            .balanceAfter(wallet.getBalanceAfter())
            .category(wallet.getCategory())
            .description(wallet.getDescription())
            .transactionDate(wallet.getTransactionDate())
            .teacherId(wallet.getTeacher() != null ? wallet.getTeacher().getId() : null)
            .teacherName(wallet.getTeacherName())
            .approved(wallet.getApproved())
            .notes(wallet.getNotes())
            .syncStatus(wallet.getSyncStatus())
            .createdAt(wallet.getCreatedAt())
            .updatedAt(wallet.getUpdatedAt())
            .build();
    }

    private ClassWallet convertToClassWalletEntity(ClassWalletDTO dto) {
        ClassWallet wallet = ClassWallet.builder()
            .transactionType(dto.getTransactionType() != null ?
                ClassWallet.TransactionType.valueOf(dto.getTransactionType()) : null)
            .amount(dto.getAmount())
            .balanceAfter(dto.getBalanceAfter())
            .category(dto.getCategory())
            .description(dto.getDescription())
            .transactionDate(dto.getTransactionDate())
            .teacherName(dto.getTeacherName())
            .approved(dto.getApproved())
            .notes(dto.getNotes())
            .syncStatus(dto.getSyncStatus())
            .build();

        // Set student relationship
        if (dto.getStudentId() != null) {
            studentRepository.findById(dto.getStudentId()).ifPresent(wallet::setStudent);
        }

        // Set teacher relationship
        if (dto.getTeacherId() != null) {
            teacherRepository.findById(dto.getTeacherId()).ifPresent(wallet::setTeacher);
        }

        return wallet;
    }

    private ClubDTO convertToClubDTO(Club club) {
        return ClubDTO.builder()
            .id(club.getId())
            .name(club.getName())
            .description(club.getDescription())
            .category(club.getCategory())
            .advisorName(club.getAdvisorName())
            .advisorId(club.getAdvisor() != null ? club.getAdvisor().getId() : null)
            .meetingDay(club.getMeetingDay())
            .meetingTime(club.getMeetingTime())
            .durationMinutes(club.getDurationMinutes())
            .location(club.getLocation())
            .maxCapacity(club.getMaxCapacity())
            .currentEnrollment(club.getCurrentEnrollment())
            .active(club.getActive())
            .requiresApproval(club.getRequiresApproval())
            .startDate(club.getStartDate())
            .endDate(club.getEndDate())
            .nextMeetingDate(club.getNextMeetingDate())
            .notes(club.getNotes())
            .syncStatus(club.getSyncStatus())
            .availableSpots(club.getAvailableSpots())
            .atCapacity(club.isAtCapacity())
            .memberIds(club.getMembers().stream().map(Student::getId).collect(Collectors.toList()))
            .createdAt(club.getCreatedAt())
            .updatedAt(club.getUpdatedAt())
            .build();
    }

    private Club convertToClubEntity(ClubDTO dto) {
        Club club = Club.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .category(dto.getCategory())
            .advisorName(dto.getAdvisorName())
            .meetingDay(dto.getMeetingDay())
            .meetingTime(dto.getMeetingTime())
            .durationMinutes(dto.getDurationMinutes())
            .location(dto.getLocation())
            .maxCapacity(dto.getMaxCapacity())
            .currentEnrollment(dto.getCurrentEnrollment())
            .active(dto.getActive())
            .requiresApproval(dto.getRequiresApproval())
            .startDate(dto.getStartDate())
            .endDate(dto.getEndDate())
            .nextMeetingDate(dto.getNextMeetingDate())
            .notes(dto.getNotes())
            .syncStatus(dto.getSyncStatus())
            .build();

        // Set advisor relationship
        if (dto.getAdvisorId() != null) {
            teacherRepository.findById(dto.getAdvisorId()).ifPresent(club::setAdvisor);
        }

        // Set members if provided
        if (dto.getMemberIds() != null) {
            for (Long studentId : dto.getMemberIds()) {
                studentRepository.findById(studentId).ifPresent(club::addMember);
            }
        }

        return club;
    }

    // ========================================================================
    // SCHEDULE AND ROSTER ENDPOINTS (Phase 4D - December 8, 2025)
    // ========================================================================

    @GetMapping("/schedule")
    @Operation(summary = "Get teacher's schedule",
               description = "Returns teacher's course assignments by period for EduPro-Teacher sync")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Schedule retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Teacher not found"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<TeacherScheduleDTO> getTeacherSchedule(
            @RequestParam @Parameter(description = "Teacher employee ID") String employeeId) {

        log.info("Fetching schedule for teacher: {}", employeeId);

        try {
            // Find teacher by employee ID
            Optional<Teacher> teacherOpt = teacherRepository.findByEmployeeId(employeeId);

            if (teacherOpt.isEmpty()) {
                log.warn("Teacher not found: {}", employeeId);
                return ResponseEntity.notFound().build();
            }

            Teacher teacher = teacherOpt.get();

            // Find all schedule slots for this teacher
            List<ScheduleSlot> slots = scheduleSlotRepository.findByTeacherIdWithDetails(teacher.getId());

            log.info("Found {} schedule slots for teacher {}", slots.size(), employeeId);

            // Group slots by period number
            Map<Integer, TeacherScheduleDTO.PeriodAssignmentDTO> periodMap = new HashMap<>();

            for (ScheduleSlot slot : slots) {
                if (slot.getPeriodNumber() != null) {
                    Integer period = slot.getPeriodNumber();

                    // Build period assignment DTO
                    TeacherScheduleDTO.PeriodAssignmentDTO periodAssignment =
                        TeacherScheduleDTO.PeriodAssignmentDTO.builder()
                            .period(period)
                            .courseId(slot.getCourse() != null ? slot.getCourse().getId() : null)
                            .courseName(slot.getCourse() != null ? slot.getCourse().getCourseName() : null)
                            .courseCode(slot.getCourse() != null ? slot.getCourse().getCourseCode() : null)
                            .subject(slot.getCourse() != null ? slot.getCourse().getSubject() : null)
                            .roomId(slot.getRoom() != null ? slot.getRoom().getId() : null)
                            .roomNumber(slot.getRoom() != null ? slot.getRoom().getRoomNumber() : null)
                            .roomType(slot.getRoom() != null && slot.getRoom().getRoomType() != null ?
                                     slot.getRoom().getRoomType().name() : null)
                            .startTime(slot.getStartTime())
                            .endTime(slot.getEndTime())
                            .enrolledCount(slot.getEnrolledStudents())
                            .maxCapacity(slot.getCourse() != null ? slot.getCourse().getMaxStudents() : null)
                            .studentIds(slot.getStudents() != null ?
                                       slot.getStudents().stream().map(Student::getId).collect(Collectors.toList()) :
                                       new ArrayList<>())
                            .build();

                    periodMap.put(period, periodAssignment);
                }
            }

            // Build teacher schedule DTO
            TeacherScheduleDTO scheduleDTO = TeacherScheduleDTO.builder()
                .teacherId(teacher.getId())
                .employeeId(teacher.getEmployeeId())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .fullName(teacher.getFirstName() + " " + teacher.getLastName())
                .department(teacher.getDepartment())
                .email(teacher.getEmail())
                .periods(periodMap)
                .build();

            log.info("Successfully built schedule for teacher {} with {} periods",
                     employeeId, periodMap.size());

            return ResponseEntity.ok(scheduleDTO);

        } catch (Exception e) {
            log.error("Error fetching teacher schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/roster")
    @Operation(summary = "Get class roster for specific period",
               description = "Returns students enrolled in teacher's course for specific period")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Roster retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Teacher or period not found"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<ClassRosterDTO> getClassRoster(
            @RequestParam @Parameter(description = "Teacher employee ID") String employeeId,
            @RequestParam @Parameter(description = "Period number (0-7)") Integer period) {

        log.info("Fetching roster for teacher: {}, period: {}", employeeId, period);

        try {
            // Find teacher
            Optional<Teacher> teacherOpt = teacherRepository.findByEmployeeId(employeeId);

            if (teacherOpt.isEmpty()) {
                log.warn("Teacher not found: {}", employeeId);
                return ResponseEntity.notFound().build();
            }

            Teacher teacher = teacherOpt.get();

            // Find schedule slots for this teacher
            List<ScheduleSlot> slots = scheduleSlotRepository.findByTeacherIdWithDetails(teacher.getId());

            // Find slot for requested period
            Optional<ScheduleSlot> slotOpt = slots.stream()
                .filter(s -> period.equals(s.getPeriodNumber()))
                .findFirst();

            if (slotOpt.isEmpty()) {
                log.info("No class found for period {} - might be planning period or lunch", period);
                return ResponseEntity.ok(null); // Return null for empty periods (planning/lunch)
            }

            ScheduleSlot slot = slotOpt.get();

            // Build roster DTO
            ClassRosterDTO roster = buildClassRosterDTO(slot);

            log.info("Successfully built roster for period {} with {} students",
                     period, roster.getEnrolledCount());

            return ResponseEntity.ok(roster);

        } catch (Exception e) {
            log.error("Error fetching class roster", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/rosters")
    @Operation(summary = "Get all class rosters",
               description = "Returns all rosters for all periods taught by teacher")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rosters retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Teacher not found"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<Map<Integer, ClassRosterDTO>> getAllRosters(
            @RequestParam @Parameter(description = "Teacher employee ID") String employeeId) {

        log.info("Fetching all rosters for teacher: {}", employeeId);

        try {
            // Find teacher
            Optional<Teacher> teacherOpt = teacherRepository.findByEmployeeId(employeeId);

            if (teacherOpt.isEmpty()) {
                log.warn("Teacher not found: {}", employeeId);
                return ResponseEntity.notFound().build();
            }

            Teacher teacher = teacherOpt.get();

            // Find all schedule slots for this teacher
            List<ScheduleSlot> slots = scheduleSlotRepository.findByTeacherIdWithDetails(teacher.getId());

            log.info("Found {} schedule slots for teacher {}", slots.size(), employeeId);

            // Build rosters for all periods
            Map<Integer, ClassRosterDTO> rosters = new HashMap<>();

            for (ScheduleSlot slot : slots) {
                if (slot.getPeriodNumber() != null) {
                    ClassRosterDTO roster = buildClassRosterDTO(slot);
                    rosters.put(slot.getPeriodNumber(), roster);
                }
            }

            log.info("Successfully built {} rosters for teacher {}", rosters.size(), employeeId);

            return ResponseEntity.ok(rosters);

        } catch (Exception e) {
            log.error("Error fetching all rosters", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========================================================================
    // HELPER METHODS FOR ROSTER/SCHEDULE SYNC
    // ========================================================================

    /**
     * Build ClassRosterDTO from ScheduleSlot
     */
    private ClassRosterDTO buildClassRosterDTO(ScheduleSlot slot) {
        // Build student roster
        List<ClassRosterDTO.RosterStudentDTO> studentDTOs = new ArrayList<>();

        if (slot.getStudents() != null) {
            for (Student student : slot.getStudents()) {
                // Parse grade level to integer
                Integer gradeLevelInt = null;
                try {
                    if (student.getGradeLevel() != null) {
                        gradeLevelInt = Integer.parseInt(student.getGradeLevel().trim());
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid grade level for student {}: {}", student.getId(), student.getGradeLevel());
                }

                ClassRosterDTO.RosterStudentDTO studentDTO =
                    ClassRosterDTO.RosterStudentDTO.builder()
                        .studentId(student.getId())
                        .studentNumber(student.getStudentId())
                        .firstName(student.getFirstName())
                        .lastName(student.getLastName())
                        .fullName(student.getFirstName() + " " + student.getLastName())
                        .gradeLevel(gradeLevelInt)
                        .email(student.getEmail())
                        .hasIep(student.getHasIEP() != null && student.getHasIEP())
                        .has504(student.getHas504Plan() != null && student.getHas504Plan())
                        .photoUrl(null) // Optional - add if photo field exists
                        .currentGpa(student.getCurrentGPA())
                        .notes(null) // Optional - add if needed
                        .build();

                studentDTOs.add(studentDTO);
            }
        }

        // Build period display string
        String periodDisplay = slot.getPeriodNumber() == 0 ? "Homeroom" :
                              "Period " + slot.getPeriodNumber();

        // Calculate duration
        Integer durationMinutes = null;
        if (slot.getStartTime() != null && slot.getEndTime() != null) {
            durationMinutes = (int) java.time.Duration.between(
                slot.getStartTime(), slot.getEndTime()).toMinutes();
        }

        return ClassRosterDTO.builder()
            .period(slot.getPeriodNumber())
            .periodDisplay(periodDisplay)
            .courseId(slot.getCourse() != null ? slot.getCourse().getId() : null)
            .courseName(slot.getCourse() != null ? slot.getCourse().getCourseName() : null)
            .courseCode(slot.getCourse() != null ? slot.getCourse().getCourseCode() : null)
            .subject(slot.getCourse() != null ? slot.getCourse().getSubject() : null)
            .roomId(slot.getRoom() != null ? slot.getRoom().getId() : null)
            .roomNumber(slot.getRoom() != null ? slot.getRoom().getRoomNumber() : null)
            .roomType(slot.getRoom() != null && slot.getRoom().getRoomType() != null ?
                     slot.getRoom().getRoomType().name() : null)
            .startTime(slot.getStartTime())
            .endTime(slot.getEndTime())
            .durationMinutes(durationMinutes)
            .enrolledCount(slot.getEnrolledStudents())
            .maxCapacity(slot.getCourse() != null ? slot.getCourse().getMaxStudents() : null)
            .students(studentDTOs)
            .build();
    }
}
