package com.heronix.controller;

import com.heronix.model.dto.AIAnalysisResult;
import com.heronix.service.BackgroundScheduleAnalyzer;
import com.heronix.service.OllamaAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for AI Status and Dashboard Integration
 *
 * Endpoints for Dashboard to display AI analysis results,
 * health scores, and issue alerts.
 *
 * @since Phase 2 - Background Analysis
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/status")
@ConditionalOnProperty(name = "ai.ollama.enabled", havingValue = "true")
public class AIStatusController {

    @Autowired(required = false)
    private OllamaAIService aiService;

    @Autowired(required = false)
    private BackgroundScheduleAnalyzer analyzer;

    /**
     * Get current AI status and health metrics
     * GET /api/ai/status
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        Map<String, Object> status = new HashMap<>();

        // Check if services are available
        if (aiService == null || analyzer == null) {
            status.put("enabled", false);
            status.put("message", "AI services are disabled");
            return ResponseEntity.ok(status);
        }

        // Check Ollama availability
        boolean ollamaAvailable = aiService.isOllamaAvailable();
        status.put("enabled", true);
        status.put("ollamaAvailable", ollamaAvailable);

        if (!ollamaAvailable) {
            status.put("message", "Ollama server not available. Please start: ollama serve");
            return ResponseEntity.ok(status);
        }

        // Get last analysis
        AIAnalysisResult lastAnalysis = analyzer.getLastAnalysis();
        LocalDateTime lastAnalysisTime = analyzer.getLastAnalysisTime();

        if (lastAnalysis == null) {
            status.put("message", "No analysis available yet. Analysis will run automatically.");
            status.put("healthScore", 0);
            status.put("hasIssues", false);
            return ResponseEntity.ok(status);
        }

        // Build status response
        status.put("healthScore", lastAnalysis.getHealthScore());
        status.put("lastAnalysisTime", lastAnalysisTime);
        status.put("criticalIssuesCount", lastAnalysis.getCriticalIssues().size());
        status.put("warningsCount", lastAnalysis.getWarnings().size());
        status.put("suggestionsCount", lastAnalysis.getSuggestions().size());
        status.put("hasIssues", lastAnalysis.hasCriticalIssues() || !lastAnalysis.getWarnings().isEmpty());
        status.put("hasCriticalIssues", lastAnalysis.hasCriticalIssues());
        status.put("summary", lastAnalysis.getSummary());
        status.put("analysisTimeMs", lastAnalysis.getAnalysisTimeMs());

        // Health status icon
        int score = lastAnalysis.getHealthScore();
        String healthIcon;
        String healthStatus;
        if (score >= 90) {
            healthIcon = "✅";
            healthStatus = "Excellent";
        } else if (score >= 75) {
            healthIcon = "🟢";
            healthStatus = "Good";
        } else if (score >= 60) {
            healthIcon = "🟡";
            healthStatus = "Fair";
        } else if (score >= 40) {
            healthIcon = "🟠";
            healthStatus = "Poor";
        } else {
            healthIcon = "🔴";
            healthStatus = "Critical";
        }
        status.put("healthIcon", healthIcon);
        status.put("healthStatus", healthStatus);

        return ResponseEntity.ok(status);
    }

    /**
     * Get last analysis details
     * GET /api/ai/status/last-analysis
     */
    @GetMapping("/last-analysis")
    public ResponseEntity<AIAnalysisResult> getLastAnalysis() {
        if (analyzer == null) {
            return ResponseEntity.notFound().build();
        }

        AIAnalysisResult lastAnalysis = analyzer.getLastAnalysis();
        if (lastAnalysis == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(lastAnalysis);
    }

    /**
     * Get analysis history
     * GET /api/ai/status/history?limit=10
     */
    @GetMapping("/history")
    public ResponseEntity<List<AIAnalysisResult>> getAnalysisHistory(
            @RequestParam(defaultValue = "10") int limit) {

        if (analyzer == null) {
            return ResponseEntity.notFound().build();
        }

        List<AIAnalysisResult> history = analyzer.getAnalysisHistory();

        // Limit results
        int start = Math.max(0, history.size() - limit);
        List<AIAnalysisResult> limitedHistory = history.subList(start, history.size());

        return ResponseEntity.ok(limitedHistory);
    }

    /**
     * Get health trend (scores over time)
     * GET /api/ai/status/health-trend?count=20
     */
    @GetMapping("/health-trend")
    public ResponseEntity<Map<String, Object>> getHealthTrend(
            @RequestParam(defaultValue = "20") int count) {

        if (analyzer == null) {
            return ResponseEntity.notFound().build();
        }

        List<Integer> trend = analyzer.getHealthTrend(count);

        Map<String, Object> response = new HashMap<>();
        response.put("trend", trend);
        response.put("count", trend.size());

        if (!trend.isEmpty()) {
            response.put("current", trend.get(trend.size() - 1));
            response.put("average", trend.stream().mapToInt(Integer::intValue).average().orElse(0));
            response.put("min", trend.stream().mapToInt(Integer::intValue).min().orElse(0));
            response.put("max", trend.stream().mapToInt(Integer::intValue).max().orElse(0));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Trigger immediate analysis
     * POST /api/ai/status/analyze-now
     */
    @PostMapping("/analyze-now")
    public ResponseEntity<Map<String, Object>> triggerAnalysis() {
        if (analyzer == null) {
            return ResponseEntity.notFound().build();
        }

        log.info("Manual analysis triggered via API");

        AIAnalysisResult result = analyzer.analyzeNow();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Analysis completed");
        response.put("result", result);

        return ResponseEntity.ok(response);
    }

    /**
     * Check if there are unresolved critical issues
     * GET /api/ai/status/has-critical-issues
     */
    @GetMapping("/has-critical-issues")
    public ResponseEntity<Map<String, Object>> hasCriticalIssues() {
        if (analyzer == null) {
            return ResponseEntity.ok(Map.of("hasCriticalIssues", false));
        }

        boolean hasCritical = analyzer.hasUnresolvedCriticalIssues();
        int healthScore = analyzer.getCurrentHealthScore();

        Map<String, Object> response = new HashMap<>();
        response.put("hasCriticalIssues", hasCritical);
        response.put("healthScore", healthScore);

        if (hasCritical) {
            AIAnalysisResult lastAnalysis = analyzer.getLastAnalysis();
            response.put("criticalIssuesCount", lastAnalysis.getCriticalIssues().size());
            response.put("message", "Critical issues detected - review schedule");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get formatted summary for display
     * GET /api/ai/status/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        if (analyzer == null) {
            return ResponseEntity.ok(Map.of(
                "available", false,
                "message", "AI analysis not available"
            ));
        }

        AIAnalysisResult lastAnalysis = analyzer.getLastAnalysis();
        if (lastAnalysis == null) {
            return ResponseEntity.ok(Map.of(
                "available", true,
                "message", "Analysis will run automatically"
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("available", true);
        response.put("healthScore", lastAnalysis.getHealthScore());
        response.put("formattedSummary", lastAnalysis.getFormattedSummary());
        response.put("lastAnalysisTime", analyzer.getLastAnalysisTime());

        return ResponseEntity.ok(response);
    }
}
