package com.heronix.controller;

import com.heronix.model.dto.AIAnalysisResult;
import com.heronix.model.dto.AIIssue;
import com.heronix.service.BackgroundScheduleAnalyzer;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for AIStatusController
 * Tests AI status and analysis REST API endpoints
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
public class AIStatusControllerTest {

    @Autowired(required = false)
    private AIStatusController aiStatusController;

    @MockBean(name = "ollamaAIService")
    private OllamaAIService mockAIService;

    @MockBean(name = "backgroundScheduleAnalyzer")
    private BackgroundScheduleAnalyzer mockAnalyzer;

    private AIAnalysisResult mockAnalysisResult;

    @BeforeEach
    public void setup() {
        // Skip tests if controller is not available (AI disabled in test profile)
        if (aiStatusController == null) {
            return;
        }

        // Create mock analysis result
        mockAnalysisResult = new AIAnalysisResult();
        mockAnalysisResult.setHealthScore(85);
        mockAnalysisResult.setSummary("Schedule is in good condition");
        mockAnalysisResult.setAnalysisTimeMs(1500L);
        mockAnalysisResult.setCriticalIssues(new ArrayList<>());

        // Create warning issue
        AIIssue warningIssue = AIIssue.builder()
            .severity(AIIssue.Severity.WARNING)
            .type(AIIssue.Type.ROOM_CAPACITY)
            .description("Room 101 slightly over capacity")
            .build();
        mockAnalysisResult.setWarnings(List.of(warningIssue));

        // Create suggestion
        AIIssue suggestion = AIIssue.builder()
            .severity(AIIssue.Severity.INFO)
            .type(AIIssue.Type.OTHER)
            .description("Consider balancing teacher workloads")
            .build();
        mockAnalysisResult.setSuggestions(List.of(suggestion));
    }

    // ========== GET AI STATUS ==========

    @Test
    public void testGetAIStatus_WhenOllamaAvailable_ShouldReturnFullStatus() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAIService.isOllamaAvailable()).thenReturn(true);
        when(mockAnalyzer.getLastAnalysis()).thenReturn(mockAnalysisResult);
        when(mockAnalyzer.getLastAnalysisTime()).thenReturn(LocalDateTime.now());

        // Act
        ResponseEntity<Map<String, Object>> response = aiStatusController.getAIStatus();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("enabled"));
        assertEquals(true, response.getBody().get("ollamaAvailable"));
        assertEquals(85, response.getBody().get("healthScore"));
        assertEquals("Good", response.getBody().get("healthStatus"));
        assertEquals("🟢", response.getBody().get("healthIcon"));

        System.out.println("✓ AI status retrieved successfully");
        System.out.println("  - Health Score: " + response.getBody().get("healthScore"));
        System.out.println("  - Status: " + response.getBody().get("healthStatus"));
    }

    @Test
    public void testGetAIStatus_WhenOllamaUnavailable_ShouldReturnMessage() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAIService.isOllamaAvailable()).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = aiStatusController.getAIStatus();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("enabled"));
        assertEquals(false, response.getBody().get("ollamaAvailable"));
        assertTrue(response.getBody().get("message").toString().contains("Ollama server not available"));

        System.out.println("✓ AI status handles Ollama unavailable");
        System.out.println("  - Message: " + response.getBody().get("message"));
    }

    @Test
    public void testGetAIStatus_WhenNoAnalysis_ShouldReturnDefaultValues() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAIService.isOllamaAvailable()).thenReturn(true);
        when(mockAnalyzer.getLastAnalysis()).thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = aiStatusController.getAIStatus();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().get("healthScore"));
        assertEquals(false, response.getBody().get("hasIssues"));

        System.out.println("✓ AI status handles no analysis available");
    }

    @Test
    public void testGetAIStatus_HealthScoreMapping_ShouldSetCorrectIcons() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Test different health scores and their icons
        testHealthScoreIcon(95, "✅", "Excellent");
        testHealthScoreIcon(80, "🟢", "Good");
        testHealthScoreIcon(65, "🟡", "Fair");
        testHealthScoreIcon(50, "🟠", "Poor");
        testHealthScoreIcon(30, "🔴", "Critical");

        System.out.println("✓ Health score icons mapped correctly");
    }

    private void testHealthScoreIcon(int score, String expectedIcon, String expectedStatus) {
        mockAnalysisResult.setHealthScore(score);
        when(mockAIService.isOllamaAvailable()).thenReturn(true);
        when(mockAnalyzer.getLastAnalysis()).thenReturn(mockAnalysisResult);
        when(mockAnalyzer.getLastAnalysisTime()).thenReturn(LocalDateTime.now());

        ResponseEntity<Map<String, Object>> response = aiStatusController.getAIStatus();

        assertEquals(expectedIcon, response.getBody().get("healthIcon"));
        assertEquals(expectedStatus, response.getBody().get("healthStatus"));
    }

    // ========== GET LAST ANALYSIS ==========

    @Test
    public void testGetLastAnalysis_WithValidAnalysis_ShouldReturnResult() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAnalyzer.getLastAnalysis()).thenReturn(mockAnalysisResult);

        // Act
        ResponseEntity<AIAnalysisResult> response = aiStatusController.getLastAnalysis();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(85, response.getBody().getHealthScore());
        assertEquals("Schedule is in good condition", response.getBody().getSummary());

        System.out.println("✓ Last analysis retrieved successfully");
        System.out.println("  - Health Score: " + response.getBody().getHealthScore());
    }

    @Test
    public void testGetLastAnalysis_WhenNoAnalysis_ShouldReturn204() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAnalyzer.getLastAnalysis()).thenReturn(null);

        // Act
        ResponseEntity<AIAnalysisResult> response = aiStatusController.getLastAnalysis();

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        System.out.println("✓ No analysis correctly returns 204");
    }

    // ========== GET ANALYSIS HISTORY ==========

    @Test
    public void testGetAnalysisHistory_ShouldReturnLimitedResults() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        List<AIAnalysisResult> history = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            AIAnalysisResult result = new AIAnalysisResult();
            result.setHealthScore(80 + i);
            history.add(result);
        }
        when(mockAnalyzer.getAnalysisHistory()).thenReturn(history);

        // Act - Request last 10
        ResponseEntity<List<AIAnalysisResult>> response = aiStatusController.getAnalysisHistory(10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().size());

        System.out.println("✓ Analysis history limited correctly");
        System.out.println("  - Requested: 10, Returned: " + response.getBody().size());
    }

    // ========== GET HEALTH TREND ==========

    @Test
    public void testGetHealthTrend_ShouldReturnTrendData() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        List<Integer> trend = List.of(75, 80, 82, 85, 88, 90);
        when(mockAnalyzer.getHealthTrend(20)).thenReturn(trend);

        // Act
        ResponseEntity<Map<String, Object>> response = aiStatusController.getHealthTrend(20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(6, response.getBody().get("count"));
        assertEquals(90, response.getBody().get("current"));
        assertEquals(75, response.getBody().get("min"));
        assertEquals(90, response.getBody().get("max"));

        System.out.println("✓ Health trend retrieved successfully");
        System.out.println("  - Current: " + response.getBody().get("current"));
        System.out.println("  - Average: " + response.getBody().get("average"));
    }

    // ========== TRIGGER ANALYSIS ==========

    @Test
    public void testTriggerAnalysis_ShouldExecuteImmediately() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAnalyzer.analyzeNow()).thenReturn(mockAnalysisResult);

        // Act
        ResponseEntity<Map<String, Object>> response = aiStatusController.triggerAnalysis();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("Analysis completed", response.getBody().get("message"));
        assertNotNull(response.getBody().get("result"));

        verify(mockAnalyzer, times(1)).analyzeNow();

        System.out.println("✓ Manual analysis triggered successfully");
    }

    // ========== HAS CRITICAL ISSUES ==========

    @Test
    public void testHasCriticalIssues_WhenCriticalIssuesExist_ShouldReturnTrue() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        AIIssue criticalIssue = AIIssue.builder()
            .severity(AIIssue.Severity.CRITICAL)
            .type(AIIssue.Type.TEACHER_OVERLOAD)
            .description("Teacher overload detected")
            .build();
        mockAnalysisResult.setCriticalIssues(List.of(criticalIssue));
        when(mockAnalyzer.hasUnresolvedCriticalIssues()).thenReturn(true);
        when(mockAnalyzer.getCurrentHealthScore()).thenReturn(50);
        when(mockAnalyzer.getLastAnalysis()).thenReturn(mockAnalysisResult);

        // Act
        ResponseEntity<Map<String, Object>> response = aiStatusController.hasCriticalIssues();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("hasCriticalIssues"));
        assertEquals(50, response.getBody().get("healthScore"));
        assertEquals(1, response.getBody().get("criticalIssuesCount"));

        System.out.println("✓ Critical issues detection working");
        System.out.println("  - Has critical: " + response.getBody().get("hasCriticalIssues"));
    }

    @Test
    public void testHasCriticalIssues_WhenNoCriticalIssues_ShouldReturnFalse() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAnalyzer.hasUnresolvedCriticalIssues()).thenReturn(false);
        when(mockAnalyzer.getCurrentHealthScore()).thenReturn(85);

        // Act
        ResponseEntity<Map<String, Object>> response = aiStatusController.hasCriticalIssues();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("hasCriticalIssues"));

        System.out.println("✓ No critical issues correctly reported");
    }

    // ========== GET SUMMARY ==========

    @Test
    public void testGetSummary_WithAnalysis_ShouldReturnFormattedSummary() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAnalyzer.getLastAnalysis()).thenReturn(mockAnalysisResult);
        when(mockAnalyzer.getLastAnalysisTime()).thenReturn(LocalDateTime.now());

        // Act
        ResponseEntity<Map<String, Object>> response = aiStatusController.getSummary();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("available"));
        assertEquals(85, response.getBody().get("healthScore"));

        System.out.println("✓ Summary retrieved successfully");
    }

    @Test
    public void testGetSummary_WhenNoAnalysis_ShouldReturnMessage() {
        if (aiStatusController == null) {
            System.out.println("⊘ Skipped: AI services disabled in test profile");
            return;
        }

        // Arrange
        when(mockAnalyzer.getLastAnalysis()).thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = aiStatusController.getSummary();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("available"));
        assertTrue(response.getBody().get("message").toString().contains("automatically"));

        System.out.println("✓ Summary handles no analysis gracefully");
    }
}
