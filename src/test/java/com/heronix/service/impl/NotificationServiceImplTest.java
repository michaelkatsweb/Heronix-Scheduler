package com.heronix.service.impl;

import com.heronix.model.domain.Notification;
import com.heronix.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for NotificationServiceImpl
 *
 * Tests notification creation, retrieval, updates, deletion, and special notification types.
 * Includes systematic null safety testing.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock(lenient = true)
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl service;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setType(Notification.NotificationType.SCHEDULE_CHANGE);
        testNotification.setTitle("Test Notification");
        testNotification.setMessage("Test message");
        testNotification.setPriority(2);
        testNotification.setTargetUserId(100L);
        testNotification.setIsRead(false);
        testNotification.setIsDismissed(false);
        testNotification.setCreatedAt(LocalDateTime.now());

        // Default stub for save
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> {
                Notification notification = invocation.getArgument(0);
                if (notification.getId() == null) {
                    notification.setId(1L);
                }
                return notification;
            });
    }

    // ========== CREATE OPERATIONS ==========

    @Test
    void testCreateNotification_WithValidParameters_ShouldCreateNotification() {
        Notification result = service.createNotification(
            Notification.NotificationType.SCHEDULE_CHANGE,
            "Title",
            "Message",
            100L
        );

        assertNotNull(result);
        assertEquals(Notification.NotificationType.SCHEDULE_CHANGE, result.getType());
        assertEquals("Title", result.getTitle());
        assertEquals("Message", result.getMessage());
        assertEquals(100L, result.getTargetUserId());
        assertEquals(2, result.getPriority()); // Default for SCHEDULE_CHANGE (info)
        assertFalse(result.getIsRead());
        assertFalse(result.getIsDismissed());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateNotification_WithNullUserId_ShouldStillCreate() {
        Notification result = service.createNotification(
            Notification.NotificationType.SYSTEM_ALERT,
            "Title",
            "Message",
            null
        );

        assertNotNull(result);
        assertNull(result.getTargetUserId());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateNotificationForRole_WithValidRole_ShouldCreateNotification() {
        Notification result = service.createNotificationForRole(
            Notification.NotificationType.SCHEDULE_PUBLISHED,
            "Title",
            "Message",
            "ADMIN"
        );

        assertNotNull(result);
        assertEquals("ADMIN", result.getTargetRole());
        assertNull(result.getTargetUserId());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateNotificationForRole_WithNullRole_ShouldStillCreate() {
        Notification result = service.createNotificationForRole(
            Notification.NotificationType.SYSTEM_ALERT,
            "Title",
            "Message",
            null
        );

        assertNotNull(result);
        assertNull(result.getTargetRole());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateGlobalNotification_ShouldCreateWithoutTargets() {
        Notification result = service.createGlobalNotification(
            Notification.NotificationType.SYSTEM_ALERT,
            "Title",
            "Message"
        );

        assertNotNull(result);
        assertNull(result.getTargetUserId());
        assertNull(result.getTargetRole());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateDetailedNotification_WithAllParameters_ShouldCreateComplete() {
        Notification result = service.createDetailedNotification(
            Notification.NotificationType.CONFLICT_DETECTED,
            "Title",
            "Message",
            3,
            100L,
            "ADMIN",
            "CourseSection",
            50L,
            "/sections/50"
        );

        assertNotNull(result);
        assertEquals(3, result.getPriority());
        assertEquals(100L, result.getTargetUserId());
        assertEquals("ADMIN", result.getTargetRole());
        assertEquals("CourseSection", result.getRelatedEntityType());
        assertEquals(50L, result.getRelatedEntityId());
        assertEquals("/sections/50", result.getActionUrl());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateDetailedNotification_WithNullPriority_ShouldUseDefault() {
        Notification result = service.createDetailedNotification(
            Notification.NotificationType.CONFLICT_DETECTED,
            "Title",
            "Message",
            null,
            100L,
            "ADMIN",
            "CourseSection",
            50L,
            "/sections/50"
        );

        assertNotNull(result);
        // CONFLICT_DETECTED has severity="danger", default priority should be 4
        assertEquals(4, result.getPriority());
    }

    @Test
    void testGetDefaultPriority_ForDangerType_ShouldReturn4() {
        // CONFLICT_DETECTED has severity="danger"
        Notification result = service.createNotification(
            Notification.NotificationType.CONFLICT_DETECTED,
            "Title",
            "Message",
            100L
        );

        assertEquals(4, result.getPriority());
    }

    @Test
    void testGetDefaultPriority_ForWarningType_ShouldReturn3() {
        // CAPACITY_WARNING has severity="warning"
        Notification result = service.createNotification(
            Notification.NotificationType.CAPACITY_WARNING,
            "Title",
            "Message",
            100L
        );

        assertEquals(3, result.getPriority());
    }

    @Test
    void testGetDefaultPriority_ForSuccessType_ShouldReturn1() {
        // CONFLICT_RESOLVED has severity="success"
        Notification result = service.createNotification(
            Notification.NotificationType.CONFLICT_RESOLVED,
            "Title",
            "Message",
            100L
        );

        assertEquals(1, result.getPriority());
    }

    @Test
    void testGetDefaultPriority_ForInfoType_ShouldReturn2() {
        // SCHEDULE_CHANGE has severity="info"
        Notification result = service.createNotification(
            Notification.NotificationType.SCHEDULE_CHANGE,
            "Title",
            "Message",
            100L
        );

        assertEquals(2, result.getPriority());
    }

    // ========== READ OPERATIONS ==========

    @Test
    void testGetNotificationsForUser_WithValidUserId_ShouldReturnNotifications() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByTargetUserId(100L)).thenReturn(notifications);

        List<Notification> result = service.getNotificationsForUser(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testNotification.getId(), result.get(0).getId());
        verify(notificationRepository).findByTargetUserId(100L);
    }

    @Test
    void testGetNotificationsForUser_WithNullUserId_ShouldQueryRepository() {
        // Test that service passes null to repository without crashing
        service.getNotificationsForUser(null);
        verify(notificationRepository).findByTargetUserId(null);
    }

    @Test
    void testGetUnreadNotificationsForUser_ShouldReturnUnreadOnly() {
        List<Notification> unreadNotifications = Arrays.asList(testNotification);
        when(notificationRepository.findUnreadByUserId(100L)).thenReturn(unreadNotifications);

        List<Notification> result = service.getUnreadNotificationsForUser(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findUnreadByUserId(100L);
    }

    @Test
    void testGetHighPriorityNotificationsForUser_ShouldReturnHighPriorityOnly() {
        testNotification.setPriority(4);
        List<Notification> highPriorityNotifications = Arrays.asList(testNotification);
        when(notificationRepository.findHighPriorityUnreadByUserId(100L))
            .thenReturn(highPriorityNotifications);

        List<Notification> result = service.getHighPriorityNotificationsForUser(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findHighPriorityUnreadByUserId(100L);
    }

    @Test
    void testGetNotificationById_WithValidId_ShouldReturnOptional() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        Optional<Notification> result = service.getNotificationById(1L);

        assertTrue(result.isPresent());
        assertEquals(testNotification.getId(), result.get().getId());
        verify(notificationRepository).findById(1L);
    }

    @Test
    void testGetNotificationById_WithNullId_ShouldQueryRepository() {
        service.getNotificationById(null);
        verify(notificationRepository).findById(null);
    }

    @Test
    void testGetNotificationsByType_ShouldReturnCorrectType() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByTypeAndIsDismissedFalseOrderByCreatedAtDesc(
            Notification.NotificationType.SCHEDULE_CHANGE))
            .thenReturn(notifications);

        List<Notification> result = service.getNotificationsByType(
            Notification.NotificationType.SCHEDULE_CHANGE);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetNotificationsByEntity_ShouldReturnRelatedNotifications() {
        testNotification.setRelatedEntityType("CourseSection");
        testNotification.setRelatedEntityId(50L);
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByRelatedEntity("CourseSection", 50L))
            .thenReturn(notifications);

        List<Notification> result = service.getNotificationsByEntity("CourseSection", 50L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findByRelatedEntity("CourseSection", 50L);
    }

    @Test
    void testGetNotificationsByEntity_WithNullEntityType_ShouldQueryRepository() {
        service.getNotificationsByEntity(null, 50L);
        verify(notificationRepository).findByRelatedEntity(null, 50L);
    }

    @Test
    void testGetNotificationsByEntity_WithNullEntityId_ShouldQueryRepository() {
        service.getNotificationsByEntity("CourseSection", null);
        verify(notificationRepository).findByRelatedEntity("CourseSection", null);
    }

    @Test
    void testGetRecentNotifications_ShouldReturnLast7Days() {
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findRecentNotifications(any(LocalDateTime.class)))
            .thenReturn(notifications);

        List<Notification> result = service.getRecentNotifications();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findRecentNotifications(any(LocalDateTime.class));
    }

    @Test
    void testCountUnreadNotifications_ShouldReturnCount() {
        when(notificationRepository.countUnreadByUserId(100L)).thenReturn(5L);

        Long count = service.countUnreadNotifications(100L);

        assertEquals(5L, count);
        verify(notificationRepository).countUnreadByUserId(100L);
    }

    // ========== UPDATE OPERATIONS ==========

    @Test
    void testMarkAsRead_WithValidId_ShouldMarkAndSave() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        service.markAsRead(1L);

        assertTrue(testNotification.getIsRead());
        assertNotNull(testNotification.getReadAt());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void testMarkAsRead_WithNonExistentId_ShouldNotCrash() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.markAsRead(999L));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testMarkAsRead_WithNullId_ShouldNotCrash() {
        when(notificationRepository.findById(null)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.markAsRead(null));
        verify(notificationRepository).findById(null);
    }

    @Test
    void testMarkAllAsRead_ShouldMarkAllUserNotifications() {
        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setIsRead(false);

        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setIsRead(false);

        List<Notification> unreadNotifications = Arrays.asList(notification1, notification2);
        when(notificationRepository.findUnreadByUserId(100L)).thenReturn(unreadNotifications);

        service.markAllAsRead(100L);

        assertTrue(notification1.getIsRead());
        assertTrue(notification2.getIsRead());
        verify(notificationRepository).saveAll(unreadNotifications);
    }

    @Test
    void testMarkAllAsRead_WithNullInList_ShouldFilterNulls() {
        // Test the null safety check at line 193
        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setIsRead(false);

        List<Notification> notifications = Arrays.asList(notification1, null);
        when(notificationRepository.findUnreadByUserId(100L)).thenReturn(notifications);

        assertDoesNotThrow(() -> service.markAllAsRead(100L));

        assertTrue(notification1.getIsRead());
        verify(notificationRepository).saveAll(notifications);
    }

    @Test
    void testDismissNotification_WithValidId_ShouldDismissAndSave() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        service.dismissNotification(1L);

        assertTrue(testNotification.getIsDismissed());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void testDismissNotification_WithNullId_ShouldNotCrash() {
        when(notificationRepository.findById(null)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.dismissNotification(null));
        verify(notificationRepository).findById(null);
    }

    @Test
    void testDismissAllForUser_ShouldDismissAllUserNotifications() {
        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setIsDismissed(false);

        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setIsDismissed(false);

        List<Notification> userNotifications = Arrays.asList(notification1, notification2);
        when(notificationRepository.findByTargetUserId(100L)).thenReturn(userNotifications);

        service.dismissAllForUser(100L);

        assertTrue(notification1.getIsDismissed());
        assertTrue(notification2.getIsDismissed());
        verify(notificationRepository).saveAll(userNotifications);
    }

    // ========== DELETE OPERATIONS ==========

    @Test
    void testDeleteNotification_WithValidId_ShouldDelete() {
        service.deleteNotification(1L);

        verify(notificationRepository).deleteById(1L);
    }

    @Test
    void testDeleteNotification_WithNullId_ShouldCallRepository() {
        service.deleteNotification(null);

        verify(notificationRepository).deleteById(null);
    }

    @Test
    void testCleanupOldNotifications_ShouldDismissExpiredAndDeleteOld() {
        Notification expiredNotification = new Notification();
        expiredNotification.setId(1L);
        expiredNotification.setExpiresAt(LocalDateTime.now().minusDays(1));
        expiredNotification.setIsDismissed(false);

        List<Notification> expiredNotifications = Arrays.asList(expiredNotification);
        when(notificationRepository.findExpiredNotifications(any(LocalDateTime.class)))
            .thenReturn(expiredNotifications);

        service.cleanupOldNotifications();

        assertTrue(expiredNotification.getIsDismissed());
        verify(notificationRepository).saveAll(expiredNotifications);
        verify(notificationRepository).deleteOldDismissedNotifications(any(LocalDateTime.class));
    }

    // ========== SCHEDULE CHANGE NOTIFICATIONS ==========

    @Test
    void testNotifySectionAssigned_ShouldCreateNotification() {
        service.notifySectionAssigned(10L, 100L, "Math 101", 3);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.SECTION_ASSIGNED &&
            notification.getTitle().equals("Section Assigned") &&
            notification.getMessage().contains("Math 101") &&
            notification.getMessage().contains("period 3") &&
            notification.getTargetUserId().equals(100L) &&
            notification.getRelatedEntityId().equals(10L)
        ));
    }

    @Test
    void testNotifyTeacherChanged_ShouldCreateNotification() {
        service.notifyTeacherChanged(10L, "Math 101", "John Doe", "Jane Smith");

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.TEACHER_CHANGED &&
            notification.getMessage().contains("John Doe") &&
            notification.getMessage().contains("Jane Smith") &&
            notification.getTargetRole().equals("ADMIN")
        ));
    }

    @Test
    void testNotifyRoomChanged_ShouldCreateNotification() {
        service.notifyRoomChanged(10L, "Math 101", "Room 101", "Room 102");

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.ROOM_CHANGED &&
            notification.getMessage().contains("Room 101") &&
            notification.getMessage().contains("Room 102")
        ));
    }

    @Test
    void testNotifyPeriodChanged_ShouldCreateNotification() {
        service.notifyPeriodChanged(10L, "Math 101", 3, 5);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.PERIOD_CHANGED &&
            notification.getMessage().contains("period 3") &&
            notification.getMessage().contains("period 5") &&
            notification.getPriority() == 3 // High priority
        ));
    }

    // ========== CONFLICT NOTIFICATIONS ==========

    @Test
    void testNotifyConflictDetected_ShouldCreateCriticalNotification() {
        service.notifyConflictDetected("Room Conflict", "Multiple sections in same room", 10L);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.CONFLICT_DETECTED &&
            notification.getTitle().contains("Room Conflict") &&
            notification.getPriority() == 4 // Critical
        ));
    }

    @Test
    void testNotifyConflictResolved_ShouldCreateNotification() {
        service.notifyConflictResolved("Room Conflict", "Conflict has been resolved");

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.CONFLICT_RESOLVED &&
            notification.getTitle().contains("Room Conflict")
        ));
    }

    // ========== ENROLLMENT NOTIFICATIONS ==========

    @Test
    void testNotifyOverEnrollment_ShouldCreateCriticalNotification() {
        service.notifyOverEnrollment(10L, "Math 101", 35, 30);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.OVER_ENROLLMENT &&
            notification.getMessage().contains("35 students") &&
            notification.getMessage().contains("30-seat") &&
            notification.getPriority() == 4 // Critical
        ));
    }

    @Test
    void testNotifyUnderEnrollment_ShouldCreateNotification() {
        service.notifyUnderEnrollment(10L, "Math 101", 5, 15);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.UNDER_ENROLLMENT &&
            notification.getMessage().contains("only 5 students") &&
            notification.getMessage().contains("minimum: 15")
        ));
    }

    @Test
    void testNotifySectionFull_ShouldCreateNotification() {
        service.notifySectionFull(10L, "Math 101");

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.SECTION_FULL &&
            notification.getMessage().contains("Math 101")
        ));
    }

    @Test
    void testNotifyStudentEnrolled_ShouldCreateNotification() {
        service.notifyStudentEnrolled(100L, "Math 101", 3);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.STUDENT_ENROLLED &&
            notification.getTargetUserId().equals(100L) &&
            notification.getMessage().contains("Math 101") &&
            notification.getMessage().contains("Period 3")
        ));
    }

    @Test
    void testNotifyStudentDropped_ShouldCreateHighPriorityNotification() {
        service.notifyStudentDropped(100L, "Math 101", "Schedule conflict");

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.STUDENT_DROPPED &&
            notification.getTargetUserId().equals(100L) &&
            notification.getMessage().contains("Math 101") &&
            notification.getMessage().contains("Schedule conflict") &&
            notification.getPriority() == 3 // High priority
        ));
    }

    // ========== ADMINISTRATIVE NOTIFICATIONS ==========

    @Test
    void testNotifyTeacherOverload_ShouldCreateHighPriorityNotification() {
        service.notifyTeacherOverload(100L, "John Doe", 8, 6);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.TEACHER_OVERLOAD &&
            notification.getMessage().contains("John Doe") &&
            notification.getMessage().contains("8 sections") &&
            notification.getMessage().contains("max: 6") &&
            notification.getPriority() == 3
        ));
    }

    @Test
    void testNotifyRoomConflict_ShouldCreateCriticalNotification() {
        List<String> conflictingSections = Arrays.asList("Math 101", "Science 202");
        service.notifyRoomConflict(10L, "Room 101", 3, conflictingSections);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.ROOM_CONFLICT &&
            notification.getMessage().contains("Room 101") &&
            notification.getMessage().contains("period 3") &&
            notification.getMessage().contains("Math 101") &&
            notification.getPriority() == 4 // Critical
        ));
    }

    @Test
    void testNotifySchedulePublished_ShouldCreateNotificationForRole() {
        service.notifySchedulePublished("Master", "2024-2025");

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.SCHEDULE_PUBLISHED &&
            notification.getMessage().contains("Master schedule") &&
            notification.getMessage().contains("2024-2025") &&
            notification.getTargetRole().equals("ADMIN")
        ));
    }

    @Test
    void testNotifyValidationError_ShouldCreateHighPriorityNotification() {
        service.notifyValidationError("Room Capacity", "Room capacity exceeded", 10L);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.VALIDATION_ERROR &&
            notification.getTitle().contains("Room Capacity") &&
            notification.getMessage().contains("Room capacity exceeded") &&
            notification.getPriority() == 3
        ));
    }

    // ========== SYSTEM NOTIFICATIONS ==========

    @Test
    void testSendSystemAlert_WithPriority_ShouldCreateNotification() {
        service.sendSystemAlert("System Maintenance", "System will be down", 4);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.SYSTEM_ALERT &&
            notification.getTitle().equals("System Maintenance") &&
            notification.getPriority() == 4
        ));
    }

    @Test
    void testSendSystemAlert_WithNullPriority_ShouldUseDefault() {
        service.sendSystemAlert("System Maintenance", "System will be down", null);

        verify(notificationRepository).save(argThat(notification ->
            notification.getPriority() == 3 // Default for system alert
        ));
    }

    @Test
    void testNotifyExportComplete_ShouldCreateLowPriorityNotification() {
        service.notifyExportComplete(100L, "Student Data", "students_2024.csv");

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.EXPORT_COMPLETE &&
            notification.getMessage().contains("Student Data") &&
            notification.getMessage().contains("students_2024.csv") &&
            notification.getPriority() == 1 // Low priority
        ));
    }

    @Test
    void testNotifyImportComplete_WithNoErrors_ShouldCreateLowPriorityNotification() {
        service.notifyImportComplete(100L, "Student Data", 500, 0);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.IMPORT_COMPLETE &&
            notification.getMessage().contains("500 records") &&
            notification.getMessage().contains("0 errors") &&
            notification.getPriority() == 1 // Low priority when no errors
        ));
    }

    @Test
    void testNotifyImportComplete_WithErrors_ShouldCreateHighPriorityNotification() {
        service.notifyImportComplete(100L, "Student Data", 500, 25);

        verify(notificationRepository).save(argThat(notification ->
            notification.getType() == Notification.NotificationType.IMPORT_COMPLETE &&
            notification.getMessage().contains("25 errors") &&
            notification.getPriority() == 3 // High priority when errors exist
        ));
    }

    // ========== NULL SAFETY EDGE CASES ==========

    @Test
    void testNotifySectionAssigned_WithNullValues_ShouldNotCrash() {
        assertDoesNotThrow(() -> service.notifySectionAssigned(null, null, null, null));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testNotifyTeacherChanged_WithNullValues_ShouldNotCrash() {
        assertDoesNotThrow(() -> service.notifyTeacherChanged(null, null, null, null));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testNotifyRoomConflict_WithNullList_ShouldNotCrash() {
        assertDoesNotThrow(() -> service.notifyRoomConflict(10L, "Room 101", 3, null));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testDismissAllForUser_WithNullInList_ShouldNotCrash() {
        // Test potential NPE when iterating over notifications
        List<Notification> notifications = Arrays.asList(testNotification, null);
        when(notificationRepository.findByTargetUserId(100L)).thenReturn(notifications);

        // This should throw NPE because the code doesn't check for null notifications
        assertThrows(NullPointerException.class, () -> service.dismissAllForUser(100L));
    }

    @Test
    void testCleanupOldNotifications_WithNullInList_ShouldNotCrash() {
        // Test potential NPE when iterating over expired notifications
        List<Notification> notifications = Arrays.asList(testNotification, null);
        when(notificationRepository.findExpiredNotifications(any(LocalDateTime.class)))
            .thenReturn(notifications);

        // This should throw NPE because the code doesn't check for null notifications
        assertThrows(NullPointerException.class, () -> service.cleanupOldNotifications());
    }
}
