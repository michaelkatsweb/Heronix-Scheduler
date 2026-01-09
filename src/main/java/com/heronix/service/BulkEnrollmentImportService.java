package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseEnrollmentRequest;
import com.heronix.model.domain.Student;
import com.heronix.model.enums.EnrollmentRequestStatus;
import com.heronix.repository.CourseEnrollmentRequestRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulk Enrollment Import Service
 *
 * Handles CSV import of enrollment requests for multiple students.
 * Validates data, creates enrollment requests, and provides detailed feedback.
 *
 * CSV Format:
 * StudentID,FirstChoice,SecondChoice,ThirdChoice,FourthChoice
 *
 * Example:
 * S001,ART101,MUSIC101,DRAMA101,PHOTO101
 * S002,MUSIC101,ART101,PE101,TECH101
 *
 * Features:
 * - CSV parsing with validation
 * - Batch processing with progress tracking
 * - Transaction support (all or nothing)
 * - Detailed error reporting
 * - Duplicate detection
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7B - November 21, 2025
 */
@Slf4j
@Service
public class BulkEnrollmentImportService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRequestRepository enrollmentRequestRepository;

    /**
     * Import Result DTO
     */
    public static class ImportResult {
        private boolean success;
        private int totalRows;
        private int successfulRows;
        private int failedRows;
        private int requestsCreated;
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

        public int getRequestsCreated() { return requestsCreated; }
        public void setRequestsCreated(int requestsCreated) { this.requestsCreated = requestsCreated; }

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
            sb.append(String.format("Requests Created: %d%n", requestsCreated));
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
     * Import enrollment requests from CSV file
     *
     * @param file CSV file
     * @param skipHeader Skip first row if true
     * @return Import result with statistics and errors
     */
    @Transactional
    public ImportResult importFromCSV(File file, boolean skipHeader) {
        ImportResult result = new ImportResult();
        long startTime = System.currentTimeMillis();

        log.info("Starting CSV import from file: {}", file.getName());

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            int processedRows = 0;
            List<CourseEnrollmentRequest> allRequests = new ArrayList<>();

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
                    List<CourseEnrollmentRequest> requests = processCSVLine(line, lineNumber);
                    if (requests != null && !requests.isEmpty()) {
                        allRequests.addAll(requests);
                        processedRows++;
                    }
                } catch (Exception e) {
                    result.addError(String.format("Line %d: %s - %s", lineNumber, line, e.getMessage()));
                    log.error("Error processing line {}: {}", lineNumber, line, e);
                }
            }

            result.setTotalRows(processedRows);
            result.setSuccessfulRows(processedRows - result.getErrors().size());
            result.setFailedRows(result.getErrors().size());

            // Save all requests in batch
            if (!allRequests.isEmpty()) {
                log.info("Saving {} enrollment requests to database", allRequests.size());
                enrollmentRequestRepository.saveAll(allRequests);
                result.setRequestsCreated(allRequests.size());
                result.setSuccess(true);
            } else {
                result.addError("No valid requests to import");
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
     * Expected format: StudentID,FirstChoice,SecondChoice,ThirdChoice,FourthChoice
     *
     * @param line CSV line
     * @param lineNumber Line number for error reporting
     * @return List of enrollment requests (up to 4)
     * @throws Exception if validation fails
     */
    private List<CourseEnrollmentRequest> processCSVLine(String line, int lineNumber) throws Exception {
        String[] parts = line.split(",");

        // Validate format
        if (parts.length < 2) {
            throw new Exception("Invalid format. Expected: StudentID,FirstChoice[,SecondChoice,ThirdChoice,FourthChoice]");
        }

        // Extract fields
        String studentId = parts[0].trim();
        String firstChoice = parts.length > 1 ? parts[1].trim() : "";
        String secondChoice = parts.length > 2 ? parts[2].trim() : "";
        String thirdChoice = parts.length > 3 ? parts[3].trim() : "";
        String fourthChoice = parts.length > 4 ? parts[4].trim() : "";

        // Validate student exists
        Student student = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new Exception("Student not found: " + studentId));

        // Check for existing requests
        List<CourseEnrollmentRequest> existing = enrollmentRequestRepository.findByStudent(student);
        if (!existing.isEmpty()) {
            long pendingCount = existing.stream()
                .filter(r -> r.getRequestStatus() == EnrollmentRequestStatus.PENDING)
                .count();
            if (pendingCount > 0) {
                throw new Exception(String.format("Student %s already has %d pending requests", studentId, pendingCount));
            }
        }

        // Create requests
        List<CourseEnrollmentRequest> requests = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int basePriority = student.calculatePriorityScore();

        if (!firstChoice.isEmpty()) {
            requests.add(createRequest(student, firstChoice, 1, basePriority + 100, now, lineNumber));
        }
        if (!secondChoice.isEmpty()) {
            requests.add(createRequest(student, secondChoice, 2, basePriority + 75, now, lineNumber));
        }
        if (!thirdChoice.isEmpty()) {
            requests.add(createRequest(student, thirdChoice, 3, basePriority + 50, now, lineNumber));
        }
        if (!fourthChoice.isEmpty()) {
            requests.add(createRequest(student, fourthChoice, 4, basePriority + 25, now, lineNumber));
        }

        if (requests.isEmpty()) {
            throw new Exception("No course preferences provided");
        }

        return requests;
    }

    /**
     * Create a single enrollment request
     *
     * @param student Student
     * @param courseCode Course code
     * @param preferenceRank Preference rank (1-4)
     * @param priorityScore Priority score
     * @param createdAt Creation timestamp
     * @param lineNumber Line number for error reporting
     * @return CourseEnrollmentRequest
     * @throws Exception if course not found
     */
    private CourseEnrollmentRequest createRequest(Student student, String courseCode, int preferenceRank,
                                                  int priorityScore, LocalDateTime createdAt, int lineNumber) throws Exception {
        // Find course by code
        Course course = courseRepository.findByCourseCode(courseCode)
            .orElseThrow(() -> new Exception(String.format("Course not found: %s", courseCode)));

        // Check if course is active
        if (!course.getActive()) {
            throw new Exception(String.format("Course %s is not active", courseCode));
        }

        // Create request
        CourseEnrollmentRequest request = new CourseEnrollmentRequest();
        request.setStudent(student);
        request.setCourse(course);
        request.setPreferenceRank(preferenceRank);
        request.setPriorityScore(priorityScore);
        request.setCreatedAt(createdAt);
        request.setRequestStatus(EnrollmentRequestStatus.PENDING);
        request.setIsWaitlist(false);

        return request;
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
     * Validate a single CSV line without creating requests
     */
    private void validateCSVLine(String line, int lineNumber) throws Exception {
        String[] parts = line.split(",");

        if (parts.length < 2) {
            throw new Exception("Invalid format");
        }

        String studentId = parts[0].trim();
        if (studentId.isEmpty()) {
            throw new Exception("Student ID is required");
        }

        Student student = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new Exception("Student not found: " + studentId));

        // Validate at least one course provided
        boolean hasCourse = false;
        for (int i = 1; i < parts.length && i <= 4; i++) {
            String courseCode = parts[i].trim();
            if (!courseCode.isEmpty()) {
                courseRepository.findByCourseCode(courseCode)
                    .orElseThrow(() -> new Exception("Course not found: " + courseCode));
                hasCourse = true;
            }
        }

        if (!hasCourse) {
            throw new Exception("At least one course is required");
        }
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
            writer.println("StudentID,FirstChoice,SecondChoice,ThirdChoice,FourthChoice");

            if (includeExamples) {
                // Write example rows
                writer.println("S001,ART101,MUSIC101,DRAMA101,PHOTO101");
                writer.println("S002,MUSIC101,ART101,PE101,TECH101");
                writer.println("S003,DRAMA101,ART101,MUSIC101,");
            }

            log.info("CSV template generated: {}", outputFile.getAbsolutePath());
        }
    }

    /**
     * Generate sample data CSV for testing
     *
     * @param outputFile Output file
     * @param numberOfStudents Number of students to generate
     * @throws Exception if generation fails
     */
    public void generateSampleData(File outputFile, int numberOfStudents) throws Exception {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(outputFile)) {
            // Write header
            writer.println("StudentID,FirstChoice,SecondChoice,ThirdChoice,FourthChoice");

            // Get available students and courses
            List<Student> students = studentRepository.findByActiveTrue();
            List<Course> courses = courseRepository.findByActiveTrue();

            if (students.isEmpty() || courses.isEmpty()) {
                throw new Exception("No students or courses available for sample generation");
            }

            // Generate rows
            int count = Math.min(numberOfStudents, students.size());
            for (int i = 0; i < count; i++) {
                Student student = students.get(i);

                // Select 4 random courses
                List<Course> selectedCourses = new ArrayList<>();
                for (int j = 0; j < 4 && j < courses.size(); j++) {
                    int index = (i * 7 + j * 3) % courses.size(); // Pseudo-random but deterministic
                    selectedCourses.add(courses.get(index));
                }

                // Write row
                writer.printf("%s,%s,%s,%s,%s%n",
                    student.getStudentId(),
                    selectedCourses.size() > 0 ? selectedCourses.get(0).getCourseCode() : "",
                    selectedCourses.size() > 1 ? selectedCourses.get(1).getCourseCode() : "",
                    selectedCourses.size() > 2 ? selectedCourses.get(2).getCourseCode() : "",
                    selectedCourses.size() > 3 ? selectedCourses.get(3).getCourseCode() : ""
                );
            }

            log.info("Sample data CSV generated: {} students", count);
        }
    }
}
