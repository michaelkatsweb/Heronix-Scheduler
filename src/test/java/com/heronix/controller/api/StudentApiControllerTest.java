package com.heronix.controller.api;

import com.heronix.api.dto.StudentAuthRequestDTO;
import com.heronix.api.dto.StudentAuthResponseDTO;
import com.heronix.model.domain.Student;
import com.heronix.repository.StudentRepository;
import com.heronix.service.FacialRecognitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for StudentApiController
 * Tests multi-factor authentication system: Password, QR code, Facial recognition, PIN
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 19, 2025 - Controller Layer Test Coverage
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class StudentApiControllerTest {

    @Autowired
    private StudentApiController studentApiController;

    @Autowired
    private StudentRepository studentRepository;

    @MockBean
    private FacialRecognitionService facialRecognitionService;

    private Student testStudent;
    private static final String TEST_STUDENT_ID = "API_STU_001";
    private static final String TEST_PASSWORD = "API_STU_001"; // Default: studentId = password
    private static final String TEST_PIN = "1234";

    @BeforeEach
    public void setup() {
        // Create test student with all authentication fields
        testStudent = new Student();
        testStudent.setStudentId(TEST_STUDENT_ID);
        testStudent.setFirstName("API");
        testStudent.setLastName("Student");
        testStudent.setEmail("api.student@eduscheduler.com");
        testStudent.setGradeLevel("10");
        testStudent.setActive(true);

        // QR code authentication
        testStudent.setQrCodeId(UUID.randomUUID().toString());

        // PIN authentication (BCrypt hashed)
        testStudent.setPinRequired(false); // Default: no PIN required
        testStudent.setPinHash(BCrypt.hashpw(TEST_PIN, BCrypt.gensalt()));
        testStudent.setFailedPinAttempts(0);
        testStudent.setPinLockedUntil(null);

        // IEP/504 flags
        testStudent.setHasIEP(false);
        testStudent.setHas504Plan(false);

        // Photo path for facial recognition
        testStudent.setPhotoPath("./data/student-photos/test_student.jpg");

        testStudent = studentRepository.save(testStudent);
    }

    // ========================================================================
    // TEST 1: BASIC PASSWORD AUTHENTICATION
    // ========================================================================

    @Test
    public void testAuthenticateStudent_WithValidPassword_ShouldSucceed() {
        // Arrange
        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertTrue(responseBody.getSuccess(), "Authentication should succeed");
        assertEquals(testStudent.getId(), responseBody.getStudentId(), "Student ID should match");
        assertEquals(TEST_STUDENT_ID, responseBody.getStudentIdNumber(), "Student ID number should match");
        assertEquals("API", responseBody.getFirstName(), "First name should match");
        assertEquals("Student", responseBody.getLastName(), "Last name should match");
        assertEquals("10", responseBody.getGradeLevel(), "Grade level should match");
        assertEquals(testStudent.getQrCodeId(), responseBody.getQrCodeId(), "QR code ID should be present");
        assertFalse(responseBody.getPinRequired(), "PIN should not be required");

        System.out.println("✓ Basic password authentication successful");
    }

    @Test
    public void testAuthenticateStudent_WithInvalidPassword_ShouldFail() {
        // Arrange
        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password("WRONG_PASSWORD")
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "Should return UNAUTHORIZED status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertFalse(responseBody.getSuccess(), "Authentication should fail");
        assertNotNull(responseBody.getMessage(), "Error message should be present");
        assertTrue(responseBody.getMessage().toLowerCase().contains("password") ||
                   responseBody.getMessage().toLowerCase().contains("credentials"),
                   "Error message should mention password/credentials");

        System.out.println("✓ Invalid password correctly rejected");
    }

    @Test
    public void testAuthenticateStudent_WithNonExistentStudent_ShouldFail() {
        // Arrange
        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId("NONEXISTENT_STUDENT")
            .password("any_password")
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "Should return UNAUTHORIZED status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertFalse(responseBody.getSuccess(), "Authentication should fail");

        System.out.println("✓ Non-existent student correctly rejected");
    }

    @Test
    public void testAuthenticateStudent_WithInactiveStudent_ShouldFail() {
        // Arrange: Mark student as inactive
        testStudent.setActive(false);
        studentRepository.save(testStudent);

        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should return FORBIDDEN status for inactive account");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertFalse(responseBody.getSuccess(), "Authentication should fail");
        assertTrue(responseBody.getMessage().toLowerCase().contains("inactive") ||
                   responseBody.getMessage().toLowerCase().contains("deactivated"),
                   "Error message should mention account is inactive");

        System.out.println("✓ Inactive student correctly rejected");
    }

    // ========================================================================
    // TEST 2: QR CODE AUTHENTICATION
    // ========================================================================

    @Test
    public void testAuthenticateStudent_WithValidQrCode_ShouldSucceed() {
        // Arrange
        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .qrCodeId(testStudent.getQrCodeId()) // Valid QR code ID
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertTrue(responseBody.getSuccess(), "Authentication should succeed");
        assertEquals(testStudent.getQrCodeId(), responseBody.getQrCodeId(), "QR code ID should match");

        System.out.println("✓ QR code authentication successful");
    }

    @Test
    public void testAuthenticateStudent_WithInvalidQrCode_ShouldFail() {
        // Arrange
        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .qrCodeId("INVALID_QR_CODE_ID") // Wrong QR code
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "Should return UNAUTHORIZED status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertFalse(responseBody.getSuccess(), "Authentication should fail");
        assertTrue(responseBody.getMessage().toLowerCase().contains("qr"),
                   "Error message should mention QR code");

        System.out.println("✓ Invalid QR code correctly rejected");
    }

    // ========================================================================
    // TEST 3: FACIAL RECOGNITION AUTHENTICATION
    // ========================================================================

    @Test
    public void testAuthenticateStudent_WithFacialRecognition_HighConfidence_ShouldSucceed() {
        // Arrange: Mock facial recognition service to return high confidence
        FacialRecognitionService.VerificationResult verificationResult =
            FacialRecognitionService.VerificationResult.success(0.95);

        when(facialRecognitionService.verifyFace(anyString(), anyString()))
            .thenReturn(verificationResult);

        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .photoBase64("BASE64_ENCODED_PHOTO_DATA") // Facial recognition
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertTrue(responseBody.getSuccess(), "Authentication should succeed");
        assertTrue(responseBody.getFaceVerified(), "Face should be verified");
        assertTrue(responseBody.getFaceMatchConfidence() >= 0.75, "Confidence should be >= 75%");

        System.out.println("✓ Facial recognition with high confidence successful");
    }

    @Test
    public void testAuthenticateStudent_WithFacialRecognition_LowConfidence_ShouldFail() {
        // Arrange: Mock facial recognition service to return low confidence
        FacialRecognitionService.VerificationResult verificationResult =
            FacialRecognitionService.VerificationResult.failed(0.45);

        when(facialRecognitionService.verifyFace(anyString(), anyString()))
            .thenReturn(verificationResult);

        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .photoBase64("BASE64_ENCODED_PHOTO_DATA")
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "Should return UNAUTHORIZED status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertFalse(responseBody.getSuccess(), "Authentication should fail");
        assertTrue(responseBody.getMessage().toLowerCase().contains("face") ||
                   responseBody.getMessage().toLowerCase().contains("recognition"),
                   "Error message should mention facial recognition");

        System.out.println("✓ Facial recognition with low confidence correctly rejected");
    }

    // ========================================================================
    // TEST 4: PIN AUTHENTICATION
    // ========================================================================

    @Test
    public void testAuthenticateStudent_WithPinRequired_ValidPin_ShouldSucceed() {
        // Arrange: Enable PIN requirement
        testStudent.setPinRequired(true);
        studentRepository.save(testStudent);

        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .pin(TEST_PIN) // Correct PIN
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertTrue(responseBody.getSuccess(), "Authentication should succeed");
        assertTrue(responseBody.getPinRequired(), "PIN should be required");
        assertTrue(responseBody.getPinVerified(), "PIN should be verified");

        // Verify failed PIN attempts were reset
        Student updatedStudent = studentRepository.findById(testStudent.getId()).get();
        assertEquals(0, updatedStudent.getFailedPinAttempts(), "Failed PIN attempts should be reset");

        System.out.println("✓ PIN authentication successful");
    }

    @Test
    public void testAuthenticateStudent_WithPinRequired_InvalidPin_ShouldFail() {
        // Arrange: Enable PIN requirement
        testStudent.setPinRequired(true);
        testStudent.setFailedPinAttempts(0);
        studentRepository.save(testStudent);

        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .pin("9999") // Wrong PIN
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "Should return UNAUTHORIZED status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertFalse(responseBody.getSuccess(), "Authentication should fail");
        assertTrue(responseBody.getMessage().toLowerCase().contains("pin"),
                   "Error message should mention PIN");

        // Verify failed attempts incremented
        Student updatedStudent = studentRepository.findById(testStudent.getId()).get();
        assertEquals(1, updatedStudent.getFailedPinAttempts(), "Failed attempts should increment");

        System.out.println("✓ Invalid PIN correctly rejected and attempt recorded");
    }

    @Test
    public void testAuthenticateStudent_WithPinRequired_MissingPin_ShouldFail() {
        // Arrange: Enable PIN requirement but don't provide PIN
        testStudent.setPinRequired(true);
        studentRepository.save(testStudent);

        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            // No PIN provided
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "Should return UNAUTHORIZED status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertFalse(responseBody.getSuccess(), "Authentication should fail");
        assertTrue(responseBody.getMessage().toLowerCase().contains("pin") ||
                   responseBody.getMessage().toLowerCase().contains("required"),
                   "Error message should mention PIN is required");

        System.out.println("✓ Missing required PIN correctly rejected");
    }

    @Test
    public void testAuthenticateStudent_PinAccountLockout_After3FailedAttempts() {
        // Arrange: Enable PIN requirement and set 2 previous failed attempts
        testStudent.setPinRequired(true);
        testStudent.setFailedPinAttempts(2); // Already 2 failed attempts
        studentRepository.save(testStudent);

        // Act: Make 3rd failed attempt
        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .pin("9999") // Wrong PIN (3rd failure)
            .build();

        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should return FORBIDDEN status for locked account");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertFalse(responseBody.getSuccess(), "Authentication should fail");
        assertTrue(responseBody.getAccountLocked(), "Account should be locked");
        assertNotNull(responseBody.getLockedUntil(), "Locked until timestamp should be set");
        assertTrue(responseBody.getMessage().toLowerCase().contains("lock"),
                   "Error message should mention account lock");

        // Verify account locked in database
        Student updatedStudent = studentRepository.findById(testStudent.getId()).get();
        assertEquals(3, updatedStudent.getFailedPinAttempts(), "Should have 3 failed attempts");
        assertNotNull(updatedStudent.getPinLockedUntil(), "Account should be locked");
        assertTrue(updatedStudent.getPinLockedUntil().isAfter(LocalDateTime.now()),
                   "Lock should be in future (30 minutes)");

        System.out.println("✓ Account locked after 3 failed PIN attempts");
    }

    @Test
    public void testAuthenticateStudent_WithLockedAccount_ShouldRejectEvenWithCorrectPin() {
        // Arrange: Lock the account
        testStudent.setPinRequired(true);
        testStudent.setFailedPinAttempts(3);
        testStudent.setPinLockedUntil(LocalDateTime.now().plusMinutes(30));
        studentRepository.save(testStudent);

        // Act: Try to authenticate with correct PIN
        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .pin(TEST_PIN) // Correct PIN
            .build();

        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Should return FORBIDDEN status for locked account");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertFalse(responseBody.getSuccess(), "Authentication should fail");
        assertTrue(responseBody.getAccountLocked(), "Account should be locked");
        assertNotNull(responseBody.getLockedUntil(), "Locked until timestamp should be present");

        System.out.println("✓ Locked account correctly rejected even with correct PIN");
    }

    // ========================================================================
    // TEST 5: QR CODE GENERATION
    // ========================================================================

    @Test
    public void testGenerateQRCode_WithValidStudentId_ShouldReturnQRCodeImage() {
        // Act
        ResponseEntity<byte[]> response = studentApiController.generateQRCode(testStudent.getId(), 300);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "QR code image data should be present");
        assertTrue(response.getBody().length > 0, "QR code image should have data");

        // Verify content type is image/png
        assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType(),
                     "Content-Type should be image/png");

        System.out.println("✓ QR code generated successfully");
        System.out.println("  - Image size: " + response.getBody().length + " bytes");
    }

    @Test
    public void testGenerateQRCode_WithNonExistentStudent_ShouldReturnNotFound() {
        // Act
        ResponseEntity<byte[]> response = studentApiController.generateQRCode(999999L, 300);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return NOT_FOUND status");

        System.out.println("✓ QR code generation for non-existent student correctly rejected");
    }

    @Test
    public void testGenerateQRCodeId_WithValidStudentId_ShouldReturnQRCodeId() {
        // Act
        ResponseEntity<Map<String, String>> response = studentApiController.generateQRCodeId(testStudent.getId());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");
        assertTrue(response.getBody().containsKey("qrCodeId"), "Response should contain qrCodeId");
        assertNotNull(response.getBody().get("qrCodeId"), "QR code ID should not be null");

        System.out.println("✓ QR code ID retrieved successfully");
        System.out.println("  - QR code ID: " + response.getBody().get("qrCodeId"));
    }

    // ========================================================================
    // TEST 6: PHOTO UPLOAD
    // ========================================================================

    @Test
    public void testUploadPhoto_WithValidImage_ShouldSucceed() {
        // Arrange: Create mock image file
        MockMultipartFile photoFile = new MockMultipartFile(
            "photo",
            "student_photo.jpg",
            "image/jpeg",
            "fake_image_data".getBytes()
        );

        // Act
        ResponseEntity<Map<String, String>> response = studentApiController.uploadPhoto(testStudent.getId(), photoFile);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody(), "Response body should be present");

        // Verify photo path was saved
        Student updatedStudent = studentRepository.findById(testStudent.getId()).get();
        assertNotNull(updatedStudent.getPhotoPath(), "Photo path should be saved");

        System.out.println("✓ Photo uploaded successfully");
        System.out.println("  - Photo path: " + updatedStudent.getPhotoPath());
    }

    @Test
    public void testUploadPhoto_WithNonExistentStudent_ShouldReturnNotFound() {
        // Arrange
        MockMultipartFile photoFile = new MockMultipartFile(
            "photo",
            "student_photo.jpg",
            "image/jpeg",
            "fake_image_data".getBytes()
        );

        // Act
        ResponseEntity<Map<String, String>> response = studentApiController.uploadPhoto(999999L, photoFile);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Should return NOT_FOUND status");

        System.out.println("✓ Photo upload for non-existent student correctly rejected");
    }

    @Test
    public void testUploadPhoto_WithEmptyFile_ShouldReturnBadRequest() {
        // Arrange: Create empty file
        MockMultipartFile emptyFile = new MockMultipartFile(
            "photo",
            "empty.jpg",
            "image/jpeg",
            new byte[0] // Empty file
        );

        // Act
        ResponseEntity<Map<String, String>> response = studentApiController.uploadPhoto(testStudent.getId(), emptyFile);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Should return BAD_REQUEST status");

        System.out.println("✓ Empty photo file correctly rejected");
    }

    // ========================================================================
    // TEST 7: MULTI-FACTOR AUTHENTICATION COMBINATIONS
    // ========================================================================

    @Test
    public void testAuthenticateStudent_WithAllFactors_ShouldSucceed() {
        // Arrange: Enable all authentication factors
        testStudent.setPinRequired(true);
        studentRepository.save(testStudent);

        // Mock facial recognition
        FacialRecognitionService.VerificationResult verificationResult =
            FacialRecognitionService.VerificationResult.success(0.95);
        when(facialRecognitionService.verifyFace(anyString(), anyString()))
            .thenReturn(verificationResult);

        // Full authentication request
        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .qrCodeId(testStudent.getQrCodeId())
            .photoBase64("BASE64_ENCODED_PHOTO_DATA")
            .pin(TEST_PIN)
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertTrue(responseBody.getSuccess(), "Authentication should succeed");
        assertTrue(responseBody.getFaceVerified(), "Face should be verified");
        assertTrue(responseBody.getPinVerified(), "PIN should be verified");
        assertTrue(responseBody.getFaceMatchConfidence() >= 0.75, "Face confidence should be high");

        System.out.println("✓ Multi-factor authentication (all factors) successful");
        System.out.println("  - Password: ✓");
        System.out.println("  - QR Code: ✓");
        System.out.println("  - Facial Recognition: ✓ (confidence: " + responseBody.getFaceMatchConfidence() + ")");
        System.out.println("  - PIN: ✓");
    }

    @Test
    public void testAuthenticateStudent_WithIEPFlag_ShouldIncludeInResponse() {
        // Arrange: Student with IEP
        testStudent.setHasIEP(true);
        testStudent.setHas504Plan(false);
        studentRepository.save(testStudent);

        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertTrue(responseBody.getSuccess(), "Authentication should succeed");
        assertTrue(responseBody.getHasIEP(), "IEP flag should be true");
        assertFalse(responseBody.getHas504Plan(), "504 Plan flag should be false");

        System.out.println("✓ IEP flag correctly included in authentication response");
    }

    @Test
    public void testAuthenticateStudent_With504PlanFlag_ShouldIncludeInResponse() {
        // Arrange: Student with 504 Plan
        testStudent.setHasIEP(false);
        testStudent.setHas504Plan(true);
        studentRepository.save(testStudent);

        StudentAuthRequestDTO request = StudentAuthRequestDTO.builder()
            .studentId(TEST_STUDENT_ID)
            .password(TEST_PASSWORD)
            .build();

        // Act
        ResponseEntity<StudentAuthResponseDTO> response = studentApiController.authenticateStudent(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return OK status");
        assertNotNull(response.getBody());

        StudentAuthResponseDTO responseBody = response.getBody();
        assertTrue(responseBody.getSuccess(), "Authentication should succeed");
        assertFalse(responseBody.getHasIEP(), "IEP flag should be false");
        assertTrue(responseBody.getHas504Plan(), "504 Plan flag should be true");

        System.out.println("✓ 504 Plan flag correctly included in authentication response");
    }
}
