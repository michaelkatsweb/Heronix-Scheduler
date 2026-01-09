package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.StudentEnrollment;
import com.heronix.model.domain.Schedule;
import com.heronix.model.enums.EnrollmentStatus;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.StudentEnrollmentRepository;
import com.heronix.repository.ScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enrollment Import Service
 *
 * Handles CSV import of student enrollments (linking students to courses).
 * This service is designed for initial data loading before schedule generation.
 *
 * CSV Format:
 * studentId,courseCode,enrollmentDate,status
 *
 * Example:
 * S001,MATH9-01,2025-08-15,ENROLLED
 * S002,ENG9-01,2025-08-15,ENROLLED
 * S003,SCI9-02,2025-08-20,ACTIVE
 *
 * Features:
 * - CSV parsing with validation
 * - Batch processing
 * - Detailed error reporting
 * - Duplicate detection
 * - Links students to courses (enrolledCourses many-to-many relationship)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 5, 2025
 */
@Slf4j
@Service
public class EnrollmentImportService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentEnrollmentRepository enrollmentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    /**
     * Import Result DTO
     */
    public static class ImportResult {
        private boolean success;
        private int totalRows;
        private int successfulRows;
        private int failedRows;
        private int enrollmentsCreated;
        private List<String> errors;
        private List<String> warnings;
        private long processingTimeMs;

        public ImportResult() {
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

        public int getSuccessfulRows() { return successfulRows; }
        public void setSuccessfulRows(int successfulRows) { this.successfulRows = successfulRows; }

        public int getFailedRows() { return failedRows; }
        public void setFailedRows(int failedRows) { this.failedRows = failedRows; }

        public int getEnrollmentsCreated() { return enrollmentsCreated; }
        public void setEnrollmentsCreated(int enrollmentsCreated) { this.enrollmentsCreated = enrollmentsCreated; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }

        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }

        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

        public void addError(String error) {
            this.errors.add(error);
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Import Complete%n"));
            sb.append(String.format("Total Rows: %d%n", totalRows));
            sb.append(String.format("Successful: %d%n", successfulRows));
            sb.append(String.format("Failed: %d%n", failedRows));
            sb.append(String.format("Enrollments Created: %d%n", enrollmentsCreated));
            sb.append(String.format("Processing Time: %d ms%n", processingTimeMs));

            if (!errors.isEmpty()) {
                sb.append(String.format("%nErrors: %d%n", errors.size()));
                errors.stream().limit(10).forEach(e -> sb.append("  - ").append(e).append("\n"));
                if (errors.size() > 10) {
                    sb.append(String.format("  ... and %d more errors%n", errors.size() - 10));
                }
            }

            if (!warnings.isEmpty()) {
                sb.append(String.format("%nWarnings: %d%n", warnings.size()));
                warnings.stream().limit(5).forEach(w -> sb.append("  - ").append(w).append("\n"));
                if (warnings.size() > 5) {
                    sb.append(String.format("  ... and %d more warnings%n", warnings.size() - 5));
                }
            }

            return sb.toString();
        }
    }

    /**
     * Import enrollments from CSV file
     *
     * @param file CSV file
     * @param skipHeader Skip first row if true
     * @return Import result with statistics and errors
     */
    @Transactional
    public ImportResult importFromCSV(File file, boolean skipHeader) {
        ImportResult result = new ImportResult();
        long startTime = System.currentTimeMillis();

        log.info("Starting enrollment CSV import from file: {}", file.getName());

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            int processedRows = 0;

            // Skip header if requested
            if (skipHeader && (line = reader.readLine()) != null) {
                lineNumber++;
                log.debug("Skipped header row: {}", line);
            }

            // Process each line
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    processCSVLine(line, lineNumber);
                    processedRows++;
                } catch (Exception e) {
                    result.addError(String.format("Line %d: %s - %s", lineNumber, line, e.getMessage()));
                    log.error("Error processing line {}: {}", lineNumber, line, e);
                }
            }

            result.setTotalRows(processedRows);
            result.setSuccessfulRows(processedRows - result.getErrors().size());
            result.setFailedRows(result.getErrors().size());
            result.setEnrollmentsCreated(processedRows - result.getErrors().size());

            if (result.getSuccessfulRows() > 0) {
                result.setSuccess(true);
                log.info("Successfully processed {} enrollment records", result.getSuccessfulRows());
            } else {
                result.addError("No valid enrollments to import");
                result.setSuccess(false);
            }

        } catch (Exception e) {
            log.error("Fatal error during CSV import", e);
            result.addError("Fatal error: " + e.getMessage());
            result.setSuccess(false);
        }

        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        log.info("CSV import completed in {} ms. Success: {}", result.getProcessingTimeMs(), result.isSuccess());

        return result;
    }

    /**
     * Process a single CSV line
     *
     * Expected format: studentId,courseCode,enrollmentDate,status
     *
     * @param line CSV line
     * @param lineNumber Line number for error reporting
     * @throws Exception if validation fails
     */
    private void processCSVLine(String line, int lineNumber) throws Exception {
        String[] parts = line.split(",");

        // Validate format
        if (parts.length < 2) {
            throw new Exception("Invalid format. Expected: studentId,courseCode[,enrollmentDate,status]");
        }

        // Extract fields
        String studentId = parts[0].trim();
        String courseCode = parts.length > 1 ? parts[1].trim() : "";
        String enrollmentDateStr = parts.length > 2 ? parts[2].trim() : "";
        String statusStr = parts.length > 3 ? parts[3].trim() : "ENROLLED";

        // Validate student exists
        Student student = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new Exception("Student not found: " + studentId));

        // Validate course exists
        Course course = courseRepository.findByCourseCode(courseCode)
            .orElseThrow(() -> new Exception("Course not found: " + courseCode));

        // Check for duplicate enrollment
        boolean alreadyEnrolled = student.getEnrolledCourses().stream()
            .anyMatch(c -> c.getId().equals(course.getId()));

        if (alreadyEnrolled) {
            log.warn("Student {} already enrolled in course {}. Skipping.", studentId, courseCode);
            return;
        }

        // Parse enrollment date
        LocalDateTime enrollmentDate = LocalDateTime.now();
        if (!enrollmentDateStr.isEmpty()) {
            try {
                enrollmentDate = parseDate(enrollmentDateStr);
            } catch (DateTimeParseException e) {
                log.warn("Invalid date format '{}', using current date", enrollmentDateStr);
            }
        }

        // Add course to student's enrolled courses (many-to-many relationship)
        student.getEnrolledCourses().add(course);
        studentRepository.save(student);

        log.debug("Enrolled student {} in course {} (line {})", studentId, courseCode, lineNumber);
    }

    /**
     * Parse date string with multiple format support
     */
    private LocalDateTime parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return date.atStartOfDay();
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        throw new DateTimeParseException("Unable to parse date: " + dateStr, dateStr, 0);
    }

    /**
     * Validate CSV file before import
     *
     * @param file CSV file
     * @param skipHeader Skip first row if true
     * @return Validation result
     */
    public ImportResult validateCSV(File file, boolean skipHeader) {
        ImportResult result = new ImportResult();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            int validRows = 0;

            // Skip header if requested
            if (skipHeader && (line = reader.readLine()) != null) {
                lineNumber++;
            }

            // Validate each line
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    validateCSVLine(line, lineNumber);
                    validRows++;
                } catch (Exception e) {
                    result.addError(String.format("Line %d: %s", lineNumber, e.getMessage()));
                }
            }

            result.setTotalRows(validRows + result.getErrors().size());
            result.setSuccessfulRows(validRows);
            result.setFailedRows(result.getErrors().size());
            result.setSuccess(result.getErrors().isEmpty());

        } catch (Exception e) {
            result.addError("Failed to read file: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    /**
     * Validate a single CSV line without creating enrollments
     */
    private void validateCSVLine(String line, int lineNumber) throws Exception {
        String[] parts = line.split(",");

        if (parts.length < 2) {
            throw new Exception("Invalid format");
        }

        String studentId = parts[0].trim();
        String courseCode = parts[1].trim();

        if (studentId.isEmpty()) {
            throw new Exception("Student ID is required");
        }

        if (courseCode.isEmpty()) {
            throw new Exception("Course code is required");
        }

        // Validate student exists
        studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new Exception("Student not found: " + studentId));

        // Validate course exists
        courseRepository.findByCourseCode(courseCode)
            .orElseThrow(() -> new Exception("Course not found: " + courseCode));
    }

    /**
     * Generate CSV template file
     *
     * @param outputFile Output file
     * @param includeExamples Include example rows
     * @throws Exception if file creation fails
     */
    public void generateCSVTemplate(File outputFile, boolean includeExamples) throws Exception {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(outputFile)) {
            // Write header
            writer.println("studentId,courseCode,enrollmentDate,status");

            if (includeExamples) {
                // Write example rows
                writer.println("S001,MATH9-01,2025-08-15,ENROLLED");
                writer.println("S002,ENG9-01,2025-08-15,ENROLLED");
                writer.println("S003,SCI9-02,2025-08-20,ACTIVE");
            }

            log.info("CSV template generated: {}", outputFile.getAbsolutePath());
        }
    }
}
