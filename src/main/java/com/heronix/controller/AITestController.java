package com.heronix.controller;

import com.heronix.service.OllamaAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for testing AI integration
 * Only active when ai.ollama.enabled=true
 *
 * Endpoints:
 * - GET  /api/ai/test/health    - Check if Ollama is available
 * - GET  /api/ai/test/models    - List available models
 * - POST /api/ai/test/generate  - Test one-off generation
 * - POST /api/ai/test/chat      - Test chat conversation
 *
 * @since Phase 4 - AI Integration
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/test")
@ConditionalOnProperty(name = "ai.ollama.enabled", havingValue = "true")
public class AITestController {

    @Autowired(required = false)
    private OllamaAIService aiService;

    /**
     * Health check endpoint
     * GET /api/ai/test/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        log.info("AI health check requested");

        Map<String, Object> response = new HashMap<>();

        if (aiService == null) {
            response.put("status", "disabled");
            response.put("message", "AI service is disabled. Set ai.ollama.enabled=true in application.yml");
            return ResponseEntity.ok(response);
        }

        boolean available = aiService.isOllamaAvailable();
        response.put("status", available ? "available" : "unavailable");
        response.put("message", available
            ? "Ollama server is running and responsive"
            : "Ollama server is not available. Please start: ollama serve");

        if (available) {
            List<String> models = aiService.getAvailableModels();
            response.put("availableModels", models);
            response.put("modelCount", models.size());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * List available models
     * GET /api/ai/test/models
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> listModels() {
        log.info("List models requested");

        Map<String, Object> response = new HashMap<>();

        if (aiService == null) {
            response.put("error", "AI service is disabled");
            return ResponseEntity.ok(response);
        }

        if (!aiService.isOllamaAvailable()) {
            response.put("error", "Ollama server not available");
            return ResponseEntity.ok(response);
        }

        List<String> models = aiService.getAvailableModels();
        response.put("models", models);
        response.put("count", models.size());
        response.put("recommended", List.of(
            "mistral:7b-instruct",
            "llama3.1:8b",
            "phi-3:3.8b"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Test one-off generation
     * POST /api/ai/test/generate
     * Body: { "prompt": "Your question here" }
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> testGenerate(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        log.info("Generate test requested with prompt: {}", prompt);

        Map<String, Object> response = new HashMap<>();

        if (aiService == null) {
            response.put("error", "AI service is disabled");
            return ResponseEntity.ok(response);
        }

        if (prompt == null || prompt.trim().isEmpty()) {
            response.put("error", "Prompt is required");
            return ResponseEntity.badRequest().body(response);
        }

        long startTime = System.currentTimeMillis();
        Optional<String> aiResponse = aiService.generate(prompt);
        long duration = System.currentTimeMillis() - startTime;

        if (aiResponse.isPresent()) {
            response.put("success", true);
            response.put("response", aiResponse.get());
            response.put("durationMs", duration);
            response.put("prompt", prompt);
        } else {
            response.put("success", false);
            response.put("error", "Failed to generate response. Check if Ollama is running.");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Test chat conversation
     * POST /api/ai/test/chat
     * Body: { "sessionId": "test-session", "message": "Your message" }
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> testChat(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "test-session");
        String message = request.get("message");

        log.info("Chat test requested for session: {}, message: {}", sessionId, message);

        Map<String, Object> response = new HashMap<>();

        if (aiService == null) {
            response.put("error", "AI service is disabled");
            return ResponseEntity.ok(response);
        }

        if (message == null || message.trim().isEmpty()) {
            response.put("error", "Message is required");
            return ResponseEntity.badRequest().body(response);
        }

        long startTime = System.currentTimeMillis();
        Optional<String> aiResponse = aiService.chat(sessionId, message);
        long duration = System.currentTimeMillis() - startTime;

        if (aiResponse.isPresent()) {
            response.put("success", true);
            response.put("response", aiResponse.get());
            response.put("durationMs", duration);
            response.put("sessionId", sessionId);
            response.put("message", message);
        } else {
            response.put("success", false);
            response.put("error", "Failed to generate response. Check if Ollama is running.");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Clear chat conversation
     * DELETE /api/ai/test/chat/{sessionId}
     */
    @DeleteMapping("/chat/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearChat(@PathVariable String sessionId) {
        log.info("Clear chat requested for session: {}", sessionId);

        Map<String, Object> response = new HashMap<>();

        if (aiService == null) {
            response.put("error", "AI service is disabled");
            return ResponseEntity.ok(response);
        }

        aiService.clearConversation(sessionId);
        response.put("success", true);
        response.put("message", "Conversation cleared for session: " + sessionId);

        return ResponseEntity.ok(response);
    }

    /**
     * Test schedule analysis
     * POST /api/ai/test/analyze
     * Body: { "scheduleData": "Schedule information here" }
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> testAnalyze(@RequestBody Map<String, String> request) {
        String scheduleData = request.get("scheduleData");
        log.info("Analyze test requested");

        Map<String, Object> response = new HashMap<>();

        if (aiService == null) {
            response.put("error", "AI service is disabled");
            return ResponseEntity.ok(response);
        }

        if (scheduleData == null || scheduleData.trim().isEmpty()) {
            response.put("error", "Schedule data is required");
            return ResponseEntity.badRequest().body(response);
        }

        long startTime = System.currentTimeMillis();
        Optional<String> aiResponse = aiService.analyzeSchedule(scheduleData);
        long duration = System.currentTimeMillis() - startTime;

        if (aiResponse.isPresent()) {
            response.put("success", true);
            response.put("analysis", aiResponse.get());
            response.put("durationMs", duration);
        } else {
            response.put("success", false);
            response.put("error", "Failed to analyze schedule. Check if Ollama is running.");
        }

        return ResponseEntity.ok(response);
    }
}
