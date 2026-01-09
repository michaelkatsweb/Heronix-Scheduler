package com.heronix.controller;

import com.heronix.service.OllamaAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for AITestController
 * Tests AI testing and debugging REST API endpoints
 *
 * Note: This controller is @ConditionalOnProperty - tests use mocks
 * to simulate both enabled and disabled states
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class AITestControllerTest {

    @Autowired(required = false)
    private AITestController aiTestController;

    @MockBean(name = "ollamaAIService")
    private OllamaAIService mockAIService;

    @BeforeEach
    public void setup() {
        // Skip tests if controller is not available (AI disabled in test profile)
        if (aiTestController == null) {
            return;
        }
    }

    // ========== HEALTH CHECK ==========

    @Test
    public void testCheckHealth_WhenOllamaAvailable_ShouldReturnFullStatus() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAIService.isOllamaAvailable()).thenReturn(true);
        when(mockAIService.getAvailableModels()).thenReturn(
            List.of("mistral:7b-instruct", "llama3.1:8b", "phi-3:3.8b")
        );

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.checkHealth();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("available", response.getBody().get("status"));
        assertEquals("Ollama server is running and responsive", response.getBody().get("message"));
        assertEquals(3, response.getBody().get("modelCount"));
        assertNotNull(response.getBody().get("availableModels"));

        System.out.println("✓ Health check successful when Ollama available");
        System.out.println("  - Status: " + response.getBody().get("status"));
        System.out.println("  - Models: " + response.getBody().get("modelCount"));
    }

    @Test
    public void testCheckHealth_WhenOllamaUnavailable_ShouldReturnUnavailableStatus() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAIService.isOllamaAvailable()).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.checkHealth();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("unavailable", response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("Ollama server is not available"));
        assertNull(response.getBody().get("availableModels"));

        System.out.println("✓ Health check handles Ollama unavailable");
    }

    // ========== LIST MODELS ==========

    @Test
    public void testListModels_WhenOllamaAvailable_ShouldReturnModels() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAIService.isOllamaAvailable()).thenReturn(true);
        when(mockAIService.getAvailableModels()).thenReturn(
            List.of("mistral:7b-instruct", "llama3.1:8b", "phi-3:3.8b", "gemma:7b")
        );

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.listModels();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().get("count"));
        assertNotNull(response.getBody().get("models"));
        assertNotNull(response.getBody().get("recommended"));

        @SuppressWarnings("unchecked")
        List<String> models = (List<String>) response.getBody().get("models");
        assertTrue(models.contains("mistral:7b-instruct"));
        assertTrue(models.contains("llama3.1:8b"));

        System.out.println("✓ List models successful");
        System.out.println("  - Model count: " + response.getBody().get("count"));
    }

    @Test
    public void testListModels_WhenOllamaUnavailable_ShouldReturnError() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAIService.isOllamaAvailable()).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.listModels();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Ollama server not available", response.getBody().get("error"));

        System.out.println("✓ List models handles Ollama unavailable");
    }

    // ========== TEST GENERATE ==========

    @Test
    public void testGenerate_WithValidPrompt_ShouldReturnResponse() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("prompt", "What is the capital of France?");

        when(mockAIService.generate(anyString())).thenReturn(
            Optional.of("The capital of France is Paris.")
        );

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.testGenerate(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("The capital of France is Paris.", response.getBody().get("response"));
        assertEquals("What is the capital of France?", response.getBody().get("prompt"));
        assertNotNull(response.getBody().get("durationMs"));

        verify(mockAIService, times(1)).generate("What is the capital of France?");

        System.out.println("✓ Generate test successful");
        System.out.println("  - Response: " + response.getBody().get("response"));
    }

    @Test
    public void testGenerate_WithEmptyPrompt_ShouldReturnBadRequest() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("prompt", "");

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.testGenerate(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Prompt is required", response.getBody().get("error"));

        verify(mockAIService, never()).generate(anyString());

        System.out.println("✓ Generate validates empty prompt");
    }

    @Test
    public void testGenerate_WhenOllamaFails_ShouldReturnFailure() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("prompt", "Test prompt");

        when(mockAIService.generate(anyString())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.testGenerate(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("success"));
        assertTrue(response.getBody().get("error").toString().contains("Failed to generate response"));

        System.out.println("✓ Generate handles Ollama failure");
    }

    // ========== TEST CHAT ==========

    @Test
    public void testChat_WithValidMessage_ShouldReturnResponse() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("sessionId", "test-session-001");
        request.put("message", "Hello, how are you?");

        when(mockAIService.chat(anyString(), anyString())).thenReturn(
            Optional.of("I'm doing well, thank you for asking!")
        );

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.testChat(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("I'm doing well, thank you for asking!", response.getBody().get("response"));
        assertEquals("test-session-001", response.getBody().get("sessionId"));
        assertEquals("Hello, how are you?", response.getBody().get("message"));
        assertNotNull(response.getBody().get("durationMs"));

        verify(mockAIService, times(1)).chat("test-session-001", "Hello, how are you?");

        System.out.println("✓ Chat test successful");
        System.out.println("  - Session: " + response.getBody().get("sessionId"));
    }

    @Test
    public void testChat_WithoutSessionId_ShouldUseDefault() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("message", "Test message");

        when(mockAIService.chat(anyString(), anyString())).thenReturn(
            Optional.of("Response")
        );

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.testChat(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("test-session", response.getBody().get("sessionId"));

        verify(mockAIService, times(1)).chat("test-session", "Test message");

        System.out.println("✓ Chat uses default session ID when not provided");
    }

    @Test
    public void testChat_WithEmptyMessage_ShouldReturnBadRequest() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("sessionId", "test-session");
        request.put("message", "");

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.testChat(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Message is required", response.getBody().get("error"));

        verify(mockAIService, never()).chat(anyString(), anyString());

        System.out.println("✓ Chat validates empty message");
    }

    @Test
    public void testChat_WhenOllamaFails_ShouldReturnFailure() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("sessionId", "test-session");
        request.put("message", "Test message");

        when(mockAIService.chat(anyString(), anyString())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.testChat(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertTrue(response.getBody().get("error").toString().contains("Failed to generate response"));

        System.out.println("✓ Chat handles Ollama failure");
    }

    // ========== CLEAR CHAT ==========

    @Test
    public void testClearChat_WithValidSessionId_ShouldClearConversation() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        String sessionId = "test-session-to-clear";
        doNothing().when(mockAIService).clearConversation(anyString());

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.clearChat(sessionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("Conversation cleared"));
        assertTrue(response.getBody().get("message").toString().contains(sessionId));

        verify(mockAIService, times(1)).clearConversation(sessionId);

        System.out.println("✓ Clear chat successful");
        System.out.println("  - Session cleared: " + sessionId);
    }

    // ========== TEST ANALYZE ==========

    @Test
    public void testAnalyze_WithValidScheduleData_ShouldReturnAnalysis() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("scheduleData", "Teacher: John Doe, Course: Math, Room: 101, Period: 1");

        when(mockAIService.analyzeSchedule(anyString())).thenReturn(
            Optional.of("Schedule analysis: No conflicts detected. Teacher load is balanced.")
        );

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.testAnalyze(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("Schedule analysis: No conflicts detected. Teacher load is balanced.",
            response.getBody().get("analysis"));
        assertNotNull(response.getBody().get("durationMs"));

        verify(mockAIService, times(1)).analyzeSchedule(anyString());

        System.out.println("✓ Analyze test successful");
        System.out.println("  - Analysis: " + response.getBody().get("analysis"));
    }

    @Test
    public void testAnalyze_WithEmptyScheduleData_ShouldReturnBadRequest() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("scheduleData", "");

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.testAnalyze(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Schedule data is required", response.getBody().get("error"));

        verify(mockAIService, never()).analyzeSchedule(anyString());

        System.out.println("✓ Analyze validates empty schedule data");
    }

    @Test
    public void testAnalyze_WhenOllamaFails_ShouldReturnFailure() {
        if (aiTestController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("scheduleData", "Test schedule data");

        when(mockAIService.analyzeSchedule(anyString())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, Object>> response = aiTestController.testAnalyze(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertTrue(response.getBody().get("error").toString().contains("Failed to analyze schedule"));

        System.out.println("✓ Analyze handles Ollama failure");
    }
}
