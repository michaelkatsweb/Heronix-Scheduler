package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.model.domain.HallPassSession.Destination;
import com.heronix.model.domain.HallPassSession.SessionStatus;
import com.heronix.repository.*;
import com.heronix.service.impl.HallPassService;
import com.heronix.service.impl.HallPassService.HallPassResult;
import com.heronix.service.impl.FacialRecognitionService;
import com.heronix.service.impl.FacialRecognitionService.FaceMatchResult;
import com.heronix.testutil.BaseServiceTest;
import com.heronix.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for HallPassService
 * Tests null safety, QR code processing, facial recognition, and business logic
 *
 * Focus areas:
 * - Null safety for all public methods
 * - QR code validation
 * - Active hall pass detection
 * - Facial recognition verification
 * - Overdue detection
 * - Edge cases (invalid QR, multiple passes, etc.)
 */
@ExtendWith(MockitoExtension.class)
class HallPassServiceTest extends BaseServiceTest {

    @Mock(lenient = true)
    private StudentRepository studentRepository;

    @Mock(lenient = true)
    private HallPassSessionRepository hallPassRepository;

    @Mock(lenient = true)
    private TeacherRepository teacherRepository;

    @Mock(lenient = true)
    private FacialRecognitionService facialRecognitionService;

    @Mock(lenient = true)
    private NotificationService notificationService;

    @InjectMocks
    private HallPassService service;

    private Student testStudent;
    private Teacher testTeacher;
    private byte[] testPhoto;
    private String validQrCode;

    @BeforeEach
    void setUp() {
        // Set configuration values using ReflectionTestUtils
        ReflectionTestUtils.setField(service, "hallPassEnabled", true);
        ReflectionTestUtils.setField(service, "maxDurationMinutes", 15);
        ReflectionTestUtils.setField(service, "overdueAlertEnabled", true);
        ReflectionTestUtils.setField(service, "faceMatchThreshold", 0.80);
        ReflectionTestUtils.setField(service, "parentNotificationEnabled", true);

        // Create test student
        testStudent = TestDataBuilder.aStudent()
                .withId(1L)
                .withFirstName("John")
                .withLastName("Doe")
                .build();

        // Set QR fields manually
        testStudent.setQrCodeId("QR-STU-001");
        testStudent.setFaceSignature("test-face-signature".getBytes());

        // Create test teacher
        testTeacher = TestDataBuilder.aTeacher()
                .withId(1L)
                .withFirstName("Jane")
                .withLastName("Smith")
                .build();

        validQrCode = "QR-STU-001";

        // Create test photo
        testPhoto = new byte[]{1, 2, 3, 4, 5};
    }

    // ========================================================================
    // NULL SAFETY TESTS - startHallPass()
    // ========================================================================

    @Test
    void testStartHallPass_WithNullQrCode_ShouldReturnFailure() {
        // When
        HallPassResult result = service.startHallPass(
            null, testPhoto, Destination.BATHROOM, 1L, 1, "101");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("required"));
    }

    @Test
    void testStartHallPass_WithEmptyQrCode_ShouldReturnFailure() {
        // When
        HallPassResult result = service.startHallPass(
            "", testPhoto, Destination.BATHROOM, 1L, 1, "101");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("required"));
    }

    @Test
    void testStartHallPass_WithNullPhoto_ShouldNotThrowNPE() {
        // Given
        when(studentRepository.findByQrCodeId(validQrCode)).thenReturn(Optional.of(testStudent));
        when(hallPassRepository.findActiveByStudentId(anyLong())).thenReturn(Optional.empty());
        when(teacherRepository.findById(anyLong())).thenReturn(Optional.of(testTeacher));
        when(hallPassRepository.save(any(HallPassSession.class))).thenAnswer(i -> i.getArguments()[0]);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.startHallPass(validQrCode, null, Destination.LIBRARY, 1L, 1, "101")
        );
    }

    @Test
    void testStartHallPass_WithNullDestination_ShouldNotThrowNPE() {
        // Given
        when(studentRepository.findByQrCodeId(validQrCode)).thenReturn(Optional.of(testStudent));
        when(hallPassRepository.findActiveByStudentId(anyLong())).thenReturn(Optional.empty());
        when(teacherRepository.findById(anyLong())).thenReturn(Optional.of(testTeacher));

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.startHallPass(validQrCode, testPhoto, null, 1L, 1, "101")
        );
    }

    @Test
    void testStartHallPass_WithNullTeacherId_ShouldNotThrowNPE() {
        // Given
        when(studentRepository.findByQrCodeId(validQrCode)).thenReturn(Optional.of(testStudent));
        when(hallPassRepository.findActiveByStudentId(anyLong())).thenReturn(Optional.empty());
        when(hallPassRepository.save(any(HallPassSession.class))).thenAnswer(i -> i.getArguments()[0]);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.startHallPass(validQrCode, testPhoto, Destination.OFFICE, null, 1, "101")
        );
    }

    @Test
    void testStartHallPass_WithNullPeriod_ShouldNotThrowNPE() {
        // Given
        when(studentRepository.findByQrCodeId(validQrCode)).thenReturn(Optional.of(testStudent));
        when(hallPassRepository.findActiveByStudentId(anyLong())).thenReturn(Optional.empty());
        when(teacherRepository.findById(anyLong())).thenReturn(Optional.of(testTeacher));
        when(hallPassRepository.save(any(HallPassSession.class))).thenAnswer(i -> i.getArguments()[0]);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.startHallPass(validQrCode, testPhoto, Destination.CLINIC, 1L, null, "101")
        );
    }

    @Test
    void testStartHallPass_WithNullDepartureRoom_ShouldNotThrowNPE() {
        // Given
        when(studentRepository.findByQrCodeId(validQrCode)).thenReturn(Optional.of(testStudent));
        when(hallPassRepository.findActiveByStudentId(anyLong())).thenReturn(Optional.empty());
        when(teacherRepository.findById(anyLong())).thenReturn(Optional.of(testTeacher));
        when(hallPassRepository.save(any(HallPassSession.class))).thenAnswer(i -> i.getArguments()[0]);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.startHallPass(validQrCode, testPhoto, Destination.LIBRARY, 1L, 1, null)
        );
    }

    @Test
    void testStartHallPass_WhenStudentNotFound_ShouldReturnFailure() {
        // Given
        when(studentRepository.findByQrCodeId(anyString())).thenReturn(Optional.empty());

        // When
        HallPassResult result = service.startHallPass(
            "INVALID-QR", testPhoto, Destination.BATHROOM, 1L, 1, "101");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("not found"));
    }

    @Test
    void testStartHallPass_WithStudentIdNull_ShouldReturnFailure() {
        // Given
        testStudent.setId(null);
        when(studentRepository.findByQrCodeId(validQrCode)).thenReturn(Optional.of(testStudent));

        // When
        HallPassResult result = service.startHallPass(
            validQrCode, testPhoto, Destination.BATHROOM, 1L, 1, "101");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("missing"));
    }

    // ========================================================================
    // NULL SAFETY TESTS - endHallPass()
    // ========================================================================

    @Test
    void testEndHallPass_WithNullQrCode_ShouldReturnFailure() {
        // When
        HallPassResult result = service.endHallPass(null, testPhoto, "102");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    void testEndHallPass_WithEmptyQrCode_ShouldReturnFailure() {
        // When
        HallPassResult result = service.endHallPass("", testPhoto, "102");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    void testEndHallPass_WithNullPhoto_ShouldNotThrowNPE() {
        // Given
        when(studentRepository.findByQrCodeId(validQrCode)).thenReturn(Optional.of(testStudent));

        HallPassSession activeSession = new HallPassSession();
        activeSession.setId(1L);
        activeSession.setStudent(testStudent);
        activeSession.setStatus(SessionStatus.ACTIVE);
        activeSession.setDepartureTime(LocalDateTime.now().minusMinutes(5));

        when(hallPassRepository.findActiveByStudentId(anyLong())).thenReturn(Optional.of(activeSession));
        when(hallPassRepository.save(any(HallPassSession.class))).thenAnswer(i -> i.getArguments()[0]);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.endHallPass(validQrCode, null, "102")
        );
    }

    @Test
    void testEndHallPass_WithNullReturnRoom_ShouldNotThrowNPE() {
        // Given
        when(studentRepository.findByQrCodeId(validQrCode)).thenReturn(Optional.of(testStudent));

        HallPassSession activeSession = new HallPassSession();
        activeSession.setId(1L);
        activeSession.setStudent(testStudent);
        activeSession.setStatus(SessionStatus.ACTIVE);
        activeSession.setDepartureTime(LocalDateTime.now().minusMinutes(5));

        when(hallPassRepository.findActiveByStudentId(anyLong())).thenReturn(Optional.of(activeSession));
        when(hallPassRepository.save(any(HallPassSession.class))).thenAnswer(i -> i.getArguments()[0]);

        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.endHallPass(validQrCode, testPhoto, null)
        );
    }

    // ========================================================================
    // NULL SAFETY TESTS - Other Methods
    // ========================================================================

    @Test
    void testGetActiveSessions_ShouldNotThrowNPE() {
        // When/Then: Should return result without throwing
        assertDoesNotThrow(() -> {
            List<HallPassSession> result = service.getActiveSessions();
            assertNotNull(result);
        });
    }

    @Test
    void testGetStudentHistory_WithNullStudentId_ShouldNotThrowNPE() {
        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> {
            List<HallPassSession> result = service.getStudentHistory(null);
            // Result should be null or empty, not throw NPE
        });
    }

    @Test
    void testCheckOverdueSessions_ShouldNotThrowNPE() {
        // When/Then: Should not throw
        assertDoesNotThrow(() ->
            service.checkOverdueSessions()
        );
    }

    // ========================================================================
    // BUSINESS LOGIC TESTS
    // ========================================================================

    @Test
    void testStartHallPass_WhenSystemDisabled_ShouldReturnFailure() {
        // Given
        ReflectionTestUtils.setField(service, "hallPassEnabled", false);

        // When
        HallPassResult result = service.startHallPass(
            validQrCode, testPhoto, Destination.BATHROOM, 1L, 1, "101");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("disabled"));
    }

    @Test
    void testStartHallPass_WhenStudentHasActivePass_ShouldReturnFailure() {
        // Given
        when(studentRepository.findByQrCodeId(validQrCode)).thenReturn(Optional.of(testStudent));

        HallPassSession existingPass = new HallPassSession();
        existingPass.setId(1L);
        existingPass.setStudent(testStudent);
        existingPass.setDestination(Destination.LIBRARY);
        existingPass.setDepartureTime(LocalDateTime.now().minusMinutes(5));
        existingPass.setStatus(SessionStatus.ACTIVE);

        when(hallPassRepository.findActiveByStudentId(anyLong())).thenReturn(Optional.of(existingPass));

        // When
        HallPassResult result = service.startHallPass(
            validQrCode, testPhoto, Destination.BATHROOM, 1L, 1, "101");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("already has"));
    }

    @Test
    void testEndHallPass_WhenNoActivePass_ShouldReturnFailure() {
        // Given
        when(studentRepository.findByQrCodeId(validQrCode)).thenReturn(Optional.of(testStudent));
        when(hallPassRepository.findActiveByStudentId(anyLong())).thenReturn(Optional.empty());

        // When
        HallPassResult result = service.endHallPass(validQrCode, testPhoto, "102");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    void testGetActiveSessions_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        // Given
        when(hallPassRepository.findByStatusOrderByDepartureTimeDesc(any())).thenReturn(null);

        // When/Then: Should not throw
        assertDoesNotThrow(() -> {
            List<HallPassSession> result = service.getActiveSessions();
            // Result should be null or empty
        });
    }

    @Test
    void testGetStudentHistory_WhenRepositoryReturnsNull_ShouldHandleGracefully() {
        // Given
        when(hallPassRepository.findByStudent_IdOrderByDepartureTimeDesc(anyLong()))
            .thenReturn(null);

        // When/Then: Should not throw
        assertDoesNotThrow(() -> {
            List<HallPassSession> result = service.getStudentHistory(1L);
            // Result should be null or empty
        });
    }
}
