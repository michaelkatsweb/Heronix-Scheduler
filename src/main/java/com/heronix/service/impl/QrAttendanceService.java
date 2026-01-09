package com.heronix.service.impl;

import com.heronix.model.domain.*;
import com.heronix.model.domain.QrAttendanceLog.VerificationStatus;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * QR Attendance Service
 * Handles QR code scanning with two-factor facial recognition verification for attendance tracking.
 *
 * Features:
 * - Two-factor authentication (QR code + facial recognition)
 * - Auto-verification when face match score exceeds threshold
 * - Admin verification workflow for low-confidence matches
 * - Duplicate scan detection
 * - Period-based attendance tracking
 * - Real-time parent notifications
 * - Complete audit trail
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - QR Attendance System Phase 1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QrAttendanceService {

    private final StudentRepository studentRepository;
    private final QrAttendanceLogRepository attendanceLogRepository;
    private final PeriodTimerRepository periodTimerRepository;
    private final TeacherRepository teacherRepository;
    private final FacialRecognitionService facialRecognitionService;
    private final NotificationService notificationService;

    @Value("${qr.attendance.enabled:true}")
    private boolean qrAttendanceEnabled;

    @Value("${qr.attendance.duplicate.window.minutes:5}")
    private int duplicateScanWindowMinutes;

    @Value("${qr.attendance.require.photo:true}")
    private boolean requirePhoto;

    @Value("${facial.recognition.confidence.threshold:0.85}")
    private double faceMatchThreshold;

    @Value("${notification.parent.enabled:true}")
    private boolean parentNotificationEnabled;

    /**
     * Process QR code scan for attendance
     *
     * @param qrCodeData QR code scanned from student ID card
     * @param capturedPhoto Photo captured during scan (for facial verification)
     * @param period Period number (null for arrival/dismissal)
     * @param teacherId Teacher ID (who's taking attendance)
     * @param roomNumber Room where scan occurred
     * @return Attendance result with verification status
     */
    @Transactional
    public AttendanceResult processQrScan(
            String qrCodeData,
            byte[] capturedPhoto,
            Integer period,
            Long teacherId,
            String roomNumber) {

        log.info("Processing QR scan: qrCode={}, period={}, room={}", qrCodeData, period, roomNumber);

        // Step 1: Validate QR attendance is enabled
        if (!qrAttendanceEnabled) {
            return AttendanceResult.failure("QR attendance is currently disabled");
        }

        // ✅ NULL SAFE: Validate QR code data before lookup
        if (qrCodeData == null || qrCodeData.trim().isEmpty()) {
            log.warn("QR code data is null or empty");
            return AttendanceResult.failure("QR code data is required");
        }

        // Step 2: Find student by QR code
        Student student = studentRepository.findByQrCodeId(qrCodeData)
            .orElse(null);

        if (student == null) {
            log.warn("Invalid QR code scanned: {}", qrCodeData);
            return AttendanceResult.failure("Invalid QR code - student not found");
        }

        // Step 3: Check if student has QR attendance enabled
        if (Boolean.FALSE.equals(student.getQrAttendanceEnabled())) {
            log.warn("QR attendance disabled for student: {}", student.getId());
            return AttendanceResult.failure("QR attendance is disabled for this student");
        }

        // Step 4: Check for duplicate scans
        // ✅ NULL SAFE: Validate student ID before querying
        if (student.getId() == null) {
            log.warn("Student has null ID");
            return AttendanceResult.failure("Student ID is missing");
        }

        LocalDateTime duplicateThreshold = LocalDateTime.now().minusMinutes(duplicateScanWindowMinutes);
        List<QrAttendanceLog> recentScans = attendanceLogRepository.findRecentScansForPeriod(
            student.getId(),
            period,
            duplicateThreshold
        );

        // ✅ NULL SAFE: Validate repository result
        if (recentScans == null) {
            log.warn("Repository returned null for recent scans");
            recentScans = java.util.Collections.emptyList();
        }

        if (!recentScans.isEmpty()) {
            log.warn("Duplicate scan detected for student {} in period {}", student.getId(), period);
            return AttendanceResult.failure(
                String.format("Duplicate scan - student already scanned %d minutes ago",
                    duplicateScanWindowMinutes)
            );
        }

        // Step 5: Validate photo if required
        if (requirePhoto && (capturedPhoto == null || capturedPhoto.length == 0)) {
            log.warn("Photo required but not provided for student {}", student.getId());
            return AttendanceResult.pendingPhoto(student, "Photo capture required for attendance verification");
        }

        // Step 6: Perform facial recognition verification
        FaceMatchResult faceMatch = null;
        if (capturedPhoto != null && capturedPhoto.length > 0) {
            if (student.getFaceSignature() == null) {
                log.warn("No face signature on file for student {}", student.getId());
                // First time - extract and store signature
                byte[] newSignature = facialRecognitionService.extractFaceSignature(capturedPhoto);
                // ✅ NULL SAFE: Validate facial recognition result
                if (newSignature == null) {
                    log.error("Failed to extract face signature for student {}", student.getId());
                    return AttendanceResult.failure("Failed to extract face signature - please try again");
                }
                student.setFaceSignature(newSignature);
                student.setPhotoData(capturedPhoto);
                studentRepository.save(student);

                faceMatch = FaceMatchResult.builder()
                    .success(true)
                    .matchScore(1.0)
                    .method("INITIAL_ENROLLMENT")
                    .message("Initial face signature created")
                    .timestamp(LocalDateTime.now())
                    .build();
            } else {
                // Match against stored signature
                faceMatch = facialRecognitionService.matchFace(
                    student.getFaceSignature(),
                    capturedPhoto
                );
            }
        }

        // Step 7: Determine verification status
        VerificationStatus verificationStatus;
        boolean autoVerified = false;

        if (faceMatch != null && faceMatch.isSuccess() && faceMatch.getMatchScore() >= faceMatchThreshold) {
            verificationStatus = VerificationStatus.AUTO_VERIFIED;
            autoVerified = true;
            log.info("Auto-verified attendance for student {} (score: {})",
                student.getId(), faceMatch.getMatchScore());
        } else if (faceMatch != null && faceMatch.getMatchScore() < (faceMatchThreshold - 0.15)) {
            // Very low confidence - flag for review
            verificationStatus = VerificationStatus.FLAGGED;
            log.warn("Low confidence face match for student {} (score: {})",
                student.getId(), faceMatch.getMatchScore());
        } else {
            // Medium confidence - pending admin review
            verificationStatus = VerificationStatus.PENDING;
            log.info("Pending admin review for student {} (score: {})",
                student.getId(), faceMatch != null ? faceMatch.getMatchScore() : "N/A");
        }

        // Step 8: Create attendance log
        // ✅ NULL SAFE: Safe teacher lookup with null handling
        QrAttendanceLog attendanceLog = QrAttendanceLog.builder()
            .student(student)
            .qrCodeScanned(qrCodeData)
            .scanTimestamp(LocalDateTime.now())
            .period(period)
            .teacher(teacherId != null ? teacherRepository.findById(teacherId).orElse(null) : null)
            .roomNumber(roomNumber)
            .capturedPhoto(capturedPhoto)
            .faceMatchScore(faceMatch != null ? faceMatch.getMatchScore() : null)
            .faceMatchSuccess(faceMatch != null ? faceMatch.isSuccess() : null)
            .verificationStatus(verificationStatus)
            .parentNotified(false)
            .build();

        attendanceLog = attendanceLogRepository.save(attendanceLog);

        // Step 9: Update student's last scan timestamp
        student.setLastQrScan(LocalDateTime.now());
        studentRepository.save(student);

        // Step 10: Send parent notification if enabled and auto-verified
        if (parentNotificationEnabled && autoVerified) {
            sendParentNotification(student, attendanceLog);
        }

        // Step 11: Return result
        if (autoVerified) {
            return AttendanceResult.success(student, attendanceLog, faceMatch);
        } else if (verificationStatus == VerificationStatus.FLAGGED) {
            return AttendanceResult.flagged(student, attendanceLog, faceMatch,
                "Low confidence match - requires admin verification");
        } else {
            return AttendanceResult.pending(student, attendanceLog, faceMatch,
                "Attendance recorded - pending admin verification");
        }
    }

    /**
     * Admin verify attendance log
     */
    @Transactional
    public void adminVerify(Long attendanceLogId, Long adminUserId, boolean approved, String notes) {
        // ✅ NULL SAFE: Validate attendanceLogId parameter
        if (attendanceLogId == null) {
            throw new IllegalArgumentException("Attendance log ID cannot be null");
        }

        QrAttendanceLog attendanceLog = attendanceLogRepository.findById(attendanceLogId)
            .orElseThrow(() -> new IllegalArgumentException("Attendance log not found: " + attendanceLogId));

        attendanceLog.setVerificationStatus(approved ? VerificationStatus.ADMIN_VERIFIED : VerificationStatus.REJECTED);
        attendanceLog.setVerificationNotes(notes);
        attendanceLog.setVerifiedAt(LocalDateTime.now());
        // Note: Would set verifiedBy if User entity is available

        attendanceLogRepository.save(attendanceLog);

        // Send notification if now verified
        // ✅ NULL SAFE: Check student exists and parent notified flag
        if (approved && parentNotificationEnabled && attendanceLog.getStudent() != null
                && !Boolean.TRUE.equals(attendanceLog.getParentNotified())) {
            sendParentNotification(attendanceLog.getStudent(), attendanceLog);
        }

        // ✅ NULL SAFE: Safe logging with student ID validation
        Long studentId = (attendanceLog.getStudent() != null) ? attendanceLog.getStudent().getId() : null;
        log.info("Admin {} attendance log {} for student {}",
            approved ? "approved" : "rejected", attendanceLogId, studentId);
    }

    /**
     * Get attendance logs pending admin review
     */
    public List<QrAttendanceLog> getPendingReview() {
        List<QrAttendanceLog> pending = attendanceLogRepository.findPendingReview();
        // ✅ NULL SAFE: Validate repository result
        if (pending == null) {
            log.warn("Repository returned null for pending review logs");
            return java.util.Collections.emptyList();
        }
        return pending;
    }

    /**
     * Get attendance logs for a student on a specific date
     */
    public List<QrAttendanceLog> getAttendanceForStudentOnDate(Long studentId, LocalDate date) {
        // ✅ NULL SAFE: Validate parameters
        if (studentId == null || date == null) {
            log.warn("Cannot get attendance with null studentId or date");
            return java.util.Collections.emptyList();
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<QrAttendanceLog> logs = attendanceLogRepository.findByStudentAndDate(studentId, startOfDay, endOfDay);
        // ✅ NULL SAFE: Validate repository result
        if (logs == null) {
            log.warn("Repository returned null for student {} attendance on {}", studentId, date);
            return java.util.Collections.emptyList();
        }

        return logs;
    }

    /**
     * Get current period based on current time
     */
    public Optional<PeriodTimer> getCurrentPeriod() {
        return periodTimerRepository.findByCurrentTime(LocalTime.now());
    }

    /**
     * Send parent notification for attendance
     */
    private void sendParentNotification(Student student, QrAttendanceLog attendanceLog) {
        try {
            // ✅ NULL SAFE: Check student and attendance log fields exist
            if (student == null || attendanceLog == null) {
                log.warn("Cannot send notification - student or attendance log is null");
                return;
            }

            String title = "Attendance Notification";
            String message = String.format(
                "Attendance confirmed for %s %s at %s in period %s",
                student.getFirstName() != null ? student.getFirstName() : "",
                student.getLastName() != null ? student.getLastName() : "",
                attendanceLog.getScanTimestamp() != null ? attendanceLog.getScanTimestamp().toLocalTime() : "unknown time",
                attendanceLog.getPeriod() != null ? attendanceLog.getPeriod() : "arrival"
            );

            notificationService.createDetailedNotification(
                Notification.NotificationType.SYSTEM_ALERT,
                title,
                message,
                2, // Priority: Medium
                null, // targetUserId - would link to parent user if available
                "PARENT", // targetRole
                "ATTENDANCE",
                attendanceLog.getId(),
                null
            );

            attendanceLog.setParentNotified(true);
            attendanceLog.setNotificationSentAt(LocalDateTime.now());
            attendanceLogRepository.save(attendanceLog);

            log.info("Parent notification sent for student {}", student.getId());

        } catch (Exception e) {
            log.error("Failed to send parent notification for student {}", student.getId(), e);
        }
    }

    // ========================================================================
    // DTOs
    // ========================================================================

    /**
     * Attendance result DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceResult {
        private boolean success;
        private String status; // SUCCESS, PENDING, FLAGGED, FAILED
        private String message;
        private Student student;
        private QrAttendanceLog attendanceLog;
        private FaceMatchResult faceMatchResult;
        private LocalDateTime timestamp;

        public static AttendanceResult success(Student student, QrAttendanceLog log, FaceMatchResult faceMatch) {
            return AttendanceResult.builder()
                .success(true)
                .status("SUCCESS")
                .message("Attendance verified successfully")
                .student(student)
                .attendanceLog(log)
                .faceMatchResult(faceMatch)
                .timestamp(LocalDateTime.now())
                .build();
        }

        public static AttendanceResult pending(Student student, QrAttendanceLog log, FaceMatchResult faceMatch, String message) {
            return AttendanceResult.builder()
                .success(false)
                .status("PENDING")
                .message(message)
                .student(student)
                .attendanceLog(log)
                .faceMatchResult(faceMatch)
                .timestamp(LocalDateTime.now())
                .build();
        }

        public static AttendanceResult flagged(Student student, QrAttendanceLog log, FaceMatchResult faceMatch, String message) {
            return AttendanceResult.builder()
                .success(false)
                .status("FLAGGED")
                .message(message)
                .student(student)
                .attendanceLog(log)
                .faceMatchResult(faceMatch)
                .timestamp(LocalDateTime.now())
                .build();
        }

        public static AttendanceResult failure(String message) {
            return AttendanceResult.builder()
                .success(false)
                .status("FAILED")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        }

        public static AttendanceResult pendingPhoto(Student student, String message) {
            return AttendanceResult.builder()
                .success(false)
                .status("PENDING_PHOTO")
                .message(message)
                .student(student)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }
}
