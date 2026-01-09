package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.dto.FrontlineImportDTO;
import com.heronix.model.enums.*;
import com.heronix.repository.*;
import com.heronix.service.parser.FrontlineCSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for importing substitute assignments from Frontline CSV files
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Service
@Transactional
public class FrontlineImportService {

    private static final Logger logger = LoggerFactory.getLogger(FrontlineImportService.class);

    private final FrontlineCSVParser csvParser;
    private final SubstituteRepository substituteRepository;
    private final SubstituteAssignmentRepository assignmentRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public FrontlineImportService(FrontlineCSVParser csvParser,
                                 SubstituteRepository substituteRepository,
                                 SubstituteAssignmentRepository assignmentRepository,
                                 TeacherRepository teacherRepository,
                                 RoomRepository roomRepository,
                                 CourseRepository courseRepository) {
        this.csvParser = csvParser;
        this.substituteRepository = substituteRepository;
        this.assignmentRepository = assignmentRepository;
        this.teacherRepository = teacherRepository;
        this.roomRepository = roomRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Import assignments from a Frontline CSV file
     */
    public ImportResult importFromCSV(File file) throws IOException {
        logger.info("Starting Frontline import from: {}", file.getName());

        ImportResult result = new ImportResult();
        result.setFileName(file.getName());

        // Parse CSV file
        List<FrontlineImportDTO> dtos;
        try {
            dtos = csvParser.parseCSV(file);
            result.setTotalRecords(dtos.size());
            logger.info("Parsed {} records from CSV", dtos.size());
        } catch (IOException e) {
            logger.error("Error parsing CSV file: {}", e.getMessage());
            throw e;
        }

        // Process each record
        for (FrontlineImportDTO dto : dtos) {
            try {
                processImportRecord(dto, result);
            } catch (Exception e) {
                logger.error("Error processing row {}: {}", dto.getRowNumber(), e.getMessage(), e);
                result.addError(dto.getRowNumber(), e.getMessage());
            }
        }

        logger.info("Import completed: {} created, {} updated, {} skipped, {} errors",
                result.getCreatedCount(), result.getUpdatedCount(),
                result.getSkippedCount(), result.getErrorCount());

        return result;
    }

    /**
     * Process a single import record
     */
    private void processImportRecord(FrontlineImportDTO dto, ImportResult result) {
        // Validate
        if (!dto.isValid()) {
            result.addError(dto.getRowNumber(), dto.getValidationError());
            return;
        }

        // Check if assignment already exists by Frontline job ID
        if (dto.getJobId() != null && assignmentRepository.existsByFrontlineJobId(dto.getJobId())) {
            logger.debug("Assignment {} already exists, skipping", dto.getJobId());
            result.incrementSkipped();
            return;
        }

        // Find or create substitute
        Substitute substitute = findOrCreateSubstitute(dto);

        // Find replaced teacher (if applicable)
        Teacher replacedTeacher = findReplacedTeacher(dto);

        // Find room (if specified)
        Room room = findRoom(dto);

        // Find course (if specified)
        Course course = findCourse(dto);

        // Create assignment
        SubstituteAssignment assignment = createAssignment(dto, substitute, replacedTeacher, room, course);

        // Save assignment
        assignmentRepository.save(assignment);
        result.incrementCreated();

        logger.debug("Created assignment for substitute {} on {}", substitute.getFullName(), dto.getAssignmentDate());
    }

    /**
     * Find or create a substitute from DTO data
     */
    private Substitute findOrCreateSubstitute(FrontlineImportDTO dto) {
        // Try to find by employee ID first
        if (dto.getSubstituteEmployeeId() != null) {
            Optional<Substitute> existing = substituteRepository.findByEmployeeId(dto.getSubstituteEmployeeId());
            if (existing.isPresent()) {
                logger.debug("Found existing substitute by employee ID: {}", dto.getSubstituteEmployeeId());
                return updateSubstituteIfNeeded(existing.get(), dto);
            }
        }

        // Try to find by name
        String fullName = dto.getSubstituteFullName().trim();
        List<Substitute> byName = substituteRepository.findByNameContaining(fullName);
        if (!byName.isEmpty()) {
            // Check for exact match
            for (Substitute sub : byName) {
                if (sub.getFullName().equalsIgnoreCase(fullName)) {
                    logger.debug("Found existing substitute by name: {}", fullName);
                    return updateSubstituteIfNeeded(sub, dto);
                }
            }
        }

        // Create new substitute
        logger.info("Creating new substitute: {}", fullName);
        Substitute substitute = new Substitute();
        substitute.setFirstName(dto.getSubstituteFirstName());
        substitute.setLastName(dto.getSubstituteLastName());
        substitute.setEmployeeId(dto.getSubstituteEmployeeId());
        substitute.setEmail(dto.getSubstituteEmail());
        substitute.setPhoneNumber(dto.getSubstitutePhone());
        substitute.setType(mapSubstituteType(dto.getSubstituteType()));
        substitute.setActive(true);

        return substituteRepository.save(substitute);
    }

    /**
     * Update substitute information if needed
     */
    private Substitute updateSubstituteIfNeeded(Substitute substitute, FrontlineImportDTO dto) {
        boolean updated = false;

        if (dto.getSubstituteEmail() != null && !dto.getSubstituteEmail().equals(substitute.getEmail())) {
            substitute.setEmail(dto.getSubstituteEmail());
            updated = true;
        }

        if (dto.getSubstitutePhone() != null && !dto.getSubstitutePhone().equals(substitute.getPhoneNumber())) {
            substitute.setPhoneNumber(dto.getSubstitutePhone());
            updated = true;
        }

        if (updated) {
            return substituteRepository.save(substitute);
        }

        return substitute;
    }

    /**
     * Find replaced teacher by name or employee ID
     */
    private Teacher findReplacedTeacher(FrontlineImportDTO dto) {
        if (dto.getReplacedStaffFirstName() == null && dto.getReplacedStaffLastName() == null) {
            return null;
        }

        // Try employee ID first
        if (dto.getReplacedStaffEmployeeId() != null) {
            List<Teacher> teachers = teacherRepository.findAllActive();
            for (Teacher teacher : teachers) {
                // Assuming Teacher has an employeeId field (may need to add if not present)
                if (dto.getReplacedStaffEmployeeId().equals(teacher.getEmployeeId())) {
                    logger.debug("Found replaced teacher by employee ID: {}", dto.getReplacedStaffEmployeeId());
                    return teacher;
                }
            }
        }

        // Try by name
        String fullName = dto.getReplacedStaffFullName().trim();
        List<Teacher> teachers = teacherRepository.findAllActive();
        for (Teacher teacher : teachers) {
            if (teacher.getName().equalsIgnoreCase(fullName)) {
                logger.debug("Found replaced teacher by name: {}", fullName);
                return teacher;
            }
        }

        logger.debug("Could not find replaced teacher: {}", fullName);
        return null;
    }

    /**
     * Find room by number
     */
    private Room findRoom(FrontlineImportDTO dto) {
        if (dto.getRoomNumber() == null) {
            return null;
        }

        Optional<Room> room = roomRepository.findByRoomNumber(dto.getRoomNumber());
        if (room.isPresent()) {
            logger.debug("Found room: {}", dto.getRoomNumber());
            return room.get();
        }

        logger.debug("Could not find room: {}", dto.getRoomNumber());
        return null;
    }

    /**
     * Find course by code or name
     */
    private Course findCourse(FrontlineImportDTO dto) {
        if (dto.getCourseCode() == null && dto.getCourseName() == null) {
            return null;
        }

        // Try by course code first
        if (dto.getCourseCode() != null) {
            Optional<Course> course = courseRepository.findByCourseCode(dto.getCourseCode());
            if (course.isPresent()) {
                logger.debug("Found course by code: {}", dto.getCourseCode());
                return course.get();
            }
        }

        // Try by name
        if (dto.getCourseName() != null) {
            List<Course> courses = courseRepository.findByCourseNameContaining(dto.getCourseName());
            if (!courses.isEmpty()) {
                logger.debug("Found course by name: {}", dto.getCourseName());
                return courses.get(0);
            }
        }

        logger.debug("Could not find course: {} / {}", dto.getCourseCode(), dto.getCourseName());
        return null;
    }

    /**
     * Create SubstituteAssignment from DTO
     */
    private SubstituteAssignment createAssignment(FrontlineImportDTO dto, Substitute substitute,
                                                  Teacher replacedTeacher, Room room, Course course) {
        SubstituteAssignment assignment = new SubstituteAssignment();

        // Basic assignment info
        assignment.setSubstitute(substitute);
        assignment.setReplacedTeacher(replacedTeacher);
        assignment.setReplacedStaffType(mapStaffType(dto.getReplacedStaffType()));
        assignment.setReplacedStaffName(dto.getReplacedStaffFullName());

        // Date/Time
        assignment.setAssignmentDate(dto.getAssignmentDate());
        assignment.setStartTime(dto.getStartTime());
        assignment.setEndTime(dto.getEndTime());
        assignment.setEndDate(dto.getEndDate());

        // Duration and status
        assignment.setDurationType(mapDurationType(dto.getDurationType(), dto.getAssignmentDate(), dto.getEndDate()));
        assignment.setStatus(mapStatus(dto.getStatus()));

        // Details
        assignment.setAbsenceReason(mapAbsenceReason(dto.getAbsenceReason()));
        assignment.setAssignmentSource(AssignmentSource.FRONTLINE);
        assignment.setFrontlineJobId(dto.getJobId());
        assignment.setNotes(dto.getNotes());

        // Location and course
        assignment.setRoom(room);
        assignment.setCourse(course);

        // Floater flag
        assignment.setIsFloater(dto.getIsFloater());

        // Calculate hours if not provided
        if (dto.getHours() != null) {
            assignment.setTotalHours(dto.getHours());
        } else if (dto.getStartTime() != null && dto.getEndTime() != null) {
            Duration duration = Duration.between(dto.getStartTime(), dto.getEndTime());
            assignment.setTotalHours(duration.toMinutes() / 60.0);
        }

        // Pay amount
        assignment.setPayAmount(dto.getPayAmount());

        return assignment;
    }

    // ==================== MAPPING METHODS ====================

    /**
     * Map string to SubstituteType enum
     */
    private SubstituteType mapSubstituteType(String type) {
        if (type == null) {
            return SubstituteType.UNCERTIFIED_SUBSTITUTE;
        }

        String normalized = type.toLowerCase().trim();
        if (normalized.contains("certified") || normalized.contains("teacher")) {
            return SubstituteType.CERTIFIED_TEACHER;
        } else if (normalized.contains("para")) {
            return SubstituteType.PARAPROFESSIONAL;
        } else if (normalized.contains("long") && normalized.contains("term")) {
            return SubstituteType.LONG_TERM_SUBSTITUTE;
        }

        return SubstituteType.UNCERTIFIED_SUBSTITUTE;
    }

    /**
     * Map string to StaffType enum
     */
    private StaffType mapStaffType(String type) {
        if (type == null) {
            return StaffType.TEACHER;
        }

        String normalized = type.toLowerCase().trim();
        if (normalized.contains("co") && normalized.contains("teacher")) {
            return StaffType.CO_TEACHER;
        } else if (normalized.contains("para")) {
            return StaffType.PARAPROFESSIONAL;
        } else if (normalized.contains("special")) {
            return StaffType.SPECIAL_ED;
        }

        return StaffType.TEACHER;
    }

    /**
     * Map string to AssignmentDuration enum
     */
    private AssignmentDuration mapDurationType(String type, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (type != null) {
            String normalized = type.toLowerCase().trim();
            if (normalized.contains("hourly") || normalized.contains("hour")) {
                return AssignmentDuration.HOURLY;
            } else if (normalized.contains("half")) {
                return AssignmentDuration.HALF_DAY;
            } else if (normalized.contains("full")) {
                return AssignmentDuration.FULL_DAY;
            } else if (normalized.contains("multi")) {
                return AssignmentDuration.MULTI_DAY;
            } else if (normalized.contains("long")) {
                return AssignmentDuration.LONG_TERM;
            }
        }

        // Auto-detect based on dates
        if (endDate != null && !endDate.equals(startDate)) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            if (days >= 7) {
                return AssignmentDuration.LONG_TERM;
            } else if (days >= 2) {
                return AssignmentDuration.MULTI_DAY;
            }
        }

        return AssignmentDuration.FULL_DAY;
    }

    /**
     * Map string to AssignmentStatus enum
     */
    private AssignmentStatus mapStatus(String status) {
        if (status == null) {
            return AssignmentStatus.CONFIRMED;
        }

        String normalized = status.toLowerCase().trim();
        if (normalized.contains("pending")) {
            return AssignmentStatus.PENDING;
        } else if (normalized.contains("confirmed") || normalized.contains("assigned")) {
            return AssignmentStatus.CONFIRMED;
        } else if (normalized.contains("progress") || normalized.contains("active")) {
            return AssignmentStatus.IN_PROGRESS;
        } else if (normalized.contains("completed") || normalized.contains("done")) {
            return AssignmentStatus.COMPLETED;
        } else if (normalized.contains("cancel")) {
            return AssignmentStatus.CANCELLED;
        } else if (normalized.contains("no") && normalized.contains("show")) {
            return AssignmentStatus.NO_SHOW;
        }

        return AssignmentStatus.CONFIRMED;
    }

    /**
     * Map string to AbsenceReason enum
     */
    private AbsenceReason mapAbsenceReason(String reason) {
        if (reason == null) {
            return AbsenceReason.OTHER;
        }

        String normalized = reason.toLowerCase().trim();
        if (normalized.contains("sick")) {
            return AbsenceReason.SICK_LEAVE;
        } else if (normalized.contains("personal")) {
            return AbsenceReason.PERSONAL_DAY;
        } else if (normalized.contains("iep")) {
            return AbsenceReason.IEP_MEETING;
        } else if (normalized.contains("504")) {
            return AbsenceReason.PLAN_504_MEETING;
        } else if (normalized.contains("professional") || normalized.contains("pd")) {
            return AbsenceReason.PROFESSIONAL_DEV;
        } else if (normalized.contains("medical") || normalized.contains("fmla")) {
            return AbsenceReason.MEDICAL_LEAVE;
        } else if (normalized.contains("maternity") || normalized.contains("paternity")) {
            return AbsenceReason.MATERNITY_LEAVE;
        } else if (normalized.contains("emergency")) {
            return AbsenceReason.FAMILY_EMERGENCY;
        } else if (normalized.contains("bereavement") || normalized.contains("funeral")) {
            return AbsenceReason.BEREAVEMENT;
        } else if (normalized.contains("jury")) {
            return AbsenceReason.JURY_DUTY;
        }

        return AbsenceReason.OTHER;
    }

    // ==================== IMPORT RESULT CLASS ====================

    /**
     * Result object for import operations
     */
    public static class ImportResult {
        private String fileName;
        private int totalRecords;
        private int createdCount;
        private int updatedCount;
        private int skippedCount;
        private List<ImportError> errors = new ArrayList<>();

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public int getTotalRecords() {
            return totalRecords;
        }

        public void setTotalRecords(int totalRecords) {
            this.totalRecords = totalRecords;
        }

        public int getCreatedCount() {
            return createdCount;
        }

        public void incrementCreated() {
            this.createdCount++;
        }

        public int getUpdatedCount() {
            return updatedCount;
        }

        public void incrementUpdated() {
            this.updatedCount++;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public void incrementSkipped() {
            this.skippedCount++;
        }

        public List<ImportError> getErrors() {
            return errors;
        }

        public void addError(int rowNumber, String message) {
            errors.add(new ImportError(rowNumber, message));
        }

        public int getErrorCount() {
            return errors.size();
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public String getSummary() {
            return String.format("Import Summary - Total: %d, Created: %d, Updated: %d, Skipped: %d, Errors: %d",
                    totalRecords, createdCount, updatedCount, skippedCount, getErrorCount());
        }
    }

    /**
     * Import error details
     */
    public static class ImportError {
        private int rowNumber;
        private String message;

        public ImportError(int rowNumber, String message) {
            this.rowNumber = rowNumber;
            this.message = message;
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "Row " + rowNumber + ": " + message;
        }
    }
}
