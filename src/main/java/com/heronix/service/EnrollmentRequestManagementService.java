package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.CourseEnrollmentRequest;
import com.heronix.model.domain.Student;
import com.heronix.model.enums.EnrollmentRequestStatus;
import com.heronix.repository.CourseEnrollmentRequestRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.StudentRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enrollment Request Management Service
 *
 * Provides comprehensive management capabilities for enrollment requests:
 * - Search and filter requests
 * - View request details
 * - Edit and update requests
 * - Cancel and delete requests
 * - Bulk operations
 * - Statistics and analytics
 *
 * Used by EnrollmentRequestManagementController to provide administrators
 * with tools to review and manage all enrollment requests before running
 * the assignment wizard.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - November 21, 2025
 */
@Slf4j
@Service
public class EnrollmentRequestManagementService {

    @Autowired
    private CourseEnrollmentRequestRepository requestRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    // ========================================================================
    // SEARCH AND RETRIEVAL
    // ========================================================================

    /**
     * Get all enrollment requests
     *
     * @return List of all requests
     */
    public List<CourseEnrollmentRequest> getAllRequests() {
        log.info("Retrieving all enrollment requests");
        return requestRepository.findAll();
    }

    /**
     * Get requests by status
     *
     * @param status Request status
     * @return List of requests with given status
     */
    public List<CourseEnrollmentRequest> getRequestsByStatus(EnrollmentRequestStatus status) {
        log.info("Retrieving requests with status: {}", status);
        return requestRepository.findByRequestStatus(status);
    }

    /**
     * Get requests for a specific student
     *
     * @param student Student entity
     * @return List of student's requests
     */
    public List<CourseEnrollmentRequest> getRequestsByStudent(Student student) {
        return requestRepository.findByStudent(student);
    }

    /**
     * Get requests for a specific course
     *
     * @param course Course entity
     * @return List of requests for the course
     */
    public List<CourseEnrollmentRequest> getRequestsByCourse(Course course) {
        return requestRepository.findByCourse(course);
    }

    /**
     * Get request by ID
     *
     * @param id Request ID
     * @return Optional containing request if found
     */
    public Optional<CourseEnrollmentRequest> getRequestById(Long id) {
        return requestRepository.findById(id);
    }

    /**
     * Search requests using multiple criteria
     *
     * @param criteria Search criteria
     * @return List of matching requests
     */
    public List<CourseEnrollmentRequest> searchRequests(SearchCriteria criteria) {
        log.info("Searching requests with criteria: {}", criteria);

        List<CourseEnrollmentRequest> results = requestRepository.findAll();

        // Filter by student name or ID
        if (criteria.getStudentNameOrId() != null && !criteria.getStudentNameOrId().trim().isEmpty()) {
            String searchTerm = criteria.getStudentNameOrId().toLowerCase().trim();
            results = results.stream()
                .filter(r -> {
                    Student s = r.getStudent();
                    return s.getStudentId().toLowerCase().contains(searchTerm) ||
                           s.getFirstName().toLowerCase().contains(searchTerm) ||
                           s.getLastName().toLowerCase().contains(searchTerm) ||
                           s.getFullName().toLowerCase().contains(searchTerm);
                })
                .collect(Collectors.toList());
        }

        // Filter by course code or name
        if (criteria.getCourseCodeOrName() != null && !criteria.getCourseCodeOrName().trim().isEmpty()) {
            String searchTerm = criteria.getCourseCodeOrName().toLowerCase().trim();
            results = results.stream()
                .filter(r -> {
                    Course c = r.getCourse();
                    return c.getCourseCode().toLowerCase().contains(searchTerm) ||
                           c.getCourseName().toLowerCase().contains(searchTerm);
                })
                .collect(Collectors.toList());
        }

        // Filter by status
        if (criteria.getStatus() != null) {
            results = results.stream()
                .filter(r -> r.getRequestStatus() == criteria.getStatus())
                .collect(Collectors.toList());
        }

        // Filter by grade level
        if (criteria.getGradeLevel() != null) {
            results = results.stream()
                .filter(r -> criteria.getGradeLevel().equals(r.getStudent().getGradeLevel()))
                .collect(Collectors.toList());
        }

        // Filter by preference rank
        if (criteria.getPreferenceRank() != null) {
            results = results.stream()
                .filter(r -> criteria.getPreferenceRank().equals(r.getPreferenceRank()))
                .collect(Collectors.toList());
        }

        // Filter by date range
        if (criteria.getStartDate() != null) {
            results = results.stream()
                .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(criteria.getStartDate()))
                .collect(Collectors.toList());
        }

        if (criteria.getEndDate() != null) {
            results = results.stream()
                .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isAfter(criteria.getEndDate()))
                .collect(Collectors.toList());
        }

        log.info("Search returned {} results", results.size());
        return results;
    }

    // ========================================================================
    // UPDATE OPERATIONS
    // ========================================================================

    /**
     * Update an existing enrollment request
     *
     * @param request Updated request
     * @return Updated request
     */
    @Transactional
    public CourseEnrollmentRequest updateRequest(CourseEnrollmentRequest request) {
        log.info("Updating enrollment request ID: {}", request.getId());

        if (request.getId() == null) {
            throw new IllegalArgumentException("Request ID cannot be null for update");
        }

        CourseEnrollmentRequest existing = requestRepository.findById(request.getId())
            .orElseThrow(() -> new IllegalArgumentException("Request not found: " + request.getId()));

        // Update fields
        existing.setPreferenceRank(request.getPreferenceRank());
        existing.setPriorityScore(request.getPriorityScore());
        existing.setRequestStatus(request.getRequestStatus());
        existing.setIsWaitlist(request.getIsWaitlist());

        return requestRepository.save(existing);
    }

    /**
     * Update preference rank for a request
     *
     * @param requestId Request ID
     * @param newPreferenceRank New preference rank (1-4)
     */
    @Transactional
    public void updatePreferenceRank(Long requestId, int newPreferenceRank) {
        log.info("Updating preference rank for request {} to {}", requestId, newPreferenceRank);

        if (newPreferenceRank < 1 || newPreferenceRank > 4) {
            throw new IllegalArgumentException("Preference rank must be between 1 and 4");
        }

        CourseEnrollmentRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        request.setPreferenceRank(newPreferenceRank);
        requestRepository.save(request);
    }

    /**
     * Update priority score for a request
     *
     * @param requestId Request ID
     * @param newPriorityScore New priority score
     * @param justification Reason for manual priority adjustment
     */
    @Transactional
    public void updatePriorityScore(Long requestId, int newPriorityScore, String justification) {
        log.info("Updating priority score for request {} to {} (Reason: {})",
                 requestId, newPriorityScore, justification);

        CourseEnrollmentRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        request.setPriorityScore(newPriorityScore);
        // Note: In future, could add audit log for manual priority changes
        requestRepository.save(request);
    }

    // ========================================================================
    // CANCEL AND DELETE OPERATIONS
    // ========================================================================

    /**
     * Cancel an enrollment request
     *
     * @param requestId Request ID
     * @param reason Cancellation reason
     */
    @Transactional
    public void cancelRequest(Long requestId, String reason) {
        log.info("Cancelling enrollment request ID: {} (Reason: {})", requestId, reason);

        CourseEnrollmentRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        request.setRequestStatus(EnrollmentRequestStatus.CANCELLED);
        // Note: In future, could add cancellation reason field
        requestRepository.save(request);
    }

    /**
     * Delete an enrollment request
     *
     * @param requestId Request ID
     */
    @Transactional
    public void deleteRequest(Long requestId) {
        log.info("Deleting enrollment request ID: {}", requestId);

        if (!requestRepository.existsById(requestId)) {
            throw new IllegalArgumentException("Request not found: " + requestId);
        }

        requestRepository.deleteById(requestId);
    }

    // ========================================================================
    // BULK OPERATIONS
    // ========================================================================

    /**
     * Cancel multiple requests at once
     *
     * @param requestIds List of request IDs to cancel
     * @param reason Cancellation reason
     * @return Number of requests cancelled
     */
    @Transactional
    public int bulkCancelRequests(List<Long> requestIds, String reason) {
        log.info("Bulk cancelling {} requests (Reason: {})", requestIds.size(), reason);

        int cancelledCount = 0;
        for (Long id : requestIds) {
            try {
                cancelRequest(id, reason);
                cancelledCount++;
            } catch (Exception e) {
                log.error("Failed to cancel request {}: {}", id, e.getMessage());
            }
        }

        log.info("Successfully cancelled {} of {} requests", cancelledCount, requestIds.size());
        return cancelledCount;
    }

    /**
     * Delete multiple requests at once
     *
     * @param requestIds List of request IDs to delete
     * @return Number of requests deleted
     */
    @Transactional
    public int bulkDeleteRequests(List<Long> requestIds) {
        log.info("Bulk deleting {} requests", requestIds.size());

        int deletedCount = 0;
        for (Long id : requestIds) {
            try {
                deleteRequest(id);
                deletedCount++;
            } catch (Exception e) {
                log.error("Failed to delete request {}: {}", id, e.getMessage());
            }
        }

        log.info("Successfully deleted {} of {} requests", deletedCount, requestIds.size());
        return deletedCount;
    }

    /**
     * Change status for multiple requests
     *
     * @param requestIds List of request IDs
     * @param newStatus New status to apply
     * @return Number of requests updated
     */
    @Transactional
    public int bulkUpdateStatus(List<Long> requestIds, EnrollmentRequestStatus newStatus) {
        log.info("Bulk updating {} requests to status: {}", requestIds.size(), newStatus);

        int updatedCount = 0;
        for (Long id : requestIds) {
            try {
                CourseEnrollmentRequest request = requestRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Request not found: " + id));
                request.setRequestStatus(newStatus);
                requestRepository.save(request);
                updatedCount++;
            } catch (Exception e) {
                log.error("Failed to update request {}: {}", id, e.getMessage());
            }
        }

        log.info("Successfully updated {} of {} requests", updatedCount, requestIds.size());
        return updatedCount;
    }

    // ========================================================================
    // STATISTICS AND ANALYTICS
    // ========================================================================

    /**
     * Get comprehensive statistics about enrollment requests
     *
     * @return Request statistics
     */
    public RequestStatistics getStatistics() {
        log.info("Calculating enrollment request statistics");

        List<CourseEnrollmentRequest> allRequests = requestRepository.findAll();

        RequestStatistics stats = new RequestStatistics();
        stats.setTotalRequests(allRequests.size());

        // Count by status
        stats.setPendingRequests(allRequests.stream()
            .filter(r -> r.getRequestStatus() == EnrollmentRequestStatus.PENDING)
            .count());

        stats.setAssignedRequests(allRequests.stream()
            .filter(r -> r.getRequestStatus() == EnrollmentRequestStatus.APPROVED)
            .count());

        stats.setCancelledRequests(allRequests.stream()
            .filter(r -> r.getRequestStatus() == EnrollmentRequestStatus.CANCELLED)
            .count());

        stats.setWaitlistRequests(allRequests.stream()
            .filter(r -> r.getIsWaitlist())
            .count());

        // Count by grade level
        Map<String, Long> byGrade = new HashMap<>();
        allRequests.stream()
            .filter(r -> r.getStudent() != null && r.getStudent().getGradeLevel() != null)
            .forEach(r -> {
                String grade = r.getStudent().getGradeLevel();
                byGrade.put(grade, byGrade.getOrDefault(grade, 0L) + 1);
            });
        stats.setRequestsByGrade(byGrade);

        // Top requested courses
        Map<Long, Long> courseCounts = allRequests.stream()
            .filter(r -> r.getCourse() != null)
            .collect(Collectors.groupingBy(
                r -> r.getCourse().getId(),
                Collectors.counting()
            ));

        List<CourseRequestCount> topCourses = courseCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(10)
            .map(entry -> {
                Course course = courseRepository.findById(entry.getKey()).orElse(null);
                if (course != null) {
                    return new CourseRequestCount(
                        course.getCourseCode(),
                        course.getCourseName(),
                        entry.getValue()
                    );
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        stats.setTopRequestedCourses(topCourses);

        log.info("Statistics calculated: {} total requests", stats.getTotalRequests());
        return stats;
    }

    /**
     * Get most requested courses
     *
     * @param limit Number of courses to return
     * @return List of top requested courses
     */
    public List<CourseRequestCount> getMostRequestedCourses(int limit) {
        List<CourseEnrollmentRequest> allRequests = requestRepository.findAll();

        Map<Long, Long> courseCounts = allRequests.stream()
            .filter(r -> r.getCourse() != null)
            .collect(Collectors.groupingBy(
                r -> r.getCourse().getId(),
                Collectors.counting()
            ));

        return courseCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                Course course = courseRepository.findById(entry.getKey()).orElse(null);
                if (course != null) {
                    return new CourseRequestCount(
                        course.getCourseCode(),
                        course.getCourseName(),
                        entry.getValue()
                    );
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Get students who have no enrollment requests
     *
     * @return List of students without requests
     */
    public List<Student> getStudentsWithoutRequests() {
        log.info("Finding students without enrollment requests");

        List<Student> allStudents = studentRepository.findByActiveTrue();
        List<Student> studentsWithRequests = requestRepository.findAll().stream()
            .map(CourseEnrollmentRequest::getStudent)
            .distinct()
            .collect(Collectors.toList());

        List<Student> studentsWithout = allStudents.stream()
            .filter(s -> !studentsWithRequests.contains(s))
            .collect(Collectors.toList());

        log.info("Found {} students without enrollment requests", studentsWithout.size());
        return studentsWithout;
    }

    // ========================================================================
    // DTOs AND HELPER CLASSES
    // ========================================================================

    /**
     * Search criteria for filtering enrollment requests
     */
    @Data
    public static class SearchCriteria {
        private String studentNameOrId;
        private String courseCodeOrName;
        private EnrollmentRequestStatus status;
        private Integer gradeLevel;
        private Integer preferenceRank;
        private LocalDateTime startDate;
        private LocalDateTime endDate;

        public SearchCriteria() {
        }

        @Override
        public String toString() {
            return String.format(
                "SearchCriteria{student='%s', course='%s', status=%s, grade=%d, pref=%d}",
                studentNameOrId, courseCodeOrName, status, gradeLevel, preferenceRank
            );
        }
    }

    /**
     * Statistics about enrollment requests
     */
    @Data
    public static class RequestStatistics {
        private long totalRequests;
        private long pendingRequests;
        private long assignedRequests;
        private long cancelledRequests;
        private long waitlistRequests;
        private Map<String, Long> requestsByGrade;
        private List<CourseRequestCount> topRequestedCourses;

        public RequestStatistics() {
            this.requestsByGrade = new HashMap<>();
            this.topRequestedCourses = new ArrayList<>();
        }
    }

    /**
     * Count of requests for a specific course
     */
    @Data
    public static class CourseRequestCount {
        private String courseCode;
        private String courseName;
        private long requestCount;

        public CourseRequestCount(String courseCode, String courseName, long requestCount) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.requestCount = requestCount;
        }

        @Override
        public String toString() {
            return String.format("%s (%s): %d requests", courseName, courseCode, requestCount);
        }
    }
}
