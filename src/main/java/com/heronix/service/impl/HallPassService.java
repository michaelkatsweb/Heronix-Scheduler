package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.domain.HallPassSession.Destination;
import com.heronix.model.domain.HallPassSession.SessionStatus;
import com.heronix.repository.*;
import com.heronix.service.NotificationService;
import com.heronix.service.impl.FacialRecognitionService.FaceMatchResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Hall Pass Service
 * Handles hall pass workflow with QR code and facial recognition verification.
 *
 * Features:
 * - Two-factor authentication for departure and return
 * - Automatic duration tracking
 * - Multiple destination support
 * - Overdue detection and alerts
 * - Parent notifications for departures and returns
 * - Complete audit trail
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - QR Attendance System Phase 1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HallPassService {

    private final StudentRepository studentRepository;
    private final HallPassSessionRepository hallPassRepository;
    private final TeacherRepository teacherRepository;
    private final FacialRecognitionService facialRecognitionService;
    private final NotificationService notificationService;

    @Value("${hall.pass.enabled:true}")
    private boolean hallPassEnabled;

    @Value("${hall.pass.max.duration.minutes:15}")
    private int maxDurationMinutes;

    @Value("${hall.pass.overdue.alert.enabled:true}")
    private boolean overdueAlertEnabled;

    @Value("${facial.recognition.confidence.threshold:0.80}")
    private double faceMatchThreshold;

    @Value("${notification.parent.enabled:true}")
    private boolean parentNotificationEnabled;

    /**
     * Start a new hall pass session (student departing)
     *
     * @param qrCodeData QR code scanned from student ID card
     * @param capturedPhoto Photo captured during departure scan
     * @param destination Where student is going
     * @param teacherId Teacher approving the hall pass
     * @param period Current period number
     * @param departureRoom Room student is leaving from
     * @return Hall pass result with session details
     */
    @Transactional
    public HallPassResult startHallPass(
            String qrCodeData,
            byte[] capturedPhoto,
            Destination destination,
            Long teacherId,
            Integer period,
            String departureRoom) {

        log.info("Starting hall pass: qrCode={}, destination={}, room={}",
            qrCodeData, destination, departureRoom);

        // Step 1: Validate hall pass system is enabled
        if (!hallPassEnabled) {
            return HallPassResult.failure("Hall pass system is currently disabled");
        }

        // ✅ NULL SAFE: Validate QR code data before lookup
        if (qrCodeData == null || qrCodeData.trim().isEmpty()) {
            return HallPassResult.failure("QR code data is required");
        }

        // Step 2: Find student by QR code
        Student student = studentRepository.findByQrCodeId(qrCodeData)
            .orElse(null);

        if (student == null) {
            log.warn("Invalid QR code scanned: {}", qrCodeData);
            return HallPassResult.failure("Invalid QR code - student not found");
        }

        // Step 3: Check for existing active hall pass
        // ✅ NULL SAFE: Check student ID exists before querying
        if (student.getId() == null) {
            return HallPassResult.failure("Student ID is missing");
        }

        Optional<HallPassSession> existingPass = hallPassRepository.findActiveByStudentId(student.getId());
        if (existingPass.isPresent()) {
            log.warn("Student {} already has an active hall pass", student.getId());
            // ✅ NULL SAFE: Safe extraction with null checks for destination and time
            String errorMessage = existingPass
                .map(pass -> String.format("Student already has an active hall pass to %s since %s",
                    pass.getDestination() != null ? pass.getDestination().getDisplayName() : "Unknown",
                    pass.getDepartureTime() != null ? pass.getDepartureTime().toString() : "Unknown"))
                .orElse("Student already has an active hall pass");
            return HallPassResult.failure(errorMessage);
        }

        // Step 4: Perform facial recognition verification
        FaceMatchResult faceMatch = null;
        if (capturedPhoto != null && capturedPhoto.length > 0 && student.getFaceSignature() != null) {
            faceMatch = facialRecognitionService.matchFace(
                student.getFaceSignature(),
                capturedPhoto
            );

            // ✅ NULL SAFE: Check faceMatch is not null before accessing its methods
            // Lower threshold for hall passes (0.80 instead of 0.85)
            if (faceMatch != null && faceMatch.getMatchScore() < (faceMatchThreshold - 0.10)) {
                log.warn("Face match failed for hall pass departure: score={}",
                    faceMatch.getMatchScore());
                return HallPassResult.failure(
                    String.format("Identity verification failed (confidence: %.2f)", faceMatch.getMatchScore())
                );
            }
        }

        // Step 5: Create hall pass session
        // ✅ NULL SAFE: Validate teacherId and lookup teacher safely
        Teacher teacher = null;
        if (teacherId != null) {
            teacher = teacherRepository.findById(teacherId).orElse(null);
            if (teacher == null) {
                log.warn("Teacher not found with ID: {}", teacherId);
            }
        }

        HallPassSession session = HallPassSession.builder()
            .student(student)
            .departureTime(LocalDateTime.now())
            .destination(destination)
            .departureRoom(departureRoom)
            .teacher(teacher)
            .period(period)
            .departurePhoto(capturedPhoto)
            .departureFaceMatchScore(faceMatch != null ? faceMatch.getMatchScore() : null)
            .status(SessionStatus.ACTIVE)
            .parentNotifiedDeparture(false)
            .parentNotifiedReturn(false)
            .overdueAlertSent(false)
            .build();

        session = hallPassRepository.save(session);

        // Step 6: Send departure notification to parent
        if (parentNotificationEnabled) {
            sendDepartureNotification(student, session);
        }

        // Step 7: Return success result
        // ✅ NULL SAFE: Check destination is not null before accessing getDisplayName()
        log.info("Hall pass started for student {} to {}", student.getId(), destination);
        String destinationName = (destination != null) ? destination.getDisplayName() : "Unknown";
        return HallPassResult.success(student, session, faceMatch,
            String.format("Hall pass approved - %s", destinationName));
    }

    /**
     * End a hall pass session (student returning)
     *
     * @param qrCodeData QR code scanned from student ID card
     * @param capturedPhoto Photo captured during return scan
     * @param returnRoom Room student is returning to
     * @return Hall pass result with completion details
     */
    @Transactional
    public HallPassResult endHallPass(
            String qrCodeData,
            byte[] capturedPhoto,
            String returnRoom) {

        log.info("Ending hall pass: qrCode={}, returnRoom={}", qrCodeData, returnRoom);

        // ✅ NULL SAFE: Validate QR code data before lookup
        if (qrCodeData == null || qrCodeData.trim().isEmpty()) {
            return HallPassResult.failure("QR code data is required");
        }

        // Step 1: Find student by QR code
        Student student = studentRepository.findByQrCodeId(qrCodeData)
            .orElse(null);

        if (student == null) {
            log.warn("Invalid QR code scanned: {}", qrCodeData);
            return HallPassResult.failure("Invalid QR code - student not found");
        }

        // Step 2: Find active hall pass session
        Optional<HallPassSession> activePass = hallPassRepository.findActiveByStudentId(student.getId());
        if (!activePass.isPresent()) {
            log.warn("No active hall pass found for student {}", student.getId());
            return HallPassResult.failure("No active hall pass found for this student");
        }

        HallPassSession session = activePass.get();

        // Step 3: Perform facial recognition verification
        FaceMatchResult faceMatch = null;
        if (capturedPhoto != null && capturedPhoto.length > 0 && student.getFaceSignature() != null) {
            faceMatch = facialRecognitionService.matchFace(
                student.getFaceSignature(),
                capturedPhoto
            );

            // ✅ NULL SAFE: Check faceMatch is not null before accessing its methods
            // Log low confidence but allow return (better to let them back than leave them stranded)
            if (faceMatch != null && faceMatch.getMatchScore() < (faceMatchThreshold - 0.10)) {
                log.warn("Low confidence face match on return: score={}", faceMatch.getMatchScore());
                session.setNotes(
                    (session.getNotes() != null ? session.getNotes() + "\n" : "") +
                    String.format("Low confidence return scan: %.2f", faceMatch.getMatchScore())
                );
            }
        }

        // Step 4: Complete the session
        // ✅ NULL SAFE: Validate session is not null before completing
        if (session == null) {
            log.error("Session is null, cannot complete");
            return HallPassResult.failure("Internal error - session is null");
        }

        session.completeSession(
            capturedPhoto,
            faceMatch != null ? faceMatch.getMatchScore() : null
        );
        session.setArrivalRoom(returnRoom);

        session = hallPassRepository.save(session);

        // Step 5: Send return notification to parent
        if (parentNotificationEnabled) {
            sendReturnNotification(student, session);
        }

        // Step 6: Return success result
        log.info("Hall pass completed for student {} - duration: {} minutes",
            student.getId(), session.getDurationMinutes());

        return HallPassResult.completed(student, session, faceMatch,
            String.format("Hall pass completed - Duration: %s", session.getFormattedDuration()));
    }

    /**
     * Get all active hall pass sessions
     */
    public List<HallPassSession> getActiveSessions() {
        List<HallPassSession> sessions = hallPassRepository.findAllActive();
        // ✅ NULL SAFE: Validate repository result
        if (sessions == null) {
            log.warn("Repository returned null for active sessions");
            return java.util.Collections.emptyList();
        }
        return sessions;
    }

    /**
     * Get hall pass history for a student
     */
    public List<HallPassSession> getStudentHistory(Long studentId) {
        // ✅ NULL SAFE: Validate studentId parameter
        if (studentId == null) {
            log.warn("Cannot get hall pass history for null studentId");
            return java.util.Collections.emptyList();
        }

        List<HallPassSession> history = hallPassRepository.findByStudent_IdOrderByDepartureTimeDesc(studentId);
        // ✅ NULL SAFE: Validate repository result
        if (history == null) {
            log.warn("Repository returned null for student {} history", studentId);
            return java.util.Collections.emptyList();
        }
        return history;
    }

    /**
     * Check for overdue hall passes (scheduled task - runs every 2 minutes)
     */
    @Scheduled(fixedDelay = 120000) // 2 minutes
    @Transactional
    public void checkOverdueSessions() {
        if (!overdueAlertEnabled) {
            return;
        }

        LocalDateTime overdueThreshold = LocalDateTime.now().minusMinutes(maxDurationMinutes);
        List<HallPassSession> overdueSessions = hallPassRepository.findOverdueSessions(overdueThreshold);

        // ✅ NULL SAFE: Validate repository result
        if (overdueSessions == null) {
            log.warn("Repository returned null for overdue sessions");
            return;
        }

        for (HallPassSession session : overdueSessions) {
            // ✅ NULL SAFE: Skip null sessions and validate session status
            if (session == null || session.getStatus() == null) {
                continue;
            }

            // Mark as overdue
            if (session.getStatus() == SessionStatus.ACTIVE) {
                session.markOverdue();
                hallPassRepository.save(session);

                // Send overdue alert (only once)
                // ✅ NULL SAFE: Check overdueAlertSent flag is not null
                Boolean alertSent = session.getOverdueAlertSent();
                if (alertSent == null || !alertSent) {
                    sendOverdueAlert(session);
                    session.setOverdueAlertSent(true);
                    hallPassRepository.save(session);
                }
            }
        }

        if (!overdueSessions.isEmpty()) {
            log.warn("Found {} overdue hall pass sessions", overdueSessions.size());
        }
    }

    /**
     * Send departure notification to parent
     */
    private void sendDepartureNotification(Student student, HallPassSession session) {
        try {
            // ✅ NULL SAFE: Check student and session exist
            if (student == null || session == null) {
                log.warn("Cannot send departure notification - student or session is null");
                return;
            }

            String title = "Hall Pass - Departure";
            // ✅ NULL SAFE: Safe field extraction with defaults
            String firstName = student.getFirstName() != null ? student.getFirstName() : "Student";
            String lastName = student.getLastName() != null ? student.getLastName() : "";
            String timeStr = session.getDepartureTime() != null ? session.getDepartureTime().toLocalTime().toString() : "Unknown";
            String destinationStr = (session.getDestination() != null) ? session.getDestination().getDisplayName() : "Unknown";

            String message = String.format(
                "%s %s left class at %s to go to %s",
                firstName,
                lastName,
                timeStr,
                destinationStr
            );

            notificationService.createDetailedNotification(
                Notification.NotificationType.SYSTEM_ALERT,
                title,
                message,
                2, // Priority: Medium
                null, // targetUserId
                "PARENT", // targetRole
                "HALL_PASS",
                session.getId(),
                null
            );

            session.setParentNotifiedDeparture(true);
            hallPassRepository.save(session);

            log.info("Departure notification sent for student {}", student.getId());

        } catch (Exception e) {
            log.error("Failed to send departure notification for student {}", student.getId(), e);
        }
    }

    /**
     * Send return notification to parent
     */
    private void sendReturnNotification(Student student, HallPassSession session) {
        try {
            // ✅ NULL SAFE: Check student and session exist
            if (student == null || session == null) {
                log.warn("Cannot send return notification - student or session is null");
                return;
            }

            String title = "Hall Pass - Return";
            // ✅ NULL SAFE: Safe field extraction with defaults
            String firstName = student.getFirstName() != null ? student.getFirstName() : "Student";
            String lastName = student.getLastName() != null ? student.getLastName() : "";
            String timeStr = (session.getReturnTime() != null) ? session.getReturnTime().toLocalTime().toString() : "Unknown";
            String durationStr = (session.getFormattedDuration() != null) ? session.getFormattedDuration() : "Unknown";

            String message = String.format(
                "%s %s returned to class at %s (Duration: %s)",
                firstName,
                lastName,
                timeStr,
                durationStr
            );

            notificationService.createDetailedNotification(
                Notification.NotificationType.SYSTEM_ALERT,
                title,
                message,
                2, // Priority: Medium
                null, // targetUserId
                "PARENT", // targetRole
                "HALL_PASS",
                session.getId(),
                null
            );

            session.setParentNotifiedReturn(true);
            hallPassRepository.save(session);

            log.info("Return notification sent for student {}", student.getId());

        } catch (Exception e) {
            log.error("Failed to send return notification for student {}", student.getId(), e);
        }
    }

    /**
     * Send overdue alert
     */
    private void sendOverdueAlert(HallPassSession session) {
        try {
            // ✅ NULL SAFE: Check session exists
            if (session == null) {
                log.warn("Cannot send overdue alert - session is null");
                return;
            }

            Student student = session.getStudent();
            // ✅ NULL SAFE: Check student exists
            if (student == null) {
                log.warn("Cannot send overdue alert - student is null for session {}", session.getId());
                return;
            }

            String title = "Hall Pass - OVERDUE Alert";
            // ✅ NULL SAFE: Safe field extraction with defaults
            String firstName = student.getFirstName() != null ? student.getFirstName() : "Student";
            String lastName = student.getLastName() != null ? student.getLastName() : "";
            String destinationStr = (session.getDestination() != null) ? session.getDestination().getDisplayName() : "Unknown";
            String timeStr = (session.getDepartureTime() != null) ? session.getDepartureTime().toLocalTime().toString() : "Unknown";

            // ✅ NULL SAFE: Safe calculation of duration with null check
            long durationMinutes = 0;
            if (session.getDepartureTime() != null) {
                durationMinutes = session.calculateDuration();
            }

            String message = String.format(
                "ALERT: %s %s hall pass is overdue. Departed to %s at %s (%d minutes ago)",
                firstName,
                lastName,
                destinationStr,
                timeStr,
                durationMinutes
            );

            // Notify parent
            notificationService.createDetailedNotification(
                Notification.NotificationType.CAPACITY_WARNING,
                title,
                message,
                4, // Priority: High
                null, // targetUserId
                "PARENT", // targetRole
                "HALL_PASS",
                session.getId(),
                null
            );

            // Notify admin/teacher
            notificationService.createDetailedNotification(
                Notification.NotificationType.CAPACITY_WARNING,
                title,
                message,
                4, // Priority: High
                null, // targetUserId
                "ADMIN", // targetRole
                "HALL_PASS",
                session.getId(),
                null
            );

            log.warn("Overdue alert sent for student {} - {} minutes overdue",
                student.getId(), session.calculateDuration() - maxDurationMinutes);

        } catch (Exception e) {
            log.error("Failed to send overdue alert for session {}", session.getId(), e);
        }
    }

    // ========================================================================
    // DTOs
    // ========================================================================

    /**
     * Hall pass result DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HallPassResult {
        private boolean success;
        private String status; // SUCCESS, COMPLETED, FAILED
        private String message;
        private Student student;
        private HallPassSession session;
        private FaceMatchResult faceMatchResult;
        private LocalDateTime timestamp;

        public static HallPassResult success(Student student, HallPassSession session,
                                            FaceMatchResult faceMatch, String message) {
            return HallPassResult.builder()
                .success(true)
                .status("SUCCESS")
                .message(message)
                .student(student)
                .session(session)
                .faceMatchResult(faceMatch)
                .timestamp(LocalDateTime.now())
                .build();
        }

        public static HallPassResult completed(Student student, HallPassSession session,
                                              FaceMatchResult faceMatch, String message) {
            return HallPassResult.builder()
                .success(true)
                .status("COMPLETED")
                .message(message)
                .student(student)
                .session(session)
                .faceMatchResult(faceMatch)
                .timestamp(LocalDateTime.now())
                .build();
        }

        public static HallPassResult failure(String message) {
            return HallPassResult.builder()
                .success(false)
                .status("FAILED")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }
}
